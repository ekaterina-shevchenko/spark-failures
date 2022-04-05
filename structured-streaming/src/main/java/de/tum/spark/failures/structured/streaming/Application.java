package de.tum.spark.failures.structured.streaming;

import de.tum.spark.failures.structured.streaming.config.KafkaConfig;
import de.tum.spark.failures.structured.streaming.config.StreamingConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;
import org.apache.spark.sql.streaming.OutputMode;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;

import java.util.concurrent.TimeoutException;

@Slf4j
public class Application {

  public static void main(String[] args) throws TimeoutException, StreamingQueryException {
    SparkSession session = SparkSession
        .builder()
        .appName("structured-streaming-app-purchase-count")
        .getOrCreate();
    Dataset<Row> dataframe = initInputStream(session);
    Dataset<Row> rowDataset = dataframe
        .selectExpr("CAST(value AS STRING) as message", "timestamp as kafkaTimestamp", "current_timestamp() as sparkIngestionTimestamp")
        .select(functions.from_json(functions.col("message"), StreamingConfig.schema)
                    .as("json"), functions.col("kafkaTimestamp"), functions.col("sparkIngestionTimestamp"))
        .select("sparkIngestionTimestamp", "kafkaTimestamp", "json.product", "json.number")
        .withWatermark("kafkaTimestamp", "0 seconds")
        .groupBy(
            functions.window(functions.col("kafkaTimestamp"), "2 seconds", "2 seconds"),
            functions.col("product")
        ).agg(functions.to_json(functions.struct(
                                    functions.expr("count(number)").cast("int").as("totalCount"),
                                    functions.expr("sum(number)").cast("int").as("totalNumber"),
                                    functions.col("window").getItem("start").as("windowStart"),
                                    functions.col("window").getItem("end").as("windowEnd"),
                                    functions.col("product"),
                                    functions.expr("min(sparkIngestionTimestamp)").as("minimumSparkIngestionTimestamp"), // timestamp of first event in the window
                                    functions.expr("min(kafkaTimestamp)").as("minimumKafkaIngestionTimestamp"), // timestamp of first event in the window
                                    functions.current_timestamp().as("timestamp")
                                )
        ).as("value"))
        .withColumnRenamed("product", "key");
    StreamingQuery query = writeOutputStream(rowDataset);
    query.awaitTermination();
  }

  private static Dataset<Row> initInputStream(SparkSession session) {
    return session
        .readStream()
        .format("kafka")
        .option("kafka.bootstrap.servers", KafkaConfig.BOOTSTRAP_KAFKA_SERVER)
        .option("subscribe", KafkaConfig.TOPIC_PURCHASES)
        .load();
  }

  private static StreamingQuery writeOutputStream(Dataset<?> dataset) throws TimeoutException {
    return dataset
        .writeStream()
        .format("kafka")
        .option("kafka.bootstrap.servers", KafkaConfig.BOOTSTRAP_KAFKA_SERVER)
        .option("topic", KafkaConfig.TOPIC_OUTPUT)
        .option("acks","all") // TODO: check that it works
        .option("kafka.acks","all") // TODO: check that it works
        .option("max.in.flight.requests.per.connection", 1) // TODO: check that it works
        .option("kafka.max.in.flight.requests.per.connection", 1) // TODO: check that it works
        .option("checkpointLocation", "logs/checkpoint1")
        .outputMode(OutputMode.Append())
        .start();
  }
}

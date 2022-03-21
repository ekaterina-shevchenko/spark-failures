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
            .selectExpr("CAST(value AS STRING) as message", "timestamp as kafkaTimestamp")
            .select(functions.from_json(functions.col("message"), StreamingConfig.schema)
                        .as("json"), functions.col("kafkaTimestamp"))
            .select("kafkaTimestamp", "json.product", "json.number")
            .withWatermark("kafkaTimestamp", "1 seconds")
            .groupBy(
                functions.window(functions.col("kafkaTimestamp"), "5 seconds", "5 seconds"),
                functions.col("product")
            ).sum("number")
            .withColumnRenamed("sum(number)", "number")
            .selectExpr("product as key", "CAST(CAST(number AS int) as BINARY) as value");
        StreamingQuery query = writeOutputStream(rowDataset);
        query.awaitTermination();
    }

    private static Dataset<Row> initInputStream(SparkSession session) {
        return session
                .readStream()
                .format("kafka")
                .option("kafka.bootstrap.servers", KafkaConfig.BOOTSTRAP_KAFKA_SERVER)
                .option("subscribe", KafkaConfig.TOPIC_PURCHASES)
//                .option("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
//                .option("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
//                .option("group.id", "consumer-group")
//                .option("auto.offset.reset", "latest")
//                .option("enable.auto.commit", false)
                .load();
    }

    private static StreamingQuery writeOutputStream(Dataset<?> dataset) throws TimeoutException {
        return dataset
                .writeStream()
                .format("kafka")
                .option("kafka.bootstrap.servers", KafkaConfig.BOOTSTRAP_KAFKA_SERVER)
                .option("topic", KafkaConfig.TOPIC_OUTPUT)
//                .option("acks", "all")
//                .option("retries", 10)
//                .option("buffer.memory", 100_000_000)
//                .option("batch.size", 100_000)
//                .option("compression.type", "gzip")
//                .option("max.request.size", 5_000_000)
//                .option("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
//                .option("value.serializer", "org.apache.kafka.common.serialization.IntegerSerializer")
//                .option("max.in.flight.requests.per.connection", 50)
                .option("checkpointLocation", "logs/checkpoint1")
                .outputMode(OutputMode.Append())
                .start();
    }
}

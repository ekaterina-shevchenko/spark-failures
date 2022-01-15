package de.tum.spark.failures.structured.streaming;

import de.tum.spark.failures.structured.streaming.config.KafkaConfig;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

public class Application {

    public static void main(String[] args) {
        SparkSession session = SparkSession
                .builder()
                .appName("JavaStructuredNetworkWordCount")
                .getOrCreate();

        Dataset<Row> df = session
                .readStream()
                .format("kafka")
                .option("kafka.bootstrap.servers", KafkaConfig.BOOTSTRAP_KAFKA_SERVER)
                .option("subscribe", KafkaConfig.TOPIC_PURCHASES)
                .option("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
                .option("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
                .option("group.id", "consumer-group")
                .option("auto.offset.reset", "latest")
                .option("enable.auto.commit", false)
                .load();

        df.selectExpr("CAST(key AS STRING)", "CAST(value AS STRING)");
    }
}

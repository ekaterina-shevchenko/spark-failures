package de.tum.spark.failures.streaming.config;

import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.HashMap;
import java.util.Map;

public class KafkaConfig {

    public static final String TOPIC_PURCHASES = "purchases";
    public static final String TOPIC_ADS = "advertisements";
    public static final String BOOTSTRAP_KAFKA_SERVER = "kafka:9092";
    public static final String TOPIC_OUTPUT = "output";

    public static Map<String, Object> initKafkaConsumerParameters() {
        Map<String, Object> kafkaParams = new HashMap<>();
        kafkaParams.put("bootstrap.servers", BOOTSTRAP_KAFKA_SERVER);
        kafkaParams.put("key.deserializer", StringDeserializer.class);
        kafkaParams.put("value.deserializer", StringDeserializer.class);
        kafkaParams.put("group.id", "consumer-group");
        kafkaParams.put("auto.offset.reset", "latest");
        kafkaParams.put("enable.auto.commit", false);
        return kafkaParams;
    }

    public static Map<String, Object> initKafkaProducerParameters() {
        Map<String, Object> kafkaParams = new HashMap<>();
        kafkaParams.put("bootstrap.servers", BOOTSTRAP_KAFKA_SERVER);
        kafkaParams.put("acks", "all");
        kafkaParams.put("retries", 10);
        kafkaParams.put("buffer.memory", 100_000_000);
        kafkaParams.put("batch.size", 100_000);
        kafkaParams.put("compression.type", "gzip");
        kafkaParams.put("max.request.size", 5_000_000);
        kafkaParams.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        kafkaParams.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        kafkaParams.put("max.in.flight.requests.per.connection", 50);
        return kafkaParams;
    }

}

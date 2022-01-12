package de.tum.spark.failures.streaming.config;

import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.HashMap;
import java.util.Map;

public class KafkaConfig {

    public static final String TOPIC_PURCHASES = "purchases";
    public static final String TOPIC_ADS = "advertisements";
    public static final String BOOTSTRAP_KAFKA_SERVER = "127.0.0.1:9092";
    public static final String TOPIC_OUTPUT = "output";

    public static Map<String, Object> initKafkaParameters() {
        Map<String, Object> kafkaParams = new HashMap<>();
        kafkaParams.put("bootstrap.servers", KafkaConfig.BOOTSTRAP_KAFKA_SERVER);
        kafkaParams.put("key.deserializer", StringDeserializer.class);
        kafkaParams.put("value.deserializer", StringDeserializer.class);
        kafkaParams.put("group.id", "consumer-group");
        kafkaParams.put("auto.offset.reset", "latest");
        kafkaParams.put("enable.auto.commit", false);
        return kafkaParams;
    }

}

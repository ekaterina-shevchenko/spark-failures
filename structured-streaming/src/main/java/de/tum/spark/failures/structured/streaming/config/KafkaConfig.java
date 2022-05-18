package de.tum.spark.failures.structured.streaming.config;


import java.util.HashMap;
import java.util.Map;

public class KafkaConfig {

    public static final String TOPIC_PURCHASES = "purchases";
    public static final String BOOTSTRAP_KAFKA_SERVER = "swarm-cluster_kafka_1:9092";
    public static final String TOPIC_OUTPUT = "output";

    public static Map<String, Object> initKafkaAdminParameters() {
        Map<String, Object> kafkaParams = new HashMap<>();
        kafkaParams.put("bootstrap.servers", BOOTSTRAP_KAFKA_SERVER);
        return kafkaParams;
    }

}

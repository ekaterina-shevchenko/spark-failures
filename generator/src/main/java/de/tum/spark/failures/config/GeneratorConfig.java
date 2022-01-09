package de.tum.spark.failures.config;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GeneratorConfig {

    private static final Integer DEFAULT_THROUGHPUT = 100_000;
    private static final String DEFAULT_TOPIC_PURCHASES = "purchases";
    private static final String DEFAULT_TOPIC_ADS = "advertisements";
    public static final String DEFAULT_BOOTSTRAP_KAFKA_SERVER = "127.0.0.1:9092"; // 127.0.0.1 worked locally

    private static final String THROUGHPUT_PROPERTY = "throughput";
    private static final String TOPIC_PURCHASES_PROPERTY = "topic.purchases";
    private static final String TOPIC_ADS_PROPERTY = "topic.ads";
    private static final String BOOTSTRAP_KAFKA_SERVER_PROPERTY = "bootstrap.kafka.server";

    public static final Integer THROUGHPUT;
    public static final String TOPIC_PURCHASES;
    public static final String TOPIC_ADS;
    public static final String BOOTSTRAP_KAFKA_SERVER;

    static {
        THROUGHPUT = getOrDefault(THROUGHPUT_PROPERTY, DEFAULT_THROUGHPUT);
        TOPIC_PURCHASES = getOrDefault(TOPIC_PURCHASES_PROPERTY, DEFAULT_TOPIC_PURCHASES);
        TOPIC_ADS = getOrDefault(TOPIC_ADS_PROPERTY, DEFAULT_TOPIC_ADS);
        BOOTSTRAP_KAFKA_SERVER = getOrDefault(BOOTSTRAP_KAFKA_SERVER_PROPERTY, DEFAULT_BOOTSTRAP_KAFKA_SERVER);
    }

    private static Integer getOrDefault(String property, Integer defaultValue) {
        return System.getProperty(property) == null ? defaultValue: Integer.parseInt(System.getProperty(property));
    }
    private static String getOrDefault(String property, String defaultValue) {
        return System.getProperty(property) == null ? defaultValue: System.getProperty(property);
    }
}

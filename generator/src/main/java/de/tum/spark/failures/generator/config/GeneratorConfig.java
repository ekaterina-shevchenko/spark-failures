package de.tum.spark.failures.generator.config;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GeneratorConfig {

    private static final Integer DEFAULT_THROUGHPUT = 150_000;
    private static final String DEFAULT_TOPIC_OUTPUT = "output";
    private static final String DEFAULT_TOPIC_PURCHASES = "purchases";
    public static final String DEFAULT_BOOTSTRAP_KAFKA_SERVER = "127.0.0.1:9092"; // 127.0.0.1 worked locally

    private static final String THROUGHPUT_PROPERTY = "throughput";
    private static final String TOPIC_PURCHASES_PROPERTY = "topic.purchases";
    private static final String BOOTSTRAP_KAFKA_SERVER_PROPERTY = "bootstrap.kafka.server";
    private static final String TOPIC_OUTPUT_PROPERTY = "output";

    public static final Integer THROUGHPUT;
    public static final String TOPIC_PURCHASES;
    public static final String TOPIC_OUTPUT;
    public static final String BOOTSTRAP_KAFKA_SERVER;

    static {
        THROUGHPUT = getOrDefault(THROUGHPUT_PROPERTY, DEFAULT_THROUGHPUT);
        TOPIC_PURCHASES = getOrDefault(TOPIC_PURCHASES_PROPERTY, DEFAULT_TOPIC_PURCHASES);
        TOPIC_OUTPUT = getOrDefault(TOPIC_OUTPUT_PROPERTY, DEFAULT_TOPIC_OUTPUT);
        BOOTSTRAP_KAFKA_SERVER = getOrDefault(BOOTSTRAP_KAFKA_SERVER_PROPERTY, DEFAULT_BOOTSTRAP_KAFKA_SERVER);
    }

    private static Integer getOrDefault(String property, Integer defaultValue) {
        return System.getProperty(property) == null ? defaultValue: Integer.parseInt(System.getProperty(property));
    }
    private static String getOrDefault(String property, String defaultValue) {
        return System.getProperty(property) == null ? defaultValue: System.getProperty(property);
    }
}

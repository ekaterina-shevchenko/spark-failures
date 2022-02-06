package de.tum.spark.failures.structured.streaming.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class KafkaConfig {

    public static final String TOPIC_PURCHASES = "purchases";
    public static final String TOPIC_ADS = "advertisements";
    public static final String BOOTSTRAP_KAFKA_SERVER = "kafka:9092";
    public static final String TOPIC_OUTPUT = "output";

    static {
        AdminClient kafkaAdmin = initKafkaAdmin();
        kafkaAdmin.createTopics(
                Collections.singletonList(new NewTopic(TOPIC_OUTPUT, 3, (short) 1))
        );
        kafkaAdmin.close();
    }

    private static AdminClient initKafkaAdmin() {
        Map<String, Object> props = new HashMap<>();
        props.put("bootstrap.servers", BOOTSTRAP_KAFKA_SERVER);
        return KafkaAdminClient.create(props);
    }

}

package de.tum.spark.failures.structured.streaming.config;

import java.util.ArrayList;
import java.util.List;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class KafkaConfig {

    public static final String TOPIC_PURCHASES = "purchases";
    public static final String BOOTSTRAP_KAFKA_SERVER = "kafka_1:9092";
    public static final String TOPIC_OUTPUT = "output";

    static {
        AdminClient kafkaAdmin = initKafkaAdmin();
        List<NewTopic> topics = new ArrayList<>();
        topics.add(new NewTopic(TOPIC_PURCHASES, 6, (short) 1));
        topics.add(new NewTopic(TOPIC_OUTPUT, 6, (short) 1));
        kafkaAdmin.createTopics(topics);
        kafkaAdmin.close();
    }

    private static AdminClient initKafkaAdmin() {
        Map<String, Object> props = new HashMap<>();
        props.put("bootstrap.servers", BOOTSTRAP_KAFKA_SERVER);
        props.put("batch.size", 8192);
        props.put("buffer.memory", 16384);
        props.put("kafka.batch.size", 8192);
        props.put("kafka.buffer.memory", 16384);
        return KafkaAdminClient.create(props);
    }

}

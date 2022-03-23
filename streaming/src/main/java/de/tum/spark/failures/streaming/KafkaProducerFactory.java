package de.tum.spark.failures.streaming;

import de.tum.spark.failures.streaming.config.KafkaConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;

import java.util.Map;

public class KafkaProducerFactory {

    private static final Producer<Object, String> kafkaProducer;

    static {
        kafkaProducer = new KafkaProducer<>(KafkaConfig.initKafkaProducerParameters());
    }

    public static Producer<Object, String> getKafkaProducer() {
        return kafkaProducer;
    }
}

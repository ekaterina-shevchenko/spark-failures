package de.tum.spark.failures.streaming;

import de.tum.spark.failures.streaming.config.KafkaConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;

public class KafkaProducerFactory {

    private static final Producer<String, Integer> kafkaProducer;

    static {
        kafkaProducer = new KafkaProducer<>(KafkaConfig.initKafkaProducerParameters());
    }

    public static Producer<String, Integer> getKafkaProducer() {
        return kafkaProducer;
    }
}

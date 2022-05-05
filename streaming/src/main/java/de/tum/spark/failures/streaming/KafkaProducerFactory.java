package de.tum.spark.failures.streaming;

import de.tum.spark.failures.streaming.config.KafkaConfig;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;

public class KafkaProducerFactory {

  private static final Producer<Object, String> kafkaProducer = new KafkaProducer<>(KafkaConfig.initKafkaProducerParameters());

  public static Producer<Object, String> getKafkaProducer() {
    return kafkaProducer;
  }

}

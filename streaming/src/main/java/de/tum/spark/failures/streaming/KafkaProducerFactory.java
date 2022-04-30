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

  private static final Producer<Object, String> kafkaProducer;

  static {
    kafkaProducer = new KafkaProducer<>(KafkaConfig.initKafkaProducerParameters());
    AdminClient adminClient = KafkaAdminClient.create(KafkaConfig.initKafkaAdminParameters());
    try {
      adminClient.createTopics(Arrays.asList(new NewTopic(KafkaConfig.TOPIC_OUTPUT, 6, (short) 1),
                                             new NewTopic(KafkaConfig.TOPIC_PURCHASES, 6, (short) 1)))
          .all()
          .get();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (ExecutionException e) {
      e.printStackTrace();
    }
  }

  public static Producer<Object, String> getKafkaProducer() {
    return kafkaProducer;
  }
}

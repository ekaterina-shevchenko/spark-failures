package de.tum.spark.failures.streaming;

import de.tum.spark.failures.common.domain.Purchase;
import de.tum.spark.failures.streaming.config.KafkaConfig;
import de.tum.spark.failures.streaming.config.StreamingConfig;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import scala.Tuple2;

import java.util.Collections;

@Slf4j
public class Application {

  private static final String checkpointDirectory = "s3a://spark-failures-checkpoints/checkpoints";
  private static final String topic = KafkaConfig.TOPIC_PURCHASES;

  public static void main(String[] args) throws InterruptedException {
    JavaStreamingContext streamingContext =
        JavaStreamingContext.getOrCreate(
            checkpointDirectory,
            () -> {
              JavaStreamingContext sc =
                  new JavaStreamingContext(
                      StreamingConfig.SPARK_MASTER,
                      "streaming-app-purchase-count",
                      StreamingConfig.BATCH_DURATION);
              sc.checkpoint(checkpointDirectory);
              JavaInputDStream<ConsumerRecord<String, String>> directStream =
                  KafkaUtils.createDirectStream(
                      sc,
                      LocationStrategies.PreferConsistent(),
                      ConsumerStrategies.Subscribe(
                          Collections.singletonList(topic),
                          KafkaConfig.initKafkaConsumerParameters()));
              directStream
                  .mapToPair(
                      record -> {
                        Purchase purchase =
                            ObjectMapperFactory.getObjectMapper()
                                .readValue(record.value(), Purchase.class);
                        purchase.setSparkIngestionTime(System.currentTimeMillis());
                        purchase.setKafkaIngestionTime(record.timestamp());
                        long l = System.currentTimeMillis();
                        Output o =
                            new Output(
                                1L,
                                (long) purchase.getNumber(),
                                record.timestamp(),
                                record.timestamp(),
                                l,
                                l);
                        return new Tuple2<>(record.key(), o);
                      })
                  .reduceByKey(
                      (o1, o2) ->
                          new Output(
                              o1.totalCount + o2.totalCount,
                              o1.totalNumber + o2.totalNumber,
                              Long.min(o1.minKafkaTime, o2.minKafkaTime),
                              Long.max(o1.maxKafkaTime, o2.maxKafkaTime),
                              Long.min(o1.minSparkTime, o2.minSparkTime),
                              Long.max(o1.maxSparkTime, o2.maxSparkTime)))
                  .foreachRDD(
                      rdd -> {
                        rdd.foreachPartition(
                            p -> {
                              while (p.hasNext()) {
                                Tuple2<String, Output> next = p.next();
                                String product = next._1;
                                Output output = next._2;
                                Map<String, Object> message = new HashMap<>();
                                message.put("product", product);
                                message.put("minWindowSparkTime", output.minSparkTime);
                                message.put("minWindowKafkaTime", output.minKafkaTime);
                                message.put("maxWindowKafkaTime", output.maxKafkaTime);
                                message.put("maxWindowSparkTime", output.maxSparkTime);
                                message.put("totalNumber", output.totalNumber);
                                message.put("totalCount", output.totalCount);
                                String valueAsString =
                                    ObjectMapperFactory.getObjectMapper()
                                        .writeValueAsString(message);
                                ProducerRecord<Object, String> record =
                                    new ProducerRecord<>(
                                        KafkaConfig.TOPIC_OUTPUT, product, valueAsString);
                                KafkaProducerFactory.getKafkaProducer().send(record);
                              }
                            });
                      });
              return sc;
            });

    streamingContext.start();
    streamingContext.awaitTermination();
  }

  @Data
  @AllArgsConstructor
  static class Output implements Serializable {

    private Long totalCount;
    private Long totalNumber;
    private Long minKafkaTime;
    private Long maxKafkaTime;
    private Long minSparkTime;
    private Long maxSparkTime;
  }
}

package de.tum.spark.failures.streaming;

import de.tum.spark.failures.common.domain.Purchase;
import de.tum.spark.failures.streaming.config.KafkaConfig;
import de.tum.spark.failures.streaming.config.StreamingConfig;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import scala.Tuple2;
import scala.Tuple3;

import java.util.Collections;

@Slf4j
public class Application {

    private static final String topic = KafkaConfig.TOPIC_PURCHASES;

    public static void main(String[] args) throws InterruptedException {
        JavaStreamingContext streamingContext = new JavaStreamingContext(
                StreamingConfig.SPARK_MASTER,
                "streaming-app-purchase-count",
                StreamingConfig.BATCH_DURATION);
        JavaInputDStream<ConsumerRecord<String, String>> directStream =
                KafkaUtils.createDirectStream(
                        streamingContext,
                        LocationStrategies.PreferConsistent(),
                        ConsumerStrategies.Subscribe(
                                Collections.singletonList(topic),
                                KafkaConfig.initKafkaConsumerParameters()
                        )
                );
        JavaPairDStream<String, Tuple3<Integer, Long, Long>> pairDStream = directStream.map(record -> {
            Purchase purchase =
                    ObjectMapperFactory.getObjectMapper().readValue(record.value(), Purchase.class);
            purchase.setSparkIngestionTime(System.currentTimeMillis());
            purchase.setKafkaIngestionTime(record.timestamp());
            return purchase;
        }).mapToPair(record -> new Tuple2<>(record.getProduct(),
                new Tuple3<>(record.getNumber(), record.getSparkIngestionTime(), record.getKafkaIngestionTime()))).cache();

        pairDStream.foreachRDD(rdd -> {
            if (!rdd.isEmpty()) {
                Long sparkTime = rdd.first()._2._2();
                Long kafkaTime = rdd.first()._2._3();
                rdd.reduceByKey((t1, t2) -> new Tuple3<>(t1._1() + t2._1(), Math.min(t1._2(), t2._2()), Math.min(t1._3(), t2._3())))
                        .foreach(t -> {
                            Map<String, Object> message = new HashMap<>();
                            message.put("windowFirstSparkIngestion", sparkTime);
                            message.put("windowFirstKafkaIngestion", kafkaTime);
                            message.put("product", t._1);
                            message.put("totalNumber", t._2._1());
                            String valueAsString = ObjectMapperFactory.getObjectMapper().writeValueAsString(message);
                            ProducerRecord<Object, String> record = new ProducerRecord<>(KafkaConfig.TOPIC_OUTPUT, valueAsString);
                            KafkaProducerFactory.getKafkaProducer().send(record);
                        });
            }
        });
        streamingContext.start();
        streamingContext.awaitTermination();
    }

    public static void sendMessage(JavaPairRDD<String, Tuple2<Integer, Long>> rdd) {
        Long windowFirstTime = rdd.values().collect().stream().min((t1,t2) -> Long.compare(t1._2,t2._2)).get()._2;
        rdd.foreach(t -> {
            Map<String, Object> message = new HashMap<>();
            message.put("windowFirst", windowFirstTime);
            message.put("product", t._1);
            message.put("totalNumber", t._2);
            ProducerRecord<Object, Map<String, Object>> record =
                    new ProducerRecord<>(KafkaConfig.TOPIC_OUTPUT, message);
            //KafkaProducerFactory.getKafkaProducer().send(record);
        });
    }

}
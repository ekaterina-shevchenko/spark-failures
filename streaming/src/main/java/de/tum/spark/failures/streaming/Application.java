package de.tum.spark.failures.streaming;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.spark.failures.common.domain.Purchase;
import de.tum.spark.failures.streaming.config.KafkaConfig;
import de.tum.spark.failures.streaming.config.StreamingConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import scala.Tuple2;

import java.util.Collections;

@Slf4j
public class Application {

    private static final String topic = KafkaConfig.TOPIC_PURCHASES;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Restarted");
        JavaStreamingContext streamingContext = new JavaStreamingContext(
                StreamingConfig.SPARK_MASTER,
                "streaming-app-purchase-count",
                StreamingConfig.BATCH_DURATION);
        ObjectMapper recordMapper = new ObjectMapper();
        JavaInputDStream<ConsumerRecord<String, String>> directStream =
            KafkaUtils.createDirectStream(
                streamingContext,
                LocationStrategies.PreferConsistent(),
                ConsumerStrategies.Subscribe(
                    Collections.singletonList(topic),
                    KafkaConfig.initKafkaConsumerParameters()
                )
            );
        directStream.map(record -> recordMapper.readValue(record.value(), Purchase.class))
            .mapToPair(record -> new Tuple2<>(record.getProduct(), record.getNumber()))
            .reduceByKey(Integer::sum)
            .foreachRDD(Application::sendMessage);
        streamingContext.start();
        streamingContext.awaitTermination();
    }

    private static void sendMessage(JavaPairRDD<String, Integer> rdd) {
            rdd.foreachPartition(partition -> {
                Producer<String, Integer> producer = KafkaProducerFactory.getKafkaProducer();
                partition.forEachRemaining(pair -> {
                ProducerRecord<String, Integer> record = new ProducerRecord<>(KafkaConfig.TOPIC_OUTPUT, pair._1, pair._2);
                producer.send(record);
                });
            });
    }

}

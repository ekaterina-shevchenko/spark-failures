package de.tum.spark.failures.streaming;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.spark.failures.common.domain.Purchase;
import de.tum.spark.failures.streaming.config.KafkaConfig;
import de.tum.spark.failures.streaming.config.StreamingConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Application {

    private static final String topic = KafkaConfig.TOPIC_PURCHASES;

    public static void main(String[] args) throws InterruptedException {
        JavaStreamingContext streamingContext = new JavaStreamingContext(
                StreamingConfig.SPARK_MASTER,
                "streaming-application",
                StreamingConfig.BATCH_DURATION);

        List<JavaInputDStream<ConsumerRecord<String, String>>> streams = new ArrayList<>();
        for (int i = 0; i < StreamingConfig.INPUT_DSTREAMS; i++) {
            streams.add(
                    KafkaUtils.createDirectStream(
                            streamingContext,
                            LocationStrategies.PreferConsistent(),
                            ConsumerStrategies.Subscribe(
                                    Collections.singletonList(topic),
                                    KafkaConfig.initKafkaParameters())));
        }
        ObjectMapper recordMapper = new ObjectMapper();
        streams.forEach(s -> s
                .map(record -> recordMapper.readValue(record.value(), Purchase.class))
                .window(StreamingConfig.WINDOW, StreamingConfig.SLIDE)
                .map(record -> new Tuple2<>(record.getProduct(), record.getNumber()))
                .reduce((pair1, pair2) -> new Tuple2<>(pair1._1, pair1._2 + pair1._2))
                .foreachRDD(Application::sendMessage));
        streamingContext.start();
        streamingContext.awaitTermination();
    }

    private static void sendMessage(JavaRDD<Tuple2<String, Integer>> rdd) {
            Producer<String, Integer> producer = KafkaProducerFactory.getKafkaProducer();
            rdd.foreach(pair -> { // TODO: send rdds by partitions, not as a whole
                ProducerRecord<String, Integer> record = new ProducerRecord<>(topic, pair._1, pair._2);
                producer.send(record);
            });
    }

}

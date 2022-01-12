package de.tum.spark.failures.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.ConsumerGroupMetadata;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.ProducerFencedException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

@RequiredArgsConstructor
public class KafkaJsonProducer<V> implements Producer<String, V> {
    private final KafkaProducer<String, String> nestedProducer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void initTransactions() {
        nestedProducer.initTransactions();
    }

    @Override
    public void beginTransaction() throws ProducerFencedException {
        nestedProducer.beginTransaction();
    }

    @Override
    public void sendOffsetsToTransaction(Map<TopicPartition, OffsetAndMetadata> map, String s) throws ProducerFencedException {
        nestedProducer.sendOffsetsToTransaction(map, s);
    }

    @Override
    public void sendOffsetsToTransaction(Map<TopicPartition, OffsetAndMetadata> map, ConsumerGroupMetadata consumerGroupMetadata) throws ProducerFencedException {
        nestedProducer.sendOffsetsToTransaction(map, consumerGroupMetadata);
    }

    @Override
    public void commitTransaction() throws ProducerFencedException {
        nestedProducer.commitTransaction();
    }

    @Override
    public void abortTransaction() throws ProducerFencedException {
        nestedProducer.abortTransaction();
    }

    @SneakyThrows
    @Override
    public Future<RecordMetadata> send(ProducerRecord<String, V> producerRecord) {
        String value = objectMapper.writeValueAsString(producerRecord.value());
        return nestedProducer.send(new ProducerRecord<>(producerRecord.topic(), producerRecord.key(), value));
    }

    @SneakyThrows
    @Override
    public Future<RecordMetadata> send(ProducerRecord<String, V> producerRecord, Callback callback) {
        String value = objectMapper.writeValueAsString(producerRecord.value());
        return nestedProducer.send(new ProducerRecord<>(producerRecord.topic(), producerRecord.key(), value), callback);
    }

    @Override
    public void flush() {
        nestedProducer.flush();
    }

    @Override
    public List<PartitionInfo> partitionsFor(java.lang.String s) {
        return nestedProducer.partitionsFor(s);
    }

    @Override
    public Map<MetricName, ? extends Metric> metrics() {
        return nestedProducer.metrics();
    }

    @Override
    public void close() {
        nestedProducer.close();
    }

    @Override
    public void close(Duration duration) {
        nestedProducer.close(duration);
    }
}

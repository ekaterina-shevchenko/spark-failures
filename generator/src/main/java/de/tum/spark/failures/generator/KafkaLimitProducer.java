package de.tum.spark.failures.generator;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerGroupMetadata;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.Callback;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@RequiredArgsConstructor
public class KafkaLimitProducer<K, V> implements Producer<K, V> {
    @Setter
    private volatile Integer limit;
    private final ConcurrentMap<String, AtomicInteger> counterMap = new ConcurrentHashMap<>();
    private final AtomicLong second = new AtomicLong(System.currentTimeMillis() / 1000);
    private final Producer<K,V> nestedProducer;

    public KafkaLimitProducer(Integer limit, Producer<K, V> nestedProducer) {
        this.limit = limit;
        this.nestedProducer = nestedProducer;
    }

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

    @Override
    public Future<RecordMetadata> send(ProducerRecord<K, V> producerRecord) {
        long currentSecond = System.currentTimeMillis() / 1000;
        long epochSecond = second.get();
        if (epochSecond < currentSecond) {
            if (second.compareAndSet(epochSecond, currentSecond)) {
                log.info("Epoch with counter {} has been finished", counterMap);
                for (AtomicInteger value : counterMap.values()) {
                    value.set(0);
                }
            }
        }
        String topic = producerRecord.topic();
        if (!counterMap.containsKey(topic)) {
            counterMap.put(topic,  new AtomicInteger());
        }
        AtomicInteger topicCounter = counterMap.get(topic);
        while(true) {
            int val = topicCounter.get();
            if (val < limit) {
                if (topicCounter.compareAndSet(val, val + 1)) {
                    return nestedProducer.send(producerRecord);
                }
            } else {
                throw new IllegalStateException();
            }
        }
    }

    @Override
    public Future<RecordMetadata> send(ProducerRecord<K, V> producerRecord, Callback callback) {
        return nestedProducer.send(producerRecord, callback);
    }

    @Override
    public void flush() {
        nestedProducer.flush();
    }

    @Override
    public List<PartitionInfo> partitionsFor(String s) {
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

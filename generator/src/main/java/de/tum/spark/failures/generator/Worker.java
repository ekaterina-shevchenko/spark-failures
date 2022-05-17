package de.tum.spark.failures.generator;

import de.tum.spark.failures.common.domain.Event;
import de.tum.spark.failures.generator.generators.Generator;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

@RequiredArgsConstructor
public class Worker<T extends Generator<? extends Event>> implements Runnable {
    private final String topic;
    private final T generator;
    private final KafkaLimitProducer<String, Event> producer;
    private final AtomicLong counter = new AtomicLong();

    @Override
    public void run() {
        try {
            for (int i = 1; i < 9; i++) {
                producer.setLimit(250_000 * i);
                while (counter.get() < 30_000_000 * i) {
                    try {
                        Event event = generator.generate();
                        ProducerRecord<String, Event> record = new ProducerRecord<>(topic, event.getKey(), event);
                        producer.send(record);
                        counter.incrementAndGet();
                    } catch (IllegalStateException ignored) {

                    }
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                }
                Thread.sleep(7 * 60 * 1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

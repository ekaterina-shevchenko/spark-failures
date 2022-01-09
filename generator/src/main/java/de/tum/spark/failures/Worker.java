package de.tum.spark.failures;

import de.tum.spark.failures.domain.Event;
import de.tum.spark.failures.generator.Generator;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.UUID;

@RequiredArgsConstructor
public class Worker<T extends Generator<? extends Event>> implements Runnable {
    private final String topic;
    private final T generator;
    private final Producer<String, Event> producer;

    @Override
    public void run() {
        while (true) {
            try {
                Event event = generator.generate();
                ProducerRecord<String, Event> record = new ProducerRecord<>(topic, UUID.randomUUID().toString(), event);
                producer.send(record);
            } catch (IllegalStateException ignored) {

            }
        }
    }
}

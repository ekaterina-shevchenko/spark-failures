package de.tum.spark.failures;

import de.tum.spark.failures.domain.Event;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.Producer;

@RequiredArgsConstructor
public class KafkaFlusher implements Runnable{
    private final Producer<String, Event> kafkaProducer;

    @Override
    public void run() {
        try {
            while (true) {
                kafkaProducer.flush();
                Thread.sleep(10);
            }
        } catch (InterruptedException ignored){

        }
    }
}

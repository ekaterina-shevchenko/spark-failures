package de.tum.structured.processors;

import de.tum.Application;
import de.tum.common.Purchase;
import de.tum.common.StreamingOutput;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.ProcessorSupplier;
import org.apache.kafka.streams.processor.api.Record;

import static de.tum.Application.KAFKA_PARTITIONS;

public class CountersStreamingIncrementProcessor implements ProcessorSupplier<String, Purchase, Void, Void> {
  private static final ConcurrentMap<Processor, Boolean> stoppedSet = new ConcurrentHashMap<>();
  private static final AtomicBoolean nextStep = new AtomicBoolean(false);
  private final Runnable asyncStopCommand;

  public CountersStreamingIncrementProcessor(Runnable asyncStopCommand) {
    this.asyncStopCommand = asyncStopCommand;
  }

  @Override
  public Processor<String, Purchase, Void, Void> get() {
    return new Processor<String, Purchase, Void, Void>() {
      AtomicLong lastTimestamp = new AtomicLong();
      ProcessorContext<Void, Void> context;

      @Override
      public void init(ProcessorContext<Void, Void> context) {
        Processor.super.init(context);
        this.context = context;
        context.schedule(
            Duration.ofMillis(1000),
            PunctuationType.WALL_CLOCK_TIME,
            ts -> {
              long l = lastTimestamp.get();
              if (l != 0 && ts - l > 15000) {
                stoppedSet.put(this, true);
                if (stoppedSet.size() == KAFKA_PARTITIONS) {
                  if (nextStep.compareAndSet(false, true)) {
                    System.out.println("Stopping stream, reached end");
                    asyncStopCommand.run();
                  }
                }
              }
            });
      }

      @Override
      public void process(Record<String, Purchase> record) {
        long offset = context.recordMetadata().get().offset();
        Application.purchases.incrementAndGet();
        lastTimestamp.set(System.currentTimeMillis());
        String product = record.value().getProduct();
        boolean good = false;
        for (StreamingOutput timeRange : Application.streamingCounters.keySet()) {
          long from = timeRange.getMinWindowOffset();
          long to = timeRange.getMaxWindowOffset();
          if (from <= offset && to >= offset && timeRange.getProduct().equals(product)) {
            Application.streamingCounters.get(timeRange).incrementAndGet();
            good = true;
            break;
          }
        }
        if (!good) {
          System.out.println("WE GOT SOMETHING BAD HERE: NO MATCH IN OUTPUT");
        }
      }
    };
  }
}

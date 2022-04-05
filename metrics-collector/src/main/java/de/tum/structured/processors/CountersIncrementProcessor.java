package de.tum.structured.processors;

import de.tum.Application;
import de.tum.common.Purchase;
import de.tum.structured.classes.TimeRange;
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

public class CountersIncrementProcessor implements ProcessorSupplier<String, Purchase, Void, Void> {
  private static final ConcurrentMap<Processor, Boolean> stoppedSet = new ConcurrentHashMap<>();
  private static final AtomicBoolean nextStep = new AtomicBoolean(false);
  private final Runnable asyncStopCommand;

  public CountersIncrementProcessor(Runnable asyncStopCommand) {
    this.asyncStopCommand = asyncStopCommand;
  }

  @Override
  public Processor<String, Purchase, Void, Void> get() {
    return new Processor<String, Purchase, Void, Void>() {
      AtomicLong lastTimestamp = new AtomicLong();

      @Override
      public void init(ProcessorContext<Void, Void> context) {
        Processor.super.init(context);
        context.schedule(
            Duration.ofMillis(1000),
            PunctuationType.WALL_CLOCK_TIME,
            ts -> {
              long l = lastTimestamp.get();
              if (ts - l > 10000) {
                stoppedSet.put(this, true);
                if (stoppedSet.size() == 3 && nextStep.compareAndSet(false, true)) {
                  System.out.println("Stopping stream, reached end");
                  asyncStopCommand.run();
                }
              }
            }
        );
      }

      @Override
      public void process(Record<String, Purchase> record) {
        Application.purchases.incrementAndGet();
        lastTimestamp.set(System.currentTimeMillis());
        String product = record.value().getProduct();
        long kafkaIngestionTime = record.timestamp();
        boolean good = false;
        for (TimeRange timeRange : Application.counters.keySet()) {
          long from = timeRange.from + 3 * 60 * 60 * 1000;
          long to = timeRange.to + 3 * 60 * 60 * 1000;
          if (from <= kafkaIngestionTime && to > kafkaIngestionTime && timeRange.product.equals(product)) {
            Application.counters.get(timeRange).incrementAndGet();
            good = true;
            break;
          }
        }
      }
    };
  }
}

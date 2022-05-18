package de.tum.structured.processors;

import de.tum.Application;
import de.tum.common.Output;
import de.tum.common.StreamingOutput;
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

import static de.tum.Application.KAFKA_PARTITIONS;

public class CountersStreamingEnrichProcessor implements ProcessorSupplier<String, StreamingOutput, Void, Void> {

  private static final ConcurrentMap<Processor, Boolean> stoppedSet = new ConcurrentHashMap<>();
  private static final AtomicBoolean nextStep = new AtomicBoolean(false);
  private final Runnable asyncStopCommand;

  public CountersStreamingEnrichProcessor(Runnable asyncStopCommand) {
    this.asyncStopCommand = asyncStopCommand;
  }

  @Override
  public Processor<String, StreamingOutput, Void, Void> get() {
    return new Processor<String, StreamingOutput, Void, Void>() {
      ProcessorContext<Void, Void> context;
      AtomicLong lastTimestamp = new AtomicLong();

      @Override
      public void init(ProcessorContext<Void, Void> context) {
        Processor.super.init(context);
        context.schedule(Duration.ofMillis(1000), PunctuationType.WALL_CLOCK_TIME, ts -> {
          long l = lastTimestamp.get();
          if (l != 0 && ts - l > 15000) {
            stoppedSet.put(this, true);
            if (stoppedSet.size() == KAFKA_PARTITIONS && nextStep.compareAndSet(false, true)) {
              System.out.println("Stopping stream, reached end");
              asyncStopCommand.run();
            }
          }
        });
      }

      @Override
      public void process(Record<String, StreamingOutput> record) {
        long timestamp = record.timestamp();
        lastTimestamp.set(System.currentTimeMillis());
        StreamingOutput value = record.value();
        value.setKafkaTimestamp(timestamp);
        Application.streamingCounters.putIfAbsent(value, new AtomicLong());
      }
    };
  }
}

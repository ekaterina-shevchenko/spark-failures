package de.tum;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.common.Output;
import de.tum.common.Purchase;
import de.tum.common.StreamingOutput;
import de.tum.structured.classes.TimeRange;
import de.tum.structured.processors.CountersEnrichProcessor;
import de.tum.structured.processors.CountersIncrementProcessor;
import de.tum.structured.processors.CountersStreamingEnrichProcessor;
import de.tum.structured.processors.CountersStreamingIncrementProcessor;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;

public class Application {

  public static final Integer KAFKA_PARTITIONS = 6;
  public static ConcurrentMap<TimeRange, AtomicLong> counters = new ConcurrentHashMap<>();
  public static ConcurrentMap<StreamingOutput, AtomicLong> streamingCounters = new ConcurrentHashMap<>();
  public static AtomicLong purchases = new AtomicLong();

  public static final boolean structured = System.getenv("MODE").equals("structured");
  public static final String fault = System.getenv("FAULT");
  public static final Integer testNumber = Integer.parseInt(System.getenv("TEST_NUMBER"));
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final Serde<String> stringSerde = Serdes.String();

  public static void main(String[] args) throws InterruptedException {
    StreamStopCommand streamStopCommand = new StreamStopCommand();
    Topology topology = buildOutputTopology(streamStopCommand);
    KafkaStreams ks = new KafkaStreams(topology, buildProperties());
    streamStopCommand.kafkaStreams = ks;
    streamStopCommand.nextStep = Application::purchasesStream;
    String wait = System.getProperty("wait");
    if (wait != null) {
      Thread.sleep(Long.parseLong(wait));
    }
    ks.start();
  }

  static void purchasesStream() {
    StreamStopCommand streamStopCommand = new StreamStopCommand();
    Topology topology = buildPurchasesTopology(streamStopCommand);
    KafkaStreams ks = new KafkaStreams(topology, buildProperties());
    streamStopCommand.kafkaStreams = ks;
    if (structured) {
      streamStopCommand.nextStep = Application::pushMetrics;
    } else {
      streamStopCommand.nextStep = Application::pushMetricsStreaming;
    }
    ks.start();
  }

  private static void pushMetricsStreaming() {
    String jdbcUrl = System.getProperty("jdbcUrl");
    try (Connection con = DriverManager.getConnection(
            jdbcUrl == null? "jdbc:mysql://localhost:3306/spark": jdbcUrl,
            "spark",
            "spark"
    )) {
      con.setAutoCommit(false);
      List<StreamingOutput> series = Application.streamingCounters.entrySet().stream()
              .map(Entry::getKey)
              .sorted(Comparator.comparingLong(e -> e.getMinWindowSparkTime()))
              .collect(Collectors.toList());
      for (Entry<StreamingOutput, AtomicLong> entry : Application.streamingCounters.entrySet()) {
        StreamingOutput key = entry.getKey();
        writeToDbValidate(con, entry.getValue().get(), key.getTotalCount(), key.getMinWindowSparkTime());
      }
      for (StreamingOutput windowOutput : series) {
        long l = (windowOutput.getEndOfProcessingTimestamp() - windowOutput.getMinWindowSparkTime()) / 1000;
        windowOutput.setAvgTotalNumberPerSecond(windowOutput.getTotalCount() / l);
      }
      // 162138435000:1456, 162138436000:1456,
      Map<Long, AtomicLong> throughput = new HashMap<>();
      for (StreamingOutput windowOutput : series) {
        long outputTimestamp = windowOutput.getEndOfProcessingTimestamp();
        long minSparkIngestionTimestamp = windowOutput.getMinWindowSparkTime();
        for (long i = minSparkIngestionTimestamp / 1000; i < outputTimestamp / 1000; i++) {
          throughput.putIfAbsent(i, new AtomicLong());
          throughput.get(i).addAndGet(windowOutput.getAvgTotalNumberPerSecond());
        }
      }
      for (Entry<Long, AtomicLong> longAtomicLongEntry : throughput.entrySet()) {
        writeToDbThroughput(
                con,
                longAtomicLongEntry.getKey(),
                longAtomicLongEntry.getValue().get()
        );
      }
      for (StreamingOutput windowOutput : series) {
        long endToEndLatency = windowOutput.getKafkaTimestamp() - windowOutput.getMinWindowKafkaTime();
        long processingLatency = windowOutput.getKafkaTimestamp() - windowOutput.getMinWindowSparkTime();
        writeToDbLatency(con, windowOutput.getMinWindowKafkaTime(), endToEndLatency, processingLatency);
      }
      con.commit();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private static void pushMetrics() {
    String jdbcUrl = System.getProperty("jdbcUrl");
    try (Connection con = DriverManager.getConnection(
        jdbcUrl == null? "jdbc:mysql://localhost:3306/spark": jdbcUrl,
        "spark",
        "spark"
    )) {
      con.setAutoCommit(false);
      List<TimeRange> series = Application.counters.entrySet().stream()
          .map(Entry::getKey)
          .sorted(Comparator.comparingLong(e -> e.from))
          .collect(Collectors.toList());
      List<Entry<TimeRange, AtomicLong>> sortedCounterEntries = counters.entrySet().stream()
              .sorted(Comparator.comparingLong(e -> e.getKey().from))
              .collect(Collectors.toList());
      long from = sortedCounterEntries.get(0).getKey().from;
      int totalCount = 0;
      int counted = 0;
      for (int i = 1; i < sortedCounterEntries.size(); i++) { if (from == sortedCounterEntries.get(i).getKey().from) {
          totalCount += sortedCounterEntries.get(i).getKey().totalCount;
          counted += sortedCounterEntries.get(i).getValue().get();
        } else {
          writeToDbValidate(con, counted, totalCount, from);
          counted = 0;
          totalCount = 0;
          from = sortedCounterEntries.get(i).getKey().from;
        }
      }
      writeToDbValidate(con, counted, totalCount, from);
      for (TimeRange timeRange : series) {
        // 10000 / 3 seconds = throughput 3333 records/sec
        long l = (timeRange.outputTimestamp - timeRange.minSparkIngestionTimestamp) / 1000;
        timeRange.avgTotalNumberPerSecond = timeRange.totalCount / l;
      }
      // 162138435000:1456, 162138436000:1456,
      Map<Long, AtomicLong> throughput = new HashMap<>();
      for (TimeRange timeRange : series) {
        long outputTimestamp = timeRange.outputTimestamp;
        long minSparkIngestionTimestamp = timeRange.minSparkIngestionTimestamp;
        for (long i = minSparkIngestionTimestamp / 1000; i < outputTimestamp / 1000; i++) {
          throughput.putIfAbsent(i, new AtomicLong());
          throughput.get(i).addAndGet(timeRange.avgTotalNumberPerSecond);
        }
      }
      for (Entry<Long, AtomicLong> longAtomicLongEntry : throughput.entrySet()) {
        writeToDbThroughput(con, longAtomicLongEntry.getKey(), longAtomicLongEntry.getValue().get());
      }
      for (TimeRange timeRange : series) {
        long endToEndLatency = timeRange.kafkaTimestamp - timeRange.minKafkaIngestionTimestamp;
        long processingLatency = timeRange.kafkaTimestamp - timeRange.minSparkIngestionTimestamp;
        writeToDbLatency(con, timeRange.minKafkaIngestionTimestamp, endToEndLatency, processingLatency);
      }
      con.commit();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private static void writeToDbValidate(Connection connection, long purchasesCount, long totalCount, long timestamp)
          throws SQLException {
    Statement statement = connection.createStatement();
    String st = MessageFormat.format(
            "insert into spark_validate(purchases_number, output_number, timestamp, job, fault, test_number)" +
                    " value ({0},{1},from_unixtime({2}),\"{3}\",\"{4}\",{5})",
            String.valueOf(purchasesCount),
            String.valueOf(totalCount),
            String.valueOf(timestamp / 1000),
            structured? "structured-streaming": "streaming", fault, String.valueOf(testNumber)
    );
    statement.execute(st);
    statement.close();
  }

  private static void writeToDbThroughput(Connection connection, long timestamp, long throughput)
      throws SQLException {
    Statement statement = connection.createStatement();
    String st =
        MessageFormat.format(
            "insert into spark_throughput(throughput, timestamp, job, fault, test_number)"
                + " value ({0},from_unixtime({1}),\"{2}\",\"{3}\",{4})",
            String.valueOf(throughput),
            String.valueOf(timestamp),
            structured ? "structured-streaming" : "streaming",
            fault,
            String.valueOf(testNumber));
    statement.execute(st);
    statement.close();
  }

  private static void writeToDbLatency(Connection connection,
                                       long timestamp,
                                       long endToEndLatency,
                                       long processingLatency) throws SQLException {
    Statement statement = connection.createStatement();
    String st =
        MessageFormat.format(
            "insert into spark_latency(end_to_end_latency, "
                + "processing_latency, output_timestamp, job, fault, test_number)"
                + " value ({0},{1},from_unixtime({2}),\"{3}\",\"{4}\",{5})",
            String.valueOf(endToEndLatency),
            String.valueOf(processingLatency),
            String.valueOf(timestamp / 1000),
            structured ? "structured-streaming" : "streaming",
            fault,
            String.valueOf(testNumber));
    statement.execute(st);
    statement.close();
  }


  static Properties buildProperties() {
    Properties properties = new Properties();
    properties.put(StreamsConfig.APPLICATION_ID_CONFIG, UUID.randomUUID().toString());
    String kafkaUrl = System.getProperty("kafkaUrl");
    properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaUrl == null ? "localhost:29092": kafkaUrl);
    return properties;
  }

  static Topology buildOutputTopology(Runnable stopCommand) {
    StreamsBuilder streamsBuilder = new StreamsBuilder();
    if (structured) {
      streamsBuilder.stream("output", Consumed.with(stringSerde, stringSerde))
          .map((k, v) -> toStructuredOutput(v))
          .process(new CountersEnrichProcessor(() -> {
            new Thread(stopCommand).start();
          }));
    } else {
      streamsBuilder.stream("output", Consumed.with(stringSerde, stringSerde))
          .map((k, v) -> toStreamingOutput(v))
          .process(new CountersStreamingEnrichProcessor(() -> {
            new Thread(stopCommand).start();
          }));
    }
    return streamsBuilder.build();
  }

  static Topology buildPurchasesTopology(Runnable stopCommand) {
    StreamsBuilder streamsBuilder = new StreamsBuilder();
    if (structured) {
      streamsBuilder.stream("purchases", Consumed.with(stringSerde, stringSerde))
          .map((k, v) -> toPurchase(v))
          .process(new CountersIncrementProcessor(() -> {
            new Thread(stopCommand).start();
          }));
    } else {
      streamsBuilder.stream("purchases", Consumed.with(stringSerde, stringSerde))
          .map((k, v) -> toPurchase(v))
          .process(new CountersStreamingIncrementProcessor(() -> {
            new Thread(stopCommand).start();
          }));
    }
    return streamsBuilder.build();
  }

  static KeyValue<String, Output> toStructuredOutput(String str) {
    try {
      Output output = objectMapper.readValue(str, Output.class);
      return new KeyValue<>(output.getProduct(), output);
    } catch (JsonProcessingException e) {
      throw new RuntimeException();
    }
  }
  static KeyValue<String, StreamingOutput> toStreamingOutput(String str) {
    try {
      StreamingOutput output = objectMapper.readValue(str, StreamingOutput.class);
      return new KeyValue<>(output.getProduct(), output);
    } catch (JsonProcessingException e) {
      throw new RuntimeException();
    }
  }

  static KeyValue<String, Purchase> toPurchase(String str) {
    try {
      Purchase purchase = objectMapper.readValue(str, Purchase.class);
      return new KeyValue<>(purchase.getProduct(), purchase);
    } catch (JsonProcessingException e) {
      throw new RuntimeException();
    }
  }

}

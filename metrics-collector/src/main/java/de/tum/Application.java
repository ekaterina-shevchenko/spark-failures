package de.tum;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.common.Output;
import de.tum.common.Purchase;
import de.tum.structured.classes.TimeRange;
import de.tum.structured.processors.CountersEnrichProcessor;
import de.tum.structured.processors.CountersIncrementProcessor;
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

  public static ConcurrentMap<TimeRange, AtomicLong> counters = new ConcurrentHashMap<>();
  public static AtomicLong purchases = new AtomicLong();

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
    streamStopCommand.nextStep = Application::pushMetrics;
    ks.start();
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
      for (Entry<TimeRange, AtomicLong> entry : sortedCounterEntries) {
        AtomicLong purchasesCount = entry.getValue();
        long totalCount = entry.getKey().totalCount;
        writeToDbValidate(con, purchasesCount.get(), totalCount);
      }
      for (TimeRange timeRange : series) {
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
        writeToDbThroughput(
            con,
            longAtomicLongEntry.getKey(),
            longAtomicLongEntry.getValue().get()
        );
      }
      for (TimeRange timeRange : series) {
        long endToEndLatency = timeRange.outputTimestamp - timeRange.minKafkaIngestionTimestamp;
        long processingLatency = timeRange.outputTimestamp - timeRange.minSparkIngestionTimestamp;
        writeToDbLatency(con, timeRange.minKafkaIngestionTimestamp, endToEndLatency, processingLatency);
      }
      con.commit();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private static void writeToDbValidate(Connection connection, long purchasesCount, long totalCount)
          throws SQLException {
    Statement statement = connection.createStatement();
    String st = MessageFormat.format(
            "insert into spark_structured_streaming_validate(purchases_number, output_number)" +
                    " value ({0},{1})",
            String.valueOf(purchasesCount),
            String.valueOf(totalCount)
    );
    statement.execute(st);
    statement.close();
  }

  private static void writeToDbThroughput(Connection connection, long timestamp, long throughput)
      throws SQLException {
    Statement statement = connection.createStatement();
    String st = MessageFormat.format(
        "insert into spark_structured_streaming_throughput(throughput, timestamp)" +
        " value ({0},from_unixtime({1}))",
        String.valueOf(throughput),
        String.valueOf(timestamp)
    );
    statement.execute(st);
    statement.close();
  }

  private static void writeToDbLatency(Connection connection,
                                       long timestamp,
                                       long endToEndLatency,
                                       long processingLatency) throws SQLException {
    Statement statement = connection.createStatement();
    String st = MessageFormat.format(
        "insert into spark_structured_streaming_latency(end_to_end_latency, " +
        "processing_latency, output_timestamp)" +
        " value ({0},{1},from_unixtime({2}))",
        String.valueOf(endToEndLatency), String.valueOf(processingLatency), String.valueOf(timestamp / 1000)
    );
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
    streamsBuilder.stream("output", Consumed.with(stringSerde, stringSerde))
        .map((k, v) -> toOutput(v))
        .process(new CountersEnrichProcessor(() -> {
          new Thread(stopCommand).start();
        }));
    return streamsBuilder.build();
  }

  static Topology buildPurchasesTopology(Runnable stopCommand) {
    StreamsBuilder streamsBuilder = new StreamsBuilder();
    streamsBuilder.stream("purchases", Consumed.with(stringSerde, stringSerde))
        .map((k, v) -> toPurchase(v))
        .process(new CountersIncrementProcessor(() -> {
          new Thread(stopCommand).start();
        }));
    return streamsBuilder.build();
  }

  static KeyValue<String, Output> toOutput(String str) {
    try {
      Output output = objectMapper.readValue(str, Output.class);
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

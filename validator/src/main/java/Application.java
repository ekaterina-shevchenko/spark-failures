import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import config.KafkaConfig;
import de.tum.spark.failures.common.domain.Purchase;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;

import javax.naming.Binding;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class Application {

    @SneakyThrows
    public static void main(String[] args) {
        Properties streamsConfiguration = getProperties();
        StreamsBuilder builder = new StreamsBuilder();
        ObjectMapper mapper = new ObjectMapper();
        KStream<String, String> stream = builder.stream(KafkaConfig.TOPIC_PURCHASES, Consumed.with(Serdes.String(), Serdes.String()));
        final Map<String, AtomicInteger> totalAmount = new HashMap<>();
        stream.mapValues(value -> {
            try {
                return mapper.readValue(value, Purchase.class);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return new Purchase();
        })
                .map((key, value) -> new KeyValue<>(value.getProduct(), value.getNumber()))
                .filter((key, value) -> !key.equals(""))
                .foreach((k,v) -> {
                    totalAmount.putIfAbsent(k, new AtomicInteger(0));
                    totalAmount.get(k).addAndGet(v);
                });

        KafkaStreams streams = new KafkaStreams(builder.build(), streamsConfiguration);
        streams.start();

        Thread.sleep(50000);

        streams.close();
        log.info("Total: {}", totalAmount);

    }

    @SneakyThrows
    private static Properties getProperties() {
        Properties streamsConfiguration = new Properties();
        streamsConfiguration.put(
                StreamsConfig.APPLICATION_ID_CONFIG,
                "validator2");
        streamsConfiguration.put(
                StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,
                KafkaConfig.BOOTSTRAP_KAFKA_SERVER);
        streamsConfiguration.put(
                StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,
                Serdes.Integer().getClass().getName());
        Path stateDirectory = Files.createTempDirectory("kafka-streams");
        streamsConfiguration.put(
                StreamsConfig.STATE_DIR_CONFIG, stateDirectory.toAbsolutePath().toString());
        streamsConfiguration.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG,
                StreamsConfig.EXACTLY_ONCE_V2);
        return streamsConfiguration;
    }
}

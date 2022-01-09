package de.tum.spark.failures;

import de.tum.spark.failures.config.GeneratorConfig;
import de.tum.spark.failures.domain.Event;
import de.tum.spark.failures.domain.Product;
import de.tum.spark.failures.domain.User;
import de.tum.spark.failures.generator.AdvertisementGenerator;
import de.tum.spark.failures.generator.PurchaseGenerator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Application {

    public static void main(String[] args) throws IOException {
        List<User> users = readUsers();
        List<Product> products = readProducts();

        AdminClient kafkaAdmin = initKafkaAdmin();
        kafkaAdmin.createTopics(
                Stream.of(
                        new NewTopic(GeneratorConfig.TOPIC_ADS, 3, (short) 1),
                        new NewTopic(GeneratorConfig.TOPIC_PURCHASES, 3, (short) 1)
                ).collect(Collectors.toSet()));
        kafkaAdmin.close();
        Producer<String, Event> kafkaProducer = initKafkaProducer();

        AdvertisementGenerator advertisementGenerator = new AdvertisementGenerator(users, products);
        Worker<AdvertisementGenerator> worker1 = new Worker<>(GeneratorConfig.TOPIC_ADS, advertisementGenerator, kafkaProducer);

        PurchaseGenerator purchaseGenerator = new PurchaseGenerator(users, products);
        Worker<PurchaseGenerator> worker2 = new Worker<>(GeneratorConfig.TOPIC_PURCHASES, purchaseGenerator, kafkaProducer);

        KafkaFlusher kafkaFlusher = new KafkaFlusher(kafkaProducer);

        Thread thread1_1 = new Thread(worker1);
        Thread thread2_1 = new Thread(worker2);
        Thread thread3 = new Thread(kafkaFlusher);
        thread1_1.start();
        thread2_1.start();
        thread3.start();
    }

    private static List<User> readUsers() throws IOException {
        InputStream resourceAsStream = Application.class.getResourceAsStream("/users.csv");
        Reader reader = new InputStreamReader(resourceAsStream);
        CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT);
        return parser.stream()
                .skip(1) // skip header
                .map(r -> new User(Integer.parseInt(r.get(0)), r.get(1), r.get(2)))
                .collect(Collectors.toList());
    }

    private static List<Product> readProducts() throws IOException {
        InputStream resourceAsStream = Application.class.getResourceAsStream("/products.csv");
        Reader reader = new InputStreamReader(resourceAsStream);
        CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT);
        return parser.stream()
                .skip(1) // skip header
                .map(r -> new Product(r.get(0)))
                .collect(Collectors.toList());
    }

    private static Producer<String, Event> initKafkaProducer() {
        Map<String, Object> props = new HashMap<>();
        props.put("bootstrap.servers", GeneratorConfig.BOOTSTRAP_KAFKA_SERVER);
        props.put("acks", "all");
        props.put("retries", 10);
        props.put("buffer.memory", 100_000_000);
        props.put("batch.size", 100_000);
        props.put("compression.type", "gzip");
        props.put("max.request.size", 5_000_000);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(props);
        KafkaJsonProducer<Event> jsonProducer = new KafkaJsonProducer<>(kafkaProducer);
        return new KafkaLimitProducer<>(GeneratorConfig.THROUGHPUT, jsonProducer);
    }

    private static AdminClient initKafkaAdmin() {
        Map<String, Object> props = new HashMap<>();
        props.put("bootstrap.servers", GeneratorConfig.BOOTSTRAP_KAFKA_SERVER);
        return KafkaAdminClient.create(props);
    }

}

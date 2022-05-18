package de.tum.spark.failures.generator;

import de.tum.spark.failures.common.domain.Event;
import de.tum.spark.failures.generator.config.GeneratorConfig;
import de.tum.spark.failures.generator.domain.Product;
import de.tum.spark.failures.generator.domain.User;
import de.tum.spark.failures.generator.generators.PurchaseGenerator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;

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

    public static void main(String[] args) throws IOException, InterruptedException {
        List<User> users = readUsers();
        List<Product> products = readProducts();

        AdminClient kafkaAdmin = initKafkaAdmin();
        CreateTopicsResult result = kafkaAdmin.createTopics(
                Stream.of(
                        new NewTopic(GeneratorConfig.TOPIC_PURCHASES, 6, (short) 1),
                        new NewTopic(GeneratorConfig.TOPIC_OUTPUT, 6, (short) 1)
                ).collect(Collectors.toSet()));
        try {
            result.all().get();
        } catch (Exception e) {

        }
        kafkaAdmin.close();
        KafkaLimitProducer<String, Event> kafkaProducer = initKafkaProducer();

        PurchaseGenerator purchaseGenerator = new PurchaseGenerator(users, products);
        Worker<PurchaseGenerator> worker2 = new Worker<>(GeneratorConfig.TOPIC_PURCHASES, purchaseGenerator, kafkaProducer);

        Thread thread = new Thread(worker2);
        Thread.sleep(180000); // Gives time to spark to initialize and start job execution
        thread.start();
        thread.join();
        kafkaProducer.flush();
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

    private static KafkaLimitProducer<String, Event> initKafkaProducer() {
        Map<String, Object> props = new HashMap<>();
        props.put("bootstrap.servers", GeneratorConfig.BOOTSTRAP_KAFKA_SERVER);
        props.put("acks", "all");
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

package de.tum.spark.failures.generator.generators;

import de.tum.spark.failures.common.domain.Event;
import de.tum.spark.failures.generator.domain.Product;
import de.tum.spark.failures.generator.domain.User;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Random;

@RequiredArgsConstructor
public abstract class Generator<T extends Event> {
    protected final Random random = new Random();
    private final List<User> users;
    private final List<Product> products;

    public T generate() {
        User randomUser = users.get(random.nextInt(users.size()));
        Product randomProduct = products.get(random.nextInt(products.size()));
        return generateInstance(randomUser, randomProduct);
    }

    protected abstract T generateInstance(User user, Product product);

}

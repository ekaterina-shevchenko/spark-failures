package de.tum.spark.failures.generator.generators;

import de.tum.spark.failures.common.domain.Purchase;
import de.tum.spark.failures.generator.domain.Product;
import de.tum.spark.failures.generator.domain.User;

import java.util.List;

public class PurchaseGenerator extends Generator<Purchase> {

    public PurchaseGenerator(List<User> users, List<Product> products) {
        super(users, products);
    }

    @Override
    protected Purchase generateInstance(User user, Product product) {
        return new Purchase(user.getUserId(), product.getProduct(), random.nextInt(7));
    }
}

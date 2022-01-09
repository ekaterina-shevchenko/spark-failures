package de.tum.spark.failures.generator;

import de.tum.spark.failures.domain.Product;
import de.tum.spark.failures.domain.Purchase;
import de.tum.spark.failures.domain.User;

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

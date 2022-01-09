package de.tum.spark.failures.generator;

import de.tum.spark.failures.domain.Advertisement;
import de.tum.spark.failures.domain.Product;
import de.tum.spark.failures.domain.User;

import java.util.List;

public class AdvertisementGenerator extends Generator<Advertisement> {

    public AdvertisementGenerator(List<User> users, List<Product> products) {
        super(users, products);
    }

    @Override
    protected Advertisement generateInstance(User user, Product product) {
        return new Advertisement(user.getUserId(), product.getProduct());
    }
}

package de.tum.spark.failures.generator.generators;

import de.tum.spark.failures.common.domain.Advertisement;
import de.tum.spark.failures.generator.domain.Product;
import de.tum.spark.failures.generator.domain.User;

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

package de.tum.spark.failures.generator.domain;

import lombok.Data;

@Data
public class User {

    private final Integer userId;
    private final String firstName;
    private final String lastName;
}

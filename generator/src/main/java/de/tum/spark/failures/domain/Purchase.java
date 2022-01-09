package de.tum.spark.failures.domain;

import lombok.Data;

import java.util.Date;

@Data
public class Purchase implements Event{

    private final Integer userId;
    private final String product;
    private final Integer number;
    private final Long timestamp = new Date().getTime();
}

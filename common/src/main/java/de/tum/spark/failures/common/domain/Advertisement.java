package de.tum.spark.failures.common.domain;

import lombok.Data;

import java.util.Date;

@Data
public class Advertisement implements Event{

    private final Integer userId;
    private final String product;
    private final Long timestamp = new Date().getTime();
}
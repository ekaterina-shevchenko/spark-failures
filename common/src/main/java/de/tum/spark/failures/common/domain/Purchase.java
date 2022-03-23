package de.tum.spark.failures.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Purchase implements Event {

    private Integer userId;
    private String product;
    private Integer number;
    private Long timestamp;
    private Long sparkIngestionTime;
    private Long kafkaIngestionTime;

    public Purchase(Integer userId, String product, Integer number) {
        this.userId = userId;
        this.product = product;
        this.number = number;
        timestamp = new Date().getTime();
    }

}

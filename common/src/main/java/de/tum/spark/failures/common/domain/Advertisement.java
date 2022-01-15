package de.tum.spark.failures.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Advertisement implements Event{

    private Integer userId;
    private String product;
    private Long timestamp;

    public Advertisement(Integer userId, String product) {
        this.userId = userId;
        this.product = product;
        this.timestamp = new Date().getTime();
    }

}

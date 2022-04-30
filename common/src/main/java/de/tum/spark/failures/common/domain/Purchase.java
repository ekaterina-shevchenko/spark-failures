package de.tum.spark.failures.common.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
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

    @Override
    public String getKey() {
        return product;
    }
}

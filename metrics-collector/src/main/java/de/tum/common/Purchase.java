package de.tum.common;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Purchase {

  private String key;
  private Long sparkIngestionTime;
  private Long kafkaIngestionTime;
  private String userId;
  private Long timestamp;
  private String product;
  private Integer number;

}
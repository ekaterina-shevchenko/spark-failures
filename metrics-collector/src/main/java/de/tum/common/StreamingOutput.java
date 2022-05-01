package de.tum.common;

import java.text.SimpleDateFormat;
import lombok.Data;

@Data
public class StreamingOutput {
  static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS'Z'");
  private String product;
  private Integer totalNumber;
  private Integer totalCount;
  private Long minWindowKafkaTime;
  private Long maxWindowKafkaTime;
  private Long minWindowSparkTime;
  private Long maxWindowSparkTime;
  private Long endOfProcessingTimestamp;
  private Long kafkaTimestamp;
  private Long avgTotalNumberPerSecond;
  private Long minWindowOffset;
  private Long maxWindowOffset;

}

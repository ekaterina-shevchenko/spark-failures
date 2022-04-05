package de.tum.common;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class Output {
  static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS'Z'");
  private String product;
  private Integer totalCount;
  private Integer totalNumber;
  private String windowStart;
  private String windowEnd;
  private Long windowStartLong;
  private Long windowEndLong;
  private Long timestampLong;
  private String timestamp;
  private Long minimumSparkIngestionTimestampLong;
  private String minimumSparkIngestionTimestamp;
  private Long minimumKafkaIngestionTimestampLong;
  private String minimumKafkaIngestionTimestamp;

  public Output() {
  }

  public Output(String product, Integer number, Long windowStart, Long windowEnd) {
    this.product = product;
    this.totalNumber = number;
    this.windowStartLong = windowStart;
    this.windowEndLong = windowEnd;
  }

  public Long getTimestampLong() {
    return timestampLong;
  }

  public void setTimestampLong(Long timestampLong) {
    this.timestampLong = timestampLong;
  }

  public String getProduct() {
    return product;
  }

  public Integer getTotalNumber() {
    return totalNumber;
  }

  public Long getMinimumSparkIngestionTimestampLong() {
    return minimumSparkIngestionTimestampLong;
  }

  public void setMinimumSparkIngestionTimestampLong(Long minimumSparkIngestionTimestampLong) {
    this.minimumSparkIngestionTimestampLong = minimumSparkIngestionTimestampLong;
  }

  public String getMinimumSparkIngestionTimestamp() {
    return minimumSparkIngestionTimestamp;
  }

  public Long getMinimumKafkaIngestionTimestampLong() {
    return minimumKafkaIngestionTimestampLong;
  }

  public void setMinimumKafkaIngestionTimestampLong(Long minimumKafkaIngestionTimestampLong) {
    this.minimumKafkaIngestionTimestampLong = minimumKafkaIngestionTimestampLong;
  }

  public String getMinimumKafkaIngestionTimestamp() {
    return minimumKafkaIngestionTimestamp;
  }

  public Long getWindowStartLong() {
    return windowStartLong;
  }

  public Long getWindowEndLong() {
    return windowEndLong;
  }

  public Integer getTotalCount() {
    return totalCount;
  }

  public void setTotalCount(Integer totalCount) {
    this.totalCount = totalCount;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
    try {
      this.timestampLong = simpleDateFormat.parse(timestamp).getTime();
    } catch (ParseException e) {
      e.printStackTrace();
    }
  }

  public void setMinimumSparkIngestionTimestamp(String minimumSparkIngestionTimestamp) {
    this.minimumSparkIngestionTimestamp = minimumSparkIngestionTimestamp;
    try {
      this.minimumSparkIngestionTimestampLong = simpleDateFormat.parse(minimumSparkIngestionTimestamp).getTime();
    } catch (ParseException e) {
      e.printStackTrace();
    }
  }

  public void setMinimumKafkaIngestionTimestamp(String minimumKafkaIngestionTimestamp) {
    this.minimumKafkaIngestionTimestamp = minimumKafkaIngestionTimestamp;
    try {
      this.minimumKafkaIngestionTimestampLong = simpleDateFormat.parse(minimumKafkaIngestionTimestamp).getTime();
    } catch (ParseException e) {
      e.printStackTrace();
    }
  }
  public void setWindowStart(String windowStart) {
    this.windowStart = windowStart;
    try {
      this.windowStartLong = simpleDateFormat.parse(windowStart).getTime();
    } catch (ParseException e) {
      e.printStackTrace();
    }
  }

  public void setWindowEnd(String windowEnd) {
    this.windowEnd = windowEnd;
    try {
      this.windowEndLong = simpleDateFormat.parse(windowEnd).getTime();
    } catch (ParseException e) {
      e.printStackTrace();
    }
  }
}

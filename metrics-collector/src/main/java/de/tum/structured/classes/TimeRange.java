package de.tum.structured.classes;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class TimeRange {
  public String product;
  public long minSparkIngestionTimestamp;
  public long minKafkaIngestionTimestamp;
  public long outputTimestamp;
  public long kafkaTimestamp;
  public long from;
  public long to;
  public long totalCount;
  public long totalNumber;

  public long avgTotalNumberPerSecond;

  public TimeRange(String product, long minKafkaIngestionTimestamp, long minSparkIngestionTimestamp,
                   long outputTimestamp, long kafkaTimestamp, long from, long to, long totalNumber,
                   long totalCount) {
    this.product = product;
    this.minSparkIngestionTimestamp = minSparkIngestionTimestamp;
    this.minKafkaIngestionTimestamp = minKafkaIngestionTimestamp;
    this.outputTimestamp = outputTimestamp;
    this.kafkaTimestamp = kafkaTimestamp;
    this.from = from;
    this.to = to;
    this.totalNumber = totalNumber;
    this.totalCount = totalCount;
  }

}

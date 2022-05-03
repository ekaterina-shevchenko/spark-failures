package de.tum.spark.failures.streaming.config;

import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.Durations;

public class StreamingConfig {

    public static final String SPARK_MASTER = "spark://spark-master:7077";
    public static final String SPARK_DRIVER = "172.24.40.135";
    public static final Duration BATCH_DURATION = Durations.seconds(10);

}

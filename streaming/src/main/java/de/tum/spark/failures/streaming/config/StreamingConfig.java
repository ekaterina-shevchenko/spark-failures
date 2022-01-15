package de.tum.spark.failures.streaming.config;

import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.Durations;

public class StreamingConfig {

    public static final Integer INPUT_DSTREAMS = 3;
    public static final Integer THREADS_PER_DSTREAM = 1;
    public static final String SPARK_MASTER = "spark://spark-master:7077";
    public static final Duration BATCH_DURATION = Durations.seconds(1);
    public static final Duration WINDOW = Durations.seconds(5);
    public static final Duration SLIDE = Durations.seconds(5);


}

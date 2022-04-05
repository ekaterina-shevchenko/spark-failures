package de.tum;

import org.apache.kafka.streams.KafkaStreams;

public class StreamStopCommand implements Runnable{
  volatile KafkaStreams kafkaStreams;
  volatile Runnable nextStep;


  @Override
  public void run() {
    kafkaStreams.close();
    if (nextStep!= null) {
      nextStep.run();
    }
  }
}

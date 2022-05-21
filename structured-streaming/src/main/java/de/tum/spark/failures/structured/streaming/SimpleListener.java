package de.tum.spark.failures.structured.streaming;

import org.apache.spark.sql.streaming.StreamingQueryListener;

import java.util.concurrent.atomic.AtomicLong;

public class SimpleListener extends StreamingQueryListener {

    private AtomicLong numInputRows;

    @Override
    public void onQueryStarted(QueryStartedEvent event) {

    }

    @Override
    public void onQueryProgress(QueryProgressEvent event) {
        numInputRows.addAndGet(event.progress().numInputRows());
        System.out.println(numInputRows);
    }

    @Override
    public void onQueryTerminated(QueryTerminatedEvent event) {

    }
}

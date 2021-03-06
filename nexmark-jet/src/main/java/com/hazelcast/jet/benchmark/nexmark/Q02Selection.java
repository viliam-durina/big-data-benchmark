package com.hazelcast.jet.benchmark.nexmark;

import com.hazelcast.jet.benchmark.nexmark.model.Bid;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.StreamStage;

import java.util.Properties;

import static com.hazelcast.jet.benchmark.nexmark.EventSourceP.eventSource;
import static com.hazelcast.jet.datamodel.Tuple3.tuple3;

public class Q02Selection extends BenchmarkBase {

    Q02Selection() {
        super("q02-selection");
    }

    @Override
    StreamStage<Tuple2<Long, Long>> addComputation(
            Pipeline pipeline, Properties props
    ) throws ValidationException {
        int numDistinctKeys = parseIntProp(props, PROP_NUM_DISTINCT_KEYS);
        int auctionIdModulus = 128;
        int eventsPerSecond = parseIntProp(props, PROP_EVENTS_PER_SECOND);
        int sievingFactor = Math.max(1, eventsPerSecond / (8192 * auctionIdModulus));

        return pipeline
                .readFrom(eventSource(eventsPerSecond, INITIAL_SOURCE_DELAY_MILLIS, (seq, timestamp) ->
                        new Bid(seq, timestamp, seq % numDistinctKeys, getRandom(seq, 100))))
                .withNativeTimestamps(0)
                .filter(bid -> bid.auctionId() % auctionIdModulus == 0)
                .map(bid -> tuple3(bid.timestamp(), bid.auctionId(), bid.price()))

                .filter(t3 -> t3.f1() % sievingFactor == 0)
                .apply(stage -> determineLatency(stage, Tuple3::f0));
    }
}

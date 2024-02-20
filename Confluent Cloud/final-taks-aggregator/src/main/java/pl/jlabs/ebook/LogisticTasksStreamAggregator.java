package pl.jlabs.ebook;

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.jlabs.ebook.truck.AggregatedTasks;
import pl.jlabs.ebook.truck.LogisticTask;

import java.time.Duration;
import java.util.Properties;

import static org.apache.kafka.streams.kstream.Suppressed.BufferConfig.unbounded;
import static org.apache.kafka.streams.kstream.Suppressed.untilWindowCloses;
import static pl.jlabs.ebook.SerdesUtil.getSpecificAvroSerde;

public class LogisticTasksStreamAggregator {

  private static final Logger log = LoggerFactory.getLogger(LogisticTasksStreamAggregator.class);
  private final Properties properties;

  public LogisticTasksStreamAggregator(Properties properties) {
	this.properties = properties;
  }

  public Topology createTopology() {
	SpecificAvroSerde<LogisticTask> logisticTaskSerde = getSpecificAvroSerde(properties);
	Serde<AggregatedTasks> aggregatedTasksSerde = getSpecificAvroSerde(properties);

	StreamsBuilder streamsBuilder = new StreamsBuilder();
	streamsBuilder
			.stream("logistic_tasks", Consumed.with(Serdes.String(), logisticTaskSerde))
			.peek((k, v) -> log.info("Consumed logistic task record: {}", v))
			.map((s1, logisticTask) -> new KeyValue<>( // mapping to key value event to be able to group by key
						 logisticTask.getCity(),
						 logisticTask
				 )
			)
			.groupByKey(Grouped.with(Serdes.String(), logisticTaskSerde)) // grouping by key using String Serde
			.windowedBy(TimeWindows.ofSizeAndGrace( // create window of 10s for those group
					Duration.ofSeconds(10),
					Duration.ofMillis(10)
			))
			.aggregate( // aggregate each events in window to new AggregatedTasks events
					() -> new AggregatedTasks(0, ""),
					(city, logisticTask, aggregatedTasks) -> {
					  aggregatedTasks.setCity(city);
					  int current = aggregatedTasks.getRequiredFleet();
					  aggregatedTasks.setRequiredFleet(++current);
					  return aggregatedTasks;
					},
					Materialized.with(Serdes.String(), aggregatedTasksSerde)
			)
			.suppress(untilWindowCloses(unbounded())) // wait for window to close
			.toStream()
			.map((wk, value) -> KeyValue.pair(wk.key(), value))
			.peek((s, aggregatedTasks) ->
						  log.info("Sending a new aggregated logistic tasks event: {}", aggregatedTasks))
			.to("aggregated_tasks", Produced.with(Serdes.String(), aggregatedTasksSerde)); // send aggregated events to new topic
	return streamsBuilder.build();
  }
}

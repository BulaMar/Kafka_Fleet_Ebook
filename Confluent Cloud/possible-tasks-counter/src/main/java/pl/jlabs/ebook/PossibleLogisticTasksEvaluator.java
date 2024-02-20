package pl.jlabs.ebook;

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.JoinWindows;
import org.apache.kafka.streams.kstream.Joined;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.StreamJoined;
import org.apache.kafka.streams.kstream.ValueJoiner;
import org.apache.kafka.streams.processor.internals.StreamThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.jlabs.ebook.truck.AggregatedTasks;
import pl.jlabs.ebook.truck.AvailableTrucks;
import pl.jlabs.ebook.truck.PossibleTasks;

import java.time.Duration;
import java.util.Properties;

import static pl.jlabs.ebook.SerdesUtil.getSpecificAvroSerde;

public class PossibleLogisticTasksEvaluator {

  private static final Logger log = LoggerFactory.getLogger(PossibleLogisticTasksEvaluator.class);
  private final Properties properties;

  public PossibleLogisticTasksEvaluator(Properties properties) {
	this.properties = properties;
  }

  public Topology evaluate() {
	SpecificAvroSerde<AggregatedTasks> logisticTaskSerde = getSpecificAvroSerde(properties);
	SpecificAvroSerde<AvailableTrucks> availableTrucksSerde = getSpecificAvroSerde(properties);
	SpecificAvroSerde<PossibleTasks> possibleTasksSerde = getSpecificAvroSerde(properties);

	StreamsBuilder builder = new StreamsBuilder();
	KStream<String, AvailableTrucks> availableTrucksStream = builder
			.stream("available_trucks", Consumed.with(Serdes.String(), availableTrucksSerde))
			.peek((k, v) -> log.info("Consumed available fleet: {}", v));

	KStream<String, AggregatedTasks> logisticTasksStream = builder
			.stream("aggregated_tasks", Consumed.with(Serdes.String(), logisticTaskSerde))
			.peek((k, v) -> log.info("Consumed aggregated tasks: {}", v));

	ValueJoiner<AvailableTrucks, AggregatedTasks, Integer> taskJoiner =
			(availableTrucks, logisticTasksNumber) -> {
			  log.info(
					  "Joining available fleet record {} with aggregated tasks {}",
					  availableTrucks,
					  logisticTasksNumber
			  );
			  int fleetSize = availableTrucks.getFleetSize();
			  return Math.min(fleetSize, logisticTasksNumber.getRequiredFleet());
			};

	availableTrucksStream.join(
					logisticTasksStream,
					taskJoiner,
					JoinWindows.ofTimeDifferenceAndGrace(
							Duration.ofMinutes(1),
							Duration.ofSeconds(10)
					),
					StreamJoined.with(
							Serdes.String(),
							availableTrucksSerde,
							logisticTaskSerde
					)
			)
			.peek((k, v) -> log.info("Joined {} truck(s) for {}", v, k))
			.groupByKey(Grouped.with(Serdes.String(), Serdes.Integer()))
			.reduce(Integer::sum)
			.mapValues((readOnlyKey, value) ->
							   new PossibleTasks(value, readOnlyKey))
			.toStream()
			.to(
					"possible_tasks",
					Produced.with(Serdes.String(), possibleTasksSerde)
			);

	return builder.build();
  }
}

package pl.jlabs.ebook;

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.jlabs.ebook.truck.AvailableTrucks;
import pl.jlabs.ebook.truck.TruckStatus;

import java.time.Duration;
import java.util.Properties;

import static org.apache.kafka.streams.kstream.Suppressed.BufferConfig.unbounded;
import static org.apache.kafka.streams.kstream.Suppressed.untilWindowCloses;
import static pl.jlabs.ebook.SerdesUtil.getSpecificAvroSerde;

public class AvailableTrucksStreamAggregator {

  private static final Logger log = LoggerFactory.getLogger(AvailableTrucksStreamAggregator.class);
  private final Properties properties;

  public AvailableTrucksStreamAggregator(Properties properties) {
	this.properties = properties;
  }

  public Topology createTopology() {
	SpecificAvroSerde<TruckStatus> truckStatusSerde = getSpecificAvroSerde(properties);
	Serde<AvailableTrucks> availableTrucksSerde = getSpecificAvroSerde(properties);

	StreamsBuilder streamsBuilder = new StreamsBuilder();
	KTable<Windowed<String>, AvailableTrucks> trucksStatus = streamsBuilder
			.stream("trucks_status", Consumed.with(Serdes.String(), truckStatusSerde))
			.peek((k, v) -> log.info("Consumed truck status: {}", v))
			.filter((k, v) -> v.getAvailable())
			.map((s, truckStatus) -> new KeyValue<>(
						 truckStatus.getLocation().getCity(),
						 truckStatus
				 )
			)
			.groupByKey(Grouped.with(Serdes.String(), truckStatusSerde))
			.windowedBy(TimeWindows.ofSizeAndGrace(
					Duration.ofSeconds(10),
					Duration.ofMillis(10)
			))
			.aggregate(
					() -> new AvailableTrucks(0, "", ""),
					(city, truckStatus, availableTrucks) -> {
					  availableTrucks.setCity(city);
					  availableTrucks.setCountry(truckStatus.getLocation().getCountry());
					  int current = availableTrucks.getFleetSize();
					  availableTrucks.setFleetSize(++current);
					  return availableTrucks;
					},
					Materialized.with(Serdes.String(), availableTrucksSerde)
			);
	trucksStatus.mapValues((readOnlyKey, value) -> value);
	trucksStatus
			.suppress(untilWindowCloses(unbounded()))
			.toStream()
			.map((wk, value) -> KeyValue.pair(wk.key(), value))
			.peek((s, availableTrucks) ->
						  log.info("Sending a new trucks availability event: {}", availableTrucks))
			.to("available_trucks", Produced.with(Serdes.String(), availableTrucksSerde));
	return streamsBuilder.build();
  }
}

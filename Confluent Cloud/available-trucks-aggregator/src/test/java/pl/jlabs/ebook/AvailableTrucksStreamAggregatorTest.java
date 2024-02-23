package pl.jlabs.ebook;

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pl.jlabs.ebook.truck.AvailableTrucks;
import pl.jlabs.ebook.truck.BasicLocation;
import pl.jlabs.ebook.truck.TruckStatus;

import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static pl.jlabs.ebook.SerdesUtil.*;

class AvailableTrucksStreamAggregatorTest {

  private static final String POLAND = "POLAND";
  private static final String KRAKOW = "KRAKOW";
  private static final String WARSAW = "WARSAW";
  private static final String GDANSK = "GDANSK";
  private static final String GERMANY = "GERMANY";
  private static final String BERLIN = "BERLIN";

  @Test
  void shouldAggregateAvailableTrucks() {

	final Properties properties = new Properties();
	properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "available-trucks-aggregate-test");
	// TopologyTestDriver can use a in memory schema registry MockSchemaRegistry.
	// It will pick it up automatically when there will be mock:// instead of http:// in the schema.registry.url property
	properties.put("schema.registry.url", "mock://available-trucks-aggregation-test-schema-registry");

	Topology topology = new AvailableTrucksStreamAggregator(properties).createTopology();

	// The TopologyTestDriver allows us to verify our stream topologies without running a kafka instance
	try (TopologyTestDriver testDriver = new TopologyTestDriver(topology, properties)) {
	  SpecificAvroSerde<TruckStatus> truckStatusSerde = getSpecificAvroSerde(properties);
	  SpecificAvroSerde<AvailableTrucks> availableTrucksSerde = getSpecificAvroSerde(properties);
	  TestInputTopic<String, TruckStatus> testInputTopic = testDriver.createInputTopic(
			  "trucks_status",
			  Serdes.String().serializer(),
			  truckStatusSerde.serializer()
	  );
	  TestOutputTopic<String, AvailableTrucks> testOutputTopic = testDriver.createOutputTopic(
			  "available_trucks",
			  Serdes.String().deserializer(),
			  availableTrucksSerde.deserializer()
	  );

	  long timestampMs = 0L;

	  BasicLocation berlin = new BasicLocation(BERLIN, GERMANY);
	  BasicLocation gdansk = new BasicLocation(GDANSK, POLAND);
	  BasicLocation warsaw = new BasicLocation(WARSAW, POLAND);
	  BasicLocation krakow = new BasicLocation(KRAKOW, POLAND);
	  // Let's create some input events and pass them to the input topic
	  for (TruckStatus value : List.of(
			  createTruckStatus(true, krakow),
			  createTruckStatus(false, krakow),
			  createTruckStatus(true, warsaw),
			  createTruckStatus(false, warsaw),
			  createTruckStatus(true, warsaw),
			  createTruckStatus(true, gdansk),
			  createTruckStatus(true, gdansk),
			  createTruckStatus(true, berlin),
			  createTruckStatus(true, berlin)
	  )) {

		testInputTopic.pipeInput(null, value, timestampMs);
		timestampMs += 1000; // we are increasing timestamp to simulate gaps between events delivery
	  }

	  // PUT ONE MORE TO KEEP FLOW OF EVENTS
	  testInputTopic.pipeInput(null, createTruckStatus(true, warsaw), 10001);

	  // LATE ARRIVAL WITHIN GRACE PERIOD
	  testInputTopic.pipeInput(null, createTruckStatus(true, berlin), 9991);

	  // PUT ONE MORE TO KEEP FLOW OF EVENTS
	  testInputTopic.pipeInput(null, createTruckStatus(true, krakow), 10010);

	  // LATE ARRIVAL EXPIRED
	  testInputTopic.pipeInput(null, createTruckStatus(true, berlin), 9000);

	  List<AvailableTrucks> expectedOutput = List.of(
			  new AvailableTrucks(3, BERLIN, GERMANY),
			  new AvailableTrucks(2, GDANSK, POLAND),
			  new AvailableTrucks(1, KRAKOW, POLAND),
			  new AvailableTrucks(2, WARSAW, POLAND)
	  );

	  List<AvailableTrucks> actualValues = testOutputTopic.readValuesToList();
	  Assertions.assertEquals(expectedOutput, actualValues);
	}
  }

  private static TruckStatus createTruckStatus(boolean available, BasicLocation krakow) {
	return new TruckStatus(UUID.randomUUID().toString(), 15000.0, available, krakow);
  }
}

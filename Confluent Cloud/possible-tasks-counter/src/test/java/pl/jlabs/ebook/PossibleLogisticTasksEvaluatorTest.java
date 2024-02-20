package pl.jlabs.ebook;

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.Test;
import pl.jlabs.ebook.truck.AggregatedTasks;
import pl.jlabs.ebook.truck.AvailableTrucks;
import pl.jlabs.ebook.truck.PossibleTasks;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static pl.jlabs.ebook.SerdesUtil.getSpecificAvroSerde;

class PossibleLogisticTaskEvaluatorTest {

  private static final String POLAND = "POLAND";
  private static final String KRAKOW = "KRAKOW";
  private static final String WARSAW = "WARSAW";
  private static final String GDANSK = "GDANSK";
  private static final String GERMANY = "GERMANY";
  private static final String BERLIN = "BERLIN";

  @Test
  void shouldEvaluatePossibleTasksNumber() {

	final Properties properties = new Properties();
	properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "possible-tasks-test");
	properties.put("schema.registry.url", "mock://possible-tasks-test-schema-registry");

	Topology topology = new PossibleLogisticTasksEvaluator(properties).evaluate();
	try (TopologyTestDriver testDriver = new TopologyTestDriver(topology, properties)) {

	  SpecificAvroSerde<AvailableTrucks> availableTrucksSerde = getSpecificAvroSerde(properties);
	  SpecificAvroSerde<AggregatedTasks> logisticTasksSerde = getSpecificAvroSerde(properties);
	  SpecificAvroSerde<PossibleTasks> possibleTasksSerde = getSpecificAvroSerde(properties);

	  TestInputTopic<String, AggregatedTasks> testAggregatedTasksInput = testDriver.createInputTopic(
			  "aggregated_tasks",
			  Serdes.String().serializer(),
			  logisticTasksSerde.serializer()
	  );
	  TestInputTopic<String, AvailableTrucks> testAvailableTrucksInput = testDriver.createInputTopic(
			  "available_trucks",
			  Serdes.String().serializer(),
			  availableTrucksSerde.serializer()
	  );
	  TestOutputTopic<String, PossibleTasks> testOutputTopic = testDriver.createOutputTopic(
			  "possible_tasks",
			  Serdes.String().deserializer(),
			  possibleTasksSerde.deserializer()
	  );
	  pipeAvailableTrucks(createAvailableTrucksInput(7, GDANSK, POLAND, 500), testAvailableTrucksInput);
	  pipeAvailableTrucks(createAvailableTrucksInput(6, KRAKOW, POLAND, 1300), testAvailableTrucksInput);
	  pipeAvailableTrucks(createAvailableTrucksInput(3, WARSAW, POLAND, 2400), testAvailableTrucksInput);
	  pipeAvailableTrucks(createAvailableTrucksInput(5, BERLIN, GERMANY, 4000), testAvailableTrucksInput);

	  pipeAggregatedTasks(createAggregatedTasksInput(2, WARSAW, 1000), testAggregatedTasksInput);
	  pipeAggregatedTasks(createAggregatedTasksInput(4, KRAKOW, 2000), testAggregatedTasksInput);
	  pipeAggregatedTasks(createAggregatedTasksInput(3, GDANSK, 3000), testAggregatedTasksInput);
	  pipeAggregatedTasks(createAggregatedTasksInput(5, BERLIN, 4100), testAggregatedTasksInput);
	  pipeAggregatedTasks(createAggregatedTasksInput(2, BERLIN, 10500), testAggregatedTasksInput);
	  pipeAggregatedTasks(createAggregatedTasksInput(4, GDANSK, 11200), testAggregatedTasksInput);
	  pipeAggregatedTasks(createAggregatedTasksInput(2, KRAKOW, 15000), testAggregatedTasksInput);
	  pipeAggregatedTasks(createAggregatedTasksInput(2, WARSAW, 18000), testAggregatedTasksInput);

	  // LATE AVAILABILITY WITHIN JOIN WINDOW
	  pipeAvailableTrucks(createAvailableTrucksInput(1, WARSAW, POLAND, 17100), testAvailableTrucksInput);
	  pipeAvailableTrucks(createAvailableTrucksInput(2, GDANSK, POLAND, 16100), testAvailableTrucksInput);
	  pipeAvailableTrucks(createAvailableTrucksInput(5, BERLIN, POLAND, 15500), testAvailableTrucksInput);
	  pipeAvailableTrucks(createAvailableTrucksInput(2, KRAKOW, POLAND, 15100), testAvailableTrucksInput);

	  Map<String, PossibleTasks> actual = testOutputTopic.readKeyValuesToMap(); // reading the latest
	  Map<String, PossibleTasks> expected = Map.of(
			  BERLIN, new PossibleTasks(7, BERLIN),
			  GDANSK, new PossibleTasks(5, GDANSK),
			  KRAKOW, new PossibleTasks(6, KRAKOW),
			  WARSAW, new PossibleTasks(3, WARSAW)
	  );
	  assertEquals(expected, actual);
	}
  }

  private static void pipeAvailableTrucks(
		  TestTuple<AvailableTrucks> availableTrucksTestTuple,
		  TestInputTopic<String, AvailableTrucks> testAvailableTrucksInput
  ) {
	AvailableTrucks value = availableTrucksTestTuple.value;
	testAvailableTrucksInput.pipeInput(value.getCity(), value, availableTrucksTestTuple.timestamp);
  }

  private static void pipeAggregatedTasks(
		  TestTuple<AggregatedTasks> logisticTaskTestTuple,
		  TestInputTopic<String, AggregatedTasks> testAggregatedTasksInput
  ) {
	AggregatedTasks logisticTask = logisticTaskTestTuple.value;
	testAggregatedTasksInput.pipeInput(logisticTask.getCity(), logisticTask, logisticTaskTestTuple.timestamp);
  }

  private static TestTuple<AggregatedTasks> createAggregatedTasksInput(
		  Integer requiredFleet,
		  String city,
		  long timestamp
  ) {
	return new TestTuple<>(new AggregatedTasks(requiredFleet, city), timestamp);
  }

  private static TestTuple<AvailableTrucks> createAvailableTrucksInput(
		  int availableFleetSize,
		  String city,
		  String country,
		  long timestamp
  ) {
	return new TestTuple<>(new AvailableTrucks(availableFleetSize, city, country), timestamp);
  }

  private record TestTuple<T>(T value, long timestamp) {
  }
}

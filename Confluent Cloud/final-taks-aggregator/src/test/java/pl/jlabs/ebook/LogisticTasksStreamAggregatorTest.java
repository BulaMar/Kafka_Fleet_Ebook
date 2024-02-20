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
import pl.jlabs.ebook.truck.AggregatedTasks;
import pl.jlabs.ebook.truck.LogisticTask;

import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static pl.jlabs.ebook.SerdesUtil.getSpecificAvroSerde;

class LogisticTasksStreamAggregatorTest {

  private static final String KRAKOW = "KRAKOW";
  private static final String WARSAW = "WARSAW";
  private static final String GDANSK = "GDANSK";
  private static final String BERLIN = "BERLIN";

  @Test
  void shouldAggregateAggregatedTasks() {

	final Properties properties = new Properties();
	properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "available-trucks-aggregate-test");
	properties.put("schema.registry.url", "mock://available-trucks-aggregation-test-schema-registry");

	Topology topology = new LogisticTasksStreamAggregator(properties).createTopology();

	// TopologyTestDriver allows us to verify our stream topologies without running kafka instance
	try (TopologyTestDriver testDriver = new TopologyTestDriver(topology, properties)) {
	  SpecificAvroSerde<LogisticTask> truckStatusSerde = getSpecificAvroSerde(properties);
	  SpecificAvroSerde<AggregatedTasks> availableTrucksSerde = getSpecificAvroSerde(properties);
	  TestInputTopic<String, LogisticTask> testInputTopic = testDriver.createInputTopic(
			  "logistic_tasks",
			  Serdes.String().serializer(),
			  truckStatusSerde.serializer()
	  );
	  TestOutputTopic<String, AggregatedTasks> testOutputTopic = testDriver.createOutputTopic(
			  "aggregated_tasks",
			  Serdes.String().deserializer(),
			  availableTrucksSerde.deserializer()
	  );

	  long timestampMs = 0L;

	  for (LogisticTask value : List.of(
			  createLogisticTask(KRAKOW),
			  createLogisticTask(KRAKOW),
			  createLogisticTask(WARSAW),
			  createLogisticTask(WARSAW),
			  createLogisticTask(WARSAW),
			  createLogisticTask(GDANSK),
			  createLogisticTask(GDANSK),
			  createLogisticTask(BERLIN),
			  createLogisticTask(BERLIN)
	  )) {

		testInputTopic.pipeInput(null, value, timestampMs);
		timestampMs += 1000;
		System.out.println(timestampMs);
	  }

	  // PUT ONE MORE TO KEEP FLOW OF EVENTS
	  testInputTopic.pipeInput(null, createLogisticTask(WARSAW), 10001);

	  // LATE ARRIVAL WITHIN GRACE PERIOD
	  testInputTopic.pipeInput(null, createLogisticTask(BERLIN), 9991);

	  // PUT ONE MORE TO KEEP FLOW OF EVENTS
	  testInputTopic.pipeInput(null, createLogisticTask(KRAKOW), 10010);

	  // LATE ARRIVAL EXPIRED
	  testInputTopic.pipeInput(null, createLogisticTask(BERLIN), 9000);

	  List<AggregatedTasks> expectedOutput = List.of(
			  new AggregatedTasks(3, BERLIN),
			  new AggregatedTasks(2, GDANSK),
			  new AggregatedTasks(2, KRAKOW),
			  new AggregatedTasks(3, WARSAW)
	  );

	  List<AggregatedTasks> actualValues = testOutputTopic.readValuesToList();
	  Assertions.assertEquals(expectedOutput, actualValues);
	}
  }

  private static LogisticTask createLogisticTask(String city) {
	return new LogisticTask(UUID.randomUUID().toString(), city);
  }
}

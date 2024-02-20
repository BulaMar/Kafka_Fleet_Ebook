package pl.jlabs.ebook;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.Topology;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class Main {

  public static void main(String[] args) throws IOException {
	Properties properties = loadConfig();
	Topology topology = new PossibleLogisticTasksEvaluator(properties).evaluate();
	runKafkaStreams(new KafkaStreams(topology, properties));
  }

  private static void runKafkaStreams(final KafkaStreams streams) {
	final CountDownLatch latch = new CountDownLatch(1);
	streams.setStateListener((newState, oldState) -> {
	  if (oldState == KafkaStreams.State.RUNNING && newState != KafkaStreams.State.RUNNING) {
		latch.countDown();
	  }
	});

	streams.start();

	try {
	  latch.await();
	} catch (final InterruptedException e) {
	  throw new RuntimeException(e);
	}
  }

  private static Properties loadConfig() throws IOException {
	final Properties properties = new Properties();
	try (InputStream inputStream = Main.class.getClassLoader().getResourceAsStream("stream.properties")) {
	  properties.load(inputStream);
	}
	return properties;
  }
}


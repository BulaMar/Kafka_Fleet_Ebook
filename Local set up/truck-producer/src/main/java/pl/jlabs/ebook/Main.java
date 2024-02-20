package pl.jlabs.ebook;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class Main {

  private static final Logger log = LoggerFactory.getLogger(Main.class);
  private static final String TOPIC = "LOCATION";

  public static void main(String[] args) throws InterruptedException {

	String truckId = System.getenv("TRUCK_ID");
	String bootstrapServers = System.getenv("BOOTSTRAP_SERVERS");

	Properties properties = new Properties() {{
	  setProperty(
			  ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
			  bootstrapServers
	  );
	  setProperty(
			  ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
			  StringSerializer.class.getName()
	  );
	  setProperty(
			  ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
			  StringSerializer.class.getName()
	  );

//	UNCOMMENT TO ENABLE ROUND ROBIN PARTITIONER
//	  setProperty(
//			  ProducerConfig.PARTITIONER_CLASS_CONFIG,
//			  RoundRobinPartitioner.class.getName()
//	  );

	}};

	try (KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(properties)) {
	  try (ScheduledExecutorService scheduledExecutorService =
				   newScheduledThreadPool(3)) {
		ScheduledFuture<?> scheduledFuture = scheduledExecutorService
				.scheduleAtFixedRate(
						() -> produceRandomLocation(truckId, kafkaProducer),
						0L,
						50L,
						MILLISECONDS
				);
		Thread.sleep(60000);
		scheduledFuture.cancel(false);
	  }
	}
  }

  private static void produceRandomLocation(
		  String truckId, KafkaProducer<String, String> kafkaProducer
  ) {
	String value = "{ \"truckId\": \"" + truckId +
				   "\", \"location\": \"" + randomLocation() +
				   "\" }";
	kafkaProducer.send(
			new ProducerRecord<>(TOPIC, value),
			(metadata, exception) -> {
			  if (exception == null) {
				log.info(
						"Message sent to topic: {} partition: {}.",
						metadata.topic(),
						metadata.partition()
				);
			  } else {
				log.error("Unable to send record due to exception.", exception);
			  }
			}
	);
  }

  private static String randomLocation() {
	double latitude = (Math.random() * 180.0) - 90.0;
	double longitude = (Math.random() * 360.0) - 180.0;
	DecimalFormat df = new DecimalFormat("#.##");
	return df.format(latitude) + "," + df.format(longitude);
  }
}

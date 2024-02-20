package pl.jlabs.ebook;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.jlabs.ebook.truck.LogisticTask;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class ProducerUtil {

  private static final Logger log = LoggerFactory.getLogger(ProducerUtil.class);
  private static final String TOPIC = "logistic_tasks";

  public static Properties loadConfig() throws IOException {
	final Properties properties = new Properties();
	try (InputStream inputStream = ProducerUtil.class.getClassLoader()
			.getResourceAsStream("client.properties")
	) {
	  properties.load(inputStream);
	}
	return properties;
  }

  public static void produceRandomTask(KafkaProducer<String, LogisticTask> kafkaProducer) {
	Random random = ThreadLocalRandom.current();
	LogisticTask logisticTask = randomLogisticTask(random);
	ProducerRecord<String, LogisticTask> record = new ProducerRecord<>(TOPIC, logisticTask);
	record.headers().add(new RecordHeader("Timestamp", Instant.now().toString().getBytes()));
	kafkaProducer.send(
			record,
			(metadata, exception) -> {
			  if (exception == null) {
				log.info(
						"Message sent to topic: {} partition: {}.",
						metadata.topic(),
						metadata.partition()
				);
			  } else {
				log.error(
						"Unable to sent record due to exception.",
						exception
				);
			  }
			}
	);
  }

  private static LogisticTask randomLogisticTask(Random random) {
	return new LogisticTask(
			UUID.randomUUID().toString(),
			City.values()[random.nextInt(City.values().length)].name()
	);
  }
}

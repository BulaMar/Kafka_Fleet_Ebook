package pl.jlabs.ebook;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.jlabs.ebook.truck.BasicLocation;
import pl.jlabs.ebook.truck.TruckStatus;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class ProducerUtil {

  private static final Logger log = LoggerFactory.getLogger(ProducerUtil.class);
  private static final String TOPIC = "trucks_status";

  public static Properties loadConfig() throws IOException {
	final Properties properties = new Properties();
	try (InputStream inputStream = ProducerUtil.class.getClassLoader()
			.getResourceAsStream("client.properties")
	) {
	  properties.load(inputStream);
	}
	return properties;
  }

  public static void produceRandomStatus(KafkaProducer<String, TruckStatus> kafkaProducer) {
	Random random = ThreadLocalRandom.current();
	TruckStatus truckStatus = randomTruckStatus(random);
	ProducerRecord<String, TruckStatus> record = new ProducerRecord<>(TOPIC, truckStatus);
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

  private static TruckStatus randomTruckStatus(Random random) {
	double mileage = random.nextDouble(15000.00, 60000.00);
	mileage = Math.round(mileage);
	mileage = mileage / 100;
	return new TruckStatus(
			UUID.randomUUID().toString(),
			mileage,
			random.nextBoolean(),
			randomTruckLocation(random)
	);
  }

  private static BasicLocation randomTruckLocation(Random random) {
	City city = City.values()[random.nextInt(City.values().length)];
	return new BasicLocation(city.name(), city.getCountry());
  }
}

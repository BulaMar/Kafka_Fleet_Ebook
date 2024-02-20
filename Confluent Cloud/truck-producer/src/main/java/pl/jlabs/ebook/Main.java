package pl.jlabs.ebook;

import org.apache.kafka.clients.producer.KafkaProducer;
import pl.jlabs.ebook.truck.TruckStatus;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static java.util.concurrent.TimeUnit.SECONDS;

public class Main {

  private static final long STATUS_INTERVAL_PERIOD = 5L;
  private static final int MAX_STATUS_SEND_TIME = 3600000;

  public static void main(String[] args) throws InterruptedException, IOException {

	Properties properties = ProducerUtil.loadConfig();

	try (KafkaProducer<String, TruckStatus> kafkaProducer =
				 new KafkaProducer<>(properties)
	) {
	  try (ScheduledExecutorService scheduledExecutorService =
				   Executors.newScheduledThreadPool(3)
	  ) {
		ScheduledFuture<?> scheduledFuture = scheduledExecutorService
				.scheduleAtFixedRate(
						() -> ProducerUtil.produceRandomStatus(kafkaProducer),
						0L,
						STATUS_INTERVAL_PERIOD,
						SECONDS
				);
		Thread.sleep(MAX_STATUS_SEND_TIME);
		scheduledFuture.cancel(false);
	  }
	}
  }
}

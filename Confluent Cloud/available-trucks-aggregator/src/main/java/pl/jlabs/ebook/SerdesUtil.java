package pl.jlabs.ebook;

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.common.utils.Utils;

import java.util.Map;
import java.util.Properties;

public class SerdesUtil {

  // Creating avro serde for specific class type
  public static <T extends SpecificRecord> SpecificAvroSerde<T> getSpecificAvroSerde(final Properties properties) {
	Map<String, Object> config = Utils.propsToMap(properties);
	final SpecificAvroSerde<T> specificAvroSerde = new SpecificAvroSerde<>();
	specificAvroSerde.configure(config, false);
	return specificAvroSerde;
  }
}

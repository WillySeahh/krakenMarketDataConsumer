import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class KafkaConsumerClass {

    public static void main(String[] args) {

        // Set up the consumer properties
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "my-group-id");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        // Create the consumer
        Consumer<String, String> consumer = new KafkaConsumer<>(props);

        // Subscribe to the topic
        consumer.subscribe(Collections.singletonList("1m_Candle"));

        // Track the offset of the last processed record
        long lastOffset = -1;
        // Continuously poll for new messages
        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1));
                for (ConsumerRecord<String, String> record : records) {
                    if (record.offset() > lastOffset) {
                        System.out.println(record.value());
                        lastOffset = record.offset();
                    }
                }
            }
        } finally {
            // Close the consumer
            consumer.close();
        }
    }
}

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.google.gson.Gson;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;

public class KrakenClient extends WebSocketClient {

    private Gson gson = new Gson();

    private final OrderBookTreeMap bookTreeMap = new OrderBookTreeMap();

    private static final SortedMap<String, Candle> candles = new TreeMap<>(Comparator.reverseOrder());

    public KrakenClient(URI serverUri, Draft draft) {
        super(serverUri, draft);
    }

    public KrakenClient(URI serverURI) {
        super(serverURI);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("New connection opened");
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("closed with exit code " + code + " additional info: " + reason);
    }

    @Override
    public void onMessage(String message) {
        if (message.contains("heartbeat") || message.contains("error") || message.contains("subscribe")) {
            return;
        }
        //System.out.println("Received raw message: " + message);
        KrakenResponseJson myObject = gson.fromJson(message, KrakenResponseJson.class);
        KrakenResponseJson.Data data = myObject.getData().get(0);

        // Benchmarking performance start
        if (data.getBids() != null) {
            for (KrakenResponseJson.Order order : data.getBids()) {
                //book.add("0", "B", order.getPrice(), order.getQuantity());
                bookTreeMap.add("B", order.getPrice(), order.getQuantity());
            }
        }
        if (data.getAsks() != null) {
            for (KrakenResponseJson.Order order : data.getAsks()) {
                //book.add("0", "S", order.getPrice(), order.getQuantity());
                bookTreeMap.add("S", order.getPrice(), order.getQuantity());
            }
        }
        // Benchmarking performance end

        // Sanity check 1: at least one bid and 1 ask present
        Double bestBid = bookTreeMap.getBestBid(); // -1.0 if no bid
        Double bestAsk = bookTreeMap.getBestAsk();
        if (bestBid == -1.0 || bestAsk == 1.0) {
            // Do not throw exception, to prevent stopping the program.
            System.out.println("Error: Do not have at least 1 bid or 1 ask.");
        }

        // Sanity check 2: highest bid < lowest ask
        if (bestBid != -1.0 && bestAsk != -1.0) {
            if (bestBid >= bestAsk) {
                // Do not throw exception, to prevent stopping the program.
                System.out.println("Error, bestBid >= bestAsk for Orderbook");
            }
        }

        updateCandle(data, bestAsk, bestBid);
        //System.out.println(bookTreeMap.toString());
    }

    private void updateCandle(KrakenResponseJson.Data data, Double bestAsk, Double bestBid ) {
        if (data.getTimestamp() != null) { // Only update candle for 'tick update' messages
            Instant timestamp = Instant.parse(data.getTimestamp());
            Instant truncatedTimestamp = timestamp.truncatedTo(ChronoUnit.MINUTES);
            String truncatedTimestampStr = truncatedTimestamp.toString();

            Candle candle = candles.computeIfAbsent(truncatedTimestampStr, k -> new Candle(truncatedTimestampStr));

            double currMidPrice = (bestAsk + bestBid) / 2;

            if (candle.getOpen() == -1.0) { // only the first tick of this minute should update this
                candle.setOpen(currMidPrice);
            }
            candle.setHigh(Math.max(candle.getHigh(), currMidPrice));
            candle.setLow(Math.min(candle.getLow(), currMidPrice));
            candle.setClose(currMidPrice);
            candle.setTicks(candle.getTicks()+1);
        }
    }

    @Override
    public void onMessage(ByteBuffer message) {
        System.out.println("received ByteBuffer");
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("an error occurred:" + ex);
    }

    public static Producer<String, String> initiateKafkaProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        Producer<String, String> producer = new KafkaProducer<>(props);
        return producer;
    }

    public static void main(String[] args) throws URISyntaxException {
        // Define the subscription message in JSON format
        String subscriptionMessage = "{\"method\": \"subscribe\", \"params\": {\"channel\": \"book\", \"symbol\": [\"ALGO/USD\"]}}";

        // [Bonus task] - remove commented lines to set up kafka publisher
        //Producer<String, String> producer = initiateKafkaProducer();
        //AtomicReference<ProducerRecord<String, String>> record = new AtomicReference<>(new ProducerRecord<>("1m_Candle", "key", "First kafka produced message"));
        //producer.send(record.get());

        try {
            // Create a WebSocket client
            WebSocketClient client = new KrakenClient(new URI("wss://ws.kraken.com/v2"));

            client.connectBlocking();
            // Send the subscription message after connection is established
            if (client.isOpen()) {
                client.send(subscriptionMessage);
                System.out.println("Sent Subscription Message: " + subscriptionMessage);
            }

            StringBuilder sb = new StringBuilder();

            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

            scheduler.scheduleAtFixedRate(() -> {
                sb.append("===================================" + '\n');
                sb.append("Printing all 1m candles every 15s" + '\n');
                for (Map.Entry<String, Candle> entry : candles.entrySet()) {
                    sb.append(entry.getValue().toString() + '\n');
                }
                sb.append("End of candles. Next candles printing in 15s." + '\n');
                sb.append("===================================" + '\n');
                // [Bonus task] - remove 2 commented lines to publish to kafka
                //record.set(new ProducerRecord<>("1m_Candle", "key", sb.toString()));
                //producer.send(record.get());
                System.out.println(sb.toString());
                sb.setLength(0); // clear string buffer cache, reuse object, reduce GC.
            }, 0, 15, TimeUnit.SECONDS);

        } catch (Exception e) {
            System.err.println("Error connecting to WebSocket: " + e.getMessage());
        }
    }
}
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
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
            if (bestBid > bestAsk) {
                // Do not throw exception, to prevent stopping the program.
                System.out.println("Error, bestBid > bestAsk for Orderbook");
            }
        }

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
        //System.out.println(bookTreeMap.toString());
    }

    @Override
    public void onMessage(ByteBuffer message) {
        System.out.println("received ByteBuffer");
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("an error occurred:" + ex);
    }

    public static void main(String[] args) throws URISyntaxException {
        // Define the subscription message in JSON format
        String subscriptionMessage = "{\"method\": \"subscribe\", \"params\": {\"channel\": \"book\", \"symbol\": [\"ALGO/USD\"]}}";

        try {
            // Create a WebSocket client
            WebSocketClient client = new KrakenClient(new URI("wss://ws.kraken.com/v2"));

            // Connect to the WebSocket server
            client.connectBlocking();

            // Send the subscription message after connection is established
            if (client.isOpen()) {
                client.send(subscriptionMessage);
                System.out.println("Sent Subscription Message: " + subscriptionMessage);
            }

            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(() -> {
                System.out.println("===================================");
                System.out.println("Printing all 1m candles every 15s");
                for (Map.Entry<String, Candle> entry : candles.entrySet()) {
                    System.out.println(entry.getValue().toString());
                }
                System.out.println("End of candles. Next candles printing in 15s.");
                System.out.println("===================================");
            }, 0, 15, TimeUnit.SECONDS);


        } catch (Exception e) {
            System.err.println("Error connecting to WebSocket: " + e.getMessage());
        }
    }
}
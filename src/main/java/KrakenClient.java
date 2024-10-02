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

    public Gson gson = new Gson();

    private Book book = new Book();

    private static SortedMap<String, Candle> candles = new TreeMap<>(Comparator.reverseOrder());

    public KrakenClient(URI serverUri, Draft draft) {
        super(serverUri, draft);
    }

    public KrakenClient(URI serverURI) {
        super(serverURI);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        send("Hello, it is me. Mario :)");
        System.out.println("new connection opened");
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
        System.out.println("received message: " + message);
        KrakenResponseJson myObject = gson.fromJson(message, KrakenResponseJson.class);
        System.out.println(myObject.toString());
        KrakenResponseJson.Data data = myObject.getData().get(0);

        if (data.getBids() != null) {
            for (KrakenResponseJson.Order order : data.getBids()) {
                book.add("0", "B", order.getPrice(), order.getQuantity());
            }
        }

        if (data.getAsks() != null) {
            for (KrakenResponseJson.Order order : data.getAsks()) {
                book.add("0", "S", order.getPrice(), order.getQuantity());
            }
        }

        // Sanity check 1: at least one bid and ask present
        OrdersAtPrice bestBids = book.bidsByPrice;
        OrdersAtPrice bestAsks = book.bidsByPrice;
        if (bestBids == null && bestAsks == null) {
            // Do not throw exception, to prevent stopping the program.
            System.out.println("No bids and ask");
        }

        // Sanity check 2: highest bid < lowest ask
        System.out.println("Sanity Check to ensure highest bid < lowest ask");
        if (book.bidsByPrice != null && book.asksByPrice != null) {
            Double bestBid = book.bidsByPrice.price;
            Double bestAsk = book.asksByPrice.price;
            System.out.println("Best Bid:" + bestBid + " " + "Best Ask:" + bestAsk);
            if (bestBid > bestAsk) {
                // Do not throw exception, to prevent stopping the program.
                System.out.println("Error, bestBid > bestAsk");
            }
        }

        if (data.getTimestamp() != null) {
            Instant timestamp = Instant.parse(data.getTimestamp());
            Instant truncatedTimestamp = timestamp.truncatedTo(ChronoUnit.MINUTES);
            String truncatedTimestampStr = truncatedTimestamp.toString();

            Candle candle = candles.computeIfAbsent(truncatedTimestampStr, k -> new Candle(truncatedTimestampStr));

            Double currMidPrice = (book.asksByPrice.price + book.bidsByPrice.price) / 2;

            if (candle.getOpen() == -1.0) {
                // only the first tick of this minute should update this
                candle.setOpen(currMidPrice);
            }
            candle.setHigh(Math.max(candle.getHigh(), currMidPrice));
            candle.setLow(Math.min(candle.getLow(), currMidPrice));
            candle.setClose(currMidPrice);
            candle.setTicks(candle.getTicks()+1);
        }

        //updateCandle(book);
        //book.add(); //    public void add(String orderId, String side, Double price, Double volume) {
        //System.out.println(book.toString());
        System.out.println("================ \n");
    }

//    private void updateCandle(Book book) {
//        Candle candle = candles.computeIfAbsent(symbol, k -> new Candle(currentMinute));
//
//    }

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
            // Create a WebSocket client (optional Draft configuration)
            WebSocketClient client = new KrakenClient(new URI("wss://ws.kraken.com/v2")); // or new EmptyClient(new URI("wss://ws.kraken.com/v2"), new Draft_6455());

            // Connect to the WebSocket server
            client.connectBlocking();

            // Send the subscription message after connection is established
            if (client.isOpen()) {
                client.send(subscriptionMessage);
                System.out.println("Sent Subscription Message: " + subscriptionMessage);
            }

//            // Keep the main thread alive while the connection is open
//            while (client.isOpen()) {
//                Thread.sleep(1000); // Check connection status periodically
//            }

            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(() -> {
                // Your code to be executed every minute
                System.out.println("This task runs every minute.");

                for (Map.Entry<String, Candle> entry : candles.entrySet()) {
                    System.out.println(entry.getValue().toString());
                }

            }, 0, 1, TimeUnit.MINUTES);


        } catch (Exception e) {
            System.err.println("Error connecting to WebSocket: " + e.getMessage());
        }
    }
}
import java.util.Collections;
import java.util.TreeMap;

public class OrderBookTreeMap {

    TreeMap<Double, Double> bids = new TreeMap<>(Collections.reverseOrder());
    TreeMap<Double, Double> asks = new TreeMap<>();

    public void add(String side, Double price, Double volume) {
        if ("B".equals(side)) {
            if (volume == 0) {
                bids.remove(price);
            } else {
                bids.put(price, volume);
            }
        } else {
            if (volume == 0) {
                asks.remove(price);
            } else {
                asks.put(price, volume);
            }
        }
    }

    public Double getBestBid() {
        if (bids.isEmpty()) {
            return -1.0; // prevent returning null, prevent NPE
        } else {
            return bids.firstKey();
        }
    }

    public Double getBestAsk() {
        if (asks.isEmpty()) {
            return -1.0; // prevent returning null, prevent NPE
        } else {
            return asks.firstKey();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Starting with the highest SELL price");
        sb.append(System.getProperty("line.separator"));

        for (Double key : asks.descendingKeySet()) {
            sb.append(key + " : " + asks.get(key));
            sb.append(System.getProperty("line.separator"));
        }
        sb.append("Bid Ask Spread");
        sb.append(System.getProperty("line.separator"));

        for(Double key : bids.keySet()) {
            sb.append(key + " : " + bids.get(key));
            sb.append(System.getProperty("line.separator"));
        }

        return sb.toString();
    }

}

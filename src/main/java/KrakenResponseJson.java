import com.google.gson.annotations.SerializedName;

import java.util.List;

public class KrakenResponseJson {
    @SerializedName("channel")
    private String channel;

    @SerializedName("type")
    private String type;

    @SerializedName("data")
    private List<Data> data;

    public String getChannel() {
        return channel;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return "KrakenMessage{" +
                "channel='" + channel + '\'' +
                ", type='" + type + '\'' +
                ", data=" + data.get(0) +
                '}';
    }

    public List<Data> getData() {
        return data;
    }

    public static class Data {
        @SerializedName("symbol")
        private String symbol;

        @SerializedName("bids")
        private List<Order> bids;

        @SerializedName("asks")
        private List<Order> asks;

        @SerializedName("checksum")
        private long checksum;

        @SerializedName("timestamp")
        private String timestamp;

        public String getSymbol() {
            return symbol;
        }

        public List<Order> getBids() {
            return bids;
        }

        public List<Order>getAsks() {
            return asks;
        }

        public long getChecksum() {
            return checksum;
        }

        public String getTimestamp() {
            return timestamp;
        }
        @Override
        public String toString() {
            return "Data{" +
                    "symbol='" + symbol + '\'' +
                    ", bids=" + bids +
                    ", asks=" + asks +
                    ", checksum=" + checksum +
                    '}';
        }

    }

    public static class Order {
        @SerializedName("price")
        private double price;

        @SerializedName("qty")
        private double quantity;

        public double getPrice() {
            return price;
        }

        public double getQuantity() {
            return quantity;
        }

        @Override
        public String toString() {
            return "Order{" +
                    "price=" + price +
                    ", quantity=" + quantity +
                    '}';
        }
    }
}

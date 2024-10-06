public class Candle {
    private String timestamp;
    private double open;
    private double high;
    private double low;
    private double close;
    private int ticks;

    public Candle(String startTime) {
        this.timestamp = startTime;
        this.open = -1.0;
        this.high = Double.MIN_VALUE; // default value
        this.low = Double.MAX_VALUE; // default value
        this.ticks = 0;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public double getOpen() {
        return open;
    }

    public double getHigh() {
        return high;
    }

    public double getLow() {
        return low;
    }

    public double getClose() {
        return close;
    }

    public int getTicks() {
        return ticks;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void setOpen(double open) {
        this.open = open;
    }

    public void setHigh(double high) {
        this.high = high;
    }

    public void setLow(double low) {
        this.low = low;
    }

    public void setClose(double close) {
        this.close = close;
    }

    public void setTicks(int ticks) {
        this.ticks = ticks;
    }

    @Override
    public String toString() {
        return "Candle{" +
                "timestamp=" + timestamp +
                ", open=" + String.format("%.6f", open) +
                ", high=" + String.format("%.6f",high) +
                ", low=" + String.format("%.6f", low) +
                ", close=" + String.format("%.6f", close) +
                ", ticks=" + ticks +
                '}';
    }
}
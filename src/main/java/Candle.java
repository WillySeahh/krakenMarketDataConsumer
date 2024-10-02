public class Candle {
    private String startTime;
    private double open;
    private double high;
    private double low;
    private double close;
    private double volume;
    private int ticks;

    public Candle(String startTime) {

        this.startTime = startTime;
        this.open = -1.0;
        this.high = Double.MIN_VALUE; // default value
        this.low = Double.MAX_VALUE; // default value
        this.ticks = 0;


    }

    public String getStartTime() {
        return startTime;
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

    public double getVolume() {
        return volume;
    }

    public int getTicks() {
        return ticks;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
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

    public void setVolume(double volume) {
        this.volume = volume;
    }

    public void setTicks(int ticks) {
        this.ticks = ticks;
    }

    @Override
    public String toString() {
        return "Candle{" +
                "startTime=" + startTime +
                ", open=" + String.format("%.6f", open) +
                ", high=" + String.format("%.6f",high) +
                ", low=" + String.format("%.6f", low) +
                ", close=" + String.format("%.6f", close) +
                ", volume=" + volume +
                ", ticks=" + ticks +
                '}';
    }
}
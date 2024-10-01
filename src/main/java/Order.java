public class Order {

    String orderId;
    public String side;
    public Double price;
    public Double volume;

    Order prevOrder = null;
    Order nextOrder  = null;

    public Order(String orderId, String side, Double price, Double volume, Order prevOrder, Order nextOrder) {
        this.orderId = orderId;
        this.side = side;
        this.price = price;
        this.volume = volume;
        this.prevOrder = prevOrder;
        this.nextOrder = nextOrder;
    }

    @Override
    public String toString() {
        return " ( " + this.volume + " ) ";
    }

}

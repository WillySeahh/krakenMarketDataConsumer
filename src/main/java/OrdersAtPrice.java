public class OrdersAtPrice {

    public String side;
    public Double price;

    public Order firstOrder = null;

    OrdersAtPrice prevEntry = null;
    OrdersAtPrice nextEntry = null;

    public OrdersAtPrice(String side, Double price, Order firstOrder, OrdersAtPrice prev, OrdersAtPrice next) {
        this.side = side;
        this.price = price;
        this.firstOrder = firstOrder;
        this.prevEntry = prev;
        this.nextEntry = next;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.side + " Price:" + this.price);

        Order temp = firstOrder;
        sb.append(temp.toString());

        temp = temp.nextOrder;

        while (temp != firstOrder) {
            sb.append(temp.toString());
            temp = temp.nextOrder;

        }

        return sb.toString();


    }


}

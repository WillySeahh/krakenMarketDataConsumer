import java.util.HashMap;

public class Book {

    OrdersAtPrice bidsByPrice = null;
    OrdersAtPrice asksByPrice = null;

    HashMap<Double, OrdersAtPrice> ordersAtPriceHashMap = new HashMap<>();


    public void add(String orderId, String side, Double price, Double volume) {
        Order newOrder = new Order(orderId, side, price, volume, null, null);
        addOrder(newOrder);
    }

    //Below is the book's internal implementation
    private OrdersAtPrice getOrdersAtPrice(Double price) {
        return ordersAtPriceHashMap.get(price);
    }

    private void addOrder(Order order) {

        OrdersAtPrice ordersAtPrice = getOrdersAtPrice(order.price);

        if (ordersAtPrice == null) {
            order.nextOrder = order.prevOrder = order;
            OrdersAtPrice newOrdersAtPrice = new OrdersAtPrice(order.side, order.price, order, null, null);
            addOrdersAtPrice(newOrdersAtPrice);
        } else {
            // TODO need to change because there is no longer a linked list, changed already

//            Order firstOrder = ordersAtPrice.firstOrder;
//            firstOrder.prevOrder.nextOrder = order;
//            order.prevOrder = firstOrder.prevOrder;
//            order.nextOrder = firstOrder;
//            firstOrder.prevOrder = order;
            order.nextOrder = order.prevOrder = order;
            if (order.volume == 0) {
                removeOrder(order);
            } else {
                ordersAtPrice.firstOrder = order;
            }


        }
    }

    private void removeOrder(Order order) {
        OrdersAtPrice ordersAtPrice = getOrdersAtPrice(order.price);

        if (order.prevOrder == order){
            removeOrdersAtPrice(order.side, order.price);
        } else {
            Order orderBefore = order.prevOrder;
            Order orderAfter = order.nextOrder;
            orderBefore.nextOrder = orderAfter;
            orderAfter.prevOrder = orderBefore;
            if (ordersAtPrice.firstOrder == order){
                ordersAtPrice.firstOrder = orderAfter;
            }
        }
    }


    private void addOrdersAtPrice(OrdersAtPrice newOrdersAtPrice) {

        ordersAtPriceHashMap.put(newOrdersAtPrice.price, newOrdersAtPrice);

        OrdersAtPrice bestOrdersByPrice = (newOrdersAtPrice.side.equals("B")) ? bidsByPrice : asksByPrice;

        if (bestOrdersByPrice == null) {

            if (newOrdersAtPrice.side.equals("B")) {
                bidsByPrice = newOrdersAtPrice;
            } else {
                asksByPrice = newOrdersAtPrice;
            }
            newOrdersAtPrice.nextEntry = newOrdersAtPrice;
            newOrdersAtPrice.prevEntry = newOrdersAtPrice;
        } else {

            OrdersAtPrice target = bestOrdersByPrice;

            boolean addAfter = ((newOrdersAtPrice.side.equals("S") && newOrdersAtPrice.price > target.price) ||
                    (newOrdersAtPrice.side.equals("B") && newOrdersAtPrice.price < target.price));


            if (addAfter) {
                target = target.nextEntry;
                addAfter = ((newOrdersAtPrice.side.equals("S") && newOrdersAtPrice.price > target.price) ||
                        (newOrdersAtPrice.side.equals("B") && newOrdersAtPrice.price < target.price));

                if (addAfter) {
                    target = target.nextEntry;
                }
            }

            while (addAfter && target != bestOrdersByPrice) {
                addAfter = ((newOrdersAtPrice.side.equals("S") && newOrdersAtPrice.price > target.price) ||
                        (newOrdersAtPrice.side.equals("B") && newOrdersAtPrice.price < target.price));

                if (addAfter) {
                    target = target.nextEntry;
                }
            }

            if (addAfter) {
                if (target == bestOrdersByPrice) {
                    target = bestOrdersByPrice.prevEntry;
                }
                newOrdersAtPrice.prevEntry = target;
                target.nextEntry.prevEntry = newOrdersAtPrice;
                newOrdersAtPrice.nextEntry = target.nextEntry;
                target.nextEntry = newOrdersAtPrice;
            } else {
                newOrdersAtPrice.prevEntry = target.prevEntry;
                newOrdersAtPrice.nextEntry = target;
                target.prevEntry.nextEntry = newOrdersAtPrice;
                target.prevEntry = newOrdersAtPrice;

                if ((newOrdersAtPrice.side.equals("B") && newOrdersAtPrice.price > bestOrdersByPrice.price) ||
                        (newOrdersAtPrice.side.equals("S") && newOrdersAtPrice.price < bestOrdersByPrice.price)) {

                    target.nextEntry = (target.nextEntry == bestOrdersByPrice ? newOrdersAtPrice : target.nextEntry);


                    if (newOrdersAtPrice.side.equals("B")) {
                        bidsByPrice = newOrdersAtPrice;
                    } else {
                        asksByPrice = newOrdersAtPrice;
                    }

                }
            }


        }

    }

    private void removeOrdersAtPrice(String side, Double price) {
        OrdersAtPrice bestOrdersAtPrice = (side.equals("B")) ? bidsByPrice : asksByPrice;

        OrdersAtPrice ordersAtPrice = getOrdersAtPrice(price);

        if (ordersAtPrice.nextEntry == ordersAtPrice) {
            if (side.equals("B")) {
                bidsByPrice = null;
            } else {
                asksByPrice = null;
            }
        } else {

            ordersAtPrice.prevEntry.nextEntry = ordersAtPrice.nextEntry;
            ordersAtPrice.nextEntry.prevEntry = ordersAtPrice.prevEntry;

            if (ordersAtPrice == bestOrdersAtPrice) {
                if (side.equals("B")) {
                    bidsByPrice = ordersAtPrice.nextEntry;
                } else {
                    asksByPrice = ordersAtPrice.nextEntry;
                }
            }

        }
        ordersAtPriceHashMap.remove(price);

    }

    @Override
    public String toString() {


        StringBuilder sb = new StringBuilder();
        sb.append("Starting with the highest SELL price");
        sb.append(System.getProperty("line.separator"));


        OrdersAtPrice lowestAsk = asksByPrice;
        if (lowestAsk != null) {

            OrdersAtPrice highestAsk = lowestAsk.prevEntry;
            sb.append(highestAsk.toString());
            sb.append(System.getProperty("line.separator"));

            OrdersAtPrice temp = highestAsk.prevEntry;

            while(temp != highestAsk) {
                sb.append(temp.toString());
                temp = temp.prevEntry;
                sb.append(System.getProperty("line.separator"));

            }

        } else {
            //no lowest ask
            sb.append("No SELL Orders");
            sb.append(System.getProperty("line.separator"));

        }

        sb.append("Bid Ask Spread");
        sb.append(System.getProperty("line.separator"));


        OrdersAtPrice highestBid = bidsByPrice;
        if (bidsByPrice != null) {

            sb.append(highestBid.toString());
            sb.append(System.getProperty("line.separator"));


            OrdersAtPrice temp = highestBid.nextEntry;
            while (temp != highestBid) {
                sb.append(temp.toString());
                temp = temp.nextEntry;
                sb.append(System.getProperty("line.separator"));

            }



        } else {
            sb.append("No BUY Orders");
            sb.append(System.getProperty("line.separator"));
        }

        return sb.toString();



    }


}

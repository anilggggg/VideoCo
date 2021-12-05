package warehouses;

import model.Order;

public class AlbertaWarehouse implements Warehouse {

    @Override
    public void ship(Order o) {
        System.out.println("Shipping Order:" + o.getOrderId() + " from Alberta Warehouse...");
    }

}

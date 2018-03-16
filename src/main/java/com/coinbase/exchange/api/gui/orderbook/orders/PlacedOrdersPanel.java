package com.coinbase.exchange.api.gui.orderbook.orders;

import com.coinbase.exchange.api.orders.Order;
import org.springframework.stereotype.Component;

import javax.swing.JList;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.Vector;

@Component
public class PlacedOrdersPanel extends JPanel {

    private JList placedOrdersList;
    private Vector dataModel;

    public PlacedOrdersPanel() {
        super();
        dataModel = new Vector();
        placedOrdersList = new JList(dataModel);
        this.setLayout(new BorderLayout());
    }

    public void init(){
        this.add(placedOrdersList, BorderLayout.CENTER);
    }

    public void update(Order updateOrder) {
        dataModel.add(updateOrder);
    }
}

package com.coinbase.exchange.api.gui.orderbook.orders;

import javax.swing.*;
import java.util.Vector;

public class PlacedOrdersPanel extends JPanel {

    private JTable placedOrdersTable;
    private Vector dataModel;

    public PlacedOrdersPanel() {
        dataModel = new Vector();
        placedOrdersTable = new JTable();


    }
}

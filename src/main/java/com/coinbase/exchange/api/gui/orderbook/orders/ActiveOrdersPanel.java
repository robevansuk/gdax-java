package com.coinbase.exchange.api.gui.orderbook.orders;

import com.coinbase.exchange.api.orders.Order;
import com.coinbase.exchange.api.orders.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.util.List;
import java.util.Vector;

@Component
public class ActiveOrdersPanel extends JPanel {

    private JList placedOrdersList;
    private Vector<Order> dataModel;
    private OrderService orderService;

    @Autowired
    public ActiveOrdersPanel(OrderService orderService) {
        super();
        this.orderService = orderService;
        this.dataModel = new Vector<>();
    }

    public JPanel init() {
        this.setLayout(new BorderLayout());

        initOutstandingOrders();

        placedOrdersList = new JList(dataModel);
        placedOrdersList.setVisibleRowCount(5);

        JScrollPane scrollPane = new JScrollPane(placedOrdersList);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        JPanel scrollPaneInPanel = new JPanel();
        scrollPaneInPanel.add(scrollPane);
        scrollPaneInPanel.setSize(1200, scrollPaneInPanel.getHeight());

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.add(new JLabel("Open Orders"));
        mainPanel.add(scrollPaneInPanel);
        this.add(mainPanel, BorderLayout.CENTER);
        return this;
    }

    private void initOutstandingOrders() {
        dataModel.clear();
        List<Order> orders = orderService.getOpenOrders();
        dataModel.addAll(orders);
    }

    public void update(Order updateOrder) {
        dataModel.add(updateOrder);
        placedOrdersList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                Order o = (Order) e.getSource();
                String response = orderService.cancelOrder(o.getId());
                dataModel.remove(e.getSource());
            }
        });
    }
}

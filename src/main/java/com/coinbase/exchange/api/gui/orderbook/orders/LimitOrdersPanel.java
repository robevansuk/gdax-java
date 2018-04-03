package com.coinbase.exchange.api.gui.orderbook.orders;

import com.coinbase.exchange.api.gui.orderbook.GdaxLiveOrderBook;
import com.coinbase.exchange.api.orders.Order;
import com.coinbase.exchange.api.orders.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import static com.coinbase.exchange.api.constants.GdaxConstants.LIMIT;

@Component
public class LimitOrdersPanel extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(LimitOrdersPanel.class);

    private GdaxLiveOrderBook liveOrderBook;
    private JTextField funds;
    private OrderService orderService;
    private BalancePanel balancePanel;
    private ActiveOrdersPanel placeOrdersPanel;
    private JTextField limitPriceField;
    private JTextField amountOfCurrencyField;
    private JButton buyButton;
    private JButton sellButton;
    private JLabel placeOrder;
    private JLabel total;

    @Autowired
    public LimitOrdersPanel(GdaxLiveOrderBook liveOrderBook,
                            OrderService orderService,
                            BalancePanel balancePanel,
                            ActiveOrdersPanel placeOrdersPanel) {
        super();
        this.liveOrderBook = liveOrderBook;
        this.orderService = orderService;
        this.balancePanel = balancePanel;
        this.placeOrdersPanel = placeOrdersPanel;
    }

    public void init() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JPanel orderSidePanel = new JPanel();
        orderSidePanel.setLayout(new GridLayout(1, 2));

        placeOrder = new JLabel("Place Buy Order");

        placeOrder.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Order marketOrder = createLimitOrder(liveOrderBook.getSelectedProductId());
                Order responseOrder = orderService.createOrder(marketOrder);
                log.info("ORDER PLACED: {}", responseOrder.toString());
                placeOrdersPanel.update(responseOrder);
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });

        buyButton = new JButton("BUY");
        buyButton.setForeground(Color.GREEN);
        sellButton = new JButton("SELL");
        sellButton.setForeground(Color.GRAY);

        total = new JLabel("0.00");

        buyButton.addActionListener((ActionEvent e) -> {
            buyButton.setForeground(Color.GREEN);
            sellButton.setForeground(Color.GRAY);
            balancePanel.getCurrencyLabel().setText(balancePanel.getFromCurrency() + " ");
            placeOrder.setText("Place Buy Order");
            placeOrder.setForeground(Color.GREEN);
            total.setText("0.00000000 " + balancePanel.getFromCurrency());
        });
        sellButton.addActionListener((ActionEvent e) -> {
            sellButton.setForeground(Color.RED);
            buyButton.setForeground(Color.GRAY);
            balancePanel.getCurrencyLabel().setText(balancePanel.getToCurrency() + " ");
            placeOrder.setText("Place Sell Order");
            placeOrder.setForeground(Color.RED);
            total.setText("0.00 " + balancePanel.getToCurrency());
        });

        amountOfCurrencyField = new JTextField("", 20);
        amountOfCurrencyField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                validate();
            }

            @Override
            public void keyPressed(KeyEvent e) {
                validate();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                validate();
            }

            public void validate() {
                if (amountOfCurrencyField.getText().length() == 0) {
                    amountOfCurrencyField.setBackground(Color.PINK);
                } else if (!amountOfCurrencyField.getText().matches("[0-9]*[\\.]*[0-9]*")) {
                    amountOfCurrencyField.setBackground(Color.PINK);
                } else {
                    amountOfCurrencyField.setBackground(Color.WHITE);
                }
            }
        });
        JPanel textFieldPanel = new JPanel();
        textFieldPanel.add(amountOfCurrencyField);

        limitPriceField = new JTextField("", 20);
        limitPriceField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                validate();
            }

            @Override
            public void keyPressed(KeyEvent e) {
                validate();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                validate();
            }

            public void validate() {
                if (limitPriceField.getText().length() == 0) {
                    limitPriceField.setBackground(Color.WHITE);
                } else if (!limitPriceField.getText().matches("[0-9]*[\\.]*[0-9]*")) {
                    limitPriceField.setBackground(Color.PINK);
                } else {
                    limitPriceField.setBackground(Color.WHITE);
                }
            }
        });
        JPanel limitPriceFieldPanel = new JPanel();
        limitPriceFieldPanel.add(limitPriceField);

        orderSidePanel.add(buyButton);
        orderSidePanel.add(sellButton);
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.add(orderSidePanel);

        JPanel totalPanel = new JPanel();
        totalPanel.add(total);
        JPanel placeOrderPanel = new JPanel();
        placeOrderPanel.add(placeOrder);
        JPanel currencyLabelPanel = new JPanel();
        currencyLabelPanel.add(balancePanel.getCurrencyLabel());

        panel.add(buttonsPanel);
        panel.add(currencyLabelPanel);
        panel.add(textFieldPanel);
        panel.add(limitPriceFieldPanel);
        panel.add(totalPanel);
        panel.add(placeOrderPanel);

        JPanel mainPanel = new JPanel();
        mainPanel.add(panel);

        this.add(mainPanel);
    }

    private Order createLimitOrder(String productId) {
        Order order = new Order();
        order.setProduct_id(productId);
        order.setType(LIMIT.toLowerCase());
        order.setPrice(limitPriceField.getText());
        order.setFunds(amountOfCurrencyField.getText());
        log.info("Making Order: {}, {}, {}", order.getType(), order.getProduct_id(), order.getFunds());
        return order;
    }
}

package com.coinbase.exchange.api.gui.orderbook.orders;

import com.coinbase.exchange.api.gui.orderbook.GdaxLiveOrderBook;
import com.coinbase.exchange.api.orders.Order;
import com.coinbase.exchange.api.orders.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import static com.coinbase.exchange.api.constants.GdaxConstants.MARKET;

@Component
public class MarketOrdersPanel extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(MarketOrdersPanel.class);

    private GdaxLiveOrderBook liveOrderBook;
    private JTextField funds;
    private OrderService orderService;
    private BalancePanel balancePanel;
    private PlacedOrdersPanel placeOrdersPanel;
    private JTextField amountOfCurrency;
    private JButton buyButton;
    private JButton sellButton;
    private JLabel placeOrder;
    private JLabel total;

    @Autowired
    public MarketOrdersPanel(GdaxLiveOrderBook liveOrderBook,
                             OrderService orderService,
                             BalancePanel balancePanel,
                             PlacedOrdersPanel placeOrdersPanel){
        super();
        this.liveOrderBook = liveOrderBook;
        this.orderService = orderService;
        this.balancePanel = balancePanel;
        this.placeOrdersPanel = placeOrdersPanel;
    }

    public void init(){
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JPanel orderSidePanel = new JPanel();
        orderSidePanel.setLayout(new GridLayout(1, 2));

        placeOrder = new JLabel("Place Buy Order");

        placeOrder.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Order marketOrder = createMarketOrder(liveOrderBook.getSelectedProductId());
                Order responseOrder = orderService.createOrder(marketOrder);
                log.info("ORDER PLACED: {}", responseOrder.toString());
                placeOrdersPanel.update(responseOrder);
            }
            @Override
            public void mousePressed(MouseEvent e) {}
            @Override
            public void mouseReleased(MouseEvent e) {}
            @Override
            public void mouseEntered(MouseEvent e) {}
            @Override
            public void mouseExited(MouseEvent e) {}
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

        amountOfCurrency = new JTextField("" ,20);
        amountOfCurrency.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                // TODO - validate you have enough currency whichever way you're attempting to trade
                if (amountOfCurrency.getText().length() == 0) {
                    amountOfCurrency.setBackground(Color.PINK);
                } else if (!amountOfCurrency.getText().matches("[0-9]+[\\.][0-9]*")) {
                    amountOfCurrency.setBackground(Color.PINK);
                } else {
                    amountOfCurrency.setBackground(Color.WHITE);
                }
            }

            @Override
            public void keyPressed(KeyEvent e) { }
            @Override
            public void keyReleased(KeyEvent e) { }
        });
        JPanel textFieldPanel = new JPanel();
        textFieldPanel.add(amountOfCurrency);

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
        panel.add(totalPanel);
        panel.add(placeOrderPanel);

        JPanel mainPanel = new JPanel();
        mainPanel.add(panel);

        this.add(mainPanel);
    }

    private Order createMarketOrder(String productId) {
        Order order = new Order();
        order.setProduct_id(productId);
        order.setType(MARKET.toLowerCase());
        order.setFunds(amountOfCurrency.getText());
        log.info("Making Order: {}, {}, {}", order.getType(), order.getProduct_id(), order.getFunds());
        return order;
    }
}

package com.coinbase.exchange.api.gui.orderbook.orders;

import com.coinbase.exchange.api.accounts.Account;
import com.coinbase.exchange.api.accounts.AccountService;
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
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.coinbase.exchange.api.constants.GdaxConstants.*;

@Component
public class MakeOrdersPanel extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(MakeOrdersPanel.class);

    private Map<String, String> accountsMap;
    private JTabbedPane tabs;
    private String[] gdaxTabNames = new String[]{MARKET, LIMIT, STOP};
    private String[] tabNames = new String[]{BUY, SELL};
    private GdaxLiveOrderBook liveOrderBook;
    private OrderService orderService;
    private AccountService accountService;
    private JTextField funds;
    private JTextField amountOfCurrency;
    private JButton buyButton;
    private JLabel fromBalanceLabel;
    private JLabel toBalanceLabel;
    private JButton sellButton;
    private String fromCurrency;
    private String toCurrency;
    private String selectedProductId;
    private JLabel currencyLabel;
    private JLabel placeOrder;
    private JLabel total;
    private String toCurrencyBalance;
    private String fromCurrencyBalance;

    @Autowired
    public MakeOrdersPanel(GdaxLiveOrderBook liveOrderBook, OrderService orderService, AccountService accountService) {
        super();
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.liveOrderBook = liveOrderBook;
        this.orderService = orderService;
        this.accountService = accountService;
        selectedProductId = liveOrderBook.getSelectedProductId();
        fromCurrency = selectedProductId.split("-")[0];
        toCurrency = selectedProductId.split("-")[1];
    }

    public JPanel init() {
        this.accountsMap = getAccountIdsMap();
        this.add(getBalanceLabel(fromCurrency, toCurrency));
        this.add(createOrderTabs());
        return this;
    }

    private JTabbedPane createOrderTabs() {
        int i = 0;
        tabs = new JTabbedPane();
        for (String tabName : gdaxTabNames){

            if (tabName.equals(MARKET)) {
                tabs.addTab(tabName, null, getMarketPanel(),"Make " + gdaxTabNames[i] + " Orders");
            } else if (tabName.equals(LIMIT)) {
                tabs.addTab(tabName, null, createLimitTab(),"Make " + gdaxTabNames[i] + " Orders");
            } else if (tabName.equals(STOP)) {
                tabs.addTab(tabName, null, createStopTab(),"Make " + gdaxTabNames[i] + " Orders");
            }

            tabs.setMnemonicAt(i, KeyEvent.VK_1+i);
            i++;
        }
        return tabs;
    }

    public JPanel getMarketPanel(){
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JPanel orderSidePanel = new JPanel();
        orderSidePanel.setLayout(new GridLayout(1, 2));

        placeOrder = new JLabel("Place Buy Order");

        placeOrder.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                createMarketOrder(selectedProductId);
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

        currencyLabel = new JLabel(fromCurrency + " ");
        total = new JLabel("0.00000000 " + fromCurrency);

        buyButton.addActionListener((ActionEvent e) -> {
            buyButton.setForeground(Color.GREEN);
            sellButton.setForeground(Color.GRAY);
            currencyLabel.setText(fromCurrency + " ");
            placeOrder.setText("Place Buy Order");
            placeOrder.setForeground(Color.GREEN);
            total.setText("0.00000000 " + fromCurrency);
        });
        sellButton.addActionListener((ActionEvent e) -> {
            sellButton.setForeground(Color.RED);
            buyButton.setForeground(Color.GRAY);
            currencyLabel.setText(toCurrency + " ");
            placeOrder.setText("Place Sell Order");
            placeOrder.setForeground(Color.RED);
            total.setText("0.00 " + toCurrency);
        });

        amountOfCurrency = new JTextField("" ,20);
        amountOfCurrency.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                // TODO - should probably validate you have enough currency whichever way you're attempting to trade
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
        currencyLabelPanel.add(currencyLabel);

        panel.add(buttonsPanel);
        panel.add(currencyLabelPanel);
        panel.add(textFieldPanel);
        panel.add(totalPanel);
        panel.add(placeOrderPanel);

        JPanel mainPanel = new JPanel();
        mainPanel.add(panel);

        return mainPanel;
    }

    private JPanel getBalanceLabel(String fromCurrency, String toCurrency) {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        JPanel fromPanel = new JPanel();
        fromPanel.setLayout(new BoxLayout(fromPanel, BoxLayout.X_AXIS));
        JLabel fromCurrencyLabel = new JLabel(fromCurrency + " ");
        fromCurrencyBalance = accountService.getAccount(accountsMap.get(fromCurrency)).getAvailable().toPlainString();
        BigDecimal fromCurrencyBalanceRounded = new BigDecimal(fromCurrencyBalance).setScale(PRICE_DECIMAL_PLACES, BigDecimal.ROUND_HALF_UP);

        fromBalanceLabel = new JLabel(fromCurrencyBalanceRounded.toString());
        fromPanel.add(fromCurrencyLabel);
        fromPanel.add(fromBalanceLabel);

        JPanel toPanel = new JPanel();
        toPanel.setLayout(new BoxLayout(toPanel, BoxLayout.X_AXIS));
        JLabel toCurrencyLabel = new JLabel(toCurrency + " ");
        toCurrencyBalance = accountService.getAccount(accountsMap.get(toCurrency)).getAvailable().toPlainString();
        BigDecimal toCurrencyBalanceRounded = new BigDecimal(toCurrencyBalance).setScale(PRICE_DECIMAL_PLACES, BigDecimal.ROUND_HALF_UP);
        toBalanceLabel = new JLabel(toCurrencyBalanceRounded.toString());
        toPanel.add(toCurrencyLabel);
        toPanel.add(toBalanceLabel);

        mainPanel.add(fromPanel);
        mainPanel.add(toPanel);
        return mainPanel;
    }

    private Order createMarketOrder(String productId) {
        Order order = new Order();
        order.setProduct_id(productId);
        order.setType(MARKET);
        order.setFunds(amountOfCurrency.getText());
        log.info("Making Order: {}, {}, {}", order.getType(), order.getProduct_id(), order.getFunds());
        return order;
    }

    public JPanel createLimitTab(){
        JPanel panel = new JPanel();
        return panel;
    }

    public JPanel createStopTab(){
        JPanel panel = new JPanel();
        return panel;
    }

    public int findTab(String name) {
        int tabId=0;
        for (String tabName : tabNames){
            if (tabName.equals(name)) {
                break;
            }
            tabId++;
        }
        return tabId;
    }

    public Map<String, String> getAccountIdsMap() {
        Map<String, String> accountIdsMap = new HashMap<>();
        List<Account> accounts = accountService.getAccounts();
        for (Account account : accounts) {
            accountIdsMap.put(account.getCurrency(), account.getId());
        }
        return accountIdsMap;
    }
}

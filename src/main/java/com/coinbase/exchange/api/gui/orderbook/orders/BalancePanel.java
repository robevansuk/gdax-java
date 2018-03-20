package com.coinbase.exchange.api.gui.orderbook.orders;

import com.coinbase.exchange.api.accounts.Account;
import com.coinbase.exchange.api.accounts.AccountService;
import com.coinbase.exchange.api.gui.orderbook.GdaxLiveOrderBook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.coinbase.exchange.api.constants.GdaxConstants.PRICE_DECIMAL_PLACES;

@Component
public class BalancePanel extends JPanel {

    private Map<String, String> accountsMap;
    private GdaxLiveOrderBook liveOrderBook;
    private AccountService accountService;

    private String fromCurrency;
    private String toCurrency;
    private String toCurrencyBalance;
    private String fromCurrencyBalance;

    private JLabel currencyLabel;
    private JLabel fromBalanceLabel;
    private JLabel toBalanceLabel;
    private JLabel total;

    @Autowired
    public BalancePanel(GdaxLiveOrderBook liveOrderBook, AccountService accountService) {
        super();
        this.liveOrderBook = liveOrderBook;
        this.accountService = accountService;
        this.accountsMap = getAccountIdsMap();
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    public void init(){
        fromCurrency = liveOrderBook.getSelectedProductId().split("-")[0];
        toCurrency = liveOrderBook.getSelectedProductId().split("-")[1];
        currencyLabel = new JLabel(fromCurrency + " ");
        total = new JLabel("0.00000000 " + fromCurrency);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        JPanel fromPanel = new JPanel();
        fromPanel.setLayout(new BoxLayout(fromPanel, BoxLayout.X_AXIS));

        JLabel fromCurrencyLabel = new JLabel(fromCurrency + " ");
        try {
            fromCurrencyBalance = accountService.getAccount(accountsMap.get(fromCurrency)).getAvailable().toPlainString();
        } catch (NullPointerException e) {
            fromCurrencyBalance = "0";
        }
        BigDecimal fromCurrencyBalanceRounded = new BigDecimal(fromCurrencyBalance).setScale(PRICE_DECIMAL_PLACES, BigDecimal.ROUND_HALF_UP);

        fromBalanceLabel = new JLabel(fromCurrencyBalanceRounded.toString());
        fromPanel.add(fromCurrencyLabel);
        fromPanel.add(fromBalanceLabel);

        JPanel toPanel = new JPanel();
        toPanel.setLayout(new BoxLayout(toPanel, BoxLayout.X_AXIS));
        JLabel toCurrencyLabel = new JLabel(toCurrency + " ");
        try {
            toCurrencyBalance = accountService.getAccount(accountsMap.get(toCurrency)).getAvailable().toPlainString();
        } catch (NullPointerException e) {
            toCurrencyBalance = "0";
        }
        BigDecimal toCurrencyBalanceRounded = new BigDecimal(toCurrencyBalance).setScale(PRICE_DECIMAL_PLACES, BigDecimal.ROUND_HALF_UP);
        toBalanceLabel = new JLabel(toCurrencyBalanceRounded.toString());
        toPanel.add(toCurrencyLabel);
        toPanel.add(toBalanceLabel);

        mainPanel.add(fromPanel);
        mainPanel.add(toPanel);
        this.add(mainPanel);
    }

    public Map<String, String> getAccountIdsMap() {
        Map<String, String> accountIdsMap = new HashMap<>();
        List<Account> accounts = accountService.getAccounts();
        for (Account account : accounts) {
            accountIdsMap.put(account.getCurrency(), account.getId());
        }
        return accountIdsMap;
    }

    public JLabel getCurrencyLabel() {
        return currencyLabel;
    }

    public String getFromCurrency() {
        return fromCurrency;
    }

    public String getToCurrency() {
        return toCurrency;
    }
}

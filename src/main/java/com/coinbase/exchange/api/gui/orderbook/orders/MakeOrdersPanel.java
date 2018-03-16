package com.coinbase.exchange.api.gui.orderbook.orders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.event.KeyEvent;

import static com.coinbase.exchange.api.constants.GdaxConstants.*;

@Component
public class MakeOrdersPanel extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(MakeOrdersPanel.class);
    private String[] gdaxTabNames = new String[]{MARKET, LIMIT, STOP};
    private String[] tabNames = new String[]{BUY, SELL};
    private MarketOrdersPanel marketOrdersPanel;
    private BalancePanel balancePanel;
    private JTabbedPane tabs;

    @Autowired
    public MakeOrdersPanel(MarketOrdersPanel marketOrdersPanel,
                            BalancePanel balancePanel) {
        super();
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.balancePanel = balancePanel;
        this.marketOrdersPanel = marketOrdersPanel;
    }

    public JPanel init() {
        balancePanel.init();
        marketOrdersPanel.init();

        this.add(balancePanel);
        this.add(createOrderTabs());
        return this;
    }

    private JTabbedPane createOrderTabs() {
        int i = 0;
        tabs = new JTabbedPane();
        for (String tabName : gdaxTabNames){

            if (tabName.equals(MARKET)) {
                tabs.addTab(tabName, null, marketOrdersPanel,"Make " + gdaxTabNames[i] + " Orders");
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
}

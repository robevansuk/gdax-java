package com.coinbase.exchange.api.gui.orderbook.info;

import com.coinbase.exchange.api.gui.orderbook.GdaxLiveOrderBook;
import org.springframework.stereotype.Component;

import javax.swing.*;

@Component
public class StatsPanel {

    private static JPanel panel;
    private GdaxLiveOrderBook liveOrderBook;

    private StatsPanel() {

    }

    public JPanel getInstance(GdaxLiveOrderBook liveOrderBook){
        this.liveOrderBook = liveOrderBook;
        panel = new JPanel();
        panel.add(new JLabel("This will be the new stats panel"));
        return panel;
    }

}

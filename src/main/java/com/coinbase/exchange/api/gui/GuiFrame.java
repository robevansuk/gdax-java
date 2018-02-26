package com.coinbase.exchange.api.gui;


import com.coinbase.exchange.api.gui.orderbook.OrderBookView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.awt.*;

import static javax.swing.JFrame.EXIT_ON_CLOSE;

/**
 * Created by robevansuk on 10/03/2017.
 */

@Component
public class GuiFrame {

    private static final Logger log = LoggerFactory.getLogger(GuiFrame.class);

    private OrderBookView orderBookView;
    private Boolean guiEnabled;

    private JFrame frame;

    @Autowired
    public GuiFrame(@Value("${gui.enabled}") boolean guiEnabled, OrderBookView orderBookView) {
        this.guiEnabled = guiEnabled;
        this.orderBookView = orderBookView;
    }

    public void init() {
        if (guiEnabled) {
            frame = new JFrame("Gdax Desktop Client");
            frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
            frame.setSize(700, 480);
            frame.setLayout(new BorderLayout());
            frame.add(orderBookView.init(), BorderLayout.CENTER);
            frame.setVisible(true);
            log.info("Frame initiation complete!");
        }
    }
}

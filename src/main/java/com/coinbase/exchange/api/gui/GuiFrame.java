package com.coinbase.exchange.api.gui;

import com.coinbase.exchange.api.gui.orderbook.OrderBookView;
import com.coinbase.exchange.api.gui.orderbook.menubar.MainMenu;
import com.coinbase.exchange.api.gui.orderbook.orders.MakeOrdersPanel;
import com.coinbase.exchange.api.products.ProductService;
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
    private MakeOrdersPanel makeOrdersPanel;
    private ProductService productService;
    private Boolean guiEnabled;

    private JFrame frame;

    @Autowired
    public GuiFrame(@Value("${gui.enabled}") boolean guiEnabled,
                    OrderBookView orderBookView,
                    MakeOrdersPanel makeOrdersPanel,
                    ProductService productService) {
        this.guiEnabled = guiEnabled;
        this.orderBookView = orderBookView;
        this.makeOrdersPanel = makeOrdersPanel;
        this.productService = productService;
    }

    public void init() {
        if (guiEnabled) {
            frame = new JFrame("Gdax Desktop Client");
            frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
            frame.setSize(1200, 480);
            frame.setLayout(new BorderLayout());
            frame.add(orderBookView.init(), BorderLayout.EAST);
            frame.add(makeOrdersPanel.init(), BorderLayout.WEST);
            frame.setJMenuBar(new MainMenu(productService, orderBookView.getLiveOrderBook()));
            frame.setVisible(true);
            log.info("Frame initiation complete!");
        }
    }
}

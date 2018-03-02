package com.coinbase.exchange.api.gui.orderbook;

import com.coinbase.exchange.api.entity.Product;
import com.coinbase.exchange.api.products.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.List;

import static javax.swing.BoxLayout.X_AXIS;

@Component
public class OrderBookView extends JPanel {

    static final Logger log = LoggerFactory.getLogger(OrderBookView.class);

    private String productId;
    private ProductService productService;
    private GdaxLiveOrderBook liveOrderBook;
    private JPanel liveOrderBookPanel;

    /**
     * Used by test code
     */
    public OrderBookView() {
        this.liveOrderBook = new GdaxLiveOrderBook();
    }

    @Autowired
    public OrderBookView(@Value("${liveorderbook.defaultProduct}") String productId,
                         ProductService productService,
                         GdaxLiveOrderBook liveOrderBook) {
        super();
        this.productId = productId;
        this.productService = productService;
        this.liveOrderBook = liveOrderBook;
        this.liveOrderBookPanel = new JPanel();
    }

    public JPanel init() {
        setLayout(new BorderLayout());
        add(getButtonsPanel(), BorderLayout.NORTH);
        add(reload(), BorderLayout.EAST);
        revalidate();
        repaint();
        return this;
    }

    public JPanel reload() {
        liveOrderBookPanel.removeAll();
        liveOrderBook.loadLiveOrderBookModel(productId);
        liveOrderBookPanel.add(getLiveOrderBookPanel());
        return liveOrderBookPanel;
    }

    private JPanel getButtonsPanel() {
        // init to the websocket feeds gone at a time - a bit more lag in terms of booting up an order book
        // but should mean the network is not clogged up with orders for feeds we're not looking at.
        List<Product> products = productService.getProducts();
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, X_AXIS));

        for (Product prod : products) {
            JButton button = new JButton(prod.getId());
            button.addActionListener(event -> {
                if (event.getActionCommand().equals(button.getText())) {
                    SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() {
                            productId = event.getActionCommand();
                            reload();
                            return null;
                        }

                        @Override
                        protected void done() {
                            revalidate();
                            repaint();
                        }
                    };
                    worker.execute();
                }
            });
            buttonsPanel.add(button);
        }
        return buttonsPanel;
    }

    private JPanel getLiveOrderBookPanel() {
        liveOrderBookPanel = new JPanel();
        liveOrderBookPanel.setLayout(new BoxLayout(liveOrderBookPanel, X_AXIS));

        OrderBookModel bids = liveOrderBook.getBids();
        OrderBookModel asks = liveOrderBook.getAsks();

        liveOrderBookPanel.add(getTableInScrollPaneWithLabel(bids));
        liveOrderBookPanel.add(getTableInScrollPaneWithLabel(asks));
        return liveOrderBookPanel;
    }

    private JPanel getTableInScrollPaneWithLabel(OrderBookModel orderBookModel) {

        JTable table = new JTable(orderBookModel);

        GdaxTableCellRenderer cellRenderer = new GdaxTableCellRenderer();
        for (int i = 0; i < table.getColumnCount(); i++) {
            TableColumn column = table.getColumnModel().getColumn(i);
            column.setCellRenderer(cellRenderer);
        }
        orderBookModel.setCellRenderer(cellRenderer);

        if (orderBookModel.equals(liveOrderBook.getBids())) {
            return getTableInScrollPaneWithLabel(table, "Bids");
        } else {
            return getTableInScrollPaneWithLabel(table, "Asks");
        }
    }

    private JPanel getTableInScrollPaneWithLabel(JTable table, String labelText) {
        JPanel panel = new JPanel();
        resizeColumns(table);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(new JLabel(labelText));
        JScrollPane scroller = new JScrollPane();
        scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scroller.setViewportBorder(BorderFactory.createEmptyBorder());
        scroller.setViewportView(table);
        table.setPreferredScrollableViewportSize(new Dimension(255, table.getParent().getHeight()));
        panel.add(scroller);
        return panel;
    }

    private void resizeColumns(JTable table) {
        int[] widths = {
                50, 50, 30
        };

        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setMinWidth(widths[i] * 2);
            table.getColumnModel().getColumn(i).setMaxWidth(widths[i] * 2);
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }
}

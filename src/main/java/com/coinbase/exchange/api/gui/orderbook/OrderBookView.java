package com.coinbase.exchange.api.gui.orderbook;

import com.coinbase.exchange.api.entity.Product;
import com.coinbase.exchange.api.gui.orderbook.ux.GdaxTableCellRenderer;
import com.coinbase.exchange.api.products.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;

import static javax.swing.BoxLayout.X_AXIS;

@Component
public class OrderBookView extends JPanel {

    static final Logger log = LoggerFactory.getLogger(OrderBookView.class);

    List<Product> products;
    private String productId;
    private ProductService productService;
    private GdaxLiveOrderBook liveOrderBook;
    private JPanel liveOrderBookPanel;
    private JLabel selectedProductLabel;

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

    public GdaxLiveOrderBook getLiveOrderBook() {
        return liveOrderBook;
    }
}

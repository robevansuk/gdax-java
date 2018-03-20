package com.coinbase.exchange.api.gui.orderbook.menubar;

import com.coinbase.exchange.api.entity.Product;
import com.coinbase.exchange.api.gui.orderbook.GdaxLiveOrderBook;
import com.coinbase.exchange.api.products.ProductService;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import java.awt.event.ActionEvent;
import java.util.List;

public class MainMenu extends JMenuBar {

    private ProductService productService;
    private GdaxLiveOrderBook liveOrderBook;
    private List<Product> products;

    public MainMenu(ProductService productService, GdaxLiveOrderBook liveOrderBook){
        super();
        this.productService = productService;
        this.products = productService.getProducts();
        this.liveOrderBook = liveOrderBook;
        initMenu();
    }

    private void initMenu() {
        JMenu menu = new JMenu("Products");
        this.add(menu);
        for (Product product: products) {
            JMenuItem menuItem = new JMenuItem(product.getId());
            menu.add(menuItem);
            menuItem.addActionListener((ActionEvent event)-> {
                liveOrderBook.loadLiveOrderBookModel(event.getActionCommand());
            });
        }
    }
}

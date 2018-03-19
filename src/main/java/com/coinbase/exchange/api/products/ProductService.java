package com.coinbase.exchange.api.products;

import com.coinbase.exchange.api.entity.Product;
import com.coinbase.exchange.api.exchange.GdaxExchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by robevansuk on 03/02/2017.
 */
@Component
public class ProductService {


    private GdaxExchange exchange;

    @Autowired
    public ProductService(GdaxExchange exchange) {
        this.exchange = exchange;
    }

    public static final String PRODUCTS_ENDPOINT = "/products";

    // no paged products necessary
    public List<Product> getProducts() {
        return exchange.getAsList(PRODUCTS_ENDPOINT, new ParameterizedTypeReference<Product[]>(){});
    }

    public List<List<BigDecimal>> getHistoricRates(String productId) {
        List<Object> response = exchange.getAsList(PRODUCTS_ENDPOINT + "/" + productId + "/candles", new ParameterizedTypeReference<Object[]>() {
        });

        List<List<BigDecimal>> result = new ArrayList<List<BigDecimal>>();
        for (Object rate : response) {
            List iResponse = (List<Object>) rate;
            List<BigDecimal> recordAsNumbers = new ArrayList<BigDecimal>();

            for (Object r : iResponse) {
                recordAsNumbers.add(new BigDecimal(r.toString()));
            }
            result.add(recordAsNumbers);
        }
        return result;
    }
}

package com.coinbase.exchange.api.constants;

import javax.crypto.Mac;
import java.security.NoSuchAlgorithmException;

/**
 * Created by robevansuk on 25/01/2017.
 */
public class GdaxConstants {

    public static final String DONE = "done";
    public static final String CANCELED = "canceled";
    public static final String MATCH = "match";
    public static final String OPEN = "open";
    public static final String FILLED = "filled";
    public static final int PRICE_DECIMAL_PLACES = 5;
    public static final int SIZE_DECIMAL_PLACES = 8;
    public static final int PRICE_COL = 0;
    public static final int SIZE_COL = 1;
    public static final int NUM_ORDERS_COL = 2;
    public static final String BUY = "buy";
    public static final String SELL = "sell";
    public static final String FULL_ORDER_BOOK = "3";
    public static final String LIMIT_ORDER_TYPE = "limit";
    public static final String BTC_GBP_PRODUCT = "BTC-GBP";
    public static final String RECEIVED = "received";
    public static final String CHANGE = "change";
    public static final String STOP = "STOP";
    public static final String MARKET = "Market";
    public static final String LIMIT = "Limit";

    public static Mac SHARED_MAC;

    static {
        try {
            SHARED_MAC = Mac.getInstance("HmacSHA256");
        } catch (NoSuchAlgorithmException nsaEx) {
            nsaEx.printStackTrace();
        }
    }
}

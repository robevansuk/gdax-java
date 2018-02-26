package com.coinbase.exchange.api.marketdata;

import com.coinbase.exchange.api.websocketfeed.message.OrderBookMessage;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * Created by robevansuk on 20/03/2017.
 */
public class OrderItem implements Comparable {

    private BigDecimal price;
    private BigDecimal size;
    private BigDecimal remainingSize;
    private String orderId; // a uuid that represents the individual order placed.
    private BigDecimal num;
    private String messageType;
    private String side;
    private String reason;

    public OrderItem(BigDecimal price, OrderBookMessage message) {
        this.price = price;
        this.size = message.getSize();
        this.remainingSize = message.getRemaining_size();
        this.orderId = message.getOrder_id();
        this.num = new BigDecimal(1);
        this.messageType = message.getType();
        this.side = message.getSide();
        this.reason = message.getReason();
    }

    @JsonCreator
    public OrderItem(List<String> limitOrders) {
        if (CollectionUtils.isEmpty(limitOrders) || limitOrders.size() < 3) {
            throw new IllegalArgumentException("LimitOrders was empty - check connection to the exchange");
        }
        price =  new BigDecimal(limitOrders.get(0));
        size = new BigDecimal(limitOrders.get(1));
        if (isString(limitOrders.get(2))) {
            orderId = limitOrders.get(2);
            num = new BigDecimal(1);
        } else {
            num = new BigDecimal(1);
        }
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getSize() {
        return size;
    }

    public String getOrderId() {
        return orderId;
    }

    public BigDecimal getNum() {
        return num;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    @Override
    public int compareTo(Object o) {
        return this.getPrice().compareTo(((OrderItem)o).getPrice()) * -1;
    }

    public boolean isString(String value) {
        boolean isDecimalSeparatorFound = false;

        for (char c : value.substring( 1 ).toCharArray()) {
            if (!Character.isDigit( c ) ) {
                if (c == '.' && !isDecimalSeparatorFound) {
                    isDecimalSeparatorFound = true;
                    continue;
                }
                return false;
            }
        }
        return true;
    }

    public BigDecimal getRemainingSize() {
        return remainingSize;
    }

    public String getReason() {
        return reason;
    }

    public void setSize(BigDecimal size) {
        this.size = size;
    }

    public void setRemainingSize(BigDecimal remainingSize) {
        this.remainingSize = remainingSize;
    }
}

package com.coinbase.exchange.api.gui.orderbook;

import com.coinbase.exchange.api.websocketfeed.message.OrderBookMessage;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static com.coinbase.exchange.api.constants.GdaxConstants.*;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by robevansuk on 31/03/2017.
 */
public class OrderBookModelTest {

    OrderBookModel testObject;

    @Before
    public void setup(){
        testObject = new OrderBookModel();
    }

    @Test
    public void shouldAddItemToTable() {
        testObject.setValueAt("Some Value", 0, 0);

        assertThat(testObject.getValueAt(0, PRICE_COL), equalTo("Some Value"));
        assertThat(testObject.getRowCount(), equalTo(1));
    }

    @Test
    public void shouldUpdateExistingRowWhenPricePointExists(){
        testObject.setValueAt("1.0", 0, PRICE_COL);
        testObject.setValueAt("1.0", 0, SIZE_COL);
        testObject.setValueAt("1", 0, NUM_ORDERS_COL);
        String testResult = ((String)testObject.getValueAt(0,PRICE_COL));
        assertThat(testObject.getRowCount(), equalTo(1));
        assertThat(testResult, equalTo("1.0"));
        OrderBookMessage message1 = new OrderBookMessage();
        message1.setType(LIMIT_ORDER_TYPE);
        message1.setSide(BUY);
        message1.setPrice(new BigDecimal(1.5));
        message1.setSize(new BigDecimal(3.0));
        message1.setSequence(1L);

        testObject.insertInto(message1);

        int firstRow = 0;
        assertThat(testObject.getRowCount(), equalTo(2));
        assertThat(testObject.getValueAt(firstRow, PRICE_COL), equalTo("1.50000"));
        assertThat(testObject.getValueAt(firstRow, SIZE_COL), equalTo("3.00000000"));
        assertThat(testObject.getValueAt(firstRow, NUM_ORDERS_COL), equalTo("1"));
        OrderBookMessage message2 = new OrderBookMessage();
        message2.setType(LIMIT_ORDER_TYPE);
        message2.setSide(BUY);
        message2.setPrice(new BigDecimal(1.5));
        message2.setSize(new BigDecimal(2.200));
        message2.setSequence(2L);

        testObject.insertInto(message2);

        assertThat(testObject.getRowCount(), equalTo(2));
        assertThat(testObject.getValueAt(firstRow, PRICE_COL), equalTo("1.50000"));
        assertThat(testObject.getValueAt(firstRow, SIZE_COL), equalTo("5.20000000"));
        assertThat(testObject.getValueAt(firstRow, NUM_ORDERS_COL), equalTo("2"));
    }

    @Test
    public void shouldInsertBuyOrderAsNewRowWhenPriceIsUnique(){
        testObject.setValueAt("1.0", 0, 0);
        String testResult = ((String)testObject.getValueAt(0,0));
        assertThat(testResult, equalTo("1.0"));
        assertThat(testObject.getRowCount(), equalTo(1));
        OrderBookMessage message1 = new OrderBookMessage();
        message1.setType(LIMIT_ORDER_TYPE);
        message1.setSide(BUY);
        message1.setPrice(new BigDecimal(1.5));
        message1.setSize(new BigDecimal(3.0));
        message1.setSequence(1L);

        testObject.insertInto(message1);

        int firstRow = 0;
        assertThat(testObject.getValueAt(firstRow, 0), equalTo("1.50000"));
        assertThat(testObject.getValueAt(firstRow, 1), equalTo("3.00000000"));
        assertThat(testObject.getValueAt(firstRow, 2), equalTo("1"));
        OrderBookMessage message2 = new OrderBookMessage();
        message2.setPrice(new BigDecimal("3.8"));
        message2.setSize(new BigDecimal("0.43400"));
        message2.setSide(BUY);
        message2.setType(LIMIT_ORDER_TYPE);
        message2.setSequence(2L);

        testObject.insertInto(message2);

        // item should appear at the top of the list since it's a new highest bidder/buy order
        assertThat(testObject.getValueAt(0, 0), equalTo("3.80000"));
        assertThat(testObject.getValueAt(0, 1), equalTo("0.43400000"));
        assertThat(testObject.getValueAt(0, 2), equalTo("1"));
    }

    @Test
    public void shouldInsertSellOrderAsNewRowWhenPriceIsUnique(){
        testObject.setValueAt("1.00000", 0, 0);
        String testResult = ((String)testObject.getValueAt(0,0));
        assertThat(testResult, equalTo("1.00000"));
        assertThat(testObject.getRowCount(), equalTo(1));
        OrderBookMessage message = new OrderBookMessage();
        message.setOrder_type(LIMIT_ORDER_TYPE);
        message.setPrice(new BigDecimal("3.8"));
        message.setRemaining_size(new BigDecimal("0.43400"));
        message.setSide(SELL);
        message.setType(OPEN);

        testObject.insertInto(message);

        int lastRow = testObject.getRowCount()-1;
        // item should appear at the top of the list since it's a new highest bidder/buy order
        assertThat(testObject.getValueAt(lastRow, PRICE_COL), equalTo("3.80000"));
        assertThat(testObject.getValueAt(lastRow, SIZE_COL), equalTo("0.43400000"));
        assertThat(testObject.getValueAt(lastRow, NUM_ORDERS_COL), equalTo("1"));
    }

    @Test
    public void shouldUpdateExistingSellOrderSizeWhenPriceEntryAlreadyExists(){
        int firstRow = 0;
        testObject.setValueAt("1.00000", firstRow, 0);
        testObject.setValueAt("0.87655",firstRow, 1);
        testObject.setValueAt("1",firstRow, 2);
        assertThat(((String)testObject.getValueAt(firstRow, 0)), equalTo("1.00000"));
        assertThat(((String)testObject.getValueAt(firstRow, 1)), equalTo("0.87655"));
        assertThat(((String)testObject.getValueAt(firstRow, 2)), equalTo("1"));
        assertThat(testObject.getRowCount(), equalTo(1));
        // create a new order
        OrderBookMessage message = new OrderBookMessage();
        message.setPrice(new BigDecimal("1.0"));
        message.setRemaining_size(new BigDecimal("0.43400"));
        message.setSide(SELL);
        message.setType(OPEN);

        // insert the new order
        testObject.insertInto(message);

        // item should appear at the top of the list since it's a new highest bidder/buy order
        assertThat(testObject.getValueAt(0, SIZE_COL), equalTo("1.31055000"));
    }

    @Test
    public void shouldUpdateExistingBuyOrderSizeAndQuantityWhenPriceEntryExists(){
        int firstRow = 0;
        testObject.setValueAt("1.00000", firstRow, PRICE_COL);
        testObject.setValueAt("0.87655",firstRow, SIZE_COL);
        testObject.setValueAt("1",firstRow, NUM_ORDERS_COL);
        assertThat(((String)testObject.getValueAt(firstRow, PRICE_COL)), equalTo("1.00000"));
        assertThat(((String)testObject.getValueAt(firstRow, SIZE_COL)), equalTo("0.87655"));
        assertThat(((String)testObject.getValueAt(firstRow, NUM_ORDERS_COL)), equalTo("1"));
        assertThat(testObject.getRowCount(), equalTo(1));
        // create a new order
        OrderBookMessage message = new OrderBookMessage();
        message.setPrice(new BigDecimal("1.0"));
        message.setRemaining_size(new BigDecimal("0.43400"));
        message.setSide(BUY);
        message.setType(OPEN);

        // insert the new order
        testObject.insertInto(message);

        // item should appear at the top of the list since it's a new highest bidder/buy order
        assertThat(testObject.getValueAt(0, SIZE_COL), equalTo("1.31055000"));
    }

    @Test
    public void shouldReduceQtyByOneForDoneOrder(){
        int firstRow = 0;
        testObject.setValueAt("1.00000", firstRow, 0);
        testObject.setValueAt("0.87655",firstRow, 1);
        testObject.setValueAt("5",firstRow, 2);
        assertThat(testObject.getRowCount(), equalTo(1));
        assertThat(((String)testObject.getValueAt(firstRow, 2)), equalTo("5"));
        // create a new order
        OrderBookMessage message = new OrderBookMessage();
        message.setPrice(new BigDecimal("1.0"));
        message.setSize(new BigDecimal("0.43400"));
        message.setSide(BUY);
        message.setType(DONE);
        message.setReason(FILLED);

        // insert the new order
        testObject.insertInto(message);

        // item should appear at the top of the list since it's a new highest bidder/buy order
        assertThat(testObject.getRowCount(), equalTo(1));
        assertThat(testObject.getValueAt(0, 2), equalTo("4"));
    }

    @Test
    public void shouldNotAlterQtyForMatchedOrder(){
        int firstRow = 0;
        testObject.setValueAt("1.00000", firstRow, PRICE_COL);
        testObject.setValueAt("0.87655",firstRow, SIZE_COL);
        testObject.setValueAt("5",firstRow, NUM_ORDERS_COL);
        assertThat(testObject.getRowCount(), equalTo(1));
        assertThat(((String)testObject.getValueAt(firstRow, NUM_ORDERS_COL)), equalTo("5"));
        // create a MATCHED order
        OrderBookMessage message = new OrderBookMessage();
        message.setPrice(new BigDecimal("1.0"));
        message.setSize(new BigDecimal("0.43400"));
        message.setSide(BUY);
        message.setType(MATCH);

        // insert the new order
        testObject.insertInto(message);

        // item should appear at the top of the list since it's a new highest bidder/buy order
        assertThat(testObject.getRowCount(), equalTo(1));
        assertThat(testObject.getValueAt(0, NUM_ORDERS_COL), equalTo("5"));
    }

    @Test
    public void shouldNotReduceQtyForMatchedOrder(){
        int firstRow = 0;
        testObject.setValueAt("1.00000", firstRow, PRICE_COL);
        testObject.setValueAt("0.87655",firstRow, SIZE_COL);
        testObject.setValueAt("5",firstRow, NUM_ORDERS_COL);
        assertThat(testObject.getRowCount(), equalTo(1));
        assertThat(((String)testObject.getValueAt(firstRow, PRICE_COL)), equalTo("1.00000"));
        assertThat(((String)testObject.getValueAt(firstRow, SIZE_COL)), equalTo("0.87655"));
        assertThat(((String)testObject.getValueAt(firstRow, NUM_ORDERS_COL)), equalTo("5"));
        // create a new order
        OrderBookMessage message = new OrderBookMessage();
        message.setPrice(new BigDecimal("1.0"));
        message.setSize(new BigDecimal("0.43400"));
        message.setSide(BUY);
        message.setType(MATCH);

        // insert the new order
        testObject.insertInto(message);

        // item should appear at the top of the list since it's a new highest bidder/buy order
        assertThat(testObject.getRowCount(), equalTo(1));
        assertThat(testObject.getValueAt(0, NUM_ORDERS_COL), equalTo("5"));
    }

    @Test
    public void shouldInsertOneOrderIntoTable(){
        // create a new order
        OrderBookMessage message = new OrderBookMessage();
        message.setType(LIMIT_ORDER_TYPE);
        message.setPrice(new BigDecimal("1.0"));
        message.setRemaining_size(new BigDecimal("0.43400"));
        message.setSide(BUY);
        message.setSequence(1L);

        testObject.insertInto(message);

        assertThat(testObject.getRowCount(), equalTo(1));
        assertThat((new BigDecimal((String)testObject.getValueAt(0, PRICE_COL))).compareTo(new BigDecimal("1.0")), equalTo(0));
    }

    @Test
    public void shouldInsertTwoBuyOrdersIntoTable(){
        // create a new order
        OrderBookMessage message1 = new OrderBookMessage();
        message1.setOrder_type(LIMIT_ORDER_TYPE);
        message1.setPrice(new BigDecimal("1.0"));
        message1.setRemaining_size(new BigDecimal("0.43400"));
        message1.setSide(BUY);
        message1.setSequence(1L);
        message1.setType(OPEN);
        testObject.insertInto(message1);
        assertThat(testObject.getRowCount(), equalTo(1));
        OrderBookMessage message2 = new OrderBookMessage();
        message2.setOrder_type(LIMIT_ORDER_TYPE);
        message2.setPrice(new BigDecimal("1.1"));
        message2.setRemaining_size(new BigDecimal("0.43400"));
        message2.setSide(BUY);
        message2.setSequence(2L);
        message2.setType(OPEN);

        testObject.insertInto(message2);

        assertThat(testObject.getRowCount(), equalTo(2));
        assertThat((new BigDecimal((String)testObject.getValueAt(0, PRICE_COL))).compareTo(new BigDecimal("1.1")), equalTo(0));
        assertThat((new BigDecimal((String)testObject.getValueAt(1, PRICE_COL))).compareTo(new BigDecimal("1.0")), equalTo(0));
    }

    @Test
    public void shouldNotUpdateTableWithANewEntryForDoneFilledOrdersWhenPriceEntryDoesNotExist() {
        // create a new order
        OrderBookMessage message1 = new OrderBookMessage();
        message1.setOrder_type(LIMIT_ORDER_TYPE);
        message1.setPrice(new BigDecimal("1.0"));
        message1.setRemaining_size(new BigDecimal("0.43400"));
        message1.setSide(BUY);
        message1.setSequence(1L);
        message1.setType(DONE);
        message1.setReason(FILLED);

        testObject.insertInto(message1);

        assertThat(testObject.getRowCount(), equalTo(0));
    }

    @Test
    public void shouldNotUpdateTableWithANewEntryForDoneCanceledOrdersWhenPriceEntryDoesNotExist() {
        // create a new order
        OrderBookMessage message1 = new OrderBookMessage();
        message1.setOrder_type(LIMIT_ORDER_TYPE);
        message1.setPrice(new BigDecimal("1.0"));
        message1.setRemaining_size(new BigDecimal("0.43400"));
        message1.setSide(BUY);
        message1.setSequence(1L);
        message1.setType(DONE);
        message1.setReason(CANCELED);

        testObject.insertInto(message1);

        assertThat(testObject.getRowCount(), equalTo(0));
    }
}
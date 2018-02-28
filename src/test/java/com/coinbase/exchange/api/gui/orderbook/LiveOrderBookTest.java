package com.coinbase.exchange.api.gui.orderbook;

import com.coinbase.exchange.api.constants.GdaxConstants;
import com.coinbase.exchange.api.marketdata.MarketData;
import com.coinbase.exchange.api.marketdata.OrderItem;
import com.coinbase.exchange.api.websocketfeed.message.OrderBookMessage;
import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static com.coinbase.exchange.api.constants.GdaxConstants.*;
import static java.math.BigDecimal.ROUND_HALF_UP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class LiveOrderBookTest {

    private static final Logger log = LoggerFactory.getLogger(LiveOrderBook.class);

    private static final long INITIAL_MAX_SEQUENCE_ID = 123456L;

    MarketData marketData;
    LiveOrderBook testObject;

    @Before
    public void setup(){
        List<OrderItem> bids = getOrderItems("/testdata/testBids.csv");
        List<OrderItem> asks = getOrderItems("/testdata/testAsks.csv");

        marketData = new MarketData(INITIAL_MAX_SEQUENCE_ID, bids, asks);
        testObject = new LiveOrderBook();
        testObject.initBidsAndAsksAndMaxSequenceId(marketData);
        testObject.setReady(true);
    }

    @Test
    public void shouldEnsureBidMarketDataIsDisplayedInOrderHighestAtTopLowestAtBottom() {
        OrderBookModel bids = testObject.getBids();
        assertThat(bids.size()).isGreaterThan(2);
        //ensure ordering is correct
        int firstRow = 0;
        BigDecimal previousBid = new BigDecimal((String)bids.getValueAt(firstRow, GdaxConstants.PRICE_COL));
        for (int i = 1; i < bids.size(); i++) { // move down the list from 1 to size()-1: 0th item is the starting item to compare
            BigDecimal thisOrderPrice = new BigDecimal((String)bids.getValueAt(i, GdaxConstants.PRICE_COL));
            assertThat(previousBid.compareTo(thisOrderPrice)).isGreaterThan(0);
            previousBid = thisOrderPrice; // get ready for the next
        }
    }

    @Test
    public void shouldEnsureAskMarketDataIsHeldInDescendingOrderLowestAtTopHighestAtBottom() {
        OrderBookModel asks = testObject.getAsks();

        // quick check to ensure there are enough orders for a comparison to be made
        assertThat(asks.size()).isGreaterThan(2);

        //ensure ordering is correct
        int firstRow = 0;
        BigDecimal previousAsk = new BigDecimal((String)asks.getValueAt(firstRow, GdaxConstants.PRICE_COL));
        for (int i = 1; i < asks.size(); i++) {
            BigDecimal thisOrderPrice = new BigDecimal((String)asks.getValueAt(i, GdaxConstants.PRICE_COL));

            assertThat(previousAsk.compareTo(thisOrderPrice)).isLessThan(0);

            previousAsk = thisOrderPrice;
        }
    }

    @Test
    public void shouldEnsureMarketDataIsHeldInTheModelInAggregatedFormBeforeOrderBookIsReadyToTakeLiveFeed(){
        // bid entrys in the model should become aggregated so there will
        OrderBookModel bids = testObject.getBids();
        OrderBookModel asks = testObject.getAsks();

        assertThat(bids.size()).isEqualTo(8); // aggregated
        assertThat(asks.size()).isEqualTo(5); // aggregated
    }

    @Test
    public void shouldQueueMessagesWhilstOrderBookViewIsNotReady() {
        List<OrderBookMessage> orders = getWebSocketFeedOrders("/testdata/incomingWebsocketFeed.json", BTC_GBP_PRODUCT);
        testObject.setReady(false);
        for (OrderBookMessage order: orders) {
            testObject.handleMessages(order);
        }

        assertThat(testObject.getBids().size()).isEqualTo(8);
        assertThat(testObject.getAsks().size()).isEqualTo(5);
        assertThat(testObject.getQueuedMessages().size()).isEqualTo(16);
    }

    @Test
    public void shouldQueueAnOutOfSequenceOrder() {
        // create a new order
        OrderBookMessage message1 = sellMessage();
        message1.setPrice(new BigDecimal(9620.0));
        message1.setRemaining_size(new BigDecimal(0.5)); // should add to the 0.01 size in testAsks.csv
        message1.setSequence(INITIAL_MAX_SEQUENCE_ID+2);

        testObject.handleMessages(message1);

        assertThat(testObject.nextMessageToExpect()).isEqualTo(INITIAL_MAX_SEQUENCE_ID+1);
        assertThat(testObject.getQueuedMessages().size()).isEqualTo(1);
    }

    @Test
    public void shouldProcessedQueuedOrdersWhenNextOrderSeqExpectedIsReceived() {
        // create a new order
        OrderBookMessage message1 = sellMessage();
        message1.setPrice(new BigDecimal(9620.0));
        message1.setRemaining_size(new BigDecimal(0.5)); // should add to the 0.01 size in testAsks.csv
        message1.setSequence(INITIAL_MAX_SEQUENCE_ID+2);

        testObject.handleMessages(message1);

        assertThat(testObject.nextMessageToExpect()).isEqualTo(INITIAL_MAX_SEQUENCE_ID+1);
        assertThat(testObject.getQueuedMessages().size()).isEqualTo(1);

        // create a new order
        OrderBookMessage message2 = sellMessage();
        message2.setOrder_type(LIMIT_ORDER_TYPE);
        message2.setPrice(new BigDecimal(9700.00));
        message2.setRemaining_size(new BigDecimal(0.5));
        message2.setSequence(INITIAL_MAX_SEQUENCE_ID+3);

        testObject.handleMessages(message2);

        // unchanged value
        assertThat(testObject.nextMessageToExpect()).isEqualTo(INITIAL_MAX_SEQUENCE_ID+1); // no change
        assertThat(testObject.getQueuedMessages().size()).isEqualTo(2);

        // create a new order
        OrderBookMessage message3 = sellMessage();
        message3.setPrice(new BigDecimal(9701.00));
        message3.setRemaining_size(new BigDecimal(0.5));

        testObject.handleMessages(message3);

        // unchanged value
        assertThat(testObject.nextMessageToExpect()).isEqualTo(INITIAL_MAX_SEQUENCE_ID+4); // now expecting the new message
        assertThat(testObject.getQueuedMessages().size()).isEqualTo(0);
    }

    @Test
    public void shouldMaintainLiveOrdersInAnAggregatedFormEvenIfTheyAreProcessedAfterBeingQueued() {
        List<OrderBookMessage> orders = getWebSocketFeedOrders("/testdata/incomingWebsocketFeed.json", BTC_GBP_PRODUCT);
        testObject.setReady(false);

        for (OrderBookMessage order: orders) {
            testObject.handleMessages(order);
        }

        testObject.setReady(true);

        assertThat(testObject.getBids().size()).isEqualTo(8);
        assertThat(testObject.getAsks().size()).isEqualTo(5);
    }



    @Test
    public void shouldGetBidOrderBookWhenMessageSideIsBuy() {
        OrderBookMessage message = buyMessage();
        message.setOrder_type(LIMIT_ORDER_TYPE);
        message.setPrice(new BigDecimal(1.0));
        message.setRemaining_size(BigDecimal.ZERO);

        OrderBookModel result = testObject.getRelevantOrderBookModel(message);

        assertThat(result).isEqualTo(testObject.getBids());
    }

    @Test
    public void shouldGetAskOrderBookWhenMessageSideIsASell() {
        OrderBookMessage message = sellMessage();
        message.setPrice(new BigDecimal(1.0));
        message.setRemaining_size(BigDecimal.ZERO);

        OrderBookModel result = testObject.getRelevantOrderBookModel(message);

        assertThat(result).isEqualTo(testObject.getAsks());
    }

    private List<OrderItem> getOrderItems(String file) {
        List<OrderItem> orderItems = new ArrayList<>();
        int lineItem = 0;
        try {
            List<String> items = Files.readAllLines(Paths.get(this.getClass().getResource(file).toURI()), StandardCharsets.UTF_8);
            for (String line : items) {
                String[] orders = line.split(",");
                orderItems.add(orderItem(
                        new BigDecimal(orders[0].trim()),
                        new BigDecimal(orders[1].trim()),
                        new Integer(orders[2].trim())));
                lineItem++;
            }
        } catch (IOException e) {
            log.error("Bad File: {} on line {}", file, lineItem);
            e.printStackTrace();
        } catch (URISyntaxException e) {
            log.error("Bad File: {} on line {}", file, lineItem);
            e.printStackTrace();
        } catch (NullPointerException npe) {
            log.error("Bad File: {} on line {}", file, lineItem);
        }

        return orderItems;
    }

    @Test
    public void shouldInitViewAndBeReadyToExpectTheNextSequenceIdForProcessing() {
        Long nextSeqToExpect = testObject.nextMessageToExpect();

        assertThat(nextSeqToExpect).isEqualTo(INITIAL_MAX_SEQUENCE_ID + 1);
    }

    @Test
    public void shouldHandleMessagesInOrderWhenOrderBookIsInitialisedAndIsReady() {
        testObject.setReady(false);
        int offset = 3;
        OrderBookMessage message1 = sellMessage();
        message1.setOrder_type(LIMIT_ORDER_TYPE);
        message1.setPrice(new BigDecimal(1.0));
        message1.setRemaining_size(BigDecimal.ZERO);
        message1.setSequence(INITIAL_MAX_SEQUENCE_ID + offset); // unique Sequence Id

        testObject.handleMessages(message1);

        assertThat(testObject.getQueuedMessages().size()).isEqualTo(1);
        assertThat(testObject.getQueuedMessages().get(INITIAL_MAX_SEQUENCE_ID+offset)).isEqualTo(message1);

        // insert another 2nd order out of sequence - effect should be about the same but need to assert
        // additional messages are queued up.
        OrderBookMessage message2 = sellMessage();
        message2.setPrice(new BigDecimal(1.0));
        message2.setRemaining_size(BigDecimal.ZERO);
        message2.setSequence(INITIAL_MAX_SEQUENCE_ID + offset-1); // unique Sequence Ids - ordered

        testObject.handleMessages(message2);

        offset = 1;

        OrderBookMessage message3 = sellMessage();
        message3.setPrice(new BigDecimal(1.0));
        message3.setRemaining_size(BigDecimal.ZERO);
        message3.setSequence(INITIAL_MAX_SEQUENCE_ID + offset-2); // unique Sequence Ids - ordered

        testObject.handleMessages(message3);
    }

    @Test
    public void shouldUpdateNumOrdersQtyForDoneFilledOrders() {
        OrderBookMessage message = sellMessage();
        message.setPrice(new BigDecimal(9600.0));
        message.setRemaining_size(new BigDecimal(0.1)); // should add to the 0.01 size in testAsks.csv
        message.setSequence(INITIAL_MAX_SEQUENCE_ID+1);
        message.setType(DONE);
        message.setReason(FILLED);

        testObject.handleMessages(message);

        assertThat(testObject.getAsks().getValueAt(0, GdaxConstants.PRICE_COL)).isEqualTo("9600.00000");
        assertThat(testObject.getAsks().getValueAt(0, GdaxConstants.NUM_ORDERS_COL)).isEqualTo("2");
        // we can't make any assertions about size remaining for done orders. This is because the remaining_size
        // only applies to the current order, not all orders at the current price entry point.
    }

    @Test
    public void shouldUpdateNumOrdersQtyForDoneCanceledOrders() {
        OrderBookMessage message = sellMessage();
        message.setPrice(new BigDecimal(9600.0));
        message.setRemaining_size(new BigDecimal(0.03)); // should add to the 0.01 size in testAsks.csv
        message.setSequence(INITIAL_MAX_SEQUENCE_ID+1);
        message.setType(DONE);
        message.setReason(CANCELED);

        testObject.handleMessages(message);

        assertThat(testObject.getAsks().getValueAt(0, GdaxConstants.PRICE_COL)).isEqualTo("9600.00000");
        assertThat(testObject.getAsks().getValueAt(0, GdaxConstants.SIZE_COL)).isEqualTo("0.10000000");
        assertThat(testObject.getAsks().getValueAt(0, GdaxConstants.NUM_ORDERS_COL)).isEqualTo("2");
    }

    @Test
    public void shouldApplyMessagesOnceOrderBookViewIsReady() {
        List<OrderBookMessage> orders = getWebSocketFeedOrders("/testdata/incomingWebsocketFeed.json", BTC_GBP_PRODUCT);

        for (OrderBookMessage order: orders) {
            testObject.handleMessages(order);
        }

        // should be no queued messages left
        assertThat(testObject.getQueuedMessages().size()).isEqualTo(0);
        assertThat(testObject.getBids().size()).isEqualTo(11);
        assertThat(testObject.getAsks().size()).isEqualTo(6);
    }

    @Test
    public void shouldReduceSizeWhenCanceledBuyOrderIsReceived() {
        OrderBookMessage canceledBuy = getOrderFromFile("/testdata/canceledBuy_001.json");
        int index = getPriceEntryIndex(testObject.getBids(), new BigDecimal(9870));

        // remaining size - the outstanding amount of the order to cancel.
        testObject.handleMessages(canceledBuy); // price: 9870, remaining_size: 0.16

        assertThat(testObject.getBids().getValueAt(index, PRICE_COL)).isEqualTo("9870.00000");
        assertThat(testObject.getBids().getValueAt(index, SIZE_COL)).isEqualTo("0.00310000");
    }

    @Test
    public void shouldReduceSizeWhenCanceledSellOrderIsReceived() {
        OrderBookMessage canceledSell = getOrderFromFile("/testdata/canceledSell_002.json");
        int index = getPriceEntryIndex(testObject.getAsks(), new BigDecimal(9610));

        // note: sequence ID must be in sequence
        // remaining size - the outstanding amount of the order to cancel.
        testObject.handleMessages(canceledSell); // price: 9610, remaining_size: 0.01000000

        assertThat(testObject.getAsks().getValueAt(index, PRICE_COL)).isEqualTo("9610.00000");
        assertThat(testObject.getAsks().getValueAt(index, SIZE_COL)).isEqualTo("0.04000000");
    }

    @Test
    public void shouldRemovePriceEntryForCanceledBuyWhenRemainingSizeMatchesAmountRemainingOnTheOrderBook() {
        OrderBookMessage canceledBuy = getOrderFromFile("/testdata/canceledBuy_003.json");

        // remaining size - the outstanding amount of the order to cancel.
        testObject.handleMessages(canceledBuy); // price: 9870, remaining_size: 0.1631

        int missingIndex = getPriceEntryIndex(testObject.getBids(), new BigDecimal(9870));
        assertThat(missingIndex).isEqualTo(-1);
    }

    @Test
    public void shouldRemovePriceEntryForCanceledSellWhenRemainingSizeMatchesAmountRemainingOnTheOrderBook() {
        OrderBookMessage canceledSell = getOrderFromFile("/testdata/canceledSell_004.json");

        // note: sequence ID must be in sequence
        // remaining size - the outstanding amount of the order to cancel.
        testObject.handleMessages(canceledSell); // price: 9610, remaining_size: 0.05000000

        int missingIndex = getPriceEntryIndex(testObject.getAsks(), new BigDecimal(9610));
        assertThat(missingIndex).isEqualTo(-1);
    }

    @Test
    public void shouldUpdateSizeWhenMatchIsMadeOnBuySide() {
        OrderBookMessage matchedBuy = getOrderFromFile("/testdata/matchedBuy_001.json");
        getPriceEntryIndex(testObject.getBids(), new BigDecimal(9879));
        int index = getPriceEntryIndex(testObject.getBids(), new BigDecimal(9879));

        // note: sequence ID must be in sequence
        // remaining size - the outstanding amount of the order to cancel.
        testObject.handleMessages(matchedBuy); // price: 9879, size: 0.00100000

        assertThat(testObject.getBids().getValueAt(index, PRICE_COL)).isEqualTo("9879.00000");
        assertThat(testObject.getBids().getValueAt(index, SIZE_COL)).isEqualTo("0.04900000");
    }

    @Test
    public void shouldUpdateSizeWhenMatchIsMadeOnSellSide() {
        OrderBookMessage matchedSell = getOrderFromFile("/testdata/matchedSell_002.json");
        getPriceEntryIndex(testObject.getAsks(), new BigDecimal(9600));
        int index = getPriceEntryIndex(testObject.getAsks(), new BigDecimal(9600));

        // note: sequence ID must be in sequence
        // remaining size - the outstanding amount of the order to cancel.
        testObject.handleMessages(matchedSell); // price: 9600, size: 0.05000000

        assertThat(testObject.getAsks().getValueAt(index, PRICE_COL)).isEqualTo("9600.00000");
        assertThat(testObject.getAsks().getValueAt(index, SIZE_COL)).isEqualTo("0.08000000");
    }

    @Test
    public void shouldRemovePriceEntryWhenMatchIsMadeOnBuySideForFullSizeOnOrderBook() {
        OrderBookMessage matchedBuy = getOrderFromFile("/testdata/matchedBuy_003.json");
        getPriceEntryIndex(testObject.getBids(), new BigDecimal(9879));

        // note: sequence ID must be in sequence
        // remaining size - the outstanding amount of the order to cancel.
        testObject.handleMessages(matchedBuy); // price: 9879, size: 0.00100000

        int index = getPriceEntryIndex(testObject.getBids(), new BigDecimal(9879));
        assertThat(index).isEqualTo(-1);
    }

    @Test
    public void shouldRemovePriceEntryWhenMatchIsMadeOnSellSideForSameSizeThatIsOnOrderBook() {
        OrderBookMessage matchedSell = getOrderFromFile("/testdata/matchedSell_004.json");
        getPriceEntryIndex(testObject.getAsks(), new BigDecimal(9600));

        // note: sequence ID must be in sequence
        // remaining size - the outstanding amount of the order to cancel.
        testObject.handleMessages(matchedSell); // price: 9600, size: 0.05000000

        int index = getPriceEntryIndex(testObject.getAsks(), new BigDecimal(9600));
        assertThat(index).isEqualTo(-1);
    }

    @Test
    public void shouldAddNewPriceEntryForOpenBuyOrdersAtNewPriceEntryPositions() {
        OrderBookMessage openBuy = getOrderFromFile("/testdata/openBuy_001.json");
        int index = getPriceEntryIndex(testObject.getBids(), new BigDecimal(9872.10));
        assertThat(index).isEqualTo(-1);

        // note: sequence ID must be in sequence
        // remaining size - the outstanding amount of the order to cancel.
        testObject.handleMessages(openBuy); // price: 9872.10, remaining_size: 0.88000000

        // rounding necessary here since the big decimal doesn't compare how we'd expect otherwise.
        index = getPriceEntryIndex(testObject.getBids(), new BigDecimal(9872.10).setScale(8, ROUND_HALF_UP));
        assertThat(index).isEqualTo(4);
        assertThat(testObject.getBids().getValueAt(index, SIZE_COL)).isEqualTo("0.88000000");
    }

    @Test
    public void shouldAddNewPriceEntryForOpenSellOrdersAtNewPriceEntryPositions() {
        OrderBookMessage openSell = getOrderFromFile("/testdata/openSell_002.json");
        int index = getPriceEntryIndex(testObject.getAsks(), new BigDecimal(9599.00));
        assertThat(index).isEqualTo(-1);

        // note: sequence ID must be in sequence
        // remaining size - the outstanding amount of the order to cancel.
        testObject.handleMessages(openSell); // price: 9599.00, remaining_size: 1.25

        // rounding necessary here since the big decimal doesn't compare how we'd expect otherwise.
        index = getPriceEntryIndex(testObject.getAsks(), new BigDecimal(9599.00).setScale(8, ROUND_HALF_UP));
        assertThat(index).isEqualTo(0);
        assertThat(testObject.getAsks().getValueAt(index, SIZE_COL)).isEqualTo("1.25000000");
    }

    @Test
    public void shouldAddSizeToExistingPriceEntryForOpenBuyOrders() {
        OrderBookMessage openBuy = getOrderFromFile("/testdata/openBuy_003.json");
        int index = getPriceEntryIndex(testObject.getBids(), new BigDecimal(9873.30).setScale(8, ROUND_HALF_UP));

        // note: sequence ID must be in sequence
        // remaining size - the outstanding amount of the order to cancel.
        testObject.handleMessages(openBuy); // price: 9873.30, remaining_size: 0.09000000

        // rounding necessary here since the big decimal doesn't compare how we'd expect otherwise.
        assertThat(testObject.getBids().getValueAt(index, SIZE_COL)).isEqualTo("0.20000000");
    }

    @Test
    public void shouldAddSizeToExistingPriceEntryForOpenSellOrders() {
        OrderBookMessage openSell = getOrderFromFile("/testdata/openSell_004.json");
        int index = getPriceEntryIndex(testObject.getAsks(), new BigDecimal(9622.48).setScale(8, ROUND_HALF_UP));

        // note: sequence ID must be in sequence
        // remaining size - the outstanding amount of the order to cancel.
        testObject.handleMessages(openSell); // price: 9622.48, remaining_size: 0.43

        // rounding necessary here since the big decimal doesn't compare how we'd expect otherwise.
        assertThat(testObject.getAsks().getValueAt(index, SIZE_COL)).isEqualTo("1.00000000");
    }

    @Test
    public void shouldIgnoreReceivedBuyOrders() {
        OrderBookMessage receivedBuy = getOrderFromFile("/testdata/receivedBuy_001.json");
        int index = getPriceEntryIndex(testObject.getAsks(), new BigDecimal(9875.00).setScale(8, ROUND_HALF_UP));
        assertThat(index).isEqualTo(-1);

        // note: sequence ID must be in sequence
        // remaining size - the outstanding amount of the order to cancel.
        testObject.handleMessages(receivedBuy); // price: 9875.00, remaining_size: 0.88

        // rounding necessary here since the big decimal doesn't compare how we'd expect otherwise.
        index = getPriceEntryIndex(testObject.getBids(), new BigDecimal(9875.00).setScale(8, ROUND_HALF_UP));
        assertThat(index).isEqualTo(-1);
    }

    @Test
    public void shouldIgnoreReceivedSellOrders() {
        OrderBookMessage receivedSell = getOrderFromFile("/testdata/receivedSell_002.json");
        int index = getPriceEntryIndex(testObject.getAsks(), new BigDecimal(9599.00));
        assertThat(index).isEqualTo(-1);

        // note: sequence ID must be in sequence
        // remaining size - the outstanding amount of the order to cancel.
        testObject.handleMessages(receivedSell); // price: 9599.00, remaining_size: 1.25

        // rounding necessary here since the big decimal doesn't compare how we'd expect otherwise.
        index = getPriceEntryIndex(testObject.getAsks(), new BigDecimal(9599.00).setScale(8, ROUND_HALF_UP));
        assertThat(index).isEqualTo(-1);
    }

    @Test
    public void shouldNotTimeoutBeforeTimeoutPeriodHasPassed() {
        assertThat(testObject.isTimedOut()).isFalse();
    }

    @Test
    public void shouldTimeoutAfterTimeoutPeriodHasElapsed() {
        try {
            Thread.sleep((testObject.getTimeout() + 1) * 1000);
            assertThat(testObject.isTimedOut()).isTrue();
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail();
        }
    }

    /**
     * IOC - immediate or cancel. buy/sell as much at a certain price, cancel any unfilfilled portion of the order.
     * This trade has the following lifecycle:
     * 1. order is received - price may be above the lowest ask, or for a sell below the lowest bid.
     * 2. this will result in matches (possibly at a better/different price)
     * 3. anything outstanding will be cancelled. No order will be opened on the book following the received message
     * Cancel orders should not result in new price entries on the orderbook.
     */
    @Test
    public void shouldNotCreateNewEntriesInTheOrderBookWhenACanceledOrderIsReceived() {
        OrderBookMessage cancelOrderNotInBook = getOrderFromFile("/testdata/cancelSell_priceDoesNotExist_001.json");
        BigDecimal priceEntry = new BigDecimal(7940.26).setScale(8, ROUND_HALF_UP);
        int index = getPriceEntryIndex(testObject.getAsks(),priceEntry);
        assertThat(index).isEqualTo(-1);

        // note: sequence ID must be in sequence
        // remaining size - the outstanding amount of the order to cancel - but do not create new entry if
        // price point does not exist.
        testObject.handleMessages(cancelOrderNotInBook); // price: 7940.26, remaining_size: 1.874

        // rounding necessary here since the big decimal doesn't compare how we'd expect otherwise.
        index = getPriceEntryIndex(testObject.getAsks(), priceEntry);
        assertThat(index).isEqualTo(-1);
    }

    private OrderBookMessage sellMessage() {
        OrderBookMessage message = new OrderBookMessage();
        message.setOrder_type(LIMIT_ORDER_TYPE);
        message.setSide(SELL);
        message.setSequence(INITIAL_MAX_SEQUENCE_ID + 1);
        message.setType(OPEN);
        return message;
    }

    private OrderBookMessage buyMessage() {
        OrderBookMessage message = new OrderBookMessage();
        message.setOrder_type(LIMIT_ORDER_TYPE);
        message.setSide(BUY);
        message.setSequence(INITIAL_MAX_SEQUENCE_ID + 1);
        message.setType(OPEN);
        return message;
    }

    private int getPriceEntryIndex(OrderBookModel model, BigDecimal lookupPrice) {
        for (int i=0; i<model.size(); i++) {
            if (new BigDecimal((String) model.getValueAt(i, GdaxConstants.PRICE_COL)).compareTo(lookupPrice) == 0)
                return i;
        }
        return -1; // not found
    }

    private OrderItem orderItem(BigDecimal price, BigDecimal size, int num) {
        List<String> bid = new ArrayList<>();
        bid.add(price.toString());
        bid.add(size.toString());
        bid.add(num + "");
        return new OrderItem(bid);
    }

    private List<OrderBookMessage> getWebSocketFeedOrders(String file, String productId) {
        List<OrderBookMessage> incomingMessages = new ArrayList<>();
        int lineItem = 0;
        try {
            Gson gson = new Gson();
            List<String> messages = Files.readAllLines(Paths.get(this.getClass().getResource(file).toURI()), StandardCharsets.UTF_8);
            for (String line : messages) {
                OrderBookMessage message = gson.fromJson(line, OrderBookMessage.class);
                if (message.getProduct_id().equals(productId)){
                    incomingMessages.add(message);
                }
                lineItem++;
            }
        } catch (IOException e) {
            log.error("Bad File: {} on line {}", file, lineItem);
            e.printStackTrace();
        } catch (URISyntaxException e) {
            log.error("Bad File: {} on line {}", file, lineItem);
            e.printStackTrace();
        } catch (NullPointerException npe) {
            log.error("Bad File: {} on line {}", file, lineItem);
        }

        return incomingMessages;
    }

    private OrderBookMessage getOrderFromFile(String file) {
        OrderBookMessage message = null;
        try {
            Gson gson = new Gson();
            List<String> messages = Files.readAllLines(Paths.get(this.getClass().getResource(file).toURI()), StandardCharsets.UTF_8);
            message = gson.fromJson(messages.get(0), OrderBookMessage.class);

        } catch (IOException e) {
            log.error("Bad File: {} on line {}", file, 0);
            e.printStackTrace();
        } catch (URISyntaxException e) {
            log.error("Bad File: {} on line {}", file, 0);
            e.printStackTrace();
        } catch (NullPointerException npe) {
            log.error("Bad File: {} on line {}", file, 0);
        }
        return message;
    }
}
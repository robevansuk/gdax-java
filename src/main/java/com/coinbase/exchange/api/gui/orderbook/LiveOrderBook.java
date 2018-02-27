package com.coinbase.exchange.api.gui.orderbook;

import com.coinbase.exchange.api.marketdata.MarketData;
import com.coinbase.exchange.api.marketdata.MarketDataService;
import com.coinbase.exchange.api.websocketfeed.WebsocketFeed;
import com.coinbase.exchange.api.websocketfeed.message.OrderBookMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static com.coinbase.exchange.api.constants.GdaxConstants.*;

@Component
public class LiveOrderBook {

    public static final Logger log = LoggerFactory.getLogger(LiveOrderBook.class);

    private MarketDataService marketDataService;
    private WebsocketFeed websocketFeed;

    private OrderBookModel bids;
    private OrderBookModel asks;
    private String productId;

    private Boolean isReady;
    private int timeout;

    private long nextSeqIdToExpect;
    private long sequenceLastProcessedTimeStamp;

    private Map<Long, OrderBookMessage> queuedMessages;

    public LiveOrderBook() {
        this.productId = "BTC-GBP";
        this.timeout = 10;
        queuedMessages = new HashMap<>();
        this.isReady = false;
        sequenceLastProcessedTimeStamp = System.currentTimeMillis();
        bids = new OrderBookModel();
        asks = new OrderBookModel();
    }

    @Autowired
    public LiveOrderBook(@Value("${liveorderbook.timeout}") int timeout,
                         @Value("${liveorderbook.defaultProduct}") String productId,
                         MarketDataService marketDataService,
                         WebsocketFeed websocketFeed) {
        this.timeout = timeout;
        this.productId = productId;
        this.isReady = false;
        this.sequenceLastProcessedTimeStamp = System.currentTimeMillis();
        this.queuedMessages = new HashMap<>();
        this.bids = new OrderBookModel();
        this.asks = new OrderBookModel();

        this.marketDataService = marketDataService;
        this.websocketFeed = websocketFeed;
        log.info("LiveOrderBook almost ready.");
    }

    public void loadLiveOrderBookModel(String productId) {
        isReady = false;
        queuedMessages.clear();
        bids.clear();
        asks.clear();
        websocketFeed.subscribe(productId, this);// *** THIS MUST HAPPEN BEFORE INITING THE OB OR MESSAGES WILL LIKELY BE MISSING
        initBidsAndAsksAndMaxSequenceId(marketDataService.getMarketDataOrderBook(productId, FULL_ORDER_BOOK));
        isReady = true;
        log.info("LiveOrderBook ready!");
    }

    public void initBidsAndAsksAndMaxSequenceId(MarketData data) {
        log.info("Init bid/ask data and max sequence ID: {}", data.getSequence());
        // bids - we want to display best at top (highest), lowest at bottom.
        // bids come in in that order.
        for (int i = data.getBids().size() - 1; i >= 0; i--) {
            getBids().insert(data.getBids().get(i), BUY);
            log.info("Loading Bid: {} {} {}",
                    data.getBids().get(i).getPrice(),
                    data.getBids().get(i).getSize(),
                    data.getBids().get(i).getNum());
        }
        // asks - we want lowest ask to appear at the top of the table.
        // since asks come in lowest at top biggest at the bottom, add as they are.
        for (int i = 0; i < data.getAsks().size(); i++) {
            getAsks().insert(data.getAsks().get(i), SELL);
            log.info("Loading Ask: {} {} {}",
                    data.getAsks().get(i).getPrice(),
                    data.getAsks().get(i).getSize(),
                    data.getAsks().get(i).getNum());
        }

        this.nextSeqIdToExpect = data.getSequence() + 1;
    }

    /**
     * takes incoming messages and routes relevant ones that will cause a material change to the orderbook to updateOrderBook
     */
    public void handleMessages(OrderBookMessage message) {
        handleMessage(message);
        processNextBatchIfAvailableInCache();
    }

    public void handleMessage(OrderBookMessage message) {
        if (isTimedOut()) {
            log.error("Message {} not received in time. Restarting Orderbook.", nextSeqIdToExpect);
            // resubscribe
            sequenceLastProcessedTimeStamp = System.currentTimeMillis();
            loadLiveOrderBookModel(productId);

        }
        if(message.getType().equals("change") || message.getType().equals("activate")) {
            log.error("Change message received");
        }
        if (isReady) {
            long sequenceId = message.getSequence();
            sequenceLastProcessedTimeStamp = System.currentTimeMillis();
            if (isOutOfSequence(message)) {
                log.info("out of sequence message queued: {}", message.getSequence());
                queueMessage(message);
            } else {
                if (isMessageTypeWeCareAbout(message)) {
                    updateOrderBook(message);
                } else {
                    log.info("IGNORED: {}, Type {}, {}, price {}", sequenceId, message.getType(),
                            message.getSize() == null ? "remainingSize " + message.getRemaining_size() : "size " + message.getSize(),
                            message.getPrice());
                }
                if (queuedMessages.containsKey(sequenceId)) {
                    queuedMessages.remove(nextSeqIdToExpect);
                }
                nextSeqIdToExpect = nextSeqIdToExpect + 1;
            }
        } else {
            log.info("message queued order book not ready: {}, {}", message.getSequence(), message);
            queueMessage(message);
        }
    }

    public boolean isTimedOut() {
        return (System.currentTimeMillis() - sequenceLastProcessedTimeStamp) / 1000 >= timeout;
    }

    private void queueMessage(OrderBookMessage message) {
        queuedMessages.put(message.getSequence(), message);
    }

    public void processNextBatchIfAvailableInCache() {
        while (isReady && queuedMessages.containsKey(nextSeqIdToExpect)) {
            handleMessage(queuedMessages.get(nextSeqIdToExpect));
        }
    }

    private boolean isMessageTypeWeCareAbout(OrderBookMessage message) {
        return (isOpenOrder(message) || isCancelledOrder(message) || isMatchOrder(message) || isDoneFilledOrder(message));
    }

    private boolean isDoneFilledOrder(OrderBookMessage message) {
        return message.getType() != null && message.getType().equals(DONE)
                && message.getReason() != null && message.getReason().equals(FILLED)
                && message.getPrice() != null;
    }

    private boolean isOpenOrder(OrderBookMessage message) {
        return message.getType() != null && message.getType().equals(OPEN);
    }

    private boolean isMatchOrder(OrderBookMessage message) {
        return message.getType() != null && message.getType().equals(MATCH);
    }

    private boolean isCancelledOrder(OrderBookMessage message) {
        return message.getType() != null
                && message.getType().equals(DONE)
                && message.getReason() != null
                && message.getReason().equals(CANCELED);
    }

    private boolean isOutOfSequence(OrderBookMessage message) {
        return message.getSequence() != null && message.getSequence().compareTo(nextMessageToExpect()) != 0;
    }

    public void updateOrderBook(OrderBookMessage message) {
        OrderBookModel model = getRelevantOrderBookModel(message);
        model.insertInto(message);
    }

    public OrderBookModel getBids() {
        return bids;
    }

    public OrderBookModel getAsks() {
        return asks;
    }

    /**
     * matches indicate opposite side of the trade was done so this is necessary for
     * updating the correct side of the order book.
     */
    public OrderBookModel getRelevantOrderBookModel(OrderBookMessage message) {
        if (isEqual(message.getSide(), BUY)) {
            return bids;
        } else {
            return asks;
        }
    }

    private boolean isEqual(String type, String match) {
        return type != null && type.equals(match);
    }

    public Map<Long, OrderBookMessage> getQueuedMessages() {
        return queuedMessages;
    }

    public Long nextMessageToExpect() {
        return nextSeqIdToExpect;
    }

    public void setReady(boolean isReady) {
        this.isReady = isReady;
    }

    public long getTimeout() {
        return timeout;
    }
}

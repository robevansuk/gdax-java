package com.coinbase.exchange.api.websocketfeed;

import com.coinbase.exchange.api.exchange.Signature;
import com.coinbase.exchange.api.gui.orderbook.GdaxLiveOrderBook;
import com.coinbase.exchange.api.websocketfeed.message.OrderBookMessage;
import com.coinbase.exchange.api.websocketfeed.message.Subscribe;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.swing.*;
import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Websocketfeed adapted from someone else's code
 * <p>
 * >Jiji Sasidharan
 */
@Component
@ClientEndpoint
public class WebsocketFeed {

    static Logger log = LoggerFactory.getLogger(WebsocketFeed.class);

    Signature signature;

    Session userSession = null;
    MessageHandler messageHandler;

    String websocketUrl;

    private Set<String> subscribedChannels;

    private Boolean isEnabled;

    private String key;
    private String passphrase;

    @Autowired
    public WebsocketFeed(@Value("${websocket.baseUrl}") String websocketUrl,
                         @Value("${websocket.enabled}") Boolean isEnabled,
                         @Value("${gdax.key}") String key,
                         @Value("${gdax.passphrase}") String passphrase,
                         Signature signature) {
        this.key = key;
        this.passphrase = passphrase;
        this.signature = signature;
        this.websocketUrl = websocketUrl;
        this.isEnabled = isEnabled;
        this.subscribedChannels = new HashSet<>();
        init();
    }

    public void init() {
        if (isEnabled) {
            try {
                WebSocketContainer container = ContainerProvider.getWebSocketContainer();
                container.connectToServer(this, new URI(websocketUrl));
            } catch (Exception e) {
                log.error("Could not connect to remote server: " + e.getMessage() + ", " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Callback hook for Connection open events.
     *
     * @param userSession the userSession which is opened.
     */
    @OnOpen
    public void onOpen(Session userSession) {
        log.info("opening websocket");
        this.userSession = userSession;
    }

    /**
     * Callback hook for Connection close events.
     *
     * @param userSession the userSession which is getting closed.
     * @param reason      the reason for connection close
     */
    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        log.info("closing websocket: {}", reason);
        this.userSession = null;
    }

    /**
     * Callback hook for events. This method will be invoked when a client sends a message.
     */
    @OnMessage
    public void onMessage(String message) {
        if (this.messageHandler != null) {
            this.messageHandler.handleMessage(message);
        }
    }

    @OnError
    public void onError(Session s, Throwable t){
        log.error("WebsocketFeed error!!!");
        t.printStackTrace();
    }

    public void setMessageHandler(MessageHandler msgHandler) {
        this.messageHandler = msgHandler;
    }

    public void sendMessage(String message) {
        if(isEnabled) {
            this.userSession.getAsyncRemote().sendText(message);
        }
    }

    public void subscribe(String productId, GdaxLiveOrderBook liveOrderBook) {
        log.info("Subscribing to {}", productId);
        setSubscribedChannels(productId);
        Subscribe msg = new Subscribe((String[]) Arrays.asList(productId).toArray());
        String jsonSubscribeMessage = signObject(msg);

        setMessageHandler(json -> {
            SwingWorker<Void, OrderBookMessage> worker = new SwingWorker<Void, OrderBookMessage>() {
                @Override
                public Void doInBackground() {
                    log.info(json);
                    OrderBookMessage message = getObject(json, new TypeReference<OrderBookMessage>() {
                    });
//                    log.info("Message Recieved: {}", message.getSequence());
                    publish(message);
                    return null;
                }

                @Override
                protected void process(List<OrderBookMessage> chunks) {
                    if (chunks != null && chunks.size() > 0) {
                        for (OrderBookMessage message : chunks) {
                            liveOrderBook.handleMessages(message);
                        }
                    }
                }
            };
            worker.execute();
        });

        sendMessage(jsonSubscribeMessage);

        log.info("Initialising order book for {} complete", productId);
    }

    public void subscribe(String productId) {
        log.info("WebSocketFeed subscribing to {}", productId);
        setSubscribedChannels(productId);
        Subscribe msg = new Subscribe((String[]) Arrays.asList(productId).toArray());
        String jsonSubscribeMessage = signObject(msg);
        sendMessage(jsonSubscribeMessage);
        log.info("WebSocketFeed subscribtion message sent");
    }

    public String signObject(Subscribe jsonObj) {
        Gson gson = new Gson();
        String jsonString = gson.toJson(jsonObj);

        String timestamp = Instant.now().getEpochSecond() + "";
        jsonObj.setTimestamp(timestamp);
        jsonObj.setSignature(signature.generate("", "GET", jsonString, timestamp));
        jsonObj.setPassphrase(passphrase);
        jsonObj.setKey(key);

        return gson.toJson(jsonObj);
    }

    public <T> T getObject(String json, TypeReference<T> type) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(json, type);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setSubscribedChannels(String subscribedProducts) {
        this.subscribedChannels.clear();
        this.subscribedChannels.addAll(Arrays.asList(subscribedProducts));
    }

    /**
     * OrderBookMessage handler. Functional Interface.
     *
     * @author Jiji_Sasidharan
     */
    public interface MessageHandler {
        public void handleMessage(String message);
    }
}

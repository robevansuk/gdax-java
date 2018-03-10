package com.coinbase.exchange.api.gui.orderbook;

import com.coinbase.exchange.api.gui.orderbook.ux.GdaxTableCellRenderer;
import com.coinbase.exchange.api.marketdata.OrderItem;
import com.coinbase.exchange.api.websocketfeed.message.OrderBookMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import static com.coinbase.exchange.api.constants.GdaxConstants.*;
import static java.math.BigDecimal.ROUND_HALF_UP;
import static java.math.BigDecimal.ZERO;
import static java.util.stream.Collectors.toList;

public class OrderBookModel implements TableModel, TableModelListener {

    private static final Logger log = LoggerFactory.getLogger(OrderBookModel.class);

    private static String[] columnNames = {
            "price",
            "size",
            "#orders"
    };

    private Vector<Vector> data;
    private GdaxTableCellRenderer cellRenderer;

    public OrderBookModel() {
        this.data = new Vector<>();
        addTableModelListener(this);
    }

    EventListenerList listenerList = new EventListenerList();

    // listener stuff
    public void addTableModelListener(TableModelListener l) {
        listenerList.add(TableModelListener.class, l);
    }

    public void removeTableModelListener(TableModelListener l) {
        listenerList.remove(TableModelListener.class, l);
    }

    public void fireTableModelEvent(TableModelEvent e) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 1; i > 0; i--) {
            if (listeners[i] == TableModelListener.class) {
                ((TableModelListener) listeners[i + 1]).tableChanged(e);
            }
        }
    }

    // contents stuff

    public Class getColumnClass(int columnIndex) {
        if (getRowCount() > 0)
            return getValueAt(0, columnIndex).getClass();
        else
            return Object.class;
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public String getColumnName(int index) {
        return columnNames[index];
    }

    public int getRowCount() {
        return data.size();
    }

    public int size() {
        return data.size();
    }


    public Object getValueAt(int rowIndex, int columnIndex) {
        return data.get(rowIndex).get(columnIndex);
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        addTableEntryIfNotAlreadyPresent(rowIndex);

        if (aValue instanceof String) {
            data.get(rowIndex).set(columnIndex, (String) aValue);
        } else if (aValue instanceof BigDecimal) {
            data.get(rowIndex).set(columnIndex, aValue.toString());
        } else if (aValue instanceof Integer) {
            data.get(rowIndex).set(columnIndex, aValue + "");
        }

        fireAllChanged();

    }

    private void addTableEntryIfNotAlreadyPresent(int rowIndex) {
        if (rowIndex >= getRowCount()) {
            data.add(new Vector(3));
            for (int i = 0; i < 3; i++) {
                data.get(rowIndex).add("");
            }
        }
    }

    public void removeRow(int index) {
        data.remove(index);
    }

    public void tableChanged(TableModelEvent e) {
        switch (e.getType()) {
            case TableModelEvent.DELETE: {

                fireAllChanged();
                break;
            }
            case TableModelEvent.INSERT: {

                fireAllChanged();
                break;
            }
            case TableModelEvent.UPDATE: {

                fireAllChanged();
                break;
            }
        }
    }

    protected void fireAllChanged() {
        TableModelEvent e = new TableModelEvent(this);
        fireTableModelEvent(e);
    }

    /**
     * inserts orderItems at the right location in the table.
     * OrderItems will be displayed in aggregated form.
     * Only used for initialising the orderbook.
     */
    public void insert(OrderItem item, String side) {
        Vector priceEntryVector = createNewVector();

        BigDecimal itemPrice = item.getPrice();
        int rowCount = data.size();

        int lastIndex = 0;
        if (side.equals(SELL)) {
            lastIndex = data.size() - 1;
        }

        if (rowCount != 0) {
            BigDecimal lastPriceEntry = new BigDecimal((String) data.get(lastIndex).get(PRICE_COL));
            if (lastPriceEntry.compareTo(itemPrice) == 0) {
                // if price of the incoming item is equal to last item on list, aggregate this order with the rest,
                // otherwise, create a new entry at the end of the list.
                updateExistingEntry(item, lastIndex);
            } else {
                if (side.equals(BUY)) {
                    createNewEntry(item, priceEntryVector, 0);
                } else {
                    createNewEntry(item, priceEntryVector, lastIndex + 1);
                }
            }
        } else {
            createNewEntry(item, priceEntryVector, 0);
        }
    }

    private BigDecimal getSize(OrderItem item) {
        if (isMatchOrder(item)) {
            return getSizeAsBigDecimal(item.getSize());
        } else if (isCanceledOrder(item)) {
            return getSizeAsBigDecimal(item.getRemainingSize());
        } else if (isOpenOrder(item)) {
            return getSizeAsBigDecimal(item.getRemainingSize());
        } else {
            if (item.getRemainingSize() != null) {
                return getSizeAsBigDecimal(item.getRemainingSize());
            }
            return getSizeAsBigDecimal(item.getSize());
        }
    }

    /**
     * not obvious here because its handled higher up when we decide whether
     * or not we're interested in certain incoming messages, but the only type of
     * done order we're interested in taking notice of is where the reason is 'canceled'
     * so those are all we'll see here.
     */
    private BigDecimal getUpdatedSize(OrderItem item, BigDecimal currentSize) {
        if (isMatchOrder(item)) {
            return getSizeAsBigDecimal(currentSize.subtract(item.getSize()));
        } else if (isCanceledOrder(item)) {
            return getSizeAsBigDecimal(currentSize.subtract(item.getRemainingSize()));
        } else if (isOpenOrder(item)) {
            return getSizeAsBigDecimal(currentSize.add(item.getRemainingSize()));
        } else if (isDoneFilledOrder(item)) {
            return currentSize;
        } else if (isChangeOrderSize(item)) {
            return getSizeAsBigDecimal(currentSize.subtract(item.getOldSize().subtract(item.getNewSize())));
        } else {
            if (item.getRemainingSize() != null) {
                return getSizeAsBigDecimal(item.getRemainingSize());
            }
            return getSizeAsBigDecimal(currentSize.add(item.getSize()));
        }
    }

    private boolean isChangeOrderSize(OrderItem item) {
        return item.getMessageType() != null && item.getMessageType().equals(CHANGE)
                && item.getPrice() != null;
    }

    private boolean isOpenOrder(OrderItem item) {
        return item.getMessageType() != null && item.getMessageType().equals(OPEN);
    }

    private BigDecimal getSizeAsBigDecimal(BigDecimal size) {
        return size.setScale(SIZE_DECIMAL_PLACES, ROUND_HALF_UP);
    }

    private Integer getUpdatedQuantity(OrderItem item, int currentQuantity) {
        if (isCanceledOrder(item) && currentQuantity != 1) {
            return currentQuantity - 1;
        } else if (isDoneFilledOrder(item) && currentQuantity != 1) {
            return currentQuantity - 1;
        } else if (isMatchOrder(item)) {
            return currentQuantity;
        } else {
            return currentQuantity + 1;
        }
    }

    private boolean isMatchOrder(OrderItem item) {
        return item.getMessageType() != null && item.getMessageType().equals(MATCH);
    }

    private boolean isCanceledOrder(OrderItem item) {
        return item.getMessageType() != null && item.getMessageType().equals(DONE)
                && item.getReason() != null && item.getReason().equals(CANCELED);
    }

    private void createNewEntry(OrderItem item, Vector vector, int rowIndex) {
        // don't create new price entries for any type of Done order whose price doesn't already exist in the table
        if (!isDoneOrder(item)) {
            data.insertElementAt(vector, rowIndex);

            setValueAt(getPriceAsString(item.getPrice()), rowIndex, PRICE_COL);
            setValueAt(getSize(item), rowIndex, SIZE_COL);
            setValueAt(item.getNum().toString(), rowIndex, NUM_ORDERS_COL);
            if (!shouldRemoveRow(rowIndex)) {
                if (cellRenderer!=null) {
                    cellRenderer.flashRow(rowIndex, System.currentTimeMillis());
                }
            }
        }
    }

    private boolean isDoneOrder(OrderItem item) {
        return item.getMessageType() != null && item.getMessageType().equals(DONE);
    }

    private void updateExistingEntry(OrderItem item, int rowIndex) {
        // get order details at the same price
        Integer currentQuantity = Integer.parseInt((String) data.get(rowIndex).get(NUM_ORDERS_COL));
        BigDecimal currentPrice = getPriceAsBigDecimal((String) data.get(rowIndex).get(PRICE_COL));
        BigDecimal currentSize = getSizeAsBigDecimal((String) data.get(rowIndex).get(SIZE_COL));

        setValueAt(currentPrice, rowIndex, PRICE_COL);
        setValueAt(getUpdatedSize(item, currentSize), rowIndex, SIZE_COL);
        setValueAt(getUpdatedQuantity(item, currentQuantity), rowIndex, NUM_ORDERS_COL);
        if(!shouldRemoveRow(rowIndex)) {
            if (cellRenderer!=null) {
                cellRenderer.flashRow(rowIndex, System.currentTimeMillis());
            }
        }
    }

    private boolean isDoneFilledOrder(OrderItem item) {
        return item.getMessageType() != null && item.getMessageType().equals(DONE)
                && item.getReason() != null && item.getReason().equals(FILLED);
    }

    public void insertInto(OrderBookMessage msg) {

        List<OrderBookMessage> orderIndex = getListOfAllRelevantOrders(msg);
        Comparator<OrderBookMessage> priceComparator = orderBookMessagePriceComparator();

        int index = Collections.binarySearch(orderIndex, msg, priceComparator);

        if (index < 0) {
            // item did not exist so negative index for the insertion point was returned
            // insert item at this point
            index = (index * -1) - 1;
            log.info("New price {}, {}, sequenceId {}", msg.getPrice(),
                    msg.getRemaining_size() != null ? "remainingSize " + msg.getRemaining_size() : "size " + msg.getSize(),
                    msg.getSequence());
            createNewEntry(convertToOrderItem(msg), createNewVector(), index);
        } else if (index >= 0) {
            log.info("Updating price {}, {} from {}, sequenceId {}, best price {} and size {}", msg.getPrice(),
                    msg.getRemaining_size() != null ? "remainingSize " + msg.getRemaining_size() : "size " + msg.getSize(),
                    getValueAt(index, SIZE_COL),
                    msg.getSequence(),
                    getValueAt(0, PRICE_COL),
                    getValueAt(0, SIZE_COL));
            updateExistingEntry(convertToOrderItem(msg), index);
        }
    }

    private Vector createNewVector() {
        Vector emptyVector = new Vector();
        emptyVector.add("");
        emptyVector.add("");
        emptyVector.add("");
        return emptyVector;
    }

    private OrderItem convertToOrderItem(OrderBookMessage message) {
        return new OrderItem(getOrderPrice(message),
                message);
    }

    public BigDecimal getOrderPrice(OrderBookMessage msg) {
        return msg.getPrice().setScale(PRICE_DECIMAL_PLACES, ROUND_HALF_UP);
    }

    private Comparator<OrderBookMessage> orderBookMessagePriceComparator() {
        return new Comparator<OrderBookMessage>() {
            @Override
            public int compare(OrderBookMessage o1, OrderBookMessage o2) {
                if (isBuySideOrderType(o1)) {
                    return o1.getPrice().compareTo(o2.getPrice()) * -1; // fills a buy - high price at top
                } else {
                    return o2.getPrice().compareTo(o1.getPrice()) * -1; // fills a sell - low price at top
                }
            }
        };
    }

    private boolean isBuySideOrderType(OrderBookMessage o1) {
        return o1.getSide() != null && o1.getSide().equals(BUY);
    }

    private List<OrderBookMessage> getListOfAllRelevantOrders(OrderBookMessage msg) {
        return data.stream()
                .map(order -> {
                    OrderBookMessage message = new OrderBookMessage();
                    message.setPrice(getPriceAsBigDecimal((String) order.get(PRICE_COL)));
                    message.setSide(msg.getSide());
                    message.setOrder_id(msg.getOrder_id());
                    return message;
                })
                .collect(toList());
    }

    private boolean shouldRemoveRow(int rowUpdated) {
        if (getValueAt(rowUpdated, PRICE_COL) == null) {
            removeRow(rowUpdated);
            return true;
        } else {
            BigDecimal currentSize = getPriceAsBigDecimal((String) getValueAt(rowUpdated, SIZE_COL));
            if (isZeroOrLess(currentSize)) {
                removeRow(rowUpdated);
                return true;
            }
        }
        return false;
    }

    private boolean isZeroOrLess(BigDecimal value) {
        return value.compareTo(ZERO) <= 0;
    }

    private BigDecimal getPriceAsBigDecimal(String priceString) {
        return new BigDecimal(priceString).setScale(PRICE_DECIMAL_PLACES, ROUND_HALF_UP);
    }

    private BigDecimal getSizeAsBigDecimal(String sizeString) {
        return new BigDecimal(sizeString).setScale(SIZE_DECIMAL_PLACES, ROUND_HALF_UP);
    }

    private String getPriceAsString(BigDecimal priceValue) {
        return priceValue.setScale(PRICE_DECIMAL_PLACES, ROUND_HALF_UP).toString();
    }

    public void clear() {
        data.clear();
    }

    public void setCellRenderer(GdaxTableCellRenderer cellRenderer) {
        this.cellRenderer = cellRenderer;
    }
}

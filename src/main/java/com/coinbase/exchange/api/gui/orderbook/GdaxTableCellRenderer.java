package com.coinbase.exchange.api.gui.orderbook;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class GdaxTableCellRenderer extends DefaultTableCellRenderer {

    private static final int durationInMillis = 1000;
    private Map<Integer, Long> rowFaderStartTimes;

    public GdaxTableCellRenderer() {
        super();
        rowFaderStartTimes = new HashMap<>();
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (rowFaderStartTimes.containsKey(row) && !hasFadeCompleted(row)) {
            Color color = getColor(rowFaderStartTimes.get(row));
            setBackground(color);
            if (color.equals(Color.WHITE)) {
                rowFaderStartTimes.remove(row);
            }
        } else {
            setBackground(table.getBackground());
        }
        return this;
    }

    private Color getColor(Long startTimestamp) {
        BigDecimal timePassedAsAPercentageOfDuration = BigDecimal.valueOf(System.currentTimeMillis() - startTimestamp).divide(BigDecimal.valueOf(durationInMillis));
        if (timePassedAsAPercentageOfDuration.compareTo(BigDecimal.valueOf(100)) <= 0) {
            int timeBasedColor = BigDecimal.valueOf(255).multiply(timePassedAsAPercentageOfDuration).intValue();
            return new Color(timeBasedColor, timeBasedColor, 255);
        }
        return Color.WHITE;
    }

    private boolean hasFadeCompleted(Integer row) {
        return rowFaderStartTimes.containsKey(row) && (rowFaderStartTimes.get(row) + durationInMillis) < System.currentTimeMillis();
    }

    public void flashRow(int rowId, long startTimeInMillis) {
        rowFaderStartTimes.put(rowId, startTimeInMillis);
    }
}

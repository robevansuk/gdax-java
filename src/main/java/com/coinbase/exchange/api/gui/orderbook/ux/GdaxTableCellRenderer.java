package com.coinbase.exchange.api.gui.orderbook.ux;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static com.coinbase.exchange.api.constants.GdaxConstants.PRICE_COL;
import static com.coinbase.exchange.api.constants.GdaxConstants.SIZE_COL;

public class GdaxTableCellRenderer extends DefaultTableCellRenderer {

    private static final int durationInMillis = 800;
    private Map<Integer, Long> rowFaderStartTimes;

    public GdaxTableCellRenderer() {
        super();
        rowFaderStartTimes = new HashMap<>();
    }

    /**
     * This table cell renderer is applied to all cells. It's like a stamper for painting the various cells of the tables.
     * This method is called continuously so all you have to do is implement the logic behind picking the right colour.
     */
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
        if (column == SIZE_COL || column == PRICE_COL) {
            setHorizontalAlignment(SwingConstants.RIGHT);
        } else {
            setHorizontalAlignment(SwingConstants.CENTER);
        }
        return this;
    }

    /**
     * TODO - get clever and make added volume green and removed volume red - or something fancy. Could also
     * add cell highlighting to indicate larger volume - white = little volume, strong colour = more volume.
     * getting an accurate percentage value required the use of BigDecimals - not pretty but works.
     */
    private Color getColor(Long startTimestamp) {
        BigDecimal durationInMsBigDecimal = BigDecimal.valueOf(durationInMillis);
        BigDecimal elapsedTime = BigDecimal.valueOf(System.currentTimeMillis() - startTimestamp);

        BigDecimal timePassedAsAPercentageOfDuration = elapsedTime.divide(durationInMsBigDecimal);
        if (timePassedAsAPercentageOfDuration.compareTo(BigDecimal.valueOf(100)) <= 0) {
            int timeBasedColor = BigDecimal.valueOf(200).multiply(timePassedAsAPercentageOfDuration).intValue();
            return new Color(55+timeBasedColor, 225, 55+timeBasedColor); // Green
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

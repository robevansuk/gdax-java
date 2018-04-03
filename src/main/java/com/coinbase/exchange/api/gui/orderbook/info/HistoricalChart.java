package com.coinbase.exchange.api.gui.orderbook.info;

import com.coinbase.exchange.api.gui.orderbook.GdaxLiveOrderBook;
import com.coinbase.exchange.api.products.ProductService;
import com.coinbase.exchange.api.websocketfeed.message.OrderBookMessage;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.Timeline;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.data.time.Minute;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesDataItem;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.DefaultOHLCDataset;
import org.jfree.data.xy.OHLCDataItem;
import org.jfree.data.xy.XYDataset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

@Component
public class HistoricalChart extends JPanel {

    private GdaxLiveOrderBook liveOrderBook;
    private ProductService productService;
    private XYDataset dataset;
    private XYPlot mainPlot;

    @Autowired
    public HistoricalChart(GdaxLiveOrderBook liveOrderBook, ProductService productService) {
        super();
        this.liveOrderBook = liveOrderBook;
        this.productService = productService;
    }

    public ChartPanel init() {
        this.removeAll();
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        ChartPanel candleStickChart = createCandleStickChart();
        return candleStickChart;
    }

    private ChartPanel createCandleStickChart() {
        String selectedProductId = liveOrderBook.getSelectedProductId();
        List<List<BigDecimal>> historicalRates = productService.getHistoricRates(selectedProductId);

        DateAxis domainAxis = new DateAxis("Date");
        NumberAxis rangeAxis = new NumberAxis("Price");
        CandlestickRenderer renderer = new CandlestickRenderer();
        dataset = getDataSet(selectedProductId, historicalRates);

        mainPlot = new XYPlot(dataset, domainAxis, rangeAxis, renderer);

        //Do some setting up, see the API Doc
        renderer.setSeriesPaint(0, Color.BLACK);
        renderer.setDrawVolume(true);
        rangeAxis.setAutoRangeIncludesZero(false);
        domainAxis.setTimeline(getTimeline(selectedProductId, historicalRates));

        // Now create the chart and chart panel
        JFreeChart chart = new JFreeChart("", null, mainPlot, false);
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(600, 300));
        return chartPanel;
    }

    private Timeline getTimeline(String selectedProductId, List<List<BigDecimal>> historicalRates) {
        DateAxis dateAxis = new DateAxis(selectedProductId);
        Long firstTimeStamp = Long.parseLong(historicalRates.get(0).get(0).toString());
        Long lastTimeStamp = Long.parseLong(historicalRates.get(historicalRates.size() - 1).get(0).toString());

        dateAxis.setMinimumDate(new Date(firstTimeStamp));
        dateAxis.setMaximumDate(new Date(lastTimeStamp));

        return dateAxis.getTimeline();
    }

    private TimeSeries getTimeSeries(String selectedProductId, List<List<BigDecimal>> historicalRates) {
        TimeSeries timeSeries = new TimeSeries(selectedProductId);
        Long interval = Long.parseLong(historicalRates.get(0).get(0).subtract(historicalRates.get(1).get(0)).toString());
        // determine what unit of time the interval is.

        for (List<BigDecimal> historicRate : historicalRates) {
            Long endTime = Long.parseLong(historicRate.get(0).toString());
            RegularTimePeriod timePeriod = RegularTimePeriod.createInstance(Minute.class, new Date(endTime), TimeZone.getDefault(), Locale.ENGLISH);
            double one = 1; // TODO what is this param?
            timeSeries.add(new TimeSeriesDataItem(timePeriod, one));
        }
        return timeSeries;
    }


    protected AbstractXYDataset getDataSet(String productId, List<List<BigDecimal>> historicalRates) {
        OHLCDataItem[] data = toArrayOfOHLCDataItems(historicalRates);

        //Create a dataset: Timestamp: Open, High, Low, Close, Volume
        DefaultOHLCDataset result = new DefaultOHLCDataset(productId, data);

        return result;
    }

    private OHLCDataItem[] toArrayOfOHLCDataItems(List<List<BigDecimal>> historicalRates) {
        OHLCDataItem[] data = new OHLCDataItem[historicalRates.size()];

        int i = 0;
        for (List<BigDecimal> historicRate : historicalRates) {
            Date time = Date.from(Instant.ofEpochSecond(getTime(historicRate)));
            Double openPrice = getOpenPrice(historicRate);
            Double highPrice = getHighPrice(historicRate);

            Double lowPrice = getLowPrice(historicRate);
            Double closePrice = getClosePrice(historicRate);
            Double volume = getVolume(historicRate);

            data[i] = new OHLCDataItem(time, openPrice, highPrice, lowPrice, closePrice, volume);

            i++;
        }
        return data;
    }

    private Double getVolume(List<BigDecimal> historicalRate) {
        String volume = historicalRate.get(5).toString();
        return Double.parseDouble(volume);
    }

    private Double getClosePrice(List<BigDecimal> historicalRate) {
        String closePrice = historicalRate.get(4).toString();
        return Double.parseDouble(closePrice);
    }

    private Double getLowPrice(List<BigDecimal> historicalRate) {
        String lowPrice = historicalRate.get(3).toString();
        return Double.parseDouble(lowPrice);
    }

    private Double getHighPrice(List<BigDecimal> historicalRate) {
        String highPrice = historicalRate.get(2).toString();
        return Double.parseDouble(highPrice);
    }

    private Double getOpenPrice(List<BigDecimal> historicalRate) {
        String openPrice = historicalRate.get(1).toString();
        return Double.parseDouble(openPrice);
    }

    private String getEndTime(List<BigDecimal> historicalRate) {
        BigDecimal finalTime = historicalRate.get(0);
        return finalTime.toString();
    }

    private Long getTime(List<BigDecimal> historicalRate) {
        Long finalTime = Long.parseLong(historicalRate.get(0).toString());
        return finalTime;
    }

    public void priceTick(OrderBookMessage matchOrder) {

    }
}

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.stream.Collectors;

public class RiskMgmtPanel {

    private StockPortfolioApp stockPortfolioApp;

    public RiskMgmtPanel(StockPortfolioApp stockPortfolioApp) {
        this.stockPortfolioApp = stockPortfolioApp;
    }

    public Map<String, Double> computeBetas(Map<String, Map<Date, Double>> stockDataMap, Map<Date, Double> spxData) {
        Map<String, Double> betas = new HashMap<>();
        
        // Convert SPX data to a sorted list of dates
        java.util.List<Date> spxDates = spxData.keySet().stream().sorted().collect(Collectors.toList());

        // If there are less than 756 data points, adjust the limit
        int dataPointsToUse = Math.min(756, spxDates.size());

        // Use only the most recent 756 data points
        java.util.List<Date> recentDates = spxDates.subList(spxDates.size() - dataPointsToUse, spxDates.size());

        // Calculate SPX returns
        java.util.List<Double> spxReturns = calculateReturns(spxData, recentDates);

        // Calculate beta for each stock
        for (Map.Entry<String, Map<Date, Double>> entry : stockDataMap.entrySet()) {
            String stockSymbol = entry.getKey();
            Map<Date, Double> stockPrices = entry.getValue();

            // Filter and sort stock prices to match the recentDates
            java.util.List<Double> stockReturns = calculateReturns(stockPrices, recentDates);

            if (stockReturns.size() == spxReturns.size() && !stockReturns.isEmpty()) {
                double covariance = calculateCovariance(stockReturns, spxReturns);
                double spxVariance = calculateVariance(spxReturns);
                double beta = covariance / spxVariance;
                System.out.println("beta: " + beta);
                betas.put(stockSymbol, beta);
            }
        }

        return betas;
    }

    private java.util.List<Double> calculateReturns(Map<Date, Double> priceData, java.util.List<Date> dates) {
    	java.util.List<Double> returns = new ArrayList<>();
        for (int i = 1; i < dates.size(); i++) {
            Date previousDate = dates.get(i - 1);
            Date currentDate = dates.get(i);

            if (priceData.containsKey(previousDate) && priceData.containsKey(currentDate)) {
                double previousPrice = priceData.get(previousDate);
                double currentPrice = priceData.get(currentDate);
                if (previousPrice != 0) { // Avoid division by zero
                    double returnPercent = (currentPrice - previousPrice) / previousPrice;
                    returns.add(returnPercent);
                }
            }
        }
        return returns;
    }

    private double calculateCovariance(java.util.List<Double> x, java.util.List<Double> y) {
        double meanX = x.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double meanY = y.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        double covariance = 0.0;
        int n = x.size();
        for (int i = 0; i < n; i++) {
            covariance += (x.get(i) - meanX) * (y.get(i) - meanY);
        }
        return covariance / n;
    }

    private double calculateVariance(java.util.List<Double> data) {
        double mean = data.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = 0.0;
        for (double value : data) {
            variance += Math.pow(value - mean, 2);
        }
        return variance / data.size();
    }

    // Method to compute expected PnL based on % SPX index move
    public XYSeries computePnLSeries(String seriesName, Map<String, Double> portfolio, Map<String, Double> betas, double minMove, double maxMove) {
        XYSeries series = new XYSeries(seriesName);
        for (double spxMove = minMove; spxMove <= maxMove; spxMove += 0.01) {
            double pnl = 0.0;
            for (Map.Entry<String, Double> entry : portfolio.entrySet()) {
                String stockSymbol = entry.getKey();
                double notional = entry.getValue();
                double beta = betas.getOrDefault(stockSymbol, 0.0);
                pnl += beta * spxMove * notional;
            }
            series.add(spxMove * 100, pnl); // x-axis: % SPX move, y-axis: expected PnL
        }
        return series;
    }

    // Method to create the PnL chart
    public ChartPanel createPnLChart(String title, XYSeries series) {
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);

        JFreeChart chart = ChartFactory.createXYLineChart(
                title,
                "% SPX Index Move",
                "Expected PnL",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);

        // Customize the plot
        XYPlot plot = chart.getXYPlot();
        NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
        xAxis.setRange(-50, 50);

        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setAutoRangeIncludesZero(false);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesShapesVisible(0, false); // Hide shapes
        plot.setRenderer(renderer);

        return new ChartPanel(chart);
    }

    // Method to compute delta (PnL for 100% SPX index move)
    public double computeDelta(Map<String, Double> portfolio, Map<String, Double> betas) {
        double delta = 0.0;
        for (Map.Entry<String, Double> entry : portfolio.entrySet()) {
            String stockSymbol = entry.getKey();
            double notional = entry.getValue();
            double beta = betas.getOrDefault(stockSymbol, 0.0);
            delta += beta * 1.0 * notional; // 100% move
        }
        return delta;
    }
}

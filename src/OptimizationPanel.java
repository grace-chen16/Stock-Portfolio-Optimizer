import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class OptimizationPanel {

    private StockPortfolioApp stockPortfolioApp;
    private PortfolioOptimizer portfolioOptimizer;
    
    // Store the portfolio and optimized portfolio
    private Map<String, Integer> originalPtf;
    private Map<String, Integer> optimizedPtf;

    public OptimizationPanel(StockPortfolioApp stockPortfolioApp) {
        this.stockPortfolioApp = stockPortfolioApp;
        this.portfolioOptimizer = new PortfolioOptimizer();
    }

    public Map<String, Double> computeOptimizedPortfolio(double riskFreeRate) {
        // Fetch stock data using the existing portfolio
        Map<String, Map<Date, Double>> stockDataMap = stockPortfolioApp.getStockDataForPortfolio();
        
        // Check if stock data is available
        if (stockDataMap.isEmpty()) {
            System.out.println("No stock data available.");
            return new HashMap<>();
        }

        // Compute the optimized weights using the efficient frontier method
        Map<String, Double> optimizedWeights = portfolioOptimizer.computeEfficientFrontier(stockDataMap, riskFreeRate);

        // Store the portfolios
        originalPtf = stockPortfolioApp.getPortfolio();
        optimizedPtf = stockPortfolioApp.getOptimizedPortfolio(optimizedWeights);

        return optimizedWeights;
    }

    public void updateSummaryTable(DefaultTableModel tableModel, Map<String, Double> userInputNotional, Map<String, Double> userInputWeights, Map<String, Double> optimizedWeights, Map<String, Map<Date, Double>> stockDataMap) {
        tableModel.setRowCount(0); // Clear existing rows

        for (String stockSymbol : originalPtf.keySet()) {
            int userQuantity = originalPtf.get(stockSymbol);
            double userNotional = userInputNotional.getOrDefault(stockSymbol, 0.0);
            double userWeight = userInputWeights.getOrDefault(stockSymbol, 0.0);
            double optimizedWeight = optimizedWeights.getOrDefault(stockSymbol, 0.0);
            double optimizedNotional = userNotional * (optimizedWeight / userWeight);
            int optimizedQuantity = (int) (optimizedNotional / getCurrentPrice(stockSymbol));

            tableModel.addRow(new Object[]{
                stockSymbol,
                NumberFormatter.formatToInteger(userQuantity),
                NumberFormatter.formatToInteger(userNotional),
                NumberFormatter.formatToPercentage(userWeight),
                NumberFormatter.formatToInteger(optimizedQuantity),
                NumberFormatter.formatToInteger(optimizedNotional),
                NumberFormatter.formatToPercentage(optimizedWeight)
            });
        }
    }

    private double getCurrentPrice(String stockSymbol) {
        // Retrieve the most recent price from stock data
        Map<String, Map<Date, Double>> stockDataMap = stockPortfolioApp.getStockDataForPortfolio();
        Map<Date, Double> stockPrices = stockDataMap.get(stockSymbol);
        if (stockPrices != null && !stockPrices.isEmpty()) {
            return stockPrices.entrySet()
                              .stream()
                              .max(Map.Entry.comparingByKey()) // Get the most recent date
                              .map(Map.Entry::getValue)
                              .orElse(0.0);
        }
        return 0.0;
    }

    // Method to update the performance chart
    
    public void updatePerformanceChart(JPanel chartPanel, Map<String, Map<Date, Double>> stockDataMap, Map<String, Double> optimizedWeights, Date fromDate, Date toDate) {
        chartPanel.removeAll(); // Clear existing chart

        // Debug: Print dates and data map size
        System.out.println("From Date: " + fromDate);
        System.out.println("To Date: " + toDate);
        System.out.println("Stock Data Map Size: " + stockDataMap.size());

        // Create series for original and optimized portfolios
        XYSeries originalPortfolioSeries = new XYSeries("Original Portfolio");
        XYSeries optimizedPortfolioSeries = new XYSeries("Optimized Portfolio");

        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;

        // Get the quantities for the original portfolio
        Map<String, Integer> portfolioQuantities = stockPortfolioApp.getPortfolioQuantities();

        // Calculate optimized quantities based on optimized weights
        Map<String, Integer> optimizedQuantities = new HashMap<>();
        double totalValue = portfolioQuantities.entrySet().stream()
            .mapToDouble(entry -> {
                String stockSymbol = entry.getKey();
                int quantity = entry.getValue();
                Double price = getCurrentPrice(stockSymbol);
                return quantity * price;
            }).sum();

        for (String stockSymbol : portfolioQuantities.keySet()) {
            Double optimizedWeight = optimizedWeights != null ? optimizedWeights.get(stockSymbol) : 0.0;
            int optimizedQuantity = (int) Math.round((optimizedWeight * totalValue) / getCurrentPrice(stockSymbol));
            optimizedQuantities.put(stockSymbol, optimizedQuantity);
        }

        // Calculate portfolio values over time
        for (Date date : stockDataMap.values().stream().findAny().orElse(new HashMap<>()).keySet()) {
            if (date.before(fromDate) || date.after(toDate)) {
                continue; // Skip dates outside the specified range
            }

            double originalPortfolioValue = 0.0;
            double optimizedPortfolioValue = 0.0;

            for (Map.Entry<String, Map<Date, Double>> stockEntry : stockDataMap.entrySet()) {
                String stockSymbol = stockEntry.getKey();
                Map<Date, Double> stockPrices = stockEntry.getValue();
                double price = stockPrices.getOrDefault(date, 0.0);

                // Calculate original portfolio value
                int originalQuantity = portfolioQuantities.getOrDefault(stockSymbol, 0);
                originalPortfolioValue += originalQuantity * price;

                // Calculate optimized portfolio value
                int optimizedQuantity = optimizedQuantities.getOrDefault(stockSymbol, 0);
                optimizedPortfolioValue += optimizedQuantity * price;
            }

            // Track min and max Y values for better Y-axis fitting
            minY = Math.min(minY, Math.min(originalPortfolioValue, optimizedPortfolioValue));
            maxY = Math.max(maxY, Math.max(originalPortfolioValue, optimizedPortfolioValue));

            originalPortfolioSeries.add(date.getTime(), originalPortfolioValue);
            optimizedPortfolioSeries.add(date.getTime(), optimizedPortfolioValue);
        }

        // Check if series contain any data
        if (originalPortfolioSeries.getItemCount() == 0 && optimizedPortfolioSeries.getItemCount() == 0) {
            JOptionPane.showMessageDialog(chartPanel, "No data available for the selected date range.", "Data Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Create dataset and add series
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(originalPortfolioSeries);
        dataset.addSeries(optimizedPortfolioSeries);

        // Update chart with new dataset
        JFreeChart chart = ChartFactory.createXYLineChart(
                "Portfolio Performance Comparison",
                "Date",
                "Portfolio Value",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);

        // Customize the plot with date axis and formatted Y-axis
        XYPlot plot = chart.getXYPlot();

        // Set the X-axis as DateAxis with date format
        DateAxis dateAxis = new DateAxis("Date");
        dateAxis.setDateFormatOverride(new SimpleDateFormat("MMM-yyyy"));
        plot.setDomainAxis(dateAxis);

        // Set Y-axis range for better fit
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setAutoRangeIncludesZero(false); // Optionally include zero
        yAxis.setRange(minY * 0.9, maxY * 1.1);

        // Improve the appearance of lines
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesShapesVisible(0, false); // Hide shapes for original series
        renderer.setSeriesShapesVisible(1, false); // Hide shapes for optimized series
        plot.setRenderer(renderer);

        // Create chart component and add it to the panel
        ChartPanel chartComponent = new ChartPanel(chart);
        chartPanel.add(chartComponent, BorderLayout.CENTER);
        chartPanel.revalidate();
        chartPanel.repaint();
    }


}

 
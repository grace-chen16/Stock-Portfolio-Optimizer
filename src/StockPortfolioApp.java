import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;

import java.awt.*;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.text.SimpleDateFormat;
import java.text.ParseException;

public class StockPortfolioApp extends JFrame {

    private JTabbedPane tabbedPane;
    private JPanel portfolioPanel;
    private JPanel optimizationPanel;
    private JPanel riskManagementPanel;

    private JComboBox<String> stockComboBox;
    private JTextField quantityField;
    private JButton addButton;
    private JButton plotButton;
    private JTable portfolioTable;
    private JDatePickerImpl fromDatePicker;
    private JDatePickerImpl toDatePicker;
    private XYSeriesCollection dataset;
    private Map<String, Integer> portfolio = new HashMap<>();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    // Shared weights result
    private Map<String, Double> optimizedWeights;
    private Map<String, Double> userInputWeights;

    public StockPortfolioApp() {
        setTitle("Stock Portfolio Manager");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Initialize components
        tabbedPane = new JTabbedPane();

        portfolioPanel = createPortfolioPanel();
        optimizationPanel = createOptimizationPanel();
        riskManagementPanel = createRiskManagementPanel();

        tabbedPane.addTab("Portfolio", portfolioPanel);
        tabbedPane.addTab("Optimization", optimizationPanel);
        tabbedPane.addTab("Risk Management", riskManagementPanel);

        add(tabbedPane);

        setVisible(true);
    }

    private JPanel createRiskManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        RiskMgmtPanel helper = new RiskMgmtPanel(this);

        // Input for the risk-free rate
        JPanel inputPanel = new JPanel();
        JTextField riskFreeRateField = new JTextField("0.02", 10); // Default to 2% risk-free rate
        inputPanel.add(new JLabel("Risk-Free Rate:"));
        inputPanel.add(riskFreeRateField);

        // Add the "Compute Risk" button
        JButton computeRiskButton = new JButton("Compute Risk");
        inputPanel.add(computeRiskButton);

        panel.add(inputPanel, BorderLayout.NORTH);

        // Create a panel to hold the charts
        JPanel chartPanel = new JPanel(new GridLayout(1, 2));
        panel.add(chartPanel, BorderLayout.CENTER);

        // Add action listener to the "Compute Risk" button
        computeRiskButton.addActionListener(e -> {
            double riskFreeRate;
            try {
                riskFreeRate = Double.parseDouble(riskFreeRateField.getText());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(panel, "Invalid risk-free rate. Please enter a valid number.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Fetch stock data and SPX data
            Map<String, Map<Date, Double>> stockDataMap = getStockDataForPortfolio();
            Map<Date, Double> spxData = getSpxData();

            // Compute betas
            Map<String, Double> betas = helper.computeBetas(stockDataMap, spxData);

            // Compute original portfolio's PnL series
            Map<String, Double> originalPortfolio = getPortfolioNotional(this.portfolio, stockDataMap);

            // Use the shared optimization result, or compute it if not already available
            if (optimizedWeights == null) {
                optimizedWeights = new OptimizationPanel(this).computeOptimizedPortfolio(riskFreeRate);
            }

            Map<String, Double> optimizedPortfolio = getOptimizedPortfolioNotional(optimizedWeights, stockDataMap, getTotalPortfolioNotional());

            // Compute PnL series for risk slides
            XYSeries originalPnLSeries = helper.computePnLSeries("Original Portfolio", originalPortfolio, betas, -0.5, 0.5);
            XYSeries optimizedPnLSeries = helper.computePnLSeries("Optimized Portfolio", optimizedPortfolio, betas, -0.5, 0.5);

            // Compute deltas
            double originalDelta = helper.computeDelta(originalPortfolio, betas);
            double optimizedDelta = helper.computeDelta(optimizedPortfolio, betas);

            // Clear previous content in chartPanel
            chartPanel.removeAll();

            // Create charts and add to chart panel
            ChartPanel originalChartPanel = helper.createPnLChart("Original Portfolio PnL", originalPnLSeries);
            originalChartPanel.getChart().addSubtitle(new TextTitle("Delta: " + NumberFormatter.formatToInteger(originalDelta)));

            ChartPanel optimizedChartPanel = helper.createPnLChart("Optimized Portfolio PnL", optimizedPnLSeries);
            optimizedChartPanel.getChart().addSubtitle(new TextTitle("Delta: " + NumberFormatter.formatToInteger(optimizedDelta)));

            chartPanel.add(originalChartPanel);
            chartPanel.add(optimizedChartPanel);

            // Refresh the UI
            panel.revalidate();
            panel.repaint();
        });

        return panel;
    }



    private JPanel createOptimizationPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Create the top panel for user inputs (risk-free rate, optimization button, and performance comparison button)
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JLabel riskFreeRateLabel = new JLabel("Risk-Free Rate:");
        JTextField riskFreeRateField = new JTextField("0.02", 10); // Default to 2%

        JButton optimizeButton = new JButton("Optimize Portfolio");
        JButton performanceComparisonButton = new JButton("Performance Comparison");

        inputPanel.add(riskFreeRateLabel);
        inputPanel.add(riskFreeRateField);
        inputPanel.add(optimizeButton);
        inputPanel.add(performanceComparisonButton);

        panel.add(inputPanel, BorderLayout.NORTH);

        // Create the portfolio summary table
        String[] columnNames = {"Stock Symbol", "User Input Quantity", "User Input Notional", "User Input Weights",
                "Optimized Quantity", "Optimized Notional", "Optimized Weights"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0);
        JTable summaryTable = new JTable(tableModel);
        JScrollPane tableScrollPane = new JScrollPane(summaryTable);

        panel.add(tableScrollPane, BorderLayout.CENTER);

        // Create the portfolio performance chart panel
        JPanel chartPanel = new JPanel(new BorderLayout());
        chartPanel.setPreferredSize(new Dimension(800, 400));
        panel.add(chartPanel, BorderLayout.SOUTH);

        OptimizationPanel helper = new OptimizationPanel(this);

        // Add action listener to the optimization button
        optimizeButton.addActionListener(e -> {
            double riskFreeRate;
            try {
                riskFreeRate = Double.parseDouble(riskFreeRateField.getText());
            } catch (NumberFormatException ex) {
                riskFreeRate = 0.02; // Default value
                JOptionPane.showMessageDialog(panel, "Invalid input for risk-free rate. Using default value of 0.02.", "Input Error", JOptionPane.WARNING_MESSAGE);
            }

            // Fetch stock data
            Map<String, Map<Date, Double>> stockDataMap = fetchStockData(this.portfolio);
            if (stockDataMap.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "No stock data available.");
                return;
            }

            // Calculate user input notional and weight
            Map<String, Double> userInputNotional = new HashMap<>();
            double totalValue = 0.0;

            for (Map.Entry<String, Integer> entry : this.portfolio.entrySet()) {
                String stockSymbol = entry.getKey();
                int quantity = entry.getValue();
                Map<Date, Double> stockPrices = stockDataMap.get(stockSymbol);

                if (stockPrices != null && !stockPrices.isEmpty()) {
                    // Use the most recent price available
                    Double price = getCurrentPrice(stockSymbol);
                    double notional = quantity * price;
                    userInputNotional.put(stockSymbol, notional);
                    totalValue += notional;
                }
            }

            Map<String, Double> userInputWeights = new HashMap<>();
            for (Map.Entry<String, Double> entry : userInputNotional.entrySet()) {
                String stockSymbol = entry.getKey();
                double notional = entry.getValue();
                double weight = totalValue > 0 ? notional / totalValue : 0.0;
                userInputWeights.put(stockSymbol, weight);
                this.userInputWeights = userInputWeights;
            }

            // Compute the optimized portfolio once and store it
            optimizedWeights = helper.computeOptimizedPortfolio(riskFreeRate);

            // Update summary table with user inputs
            helper.updateSummaryTable(tableModel, userInputNotional, userInputWeights, optimizedWeights, stockDataMap);

            // Update the portfolio performance chart
            //Date fromDate = getDateFromPicker(fromDatePicker);
            //Date toDate = getDateFromPicker(toDatePicker);
            //helper.updatePerformanceChart(chartPanel, stockDataMap, null, optimizedWeights, fromDate, toDate);
        });

        performanceComparisonButton.addActionListener(e -> {
            Date fromDate = getDateFromPicker(fromDatePicker);
            Date toDate = getDateFromPicker(toDatePicker);
            Map<String, Map<Date, Double>> stockDataMap = fetchStockData(this.portfolio);
            helper.updatePerformanceChart(chartPanel, stockDataMap,optimizedWeights, fromDate, toDate);
            //helper.updatePerformanceChart(chartPanel, stockDataMap, null, optimizedWeights, fromDate, toDate);
        });

        return panel;
    }

    private JPanel createPortfolioPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Input components
        //JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        stockComboBox = new JComboBox<>(getAllUniqueStockSymbols().toArray(new String[0]));
        quantityField = new JTextField(10);
        addButton = new JButton("Add to Portfolio");
        plotButton = new JButton("Plot Portfolio Performance");
        portfolioTable = new JTable(new PortfolioTableModel());

        UtilDateModel fromDateModel = new UtilDateModel();
        JDatePanelImpl fromDatePanel = new JDatePanelImpl(fromDateModel, new Properties());
        fromDatePicker = new JDatePickerImpl(fromDatePanel, new DateLabelFormatter());

        UtilDateModel toDateModel = new UtilDateModel();
        JDatePanelImpl toDatePanel = new JDatePanelImpl(toDateModel, new Properties());
        toDatePicker = new JDatePickerImpl(toDatePanel, new DateLabelFormatter());
        
        
        fromDatePicker.getModel().setDate(2019, 0, 1); // Jan 1, 2019
        toDatePicker.getModel().setDate(2024, 7, 13); // Aug 13, 2024
        
        
        JFreeChart chart = ChartFactory.createXYLineChart(
                "Portfolio Performance",
                "Date",
                "Value",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );
        
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(800, 400));

        JPanel inputPanel = new JPanel();
        inputPanel.add(new JLabel("Stock:"));
        inputPanel.add(stockComboBox);
        inputPanel.add(new JLabel("Quantity:"));
        inputPanel.add(quantityField);
        inputPanel.add(addButton);
        

        JPanel datePanel = new JPanel();
        datePanel.add(new JLabel("From:"));
        datePanel.add(fromDatePicker);
        datePanel.add(new JLabel("To:"));
        datePanel.add(toDatePicker);
        datePanel.add(plotButton);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(inputPanel, BorderLayout.NORTH);
        topPanel.add(datePanel, BorderLayout.SOUTH);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(portfolioTable), BorderLayout.CENTER);
        panel.add(chartPanel, BorderLayout.SOUTH);
        


        // Chart panel
        dataset = new XYSeriesCollection();

        

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setDomainAxis(new DateAxis("Date"));
        plot.setRangeAxis(new NumberAxis("Value"));

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        plot.setRenderer(renderer);


        // Add button action listener
        addButton.addActionListener(e -> {
            String stockSymbol = (String) stockComboBox.getSelectedItem();
            int quantity;
            try {
                quantity = Integer.parseInt(quantityField.getText());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(panel, "Invalid quantity. Please enter a valid number.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (quantity > 0) {
                addToPortfolio(stockSymbol, quantity);
            }
        });

        // Plot button action listener
        plotButton.addActionListener(e -> {
            Date fromDate = getDateFromPicker(fromDatePicker);
            Date toDate = getDateFromPicker(toDatePicker);
            updatePerformanceChart(chartPanel, fromDate, toDate);
        });

        return panel;
    }

    
    private void addToPortfolio(String stock, int quantity) {
        portfolio.put(stock, portfolio.getOrDefault(stock, 0) + quantity);
        updatePortfolioTable();
    }
    
    
    private void updatePortfolioTable() {
        double totalValue = portfolio.entrySet().stream()
                .mapToDouble(entry -> PortfolioTableModel.getMostRecentPrice(entry.getKey()) * entry.getValue())
                .sum();

        ((PortfolioTableModel) portfolioTable.getModel()).setData(portfolio, totalValue);
    }
    
    private List<String> getAllUniqueStockSymbols() {
        List<String> stockSymbols = new ArrayList<>();
        String url = "jdbc:sqlite:stocks.db";
        String sql = "SELECT DISTINCT stock_symbol FROM stock_data";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                stockSymbols.add(rs.getString("stock_symbol"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stockSymbols;
    }
	public void updatePerformanceChart(ChartPanel chartPanel, Date fromDate, Date toDate) {
        SwingUtilities.invokeLater(() -> {
            new Thread(() -> {
                // Fetch stock data for the selected portfolio
                Map<String, Map<Date, Double>> stockDataMap = fetchStockData(portfolio);
                if (stockDataMap.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "No stock data available.");
                    return;
                }

                // Prepare the time series
                XYSeries series = new XYSeries("Portfolio Value");


                if (fromDate == null || toDate == null) {
                    JOptionPane.showMessageDialog(this, "Please select both From and To dates.");
                    return;
                }

                //fromDate = removeTimeFromDate(fromDate);
                //toDate = removeTimeFromDate(toDate);

                Calendar calendar = Calendar.getInstance();
                calendar.setTime(fromDate);

                // Variables to determine the Y-axis range
                final double[] minValue = {Double.MAX_VALUE};
                final double[] maxValue = {Double.MIN_VALUE};

                // Iterate through each day in the date range
                while (!calendar.getTime().after(toDate)) {
                    Date currentDate = calendar.getTime();
                    double totalValue = 0.0;

                    // Calculate the total portfolio value on the current date
                    for (Map.Entry<String, Integer> entry : portfolio.entrySet()) {
                        String stockSymbol = entry.getKey();
                        int quantity = entry.getValue();
                        Map<Date, Double> stockPrices = stockDataMap.get(stockSymbol);

                        if (stockPrices != null) {
                            Double price = stockPrices.get(currentDate);
                            if (price != null) {
                                totalValue += price * quantity;
                            }
                        }
                    }

                    // Only add data points with non-zero value
                    if (totalValue > 0) {
                        series.add(currentDate.getTime(), totalValue);

                        // Update min and max values
                        if (totalValue < minValue[0]) minValue[0] = totalValue;
                        if (totalValue > maxValue[0]) maxValue[0] = totalValue;
                    }

                    calendar.add(Calendar.DAY_OF_MONTH, 1);
                }

                // Update the chart on the Event Dispatch Thread
                SwingUtilities.invokeLater(() -> {
                    // Create dataset and set it to the chart
                    XYSeriesCollection dataset = new XYSeriesCollection();
                    dataset.addSeries(series);

                    JFreeChart chart = ChartFactory.createXYLineChart(
                            "Portfolio Performance",
                            "Date",
                            "Value",
                            dataset,
                            PlotOrientation.VERTICAL,
                            true,
                            true,
                            false
                    );

                    XYPlot plot = chart.getXYPlot();
                    DateAxis dateAxis = new DateAxis("Date");
                    dateAxis.setDateFormatOverride(new SimpleDateFormat("yyyy-MM-dd"));
                    plot.setDomainAxis(dateAxis);

                    // Configure the Y-axis to fit the data range
                    NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
                    yAxis.setAutoRangeIncludesZero(false); // Optionally, you can keep zero if you want
                    yAxis.setRange(minValue[0] * 0.95, maxValue[0] * 1.05); // Adjust the range to fit data with some padding

                    //ChartPanel chartPanel = (ChartPanel) getContentPane().getComponent(0);
                    chartPanel.setChart(chart);
                    chartPanel.repaint();
                });
            }).start();
        });
    }
	
	public Map<String, Integer> getPortfolioQuantities() {
		return portfolio;
	}

    private Date getDateFromPicker(JDatePickerImpl datePicker) {
    	Date picked_date = (Date) datePicker.getModel().getValue();
        return removeTimeFromDate(picked_date);
    }
    
    private Date removeTimeFromDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Map<String, Map<Date, Double>> fetchStockData(Map<String, Integer> portfolio) {
        Map<String, Map<Date, Double>> stockDataMap = new HashMap<>();

        // Simulate fetching stock data from a database or an API
        for (String stockSymbol : portfolio.keySet()) {
            Map<Date, Double> stockPrices = new HashMap<>();
            try {
                stockPrices = getStockData(stockSymbol);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            stockDataMap.put(stockSymbol, stockPrices);
        }

        return stockDataMap;
    }
    


    private Map<Date, Double> getStockData(String stockSymbol) throws SQLException {
        Map<Date, Double> stockPrices = new HashMap<>();
        String url = "jdbc:sqlite:stocks.db";
        // Assume this method fetches data from a database
        // Example database query to get stock data
        String query = "SELECT date, close FROM stock_data WHERE stock_symbol = ? ORDER BY date ASC";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, stockSymbol);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String dateStr = rs.getString("date");
                Date date = null;
				try {
					date = DATE_FORMAT.parse(dateStr);
					//System.out.println("date in date format: "+ date);
				} catch (ParseException e) {
					System.out.println("reading data issue");
					e.printStackTrace();
				}
                double closingPrice = rs.getDouble("close");
                stockPrices.put(date, closingPrice);
            }
        }

        return stockPrices;
    }

    Map<String, Map<Date, Double>> getStockDataForPortfolio() {
        Map<String, Map<Date, Double>> stockDataMap = new HashMap<>();
        for (String stockSymbol : this.portfolio.keySet()) {
            try {
                stockDataMap.put(stockSymbol, getStockData(stockSymbol));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return stockDataMap;
    }

    public static Map<Date, Double> getSpxData() {
        Map<Date, Double> spxData = new HashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");

        String sql = "SELECT date, close FROM index_data";

        try (Connection conn = DatabaseUtil.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String dateString = rs.getString("date");
                double closePrice = rs.getDouble("close");

                try {
                    Date date = dateFormat.parse(dateString);
                    spxData.put(date, closePrice);
                    //System.out.println("date:"+ date + " and close price: " + closePrice);
                } catch (ParseException e) {
                    e.printStackTrace(); // Handle the exception
                }
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return spxData;
    }
    
    private Map<String, Double> getPortfolioNotional(Map<String, Integer> portfolio, Map<String, Map<Date, Double>> stockDataMap) {
        Map<String, Double> portfolioNotional = new HashMap<>();
        for (Map.Entry<String, Integer> entry : portfolio.entrySet()) {
            String stockSymbol = entry.getKey();
            int quantity = entry.getValue();
            Map<Date, Double> stockPrices = stockDataMap.get(stockSymbol);

            if (stockPrices != null && !stockPrices.isEmpty()) {
                // Use the most recent price available
                double price = getCurrentPrice(stockSymbol);
                portfolioNotional.put(stockSymbol, quantity * price);
            }
        }
        return portfolioNotional;
    }

    private Map<String, Double> getOptimizedPortfolioNotional(Map<String, Double> optimizedWeights, Map<String, Map<Date, Double>> stockDataMap, double totalNotional) {
        Map<String, Double> optimizedPortfolioNotional = new HashMap<>();
        for (Map.Entry<String, Double> entry : optimizedWeights.entrySet()) {
            String stockSymbol = entry.getKey();
            double weight = entry.getValue();
            double notional = weight * totalNotional;
            optimizedPortfolioNotional.put(stockSymbol, notional);
        }
        return optimizedPortfolioNotional;
    }

    public double getTotalPortfolioNotional() {
        double totalNotional = 0.0;

        // Assuming portfolio is a Map<String, Integer> where the key is the stock symbol and the value is the quantity.
        Map<String, Integer> portfolio = this.portfolio;

        // Fetch the latest stock data
        Map<String, Map<Date, Double>> stockDataMap = this.getStockDataForPortfolio();

        for (Map.Entry<String, Integer> entry : portfolio.entrySet()) {
            String stockSymbol = entry.getKey();
            int quantity = entry.getValue();

            // Get the most recent price for the stock
            double currentPrice = getCurrentPrice(stockSymbol);

            // Calculate the notional value for this stock and add it to the total
            totalNotional += quantity * currentPrice;
        }

        return totalNotional;
    }

    
    public Map<String, Integer> getOptimizedPortfolio(Map<String, Double> optimizedWeights) {
        Map<String, Integer> optimizedPortfolio = new HashMap<>();

        // Fetch the latest stock data
        Map<String, Map<Date, Double>> stockDataMap = this.getStockDataForPortfolio();

        // Get the total portfolio notional value (sum of all user input notionals)
        double totalNotional = this.getTotalPortfolioNotional();

        // Calculate optimized quantities for each stock
        for (Map.Entry<String, Double> entry : optimizedWeights.entrySet()) {
            String stockSymbol = entry.getKey();
            double optimizedWeight = entry.getValue();

            // Get the most recent price for the stock
            double currentPrice = getCurrentPrice(stockSymbol);

            // Calculate optimized notional and quantity
            double optimizedNotional = totalNotional * optimizedWeight;
            int optimizedQuantity = (int) Math.round(optimizedNotional / currentPrice);

            // Put the stock symbol and optimized quantity into the map
            optimizedPortfolio.put(stockSymbol, optimizedQuantity);
        }

        return optimizedPortfolio;
    }
    

    private double getCurrentPrice(String stockSymbol) {
        double currentPrice = 0.0;
        String url = "jdbc:sqlite:stocks.db";
        
        try {
            // Example query to fetch the most recent stock price
            String query = "SELECT close FROM stock_data WHERE stock_symbol = ? ORDER BY date DESC LIMIT 1";

            try (Connection conn = DriverManager.getConnection(url);
                 PreparedStatement stmt = conn.prepareStatement(query)) {

                stmt.setString(1, stockSymbol);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    currentPrice = rs.getDouble("close");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return currentPrice;
    }
    
    // Getter for portfolio
    public Map<String, Integer> getPortfolio() {
        return portfolio;
    }
    
    public void initializeDatabase() {
        try (InputStream dbStream = getClass().getClassLoader().getResourceAsStream("stocks.db")) {
            if (dbStream == null) {
                throw new RuntimeException("Database file not found in resources.");
            }
            // Copy the database to a writable location if needed
            Path dbPath = Files.createTempFile("stocks", ".db");
            Files.copy(dbStream, dbPath, StandardCopyOption.REPLACE_EXISTING);

            // Use dbPath.toString() as the path to your database in your code
            String databasePath = dbPath.toString();
            // Use the databasePath to connect to your SQLite database
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        StockPortfolioApp app = new StockPortfolioApp();
        //app.initializeDatabase();  // Initialize the database
    }
}

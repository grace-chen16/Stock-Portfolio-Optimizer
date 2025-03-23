

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.linear.*;
import org.apache.commons.math3.optim.*;
import org.apache.commons.math3.optim.linear.*;
import org.apache.commons.math3.optim.nonlinear.scalar.*;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.NelderMeadSimplex;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.SimplexOptimizer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


public class PortfolioOptimizer {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    public Map<String, Double> computeEfficientFrontier(Map<String, Map<Date, Double>> stockDataMap, double riskFreeRate) {
        List<String> stockSymbols = new ArrayList<>(stockDataMap.keySet());
        int numStocks = stockSymbols.size();
        
        // Define a number of recent data points to keep
        final int MAX_POINTS = 756; // 3 year historical data - 252 trading day each year

        // Fetch all dates and limit each stock to the most recent MAX_POINTS data points
        Map<Date, Integer> dateIndexMap = new TreeMap<>(); // Store date and corresponding index
        List<Map<Date, Double>> alignedStockData = new ArrayList<>(numStocks);
        for (String symbol : stockSymbols) {
            Map<Date, Double> priceData = stockDataMap.get(symbol);
            if (priceData == null) continue;

            // Get recent dates for the stock
            List<Date> recentDates = new ArrayList<>(priceData.keySet());
            recentDates.sort(Comparator.reverseOrder()); // Sort descending
            if (recentDates.size() > MAX_POINTS) {
                recentDates = recentDates.subList(0, MAX_POINTS);
            }

            // Ensure alignment of dates
            for (Date date : recentDates) {
                dateIndexMap.putIfAbsent(date, dateIndexMap.size()); // Assign index
            }

            Map<Date, Double> limitedPriceData = new LinkedHashMap<>();
            for (Date date : recentDates) {
                limitedPriceData.put(date, priceData.get(date));
            }

            alignedStockData.add(limitedPriceData);
        }

        // Create aligned returns data
        List<List<Double>> returnsData = new ArrayList<>(numStocks);
        for (Map<Date, Double> stockData : alignedStockData) {
            List<Double> returns = new ArrayList<>();
            List<Date> dates = new ArrayList<>(stockData.keySet());
            dates.sort(Comparator.naturalOrder());

            for (int i = 1; i < dates.size(); i++) {
                double previousPrice = stockData.get(dates.get(i - 1));
                double currentPrice = stockData.get(dates.get(i));
                double returnValue = (currentPrice - previousPrice) / previousPrice;
                returns.add(returnValue);
            }

            returnsData.add(returns);
        }

        // Ensure all return lists have the same length
        int returnsSize = returnsData.get(0).size();
        for (List<Double> returns : returnsData) {
            if (returns.size() != returnsSize) {
                throw new IllegalArgumentException("All return lists must have the same length.");
            }
        }

        // Convert returns data to matrix
        double[][] returnsArray = new double[numStocks][returnsSize];
        for (int i = 0; i < numStocks; i++) {
            returnsArray[i] = returnsData.get(i).stream().mapToDouble(d -> d).toArray();
        }
        RealMatrix returnsMatrix = MatrixUtils.createRealMatrix(returnsArray);
        
        //System.out.println("Returns Matrix: " + returnsMatrix);
        
        // Compute covariance matrix
        RealMatrix covarianceMatrix = calculateCovarianceMatrix(returnsMatrix);
        
        //System.out.println("Covariance Matrix: " + covarianceMatrix);
        //System.out.println("Returns Matrix Dimensions: " + returnsMatrix.getRowDimension() + "x" + returnsMatrix.getColumnDimension());
        //System.out.println("Covariance Matrix Dimensions: " + covarianceMatrix.getRowDimension() + "x" + covarianceMatrix.getColumnDimension());

        
        // Compute mean returns
        double[] meanReturns = new double[numStocks];
        for (int i = 0; i < numStocks; i++) {
            meanReturns[i] = returnsData.get(i).stream().mapToDouble(d -> d).average().orElse(0.0);
        }
        RealVector meanReturnsVector = new ArrayRealVector(meanReturns);

        // Define optimization problem
        return optimizePortfolio(covarianceMatrix, meanReturnsVector, stockSymbols, riskFreeRate);
    }


    private List<Double> computeReturns(Map<Date, Double> priceData) {
        List<Double> returns = new ArrayList<>();
        List<Date> dates = new ArrayList<>(priceData.keySet());
        dates.sort(Comparator.naturalOrder());

        for (int i = 1; i < dates.size(); i++) {
            double previousPrice = priceData.get(dates.get(i - 1));
            double currentPrice = priceData.get(dates.get(i));
            double returnValue = (currentPrice - previousPrice) / previousPrice;
            returns.add(returnValue);
        }
        
        return returns;
    }

    private RealMatrix calculateCovarianceMatrix(RealMatrix returnsMatrix) {
        // Print dimensions to debug
        //System.out.println("Returns Matrix Dimensions: " + returnsMatrix.getRowDimension() + "x" + returnsMatrix.getColumnDimension());

        // Calculate the covariance matrix
        RealMatrix covarianceMatrix = returnsMatrix.multiply(returnsMatrix.transpose())
            .scalarMultiply(1.0 / (returnsMatrix.getRowDimension() - 1));

        // Print the covariance matrix to debug
        // System.out.println("Covariance Matrix Dimensions: " + covarianceMatrix.getRowDimension() + "x" + covarianceMatrix.getColumnDimension());
        /* System.out.println("Covariance Matrix:");
        for (int i = 0; i < covarianceMatrix.getRowDimension(); i++) {
            System.out.println(Arrays.toString(covarianceMatrix.getRow(i)));
        }*/
    

        return covarianceMatrix;
    }



    private Map<String, Double> optimizePortfolio(RealMatrix covarianceMatrix, RealVector meanReturnsVector, List<String> stockSymbols, double riskFreeRate) {
        int numStocks = meanReturnsVector.getDimension();
        System.out.println("numStocks: " + numStocks);
        
        if (stockSymbols.isEmpty()) {
            throw new IllegalArgumentException("Stock symbols list cannot be empty.");
        }
        if (meanReturnsVector.getDimension() != stockSymbols.size()) {
            throw new IllegalArgumentException("Mismatch between stock symbols and mean returns vector dimension.");
        }
        

        // Define the objective function (negative Sharpe ratio to maximize)
        MultivariateFunction objectiveFunction = weights -> {
            RealVector weightsVector = new ArrayRealVector(weights);
            double portfolioReturns = meanReturnsVector.dotProduct(weightsVector);
            RealMatrix portfolioCovariance = covarianceMatrix.multiply(weightsVector.outerProduct(weightsVector));
            double portfolioVolatility = Math.sqrt(weightsVector.dotProduct(portfolioCovariance.operate(weightsVector)));
            double sharpeRatio = (portfolioReturns - riskFreeRate) / portfolioVolatility;
            return -sharpeRatio; // negate and then set goal to be minimizing
        };

        
        // Define the optimizer
        MultivariateOptimizer optimizer = new SimplexOptimizer(1e-10, 1e-30);
        // Increase the maximum number of evaluations and iterations
        int maxEvaluations = 1000;
        int maxIterations = 1000;

        // Initialize weights with equal distribution (1/n)
        double[] initialGuess = new double[numStocks];
        Arrays.fill(initialGuess, 1.0 / numStocks);

        // Perform the optimization
        PointValuePair result = optimizer.optimize(
            new MaxEval(maxEvaluations),  // Increase the maximum number of evaluations
            new MaxIter(maxIterations),   // Increase the maximum number of iterations
            new ObjectiveFunction(objectiveFunction),
            new InitialGuess(initialGuess),
            GoalType.MINIMIZE,
            new NelderMeadSimplex(numStocks)
        );

        // Retrieve optimal weights
        double[] weightsArray = result.getPoint();
        Map<String, Double> weightsMap = new HashMap<>();
        double totalWeight = Arrays.stream(weightsArray).sum();

        for (int i = 0; i < numStocks; i++) {
            weightsMap.put(stockSymbols.get(i), weightsArray[i] / totalWeight); // Normalize weights
        }

        return weightsMap;
    }


    public static void main(String[] args) {
        // Example usage
        Map<String, Integer> portfolio = new HashMap<>();
        portfolio.put("ETR", 1000);
        portfolio.put("CTVA", 1000);
        portfolio.put("CDW", 1000);
        portfolio.put("ABBV", 1000);
        portfolio.put("AIG", 1000);

        PortfolioOptimizer optimizer = new PortfolioOptimizer();
        // Provide stockDataMap based on your data
        Map<String, Map<Date, Double>> stockDataMap = fetchStockData(portfolio); // Populate this map with real data
        Map<String, Double> optimizedWeights = optimizer.computeEfficientFrontier(stockDataMap, 0.02);
        System.out.println("Optimized Weights: " + optimizedWeights);
    }

    
    private static Map<String, Map<Date, Double>> fetchStockData(Map<String, Integer> portfolio) {
        Map<String, Map<Date, Double>> stockDataMap = new HashMap<>();
        String url = "jdbc:sqlite:stocks.db";

        // Build the SQL query to fetch only the required stock symbols
        StringBuilder sqlBuilder = new StringBuilder("SELECT stock_symbol, date, close FROM stock_data WHERE stock_symbol IN (");
        for (String stockSymbol : portfolio.keySet()) {
            sqlBuilder.append("?,");
        }
        // Remove the last comma and close the parentheses
        sqlBuilder.setLength(sqlBuilder.length() - 1);
        sqlBuilder.append(")");

        String sql = sqlBuilder.toString();

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Set parameters for the query
            int index = 1;
            for (String stockSymbol : portfolio.keySet()) {
                pstmt.setString(index++, stockSymbol);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String stockSymbol = rs.getString("stock_symbol");
                    String dateStr = rs.getString("date");
                    Date date = DATE_FORMAT.parse(dateStr);
                    date = removeTimeFromDate(date); // Remove time from date
                    double close = rs.getDouble("close");

                    stockDataMap
                            .computeIfAbsent(stockSymbol, k -> new HashMap<>())
                            .put(date, close);
                }
            }
        } catch (SQLException | ParseException e) {
            e.printStackTrace();
        }
        return stockDataMap;
    }

    private static Date removeTimeFromDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }
    
}
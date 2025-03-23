import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TestStockDataQuery {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public static void main(String[] args) {
        // Define the stock symbol and date you want to query
        String stockSymbol = "MMM";
        String queryDate = "2011-08-04"; // Make sure this date exists in your database

        // Connect to the SQLite database
        String url = "jdbc:sqlite:stocks.db";
        String sql = "SELECT date, adj_close, close, high, low, open, volume FROM stock_data WHERE stock_symbol = ? AND date = ?";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Set the parameters
            pstmt.setString(1, stockSymbol);
            pstmt.setString(2, queryDate);

            // Execute the query
            ResultSet rs = pstmt.executeQuery();
            Date dateD = null;

            // Count rows by iterating
            int rowCount = 0;
            while (rs.next()) {
                rowCount++;
                String dateString = rs.getString("date");
                System.out.println("Date: " + dateString);
                try {
                    dateD = DATE_FORMAT.parse(dateString);
                    System.out.println("Date in Date format: " + dateD);
                } catch (ParseException e) {
                    System.out.println("Error parsing date.");
                    e.printStackTrace();
                }
                System.out.println("Adjusted Close: " + rs.getDouble("adj_close"));
                System.out.println("Close: " + rs.getDouble("close"));
                System.out.println("High: " + rs.getDouble("high"));
                System.out.println("Low: " + rs.getDouble("low"));
                System.out.println("Open: " + rs.getDouble("open"));
                System.out.println("Volume: " + rs.getLong("volume"));
            }

            System.out.println("Number of rows returned: " + rowCount);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

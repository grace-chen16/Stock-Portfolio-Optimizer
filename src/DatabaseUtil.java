import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseUtil {

    public static Connection connect() {
        // SQLite connection string
        String url = "jdbc:sqlite:stocks.db";
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url);
            if (conn != null) {
                DatabaseMetaData meta = conn.getMetaData();
                System.out.println("The driver name is " + meta.getDriverName());
                System.out.println("A new database has been created.");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }

    public static void createStockDataTableIfNotExists() {
        // SQL statement for creating the stock_data table
        String stockDataSql = "CREATE TABLE IF NOT EXISTS stock_data (\n"
                + "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + "    date TEXT NOT NULL,\n"
                + "    stock_symbol TEXT NOT NULL,\n"
                + "    adj_close REAL,\n"
                + "    close REAL,\n"
                + "    high REAL,\n"
                + "    low REAL,\n"
                + "    open REAL,\n"
                + "    volume INTEGER\n"
                + ");";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            // Create the stock_data table
            stmt.execute(stockDataSql);
            System.out.println("The stock_data table has been created or already exists.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void createIndexDataTableIfNotExists() {
        // SQL statement for creating the index_data table
        String indexDataSql = "CREATE TABLE IF NOT EXISTS index_data (\n"
                + "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + "    date TEXT NOT NULL,\n"
                + "    close REAL\n"
                + ");";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            // Create the index_data table
            stmt.execute(indexDataSql);
            System.out.println("The index_data table has been created or already exists.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}

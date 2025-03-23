import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class StockDataService {

    public List<StockData> getStockDataBySymbol(String symbol) {
        List<StockData> stockDataList = new ArrayList<>();
        String sql = "SELECT * FROM stock_data WHERE stock_symbol = ?";

        try (Connection conn = DatabaseUtil.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, symbol);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                StockData stockData = new StockData(
                        rs.getDate("date"),
                        rs.getString("stock_symbol"),
                        rs.getDouble("adj_close"),
                        rs.getDouble("close"),
                        rs.getDouble("high"),
                        rs.getDouble("low"),
                        rs.getDouble("open"),
                        rs.getLong("volume")
                );
                stockDataList.add(stockData);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return stockDataList;
    }
}

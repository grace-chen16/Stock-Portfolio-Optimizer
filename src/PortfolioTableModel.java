import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;

import javax.swing.table.AbstractTableModel;

public class PortfolioTableModel extends AbstractTableModel {
        private final String[] columnNames = {"Stock Symbol", "Quantity", "Current Notional", "% of Portfolio"};
        private java.util.List<Map.Entry<String, Integer>> data = new ArrayList<>();
        private double totalValue;

        public void setData(Map<String, Integer> portfolio, double totalValue) {
            this.data.clear();
            this.data.addAll(portfolio.entrySet());
            this.totalValue = totalValue;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }


        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Map.Entry<String, Integer> entry = data.get(rowIndex);
            String stockSymbol = entry.getKey();
            int quantity = entry.getValue();
            double currentNotional = 0;
			try {
				currentNotional = getMostRecentPrice(stockSymbol) * quantity;
			} catch (Exception e) {
				e.printStackTrace();
			}
            double percentage = totalValue > 0 ? (currentNotional / totalValue) * 100 : 0;

            switch (columnIndex) {
                case 0:
                    return stockSymbol;
                case 1:
                    return quantity;
                case 2:
                    return String.format("%.2f",currentNotional);
                case 3:
                    return String.format("%.2f%%", percentage);
                default:
                    return null;
            }
        }

        public static double getMostRecentPrice(String stockSymbol) {
            String url = "jdbc:sqlite:stocks.db";
            String sql = "SELECT close FROM stock_data WHERE stock_symbol = ? ORDER BY date DESC LIMIT 1";
            
            try (Connection conn = DriverManager.getConnection(url);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, stockSymbol);
                ResultSet rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    return rs.getDouble("close");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return 0.0;
        }

		@Override
        public String getColumnName(int column) {
            return columnNames[column];
        }
    }
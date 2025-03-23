import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class StockData {
    private Date date;
    private String stockSymbol;
    private double adjClose;
    private double close;
    private double high;
    private double low;
    private double open;
    private long volume;
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public StockData(Date date, String stockSymbol, double adjClose, double close, double high, double low, double open, long volume) {
    	this.date = date;
        this.stockSymbol = stockSymbol;
        this.adjClose = adjClose;
        this.close = close;
        this.high = high;
        this.low = low;
        this.open = open;
        this.volume = volume;
    }
    

    public StockData(Date date, String stockSymbol, double close) {
        this.date = date;
        this.stockSymbol = stockSymbol;
        this.close = close;
    }



    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getStockSymbol() {
        return stockSymbol;
    }

    public void setStockSymbol(String stockSymbol) {
        this.stockSymbol = stockSymbol;
    }

    public double getAdjClose() {
        return adjClose;
    }

    public void setAdjClose(double adjClose) {
        this.adjClose = adjClose;
    }

    public double getClose() {
        return close;
    }

    public void setClose(double close) {
        this.close = close;
    }

    public double getHigh() {
        return high;
    }

    public void setHigh(double high) {
        this.high = high;
    }

    public double getLow() {
        return low;
    }

    public void setLow(double low) {
        this.low = low;
    }

    public double getOpen() {
        return open;
    }

    public void setOpen(double open) {
        this.open = open;
    }

    public long getVolume() {
        return volume;
    }

    public void setVolume(long volume) {
        this.volume = volume;
    }

    @Override
    public String toString() {
        return "StockData{" +
                "date='" + date + '\'' +
                ", stockSymbol='" + stockSymbol + '\'' +
                ", adjClose=" + adjClose +
                ", close=" + close +
                ", high=" + high +
                ", low=" + low +
                ", open=" + open +
                ", volume=" + volume +
                '}';
    }
}

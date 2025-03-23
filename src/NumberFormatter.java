import java.text.DecimalFormat;

public class NumberFormatter {

    private static final DecimalFormat integerFormat = new DecimalFormat("#,###");
    private static final DecimalFormat percentageFormat = new DecimalFormat("#.##%");

    public static String formatToInteger(double value) {
        return integerFormat.format(Math.round(value));
    }
    

    public static String formatToPercentage(double value) {
        return percentageFormat.format(value);
    }
}

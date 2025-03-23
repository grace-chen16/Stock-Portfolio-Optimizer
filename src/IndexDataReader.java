import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

public class IndexDataReader extends JFrame {

    private static final int BATCH_SIZE = 5000; // Increased batch size for fewer batches
    private JProgressBar progressBar;
    private int totalLines;

    public IndexDataReader() {
        setTitle("Index Data Import");
        setSize(400, 100);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);

        add(new JLabel("Importing Index Data..."), BorderLayout.NORTH);
        add(progressBar, BorderLayout.CENTER);
    }

    public void readCSVAndStore(String filePath) {
        // Ensure the table exists
        DatabaseUtil.createIndexDataTableIfNotExists();

        SwingWorker<Void, Integer> worker = new SwingWorker<Void, Integer>() {
            @Override
            protected Void doInBackground() throws Exception {
                int lineCount = countLines(filePath);
                totalLines = lineCount;

                try (BufferedReader br = new BufferedReader(new FileReader(filePath));
                     Connection conn = DatabaseUtil.connect()) {

                    conn.setAutoCommit(false); // Disable auto-commit for better performance

                    // Erase all records in the index_data table
                    PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM index_data");
                    deleteStmt.executeUpdate();
                    deleteStmt.close();

                    PreparedStatement pstmt = conn.prepareStatement(
                            "INSERT INTO index_data(date, close) " +
                            "VALUES(?, ?)"
                    );

                    // Skip the header line
                    br.readLine();

                    String line;
                    int count = 0;

                    while ((line = br.readLine()) != null) {
                        String[] values = line.split(",");
                        if (values.length < 2) continue; // Skip if the row doesn't have all required columns

                        pstmt.setString(1, values[0]); // Date
                        pstmt.setDouble(2, Double.parseDouble(values[1])); // Close

                        pstmt.addBatch();
                        count++;

                        if (count % BATCH_SIZE == 0) {
                            pstmt.executeBatch();
                            publish(count); // Report progress
                        }
                    }

                    // Execute remaining batch
                    pstmt.executeBatch();
                    conn.commit(); // Commit the transaction
                    publish(count);
                } catch (IOException | SQLException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void process(java.util.List<Integer> chunks) {
                int processed = chunks.get(chunks.size() - 1);
                progressBar.setValue(Math.min(100, (processed * 100) / totalLines));
            }

            @Override
            protected void done() {
                try {
                    get(); // This will throw any exception from doInBackground
                    progressBar.setValue(100);
                    JOptionPane.showMessageDialog(null, "Data import completed successfully.");
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Error occurred during data import.");
                }
            }
        };

        worker.execute();
    }

    private int countLines(String filePath) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            int lines = 0;
            while (br.readLine() != null) lines++;
            return lines;
        }
    }
    

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                IndexDataReader reader = new IndexDataReader();
                reader.setVisible(true);
                // Call the method to read CSV and store data
                reader.readCSVAndStore("C:\\Users\\grace\\OneDrive\\Documents\\NYU Trandon\\CS9053 - Java\\Final\\Data\\sp500_index.csv");
            }
        });
    }
}

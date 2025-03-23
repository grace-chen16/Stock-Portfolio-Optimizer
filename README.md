# Stock-Portfolio-Optimizer

The Stock Portfolio Constructor and Optimizer application is a comprehensive tool designed to assist investors in constructing, optimizing, and managing risk and return on stock portfolios. The application leverages modern portfolio theory to optimize portfolios using the efficient frontier approach and integrates risk measurement to allow investor to assess the risk and make proper hedges. This report provides an overview of the system's architecture, functionality, and key features, along with implementation details.
The main objectives of this project were:
•	To develop a user-friendly application for constructing stock portfolios based on user inputs.
•	To implement portfolio optimization using efficient frontier theory, enabling users to maximize returns for a given level of risk.
•	To provide visualization on the portfolio performance (back testing)
•	To help the user measure the risks associated with the portfolio to allow user manage risk and return and can be used to determine what the proper hedge should be
•	To ensure the application is robust, with features like performance comparison, flexible charting options, and proper formatting of financial data.

2.	System Architecture

The system is designed as a desktop application using Java, with a graphical user interface (GUI) for user interaction. The architecture is modular, with separate components for portfolio management, optimization, and risk management.
The java code structure is:

![image](https://github.com/user-attachments/assets/65e67062-1be3-4b01-b45e-68273704e74c)

 

3.	Key Components:

1.	Portfolio Constructor: Allows users to input stock symbols, quantities, and view the portfolio summary.
2.	Portfolio Optimizer: Optimizes the portfolio based on efficient frontier theory using historical stock data.
3.	Risk Management Module: Provides tools to hedge portfolio risks using options, and includes a performance comparison feature.
4.	User Interface (UI): Developed using Swing, providing a responsive and intuitive interface for portfolio management and visualization.

4.  Functionality
4.1 Portfolio Construction
The portfolio constructor allows users to build a portfolio by specifying stock symbols and quantities. The application fetches the latest stock prices and calculates the notional value and weights of each stock in the portfolio. 
 ![image](https://github.com/user-attachments/assets/dac4cf38-bc75-4b79-b98b-7b23f8f872b4)


Key Features:
•	User-Defined Inputs: allows users to construct a stock portfolio.
•	Real-time portfolio composition computation: once “Add to Portfolio” button is clicked, the weights (% of portfolio) would be re-computed automatically
•	Back-testing of portfolio performance: the time series shows the portfolio performance from back-looking perspective. 


4.2 Portfolio Optimization
The optimization module uses historical stock data to compute the efficient frontier and optimize the portfolio for maximum returns at a given risk level. The user can input a risk-free rate, and the application calculates optimized weights for each stock.
Key Features:
•	Efficient Frontier Calculation: Utilizes the apache.commons.math3 library to perform optimization.
•	User-Defined Inputs: Allows for manual input of risk-free rate and portfolio parameters.
•	Summary Table: Displays a comparison between the user-defined portfolio and the optimized portfolio, including quantities, notional values, and weights.

 ![image](https://github.com/user-attachments/assets/286643a8-46b7-4f69-99a5-23f507672350)

Above chart can be interpreted as: to achieve the same value as of today, the initial investment for optimized portfolio would be lower than the initial investment needs to be made for what the user has specified in “Portfolio” tab. 


4.3 Risk Management
The risk management module provides measurement of the risk profile. It computes beta for each stock with respect to S&P index and the expected PnL chart shows should S&P index moves certain percentage, what the portfolio pnl would be. The expected PnL for each individual stock selection is beta adjusted with respect to S&P shock. Delta value measures the directional risk of the portfolio. Those metrics can help user to determine whether the investment fits their risk appetite and/or how much they would need to hedge in order to manage the risk (for example, they can purchase S&P Put options to offset some downside direction risks). 

![image](https://github.com/user-attachments/assets/33288448-0c81-4d9c-b189-ff652012ab31)


Above charts can be interpreted as: the optimized portfolio has smaller delta / directional risk than the original user input portfolio (same stocks selection just different weights in the portfolio). For example, given 50% downside drop to S&P index, original portfolio would have a loss of close to 600K while optimized portfolio would have a loss of less than 550K.

Key Features:
•	Performance Comparison: Compares the risk of the original and optimized portfolios over a user-defined date range.
•	Flexible Date Range: Users can select custom date ranges for performance analysis using date pickers.


5. Implementation Details
5.1 Data Feeding and Management
The application fetches historical stock data from a SQLite database (which was fed by a locally saved CSV file downloaded from Kaggle). The data is stored in a map structure (Map<String, Map<Date, Double>>) where the key is the stock symbol, and the value is a map of date-price pairs.
I attempted to develop functions to allow user download/extracts data directly from Yahoo Finance. However it wasn’t in success as it seems Yahoo disabled their Finance API service offering.  
5.2 Portfolio Optimization Logic
The optimization logic leverages the apache.commons.math3 library to compute the efficient frontier. The input data for optimization is processed and fed into the library, which returns the optimized weights for each stock. These weights are then used to update the portfolio summary and performance charts.
5.3 Risk Management
The risk computation function is encapsulated in the createRiskManagementPanel method. This method integrates beta adjusted computation of directional risk. The risk profile chart would allow users to assess risk and manage it (can be done by hedging with proper amount of index options).

6. User Interface
The user interface is designed with ease of use in mind:
•	Input Fields: For stock symbols, quantities, and risk-free rate.
•	Buttons: For triggering portfolio optimization and performance comparison.
•	Tables and Charts: To display portfolio summaries and performance over time.
•	Date Pickers: For selecting the date range for performance comparison.


7.	Techniques:
•	Java GUI Swing: user interface
•	Databases/JDBC: data storage and fetching via SQLite
•	Multithreading: multithreading technique was used to insert / upload data into database. Without applying this method, it takes hours for data uploading. With multithreading, it takes less than a minute to do so. 
•	Etc.

8. Testing and Validation
The application was tested extensively to ensure accuracy in calculations and robustness in performance. Unit tests were written for key functions, including data fetching, optimization logic, and risk management. The application was also validated with real market data to ensure its practical applicability.

9. Conclusion
The Stock Portfolio Constructor, Optimizer, and Risk Management System provides a powerful toolset for investors seeking to construct and manage portfolios efficiently. By integrating modern portfolio theory with practical risk management strategies, the application offers a comprehensive solution for portfolio management. The user-friendly interface and robust backend make it a valuable tool for both novice and experienced investors.


10. Future Enhancements:

•	Integration with Real-Time Data Feeds: explore other ways to provide real-time portfolio updates.
•	Improve the computation efficiency: the optimization process takes time (usually around 30 seconds for a portfolio with 5 stocks). Would improve the efficiency on optimization implementation.
•	Enhanced Visualization: More advanced charting options and performance metrics.
•	Expanded Risk Management Tools: Additional hedging strategies and scenario analysis.

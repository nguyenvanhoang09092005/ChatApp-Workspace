package database.connection;

import config.ServerConfig;
import java.sql.*;

public class DBConnection {

    private static Connection connection;

    /**
     * Get database connection (singleton pattern)
     */
    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(
                        ServerConfig.getDBUrl(),
                        ServerConfig.getDBUsername(),
                        ServerConfig.getDBPassword()
                );
                System.out.println("✓ Database connected successfully");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("Database connection failed");
            e.printStackTrace();
        }
        return connection;
    }

    /**
     * Test database connection
     */
    public static boolean testConnection() {
        try {
            Connection conn = getConnection();
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Close database connection
     */
    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("✓ Database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection");
            e.printStackTrace();
        }
    }

    /**
     * Close resources safely
     */
    public static void closeResources(ResultSet rs, PreparedStatement ps) {
        try {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initialize database tables if not exists
     */
    public static void initializeDatabase() {
        try {
            Connection conn = getConnection();
            Statement stmt = conn.createStatement();

            // Check if database exists
            ResultSet rs = stmt.executeQuery(
                    "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA " +
                            "WHERE SCHEMA_NAME = '" + ServerConfig.getDBName() + "'"
            );

            if (!rs.next()) {
                System.out.println("Database does not exist. Please run chatapp.sql script first.");
            } else {
                System.out.println("✓ Database initialized");
            }

            rs.close();
            stmt.close();

        } catch (SQLException e) {
            System.err.println("Error initializing database");
            e.printStackTrace();
        }
    }
}
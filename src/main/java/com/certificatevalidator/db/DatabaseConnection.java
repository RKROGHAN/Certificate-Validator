package com.certificatevalidator.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages SQLite database connection for certificate storage.
 */
public class DatabaseConnection {
    private static final String DB_URL = "jdbc:sqlite:certificate_validator.db";
    private static DatabaseConnection instance;
    private Connection connection;

    private DatabaseConnection() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
            initializeDatabase();
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    /**
     * Gets the singleton instance of DatabaseConnection
     */
    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    /**
     * Gets the database connection
     */
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(DB_URL);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get database connection", e);
        }
        return connection;
    }

    /**
     * Initializes the database schema
     */
    private void initializeDatabase() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Create certificates table
            stmt.execute("CREATE TABLE IF NOT EXISTS certificates (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "student_name TEXT NOT NULL, " +
                "course TEXT NOT NULL, " +
                "issue_date TEXT NOT NULL, " +
                "hash TEXT NOT NULL UNIQUE, " +
                "file_path TEXT, " +
                "file_hash TEXT, " +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                ")");

            // Create blockchain table
            stmt.execute("CREATE TABLE IF NOT EXISTS blockchain (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "block_index INTEGER NOT NULL, " +
                "timestamp TEXT NOT NULL, " +
                "certificate_hash TEXT NOT NULL, " +
                "previous_hash TEXT NOT NULL, " +
                "current_hash TEXT NOT NULL UNIQUE" +
                ")");

            // Create indexes for better performance
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_cert_hash ON certificates(hash)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_cert_file_hash ON certificates(file_hash)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_blockchain_hash ON blockchain(certificate_hash)");
        }
    }

    /**
     * Closes the database connection
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
        }
    }
}


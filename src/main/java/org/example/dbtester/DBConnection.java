package org.example.dbtester;

import java.sql.*;

public class DBConnection {
    private String jdbcDriverPath;
    private String dbURL;
    private String username;
    private String password;

    private Connection connection;

    // 构造器
    public DBConnection() {
    }

    public DBConnection(String jdbcDriverPath, String dbURL, String username, String password) {
        this.jdbcDriverPath = jdbcDriverPath;
        this.dbURL = dbURL;
        this.username = username;
        this.password = password;
    }

    // 连接数据库
    public boolean connect() {
        try {
            // 动态加载JDBC驱动
            Class.forName(jdbcDriverPath);
            // 建立连接
            this.connection = DriverManager.getConnection(dbURL, username, password);
            return true;
        } catch (ClassNotFoundException e) {
            System.out.println("JDBC Driver not found: " + e.getMessage());
            return false;
        } catch (SQLException e) {
            System.out.println("Failed to connect to the database: " + e.getMessage());
            return false;
        }
    }

    public String executeSQL(String sql) {
        if (this.connection == null) {
            return "No connection. Please connect to the database first.";
        }
        try (Statement statement = this.connection.createStatement()) {
            boolean isResultSet = statement.execute(sql);
            if (isResultSet) {
                return extractDataFromResultSet(statement.getResultSet());
            } else {
                return "Update count: " + statement.getUpdateCount();
            }
        } catch (SQLException e) {
            return "Error executing SQL statement: " + e.getMessage();
        }
    }

    // 从ResultSet中提取数据
    private String extractDataFromResultSet(ResultSet resultSet) throws SQLException {
        StringBuilder builder = new StringBuilder();
        while (resultSet.next()) {
            int columnCount = resultSet.getMetaData().getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                builder.append(resultSet.getString(i)).append(" ");
            }
            builder.append("\n");
        }
        return builder.toString();
    }

    // 检查数据库连接状态
    public boolean isConnected() {
        try {
            return this.connection != null && !this.connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    // 断开数据库连接
    public void disconnect() {
        if (this.connection != null) {
            try {
                this.connection.close();
            } catch (SQLException e) {
                System.out.println("Failed to close the database connection: " + e.getMessage());
            }
        }
    }

    // Getters and Setters
    public String getJdbcDriverPath() {
        return jdbcDriverPath;
    }

    public void setJdbcDriverPath(String jdbcDriverPath) {
        this.jdbcDriverPath = jdbcDriverPath;
    }

    public String getDbURL() {
        return dbURL;
    }

    public void setDbURL(String dbURL) {
        this.dbURL = dbURL;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}


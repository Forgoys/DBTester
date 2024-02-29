package org.example.dbtester;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

public class DBConnection {
    private String jdbcDriverPath;
    private String jdbcDriverClassName;
    private String dbURL;
    private String username;
    private String password;

    private Connection connection;

    public DBConnection() {
    }

    public DBConnection(String jdbcDriverPath, String jdbcDriverClassName, String dbURL, String username, String password, Connection connection) {
        this.jdbcDriverPath = jdbcDriverPath;
        this.jdbcDriverClassName = jdbcDriverClassName;
        this.dbURL = dbURL;
        this.username = username;
        this.password = password;
        this.connection = connection;
    }

    public boolean connect() {
        try {
            File jarFile = new File(jdbcDriverPath);
            URL jarUrl = jarFile.toURI().toURL();
            URLClassLoader loader = new URLClassLoader(new URL[]{jarUrl});
            // Assuming the driver class name can be determined from the jar file or is known beforehand
            // This could also be passed as a parameter if it varies
            Driver driver = (Driver) Class.forName(jdbcDriverClassName, true, loader).newInstance();
            // Wrap the driver so it can be registered
            DriverManager.registerDriver(new DriverShim(driver));
            this.connection = DriverManager.getConnection(dbURL, username, password);
            return true;
        } catch (Exception e) {
            System.out.println("Connection failed: " + e.getMessage());
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

    // Inner class to wrap the dynamically loaded driver
    private static class DriverShim implements Driver {
        private Driver driver;

        DriverShim(Driver d) {
            this.driver = d;
        }

        @Override
        public boolean acceptsURL(String u) throws SQLException {
            return this.driver.acceptsURL(u);
        }

        @Override
        public Connection connect(String u, Properties p) throws SQLException {
            return this.driver.connect(u, p);
        }

        @Override
        public int getMajorVersion() {
            return this.driver.getMajorVersion();
        }

        @Override
        public int getMinorVersion() {
            return this.driver.getMinorVersion();
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String u, Properties p) throws SQLException {
            return this.driver.getPropertyInfo(u, p);
        }

        @Override
        public boolean jdbcCompliant() {
            return this.driver.jdbcCompliant();
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return this.driver.getParentLogger();
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

    public String getJdbcDriverClassName() {
        return jdbcDriverClassName;
    }

    public void setJdbcDriverClassName(String jdbcDriverClassName) {
        this.jdbcDriverClassName = jdbcDriverClassName;
    }
}


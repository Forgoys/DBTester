package frontend.connection;

import javafx.fxml.FXML;

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

    /**
     * 这个数据库的名字，比如Postgresql等等
     */
    private String dbBrandName;

    /**
     * 要连接的数据库的名字
     */
    private String dbName;

    private Connection connection;

    public DBConnection() {
    }

    public DBConnection(String jdbcDriverPath, String dbURL, String username, String password) {
        this.jdbcDriverPath = jdbcDriverPath;
        this.dbURL = dbURL;
        this.username = username;
        this.password = password;

        analyzeURL(dbURL);
        // 注意：实际设置jdbcDriverClassName的逻辑可能需要根据jdbcDriverPath来实现，
        // 这里的实现可能需要你根据实际的JAR文件或其他逻辑来调整
        this.jdbcDriverClassName = determineDriverClassName(jdbcDriverPath);
    }

    /**
     * 根据数据库url分析数据库品牌名和数据库名
     * @param url
     */
    private void analyzeURL(String url) {
        if (url.startsWith("jdbc:postgresql://")) {
            this.dbBrandName = "PostgreSQL";
        } else if(url.startsWith("dbc:oscar://")) {
            this.dbBrandName = "oscar";
        } else if (url.startsWith("jdbc:mysql://")) {
            this.dbBrandName = "MySQL";
        } else if (url.startsWith("jdbc:oracle:thin:")) {
            this.dbBrandName = "Oracle";
        } else if (url.startsWith("jdbc:sqlserver://")) {
            this.dbBrandName = "SQL Server";
        } else if (url.startsWith("jdbc:sqlite:")) {
            this.dbBrandName = "SQLite";
        } // 可以根据需要添加更多的数据库品牌

        // 尝试解析数据库名
        int dbNameStart = url.lastIndexOf('/') + 1;
        int dbNameEnd = url.indexOf('?', dbNameStart);
        dbNameEnd = (dbNameEnd == -1) ? url.length() : dbNameEnd;
        this.dbName = url.substring(dbNameStart, dbNameEnd);
    }

    private String determineDriverClassName(String jdbcDriverPath) {
        // 这个方法的实现需要根据实际情况来设计。
        // 在很多情况下，驱动类名需要从用户那里获取或者通过分析驱动文件来确定。
        // 这里只是提供一个示例接口，实际实现可能会更复杂。

        // 尝试通过数据库品牌来获取驱动类名
        if(dbBrandName.equals("oscar")) {
            return "com.oscar.Driver";
        } else if(dbBrandName.equals("opengauss")) {
            return "org.opengauss.Driver";
        } // 可以根据需要添加更多的数据库品牌



        return ""; // 返回空字符串或根据路径推断的类名
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


    public String getDbBrandName() {
        return dbBrandName;
    }

    public void setDbBrandName(String dbBrandName) {
        this.dbBrandName = dbBrandName;
    }

    public String getDBName() {
        return dbName;
    }

    public void setDBName(String dbName) {
        this.dbName = dbName;
    }
}

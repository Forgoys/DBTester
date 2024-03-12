package frontend.connection;

import backend.tester.timeSeriesDB.PressTester;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;


public class DBConnection {
    private String jdbcDriverPath;
    private String jdbcDriverClassName;
    private String dbURL;
    private String username;
    private String password;
    private int port;

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
    // 涛思不用jdbc
    public DBConnection(String dbName, String username, String password) {
        this.dbName = dbName;
        this.username = username;
        this.password = password;
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
        if (url.startsWith("jdbc:postgresql")) {
            this.dbBrandName = "PostgreSQL";
        } else if(url.startsWith("jdbc:oscar")) {
            this.dbBrandName = "oscar";
        } else if (url.startsWith("jdbc:mysql")) {
            this.dbBrandName = "MySQL";
        } else if (url.startsWith("jdbc:sqlserver")) {
            this.dbBrandName = "SQL Server";
        } else if (url.startsWith("jdbc:sqlite")) {
            this.dbBrandName = "SQLite";
        } // 可以根据需要添加更多的数据库品牌

        // 解析端口号
        int portIndex = url.lastIndexOf(":");
        int slashIndex = url.indexOf("/", portIndex);
        String portString = url.substring(portIndex + 1, slashIndex);
        port = Integer.parseInt(portString);

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


    /**
     * 执行指定SQL文件中的SQL语句，并返回每条语句的执行结果。
     * @param sqlFilePath SQL文件的路径
     * @return 每条SQL语句执行结果的列表
     */
    public List<String> executeSQLFile(String sqlFilePath) {
        List<String> results = new ArrayList<>();

        if (this.connection == null) {
            results.add("无数据库连接，请先连接数据库。");
            return results;
        }

        String content;
        try {
            content = new String(Files.readAllBytes(Paths.get(sqlFilePath)));
        } catch (Exception e) {
            results.add("读取SQL文件错误: " + e.getMessage());
            return results;
        }

        String[] statements = content.split(";");

        for (String sql : statements) {
            sql = sql.trim();
            if (!sql.isEmpty()) {
                try (Statement statement = this.connection.createStatement()) {
                    boolean isResultSet = statement.execute(sql);
                    if (isResultSet) {
                        ResultSet resultSet = statement.getResultSet();
                        int rowCount = 0;
                        while (resultSet.next()) {
                            rowCount++;
                        }
                        results.add(String.valueOf(rowCount));
                    } else {
                        int updateCount = statement.getUpdateCount();
                        results.add(String.valueOf(updateCount));
                    }
                } catch (SQLException e) {
                    results.add("错误: " + e.getMessage());
                }
            }
        }

        return results;
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
    // 涛思不用jdbc
    public static String tdengineExecSQL(String sql, DBConnection dbConnection) {

//        PressTester.sourceBashrc();
        String command = "source ~/.bashrc && taos -u" + dbConnection.username + " -p" + dbConnection.password + " -s '" + sql + "'";
        String output = "";
    
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", "-c", command);
            Process process = processBuilder.start();
    
            // 读取输入流
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = inputReader.readLine()) != null) {
                output += line + "\n";
            }
    
            // 读取错误流
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                output += line + "\n";
            }
    
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    
        return output;
    }

    private static boolean checkDBUserPassword(DBConnection dbConnection) {
        try {
            // 输入指令：taos -u"root" -p"taosdata"能进入taos命令行
            String[] command = {"/bin/bash", "-c", "taos -u" + dbConnection.username + " -p" + dbConnection.password + " 2>&1"};
            Process process = Runtime.getRuntime().exec(command);

            // 向进程写入输入
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            writer.write("q\n");
            writer.flush();
            writer.close();

            // 读取命令行指令的输入流
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = inputReader.readLine()) != null) {
                // 打印命令行指令的执行结果

                // 检查是否存在"Authentication failure"的错误信息
                if (line.contains("Authentication failure") || line.contains("Invalid user") ){
                    return false;
                }
            }

            // 如果没有"Authentication failure"的错误信息，那么认为命令执行成功
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean checkDBExist(DBConnection dbConnection) {
        try {
            String[] command = {"/bin/bash", "-c", "taos -u" + dbConnection.username + " -p" + dbConnection.password + " -s \"use " + dbConnection.dbName + ";\" 2>&1"};
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = inputReader.readLine()) != null) {
                if (line.contains("Database not exist")) {
                    return false;
                }
            }
            return (checkDBUserPassword(dbConnection));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
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

    public int getPort() {
        return port;
    }
}

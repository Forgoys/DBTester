package backend.tester.rdb;

import backend.dataset.TestAllResult;
import backend.dataset.TestArguments;
import backend.dataset.TestResult;
import backend.tester.TestItem;
import frontend.connection.DBConnection;

import java.io.*;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import java.net.URL;
import java.net.URLClassLoader;

import java.sql.*;

import java.util.Properties;
import java.util.logging.Logger;


class MyDBConnection {
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

    public MyDBConnection() {
    }

    public MyDBConnection(String jdbcDriverPath, String dbURL, String username, String password) {
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
     *
     * @param url
     */
    private void analyzeURL(String url) {
        if (url.startsWith("jdbc:postgresql")) {
            this.dbBrandName = "PostgreSQL";
        } else if (url.startsWith("jdbc:oscar")) {
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
        if (dbBrandName.equals("oscar")) {
            return "com.oscar.Driver";
        } else if (dbBrandName.equals("opengauss")) {
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
            DriverManager.registerDriver(new MyDBConnection.DriverShim(driver));
            this.connection = DriverManager.getConnection(dbURL, username, password);
            return true;
        } catch (Exception e) {
            System.out.println("Connection failed: " + e.getMessage());
            return false;
        }
    }

    public boolean executeSQL(String sql) throws SQLException {
        if (this.connection == null) {
            return false;
        }
        Statement statement = this.connection.createStatement();
        boolean isResultSet = statement.execute(sql);
        if (isResultSet) {
            System.out.println(extractDataFromResultSet(statement.getResultSet()));
        } else {
            System.out.println("Update count: " + statement.getUpdateCount());
        }
        return true;
    }

    public boolean executeSQLScript(String filePath) throws SQLException {
        File script = new  File(filePath);
        List<String> sqlList = parseSQLScript(filePath);
        boolean ret = true;
        for(String sql : sqlList) {
            if(!executeSQL(sql)) {
                ret = false;
            }
        }
        return ret;
    }

    private static List<String> parseSQLScript(String scriptFilePath) {
        List<String> sqlStatements = new ArrayList<>();
        StringBuilder sqlStatementBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(scriptFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sqlStatementBuilder.append(line).append("\n");
                if (line.trim().endsWith(";")) {
                    sqlStatements.add(sqlStatementBuilder.toString());
                    sqlStatementBuilder.setLength(0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sqlStatements;
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


public class PressureTester extends TestItem {

    /**
     * 测试根目录
     */
    public String testHomePath;

    /**
     * 测试语句所在路径
     */
    private String sqlsPath;
    /**
     * sql文件
     */
    private File[] sqlFiles;

    /**
     * 结果文件存放路径
     */
    private String resultDirectory;

    /**
     * 数据库连接相关参数
     */
    private String jdbcDrivePath;
    private String jdbcUrl;
    private String username;
    private String password;

    /**
     * 测试时长，单位ms
     */
    private final int test_time;

    /**
     * 测试规模
     */
    private int dataSize = 1;

    /**
     * 并发线程数
     */
    private int thread_num = 8;


    public PressureTester() {
        test_time = 0;
    }

    public PressureTester(String testName, DBConnection DBStmt, TestArguments testArgs) {

        this.testName = testName;
        this.DBStmt = DBStmt;
        this.testArgs = testArgs;
        this.jdbcDrivePath = DBStmt.getJdbcDriverPath();
        this.jdbcUrl = DBStmt.getDbURL();
        this.username = DBStmt.getUsername();
        this.password = DBStmt.getPassword();

        // 检查测试参数是否正确
        if (this.testArgs == null) {
            throw new IllegalArgumentException("测试参数未配置!");
        }
        dataSize = 1;
        this.thread_num = Integer.parseInt(testArgs.values.get(0));
        // 分钟转毫秒
        test_time = Integer.parseInt(testArgs.values.get(1)) * 60000;
    }


    /**
     * 初始化相关属性
     */
    private void initialization() throws Exception{

        // 获取工具包根目录
        File projectDir = new File(System.getProperty("user.dir"));
        toolsRootPath = String.format("%s/%s/", projectDir.getParentFile().getAbsolutePath(), TOOLS_ROOT_NAME);

        // 检查工具目录
        File toolRootDir = new File(toolsRootPath);
        if(!toolRootDir.exists() || !toolRootDir.isDirectory()) {
            throw new Exception("未检测到工具目录:：" + toolsRootPath);
        }

        // 创建压力测试测试目录
        if(!toolsRootPath.endsWith("/")) {
            toolsRootPath += "/";
        }
        testHomePath = toolsRootPath + "/RDB_test/pressure_test/";

        // 安装数据库所在磁盘
//        diskNameOfDB = "sdd";

        // sql查询语句所在目录
        sqlsPath = testHomePath + "queries/";
        File sqlsDir;
        if((sqlsDir = new File(sqlsPath)).exists()) {
            sqlFiles = sqlsDir.listFiles();
        }

        // 测试结果目录
        LocalDateTime localDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");
        String formatDateTime = formatter.format(localDateTime);
        // 结果目录格式类似于/results/16_2024-03-05_21-22-34/
        resultDirectory = String.format("%s%s/%d_%s/", testHomePath, "results", thread_num, formatDateTime);
    }

    @Override
    public void testEnvPrepare() throws Exception {
        // 利用TPCHTester类来生成测试数据并导入数据库
        DBConnection dbConnection = new DBConnection(jdbcDrivePath, jdbcUrl, username, password);
        dbConnection.connect();
        TestArguments arguments = new TestArguments();
        arguments.values.add(String.valueOf(dataSize)); // 测试规模
        TPCHTester tester = new TPCHTester("pressureTest", dbConnection, arguments);
        tester.testEnvPrepare();
        dbConnection.disconnect();
        // 压力测试相关参数初始化
        initialization();
    }

    @Override
    public void startTest() throws IOException, InterruptedException {
        // 启动多个线程
        PressureThread[] pressureThreads = new PressureThread[thread_num];
        for (int i = 0; i < thread_num; i++) {
            pressureThreads[i] =  new PressureThread();
            pressureThreads[i].start();
        }

        // 等待所有线程完成
        for (int i = 0; i < thread_num; i++) {
            pressureThreads[i].join();
        }

        // 获取各线程的执行结果
        int requestSum = 0, failedSum = 0;
        for (int i = 0; i < thread_num; i++) {
            requestSum += pressureThreads[i].getTotalRequestCount();
            failedSum += pressureThreads[i].getFailedRequestCount();
        }
        int avgRequest = requestSum / thread_num;
        int avgFailed = failedSum / thread_num;

        this.testResult = new TestResult();
        testResult.names = TestResult.PRESSURE_TEST_RES_NAMES;
        testResult.values = new String[] {String.valueOf(avgRequest), String.valueOf(avgFailed)};
    }

    @Override
    public List<List<Double>> getTimeData() {
        return null;
    }

    @Override
    public TestResult getTestResults() {
        return this.testResult;
    }

    @Override
    public String getResultDicName() {
        return new File(resultDirectory).getName();
    }

    @Override
    public void writeToFile(String resultPath) {
        File retFile = new File(resultPath, "result.txt");
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(retFile);
            pw.printf("%s %s", this.testResult.values[0], this.testResult.values[1]);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if(pw != null) {
                pw.close();
            }
        }
    }

    @Override
    public TestAllResult readFromFile(String resultPath) {
        File retFile = new File(resultPath, "result.txt");
        try(BufferedReader reader = new BufferedReader(new FileReader(retFile))) {
            String[] rets = reader.readLine().split(" ");
            if(rets.length == 2) {
                this.testResult = new TestResult();
                testResult.values = rets;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
       return new TestAllResult(this.testResult);
    }

    @Override
    public List<List<Double>> readFromFile1(String resultPath) {
        return List.of();
    }

    class PressureThread extends Thread {

        private int totalRequestCount;

        private int failedRequestCount;

        @Override
        public void run() {
            long startTime = System.currentTimeMillis();
            long id = getId();
            Random random = new Random(startTime + id);
            while (System.currentTimeMillis() - startTime < test_time) {
                // 连接数据库
                MyDBConnection dbConnection = new MyDBConnection(jdbcDrivePath, jdbcUrl, username, password);
                if (dbConnection.connect()) {
                    totalRequestCount++;
                    // 随机选择一条SQL语句
                    File sqlFile = sqlFiles[random.nextInt(sqlFiles.length)];
                    System.out.printf("线程%d选择sql语句:%s\n", id, sqlFile.getName());
                    // 执行语句
                    try {
                        if(!dbConnection.executeSQLScript(sqlFile.getAbsolutePath())) {
                            System.out.println("线程" + id + "执行失败！");
                            failedRequestCount++;
                        }
                    } catch (SQLException e) {
                        System.out.println("线程" + id + "执行失败！");
                        e.printStackTrace();
                        failedRequestCount++;
                    }
                }
                // 延迟
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        public int getTotalRequestCount() {
            return totalRequestCount;
        }

        public int getFailedRequestCount() {
            return failedRequestCount;
        }
    }

    public static void main(String[] args) {
        DBConnection dbConnection = new DBConnection("/home/wlx/cx/benchmarksql-5.0/lib/oscar/oscarJDBC.jar",
                "jdbc:oscar://10.181.8.146:2004/TPCH_5",
                "SYSDBA",
                "szoscar55");

        TestArguments arguments = new TestArguments();
        arguments.values.add("4"); // 线程数
        arguments.values.add("5"); // 测试分钟数

        TestItem tester = new PressureTester("pressureTest", dbConnection, arguments);

        try {

            tester.testEnvPrepare();

            tester.startTest();

            TestResult results = tester.getTestResults();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

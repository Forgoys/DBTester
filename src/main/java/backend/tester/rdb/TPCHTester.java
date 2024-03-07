package backend.tester.rdb;

import backend.dataset.TestAllResult;
import backend.dataset.TestArguments;
import backend.dataset.TestResult;
import backend.tester.TestItem;
import frontend.connection.DBConnection;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TPCHTester extends TestItem {

    /**
     * 指定要检查的文件名列表
     */
    public static final String[] DATA_SET_FILE_NAMES = "nation.csv region.csv part.csv supplier.csv partsupp.csv customer.csv orders.csv lineitem.csv config.csv".split(" ");

    /**
     * 相关脚本
     */
    private static final String DATA_GENERATE_SCRIPT = "generate_data.sh";
    private static final String[] DATA_IMPORT_SCRIPTS = new String[]{"importData_oscar.sh"};
    private static final String MONITOR_SCRIPT = "monitorDB.sh";


    public  String testHomePath;

    public  String tpchToolPath;

    /**
     * 数据集路径
     */
    private String dataSetPath;

    /**
     * 测试语句所在路径
     */
    private String sqlDir;

    /**
     * 结果文件路径
     */
    private String resultDirectory;

    /**
     * 数据规模
     */
    private int dataSize = 5;

    /**
     * 数据库所在磁盘名，如sda，sdc等
     */
    private String diskNameOfDB;

    public TPCHTester(){}

    public TPCHTester(String testName, String toolRootPath) {
        this.testName = testName;
        this.toolRootPath = toolRootPath;
    }

    public TPCHTester(String testName, String toolRootPath, DBConnection DBStmt, TestArguments testArgs) {
        this(testName, toolRootPath);
        this.DBStmt = DBStmt;
        this.testArgs = testArgs;
    }


    private void initialization() throws Exception{

        // 检查工具目录
        File toolRootDir = new File(toolRootPath);
        if(!toolRootDir.exists() || !toolRootDir.isDirectory()) {
            throw new Exception("未检测到工具目录:：" + toolRootPath);
        }

        // 检查测试参数是否正确
        if (this.testArgs == null) {
            throw new IllegalArgumentException("测试参数未配置!");
        }
        dataSize = Integer.parseInt(testArgs.values.get(0));

        // 安装数据库所在磁盘
        diskNameOfDB = "sdd";

        // 创建tpch测试目录
        if(!toolRootPath.endsWith("/")) {
            toolRootPath += "/";
        }
        testHomePath = toolRootPath + "RDB_test/tpch/";


        // tpch工具所在根目录
        tpchToolPath = testHomePath + "tpch-tool/";
        File tpchToolDir = new File(tpchToolPath);
        if(!tpchToolDir.exists()) {
            throw new Exception("未检测到tpch相关工具: " + tpchToolPath);
        }

        // 测试数据保存目录
        dataSetPath = testHomePath + "TPCH_Files/tpch_data_" + dataSize + "/";

        // sql查询语句所在目录
        sqlDir = tpchToolPath + "queries/";

        // 测试结果目录
        LocalDateTime localDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");
        String formatDateTime = formatter.format(localDateTime);
        // 结果目录格式类似于/home/wlx/cx/result20_oscar_2024-03-05_21-22-34/
        resultDirectory = String.format("%s%d_%s_%s/", testHomePath + "results/", dataSize, DBStmt.getDbBrandName(), formatDateTime);
    }

    /**
     * 测试环境准备：软件部署、数据集导入
     */
    @Override
    public void testEnvPrepare() throws Exception {

        // 相关变量赋值
        initialization();

        // 检查测试工具tpch是否存在
        prepareTools();

        // 准备测试数据
        dataPrepare();

        // 导入测试数据到数据库
        importDataSetToDB();

        // 测试准备就绪
        status = Status.READY;
    }


    /**
     * 开始测试
     */
    @Override
    public void startTest() throws IOException, InterruptedException {
        if (status == Status.UNPREPARED) {
            throw new InterruptedException("测试尚未就绪，请先调用环境配置函数");
        } else if (status == Status.RUNNING) {
            throw new InterruptedException("测试正在进行，请勿重复启动");
        } else if(status == Status.FINISHED) {
            throw new InterruptedException("测试已经结束，如需再次测试，请新建测试实例");
        }

        // 创建结果文件夹
        File resDir = new File(resultDirectory);
        if(!resDir.exists()) {
            resDir.mkdirs();
        }
        /*                                    正式执行测试                                       */
        // 启动系统资源监测脚本
        String cmd = String.format("%s %d %s %s &", MONITOR_SCRIPT, 2, resultDirectory+"monitor.csv", diskNameOfDB);
        String out = execCommandsWithReturn(new File(tpchToolPath), cmd);
        String pid = null;
        if(!out.isEmpty()) {
            pid = out.trim().split(" ")[1];
        }
        // 开始测试
        executeSQLFilesInDirectory(sqlDir);
        // 关闭系统资源监测脚本
        execCommandsWithReturn("kill " + "pid");
        this.status = Status.FINISHED;
    }

    private void executeSQLFilesInDirectory(String sqlFilesPath) {

        File directory = new File(sqlFilesPath);
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".sql"));

        if (files == null) {
            System.out.println("指定目录中没有 SQL 文件");
            return ;
        }

        double totalTime = 0;
        try (FileWriter writer = new FileWriter(resultDirectory+"result.txt")) {
            testResult.values = new String[files.length];
            for(int i = 1; i <= files.length; i++) {

                long startTime = System.currentTimeMillis();
                File file = new File(sqlFilesPath, "db" + i + ".sql");
                List<String> res= DBStmt.executeSQLFile(file.getAbsolutePath());
                for(String str : res) {
                    System.out.println(str);
                }
                long endTime = System.currentTimeMillis();
                double executionTime = (endTime - startTime) / 1000.0;
                totalTime += executionTime;
                if(i != 1) {
                    writer.write(" ");
                }
                writer.write(String.format("%.3f", executionTime));
            }
            writer.write(String.format(" %.3f", totalTime));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 检查工具是否存在
     */
    private void prepareTools() throws Exception {
        // 检查是否存在工具包目录
        File toolDir = new File(tpchToolPath);
        if (!toolDir.exists() || !toolDir.isDirectory()) {
            throw new Exception("未检测到工具包！");
        }
        // 检查是否存在22条sql语句

    }

    public void dataPrepare() throws Exception {
        // 不存在数据集则创建数据集
        if (!existDataSetFile(dataSetPath)) {
            createDataSet(dataSetPath);
        }
    }

    /**
     * 检查测试数据集是否存在
     */
    private boolean existDataSetFile(String dataSetPath) throws Exception {
        // 检查目录是否存在
        File dataSetDir = new File(dataSetPath);
        if (!dataSetDir.exists() || !dataSetDir.isDirectory()) {
            return false;
        }
        // 执行命令检查目录下是否存在所有数据文件
        String[] nameList = dataSetDir.list();
        HashSet<String> set = new HashSet<>(List.of(nameList));
        for (String name : DATA_SET_FILE_NAMES) {
            if (!set.contains(name)) {
                return false;
            }
        }

        // 检查 config.csv 文件中warehousesSize是否为要测试的dataSize
        File cfgFile = new File(dataSetPath, "config.csv");
        try (BufferedReader br = new BufferedReader(new FileReader(cfgFile))) {
            String firstLine = br.readLine(); // 读取第一行
            if (firstLine != null) {
                String[] parts = firstLine.split(","); // 使用逗号分割
                if (parts.length == 2 && parts[0].equals("size")) {
                    try {
                        int value = Integer.parseInt(parts[1].trim()); // 获取数字部分
                        return value == dataSize;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }


    private void createDataSet(String fileDir) throws Exception {
        // 执行数据生成脚本
        String cmd = String.format("%s %d %s", DATA_GENERATE_SCRIPT, dataSize, dataSetPath);
        execCommands(new File(tpchToolPath), cmd);
        // 额外再生成一个config.csv，保存当前数据集大小
        execCommands(new File(dataSetPath), String.format("echo 'size,%d' > config.csv ", dataSize));
        System.out.println("数据生成成功！保存在：" + dataSetPath);
    }

    // 执行文件导入数据库的脚本
    private void importDataSetToDB() throws Exception {
        // 检查数据库中是否已有数据
        String sqlRes = DBStmt.executeSQL("select cfg_value from config where cfg_name='size';");
        if (!sqlRes.contains(String.valueOf(dataSize))) {
            // 删除旧表
            List<String> outList = DBStmt.executeSQLFile(tpchToolPath + "drop_table.sql");
            for(String str : outList) {
                System.out.println(str);
            }

            // 建表
            outList = DBStmt.executeSQLFile(tpchToolPath + "create_table.sql");
            for(String str : outList) {
                System.out.println(str);
            }

            // 建约束
            outList = DBStmt.executeSQLFile(tpchToolPath + "add_restraint.sql");
            for(String str : outList) {
                System.out.println(str);
            }

            // 导数据
            String cmd = String.format("%s %s %d %s", DATA_IMPORT_SCRIPTS[0], DBStmt.getDBName(), DBStmt.getPort(), dataSetPath);
            execCommands(new File(tpchToolPath), cmd);
        }
    }

    @Override
    public List<List<Double>> getTimeData() {
        // 如果已经生成系统资源监测list，则直接返回
        if (this.timeDataList != null && !this.timeDataList.isEmpty()) {
            return this.timeDataList;
        }
        // 从文件读取监测数据
        // 系统资源监视结果文件路径
        File file = new File(resultDirectory + "monitor.csv");
        List<Double> userCpuUsageList = new ArrayList<>();
        List<Double> memoryUsageList = new ArrayList<>();
        List<Double> diskReadSpeedList = new ArrayList<>();
        List<Double> diskWriteSpeedList = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            boolean headerSkipped = false;
            while ((line = br.readLine()) != null) {
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue; // 跳过标题行
                }
                String[] parts = line.split(",");
                if (parts.length >= 6) { // 确保有足够的字段
                    userCpuUsageList.add(Double.parseDouble(parts[1]));
                    memoryUsageList.add(Double.parseDouble(parts[4]));
                    diskReadSpeedList.add(Double.parseDouble(parts[5]));
                    diskWriteSpeedList.add(Double.parseDouble(parts[6]));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        timeDataList = new ArrayList<>();
        timeDataList.add(userCpuUsageList);
        timeDataList.add(memoryUsageList);
        timeDataList.add(diskReadSpeedList);
        timeDataList.add(diskWriteSpeedList);
        return timeDataList;
    }


    @Override
    public TestResult getTestResults() {
        if(this.status != Status.FINISHED) {
            System.out.println("测试正在进行中，请稍等");
            return null;
        }
        // 读取结果文件
        try(BufferedReader br = new BufferedReader(new FileReader(resultDirectory + "result.txt"))) {
            testResult = new TestResult();
            testResult.values = br.readLine().split(" ");
            testResult.names = TestResult.TPCH_RES_NAMES;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return testResult;
    }


    @Override
    public String getResultDicName() {
        return null;
    }

    @Override
    public void writeToFile(String resultPath) {

    }

    @Override
    public TestAllResult readFromFile(String resultPath) {
        return null;
    }

    @Override
    public List<List<Double>> readFromFile1(String resultPath) {
        return null;
    }

    public static void main(String[] args) {
        DBConnection dbConnection = new DBConnection("/home/wlx/cx/benchmarksql-5.0/lib/oscar/oscarJDBC.jar",
                "jdbc:oscar://10.181.8.146:2003/TPCC_20",
                "SYSDBA",
                "szoscar55");
        dbConnection.connect();

        TestArguments arguments = new TestArguments();
        arguments.values.add("5");


        TPCHTester tester = new TPCHTester("tpch", "/home/wlx/DBTestTools", dbConnection, arguments);

        try {

            tester.testEnvPrepare();

            tester.startTest();

            List<List<Double>> tmpTimeData = tester.getTimeData();

            TestResult results = tester.getTestResults();

            results = null;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        dbConnection.disconnect();
    }
}

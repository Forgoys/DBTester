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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 目前仅支持神通数据库，若需要支持另外两数据库，需要新增脚本文件
 */
public class TPCCTester extends TestItem {


    /**
     * 要检查的文件名列表
     */
    public static final String[] DATA_SET_FILE_NAMES = {"config.csv", "cust-hist.csv", "customer.csv", "district.csv", "item.csv",
            "new-order.csv", "order.csv", "order-line.csv", "stock.csv", "warehouse.csv"};

    /**
     * 相关脚本，数组形式的需要针对每个数据库都写一个版本
     */
    private static final String DATA_GENERATE_SCRIPT = "./runLoader.sh";
    private static final String[] DATA_IMPORT_SCRIPTS = new String[]{"./import_data_TPCC.sh"};
    private static final String AUTOTEST_SCRIPT = "./auto_test_one.sh";

    /**
     * 模板配置文件名字
     */
    public static final String TMP_PROPS_NAME = "props";

    /**
     * 相关路径
     */
    private String testHomePath;
    private String toolHomePath;
    private String toolPath;

    /**
     * 数据集路径
     */
    private String dataSetPath;

    /**
     * 配置文件名
     */
    private String propsFileName;

    /**
     * 数据规模
     */
    private int dataSize = 20;

    /**
     * 并发进程数
     */
    private int terminals = 8;

    /**
     * 加载进程数
     */
    private int loadWorkers = 1;

    /**
     * 运行时长，分钟数
     */
    private int runMins = 5;

    /**
     * 结果文件路径
     */
    private String resultDirectory;

    /**
     * 数据库所在磁盘名，如sda，sdc等
     */
    private String diskNameOfDB;

    public TPCCTester(){}

    public TPCCTester(String testName) {
        this.testName = testName;
//        this.toolsRootPath = toolsRootPath;
    }

    public TPCCTester(String testName, DBConnection DBStmt, TestArguments testArgs) {
        this(testName);
        this.DBStmt = DBStmt;
        this.testArgs = testArgs;
    }


    private void initialization() throws Exception{

        // 获取工具包根目录
        File projectDir = new File(System.getProperty("user.dir"));
        toolsRootPath = String.format("%s/%s/", projectDir.getParentFile().getAbsolutePath(), TOOLS_ROOT_NAME);

        // 检查工具目录
        File toolRootDir = new File(toolsRootPath);
        if(!toolRootDir.exists() || !toolRootDir.isDirectory()) {
            throw new Exception("未检测到工具目录:：" + toolsRootPath);
        }

        // 检查测试参数是否正确
        if (this.testArgs == null) {
            throw new IllegalArgumentException("测试参数未配置!");
        }
        dataSize = Integer.parseInt(testArgs.values.get(0));
        terminals = Integer.parseInt(testArgs.values.get(1));
        loadWorkers = Integer.parseInt(testArgs.values.get(2));
        runMins = Integer.parseInt(testArgs.values.get(3));
        if (dataSize * 10 <= terminals) {
            throw new IllegalArgumentException("并发线程数需要小于10倍数据规模!");
        }

        // 配置文件命名格式：“props_dbBrand_dataSize"
        propsFileName = "props_" + DBStmt.getDbBrandName() + dataSize;

        // 安装数据库所在磁盘
        diskNameOfDB = "sdd";

        // 创建tpcc测试目录
        testHomePath = toolsRootPath + "RDB_test/tpcc/";
        File testHomePathFile = new File(testHomePath);
        if(!testHomePathFile.exists())
            testHomePathFile.mkdirs();

        /// 测试工具benchmarksql工具所在目录
        toolHomePath = testHomePath + "benchmarksql-5.0/";
        toolPath = toolHomePath + "run/";
        File toolDir = new File(toolPath);
        if(!toolDir.exists()) {
            throw new Exception("未检测到benchmarksql工具");
        }

        // 测试数据保存目录
        dataSetPath = String.format("%s%s_%d/", testHomePath, "TPCC_Files/warehouses", dataSize);

        // 测试结果目录
        LocalDateTime localDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");
        String formatDateTime = formatter.format(localDateTime);
        // 结果目录格式类似于/home/wlx/cx/result20_oscar_2024-03-05_21-22-34/
        resultDirectory = String.format("%s%s/%dwarehouses_%s/", testHomePath, "results", dataSize, formatDateTime);
    }



    /**
     * 测试环境准备：软件部署、数据集导入
     */
    @Override
    public void testEnvPrepare() throws Exception {

        // 相关变量赋值
        initialization();

        // 生成配置文件
        createPropsFile();

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
            throw new InterruptedException("测试已经，如需再次测试，请新建测试实例");
        }
        // 正式执行测试
        String cmd = String.format("./auto_test_one.sh %s %s %s", propsFileName, resultDirectory, diskNameOfDB);
        execCommands(new File(toolPath), cmd);
        this.status = Status.FINISHED;
    }

    /**
     * 部署工具到testHomePath
     */
    private void prepareTools() throws Exception {
        // 检查是否存在工具包目录
        File toolDir = new File(toolHomePath);
        if (!toolDir.exists() || !toolDir.isDirectory()) {
            throw new Exception("未检测到工具包！");
        }

        String out = execCommandsWithReturn("ls " + toolHomePath);
        if (!out.contains("hasCompiled.dat")) {
            // 编译工具
            out = execCommandsWithReturn(toolDir, "ant");
            if (out.contains("BUILD SUCCESSFUL")) {
                System.out.println("编译成功!");
                // 如果编译成功,则添加标志文件
                execCommands(toolHomePath, "touch hasCompiled.dat");
            } else {
                throw new Exception("benchmarkSQL 编译失败");
            }
        }
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
        // 执行命令检查目录下是否存在文件
        String[] nameList = dataSetDir.list();
        HashSet<String> set = new HashSet<>(List.of(nameList));
        for (String name : DATA_SET_FILE_NAMES) {
            if (!set.contains(name)) {
                return false;
            }
        }

        // 检查 config.csv 文件中warehousesSize是否为要测试的dataSize
        String filePath = dataSetPath + "/config.csv";
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String firstLine = br.readLine(); // 读取第一行
            if (firstLine != null) {
                String[] parts = firstLine.split(","); // 使用逗号分割
                if (parts.length == 2 && parts[0].equals("warehouses")) {
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
        execCommands(new File(toolPath), "./runLoader.sh " + propsFileName);
        System.out.println("数据生成成功！保存在：" + dataSetPath);
    }

    /**
     * 根据模板配置文件创建配置文件
     */
    private void createPropsFile() throws Exception {

        // 删除旧的配置文件
        File oldFile = new File(toolPath + propsFileName);
        if (oldFile.exists()) {
            oldFile.delete();
        }

        // 获取配置文件中各参数的实际值
        HashMap<String, String> argsMap = getRealValueHashMap();
        // 模板配置文件中值替换并生成新配置文件
        updatePropsFile(argsMap, toolPath + TMP_PROPS_NAME, oldFile.getAbsolutePath());
    }

    private static void updatePropsFile(Map<String, String> map, String inputFilePath, String outputFilePath) {
        File inputFile = new File(inputFilePath);
        File outputFile = new File(outputFilePath);

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 检查是否存在 map 中的键
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    String key = entry.getKey();
                    if (line.startsWith(key + "=")) {
                        // 替换文件中的值
                        line = key + "=" + entry.getValue();
                        break;
                    }
                }
                // 写入更新后的行到新文件中
                writer.write(line);
                writer.newLine();
            }

            System.out.println("文件更新成功！");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private HashMap<String, String> getRealValueHashMap() {
        HashMap<String, String> argsMap = new HashMap<>();
        argsMap.put("db", DBStmt.getDbBrandName());
        argsMap.put("driver", DBStmt.getJdbcDriverClassName());
        argsMap.put("conn", DBStmt.getDbURL());
        argsMap.put("user", DBStmt.getUsername());
        argsMap.put("password", DBStmt.getPassword());
        argsMap.put("warehouses", String.valueOf(dataSize));
        argsMap.put("fileLocation", dataSetPath);
        argsMap.put("terminals", String.valueOf(terminals));
        argsMap.put("loadWorkers", String.valueOf(loadWorkers));
        argsMap.put("runMins", String.valueOf(runMins));
        argsMap.put("resultDirectory", String.valueOf(resultDirectory));
        return argsMap;
    }

    // 执行文件导入数据库的脚本
    private void importDataSetToDB() throws Exception {
        // 检查数据库中是否已有数据
        String sqlRes = DBStmt.executeSQL("select cfg_value from bmsql_config where cfg_name='warehouses';");
        if (!sqlRes.contains(String.valueOf(dataSize)))
            execCommands(new File(toolPath), "./import_data_TPCC.sh " + propsFileName);
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
        // 如果已经生成了结果，则直接返回
        if(this.testResult != null) {
            return this.testResult;
        }
        // 读取结果文件
        String res = execCommandsWithReturn("tail -n 6 " + resultDirectory + "result.txt");
        if (!res.isEmpty()) {
            // 定义正则表达式
            String regex = "Measured tpmC \\(NewOrders\\) = (\\d+\\.\\d+)";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(res);
            // 查找匹配项
            if (matcher.find()) {
                String tpmCValue = matcher.group(1);
                testResult = new TestResult();
                testResult.names = TestResult.TPCC_RES_NAMES;
                testResult.values = new String[] {tpmCValue};
                return testResult;
            } else {
                System.out.println("没有找到 Measured tpmC (NewOrders) 的值");
            }
        }
        return testResult;
    }

    @Override
    public String getResultDicName() {
        return new File(resultDirectory).getName();
    }

    @Override
    public void writeToFile(String resultPath) {
        if(status != Status.FINISHED) {
            System.out.println("还未生成结果文件");
            return ;
        }
        File file = new File(resultPath);
        if (!file.exists()) {
            file.mkdirs();
        }
        String cmd = String.format("cp -r %s* %s", resultDirectory, resultPath);
        System.out.println(cmd);
        execCommands(cmd);
    }

    @Override
    public TestAllResult readFromFile(String resultPath) {
        if(resultPath == null) {
            return null;
        }
        if(!resultPath.endsWith("/")) {
            resultPath += "/";
        }
        this.resultDirectory = resultPath;
        return new TestAllResult(getTestResults(), getTimeData());
    }

    @Override
    public List<List<Double>> readFromFile1(String resultPath) {
        return List.of();
    }

    public static void main(String[] args) {
//        DBConnection dbConnection = new DBConnection("/home/wlx/cx/benchmarksql-5.0/lib/oscar/oscarJDBC.jar",
//                "jdbc:oscar://10.181.8.146:2003/TPCC_20",
//                "SYSDBA",
//                "szoscar55");
//        dbConnection.connect();

//
//        TestArguments arguments = new TestArguments();
//        arguments.values = new ArrayList<>();
//        arguments.values.add("20");
//        arguments.values.add("128");
//        arguments.values.add("16");
//        arguments.values.add("1");
//
//        TPCCTester tester = new TPCCTester("tpcc", dbConnection, arguments);
//
//        try {
//            tester.testEnvPrepare();
//
//            tester.startTest();
//
//            List<List<Double>> tmpTimeData = tester.getTimeData();
//
//            TestResult results = tester.getTestResults();
//
//
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//        dbConnection.disconnect();
    }
}

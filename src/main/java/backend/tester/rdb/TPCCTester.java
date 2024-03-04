package backend.tester.rdb;

import backend.dataset.TestArguments;
import backend.dataset.TestResult;
import backend.tester.TestItem;
import frontend.connection.DBConnection;
import frontend.connection.SSHConnection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TPCCTester extends TestItem {

    public static String testHomePath;


    public static final String toolName = "benchmarksql-5.0";

    private static String toolPath;


    // 指定要检查的文件名列表
    public static final String[] DATA_SET_FILE_NAMES = {"config.csv", "cust-hist.csv", "customer.csv", "district.csv", "item.csv",
            "new-order.csv", "order.csv", "order-line.csv", "stock.csv", "warehouse.csv"};

    /**
     * 数据集路径
     */
    private String dataSetPath;

    /**
     * 模板配置文件名字
     */
    public static final String TMP_PROPS_NAME = "props";

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

    public TPCCTester(String testName, SSHConnection sshStmt) {
        this.testName = testName;
        this.sshStmt = sshStmt;
    }

    @Override
    public void generateTimeData() {

    }

    public TPCCTester(String testName, SSHConnection sshStmt, DBConnection DBStmt) {
        super(testName, sshStmt, DBStmt);
    }

    public TPCCTester(String testName, SSHConnection sshStmt, DBConnection DBStmt, TestArguments testArgs) {
        super(testName, sshStmt, DBStmt, testArgs);
    }

    /**
     * 测试环境准备：软件部署、数据集导入
     */
    @Override
    public void testEnvPrepare() {
        // 创建测试目录
        String testHomePath ="/home/" + sshStmt.getUserName() + "/RDB_test/tpcc";
        sshStmt.executeCommand("mkdir -p " + testHomePath);
        // 下载测试工具benchmark
        prepareTools();
        // 准备测试数据
        dataPrepare();
        // 导入测试数据到数据库
        importDataSetToDB();
    }

    /**
     * 部署工具到testHomePath
     */
    private void prepareTools() {
        toolPath = testHomePath + "/" + toolName;
        // 部署benchmarksql到testHomePath/toolName

        // 检查是否存在工具包目录
        sshStmt.executeCommand("test -d " + toolPath);
        String out = sshStmt.executeCommand("echo $?");
        if(out.equals("0")) {
            sshStmt.executeCommand("cd " + toolPath);
            out = sshStmt.executeCommand("ls");
            // 如果还未编译过工具
            if(!out.contains("hasCompiled.dat")) {
                // 编译工具
                sshStmt.executeCommand("ant");

                // 检查工具是否编译成功

                // 如果编译成功,则添加标志文件
                sshStmt.executeCommand("touch hasCompiled.dat");
            }

        }
    }

    public void dataPrepare() {
        if(this.testArgs == null) {
            throw new IllegalArgumentException("测试参数未配置");
        }
        dataSize =  Integer.parseInt(testArgs.values.get(0));
        terminals = Integer.parseInt(testArgs.values.get(1));
        loadWorkers = Integer.parseInt(testArgs.values.get(2));

        // 不存在数据集则创建数据集
        dataSetPath = testHomePath + "/TPCC_Files/warehouses_" + dataSize;
        if(!existDataSetFile(dataSetPath)) {
            createDataSet(dataSetPath);
        }
    }

    /**
     * 检查测试数据集是否存在
     */
    private boolean existDataSetFile(String dataSetPath) {
        // 检查目录是否存在
        // 执行命令检查目录下是否存在文件
        String execOut = sshStmt.executeCommand("ls " + dataSetPath);
        for(String name : DATA_SET_FILE_NAMES) {
            if(!execOut.contains(name)) {
                return false;
            }
        }

        // 检查 config.csv 文件中warehousesSize是否为要测试的dataSize
        String filePath = dataSetPath + "/config.csv";
        execOut = sshStmt.executeCommand("head -n 1 " + filePath);
        return execOut.contains("warehouses," + dataSize);
    }

    private void createDataSet(String fileDir) {
        // 创建数据集目录
        sshStmt.executeCommand("mkdir -p " + fileDir);
        // 创建配置文件
        createPropsFile();
        // 执行数据生成脚本
        sshStmt.executeCommand("cd " + toolPath + "/run");
        sshStmt.executeCommand("sh runDatabaseBuild.sh" + propsFileName);
    }

    /**
     * 根据模板配置文件创建配置文件
     */
    private void createPropsFile() {
        // 配置文件命名格式：“props_dbBrand_dataSize"
        propsFileName = "props_" + DBStmt.getDbBrandName() + dataSize;
        // 删除旧的配置文件
        sshStmt.executeCommand("cd " + toolPath + "/run");
        sshStmt.executeCommand("rm " + propsFileName);
        // 新建配置文件
        sshStmt.executeCommand("cp " + TMP_PROPS_NAME + " " + propsFileName);
        // 获取配置文件中各参数的实际值
        HashMap<String, String> argsMap = getRealValueHashMap();
        // 配置文件中值替换
        for(Map.Entry<String, String> entry : argsMap.entrySet()) {
            // 替换对应值的命令
            String cmd = String.format("sed -i \"s/^%s=.*/%s=%s/\" %s", entry.getKey(), entry.getKey(), entry.getValue(), propsFileName);
            sshStmt.executeCommand(cmd);
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
        return argsMap;
    }

    // 执行文件导入数据库的脚本
    private void importDataSetToDB() {
        sshStmt.executeCommand("cd " + toolPath + "/run");
        sshStmt.executeCommand("sh import_data_TPCC.sh " + propsFileName);
    }


    /**
     * 开始测试
     */
    @Override
    public void startTest() {

    }

    @Override
    public List<List<Double>> getTimeData() {
        return null;
    }


    @Override
    public TestResult getTestResults() {
        return null;
    }

    @Override
    public void writeToFile() {

    }

    @Override
    public void readFromFile() {

    }

    public static void main(String[] args) {
        SSHConnection connection = new SSHConnection("10.181.8.216", 22, "wlx", "Admin@wlx");
        DBConnection dbConnection = new DBConnection("/home/wlx/cx/benchmarksql-5.0/lib/oscar/oscarJDBC.jar",
                "conn=jdbc:oscar://10.181.8.146:2003/TPCC_20",
                "SYSDBA",
                "szoscar55");
//        if(connection.sshConnect()) {
//            connection.executeCommand("cd /home/wlx");
//            String out = connection.executeCommand("pwd");
//            System.out.println(out);
//        }
//        connection.sshDisconnect();
        if(dbConnection.connect()) {
            System.out.println("数据库成功连接");
        }
    }
}

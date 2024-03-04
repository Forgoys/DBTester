package backend.tester.rdb;

import backend.dataset.TestArguments;
import backend.dataset.TestResult;
import backend.tester.TestItem;
import com.jcraft.jsch.ChannelExec;
import frontend.connection.DBConnection;
import frontend.connection.SSHConnection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class TPCCTester extends TestItem {

    public static String testHomePath;


    public static final String toolName = "benchmarksql-5.0";

    private static String toolPath;

    /**
     * 模板配置文件名字
     */
    public static final String tmpPropsFileName = "props";

    private String propsFileName;
    /**
     * 数据规模
     */
    private int dataSize = 20;
    /**
     * 并发进程数
     */
    private int paraThreadNum = 8;
    /**
     * 加载进程数
     */
    private int loadThreadNum = 1;

    public TPCCTester(String testName, SSHConnection sshStmt) {
        this.testName = testName;
        this.sshStmt = sshStmt;
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

    @Override
    public void dataPrepare() {
        if(this.testArgs == null) {
            throw new IllegalArgumentException("测试参数未配置");
        }
        dataSize =  Integer.parseInt(testArgs.values.get(0));
        paraThreadNum = Integer.parseInt(testArgs.values.get(1));
        loadThreadNum = Integer.parseInt(testArgs.values.get(2));

        // 不存在数据集则创建数据集
        String dataSetPath = testHomePath + "/TPCC_Files/warehouses_" + dataSize;
        if(!existDataSetFile(dataSetPath)) {
            createDataSet(dataSetPath);
        }
    }

    /**
     * 检查测试数据集是否存在
     */
    private boolean existDataSetFile(String remoteDirectory) {
        // 指定要检查的文件名列表
        String[] filenames = {"config.csv", "cust-hist.csv", "customer.csv", "district.csv", "item.csv",
                "new-order.csv", "order.csv", "order-line.csv", "stock.csv", "warehouse.csv"};
        // 检查目录是否存在
        // 执行命令检查目录下是否存在文件
        String execOut = sshStmt.executeCommand("ls " + remoteDirectory);
        for(String name : filenames) {
            if(!execOut.contains(name)) {
                return false;
            }
        }

        // 检查 config.csv 文件中warehousesSize是否为要测试的dataSize
        String filePath = remoteDirectory + "/config.csv";
        execOut = sshStmt.executeCommand("head -n 1 " + filePath);
        return execOut.contains("warehouses," + dataSize);
    }

    private void createDataSet(String fileDir) {
        // 创建数据集目录
        sshStmt.executeCommand("mkdir -p " + fileDir);
        // 创建配置文件
        createPropsFile();
        //
        sshStmt.executeCommand("cd " + toolPath + "/run");
//        propsFileName = ""
        sshStmt.executeCommand("sh runDatabaseBuild.sh" + propsFileName);

    }

    /**
     * 根据模板配置文件创建配置文件
     */
    private void createPropsFile() {
        propsFileName = "props_" + dataSize;
        sshStmt.executeCommand("cd " + toolPath + "/run");
        sshStmt.executeCommand("rm " + propsFileName);
        // 根据模板文件创建新的配置文件

    }

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
    public void generateTimeData() {

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
        if(connection.sshConnect()) {
            connection.executeCommand("cd /home/wlx");
            String out = connection.executeCommand("pwd");
            System.out.println(out);
        }
        connection.sshDisconnect();
    }
}

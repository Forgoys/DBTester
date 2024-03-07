package backend.tester.timeSeriesDB;

import backend.dataset.TestAllResult;
import backend.dataset.TestArguments;
import backend.dataset.TestResult;
import backend.tester.TestItem;
import frontend.connection.DBConnection;
import frontend.connection.SSHConnection;
import frontend.controller.Util;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class PressTester extends TestItem{
    static String password = "";
    static String dbuser = "root";
    static String dbpassword = "taosdata";
    static String dbname = "devops";

    private static String testTime = "1分钟";
    private static int clients = 10;
    // 测试工具路径，存在二进制文件、config文件夹(内含两个toml文件)、monitor_write.sh、monitor_read.sh，以及用于存储数据集和结果的data和usage文件夹
    public static String testHomePath = "/home/wlx/disk/hugo/tsbstaos/build/tsdbcompare";
    // testHomePath为当前测试工具的路径

    // 指定要检查的文件名列表
    public static final String[] DATA_SET_FILE_NAMES = {"bulk_data_gen","bulk_load_tdengine","bulk_query_gen","query_benchmarker_tdengine",
        "taos_queries.csv","config/TDDashboardSchema.toml","config/TDengineSchema.toml","data","usage"};

    public PressTester(String testName, DBConnection DBStmt,TestArguments testArgs) {
        this.testName = testName;
        this.testArgs = testArgs;
        this.DBStmt = DBStmt;
        dbuser = DBStmt.getUsername();
        dbpassword = DBStmt.getPassword();
        dbname = DBStmt.getDBName();
        testTime = testArgs.values.get(0);
        if (testTime.contains("分钟")) {
            testTime = testTime.replace("分钟", "");
            testTime = String.valueOf(Integer.parseInt(testTime) * 60);
        }
        clients = Integer.parseInt(testArgs.values.get(1));
        testHomePath = new File(System.getProperty("user.dir")).getParent() + "/tool/TSDB";
    }
    @Override
    public void testEnvPrepare() {
        status = Status.UNPREPARED;
        // 检查测试工具是否存在
        if (!checkTestToolExist()) {
            throw new RuntimeException("测试工具不完整");
        }
        // 检查数据库是否开启，即taosd进程是否存在
        if (!checkDBStatus()) {
            throw new RuntimeException("数据库服务未开启");
        }
        // 检查数据库用户和密码是否正确
        if (!checkDBUserPassword()) {
            throw new RuntimeException("数据库用户或密码错误");
        }
        // 检查数据库名是否存在
        if (!checkDBExist()) {
            throw new RuntimeException("数据库名不存在(库名必须为devops!!!,请先完成一个同场景的写入测试以生成devops)");
        }
        status = Status.READY;
    }

    // 遍历DATA_SET_FILE_NAMES数组，检查每个文件和文件夹是否存在
    private boolean checkTestToolExist() {
        for (String fileName : DATA_SET_FILE_NAMES) {
            File file = new File(testHomePath, fileName);
            if (!file.exists()) {
                return false;
            }
        }
        return true;
    }
    // 检查数据库是否开启，即taosd进程是否存在
    private static boolean checkDBStatus() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("ps", "-ef");
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("taosd")) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    // 输入指令：taos -uroot -ptaosdata能进入taos命令行
    private static boolean checkDBUserPassword() {
        try {
            // 输入指令：taos -u"root" -p"taosdata"能进入taos命令行
            String[] command = {"/bin/bash", "-c", "taos -u" + dbuser + " -p" + dbpassword + " 2>&1"};
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
                if (line.contains("Authentication failure") || line.contains("Invalid user")) {
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
    // 检测数据库名是否存在
    private static boolean checkDBExist() {
        try {
            String[] command = {"/bin/bash", "-c", "taos -u" + dbuser + " -p" + dbpassword + " -s \"use " + dbname + ";\" 2>&1"};
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = inputReader.readLine()) != null) {
                if (line.contains("Database not exist")) {
                    return false;
                }
            }
            return true && checkDBUserPassword();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    @Override
    public void startTest() throws IOException, InterruptedException {
        if (status == Status.UNPREPARED) {
            throw new InterruptedException("测试尚未准备，请检查输入的参数");
        } else if (status == Status.RUNNING) {
            throw new InterruptedException("测试正在进行，请勿重复启动");
        } else if(status == Status.FINISHED) {
            throw new InterruptedException("测试已经结束，如需再次测试，请新建测试实例");
        }
        status = Status.RUNNING;
        // 执行过程通过一个taopress.py的程序执行来完成，这个程序的输入是dbuser,dbpassword,dbname,testTime,clients
    }
}

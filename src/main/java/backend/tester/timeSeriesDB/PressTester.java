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
    private static int success = 0;
    private static int failure = 0;
    // 测试工具路径，存在二进制文件、config文件夹(内含两个toml文件)、monitor_write.sh、monitor_read.sh，以及用于存储数据集和结果的data和usage文件夹
    public static String testHomePath = "/home/wlx/disk/hugo/tsbstaos/build/tsdbcompare";
    // testHomePath为当前测试工具的路径

    // 指定要检查的文件名列表
    public static final String[] DATA_SET_FILE_NAMES = {"bulk_data_gen","bulk_load_tdengine","bulk_query_gen","query_benchmarker_tdengine",
        "taos_queries.csv","taospress.py","config/TDDashboardSchema.toml","config/TDengineSchema.toml","data","usage"};

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
        testHomePath = new File(System.getProperty("user.dir")).getParent() + "/tools/TSDB";
        sourceBashrc();
    }
    public static void sourceBashrc() {
        try {
            String[] command = {"/bin/bash", "-c", "source ~/.bashrc"};
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public PressTester() {

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
        // 检查是否是python3.7及以后的版本，以及是否安装taos包
        try {
            Process process = Runtime.getRuntime().exec("python3 --version");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.contains("3.7")) {
                    throw new RuntimeException("请安装python3.7及以后的版本");
                }
            }
            process = Runtime.getRuntime().exec("pip3 show taospy");
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((line = reader.readLine()) != null) {
                if (line.contains("Name: taospy")) {
                    break;
                }
            }
            if (line == null) {
                throw new RuntimeException("请安装taospy包: pip3 install taospy");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        status = Status.READY;
    }

    // 遍历DATA_SET_FILE_NAMES数组，检查每个文件和文件夹是否存在
    private boolean checkTestToolExist() {
        for (String fileName : DATA_SET_FILE_NAMES) {
            System.out.println(fileName);
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
        // 例如：python taopress.py --user root --password taosdata --database devops --threadnum 5 --test_time 5
        // 这个程序的输出最后两行是：
        // Thread success count:  5051
        // Thread failure count:  0
        // 通过这两行可以得到成功次数和失败次数，赋值给success和failure
        // 执行taopress.py程序
        // 切换到testHomePath目录下

        String command = "python taospress.py --user " + dbuser + " --password " + dbpassword + 
                        " --database " + dbname + " --threadnum " + clients + " --test_time " + testTime;
        System.out.println(command);
        //Process process = Runtime.getRuntime().exec(command);
        ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", "-c", command);
        processBuilder.directory(new File(testHomePath)); // 设置工作目录
        Process process = processBuilder.start();

        try {
            // 读取程序的输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            Pattern patternSuccess = Pattern.compile("Thread success count:\\s+(\\d+)");
            Pattern patternFailure = Pattern.compile("Thread failure count:\\s+(\\d+)");
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                Matcher matcherSuccess = patternSuccess.matcher(line);
                Matcher matcherFailure = patternFailure.matcher(line);
                if (matcherSuccess.find()) {
                    success = Integer.parseInt(matcherSuccess.group(1));
                }
                if (matcherFailure.find()) {
                    failure = Integer.parseInt(matcherFailure.group(1));
                }
            }
            process.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // 输出success和failure
        System.out.println("Thread success count:  " + success);
        System.out.println("Thread failure count:  " + failure);
        status = Status.FINISHED;
    }

    @Override
    public TestResult getTestResults() {
        testResult = new TestResult();
        testResult.names = TestResult.INFLUXCOMP_PRESS_RES_NAMES;
        testResult.values = new String[]{String.valueOf(success), String.valueOf(failure), String.valueOf(Double.POSITIVE_INFINITY), "0"};
        return testResult;
    }
    @Override
    public List<List<Double>> getTimeData(){
        return null;
    }
    @Override
    public void writeToFile(String resultPath){
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        try {
            String directoryPath = resultPath;
            File directory = new File(directoryPath);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            FileWriter fileWriter = new FileWriter(directoryPath + "/"+ "press_" + testTime + "_w" + clients + "-"+dateFormat.format(new Date()) +".txt");
            fileWriter.write("Thread success count:  " + success + "\n");
            fileWriter.write("Thread failure count:  " + failure + "\n");
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public TestAllResult readFromFile(String resultPath) {
        TestAllResult result = new TestAllResult();
        
        result.timeDataResult = readFromFile1(resultPath);
        result.testResult = getTestResults1(resultPath);
        return result;
    }
    public TestResult getTestResults1(String resultPath) {
        testResult = new TestResult();
        testResult.names = TestResult.INFLUXCOMP_PRESS_RES_NAMES;
        try {
            File dir = new File(resultPath);
            File[] files = dir.listFiles();
            if (files != null) {
                File file = files[0];
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;
                int success = 0;
                int failure = 0;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("Thread success count:")) {
                        success = Integer.parseInt(line.split(":")[1].trim());
                    } else if (line.contains("Thread failure count:")) {
                        failure = Integer.parseInt(line.split(":")[1].trim());
                    }
                }
                reader.close();
                testResult.values = new String[]{String.valueOf(success), String.valueOf(failure)};
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return testResult;
    }
    @Override
    public List<List<Double>> readFromFile1(String resultPath) {
        return null;
    }
    @Override
    public String getResultDicName() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        return testName + "-" + testTime + "_" + clients + dateFormat.format(new Date());
    }

    public static void main(String[] args) {
        String homePath = "/home/wlx/disk/hugo/tsbstaos/build/tsdbcompare";
        String resultPath = "/home/wlx/disk/hugo/tsbstaos/build/tsdbcompare/result/Press-2024-03-08-11-46-13";
        TestArguments arguments = new TestArguments();
        arguments.values = new ArrayList<>();
        arguments.values.add("1分钟");
        arguments.values.add("10");
        DBConnection DBStmt = new DBConnection("devops","root","taosdata");
        //WriteTester(String testName, String homePath, String sudoPassord, DBConnection DBStmt,TestArguments testArgs)
        PressTester tester = new PressTester("Press", DBStmt, arguments);
        try {
            //tester.SetTag();
            tester.testEnvPrepare();
            tester.startTest();
            tester.writeToFile(resultPath);
            tester.getTestResults();//获取本测试结果
            //System.out.println(tester.getTestResults().values[0]);
            //System.out.println(tester.getTimeData());//获取本测试的监控数据
            System.out.println(tester.readFromFile(resultPath).testResult.values[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

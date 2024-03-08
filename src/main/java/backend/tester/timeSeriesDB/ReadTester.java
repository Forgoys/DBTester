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

public class ReadTester extends TestItem{
    static String password = "";
    static String dbuser = "root";
    static String dbpassword = "taosdata";
    static String dbname = "devops";

    // 测试工具路径，存在二进制文件、config文件夹(内含两个toml文件)、monitor_write.sh、monitor_read.sh，以及用于存储数据集和结果的data和usage文件夹
    public static String testHomePath = "/home/wlx/disk/hugo/tsbstaos/build/tsdbcompare";

    // 指定要检查的文件名列表
    public static final String[] DATA_SET_FILE_NAMES = {"bulk_data_gen","bulk_load_tdengine","bulk_query_gen","query_benchmarker_tdengine",
        "monitor_write.sh","monitor_read.sh","config/TDDashboardSchema.toml","config/TDengineSchema.toml","data","usage"};

    // 测试场景及并发写入的客户端数
    private static String scenario = "100台*30天";
    private static String query_type = "8-host-1-hr";
    private static int clients = 16;
    // 场景与数据集文件名的映射关系
    private static Map<String, String> scenarioToFile = new HashMap<>();
    static {
        scenarioToFile.put("10台*10天", "tdengine_s10_10d.gz");
        scenarioToFile.put("100台*30天", "tdengine_s100_30d.gz");
        scenarioToFile.put("4000台*3天", "tdengine_s4000_3d.gz");
        scenarioToFile.put("2万台*3小时", "tdengine_s20000_3h.gz");
        scenarioToFile.put("10万台*3小时", "tdengine_s100000_3h.gz");
        scenarioToFile.put("100万台*3分钟", "tdengine_s1000000_3min.gz");
    }
    private String tag;//形如s100_30d_w16_8-host-1hr_2021.06.01-12.00，txt后缀为结果，dat后缀为数据集文件，csv后缀为监控结果
    private String tagdat;
    private void SetTag () {
        String fileName = scenarioToFile.get(scenario);
        DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd-HH.mm");
        String time = dateFormat.format(new Date());
        tag = scenarioToFile.get(scenario).substring(fileName.indexOf("_s") + 1, fileName.indexOf(".gz")) + 
            "_w" + clients + "_" + query_type + "_" + time;
        tagdat = scenarioToFile.get(scenario).substring(fileName.indexOf("_s") + 1, fileName.indexOf(".gz")) + "_" + query_type;
    }
    public ReadTester(String testName, DBConnection DBStmt,TestArguments testArgs) {
        this.testName = testName;
        this.testArgs = testArgs;
        this.DBStmt = DBStmt;
        dbuser = DBStmt.getUsername();
        dbpassword = DBStmt.getPassword();
        dbname = DBStmt.getDBName();
        scenario = testArgs.values.get(0);
        query_type = testArgs.values.get(1);
        clients = Integer.parseInt(testArgs.values.get(2));
        password = testArgs.values.get(3);
        //testHomePath = new File(System.getProperty("user.dir")).getParent() + "/tools/TSDB";
        SetTag();
    }
    public static void checkDBStatusAndExist(String dataBaseName) {
        String title = "TDengine服务及数据库状态检测";
        String information;
    
        // 检查服务是否启动
        if (!checkDBStatus()) {
            information = "数据库服务未开启";
            Util.popUpInfo(information, title);
            return;
        }
    
        // 检查数据库是否存在
        if (!checkDBExist()) {
            information = "数据库不存在";
        } else {
            information = "数据库存在";
        }
    
        Util.popUpInfo(information, title);
    }
    /**
     * 测试环境检测，数据集和测试工具是否存在，数据库是否开启
     */
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
        // 检查sudo密码是否正确
        if (!Util.checkSudoPassword(password)) {
            System.out.println(password);
            throw new RuntimeException("sudo密码错误");
        }
        // 检查数据集是否存在
        if (!checkDataSetExist()) {
            dataGenerate();
            //throw new RuntimeException("数据集不存在,将自动创建数据集");
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
    // 检查场景对应的数据集是否存在
    private boolean checkDataSetExist() {
        //String fileName = scenarioToFile.get(scenario);
        String filePath = testHomePath + "/data/" + tagdat + ".dat";
        File file = new File(filePath);
        return file.exists();
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
    // 生成查询数据集
    /*
    ./bulk_query_gen -use-case=devops -seed=123 -scale-var=100 \
    -format="tdengine" -db=devops \
    -timestamp-start="2018-01-01T00:00:00Z" \
    -timestamp-end="2018-01-31T00:00:00Z" \
    -query-type=8-host-1-hr -queries=10000 \
    > data/query_tag.dat
     */
    private void dataGenerate() {
        try {
            // 根据场景更新参数
            String scaleVar = "100";
            String timestampEnd = "2018-01-31T00:00:00Z";
            switch (scenario) {
                case "10台*10天":
                    scaleVar = "10";
                    timestampEnd = "2018-01-11T00:00:00Z";
                    break;
                case "100台*30天":
                    scaleVar = "100";
                    timestampEnd = "2018-01-31T00:00:00Z";
                    break;
                case "4000台*3天":
                    scaleVar = "4000";
                    timestampEnd = "2018-01-04T00:00:00Z";
                    break;
                case "2万台*3小时":
                    scaleVar = "20000";
                    timestampEnd = "2018-01-01T03:00:00Z";
                    break;
                case "10万台*3小时":
                    scaleVar = "100000";
                    timestampEnd = "2018-01-01T03:00:00Z";
                    break;
                case "100万台*3分钟":
                    scaleVar = "1000000";
                    timestampEnd = "2018-01-01T00:03:00Z";
                    break;
                default:
                    throw new IllegalArgumentException("未知场景: " + scenario);
            }
            // 构建命令
            String command = testHomePath + "/bulk_query_gen -use-case=devops -seed=123 -scale-var=" + scaleVar +
                    " -format=\"tdengine\" -db=devops " +
                    "-timestamp-start=\"2018-01-01T00:00:00Z\" " +
                    "-timestamp-end=\"" + timestampEnd + "\" " +
                    "-query-type=" + query_type + " -queries=100000> " + testHomePath + "/data/"+ tagdat + ".dat";
            // 执行命令
            System.out.println(command);
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", "-c", command);
            Process process = processBuilder.start();
            // 等待命令执行完成
            process.waitFor();
            // 检查命令执行结果
            if (process.exitValue() != 0) {
                throw new RuntimeException("数据集生成失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("数据集生成失败");
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
        try {            
            
            // 获取对应场景的文件名
            String fileName = tagdat + ".dat";
            // 构建数据查询命令
            String command = "./query_benchmarker_tdengine -use-case=devops -batch-size=2000 " +
            "-urls=10.181.8.146 -print-responses=true " +
            "-workers=" + clients + " -threads=" + clients +
            " -print-interval=10000000 -http-client-type=cgo " +
            "-file=data/" + fileName;
            
            File workingDirectory = new File(testHomePath);
            
            // 执行命令
            // 创建 ProcessBuilder 对象并设置工作目录
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.directory(workingDirectory);
            // 将命令拆分为字符串数组，并设置进程的命令
            String[] commandArray = {"/bin/bash", "-c", command};
            processBuilder.command(commandArray);

            processBuilder.redirectErrorStream(true);// 将标准错误流重定向到标准输出流
            
            Process process = processBuilder.start();
            writeToFile1();//开启脚本监视资源使用，测试结束后自动停止，生成名为taosd_usage_query_tag.csv的文件
            
            String filepath = testHomePath + "/usage/" + "read_" + tag + ".txt";//存放结果
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            PrintWriter writer = new PrintWriter(new File(filepath), "UTF-8");
            while ((line = reader.readLine()) != null) {
                System.out.println(line); // 输出标准输出流内容
                writer.println(line);
            }
            writer.close();

            // 等待命令执行完成
            process.waitFor();
            
            // 检查命令执行结果
            if (process.exitValue() != 0) {
                throw new RuntimeException("数据查询失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("数据查询失败");
        }
        status = Status.FINISHED;
    }
    @Override
    public String getResultDicName() {
        return "Read_" + tag;
    }
    @Override
    // 把txt倒数第二行提取出来进行解析，要求解析出
    // TDengine max cpu, rand    1 hosts, rand 1h0m0s by 1m : min:    14.48ms (  69.05/sec), mean:   100.41ms (   9.96/sec), max:  257.53ms (  3.88/sec), count:    10000, sum: 1004.1sec 
    // 提出14.48，100.41，257.53这三个数值
    // 将这个值存入testResult.values中
    // 修改正则表达式使之能够提取出这三个数值
    // 修改result的获取方法
    public TestResult getTestResults() {
    // 获取对应场景的文件名
        String filepath = testHomePath + "/usage/" + "read_" + tag + ".txt";
        String result = "";
        String penultimateLine = "";
        testResult = new TestResult();
        testResult.names = TestResult.INFLUXCOMP_READ_RES_NAMES;
        try {
                File file = new File(filepath);
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (result != null) {
                        penultimateLine = result;
                    }
                    result = line;
            }
            reader.close();

            // 使用正则表达式提取需要的值
            Pattern pattern = Pattern.compile("min:\\s+(\\d+\\.\\d+)ms.*mean:\\s+(\\d+\\.\\d+)ms.*max:\\s+(\\d+\\.\\d+)ms");
            Matcher matcher = pattern.matcher(penultimateLine);
            if (matcher.find()) {
                testResult.values = new String[] {
                    matcher.group(1), // 14.48
                    matcher.group(2), // 100.41
                    matcher.group(3)  // 257.53
                };
            }
            // 显示提取的值
            System.out.println("min: " + testResult.values[0]);
            System.out.println("mean: " + testResult.values[1]);
            System.out.println("max: " + testResult.values[2]);
        } catch (Exception e) {
            e.printStackTrace();
        }   
        return testResult;
    }
    // 返回指定文件中的测试结果
    public TestResult getTestResults1(String resultPath) {
        String result = "";
        String penultimateLine = "";
        testResult = new TestResult();
        testResult.names = TestResult.INFLUXCOMP_READ_RES_NAMES;
        File directory = new File(resultPath);
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().matches(".*\\.txt$")) {
                    String filepath = file.getAbsolutePath();
                    try {
                        File fileName = new File(filepath);
                        BufferedReader reader = new BufferedReader(new FileReader(fileName));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (result != null) {
                                penultimateLine = result;
                            }
                            result = line;
                    }
                        reader.close();
            
                        // 使用正则表达式提取需要的值
                        Pattern pattern = Pattern.compile("min:\\s+(\\d+\\.\\d+)ms.*mean:\\s+(\\d+\\.\\d+)ms.*max:\\s+(\\d+\\.\\d+)ms");
                        Matcher matcher = pattern.matcher(penultimateLine);
                        if (matcher.find()) {
                            testResult.values = new String[] {
                                matcher.group(1), // 14.48
                                matcher.group(2), // 100.41
                                matcher.group(3)  // 257.53
                            };
                        }
                        // 显示提取的值
                        System.out.println("min: " + testResult.values[0]);
                        System.out.println("mean: " + testResult.values[1]);
                        System.out.println("max: " + testResult.values[2]);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break; //只读第一个
                }

            }
        }   
        return testResult;
    }
    @Override
    public List<List<Double>> getTimeData() {
        // 首先修改csv文件的读写权限
        String fileName = testHomePath + "/usage/taosd_usage_read_" + tag + ".csv";
        try{
            String command = "echo " + password + " | sudo -S chown $USER " + fileName;
            // 执行命令
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", "-c", command);
            processBuilder.directory(new File(testHomePath));
            Process process = processBuilder.start();
            process.waitFor();
            // 检查命令执行结果
            if (process.exitValue() != 0) {
                throw new RuntimeException("修改CSV文件权限失败, sudo权限不足");
            }
        }catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("修改CSV文件权限失败, sudo权限不足");
        }
        List<List<Double>> result = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            String line;
            // 跳过表头
            reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",");
                List<Double> row = new ArrayList<>();
                // 从第二列开始读取数据
                for (int i = 1; i < values.length; i++) {
                    row.add(Double.parseDouble(values[i]));
                }
                result.add(row);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 将result进行转置
        List<List<Double>> transposedResult = new ArrayList<>();
        for (int i = 0; i < result.get(0).size(); i++) {
            List<Double> newRow = new ArrayList<>();
            for (List<Double> row : result) {
                newRow.add(row.get(i));
            }
            transposedResult.add(newRow);
        }
        this.timeDataList = transposedResult;
        return transposedResult;
    }
    // 调用testHomePath路径中的monitor_read.sh脚本，将结果保存到testHomePath/usage文件夹中
    // 执行脚本要用sudo命令，密码可由SSHConnection类中的getPassword()获取
    // 文件名为taosd_usage_read_tag格式
    public void writeToFile1() {
        try {

            String command = "echo " + password + " | sudo -S ./monitor_read.sh " + tag;
            // 执行命令
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", "-c", command);
            processBuilder.directory(new File(testHomePath));
            Process process = processBuilder.start();
            /* 无需等待，脚本会自动停止
            process.waitFor();
            // 检查命令执行结果
            if (process.exitValue() != 0) {
                throw new RuntimeException("执行monitor_read.sh失败");
            }
            */
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("执行monitor_read.sh失败");
        }
    }
    // 复制taosd_usage_read_tag.csv和read_tag.txt文件到resultPath中
    @Override
    public void writeToFile(String resultPath) {
        // 首先修改csv文件的读写权限
        String fileName = testHomePath + "/usage/taosd_usage_read_" + tag + ".csv";
        try{
            String command = "echo " + password + " | sudo -S chown $USER " + fileName;
            System.out.println(command);
            // 执行命令
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", "-c", command);
            processBuilder.directory(new File(testHomePath));
            Process process = processBuilder.start();
            process.waitFor();
            // 检查命令执行结果
            if (process.exitValue() != 0) {
                throw new RuntimeException("修改CSV文件权限失败, sudo权限不足");
            }
        }catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("修改CSV文件权限失败, sudo权限不足");
        }
        // 将taosd_usage_read_tag.csv和read_tag.txt文件中的数据复制到resultPath中
        try {
            // 在resultPath新建getResultDicName()文件夹
            String command0 = "mkdir " + resultPath + "/" + getResultDicName();
            // 执行命令
            ProcessBuilder processBuilder0 = new ProcessBuilder("/bin/bash", "-c", command0);
            processBuilder0.directory(new File(testHomePath));
            Process process0 = processBuilder0.start();
            process0.waitFor();
            // 检查命令执行结果
            if (process0.exitValue() != 0) {
                throw new RuntimeException("新建文件夹失败");
            }
            String resultpath = resultPath + "/" + getResultDicName();
            String command1 = "cp " + testHomePath + "/usage/taosd_usage_read_" + tag + ".csv " + resultpath;
            String command2 = "cp " + testHomePath + "/usage/read_" + tag + ".txt " + resultpath;
        
            // 执行命令
            ProcessBuilder processBuilder1 = new ProcessBuilder("/bin/bash", "-c", command1);
            processBuilder1.directory(new File(testHomePath));
            Process process1 = processBuilder1.start();
            process1.waitFor();
        
            ProcessBuilder processBuilder2 = new ProcessBuilder("/bin/bash", "-c", command2);
            processBuilder2.directory(new File(testHomePath));
            Process process2 = processBuilder2.start();
            process2.waitFor();
        
            // 检查命令执行结果
            if (process1.exitValue() != 0 || process2.exitValue() != 0) {
                throw new RuntimeException("复制CSV文件或TXT文件到指定路径失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("复制CSV文件或TXT文件到指定路径失败");
        } 
    }
    @Override
    public TestAllResult readFromFile(String resultPath) {
        TestAllResult result = new TestAllResult();
        
        result.timeDataResult = readFromFile1(resultPath);
        result.testResult = getTestResults1(resultPath);
        return result;
    }
    // 实际实现了返回结果的一半功能，即监控数据
    @Override
    public List<List<Double>> readFromFile1(String resultPath) {

        List<List<Double>> result = new ArrayList<>();
        File directory = new File(resultPath);
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().matches(".*\\.csv$")) {
                    String fileName = file.getAbsolutePath();
                    System.out.println(fileName);
                    try {
                        BufferedReader reader = new BufferedReader(new FileReader(fileName));
                        String line;
                        // 跳过表头
                        reader.readLine();
                        while ((line = reader.readLine()) != null) {
                            String[] values = line.split(",");
                            List<Double> row = new ArrayList<>();
                            // 从第二列开始读取数据
                            for (int i = 1; i < values.length; i++) {
                                row.add(Double.parseDouble(values[i]));
                            }
                            result.add(row);
                        }
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break; //只读第一个符合的
                }
            }
        }
        // 将result进行转置
        List<List<Double>> transposedResult = new ArrayList<>();
        for (int i = 0; i < result.get(0).size(); i++) {
            List<Double> newRow = new ArrayList<>();
            for (List<Double> row : result) {
                newRow.add(row.get(i));
            }
            transposedResult.add(newRow);
        }
        this.timeDataList = transposedResult;
        return transposedResult;
    }
    public static void main(String[] args) {
        String homePath = "/home/wlx/disk/hugo/tsbstaos/build/tsdbcompare";
        String resultPath = homePath + "/result";
        TestArguments arguments = new TestArguments();
        arguments.values = new ArrayList<>();
        arguments.values.add("100台*30天");
        arguments.values.add("8-host-1-hr");
        arguments.values.add("16");
        arguments.values.add("Admin@wlx");
        DBConnection DBStmt = new DBConnection("devops","root","taosdata");
        //WriteTester(String testName, String homePath, String sudoPassord, DBConnection DBStmt,TestArguments testArgs)
        ReadTester tester = new ReadTester("Read", DBStmt, arguments);
        try {
            //tester.SetTag();
            tester.testEnvPrepare();
            tester.startTest();
            tester.writeToFile(resultPath);
            tester.getTestResults();//获取本测试结果
            //tester.getTestResults1(resultPath);
            //System.out.println(tester.getTestResults1(resultPath).values[0]);
            System.out.println(tester.getTimeData());//获取本测试的监控数据
            System.out.println(tester.readFromFile1(resultPath));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

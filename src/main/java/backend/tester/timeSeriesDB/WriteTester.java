package backend.tester.timeSeriesDB;

import backend.dataset.TestAllResult;
import backend.dataset.TestArguments;
import backend.dataset.TestResult;
import backend.tester.TestItem;
import frontend.connection.SSHConnection;

import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

// 此软件在服务器上运行，无需使用SSH连接
public class WriteTester extends TestItem {
    
    SSHConnection sshStmt;

    // 测试工具路径，存在二进制文件、config文件夹(内含两个toml文件)、monitor_write.sh、monitor_read.sh，以及用于存储数据集和结果的data和usage文件夹
    public static String testHomePath = "/home/wlx/disk/hugo/tsbstaos/build/tsdbcompare";

    // 指定要检查的文件名列表
    public static final String[] DATA_SET_FILE_NAMES = {"bulk_data_gen","bulk_load_tdengine","bulk_query_gen","query_benchmarker_tdengine",
        "monitor_write.sh","config/TDDashboardSchema.toml","config/TDengineSchema.toml","data","usage"};

    // 测试场景及并发写入的客户端数
    private static String scenario = "100台*30天";
    private static int clients = 16;
    // 场景与数据集文件名的映射关系
    private static Map<String, String> scenarioToFile = new HashMap<>();
    static {
        scenarioToFile.put("10台*1天", "tdengine_s10_1d.gz");
        scenarioToFile.put("100台*30天", "tdengine_s100_30d.gz");
        scenarioToFile.put("4000台*3天", "tdengine_s4000_3d.gz");
        scenarioToFile.put("2万台*3小时", "tdengine_s20000_3h.gz");
        scenarioToFile.put("10万台*3小时", "tdengine_s100000_3h.gz");
        scenarioToFile.put("100万台*3分钟", "tdengine_s1000000_3min.gz");
    }
    private String tag;//形如s100_30d_w16_2021.06.01-12.00
    private String SetTag () {
        String fileName = scenarioToFile.get(scenario);
        DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd-HH.mm");
        String time = dateFormat.format(new Date());
        tag = scenarioToFile.get(scenario).substring(fileName.indexOf("_s") + 1, fileName.indexOf(".gz")) + 
            "_w" + clients + "_" + time;
        return tag;
    }
    public WriteTester(String testName, String homePath, SSHConnection sshStmt, TestArguments testArgs) {
        this.testName = testName;
        this.sshStmt = sshStmt;
        this.testArgs = testArgs;
        testHomePath = homePath;
        scenario = testArgs.values.get(0);
        clients = Integer.parseInt(testArgs.values.get(1));
    }

    /**
     * 测试环境检测，数据集和测试工具是否存在，数据库是否开启
     */
    @Override
    public void testEnvPrepare() {
        // 检查测试工具是否存在
        if (!checkTestToolExist()) {
            throw new RuntimeException("测试工具不完整");
        }
        // 检查数据集是否存在
        if (!checkDataSetExist()) {
            dataGenerate(scenario);
            throw new RuntimeException("数据集不存在,将自动创建数据集");
        }
        // 检查数据库是否开启，即taosd进程是否存在
        if (!checkDBStatus()) {
            throw new RuntimeException("数据库未开启");
        }
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
        String fileName = scenarioToFile.get(scenario);
        String filePath = testHomePath + "/data/" + fileName;
        File file = new File(filePath);
        return file.exists();
    }
    // 检查数据库是否开启，即taosd进程是否存在
    private boolean checkDBStatus() {
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
    // 数据集生成，调用使用bulk_data_gen生成数据集(脚本位于testHomePath路径中)，生成的数据集文件存放在testHomePath/data文件夹下
    // 如果scenario为100台*30天，则脚本的命令为：
    /*
    ./bulk_data_gen -use-case="devops" -seed=123 -scale-var=100 \
    -format="tdengine" -tdschema-file config/TDengineSchema.toml \
    -timestamp-start="2018-01-01T00:00:00Z" \
    -timestamp-end="2018-01-31T00:00:00Z" \
    -sampling-interval="10s" -workers=16 \
    | gzip > data/tdengine_s100_30d.gz
    */
    // 如果是其他场景，只需要替换-scale-var和-timestamp-end,以及生成的数据集文件名
    private static void dataGenerate(String scenario) {
        try {
            // 获取对应场景的文件名
            String fileName = scenarioToFile.get(scenario);
            // 根据场景更新参数
            String scaleVar = "100";
            String timestampEnd = "2018-01-31T00:00:00Z";
            switch (scenario) {
                case "10台*1天":
                    scaleVar = "10";
                    timestampEnd = "2018-01-02T00:00:00Z";
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
            String command = testHomePath + "/bulk_data_gen -use-case=\"devops\" -seed=123 -scale-var=" + scaleVar +
                    " -format=\"tdengine\" -tdschema-file " + testHomePath + "/config/TDengineSchema.toml " +
                    "-timestamp-start=\"2018-01-01T00:00:00Z\" " +
                    "-timestamp-end=\"" + timestampEnd + "\" " +
                    "-sampling-interval=\"10s\" -workers=16 | gzip > " + testHomePath + "/data/" + fileName;
            // 执行命令
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
    /**
     * 数据写入命令如下：
     *  gunzip < data/tdengine_s100_30d.gz | \
        ./bulk_load_tdengine -use-case=devops -batch-size=2000 \
        -url=localhost -slavesource=false \
        -do-load=true -fileout=false -http-api=false \
        -workers=16
     * 其中数据集名要和scenario对应，-workers和clien对应
     */
    @Override
    public void startTest() {
        try {
            
            // 重新生成一个数据库 devops
            dropDevopsDatabase();
            createDevopsDatabase();
            
            // 获取对应场景的文件名
            String fileName = scenarioToFile.get(scenario);
            
            // 构建数据写入命令
            String command = "gunzip < data/" + fileName + " | " +
                    "./bulk_load_tdengine -use-case=devops -batch-size=2000 " +
                    "-url=10.181.8.146 -slavesource=true " +
                    "-do-load=true -fileout=false -http-api=false " +
                    "-workers=" + clients;
            
            File workingDirectory = new File("/home/wlx/disk/hugo/tsbstaos/build/tsdbcompare");
            
            // 执行命令
            // 创建 ProcessBuilder 对象并设置工作目录
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.directory(workingDirectory);
            // 将命令拆分为字符串数组，并设置进程的命令
            String[] commandArray = {"/bin/bash", "-c", command};
            processBuilder.command(commandArray);

            processBuilder.redirectErrorStream(true);// 将标准错误流重定向到标准输出流
            
            Process process = processBuilder.start();
            writeToFile(testHomePath);//实际这个路径无用。开启脚本监视资源使用，测试结束后自动停止，生成名为taosd_usage_write_后缀.csv的文件
            
            // 读取命令输出，并保存输出到usage文件夹中，
            /*
            DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd-HH.mm");
            String time = dateFormat.format(new Date());
            String filename = scenarioToFile.get(scenario).substring(fileName.indexOf("_s") + 1, fileName.indexOf(".gz")) + 
                "_w" + clients + "_" + time + ".txt";
            */
            String filepath = testHomePath + "/usage/" + tag + ".txt";
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
                throw new RuntimeException("数据写入失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("数据写入失败");
        }
    }
    @Override
    public String getResultDicName() {
        return null;
    }
    @Override
    public TestResult getTestResults() {
        // 获取对应场景的文件名
        /*
        String fileName = scenarioToFile.get(scenario);
        DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd-HH.mm");
        String time = dateFormat.format(new Date());
        */
        String filepath = testHomePath + "/usage/" + tag + ".txt";
        String result = "";
        testResult = new TestResult();
        testResult.names = TestResult.INFLUXCOMP_WRTIE_RES_NAMES;
        try {
            File file = new File(filepath);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                result = line;
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        testResult.values = new String[] {result};
        return testResult;
    }
    @Override
    public List<List<Double>> getTimeData() {
        /*
        String fileName = scenarioToFile.get(scenario);
        DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd-HH.mm");
        String time = dateFormat.format(new Date());
        String filename = scenarioToFile.get(scenario).substring(fileName.indexOf("_s") + 1, fileName.indexOf(".gz")) + 
            "_w" + clients + "_" + time + ".txt"; 
        */
        String usageFilePath = testHomePath + "/usage/" + "taosd_usage_write_" + tag +".csv";
        return readFromFile1(usageFilePath);
    }
    @Override
    // 调用testHomePath路径中的monitor.sh脚本，将结果保存到testHomePath/usage文件夹中
    // 执行脚本要用sudo命令，密码可由SSHConnection类中的getPassword()获取
    // 文件名为taosd_usage_write_s100_30d_w16_2021.06.01-12.00格式
    public void writeToFile(String resultPath) {
        try {
            // 获取对应场景的文件名
            String fileName = scenarioToFile.get(scenario);
            DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd-HH.mm");
            String time = dateFormat.format(new Date());
            String filename = scenarioToFile.get(scenario).substring(fileName.indexOf("_s") + 1, fileName.indexOf(".gz")) + 
                "_w" + clients + "_" + time;
            // 构建命令
            String password = sshStmt.getPassword();

            String command = "echo " + password + " | sudo -S ./monitor_write.sh " + filename;
            // 执行命令
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", "-c", command);
            processBuilder.directory(new File(testHomePath));
            Process process = processBuilder.start();
            /* 无需等待，脚本会自动停止
            process.waitFor();
            // 检查命令执行结果
            if (process.exitValue() != 0) {
                throw new RuntimeException("执行monitor_write.sh失败");
            }
            */
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("执行monitor_write.sh失败");
        }
    }
    @Override
    public TestAllResult readFromFile(String resultPath) {
        return null;
    }
    @Override
    public List<List<Double>> readFromFile1(String resultPath) {

        List<List<Double>> result = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(resultPath));
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
        return result;
    }

    // 删除数据库 devops
    private void dropDevopsDatabase() {
        try {
            // 构建命令
            String command = "taos -s 'drop database if exists devops'";
            // 执行命令
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", "-c", command);
            Process process = processBuilder.start();
            // 等待命令执行完成
            process.waitFor();
            if (process.exitValue() != 0) {
                throw new RuntimeException("删除数据库 devops 失败");
            }
        }
            catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("删除数据库 devops 失败");
        }
    }
    // 创建数据库 devops
    private void createDevopsDatabase() {
        try {
            // 构建命令
            String command = "taos -s 'create database if not exists devops vgroups 2 buffer 8192 stt_trigger 8;'";
            // 执行命令
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", "-c", command);
            Process process = processBuilder.start();
            // 等待命令执行完成
            process.waitFor();
            // 检查命令执行结果
            if (process.exitValue() != 0) {
                throw new RuntimeException("创建数据库 devops 失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("创建数据库 devops 失败");
        }
    }
    public static void main(String[] args) {
        SSHConnection sshStmt = new SSHConnection("10.181.8.146", 22, "wlx", "Admin@wlx");
        String homePath = "/home/wlx/disk/hugo/tsbstaos/build/tsdbcompare";
        TestArguments arguments = new TestArguments();
        arguments.values = new ArrayList<>();
        arguments.values.add("100台*30天");
        arguments.values.add("16");

        WriteTester tester = new WriteTester("Write", homePath, sshStmt, arguments);
        try {
            tester.SetTag();
            tester.testEnvPrepare();
            tester.startTest();
            tester.writeToFile(homePath);
            System.out.println(tester.getTestResults().values[0]);
            System.out.println(tester.getTimeData());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

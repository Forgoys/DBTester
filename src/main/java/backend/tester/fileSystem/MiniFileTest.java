package backend.tester.fileSystem;

import backend.dataset.TestAllResult;
import backend.dataset.TestResult;
import backend.tester.TestItem;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MiniFileTest extends TestItem {

    // 参数 root目录 测试的小文件名 测试小文件路径 脚本名 脚本路径
    private String directory;
    private String miniFileName;
    private String miniFileDirectory;
    private String miniFileReadScriptName;
    private String miniFileReadScriptPath;
    private String miniFileWriteScriptName;
    private String miniFileWriteScriptPath;
    private String miniFileWriteNum; // 文件写入数量


    // sudo权限
    String localSudoPassword;

    // 资源检测脚本名称
    private String monitorScriptName;
    private String monitorResultCSV;

    // 指令运行结果
    TestResult fioMiniFileTestResult = new TestResult();
    // 脚本执行后结果保存在txt文件
//    private String fioMiniFileTestResultPath;
//    private String fioMiniFileTestResultTxt;
    private String miniFileTestResultTxt; // 精简的结果

    public MiniFileTest() {
    }

    public MiniFileTest(String directory, String localSudoPassword) {
        this.directory = directory + "/miniFileTest";
        this.localSudoPassword = localSudoPassword;

        miniFileName = "cifar-10-batches-bin";
        miniFileDirectory = this.directory + "/" + miniFileName;
        miniFileReadScriptName = "miniFileReadTest.sh";
        miniFileReadScriptPath = this.directory + "/" + miniFileReadScriptName;
        miniFileWriteScriptName = "miniFileWriteTest.sh";
        miniFileWriteScriptPath = this.directory + "/" + miniFileWriteScriptName;
//        fioMiniFileTestResultTxt = "fioMiniFileTestResult.txt";
//        fioMiniFileTestResultPath = this.directory + "/" + fioMiniFileTestResultTxt;

        miniFileTestResultTxt = "fioMiniFileTestResult.txt";

        miniFileWriteNum = "100";

        monitorScriptName = "monitor.sh";
        monitorResultCSV = "fioMiniFileTestMonitorResult.csv";
    }

    public int executeCommand(String command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("bash", "-c", command);
        Process process = processBuilder.start();
        // 等待进程执行完毕
        int exitCode = process.waitFor();
        return exitCode;
    }

    // 准备测试数据 准备测试工具 准备测试脚本 配置文件.ini
    @Override
    public void testEnvPrepare() throws IOException, InterruptedException {
        String currentDirectory = System.getProperty("user.dir");
        System.out.println("Current directory: " + currentDirectory);
        String localReadScriptPath = currentDirectory + "/src/main/resources/scripts/" + miniFileReadScriptName;
        String localWriteScriptPath = currentDirectory + "/src/main/resources/scripts/" + miniFileWriteScriptName;

        int exitCode = 0;
        // 创建miniFileTest小文件测试文件夹
        String command = "mkdir -p " + directory;
        exitCode = executeCommand(command);
        System.out.println("创建小文件测试文件夹:" + directory + " Exit code:" + exitCode);

        // 复制miniFileTest小文件测试脚本
        command = "cp " + localReadScriptPath + " " + directory;
        exitCode = executeCommand(command);
        System.out.println("miniFileTest小文件测试脚本:" + miniFileReadScriptName + " Exit code:" + exitCode);

        // 复制miniFileTest小文件测试脚本
        command = "cp " + localWriteScriptPath + " " + directory;
        exitCode = executeCommand(command);
        System.out.println("miniFileTest小文件测试脚本:" + miniFileWriteScriptName + " Exit code:" + exitCode);

        // 给脚本添加执行权限
        command = "chmod +x " + directory + "/" + miniFileReadScriptName;
        exitCode = executeCommand(command);
        System.out.println("给脚本添加执行权限:" + miniFileReadScriptName + " Exit code:" + exitCode);

        // 给脚本添加执行权限
        command = "chmod +x " + directory + "/" + miniFileWriteScriptName;
        exitCode = executeCommand(command);
        System.out.println("给脚本添加执行权限:" + miniFileWriteScriptName + " Exit code:" + exitCode);

        // 复制cifar数据集

        String localMiniFilePath = currentDirectory + "/src/main/resources/miniFile/" + miniFileName;
        command = "cp -r " + localMiniFilePath + " " + directory;
        exitCode = executeCommand(command);
        System.out.println("复制cifar数据集:" + localMiniFilePath + " Exit code:" + exitCode);

        // 传入检测系统资源的脚本
        String localMonitorScriptPath = currentDirectory + "/src/main/resources/scripts/" + monitorScriptName;
        command = "cp " + localMonitorScriptPath + " " + directory;
        exitCode = executeCommand(command);
        System.out.println("系统资源监测脚本:" + localMonitorScriptPath + " Exit code:" + exitCode);

        // 给脚本添加执行权限
        command = "chmod +x " + directory + "/" + monitorScriptName;
        exitCode = executeCommand(command);
        System.out.println("给脚本添加执行权限:" + monitorScriptName + " Exit code:" + exitCode);
    }

    public void miniFileReadWriteTest(String fioCommand, List<String> results) throws IOException, InterruptedException {
        String password = localSudoPassword;
        fioCommand = "echo " + password + " | sudo -S " + fioCommand;
        System.out.println(fioCommand);

        // 创建一个 ProcessBuilder 对象
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("bash", "-c", fioCommand);

        // 启动进程
        Process process = processBuilder.start();

        // 获取进程的输出流
        InputStream inputStream = process.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        // 读取进程的输出
        String line;
        while ((line = reader.readLine()) != null) {
            results.add(line);
        }

        // 输出结果
        for (String s : results) {
            System.out.println(s);
        }

        // 等待进程执行完毕
        int exitCode = process.waitFor();
        System.out.println("Exit code: " + exitCode);
    }

    @Override
    public void startTest() throws IOException, InterruptedException {

        System.out.println("小文件测试环境准备");
        testEnvPrepare();
        System.out.println("小文件测试环境准备完成");

        // 检测系统资源 CPU利用率 内存使用率
        String command = directory + "/" + monitorScriptName + " " + directory + "/" + monitorResultCSV;
        ProcessBuilder monitorProcessBuilder = new ProcessBuilder();
        monitorProcessBuilder.command("bash", "-c", command);
        Process monitorProcess = monitorProcessBuilder.start();

        System.out.println("小文件测试开始");

        List<String> results = new ArrayList<>();

        // 执行读取测试脚本指令 参数：cifar路径
        System.out.println("小文件读取测试开始");
        String miniFileReadCommand = miniFileReadScriptPath + " " + miniFileDirectory;
        miniFileReadWriteTest(miniFileReadCommand, results);
        System.out.println("小文件读取测试完成");

        // 执行读取测试脚本指令 参数：cifar路径
        System.out.println("小文件写入测试开始");
        String miniFileWriteCommand = miniFileWriteScriptPath + " " + miniFileDirectory + " " + miniFileWriteNum;
        miniFileReadWriteTest(miniFileWriteCommand, results);
        System.out.println("小文件写入测试完成");

        // 系统资源监测关闭
        monitorProcess.destroy();
        int monitorExitCode = monitorProcess.waitFor();
        System.out.println("系统资源监测关闭,检测结果保存在" + monitorResultCSV + " exit code:" + monitorExitCode);

        fioResultSave(results);
    }

    // 将带宽从KiB/s或MiB/s转换为KiB/s
    private static double convertBWToMiB(String value, String unit) {
        double numericalValue = Double.parseDouble(value);
        switch (unit) {
            case "KiB":
                return numericalValue;
            case "MiB":
                return numericalValue * 1000;
            default:
                return 0;
        }
    }

    // 将延迟从nsec、usec或msec转换为usec
    private static double convertLatencyToMsec(String value, String unit) {
        double numericalValue = Double.parseDouble(value);
        switch (unit) {
            case "nsec":
                return numericalValue / 1000;
            case "usec":
                return numericalValue;
            case "msec":
                return numericalValue * 1000;
            default:
                return 0;
        }
    }

    public void fioResultSave(List<String> results) {
        if (results.isEmpty()) {
            System.out.println("小文件读取测试输出为空");
            return;
        }
        StringBuilder textBuilder = new StringBuilder();
        for (String result : results) {
            textBuilder.append(result);
            textBuilder.append(System.lineSeparator());
        }
        String content = textBuilder.toString();

//        Pattern patternIOPS_BW = Pattern.compile("(read|write): IOPS=(\\d+(?:\\.\\d+)?), BW=(\\d+(?:\\.\\d+)?)(KiB|MiB)/s");
        Pattern patternIOPS_BW = Pattern.compile("(read|write): IOPS=(\\d+(?:\\.\\d+)?k?), BW=(\\d+(?:\\.\\d+)?)(KiB|MiB)/s");
        Pattern patternLatency = Pattern.compile("(read|write):.*?\\n\\s+lat \\((nsec|usec|msec)\\):.*?avg=(\\d+(?:\\.\\d+)?)", Pattern.DOTALL);

        Matcher matcherIOPS_BW = patternIOPS_BW.matcher(content);
        Matcher matcherLatency = patternLatency.matcher(content);

        String readIOPS = "0";
        String readBW = "0";
        String writeIOPS = "0";
        String writeBW = "0";
        String readLat = "0";
        String writeLat = "0";

        while (matcherIOPS_BW.find()) {
            double bw = convertBWToMiB(matcherIOPS_BW.group(3), matcherIOPS_BW.group(4));
            if ("read".equals(matcherIOPS_BW.group(1))) {
                readIOPS = matcherIOPS_BW.group(2);
                readBW = String.valueOf(bw);
            } else if ("write".equals(matcherIOPS_BW.group(1))) {
                writeIOPS = matcherIOPS_BW.group(2);
                writeBW = String.valueOf(bw);
            }
        }

        while (matcherLatency.find()) {
            double lat = convertLatencyToMsec(matcherLatency.group(3), matcherLatency.group(2));
            if ("read".equals(matcherLatency.group(1))) {
                readLat = String.valueOf(lat);
            } else if ("write".equals(matcherLatency.group(1))) {
                writeLat = String.valueOf(lat);
            }
        }
        // 添加结果到TestResult类
        fioMiniFileTestResult.names = TestResult.FIO_MINIFILE_TEST;
        fioMiniFileTestResult.values = new String[]{readIOPS, readBW, readLat, writeIOPS, writeBW, writeLat};

        System.out.println("111");
        // 保存结果到文件
        try {
            // 创建 FileWriter 对象
            FileWriter fileWriter = new FileWriter(directory + "/" + miniFileTestResultTxt);
            // 创建 BufferedWriter 对象
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            // 写入文本内容
            for (String s : fioMiniFileTestResult.values) {
                bufferedWriter.write(s);
                bufferedWriter.write(",");
            }
            bufferedWriter.newLine();
            // 关闭 BufferedWriter
            bufferedWriter.close();
            System.out.println("小文件测试结果保存到：" + directory + "/" + miniFileTestResultTxt);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println(Arrays.toString(fioMiniFileTestResult.values));
        System.out.println("FIO小文件测试结果保存完成");
    }

    @Override
    public List<List<Double>> getTimeData() {
        return null;
    }

    @Override
    public TestResult getTestResults() {
        return fioMiniFileTestResult;
    }

    @Override
    public String getResultDicName() {
        String testName = miniFileName;
        // 获取当前的日期和时间
        LocalDateTime currentDateTime = LocalDateTime.now();
//        System.out.println("Current Date and Time: " + currentDateTime);

        // 定义日期时间格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss");

        // 格式化日期时间
        String formattedDateTime = currentDateTime.format(formatter);

        // 输出格式化后的日期时间
//        System.out.println("Formatted Date and Time: " + formattedDateTime);

        String resultDicName = testName + "_" + formattedDateTime;
        return resultDicName;
    }

    @Override
    public void writeToFile(String resultPath) {
        // 把测试结果和系统资源结果文件保存到resultPath目录
        String command = "cp " + directory + miniFileTestResultTxt + " " + directory + "/" + monitorResultCSV + " " + resultPath;
        int exitCode = 0;
        try {
            exitCode = executeCommand(command);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("保存测试结果和系统资源结果成功" + " exit code:" + exitCode);
    }

    @Override
    public TestAllResult readFromFile(String resultPath) {

        String filePath = resultPath + "/" + "fioMiniFileTestResult.txt";
//        List<String> result = new ArrayList<>();
        String[] result = new String[0];
        try {
            // 创建文件对象
            File file = new File(filePath);
            // 创建 BufferedReader 以读取文件内容
            BufferedReader reader = null;
            reader = new BufferedReader(new FileReader(file));
            String line;
            line = reader.readLine();
//            result = Arrays.asList(line).toArray(new String[0]);
            result = line.split(",");
            System.out.println(Arrays.toString(result));
            // 关闭 BufferedReader
            reader.close();
        } catch (IOException e) {
            // 处理读取文件时可能发生的异常
            e.printStackTrace();
        }

        TestResult testResult = new TestResult();
        testResult.names = TestResult.FIO_MINIFILE_TEST;
        testResult.values = result;
        return new TestAllResult(testResult);
    }

    @Override
    public List<List<Double>> readFromFile1(String resultPath) {
        return null;
    }

    public static void main(String[] args) throws Exception {
//        MiniFileTest miniFileTest = new MiniFileTest("/home/wlx/fsTest", "Admin@wlx");
        MiniFileTest miniFileTest = new MiniFileTest("/home/autotuning/zf/glusterfs/software_test", "666");
        miniFileTest.startTest();
        String name = miniFileTest.getResultDicName();
        System.out.println(name);
        miniFileTest.readFromFile("/home/autotuning/zf/glusterfs/software_test/miniFileTest");
    }
}

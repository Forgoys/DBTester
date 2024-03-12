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

public class FioParallelTest extends TestItem {

    // 获取用户参数 测试目录 并发线程数
    private String directory;
    private String numjobs;

    // sudo权限
    String localSudoPassword;

    // 资源检测脚本名称
    private String monitorScriptName;
    private String monitorResultCSV;

    // 指令运行结果
    TestResult fioParallelTestResult = new TestResult();
    // 测试结果保存文件
    private String fioParallelTestResultTxt;

    public FioParallelTest() {
    }

    public FioParallelTest(String directory, String numjobs, String localSudoPassword) {
        this.directory = directory + "/parallelTest";
        this.numjobs = numjobs;
        this.localSudoPassword = localSudoPassword;

        monitorScriptName = "monitor.sh";
        monitorResultCSV = "fioParallelTestMonitorResult.csv";
        fioParallelTestResultTxt = "fioParallelTestResult.txt";
    }

    public int executeCommand(String command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("bash", "-c", command);
        Process process = processBuilder.start();
        // 等待进程执行完毕
        int exitCode = process.waitFor();
        return exitCode;
    }

    // 并发测试环境 安装fio
    @Override
    public void testEnvPrepare() throws RuntimeException, IOException, InterruptedException {
        int exitCode = 0;
        String command = new String();

        // 创建ParallelTest文件夹
        command = "mkdir -p " + directory;
        exitCode = executeCommand(command);
        System.out.println("创建并发度测试文件夹:" + directory + " Exit code:" + exitCode);

        // 传入检测系统资源的脚本
        String currentDirectory = System.getProperty("user.dir");
        System.out.println("Current directory: " + currentDirectory);
        String localMonitorScriptPath = currentDirectory + "/src/main/resources/scripts/" + monitorScriptName;
        command = "cp " + localMonitorScriptPath + " " + directory;
        exitCode = executeCommand(command);
        System.out.println("系统资源监测脚本:" + localMonitorScriptPath + " Exit code:" + exitCode);

        // 给脚本添加执行权限
        command = "chmod +x " + directory + "/" + monitorScriptName;
        exitCode = executeCommand(command);
        System.out.println("给脚本添加执行权限:" + monitorScriptName + " Exit code:" + exitCode);
    }

    @Override
    public void startTest() throws IOException, InterruptedException {
        // 获取用户参数 定义为成员变量 默认已经存在
//        String directory = "/home/wlx/zf/data";
//        String numjobs = 128;

        // 准备环境
        testEnvPrepare();

        // 检测系统资源 CPU利用率 内存使用率
        String command = directory + "/" + monitorScriptName + " " + directory + "/" + monitorResultCSV;
        ProcessBuilder monitorProcessBuilder = new ProcessBuilder();
        monitorProcessBuilder.command("bash", "-c", command);
        Process monitorProcess = monitorProcessBuilder.start();

        // 设置 fio 测试指令
        String fioCommand = "fio -directory=" + directory + " -ioengine=libaio -direct=1 -iodepth=1 -thread=1 -numjobs=" + numjobs + " -group_reporting -allow_mounted_write=1 -rw=rw -rwmixread=70 -rwmixwrite=30 -bs=4k -size=1M -runtime=60 -name=fioTest";

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
        List<String> results = new ArrayList<>();
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
        System.out.println("指令运行结束");

        // 系统资源监测关闭
        monitorProcess.destroy();
        int monitorExitCode = monitorProcess.waitFor();
        System.out.println("系统资源监测关闭,检测结果保存在" + monitorResultCSV + " exit code:" + monitorExitCode);

        // 保存结果
        fioResultSave(results);

        System.out.println("并发度测试完成");
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
        StringBuilder textBuilder = new StringBuilder();
        for (String result : results) {
            textBuilder.append(result);
            textBuilder.append(System.lineSeparator());
        }
        String content = textBuilder.toString();

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
        fioParallelTestResult.names = TestResult.FIO_PARALLEL_TEST;
        fioParallelTestResult.values = new String[]{readIOPS, readBW, readLat, writeIOPS, writeBW, writeLat};

        // 保存结果到文件
        try {
            // 创建 FileWriter 对象
            FileWriter fileWriter = new FileWriter(directory + "/" + fioParallelTestResultTxt);
            // 创建 BufferedWriter 对象
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            // 写入文本内容
            for (String s : fioParallelTestResult.values) {
                bufferedWriter.write(s);
                bufferedWriter.write(",");
            }
            bufferedWriter.newLine();
            // 关闭 BufferedWriter
            bufferedWriter.close();
            System.out.println("读写测试结果保存到：" + directory + "/" + fioParallelTestResultTxt);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println(Arrays.toString(fioParallelTestResult.values));
        System.out.println("FIO并发度测试结果保存完成");
    }

    @Override
    public List<List<Double>> getTimeData() {
        return null;
    }

    @Override
    public TestResult getTestResults() {
        return fioParallelTestResult;
    }

    @Override
    public String getResultDicName() {
        String testName = "numjobs" + numjobs;
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

        int exitCode = 0;
        try {
            exitCode = executeCommand("mkdir -p " + resultPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        String command = "cp " + directory + "/" + fioParallelTestResultTxt + " " + directory + "/" + monitorResultCSV + " " + resultPath;
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
        String filePath = resultPath + "/" + "fioParallelTestResult.txt";
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
        testResult.names = TestResult.FIO_PARALLEL_TEST;
        testResult.values = result;
        return new TestAllResult(testResult);
    }

    @Override
    public List<List<Double>> readFromFile1(String resultPath) {
        return null;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        FioParallelTest fioParallelTest = new FioParallelTest("/home/autotuning/zf/glusterfs/software_test", "16", "666");
        fioParallelTest.startTest();
        String name = fioParallelTest.getResultDicName();
        System.out.println(name);
        fioParallelTest.readFromFile("/home/autotuning/zf/glusterfs/software_test/parallelTest");
    }
}

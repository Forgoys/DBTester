package backend.tester.fileSystem;

import backend.dataset.TestAllResult;
import backend.dataset.TestResult;
import backend.tester.TestItem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    // 指令运行结果
    TestResult fioMiniFileTestResult = new TestResult();
    // 脚本执行后结果保存在txt文件
    String fioMiniFileTestResultPath;

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
        fioMiniFileTestResultPath = this.directory + "/" + "miniFileTestResult.txt";

        miniFileWriteNum = "100";
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
        Pattern patternIOPS_BW = Pattern.compile("(read|write): IOPS=(\\d+(?:\\.\\d+)?k?), BW=(\\d+(?:\\.\\d+)?)(KiB|MiB)/s\n");
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

    public static void main(String[] args) throws Exception {
        MiniFileTest miniFileTest = new MiniFileTest("/home/wlx/fsTest", "Admin@wlx");
        miniFileTest.startTest();
    }
}

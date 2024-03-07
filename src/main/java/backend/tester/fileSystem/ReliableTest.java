package backend.tester.fileSystem;

import backend.dataset.TestAllResult;
import backend.dataset.TestResult;
import backend.dataset.TestTimeData;
import backend.tester.TestItem;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import java.io.*;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReliableTest extends TestItem {

    // 用户输入参数
    private String directory;
    private String timeChoose; // 4h 24h 7day
    private String fioReliableTestTime; // 对应的秒数
    private String reliableResultDirectory; // 结果存放目录
    private String reliableTestScriptName; // 执行可靠性测试脚本名称

    private String processReliableResultCsvName; // 保存结果的csv文件名
    private String reliableResultCSV;

    // sudo权限
    String localSudoPassword;

    // 资源检测脚本名称
    private String monitorScriptName;
    private String monitorResultCSV;

    // 存放时序性数据
    private TestTimeData reliableTimeData = new TestTimeData();
    private List<List<String>> reliableResultList = new ArrayList<>();

    public ReliableTest(String directory, String timeChoose, String localSudoPassword) throws IOException, InterruptedException {
        this.directory = directory + "/reliableTest";
        this.timeChoose = timeChoose;
        this.localSudoPassword = localSudoPassword;

        monitorScriptName = "monitor.sh";
        monitorResultCSV = "fioReliableMonitorResult.csv";
        reliableResultCSV = "reliableResult.csv";
    }

    public ReliableTest() {

    }

    public int executeCommand(String command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("bash", "-c", command);
        Process process = processBuilder.start();
        // 等待进程执行完毕
        int exitCode = process.waitFor();
        return exitCode;
    }

    // 创建文件夹 复制脚本
    @Override
    public void testEnvPrepare() throws IOException, InterruptedException {
        // 转换timeChoose为秒数
        long seconds = 0;
        try {
            // 解析数字和单位
            int value = Integer.parseInt(timeChoose.replaceAll("\\D", ""));
            String suffix = timeChoose.replaceAll("\\d", "");

            // 根据单位计算秒数
            switch (suffix.toLowerCase()) {
                case "s":
                    seconds = value;
                    break;
                case "min":
                    seconds = value * 60;
                    break;
                case "h":
                    seconds = value * 3600;
                    break;
                case "day":
                    seconds = value * 86400;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid time suffix: " + suffix);
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid input format: " + timeChoose);
            e.printStackTrace();
        }
        fioReliableTestTime = String.valueOf(seconds);
        reliableResultDirectory = directory + "/" + "fioReliableTestResult" + "_" + timeChoose;
        reliableTestScriptName = "reliableTest.sh";
        processReliableResultCsvName = "reliableResult.csv";

        String currentDirectory = System.getProperty("user.dir");
        System.out.println("Current directory: " + currentDirectory);
        String localScriptPath = currentDirectory + "/src/main/resources/scripts/" + reliableTestScriptName;

        ProcessBuilder processBuilder = new ProcessBuilder();
        // 创建reliableTest可靠性测试文件夹
        System.out.println("创建文件夹:" + directory);
        String command = "mkdir -p " + directory;
        processBuilder.command("bash", "-c", command);
        Process process = processBuilder.start();

        // 等待进程执行完毕
        int exitCode = process.waitFor();
        System.out.println("创建可靠性测试文件夹:" + directory + " Exit code:" + exitCode);

        // 复制reliableTest可靠性测试脚本
        command = "cp " + localScriptPath + " " + directory;
        processBuilder.command("bash", "-c", command);
        System.out.println(command);
        process = processBuilder.start();
        // 等待进程执行完毕
        exitCode = process.waitFor();
        System.out.println("复制reliableTest可靠性测试脚本:" + reliableTestScriptName + " Exit code:" + exitCode);

        // 给脚本添加执行权限
        command = "chmod +x " + directory + "/" + reliableTestScriptName;
        processBuilder.command("bash", "-c", command);
        System.out.println(command);
        process = processBuilder.start();
        // 等待进程执行完毕
        exitCode = process.waitFor();
        System.out.println("给脚本添加执行权限:" + reliableTestScriptName + " Exit code:" + exitCode);

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

    @Override
    public void startTest() throws IOException, InterruptedException {
        System.out.println("可靠性测试环境准备");
        testEnvPrepare();
        System.out.println("可靠性测试环境准备完成");

        // 检测系统资源 CPU利用率 内存使用率
        String command = directory + "/" + monitorScriptName + " " + directory + "/" + monitorResultCSV;
        ProcessBuilder monitorProcessBuilder = new ProcessBuilder();
        monitorProcessBuilder.command("bash", "-c", command);
        Process monitorProcess = monitorProcessBuilder.start();

        System.out.println("可靠性测试开始");
        // 测试指令
        String reliableCommand = directory + "/" + reliableTestScriptName + " " + directory + " " + fioReliableTestTime + " " + reliableResultDirectory;

        String password = localSudoPassword;
        reliableCommand = "echo " + password + " | sudo -S " + reliableCommand;
        System.out.println(reliableCommand);

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("bash", "-c", reliableCommand);

        // 启动进程 结果保存在reliableResultDirectory
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

        // 处理结果
        processReliableResult();
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

    public void fioResultSave(String content) {

        Pattern patternIOPS_BW = Pattern.compile("(read|write): IOPS=(\\d+(?:\\.\\d+)?), BW=(\\d+(?:\\.\\d+)?)(KiB|MiB)/s");
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

        // 保存结果
        List<String> result = new ArrayList<>();
        result.add(readIOPS);
        result.add(readBW);
        result.add(readLat);
        result.add(writeIOPS);
        result.add(writeBW);
        result.add(writeLat);
        reliableResultList.add(result);
    }

    // 处理保存到文件夹的结果
    public void processReliableResult() throws InterruptedException, IOException {

        File resultDirectory = new File(reliableResultDirectory);
        // 遍历目录下的所有txt文件
        File[] files = resultDirectory.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));
        if (files != null) {
            for (File file : files) {
                StringBuilder contentBuilder = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        contentBuilder.append(line).append("\n");
                    }
                    String text = contentBuilder.toString();
                    fioResultSave(text);
                } catch (IOException e) {
                    System.err.println("Error reading file: " + file.getAbsolutePath());
                    e.printStackTrace();
                }
            }
        }
        for (List<String> list : reliableResultList) {
            System.out.println(list);
        }

        // 把结果保存到csv文件
        try {
            // 创建 FileWriter 对象
            FileWriter fileWriter = new FileWriter(directory + "/" + reliableResultCSV);
            // 创建 BufferedWriter 对象
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            // 写入文本内容
            for (List<String> list : reliableResultList) {
                for (String s : list) {
                    bufferedWriter.write(s);
                    bufferedWriter.write(",");
                }
                bufferedWriter.newLine();
            }

            // 关闭 BufferedWriter
            bufferedWriter.close();
            System.out.println("可靠性测试结果保存到：" + directory + "/" + reliableResultCSV);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 转换下格式
        List<Double> readIOPSList = new ArrayList<>();
        List<Double> readBWList = new ArrayList<>();
        List<Double> readLatList = new ArrayList<>();
        List<Double> writeIOPSList = new ArrayList<>();
        List<Double> writeBWList = new ArrayList<>();
        List<Double> writeLatList = new ArrayList<>();
        for (List<String> row : reliableResultList) {
            readIOPSList.add(Double.valueOf(row.get(0)));
            readBWList.add(Double.valueOf(row.get(1)));
            readLatList.add(Double.valueOf(row.get(2)));
            writeIOPSList.add(Double.valueOf(row.get(3)));
            writeBWList.add((Double.valueOf(row.get(4))));
            writeLatList.add(Double.valueOf(row.get(5)));
        }
        List<List<Double>> reliableResult = new ArrayList<>();
        reliableResult.add(readIOPSList);
        reliableResult.add(readBWList);
        reliableResult.add(readLatList);
        reliableResult.add(writeIOPSList);
        reliableResult.add(writeBWList);
        reliableResult.add(writeLatList);

        reliableTimeData.names = TestTimeData.FS_RELIABLE_TIMEDATA_NAMES;
        reliableTimeData.values = reliableResult;

        for (List<Double> list : reliableResult) {
            System.out.println(list);
        }
    }

    @Override
    public List<List<Double>> getTimeData() {
        return reliableTimeData.values;
    }

    @Override
    public TestResult getTestResults() {
        return null;
    }

    @Override
    public String getResultDicName() {
        String testName = "timeChoose" + timeChoose;
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

    // 把可靠性结果放到csv
    @Override
    public void writeToFile(String resultPath) {
        // 把测试结果和系统资源结果文件保存到resultPath目录
        String command = "cp " + directory + "/" + reliableResultCSV + " " + directory + "/" + monitorResultCSV + " " + resultPath;
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
        String filePath = resultPath + "/" + "reliableResult.csv";
        List<List<Double>> testResult = new ArrayList<>();
        try {
            // 创建文件对象
            File file = new File(filePath);
            // 创建 BufferedReader 以读取文件内容
            BufferedReader reader = null;
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] arrayString;
                List<Double> list = new ArrayList<>();
                arrayString = line.split(",");
                for (String s : arrayString) {
                    list.add(Double.valueOf(s));
                }
                testResult.add(list);
            }
            // 关闭 BufferedReader
            reader.close();
        } catch (IOException e) {
            // 处理读取文件时可能发生的异常
            e.printStackTrace();
        }

        // 转换下格式
        List<Double> readIOPSList = new ArrayList<>();
        List<Double> readBWList = new ArrayList<>();
        List<Double> readLatList = new ArrayList<>();
        List<Double> writeIOPSList = new ArrayList<>();
        List<Double> writeBWList = new ArrayList<>();
        List<Double> writeLatList = new ArrayList<>();
        for (List<Double> row : testResult) {
            readIOPSList.add(row.get(0));
            readBWList.add(row.get(1));
            readLatList.add(row.get(2));
            writeIOPSList.add(row.get(3));
            writeBWList.add(row.get(4));
            writeLatList.add(row.get(5));
        }
        testResult.clear();
        testResult.add(readIOPSList);
        testResult.add(readBWList);
        testResult.add(readLatList);
        testResult.add(writeIOPSList);
        testResult.add(writeBWList);
        testResult.add(writeLatList);
        System.out.println(testResult);
        return new TestAllResult(testResult);
    }

    @Override
    public List<List<Double>> readFromFile1(String resultPath) {
        return List.of();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        ReliableTest reliableTest = new ReliableTest("/home/autotuning/zf/glusterfs/software_test", "1min", "666");
        reliableTest.startTest();
        reliableTest.readFromFile("/home/autotuning/zf/glusterfs/software_test/reliableTest");
    }

}

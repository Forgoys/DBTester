package backend.tester.fileSystem;

import backend.dataset.TestAllResult;
import backend.dataset.TestResult;
import backend.dataset.TestTimeData;
import backend.tester.TestItem;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
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

    // sudo权限
    String localSudoPassword;

    // 存放时序性数据
    private TestTimeData reliableTimeData = new TestTimeData();
    private List<List<String>> reliableResultList = new ArrayList<>();

    public ReliableTest(String directory, String timeChoose, String localSudoPassword) throws IOException, InterruptedException {
        this.directory = directory + "/reliableTest";
        this.timeChoose = timeChoose;
        this.localSudoPassword = localSudoPassword;
    }

    public ReliableTest() {

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
    }

    @Override
    public void startTest() throws IOException, InterruptedException {
        System.out.println("可靠性测试环境准备");
        testEnvPrepare();
        System.out.println("可靠性测试环境准备完成");

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

        File directory = new File(reliableResultDirectory);
        // 遍历目录下的所有txt文件
        File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));
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

        // 处理CSV的文件保存到时序数据里
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
        System.out.println("CSV结果处理完成");
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
        return null;
    }

    @Override
    public void writeToFile(String resultPath) {

    }

    @Override
    public TestAllResult readFromFile(String resultPath) {

        return null;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        ReliableTest reliableTest = new ReliableTest("/home/autotuning/zf/glusterfs/software_test", "1min", "666");
        reliableTest.startTest();
    }

}

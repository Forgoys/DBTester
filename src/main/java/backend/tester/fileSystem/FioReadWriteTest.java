package backend.tester.fileSystem;

import backend.dataset.TestAllResult;
import backend.dataset.TestResult;
import backend.tester.TestItem;

import java.io.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;


public class FioReadWriteTest extends TestItem {
    // 用户参数 目录 文件块大小 文件大小 读写方式
    private String directory;
    private String bs;
    private String size;
    private String rwIndex;
    private String rwOption;

    // 资源检测脚本名称
    private String monitorScriptName;
    private String monitorResultCSV;

    // 测试结果保存文件
    String fioReadWriteTestResultTxt;

    // sudo权限
    String localSudoPassword;

    // 指令运行结果
    TestResult fioRWTestResult = new TestResult();

    public FioReadWriteTest() {
    }

    public FioReadWriteTest(String directory, String localSudoPassword, String bs, String size, String rwOption) {
        this.directory = directory + "/readWriteTest";
        this.bs = bs;
        this.size = size;
        this.rwOption = rwOption;
        switch (rwOption) {
            case "随机读":
                rwIndex = "0";
                break;
            case "随机写":
                rwIndex = "1";
                break;
            case "顺序读":
                rwIndex = "2";
                break;
            case "顺序写":
                rwIndex = "3";
                break;
            case "70%顺序读,30%顺序写":
                rwIndex = "4";
                break;
            case "70%随机读,30%随机写":
                rwIndex = "5";
                break;
            default:
                rwIndex = "0";
        }
        this.localSudoPassword = localSudoPassword;

        monitorScriptName = "monitor.sh";
        monitorResultCSV = "fioReadWriteTestMonitorResult.csv";
        fioReadWriteTestResultTxt = "fioReadWriteTestResult.txt";
    }

    public int executeCommand(String command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("bash", "-c", command);
        Process process = processBuilder.start();
        // 等待进程执行完毕
        int exitCode = process.waitFor();
        return exitCode;
    }

    // 安装FIO测试工具
    @Override
    public void testEnvPrepare() throws IOException, InterruptedException {
        int exitCode = 0;
        String command = new String();

        // 创建readWriteTest文件夹
        command = "mkdir -p " + directory;
        exitCode = executeCommand(command);
        System.out.println("创建读写测试文件夹:" + directory + " Exit code:" + exitCode);

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
//        String bs = "4k";
//        String size = "1G";
//        String rwIndex = 0;

        // 准备环境
        testEnvPrepare();

        // 检测系统资源 CPU利用率 内存使用率
        String command = directory + "/" + monitorScriptName + " " + directory + "/" + monitorResultCSV;
        ProcessBuilder monitorProcessBuilder = new ProcessBuilder();
        monitorProcessBuilder.command("bash", "-c", command);
        Process monitorProcess = monitorProcessBuilder.start();

        System.out.println("FIO读写速度测试开始");
        System.out.println("测试参数为:");
        System.out.println("测试目录：" + directory + " 文件块大小：" + bs + " 文件大小：" + size + " 读写方式：" + rwIndex);

        // 创建读写方式表
        List<String> rwList = new ArrayList<>();
        rwList.add("-rw=randread");
        rwList.add("-rw=randwrite");
        rwList.add("-rw=read");
        rwList.add("-rw=write");
        rwList.add("-rw=rw -rwmixread=70 -rwmixwrite=30");
        rwList.add("-rw=randrw -rwmixread=70 -rwmixwrite=30");

        // 设置 fio 测试指令
        String fioCommand = "fio -directory=" + directory + " -ioengine=libaio -direct=1 -iodepth=1 -thread=1 -numjobs=1 -group_reporting -allow_mounted_write=1 " + rwList.get(Integer.parseInt(rwIndex)) + " -bs=" + bs + " -size=" + size + " -runtime=60 -name=fioTest";

        fioCommand = "echo " + localSudoPassword + " | sudo -S " + fioCommand;
        System.out.println(fioCommand);

        // 创建一个 ProcessBuilder 对象
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("bash", "-c", fioCommand);

        // 启动进程
        Process process = processBuilder.start();

        // 获取进程的输出流
        InputStream inputStream = process.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        List<String> results = new ArrayList<>();
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

        System.out.println("指令运行结束");

        // 系统资源监测关闭
        monitorProcess.destroy();
        int monitorExitCode = monitorProcess.waitFor();
        System.out.println("系统资源监测关闭,检测结果保存在" + monitorResultCSV + " exit code:" + monitorExitCode);

        // 提取出结果并保存到TestResult
        fioResultSave(results);

        System.out.println("FIO读写速度测试完成");
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
        fioRWTestResult.names = TestResult.FIO_RW_TEST;
        fioRWTestResult.values = new String[]{readIOPS, readBW, readLat, writeIOPS, writeBW, writeLat};
        System.out.println(Arrays.toString(fioRWTestResult.values));

        // 保存结果到文件
        try {
            // 创建 FileWriter 对象
            FileWriter fileWriter = new FileWriter(directory + "/" + fioReadWriteTestResultTxt);
            // 创建 BufferedWriter 对象
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            // 写入文本内容
            for (String s : fioRWTestResult.values) {
                bufferedWriter.write(s);
                bufferedWriter.write(",");
            }
            bufferedWriter.newLine();

            // 关闭 BufferedWriter
            bufferedWriter.close();
            System.out.println("读写测试结果保存到：" + directory + "/" + fioReadWriteTestResultTxt);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<List<Double>> monitorResult = new ArrayList<>();
        monitorResult = getTimeData();
        System.out.println(monitorResult);

        System.out.println("FIO读写测试结果保存完成");
    }


    @Override
    public TestResult getTestResults() {
        return fioRWTestResult;
    }

    // 返回系统资源数据
    @Override
    public List<List<Double>> getTimeData() {
        List<List<Double>> monitorResult = new ArrayList<>();

        String csvFile = directory + "/" + monitorResultCSV;
        try (FileReader reader = new FileReader(csvFile);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT)) {
            for (CSVRecord record : csvParser) {
                String iteration = record.get(0);
                String userCPU = record.get(1);
                String systemCPU = record.get(2);
                String ioWaitCPU = record.get(3);
                String memoryUsage = record.get(4);
                System.out.println("Iteration: " + iteration + ", User CPU Usage (%): " + userCPU +
                        ", System CPU Usage (%): " + systemCPU + ", I/O Wait CPU Usage (%): " + ioWaitCPU +
                        ", Memory Usage (%): " + memoryUsage);
                List<Double> list = new ArrayList<>();
                list.add(Double.valueOf(systemCPU));
                list.add(Double.valueOf(memoryUsage));
                monitorResult.add(list);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<Double> systemCpuUsageList = new ArrayList<>();
        List<Double> memoryUsageList = new ArrayList<>();
        for (List<Double> list : monitorResult) {
            systemCpuUsageList.add(list.get(0));
            memoryUsageList.add(list.get(1));
        }
        monitorResult.clear();
        monitorResult.add(systemCpuUsageList);
        monitorResult.add(memoryUsageList);
        return monitorResult;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public String getBs() {
        return bs;
    }

    public void setBs(String bs) {
        this.bs = bs;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getRwIndex() {
        return rwIndex;
    }

    public void setRwIndex(String rwIndex) {
        this.rwIndex = rwIndex;
    }

    @Override
    public String getResultDicName() {
        String testName = "size" + size + "_bs" + bs + "_" + rwOption;
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

    // 把fio测试结果和系统资源结果保存到这个目录
    @Override
    public void writeToFile(String resultPath) {
        int exitCode = 0;
        try {
            exitCode = executeCommand("mkdir -p " + resultPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // 把测试结果和系统资源结果文件保存到resultPath目录
        String command = "cp " + directory + "/" + fioReadWriteTestResultTxt + " " + directory + "/" + monitorResultCSV + " " + resultPath;
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
        String filePath = resultPath + "/" + "fioReadWriteTestResult.txt";
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
        testResult.names = TestResult.FIO_RW_TEST;
        testResult.values = result;

        List<List<Double>> resourceResult = new ArrayList<>();
        String resourceResultPath = resultPath + "/" + "fioReadWriteTestMonitorResult.csv";
        try {
            // 创建文件对象
            File file = new File(resourceResultPath);
            // 创建 BufferedReader 以读取文件内容
            BufferedReader reader = null;
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] arrayString;
                List<Double> list = new ArrayList<>();
                arrayString = line.split(",");

                list.add(Double.valueOf(arrayString[2]));
                list.add(Double.valueOf(arrayString[4]));

                resourceResult.add(list);
            }
            // 关闭 BufferedReader
            reader.close();
        } catch (IOException e) {
            // 处理读取文件时可能发生的异常
            e.printStackTrace();
        }
        System.out.println(resourceResult);

        // 转换下格式
        List<Double> systemCpu = new ArrayList<>();
        List<Double> memory = new ArrayList<>();
        for (List<Double> s : resourceResult) {
            systemCpu.add(s.get(0));
            memory.add(s.get(1));
        }
        resourceResult.clear();
        resourceResult.add(systemCpu);
        resourceResult.add(memory);

        return new TestAllResult(testResult, resourceResult);
    }

    @Override
    public List<List<Double>> readFromFile1(String resultPath) {
        return null;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        FioReadWriteTest fioReadWriteTest = new FioReadWriteTest("/home/autotuning/zf/glusterfs/software_test", "666", "4k", "16k", "%70随机读,%30随机写");
//        FioReadWriteTest fioReadWriteTest = new FioReadWriteTest("/home/parallels/Desktop/fs", "lhjlhj6929", "4k", "8k", "%70随机读,%30随机写");
        fioReadWriteTest.startTest();
        fioReadWriteTest.readFromFile("/home/autotuning/zf/glusterfs/software_test/readWriteTest");
    }
}

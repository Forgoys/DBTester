package backend.tester.fileSystem;

import backend.dataset.TestResult;
import backend.tester.TestItem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MiniFileTest extends TestItem {

    // 参数 root目录 测试的小文件名 测试小文件路径 脚本名 脚本路径
    private String directory;
    private String miniFileName;
    private String miniFileDirectory;
    private String miniFileScriptName;
    private String miniFileScriptDirectory;

    // 指令运行结果
    TestResult fioMiniFileTestResult = new TestResult();

    public MiniFileTest(String directory) {
        this.directory = directory;
        miniFileName = "cifar-10-batches-bin";
        miniFileDirectory = directory + "/" + miniFileName;
        miniFileScriptName = "miniFileTest.sh";
        miniFileScriptDirectory = directory + "/" + miniFileScriptName;
    }


    @Override
    public void generateTimeData() {

    }

    // 准备测试数据 准备测试工具 准备测试脚本 配置文件.ini
    @Override
    public void testEnvPrepare() throws Exception {

    }

    @Override
    public void startTest() throws IOException, InterruptedException {
//        获取参数
//        String directory;
//        String miniFileName;
//        String miniFileDirectory;
//        String miniFileScriptName;
//        String miniFileScriptDirectory;

        // 执行脚本指令
        String fioCommand = miniFileScriptDirectory + miniFileDirectory;

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

        // 等待进程执行完毕
        int exitCode = process.waitFor();
        System.out.println("Exit code: " + exitCode);

        fioResultSave(results);
    }

    public void fioResultSave(List<String> results) {
        StringBuilder textBuilder = new StringBuilder();
        for (String result : results) {
            textBuilder.append(result);
        }
        String text = textBuilder.toString();

        // 分别定义read和write的正则表达式
        String regexRead = "read: IOPS=(\\d+), BW=(\\d+)(KiB/s|kB/s).*?lat \\((usec|msec)\\):.*?avg=(\\d+\\.\\d+),";
        String regexWrite = "write: IOPS=(\\d+), BW=(\\d+)(KiB/s|kB/s).*?lat \\((usec|msec)\\):.*?avg=(\\d+\\.\\d+),";

        // 对读的部分处理
        Pattern pattern = Pattern.compile(regexRead, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        String readIops = "0";
        String readBw = "0";
        String readLat = "0";
        while (matcher.find()) {
            readIops = matcher.group(1);
            readBw = matcher.group(2);
            String unit = matcher.group(3); // 保留带宽单位
            String latUnit = matcher.group(4);
            double avgLat = Double.parseDouble(matcher.group(5));
            if ("msec".equals(latUnit)) {
                avgLat *= 1000; // 如果单位是msec，则乘以1000
            }
            readLat = String.valueOf(avgLat);
            // 打印结果
            System.out.println("read:IOPS=" + readIops + ", BW=" + readBw + unit + ", Avg Latency=" + readLat + "usec");
        }

        // 对写的部分处理
        pattern = Pattern.compile(regexWrite, Pattern.DOTALL);
        matcher = pattern.matcher(text);
        String writeIops = "0";
        String writeBw = "0";
        String writeLat = "0";
        while (matcher.find()) {
            writeIops = matcher.group(1);
            writeBw = matcher.group(2);
            String unit = matcher.group(3); // 保留带宽单位
            String latUnit = matcher.group(4);
            double avgLat = Double.parseDouble(matcher.group(5));
            if ("msec".equals(latUnit)) {
                avgLat *= 1000; // 如果单位是msec，则乘以1000
            }
            writeLat = String.valueOf(avgLat);
            // 打印结果
            System.out.println("write:IOPS=" + writeIops + ", BW=" + writeBw + unit + ", Avg Latency=" + writeLat + "usec");
        }

        // 添加结果到TestResult类
        fioMiniFileTestResult.names = TestResult.FIO_MINIFILE_TEST;
        fioMiniFileTestResult.values = new String[]{readIops, readBw, readLat, writeIops, writeBw, writeLat};
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
    public void writeToFile(String resultPath) {

    }

    @Override
    public void readFromFile(String resultPath) {

    }
}

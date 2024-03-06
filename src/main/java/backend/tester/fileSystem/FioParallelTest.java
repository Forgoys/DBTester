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

public class FioParallelTest extends TestItem {

    // 获取用户参数 测试目录 并发线程数
    private String directory;
    private String numjobs;

    // sudo权限
    String localSudoPassword;

    // 指令运行结果
    TestResult fioParallelTestResult = new TestResult();

    public FioParallelTest() {
    }

    public FioParallelTest(String directory, String numjobs, String localSudoPassword) {
        this.directory = directory;
        this.numjobs = numjobs;
        this.localSudoPassword = localSudoPassword;
    }

    // 并发测试环境 安装fio
    @Override
    public void testEnvPrepare() throws RuntimeException {

    }

    @Override
    public void startTest() throws IOException, InterruptedException {
        // 获取用户参数 定义为成员变量 默认已经存在
//        String directory = "/home/wlx/zf/data";
//        String numjobs = 128;

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

        // 添加结果到TestResult类
        fioParallelTestResult.names = TestResult.FIO_PARALLEL_TEST;
        fioParallelTestResult.values = new String[]{readIOPS, readBW, readLat, writeIOPS, writeBW, writeLat};

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
        FioParallelTest fioParallelTest = new FioParallelTest("/home/autotuning/zf/glusterfs/software_test", "32", "666");
        fioParallelTest.startTest();
    }
}

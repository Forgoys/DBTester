package backend.tester.fileSystem;

import backend.dataset.TestResult;
import backend.tester.TestItem;
import javafx.scene.SubScene;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FioReadWriteTest extends TestItem {
    // 用户参数 目录 文件块大小 文件大小 读写方式
    private String directory;
    private String bs;
    private String size;
    private String rwIndex;

    // 指令运行结果
    TestResult fioRWTestResult = new TestResult();

    public FioReadWriteTest() {
    }

    public FioReadWriteTest(String directory, String bs, String size, String rwOption) {
        this.directory = directory;
        this.bs = bs;
        this.size = size;
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
            case "%70顺序读,%30顺序写":
                rwIndex = "4";
                break;
            case "%70随机读,%30随机写":
                rwIndex = "5";
                break;
            default:
                rwIndex = "0";
        }
    }

    // 安装FIO测试工具
    @Override
    public void testEnvPrepare() {

    }

    @Override
    public void startTest() throws IOException, InterruptedException {
        // 获取用户参数 定义为成员变量 默认已经存在
//        String directory = "/home/wlx/zf/data";
//        String bs = "4k";
//        String size = "1G";
//        String rwIndex = 0;

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

        String password = "lhjlhj6929";
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

        // 提取出结果并保存到TestResult
        fioResultSave(results);

        System.out.println("FIO读写速度测试完成");
    }

    @Override
    public List<List<Double>> getTimeData() {
        return null;
    }


//    public void fioResultSave(List<String> results) {
//        StringBuilder textBuilder = new StringBuilder();
//        for (String result : results) {
//            textBuilder.append(result);
//            textBuilder.append(System.lineSeparator());
//        }
//        String text = textBuilder.toString();
//        System.out.println(text);
//
//        // 分别定义read和write的正则表达式
//        String regexRead = """
//                read: IOPS=([\\d.]+[kMG]?), BW=([\\d.]+)(KiB/s|MiB/s).*lat \\((usec|msec)\\):.*avg=([\\d.]+)""";
//        String regexWrite = """
//                write: IOPS=([\\d.]+[kMG]?), BW=([\\d.]+)(KiB/s|MiB/s).*lat \\((usec|msec)\\):.*avg=([\\d.]+)""";
//
//        // 对读的部分处理
//        Pattern readPattern = Pattern.compile(regexRead, Pattern.DOTALL);
//        Matcher readMatcher = readPattern.matcher(text);
//        String readIops = "0";
//        String readBw = "0";
//        String readLat = "0";
//        while (readMatcher.find()) {
//            readIops = readMatcher.group(1);
//            readBw = readMatcher.group(2);
//            String bwUnit = readMatcher.group(3); // 带宽单位
//            String latUnit = readMatcher.group(4); // 时延单位
//            readLat = readMatcher.group(5);
//
//            System.out.println("read:IOPS=" + readIops + ", BW=" + readBw + bwUnit + ", Avg Latency=" + readLat + latUnit);
//
//            // 转换单位
//            if ("MiB/s".equals(bwUnit)) {
//                readBw = String.valueOf(Double.parseDouble(readBw) * 1000);
//            }
//            if ("msec".equals(latUnit)) {
//                readLat = String.valueOf(Double.parseDouble(readLat) * 1000);
//            }
//        }
//
//        // 对写的部分处理
//        Pattern writePattern = Pattern.compile(regexWrite, Pattern.DOTALL);
//        Matcher writeMatcher = writePattern.matcher(text);
//        String writeIops = "0";
//        String writeBw = "0";
//        String writeLat = "0";
//        while (writeMatcher.find()) {
//            writeIops = writeMatcher.group(1);
//            writeBw = writeMatcher.group(2);
//            String bwUnit = writeMatcher.group(3); // 带宽单位
//            String latUnit = writeMatcher.group(4); // 时延单位
//            writeLat = writeMatcher.group(5);
//
//            // 打印结果
//            System.out.println("write:IOPS=" + writeIops + ", BW=" + writeBw + bwUnit + ", Avg Latency=" + writeLat + latUnit);
//
//            // 转换单位
//            if ("MiB/s".equals(bwUnit)) {
//                writeBw = String.valueOf(Double.parseDouble(writeBw) * 1000);
//            }
//            if ("msec".equals(latUnit)) {
//                writeLat = String.valueOf(Double.parseDouble(writeLat) * 1000);
//            }
//        }
//
//        // 添加结果到TestResult类
//        fioRWTestResult.names = TestResult.FIO_RW_TEST;
//        fioRWTestResult.values = new String[]{readIops, readBw, readLat, writeIops, writeBw, writeLat};
//        System.out.println(Arrays.toString(fioRWTestResult.values));
//        System.out.println("FIO读写测试结果保存完成");
//    }

    // 将带宽从KiB/s或MiB/s转换为MiB/s
    private static double convertBWToMiB(String value, String unit) {
        double numericalValue = Double.parseDouble(value);
        switch (unit) {
            case "KiB":
                return numericalValue / 1024;
            case "MiB":
                return numericalValue;
            default:
                return 0;
        }
    }

    // 将延迟从nsec、usec或msec转换为msec
    private static double convertLatencyToMsec(String value, String unit) {
        double numericalValue = Double.parseDouble(value);
        switch (unit) {
            case "nsec":
                return numericalValue / 1_000_000;
            case "usec":
                return numericalValue / 1_000;
            case "msec":
                return numericalValue;
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
        fioRWTestResult.names = TestResult.FIO_RW_TEST;
        fioRWTestResult.values = new String[]{readIOPS, readBW, readLat, writeIOPS, writeBW, writeLat};
        System.out.println(Arrays.toString(fioRWTestResult.values));
        System.out.println("FIO读写测试结果保存完成");
    }

//    public void fioResultSave(List<String> results) {
//        StringBuilder textBuilder = new StringBuilder();
//        for (String result : results) {
//            textBuilder.append(result);
//            textBuilder.append(System.lineSeparator());
//        }
//        String text = textBuilder.toString();
//        System.out.println(text);
//
//        String content = text;
//        Pattern patternReadIOPS = Pattern.compile("read: IOPS=([\\d.]+)[kMG]?, BW=([\\d.]+)");
//        Matcher matcherReadIOPS = patternReadIOPS.matcher(content);
//        Pattern patternReadLat = Pattern.compile("read:.*\\n.*lat \\(usec|msec|nsec\\):.*avg=([\\d.]+)");
////        Pattern patternReadLat = Pattern.compile("read:.*lat \\((usec|msec)\\):.*avg=([\\\\d.]+)");
//        Matcher matcherReadLat = patternReadLat.matcher(content);
//        Pattern patternWriteIOPS = Pattern.compile("write: IOPS=([\\d.]+)[kMG]?, BW=([\\d.]+)");
//        Matcher matcherWriteIOPS = patternWriteIOPS.matcher(content);
//        Pattern patternWriteLat = Pattern.compile("write:.*\\n.*lat \\((usec|msec|nsec)\\):.*avg=([\\d.]+)");
////        Pattern patternWriteLat = Pattern.compile("write:.*lat \\((usec|msec)\\):.*avg=([\\d.]+)");
//
//        Matcher matcherWriteLat = patternWriteLat.matcher(content);
//
//        String readIOPS = "0";
//        String readBW = "0";
//        String readLat = "0";
//        String writeIOPS = "0";
//        String writeBW = "0";
//        String writeLat = "0";
//
//        if (matcherReadIOPS.find()) {
//            readIOPS = matcherReadIOPS.group(1);
//            readBW = matcherReadIOPS.group(2);
//        }
//        if (matcherReadLat.find()) {
//            readLat = matcherReadLat.group(1);
//        }
//        if (matcherWriteIOPS.find()) {
//            writeIOPS = matcherWriteIOPS.group(1);
//            writeBW = matcherWriteIOPS.group(2);
//        }
//        if (matcherWriteLat.find()) {
//            writeLat = matcherWriteLat.group(1);
//        }
//
//        // 添加结果到TestResult类
//        fioRWTestResult.names = TestResult.FIO_RW_TEST;
//        fioRWTestResult.values = new String[]{readIOPS, readBW, readLat, writeIOPS, writeBW, writeLat};
//        System.out.println(Arrays.toString(fioRWTestResult.values));
//        System.out.println("FIO读写测试结果保存完成");
//    }


    @Override
    public TestResult getTestResults() {
        return fioRWTestResult;
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
    public void writeToFile(String resultPath) {

    }

    @Override
    public void readFromFile(String resultPath) {

    }

    public static void main(String[] args) throws IOException, InterruptedException {
        FioReadWriteTest fioReadWriteTest = new FioReadWriteTest("/home/parallels/Desktop/fs", "4k", "16k", "%70顺序读,%30顺序写");
        fioReadWriteTest.startTest();
    }
}

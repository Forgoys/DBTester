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
        System.out.println("测试目录：" + directory + " 文件块大小：" + bs + " 文件大小：" + size + "读写方式：" + rwIndex);

        // 创建读写方式表
        List<String> rwList = new ArrayList<>();
        rwList.add("-rw=randread");
        rwList.add("-rw=randwrite");
        rwList.add("-rw=read");
        rwList.add("-rw=write");
        rwList.add("-rw=rw -rwmixread=70 -rwmixwrite=30");
        rwList.add("-rw=randrw -rwmixread=70 -rwmixwrite=30");

        // 设置 fio 测试指令
        String fioCommand = "fio -directory=" + directory + " -ioengine=libaio -direct=1 -iodepth=1 -thread=1 -numjobs=2 -group_reporting -allow_mounted_write=1" + rwList.get(Integer.parseInt(rwIndex)) + " -bs=" + bs + " -size=" + size + " -runtime=60 -name=fioTest";

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
        fioRWTestResult.names = TestResult.FIO_RW_TEST;
        fioRWTestResult.values = new String[]{readIops, readBw, readLat, writeIops, writeBw, writeLat};
    }

    @Override
    public void generateTimeData() {

    }

    @Override
    public TestResult getTestResults() {
        return fioRWTestResult;
    }

    @Override
    public void writeToFile() {

    }

    @Override
    public void readFromFile() {

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

    public TestResult getFioRWTestResult() {
        return fioRWTestResult;
    }

    public void setFioRWTestResult(TestResult fioRWTestResult) {
        this.fioRWTestResult = fioRWTestResult;
    }

    public static void main() throws IOException, InterruptedException {
        FioReadWriteTest fioReadWriteTest = new FioReadWriteTest("/home/autotuning/zf/glusterfs/nfs_test","4k","8k","随机读");
        fioReadWriteTest.startTest();
    }
}

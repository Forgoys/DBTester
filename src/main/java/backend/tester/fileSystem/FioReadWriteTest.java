package backend.tester.fileSystem;

import backend.dataset.TestResult;
import backend.tester.TestItem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class FioReadWriteTest extends TestItem {
    // 用户参数 目录 文件块大小 文件大小 读写方式
    private String directory;
    private String bs;
    private String size;
    private String rwIndex;

    // 指令运行结果
    TestResult fioRWTestResult = new TestResult();

    public FioReadWriteTest() {}

    public FioReadWriteTest(String directory, String bs, String size, String rwIndex) {
        this.directory = directory;
        this.bs = bs;
        this.size = size;
        this.rwIndex = rwIndex;
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

        // 提取出结果并保存到TestResult
        fioResultSave(results);

        System.out.println("FIO读写速度测试完成");
    }

    @Override
    public List<List<Double>> getTimeData() {
        return null;
    }

    public void fioResultSave(List<String> results) {
        String iops = "";
        String bw = "";
        String lat = "";
        // 提取 iops、bw 和 lat 指标
        for (String result : results) {
            if (result.contains("iops")) {
                iops = result.split(":")[1].trim();
                System.out.println("IOPS: " + iops);
            } else if (result.contains("bw")) {
                bw = result.split(":")[1].trim();
                System.out.println("Bandwidth: " + bw);
            } else if (result.contains("lat")) {
                lat = result.split(":")[1].trim();
                System.out.println("Latency: " + lat);
            }
        }
        // 添加结果到TestResult类
        fioRWTestResult.names = TestResult.FIO_RW_TEST;
        fioRWTestResult.values = new String[]{iops, bw, lat};
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
}

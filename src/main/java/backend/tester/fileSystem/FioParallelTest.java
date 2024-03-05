package backend.tester.fileSystem;

import backend.dataset.TestResult;
import backend.tester.TestItem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class FioParallelTest extends TestItem {

    // 获取用户参数 测试目录 并发线程数
    private String directory;
    private String numjobs;

    // 指令运行结果
    TestResult fioParallelTestResult = new TestResult();

    public FioParallelTest() {}

    public FioParallelTest(String directory, String numjobs) {
        this.directory = directory;
        this.numjobs = numjobs;
    }

    @Override
    public void generateTimeData() {

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
        String fioCommand = "fio -directory=" + directory + " -ioengine=libaio -direct=1 -iodepth=1 -thread=1 -numjobs=" + numjobs + " -group_reporting -allow_mounted_write=1 -rw=rw -rwmixread=70 -rwmixwrite=30 -bs=4k -size=1G -runtime=60 -name=fioTest";

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
        fioParallelTestResult.names = TestResult.FIO_PARALLEL_TEST;
        fioParallelTestResult.values = new String[]{iops, bw, lat};
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
    public void writeToFile(String resultPath) {

    }

    @Override
    public void readFromFile(String resultPath) {

    }
}

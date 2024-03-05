package backend.tester.fileSystem;

import backend.dataset.TestResult;
import backend.tester.TestItem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

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
        fioMiniFileTestResult.names = TestResult.FIO_MINIFILE_TEST;
        fioMiniFileTestResult.values = new String[]{iops, bw, lat};
    }

    @Override
    public List<List<Double>> getTimeData() {
        return null;
    }

    @Override
    public TestResult getTestResults() {
        return null;
    }

    @Override
    public void writeToFile() {

    }

    @Override
    public void readFromFile() {

    }
}

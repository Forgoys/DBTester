package backend.tester.fileSystem;

import backend.dataset.TestResult;
import backend.tester.TestItem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class IozoneReadWriteTest extends TestItem {

    // 执行参数 工具路径 测试文件名 文件块大小 文件大小
    private String toolDirectory;
    private String fileName;
    private String blockSize;
    private String fileSize;

    // 指令运行结果
    TestResult iozoneRWTestResult = new TestResult();

    // 安装iozone工具
    @Override
    public void testEnvPrepare() {

    }

    @Override
    public void startTest() throws IOException, InterruptedException {
        // 获取用户参数 定义为成员变量 默认已经存在
//        String toolDirectory="/home/wlx/zf/iozone";
//        String fileName = "/home/wlx/zf/data/testfile";
//        String blockSize = "4k";
//        String fileSize = "8G";

        System.out.println("Iozone读写速度测试开始");
        System.out.println("测试参数为:");
        System.out.println("测试工具目录:" + toolDirectory + " 测试文件名称:" + fileName + " 文件块大小:" + blockSize + " 文件大小:" + fileSize);

        // 设置 iozone 测试指令
        String iozoneCommand = "sudo " + toolDirectory + "iozone" + " -a -i 0 -i 1 -i 2 -f " + fileName + "/testfile -r " + blockSize + " -s " + fileSize + " -I";

        // 创建一个 ProcessBuilder 对象
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("bash", "-c", iozoneCommand);

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

        // 提取出结果并保存到TestResult
        iozoneResultSave(results);

        System.out.println("Iozone读写速度测试完成");
    }

    @Override
    public List<List<Double>> getTimeData() {
        return null;
    }

    private void iozoneResultSave(List<String> results) {
        String[] iozoneResult = new String[6];
        // 提取结果
        for (String result : results) {
            if (result.contains("write")) {
                iozoneResult[0] = result.split(":")[1].trim();
                System.out.println("Write: " + result.split(":")[1].trim());
            } else if (result.contains("rewrite")) {
                iozoneResult[1] = result.split(":")[1].trim();
                System.out.println("Rewrite: " + result.split(":")[1].trim());
            } else if (result.contains("read")) {
                iozoneResult[2] = result.split(":")[1].trim();
                System.out.println("Read: " + result.split(":")[1].trim());
            } else if (result.contains("reread")) {
                iozoneResult[3] = result.split(":")[1].trim();
                System.out.println("Reread: " + result.split(":")[1].trim());
            } else if (result.contains("read (backward)")) {
                iozoneResult[4] = result.split(":")[1].trim();
                System.out.println("Read (backward): " + result.split(":")[1].trim());
            } else if (result.contains("write (random)")) {
                iozoneResult[5] = result.split(":")[1].trim();
                System.out.println("Write (random): " + result.split(":")[1].trim());
            }
        }
        iozoneRWTestResult.names = TestResult.IOZONE_RW_TEST;
        iozoneRWTestResult.values = iozoneResult;
    }

    @Override
    public void generateTimeData() {

    }

    @Override
    public TestResult getTestResults() {
        return iozoneRWTestResult;
    }


    @Override
    public void writeToFile(String resultPath) {

    }

    @Override
    public void readFromFile(String resultPath) {

    }
}

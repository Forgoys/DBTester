package backend.tester.fileSystem;

import backend.dataset.TestAllResult;
import backend.dataset.TestResult;
import backend.dataset.TestTimeData;
import backend.tester.TestItem;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class ReliableTest extends TestItem {

    // 用户输入参数
    private String directory;
    private String timeChoose; // 4h 24h 7day
    private String fioReliableTestTime; // 对应的秒数
    private String reliableResultDirectory; // 结果存放目录
    private String reliableScriptName; // 脚本名称

    // 存放时序性数据
    private TestTimeData reliableTimeData = new TestTimeData();

    public ReliableTest(String directory, String timeChoose) {
        this.directory = directory;
        this.timeChoose = timeChoose;
        switch (timeChoose) {
            case "4h":
                fioReliableTestTime = "14400";
                break;
            case "24h":
                fioReliableTestTime = "86400";
                break;
            case "7day":
                fioReliableTestTime = "604800";
                break;
        }
        reliableResultDirectory = "fioReliableTestResult" + "_" + timeChoose;
        reliableScriptName = "fiotest" + "_" + timeChoose + ".sh";
    }

    // 安装fio
    @Override
    public void testEnvPrepare() throws Exception {

    }

    @Override
    public void startTest() throws IOException, InterruptedException {
        // 测试指令
        String reliableCommand = directory + "/" + reliableScriptName + " -resultDirectory=" + reliableResultDirectory;

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("bash", "-c", reliableCommand);

        // 启动进程 结果保存在reliableResultDirectory
        Process process = processBuilder.start();

//        // 获取进程的输出流
//        InputStream inputStream = process.getInputStream();
//        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
//
//        // 读取进程的输出
//        String line;
//        List<String> results = new ArrayList<>();
//        while ((line = reader.readLine()) != null) {
//            results.add(line);
//        }

        // 等待进程执行完毕
        int exitCode = process.waitFor();
        System.out.println("Exit code: " + exitCode);

        // 处理结果
        processReliableResult();
    }

    // 处理保存到文件夹的结果
    public void processReliableResult() throws InterruptedException, IOException {
        // 处理结果的python脚本
        String pythonScriptPath = directory + "/" + "processReliableResult.py";

        // 创建 ProcessBuilder 对象并设置要执行的命令
        ProcessBuilder pb = new ProcessBuilder("python", pythonScriptPath, directory);

        // 启动外部进程
        Process process = pb.start();

        // 读取 Python 脚本的输出结果
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            // 处理 Python 脚本的输出结果
            System.out.println(line);
        }

        // 等待外部进程执行完毕
        int exitCode = process.waitFor();
        System.out.println("可靠性数据处理完成，保存在结果fio_metrics_summary.csv" + exitCode);

        // 处理CSV的文件保存到时序数据里

        String csvFilename = "fio_metrics_summary.csv";
        List<List<String>> csvData = new ArrayList<>();
        try (Scanner scanner = new Scanner(new File(csvFilename))) {
            while (scanner.hasNextLine()) {
                line = scanner.nextLine();
                String[] values = line.split(",");
                csvData.add(Arrays.asList(values));
            }
        }

        // 转换下格式
        List<Double> IOPSList = new ArrayList<>();
        List<Double> BWList = new ArrayList<>();
        List<Double> LatList = new ArrayList<>();
        for (List<String> row : csvData) {
            IOPSList.add(Double.valueOf(row.get(0)));
            BWList.add(Double.valueOf(row.get(1)));
            LatList.add(Double.valueOf(row.get(2)));
        }
        List<List<Double>> reliableResult = new ArrayList<>();
        reliableResult.add(IOPSList);
        reliableResult.add(BWList);
        reliableResult.add(LatList);

        reliableTimeData.names = TestTimeData.FS_RELIABLE_TIMEDATA_NAMES;
        reliableTimeData.values = reliableResult;

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
    public void writeToFile(String resultPath) {

    }

    @Override
    public TestAllResult readFromFile(String resultPath) {

        return null;
    }
}

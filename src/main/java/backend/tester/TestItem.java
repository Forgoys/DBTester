package backend.tester;


import backend.dataset.TestArguments;
import backend.dataset.TestResult;
import backend.dataset.TestTimeData;
import frontend.connection.DBConnection;
import frontend.connection.SSHConnection;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 测试项父类，各测试项需要继承该类
 */
public abstract class TestItem implements Testable, Writable{
    /**
     * 测试项目名称：TPC-C，TPC-H等
     */
    protected String testName;

    /**
     * 软件相关工具、脚本所在的根目录
     * 此目录下可再分为三个子目录分别用于关系数据库、时序数据库和文件系统     *
     */
    protected String toolRootPath;

    public enum Status {
        /**
         * 准备阶段，例如参数未配置
          */
        UNPREPARED,
        /**
         * 就绪状态
         */
        READY,
        /**
         * 测试项正在运行
         */
        RUNNING,
        /**
         * 测试项已完成
         */
        FINISHED;
    }

    /**
     * 当前测试项的状态。你需要处理该状态的切换。
     */
    protected Status status = Status.UNPREPARED;

    /**
     * ssh远程连接句柄，通过该句柄与远程服务器进行交互。。直接使用MainAppController.sshConnection
     * 如果某项测试需要用到该句柄，则需在子类中初始化该句柄。
     */
//    protected SSHConnection sshStmt;

    /**
     * 数据库连接句柄，通过该句柄与远程数据库进行交互。直接使用MainAppController.dbConnection
     * 如果某项测试需要用到该句柄，则需在子类中初始化该句柄。
     */
    protected DBConnection DBStmt;

    /**
     * 测试参数
     */
    protected TestArguments testArgs;

    /**
     * 保存测试过程中的时序数据
     */
    protected List<List<Double>> timeDataList;

    /**
     * 保存测试结果
     */
    protected TestResult testResult;

    public TestItem(){}

    public TestItem(String testName, DBConnection DBStmt) {
        this.testName = testName;
        this.DBStmt = DBStmt;
    }

    public TestItem(String testName, DBConnection DBStmt, TestArguments testArgs) {
        this.testName = testName;
        this.DBStmt = DBStmt;
        this.testArgs = testArgs;
    }

    public static void execCommands(String... commands) {
        execCommands(null, commands);
    }

    public static String execCommandsWithReturn(String... commands) {
        return execCommandsWithReturn(null, commands);
    }

    public static void execCommands(File workDir, String... commands) {
        if(workDir != null && (!workDir.exists() || !workDir.isDirectory())) {
            System.out.println("工作路径设置不正确");
        }
        // 执行数据生成脚本
        try {
            // 构建进程
            ProcessBuilder builder = new ProcessBuilder();
            if(workDir != null)
                builder.directory(workDir);

            ArrayList<String> cmdList = new ArrayList<>();
            cmdList.add("bash");
            cmdList.add("-c");
            cmdList.addAll(Arrays.asList(commands));
            builder.command(cmdList);
            // 启动进程
            Process process = builder.start();
            // 读取进程输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            // 等待进程结束
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Command execution failed with error code " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void execCommandWithOutPrint(File workDir, String cmd, String args) {
        if(workDir != null && (!workDir.exists() || !workDir.isDirectory())) {
            System.out.println("工作路径设置不正确");
        }
        // 执行数据生成脚本
        try {
            // 构建进程
            ProcessBuilder builder = new ProcessBuilder();
            if(workDir != null)
                builder.directory(workDir);
            // 设置命令及参数
            List<String> command = new ArrayList<>();
            command.add(cmd);
            if(!args.isEmpty()) {
                args = args.trim();
                args = " " + args;
                command.add(args);
            }
            builder.command(command);
            // 启动进程
            Process process = builder.start();
            // 等待进程结束
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Command execution failed with error code " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static String execCommandsWithReturn(File workDir, String... commands) {
        if(workDir != null && (!workDir.exists() || !workDir.isDirectory())) {
            System.out.println("工作路径设置不正确");
        }
        StringBuilder output = new StringBuilder();
        // 执行数据生成脚本
        try {
            // 构建进程
            ProcessBuilder builder = new ProcessBuilder();
            if(workDir != null)
                builder.directory(workDir);

            ArrayList<String> cmdList = new ArrayList<>();
            cmdList.add("bash");
            cmdList.add("-c");
            cmdList.addAll(Arrays.asList(commands));
            builder.command(cmdList);
            // 启动进程
            Process process = builder.start();
            // 读取进程输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                output.append(line).append("\n");
            }
            // 等待进程结束
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Command execution failed with error code " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return output.toString().trim();
    }

}

package frontend.connection;

import javafx.scene.control.TextField;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class FSConnection {
    private String fsUrl; // 文件系统路径
    private String mountPath; // 挂载目录
    private boolean mountFlag = false; // 是否挂载成功

    private TextField fsServerPathTextField;
    private TextField fsMountPathTextField;
    private String fileSystemOption; // 挂载方式

    // sudo权限
    String localSudoPassword;

    public FSConnection() {
    }

    public FSConnection(String fileSystemOption, String localSudoPassword, String fsUrl, String mountPath) {
        this.fileSystemOption = fileSystemOption;
        this.localSudoPassword = localSudoPassword;
        this.fsUrl = fsUrl;
        this.mountPath = mountPath;
    }

    /**
     * 挂载文件系统
     */
    public String mountFS() {
        // 实现挂载文件系统的逻辑
        System.out.println("挂载文件系统...");
        List<String> mountResult = new ArrayList<>();

        // 根据之前设置的文件系统配置参数进行文件系统挂载
        try {
            // 获取参数
            String fsServerPath = fsUrl;
            String fsMountPath = mountPath;
            System.out.println("文件系统路径:" + fsServerPath);
            System.out.println("本地目录路径:" + fsMountPath);
            // 构建 mount 挂载命令
            String mountCommand = "mount -t " + fileSystemOption + " " + fsServerPath + " " + fsMountPath;
            mountCommand = "echo " + localSudoPassword + " | sudo -S " + mountCommand;

            System.out.println(mountCommand);

            // 创建一个 ProcessBuilder 对象
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("bash", "-c", mountCommand);
            // 启动进程
            Process process = processBuilder.start();
            // 获取进程的输出流
            InputStream inputStream = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            // 读取进程的输出
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                mountResult.add(line);
            }
            System.out.println("挂载输出：" + mountResult.toString());

            // 获取进程的错误流
            InputStream errorStream = process.getErrorStream();
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream));

            // 读取进程的错误输出
            while ((line = errorReader.readLine()) != null) {
                System.err.println(line);
            }

            // 等待进程执行完毕
            int exitCode = process.waitFor();
            System.out.println("Exit code: " + exitCode);

//            // 判断是否挂载成功
//            if (exitCode == 0) {
//                System.out.println("Mount successful");
//                mountFlag = true;
//            } else {
//                System.out.println("Mount failed");
//            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return mountResult.toString();
    }

    /**
     * 检查是否挂载
     *
     * @return
     */
    public boolean isMounted() {
        return mountFlag;
    }

    public String getFsUrl() {
        return fsUrl;
    }

    public void setFsUrl(String fsUrl) {
        this.fsUrl = fsUrl;
    }

    public String getMountPath() {
        return mountPath;
    }

    public void setMountPath(String mountPath) {
        this.mountPath = mountPath;
    }

    public static void main(String[] args) {
        FSConnection fSConnection = new FSConnection("glusterfs", "666", "10.181.8.145:/gv3", "/home/autotuning/zf/glusterfs/software_test/mountTest");
        fSConnection.mountFS();
    }
}

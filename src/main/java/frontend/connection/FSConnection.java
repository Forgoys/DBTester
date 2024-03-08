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
        // ?????????????
        System.out.println("Starting to mount filesystem...");
        StringBuilder outputResult = new StringBuilder();

        try {
            // ????????????????
            String fsServerPath = fsUrl;
            String fsMountPath = mountPath;
            System.out.println("Filesystem server path: " + fsServerPath);
            System.out.println("Filesystem mount path: " + fsMountPath);

            // ??????
            String mountCommand = "echo " + localSudoPassword + " | sudo -S mount -t " + fileSystemOption + " " + fsServerPath + " " + fsMountPath;

            System.out.println("Executing command: " + mountCommand);

            // ??ProcessBuilder??????
            ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", mountCommand);
            Process process = processBuilder.start();

            // ?????????
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    outputResult.append(line).append("\n");
                }
            }

            // ???????????
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    outputResult.append(line).append("\n");
                }
            }

            // ?????????????
            int exitCode = process.waitFor();
            System.out.println("Exit code: " + exitCode);

            // ?????????
            System.out.println("Mount operation result:\n" + outputResult.toString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            // ????????????
            outputResult.append("Exception occurred: ").append(e.getMessage());
        }
        return outputResult.toString();
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

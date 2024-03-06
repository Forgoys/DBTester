package frontend.connection;

import com.jcraft.jsch.*;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.Vector;

public class SSHConnection {
    private String ip;
    private int port;
    private String userName;
    private String password;
    private boolean status;
    private Session session;
    private ChannelShell shellChannel;
    private ChannelSftp sftpChannel;
    private OutputStream shellOutputStream;
    private InputStream shellInputStream;
    private PrintStream shellPrintStream;

    public SSHConnection(String ip, int port, String userName, String password) {
        this.ip = ip;
        this.port = port;
        this.userName = userName;
        this.password = password;
        this.status = false;
    }

    public boolean sshConnect() {
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(userName, ip, port);
            session.setPassword(password);

            // Avoiding the check for key verification of the SSH server.
            session.setConfig("StrictHostKeyChecking", "no");

            session.connect(3000); // Connection timeout.
            initShell();
            status = true;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            status = false;
            return false;
        }
    }

    public void sshDisconnect() {
        // 首先，检查shell通道是否打开，如果是，则关闭它
        if (shellChannel != null && shellChannel.isConnected()) {
            shellChannel.disconnect();
        }
        // 关闭sftp通道
        if (sftpChannel != null && sftpChannel.isConnected()) {
            sftpChannel.disconnect();
        }

        // 接下来，关闭输入输出流
        try {
            if (shellOutputStream != null) {
                shellOutputStream.close();
            }
            if (shellInputStream != null) {
                shellInputStream.close();
            }
            if (shellPrintStream != null) {
                shellPrintStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 最后，断开session连接
        if (session != null && session.isConnected()) {
            session.disconnect();
        }

        // 更新状态标志
        status = false;
    }


    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public void initShell() throws Exception {
        if (session == null || !session.isConnected()) {
            throw new IllegalStateException("SSH session is not connected. Please connect first.");
        }

        shellChannel = (ChannelShell) session.openChannel("shell");
        shellOutputStream = shellChannel.getOutputStream();
        shellInputStream = shellChannel.getInputStream();
        shellPrintStream = new PrintStream(shellOutputStream, true);
        shellChannel.connect();

        // 连接sftp通道
        sftpChannel = (ChannelSftp) session.openChannel("sftp");
        sftpChannel.connect();
    }

    public String executeCommand(String command) throws Exception {
        if (shellChannel == null || !shellChannel.isConnected()) {
            throw new IllegalStateException("Shell channel is not connected. Please call initShell first.");
        }

        // 发送命令
        shellPrintStream.println(command);
        shellPrintStream.flush();

        StringBuilder output = new StringBuilder();
        byte[] tmp = new byte[1024];

        // 设置读取超时时间，例如10秒
        int timeout = 10000; // 10秒
        long startTime = System.currentTimeMillis();

        while (true) {
            // 如果有可用数据，则读取数据
            while (shellInputStream.available() > 0) {
                int i = shellInputStream.read(tmp, 0, 1024);
                if (i < 0) break;
                output.append(new String(tmp, 0, i));
            }

            // 如果当前时间超过了开始时间加上超时时间，结束循环
            if (System.currentTimeMillis() > startTime + timeout) {
                break;
            }

            // 检查命令是否执行完成的逻辑（这里需要根据实际情况来定）
            // 例如，可以检查output中是否包含特定的结束标志，或者简单地等待一段时间让命令有足够的执行时间
            if (shellInputStream.available() <= 0) {
                Thread.sleep(100); // 短暂等待，避免CPU过度使用
                if (shellInputStream.available() <= 0) {
                    break; // 如果再次检查还是没有数据，认为命令执行完成
                }
            }
        }

        return output.toString();
    }

    public String executeCommand(String command, boolean useSudo) throws Exception {
        if (shellChannel == null || !shellChannel.isConnected()) {
            throw new IllegalStateException("Shell channel is not connected. Please call initShell first.");
        }

        if (useSudo) {
            command = "echo " + password + " | sudo -S " + command;
        }

        // 发送命令
        shellPrintStream.println(command);
        shellPrintStream.flush();

        StringBuilder output = new StringBuilder();
        byte[] tmp = new byte[1024];

        // 设置读取超时时间，例如10秒
        int timeout = 10000; // 10秒
        long startTime = System.currentTimeMillis();

        while (true) {
            // 如果有可用数据，则读取数据
            while (shellInputStream.available() > 0) {
                int i = shellInputStream.read(tmp, 0, 1024);
                if (i < 0) break;
                output.append(new String(tmp, 0, i));
            }

            // 如果当前时间超过了开始时间加上超时时间，结束循环
            if (System.currentTimeMillis() > startTime + timeout) {
                break;
            }

            // 检查命令是否执行完成的逻辑（这里需要根据实际情况来定）
            // 例如，可以检查output中是否包含特定的结束标志，或者简单地等待一段时间让命令有足够的执行时间
            if (shellInputStream.available() <= 0) {
                Thread.sleep(100); // 短暂等待，避免CPU过度使用
                if (shellInputStream.available() <= 0) {
                    break; // 如果再次检查还是没有数据，认为命令执行完成
                }
            }
        }
        return output.toString();
    }

    // Method to execute a shell script located at a given path on the remote machine
    public String executeShellScript(String scriptPath) throws Exception {
        return executeCommand("bash " + scriptPath);
    }

    public String executeShellScript(String scriptPath, boolean useSudo) throws Exception {
        return executeCommand("bash " + scriptPath, useSudo);
    }

    /**
     * 从服务器下载指定路径的文件到本地目录下
     * @param remoteFilePath
     * @param localDirPath
     * @throws SftpException
     */
    public void downloadFile(String remoteFilePath, String localDirPath) throws SftpException {
        File localDir = new File(localDirPath);
        if (!localDir.exists()) {
            localDir.mkdirs();
        }
        sftpChannel.get(remoteFilePath, localDirPath + File.separator + new File(remoteFilePath).getName());
    }

    /**
     * 将服务器上指定目录下载到本地指定目录下
     * @param remoteDirPath
     * @param localDirPath
     * @throws SftpException
     */
    public void downloadDirectory(String remoteDirPath, String localDirPath) throws SftpException {
        String remoteDirName = Paths.get(remoteDirPath).getFileName().toString();
        File localDir = new File(localDirPath, remoteDirName);
        if(!localDir.exists()) {
            localDir.mkdirs();
        }
        downloadDirectoryRecursively(remoteDirPath, localDir.getAbsolutePath());
    }

    private void downloadDirectoryRecursively(String remoteDirPath, String localDirPath) throws SftpException {
        File localDir = new File(localDirPath);
        if (!localDir.exists()) {
            localDir.mkdirs();
        }
        @SuppressWarnings("unchecked")
        Vector<ChannelSftp.LsEntry> fileList = sftpChannel.ls(remoteDirPath);
        for (ChannelSftp.LsEntry entry : fileList) {
            String fileName = entry.getFilename();
            if (!fileName.equals(".") && !fileName.equals("..")) {
                if (entry.getAttrs().isDir()) {
                    downloadDirectoryRecursively(remoteDirPath + "/" + fileName, localDirPath + File.separator + fileName);
                } else {
                    sftpChannel.get(remoteDirPath + "/" + fileName, localDirPath + File.separator + fileName);
                }
            }
        }
    }

    /**
     * 上传本地文件到服务器指定路径下
     * @param localFilePath
     * @param remoteDirPath
     * @throws SftpException
     */
    public void uploadFile(String localFilePath, String remoteDirPath) throws SftpException {
        File localFile = new File(localFilePath);
        if (!localFile.exists() || !localFile.isFile()) {
            System.out.println("Local file does not exist: " + localFilePath);
            return;
        }
        sftpChannel.put(localFilePath, remoteDirPath + "/" + localFile.getName());
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean getStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }
}

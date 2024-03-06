package frontend.connection;

import com.jcraft.jsch.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SSHConnectionTest {

    private Session session;
    private ChannelShell channel;
    private String host;
    private int port;
    private String user;
    private String password;

    public SSHConnectionTest(String host, int port, String user, String password) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
    }

    public boolean connect() {
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(user, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(30000);   // making a connection with timeout.

            channel = (ChannelShell) session.openChannel("shell");
            channel.connect(3000);
            return true;
        } catch (JSchException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String executeCommand(String command) {
        if (session == null || channel == null || !session.isConnected() || !channel.isConnected()) {
            throw new IllegalStateException("SSH Connection not established.");
        }

        try {
            InputStream in = channel.getInputStream();
            OutputStream out = channel.getOutputStream();

            out.write((command + "\n").getBytes());
            out.flush();

            byte[] tmp = new byte[1024];
            StringBuilder outputBuffer = new StringBuilder();
            int waitMillis = 1000; // 设置等待超时时间
            long timeout = System.currentTimeMillis() + waitMillis;
            while (System.currentTimeMillis() < timeout) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) break;
                    outputBuffer.append(new String(tmp, 0, i));
                    timeout = System.currentTimeMillis() + waitMillis; // 重置超时时间
                }
                if (channel.isClosed()) {
                    if (in.available() > 0) continue;
                    break;
                }
                try {
                    Thread.sleep(100); // 短暂休眠以减少CPU负载
                } catch (Exception ee) {
                    Thread.currentThread().interrupt();
                }
            }
            return outputBuffer.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    public boolean isConnected() {
        return session != null && session.isConnected();
    }

    public void disconnect() {
        if (channel != null) {
            channel.disconnect();
        }
        if (session != null) {
            session.disconnect();
        }
    }

    // Usage example
    public static void main(String[] args) {
        SSHConnectionTest ssh = new SSHConnectionTest("10.181.8.216", 22, "wlx", "Admin@wlx");
        if (ssh.connect()) {
            System.out.println("Connected successfully.");
            String output = ssh.executeCommand("ls");
            System.out.println(output);
            ssh.disconnect();
        } else {
            System.out.println("Connection failed.");
        }
    }
}
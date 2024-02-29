package org.example.dbtester;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import java.io.InputStream;

public class SSHConnection {
    private String ip;
    private int port;
    private String userName;
    private String password;
    private boolean status;
    private Session session;

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

            session.connect(30000); // Connection timeout.
            status = true;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            status = false;
            return false;
        }
    }

    public void sshDisconnect() {
        if (session != null && session.isConnected()) {
            session.disconnect();
            status = false;
        }
    }

    public Session getSession() {
        return session;
    }

    public String executeCommand(String command) {
        if (session == null || !session.isConnected()) {
            throw new IllegalStateException("SSH session is not connected. Please connect first.");
        }

        StringBuilder output = new StringBuilder();
        try {
            Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);

            channel.setInputStream(null);
            ((ChannelExec) channel).setErrStream(System.err);

            InputStream in = channel.getInputStream();
            channel.connect();

            byte[] tmp = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) break;
                    output.append(new String(tmp, 0, i));
                }
                if (channel.isClosed()) {
                    if (in.available() > 0) continue;
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
            channel.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return output.toString();
    }

    // Method to execute a shell script located at a given path on the remote machine
    public String executeShellScript(String scriptPath) {
        return executeCommand("bash " + scriptPath);
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

    public void setSession(Session session) {
        this.session = session;
    }
}

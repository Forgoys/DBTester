package frontend.connection;

public class FSConnection {
    private String fsUrl;
    private String mountPath;

    public FSConnection() {}

    public FSConnection(String fsUrl, String mountPath) {
        this.fsUrl = fsUrl;
        this.mountPath = mountPath;
    }

    /**
     * 挂载文件系统
     */
    public void mountFS() {

    }

    /**
     * 检查是否挂载
     * @return
     */
    public boolean isMounted() {
        return true;
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
}

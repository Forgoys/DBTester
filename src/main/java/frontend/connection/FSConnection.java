package frontend.connection;

public class FSConnection {
    private String fsUrl;
    private String mountPath;

    public FSConnection() {}

    public FSConnection(String fsUrl, String mountPath) {
        this.fsUrl = fsUrl;
        this.mountPath = mountPath;
    }

    public void mountFS() {

    }

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

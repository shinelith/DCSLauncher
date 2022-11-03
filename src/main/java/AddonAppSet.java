import org.ini4j.Wini;

import java.io.IOException;

public class AddonAppSet {
    public static int RUNNING = 1;
    public static int NO_RUNNING = -1;

    public interface onDataChangeCallback {
        void dataChanged(AddonAppSet appset);
    }

    private Wini cfg;
    private onDataChangeCallback onDataChangeCallback;
    private String id;
    private boolean isEnable;
    private String path;
    private int runStatus = NO_RUNNING;


    public boolean isEnable() {
        return isEnable;
    }

    public void setEnable(boolean enable) {
        isEnable = enable;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getRunStatus() {
        return runStatus;
    }

    public void setCfg(Wini cfg) {
        this.cfg = cfg;
    }

    public void setOnDataChangeCallback(AddonAppSet.onDataChangeCallback onDataChangeCallback) {
        this.onDataChangeCallback = onDataChangeCallback;
    }

    public void update() {
        if (this.cfg != null) {
            this.cfg.put(getId(), "path", path);
            this.cfg.put(getId(), "enable", isEnable);
            try {
                this.cfg.store();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (this.onDataChangeCallback != null) {
            onDataChangeCallback.dataChanged(this);
        }
    }

    public void clear() {
        this.isEnable = false;
        this.path = "";
        this.runStatus = NO_RUNNING;
        update();
    }

    public void updateRunningStatus(boolean running) {
        int status = running ? RUNNING : NO_RUNNING;
        if (status != this.runStatus) {
            this.runStatus = status;
            update();
        }
    }

    public boolean isEmpty() {
        if (path == null || "".equals(path)) {
            return true;
        } else {
            return false;
        }
    }
}

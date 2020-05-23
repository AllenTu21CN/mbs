package cn.lx.mbs.impl.structures;

import java.util.List;

public class StorageState {

    private boolean isMount;
    private List<String> paths;

    public StorageState() {
    }

    public StorageState(boolean isMount, List<String> paths) {
        this.isMount = isMount;
        this.paths = paths;
    }

    public boolean isMount() {
        return isMount;
    }

    public void setMount(boolean mount) {
        isMount = mount;
    }

    public List<String> getPaths() {
        return paths;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }
}

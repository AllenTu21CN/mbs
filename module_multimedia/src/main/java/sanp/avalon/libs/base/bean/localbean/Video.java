package sanp.avalon.libs.base.bean.localbean;

import java.util.List;

/**
 * Created by Vald on 2017/6/6.
 */

public class Video {
    private String puttype;             //input输入output输出
    private String origin;              //来源
    private List<String> role;                //设置角色
    private String resolution;          //分辨率
    private String coderate;            //码率

    public String getPuttype() {
        return puttype;
    }

    public void setPuttype(String puttype) {
        this.puttype = puttype;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public List<String> getRole() {
        return role;
    }

    public void setRole(List<String> role) {
        this.role = role;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getCoderate() {
        return coderate;
    }

    public void setCoderate(String coderate) {
        this.coderate = coderate;
    }
}

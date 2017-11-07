package sanp.avalon.libs.base.bean.localbean;

/**
 * Created by Vald on 2017/6/6.
 */

public class Record {
    private String status;              //录制状态
    private String lessonmode;          //课堂模式
    private String lessonname;          //课堂模式
    private String lessonteacher;       //上课老师
    private String time;                //录制时长
    private String address;             //直播地址

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLessonmode() {
        return lessonmode;
    }

    public void setLessonmode(String lessonmode) {
        this.lessonmode = lessonmode;
    }

    public String getLessonname() {
        return lessonname;
    }

    public void setLessonname(String lessonname) {
        this.lessonname = lessonname;
    }

    public String getLessonteacher() {
        return lessonteacher;
    }

    public void setLessonteacher(String lessonteacher) {
        this.lessonteacher = lessonteacher;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

}

package sanp.tools.db;

/**
 * Created by Tom on 2017/3/28.
 */

public final class SettingsInfo {
    public final static String CAMERA_INNER_ROLE = "camera.built-in.0.role";  //内置摄像机角色
    public final static String CAMERA_INNER_FILL = "camera.built-in.0.fill-mode";  //内置摄像机填充模式
    public final static String CAMERA_1ST_IP = "camera.ipc.0.address";  //第一路网络摄像机IP
    public final static String CAMERA_1ST_PORT = "camera.ipc.0.port";  //第一路网络摄像机访问端口
    public final static String CAMERA_1ST_USERNAME = "camera.ipc.0.username";  //第一路网络摄像机访问的用户名
    public final static String CAMERA_1ST_USERPASSWORD = "camera.ipc.0.password";  //第一路网络摄像机访问的密码
    public final static String CAMERA_1ST_ROLE = "camera.ipc.0.password";  //第一路网络摄像机角色
    public final static String CAMERA_1ST_FILL = "camera.ipc.0.fill-mode";  //第一路网络摄像机填充模式
    public final static String CAMERA_2ST_IP = "camera.ipc.1.address";  //第二路网络摄像机IP
    public final static String CAMERA_2ST_PORT = "camera.ipc.1.port";  //第二路网络摄像机访问端口
    public final static String CAMERA_2ST_USERNAME = "camera.ipc.1.username";  //第二路网络摄像机访问的用户名
    public final static String CAMERA_2ST_USERPASSWORD = "camera.ipc.1.password";  //第二路网络摄像机访问的密码
    public final static String CAMERA_2ST_ROLE = "camera.ipc.1.role";  //第二路网络摄像机角色
    public final static String CAMERA_2ST_FILL = "camera.ipc.1.fill-mode";  //第二路网络摄像机填充模式

    public final static String TRACKSERVER_ENABLE = "tracking.server.enabled";  //启用独立跟踪服务器
    public final static String TRACKSERVER_ADDRESS = "tracking.server.address";  //跟踪服务器地址

    public final static String MIC_INNER_ENABLE = "microphone.built-in.enabled";  //内置麦克风的开关
    public final static String MIC_INNER_GAIN = "microphone.built-in.gain";  //内置麦克风输入增益大小
    public final static String MIC_INNER_SCENE = "microphone.built-in.scene";  //内置麦克风场景选择
    public final static String LINE_IN_ENABLE = "line-in.enabled";  //3.5mm音频输入
    public final static String LINE_IN_GAIN = "line-in.gain";  //3.5mm音频输入增益大小
    public final static String LINE_IN_EQ = "line-in.eq";  //3.5mm输入EQ?

    public final static String REC_RESOLUTION = "record.video.resolution";  //录制分辨率
    public final static String REC_FRAME_RATE = "record.video.frame-rate";  //录制帧率
    public final static String REC_BIT_RATE = "record.video.bit-rate";  //录制码率

    public final static String INSPECT_RESOLUTION = "inspect.video.resolution";  //观摩流分辨率
    public final static String INSPECT_FRAME_RATE = "inspect.video.frame-rate";  //观摩流帧率
    public final static String INSPECT_BIT_RATE = "inspect.video.bit-rate";  //观摩流码率
    public final static String INSPECT_ENABLE = "inspect.enabled";  //观摩流开关
    public final static String INSPECT_URL = "";  //观摩流URL

    public final static String LESSON_UUID = "tscloud.platform.lesson.uuid";  //课程号
    public final static String LESSON_REC_ENABLE = "tscloud.platform.rec.enable";  //录制状态
    public final static String LESSON_REC_DURATION = "tscloud.platform.rec.duration";  //录制时长
    public final static String LESSON_SUBJECT = "tscloud.platform.lesson.subject.name";  //开课科目名
    public final static String LESSON_LESSONNAME = "tscloud.platform.lesson.name";  //开课课程名
    public final static String LESSON_TEACHERNAME = "tscloud.platform.lesson.teacher.name";  //开课老师名
    public final static String LESSON_SPLIT = "tscloud.platform.lesson.split";  //开课分屏模式
    public final static String LESSON_SCHOOLNAME = "tscloud.platform.lesson.school.name";  //开课学校名称
    public final static String LESSON_GRADE = "tscloud.platform.lesson.grade.name";  //开课年级名称
    public final static String LESSON_CLASSROOMNAME = "tscloud.platform.lesson.classroom.name";  //开课教室名称
    public final static String LESSON_SUBJECT_ENABLE = "tscloud.platform.lesson.subtitle.enable";  //字幕开关
    public final static String LESSON_SUBJECT_SCHOOL = "tscloud.platform.lesson.subtitle.school";  //学校字幕
    public final static String LESSON_SUBJECT_TEACHER = "tscloud.platform.lesson.subtitle.teacher";  //老师字幕
    public final static String LESSON_SUBJECT_LESSON = "tscloud.platform.lesson.subtitle.lesson";  //课程字幕
    public final static String LESSON_USER_NAME = "tscloud.platform.user.name";  //登录老师名称
    public final static String LESSON_USER_PASSWORD = "tscloud.platform.user.password";  //登录老师密码
    public final static String LESSON_DEFAULT_SUBJECT = "tscloud.platform.default-lesson.subject.name";  //登录开课的科目
    public final static String LESSON_DEFAULT_LESSON = "tscloud.platform.default-lesson.name";  //登录开课的课程名
}

package sanp.avalon.libs.base.utils;

public enum BblError {
	e33001("server has error,apiException","api服务错误"),
	e33002("server has error,paramsError","调用参数错误"),
	e33003("server has error,operateError","权限不存在"),
	e33101("lesson is null","课程不存在"),
	e33102("lesson status is error","课程状态错误"),
	e33103("lesson status update fail","课程状态更新错误"),
	e33201("task is null","录制任务不存在"),
	e33202("task's stream is null","录制任务流信息不存在"),
	e33203("task's url is null","录制任务流地址不存在"),
	e33204("auto_segment has error","后台系统参数auto_segment配置有误"),
	e33205("fRecordTask is running","录制任务正在运行"),
	e33206("insert fRecordTask has error","插入录制任务记录失败"),
	e33207("creating RecordingJob has error","调用录制任务创建服务失败"),
	e33208("creating stream has error","创建流地址失败"),
	e33209("creating program has error","创建节目失败"),
	e33210("please wait a second","录制任务状态待更新，稍等一会儿"),
	e33211("media service has error","调用录制任务有误"),
	e33212("job status has error","录制任务状态有误，请刷新当前状态"),
	e33213("username or password error","用户名或密码错误"),
	e33214("registerid is error","平台不存在此注册文件");
	String msg;
	String zh_cnmsg;

	private BblError(String msg, String zh_cnmsg) {
		// TODO Auto-generated constructor stub
		this.msg = msg;
		this.zh_cnmsg = zh_cnmsg;
	}

	private BblError() {

	}

	public String zh_cnmsg() {
		return zh_cnmsg;
	};

	public String msg() {
		return msg;
	};
}

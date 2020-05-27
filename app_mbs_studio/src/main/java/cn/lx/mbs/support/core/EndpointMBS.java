package cn.lx.mbs.support.core;

/*
* EndpointMBS: 因为EP3是更灵活的通用SDK，为降低应用层使用EP的复杂度,封装了EndpointMBS
* 功能描述:
*   . 根据MBS的业务场景，支持: 视频源角色管理、直播录制、单点呼叫、多点会议
*   . 封装了内部线程、配置读写、状态维护
*   . 除了初始化类接口和查询类接口, 其他操作类接口为非阻塞异步调用,且线程安全
*   . 没有事件概念
* */
public class EndpointMBS {
    private static final String TAG = EndpointMBS.class.getSimpleName();
}

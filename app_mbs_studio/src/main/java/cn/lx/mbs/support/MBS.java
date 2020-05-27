package cn.lx.mbs.support;

import android.content.Context;
import android.view.Surface;

import java.util.List;
import java.util.Map;

import com.sanbu.base.Callback;

import cn.lx.mbs.support.structures.LayoutConfig;
import cn.lx.mbs.support.structures.SceneConfig;
import cn.sanbu.avalon.endpoint3.structures.jni.AudioFormat;
import cn.sanbu.avalon.endpoint3.structures.jni.DataType;
import cn.sanbu.avalon.endpoint3.structures.jni.TransitionDesc;
import cn.sanbu.avalon.endpoint3.structures.jni.VideoFormat;

import cn.lx.mbs.support.structures.AudioMode;
import cn.lx.mbs.support.structures.Input;
import cn.lx.mbs.support.structures.DisplayId;
import cn.lx.mbs.support.structures.RecProp;
import cn.lx.mbs.support.structures.Source;
import cn.lx.mbs.support.structures.ChannelId;

// MBS后台功能入口
public class MBS {

    private static final String TAG = MBS.class.getSimpleName();

    // @brief UI层需要实现的回调
    public interface Observer {
        // @brief 输入流状态变化
        void onStreamStateChanged(ChannelId channelId, DataType streamType, boolean ready);

        // @brief 输入/输出通道音量统计
        void onInputVolumeStatistic(ChannelId channelId, float left, float right);
    }

    public static void initEnv(Context context) {

    }

    private static MBS sInstance = new MBS();

    public static MBS getInstance() {
        return sInstance;
    }

    private MBS() {

    }

    public int init(Context context) {
        return 0;
    }

    public void release() {

    }

    /////////////////////////////// 基础功能

    // TODO: 基本信息/版本/授权/日志/应用数据

    /////////////////////////////// 信号源配置

    // @brief 获取可用信号源
    public List<Source> getAvailableSources() {
        return null;
    }

    // @brief 添加信号源
    public void addSource(Source source, Callback/*int sourceId*/ callback) {
    }

    // @brief 删除信号源(下次生效)
    public void removeSource(int sourceId, Callback callback) {
    }

    // @brief 修改信号源(下次生效)
    public void updateSource(Source source, Callback callback) {
    }

    /////////////////////////////// 编码参数配置

    // @brief 获取音频编码参数
    public AudioFormat getSRAudioFormat() {
        return null;
    }

    // @brief 设置音频编码参数(下次生效)
    public void setSRAudioFormat(AudioFormat format, Callback callback) {
    }

    // @brief 获取推流视频编码参数
    public VideoFormat getStreamingVideoFormat() {
        return null;
    }

    // @brief 设置推流视频编码参数(下次生效)
    public void setStreamingVideoFormat(VideoFormat format, Callback callback) {
    }

    // @brief 获取录制视频编码参数
    public VideoFormat getRecordingVideoFormat() {
        return null;
    }

    // @brief 设置录制视频编码参数(下次生效)
    public void setRecordingVideoFormat(VideoFormat format, Callback callback) {
    }

    /////////////////////////////// 推流/录制配置

    // @brief 获取推流地址
    public String getStreamingUrl() {
        return null;
    }

    // @brief 设置推流地址(下次生效)
    public void setStreamingUrl(String url, Callback callback) {
    }

    // @brief 获取录制属性
    public RecProp getRecProp() {
        return null;
    }

    // @brief 设置录制属性(下次生效)
    public void setRecProp(RecProp file, Callback callback) {
    }

    /////////////////////////////// UI层显示设置

    // @brief 本地显示创建时回调
    public void onDisplayCreated(DisplayId id, Surface handle) {
    }

    // @brief 本地显示格式改变时回调
    public void onDisplayChanged(DisplayId id, int format, int width, int height) {
    }

    // @brief 本地显示销毁时回调
    public void onDisplayDestroyed(DisplayId id) {
    }

    /////////////////////////////// 输入管理

    // 备注:
    // CUT: 跳过PVW直接将输入全屏显示到输出,且无动画
    // PAUSE: 暂停输入通道的画面(采集源冻结最后一帧;解码源继续解码,但Scene冻结最好一帧)
    // CTRL: 输入源的控制操作,如：IPC的云台控制、Camera的相机控制
    // EDIT: 待定

    // @brief 获取已加载的输入通道
    public List<Input> getLoadedInputs() {
        return null;
    }

    // @brief 加载输入通道
    public void loadInput(ChannelId id, int sourceId, Callback callback) {
    }

    // @brief 卸载输入通道
    public void unloadInput(ChannelId id, Callback callback) {
    }

    // @brief CUT输入通道(视频)
    public void cutInputVideo(ChannelId id, Callback callback) {
    }

    // @brief 暂停输入通道(视频)
    public void pauseInputVideo(ChannelId id, Callback callback) {
    }

    /////////////////////////////// 音频设置

    // 备注: 音频存在N个(1~5)输入通道,1个输出通道,1个监听通道;
    // OFF/ON/AFV开关仅针对输出通道; SOLO开关仅针对监听通道;
    // 输入通道的音量调节是针对输入本身,实际会改变输出和监听通道中的音量

    // @brief 获取音频指定通道的音量
    public float getVolume(ChannelId id) {
        return 0;
    }

    // @brief 设置音频指定通道的音量
    public void setVolume(ChannelId id, float volume, Callback callback) {
    }

    // @brief 获取音频输出的通道状态
    public Map<ChannelId, AudioMode> getAudioOutputs() {
        return null;
    }

    // @brief 设置指定音频通道的输出模式
    public void setAudioOutputMode(ChannelId id, AudioMode mode, Callback callback) {
    }

    // @brief 获取音频监听通道的状态
    public Map<ChannelId, Boolean> getAudioSolos() {
        return null;
    }

    // @brief 切换指定音频通道是否被监听
    public void switchAudioSolo(ChannelId id, boolean onOff, Callback callback) {
    }

    /////////////////////////////// 推流/录制控制

    // @brief 是否在推流
    public boolean inStreaming() {
        return false;
    }

    // @brief 开关推流
    public void switchStreaming(boolean onOff, Callback callback) {
    }

    // @brief 是否在录制
    public boolean inRecording() {
        return false;
    }

    // @brief 开关录制
    public void switchRecording(boolean onOff, Callback callback) {
    }

    /////////////////////////////// 显示切换

    // @brief 获取当前场景号
    public int getCurrentSceneId() {
        return 0;
    }

    // @brief 保存当前场景号
    public void saveCurrentSceneId(int sceneId, Callback callback) {
    }

    // @brief 获取指定场景配置
    public SceneConfig getSceneConfig(int sceneId) {
        return null;
    }

    // @brief 保存指定场景配置
    public void saveSceneConfig(int sceneId, SceneConfig config, Callback callback) {
    }

    // @brief 设置预览画面
    public void setPVWLayout(LayoutConfig config, Callback callback) {
    }

    // @brief 切换预览画面到输出画面
    public void switchPVW2PGM(TransitionDesc transition, Callback callback) {
    }

    /////////////////////////////// 内部方法

    public void onNetworkChanged() {
    }
}

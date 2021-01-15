package cn.lx.mbs.support.utils;

import com.google.gson.reflect.TypeToken;
import com.sanbu.media.AudioFormat;
import com.sanbu.media.VideoFormat;
import com.sanbu.tools.SPUtil;

import java.util.LinkedList;
import java.util.List;

import cn.lx.mbs.LXConst;
import cn.lx.mbs.support.structures.RecProp;
import cn.lx.mbs.support.structures.SRId;
import cn.lx.mbs.support.structures.Source;
import cn.sanbu.avalon.endpoint3.structures.jni.EPFixedConfig;

public class SPHelper {

    private SPUtil mCommSP;
    private SPUtil mPerSP;

    public SPHelper() {
        mCommSP = new SPUtil(LXConst.SP_COMMON_NAMESPACE);
        mPerSP = new SPUtil(LXConst.SP_PERSONALIZED_NAMESPACE);
    }

    public void release() {
        mCommSP = null;
        mPerSP = null;
    }

    public SPUtil getCommon() {
        return mCommSP;
    }

    public SPUtil getPersonalized() {
        return mPerSP;
    }

    /////////////////////////////// 基础功能

    /////////////////////////////// EP配置

    public EPFixedConfig getEPFixedConfig() {
        return new EPFixedConfig(LXConst.DEFAULT_EP_FIXED_CONFIG);
    }

    /////////////////////////////// 信号源配置

    public List<Source> getAvailableSources() {
        return mPerSP.getObject(LXConst.SP_KEY_CORE_SOURCES,
                new TypeToken<List<Source>>() {
                }.getType(),
                new LinkedList<>());
    }

    public void setAvailableSources(List<Source> sources) {
        mPerSP.putObject(LXConst.SP_KEY_CORE_SOURCES, sources);
    }

    /////////////////////////////// 推流录制参数配置

    public AudioFormat getSRAudioFormat() {
        return mCommSP.getObject(LXConst.SP_KEY_CORE_SR_AUDIO_FORMAT, AudioFormat.class,
                new AudioFormat(LXConst.DEFAULT_SR_AUDIO_FORMAT));
    }

    public void setSRAudioFormat(AudioFormat format) {
        mCommSP.putObject(LXConst.SP_KEY_CORE_SR_AUDIO_FORMAT, format);
    }

    public VideoFormat getSRVideoFormat(SRId id) {
        if (id == SRId.Streaming) {
            return mCommSP.getObject(LXConst.SP_KEY_CORE_SR_S_VIDEO_FORMAT, VideoFormat.class,
                    new VideoFormat(LXConst.DEFAULT_SR_S_VIDEO_FORMAT));
        } else if (id == SRId.Recording) {
            return mCommSP.getObject(LXConst.SP_KEY_CORE_SR_R_VIDEO_FORMAT, VideoFormat.class,
                    new VideoFormat(LXConst.DEFAULT_SR_R_VIDEO_FORMAT));
        } else {
            return null;
        }
    }

    public void setSRVideoFormat(SRId id, VideoFormat format) {
        if (id == SRId.Streaming) {
            mCommSP.putObject(LXConst.SP_KEY_CORE_SR_S_VIDEO_FORMAT, format);
        } else if (id == SRId.Recording) {
            mCommSP.putObject(LXConst.SP_KEY_CORE_SR_R_VIDEO_FORMAT, format);
        }
    }

    public List<String> getStreamingUrls() {
        return mPerSP.getObject(LXConst.SP_KEY_CORE_SR_S_URLS,
                new TypeToken<List<String>>() {
                }.getType(),
                new LinkedList<>());
    }

    public void setStreamingUrls(List<String> urls) {
        mPerSP.putObject(LXConst.SP_KEY_CORE_SR_S_URLS, urls);
    }

    public RecProp getRecProp() {
        return mCommSP.getObject(LXConst.SP_KEY_CORE_SR_R_PROP, RecProp.class,
                new RecProp(LXConst.DEFAULT_SR_REC_PROP));
    }

    public void setRecProp(RecProp file) {
        mCommSP.putObject(LXConst.SP_KEY_CORE_SR_R_PROP, file);
    }
}

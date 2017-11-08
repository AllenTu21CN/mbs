//
// Created by Tuyj on 2017/3/16.
//

#include <jni.h>
#include <memory>
#include <string>
#include <map>

#include "sanp_javalon_network_protocol_RTSPClient.h"
#include "base/log.h"
#include "base/io/io.h"
#include "base/debug.h"
#include "media/base/common.h"

//#define DEBUG_H264_VIDEO
#if defined(DEBUG_H264_VIDEO)
BASE_DEBUG_INIT("jni-rtsp-h264");
static uint8_t nalSep[4] = { 0, 0, 0, 1 };
#endif

#define USING_RTSP_CLIENT

#ifdef USING_RTSP_CLIENT
#include "network/protocol/rtsp_client/rtsp_pull_client.h"
#else
#include "network/protocol/rtsp/RTSPClient.h"
#endif


using namespace network;

#define TRANSPORT_START_BASE_PORT 12000

#ifdef USING_RTSP_CLIENT
class JniRTSPClient: public RTSPPullClient::Callback
#else
class JniRTSPClient: public RTSPClient::Callbacker
#endif
{
public:
    typedef enum {
        /*
        RTSP_EVT_CONNECTED    = 0,
        RTSP_EVT_DISCONNECTED = 1,
        RTSP_EVT_PLAYED       = 2,
        */
        RTSP_EVT_CONNECTION_BROKEN = 3,
    } JNI_EVENT_ID;

    JniRTSPClient():
        m_jvm(NULL),m_jobj(NULL),
        m_method_id_on_event(NULL),
        m_method_id_on_incoming_frame(NULL),
        m_waiting_keyframe(true),m_client(NULL)
#ifndef USING_RTSP_CLIENT
        ,m_rtsp_port(0),m_transport_start_port(TRANSPORT_START_BASE_PORT)
#endif
    {
    }

    ~JniRTSPClient() {
        deinit();
    }

    int init(JNIEnv *env, jobject obj) {
        int rc = env->GetJavaVM(&m_jvm);
        if(m_jvm == NULL) {
            base::_error("get JavaVM from JavaEnv failed: %d", rc);
            return -1;
        }
        m_jobj = env->NewGlobalRef(obj);

        // Save references for callback
        jclass jclazz = env->GetObjectClass(m_jobj);
        if (jclazz == NULL) {
            base::_error("Failed to find class");
            return -1;
        }
        // Get callback method id
        m_method_id_on_event = env->GetMethodID(jclazz, "onEvent", "(II)V");
        if (m_method_id_on_event == NULL) {
            base::_error("Unable to get method `onEvent` reference.");
            return -1;
        }
        m_method_id_on_incoming_frame = env->GetMethodID(jclazz, "onIncomingFrame", "(I[BJI)V");
        if (m_method_id_on_incoming_frame == NULL) {
            base::_error("Unable to get method `onIncomingFrame` reference.");
            return -1;
        }

        return 0;
    }

    void deinit() {
        jniStop();

        if(m_jvm) {
            JNIEnv *jenv = enterEnv(false);
            if(jenv)
                jenv->DeleteGlobalRef(m_jobj);
            exitEnv(jenv, false);

            m_jvm = NULL;
            m_jobj = NULL;
            m_method_id_on_event = NULL;
            m_method_id_on_incoming_frame = NULL;
        }
    }

    int jniInit(int rtsp_port, int transport_start_port) {
        if(rtsp_port < 0 || transport_start_port < 0) {
            base::_error("port is invalid");
            return -1;
        }

#ifndef USING_RTSP_CLIENT
        m_rtsp_port = rtsp_port;
        m_transport_start_port = transport_start_port;
        m_h264_unpacker.clear();
#endif
        m_waiting_keyframe = true;
        return 0;
    }

    int jniConnect(std::string url, bool rtp_over_tcp, uint32_t connect_timeout_ms, uint32_t stream_timeout_ms) {
#ifdef USING_RTSP_CLIENT
        m_client = RTSPPullClient::create();
        int ret = m_client->init(url.c_str(), this);
        if(ret != 0)
            return ret;
#else
        m_client.reset(new RTSPClient(base::ioGetDefaultContext(), url, m_rtsp_port));
        m_client->enableKeepAlive(45 * 1000);
        //m_client->enableAysnc();
        m_client->setCallbecker(this);
        int ret = m_client->init(m_transport_start_port);
        if(ret != RTSP_SUCCESS)
            return ret;
#endif
        base::_info("connect remote: %s", url.c_str());
        return m_client->connect(rtp_over_tcp, connect_timeout_ms, stream_timeout_ms);
    }

    int jniPlay() {
#ifdef DEBUG_H264_VIDEO
        BASE_DEBUG_FILE_PATH_OPEN("/sdcard");
#endif
        return m_client->play();
    }

    int jniPause() {
        return m_client->pause();
    }

    int jniStop() {
#ifdef DEBUG_H264_VIDEO
    BASE_DEBUG_FILE_CLOSE();
#endif
        m_client.reset();
        m_waiting_keyframe = true;
#ifndef USING_RTSP_CLIENT
        m_h264_unpacker.clear();
#endif
        return 0;
    }

    int jniAudioInfo(network::RTSPPullClient::AudioInfo &info) {
        return m_client->getAudioInfo(info);
    }

    int jniVideoInfo(network::RTSPPullClient::VideoInfo &info) {
        return m_client->getVideoInfo(info);
    }

    int jniVideoExtInfo(network::RTSPPullClient::VideoInfo &info) {
        return m_client->getVideoExtInfo(info);
    }

    /*
     * implementation of RTSPClient::Callbacker / RTSPPullClient::Callback
     * */
    virtual void onEvent(RTSPPullClient::RTSPEVENT evt, int result) {
        JNIEnv *jenv = enterEnv();
        if(jenv == NULL) return;
        jenv->CallVoidMethod(m_jobj, m_method_id_on_event, (int)((JNI_EVENT_ID)evt), result);
        exitEnv(jenv);
    }

#ifdef USING_RTSP_CLIENT
    virtual void onIncomingFrame(RTSPPullClient::MediaType type, const uint8_t *data, uint32_t len, int flags, int64_t pts_ms) {
        if(type == RTSPPullClient::Video) {
            if(m_waiting_keyframe) {
                if(RTSPPullClient::isKeyFrame(flags)) {
                    m_waiting_keyframe = false;
                } else {
                    return;
                }
            }
            onFrame(data, len, (uint32_t)pts_ms, 1);
            //base::_warning("pt-%d ssrc-%u seq-%u size-%d ", pkt->GetPayloadType(), pkt->GetSSRC(), pkt->GetSequenceNumber(), pkt->GetPayloadLength());
            //BASE_DEBUG_BIN(pkt->GetPayloadData(), pkt->GetPayloadLength()>50?50:pkt->GetPayloadLength());
        }
    }
#else
    virtual void onIncomingPacket(RTSPMediaType type, std::shared_ptr<RTPPacket> &pkt, std::shared_ptr<CRTPAddress> &addr) {
        if(type == RTSPMediaType::Video) {
            int ret = m_h264_unpacker.push(pkt);
            if (ret == ERR_TIMESTAMP) {
                base::_warning("frame broken, force to pop frame");
                popFrame();
                m_h264_unpacker.clear();
                ret = m_h264_unpacker.push(pkt);
            }

            if (ret == RTP_SUCCESSFUL_ACCESS) {
                popFrame();
                m_h264_unpacker.clear();
            }

            //base::_warning("pt-%d ssrc-%u seq-%u size-%d ", pkt->GetPayloadType(), pkt->GetSSRC(), pkt->GetSequenceNumber(), pkt->GetPayloadLength());
            //BASE_DEBUG_BIN(pkt->GetPayloadData(), pkt->GetPayloadLength()>50?50:pkt->GetPayloadLength());
        } else if (type == RTSPMediaType::Audio) {
            //base::_info("on audio");
        } else {
            base::_warning("just support video data, (type-%d)", type);
        }
    }
#endif

private:
#ifndef USING_RTSP_CLIENT
    void popFrame() {
        if(m_waiting_keyframe) {
            if(m_h264_unpacker.isKeyFrame()) {
                m_waiting_keyframe = false;
            } else {
                return;
            }
        }
        std::shared_ptr<base::BytesArray> &data = m_h264_unpacker.getH264Nal();
        onFrame(data->data(), data->size(), m_h264_unpacker.getPts(), 0);
    }
#endif

    void onFrame(const uint8_t *data, uint32_t len, uint32_t pts_ms, int has_start_code) {

        JNIEnv *jenv = enterEnv(true);
        if(jenv == NULL) return;

        jbyteArray array = jenv->NewByteArray(len);
        jenv->SetByteArrayRegion(array, 0, len, (const jbyte *)data);
        jenv->CallVoidMethod(m_jobj, m_method_id_on_incoming_frame, int(media::MediaType::MEDIA_TYPE_VIDEO), array, pts_ms*1000, has_start_code);

        exitEnv(jenv, true);

#if defined(DEBUG_H264_VIDEO)
        if(has_start_code == 0)
            BASE_DEBUG_STREAM(1, 0, nalSep, 4);
        BASE_DEBUG_STREAM(1, 0, data, len);
#endif
    }

    JNIEnv *enterEnv(bool attach = true) {
        JNIEnv *jenv = NULL;

        // Double check it's all ok
        int get_env_stat = m_jvm->GetEnv((void **)&jenv, JNI_VERSION_1_6);
        if (get_env_stat == JNI_EDETACHED) {
            if(attach) {
                #ifdef ANDROID
                if (m_jvm->AttachCurrentThread(&jenv, NULL) != 0) {
                #else
                if (m_jvm->AttachCurrentThread((void **)&jenv, NULL) != 0) {
                #endif
                    base::_error("Failed to attach");
                    return NULL;
                }
            }
        } else if (get_env_stat == JNI_EVERSION) {
            base::_error("GetEnv: version not supported");
            return NULL;
        } else if (get_env_stat == JNI_OK) {
            //
        }
        return jenv;
    }
    void exitEnv(JNIEnv *env, bool detach = true) {
        if (env->ExceptionCheck()) {
            env->ExceptionDescribe();
        }
        if(detach)
            m_jvm->DetachCurrentThread();
    }

private:
    JavaVM     *m_jvm;
    jobject     m_jobj;
    jmethodID   m_method_id_on_event;
    jmethodID   m_method_id_on_incoming_frame;

    bool        m_waiting_keyframe;
#ifdef USING_RTSP_CLIENT
    std::shared_ptr<RTSPPullClient>  m_client;
#else
    uint16_t    m_rtsp_port;
    uint16_t    m_transport_start_port;
    H264Unpacker m_h264_unpacker;
    std::shared_ptr<RTSPClient>  m_client;
#endif
};

typedef std::map<jint, std::shared_ptr<JniRTSPClient>> ClientsMap;
ClientsMap g_rtspclients;

JNIEXPORT jint JNICALL Java_sanp_javalon_network_protocol_RTSPClient_init__I
  (JNIEnv *jenv, jobject jobj, jint obj_id)
{
    std::shared_ptr<JniRTSPClient> client(NULL);
    ClientsMap::iterator itr = g_rtspclients.find(obj_id);
    if(itr != g_rtspclients.end()) {
        base::_error("had inited");
        return 0;
    }

    client.reset(new JniRTSPClient());
    int ret = client->init(jenv, jobj);
    if(ret != 0)
        return ret;
    g_rtspclients[obj_id] = client;
    return client->jniInit(0, (TRANSPORT_START_BASE_PORT+obj_id*10));
}

JNIEXPORT jint JNICALL Java_sanp_javalon_network_protocol_RTSPClient_init__III
  (JNIEnv *jenv, jobject jobj, jint obj_id, jint rtsp_port, jint transport_start_port)
{
    std::shared_ptr<JniRTSPClient> client(NULL);
    ClientsMap::iterator itr = g_rtspclients.find(obj_id);
    if(itr != g_rtspclients.end()) {
        base::_error("had inited");
        return 0;
    }

    client.reset(new JniRTSPClient());
    int ret = client->init(jenv, jobj);
    if(ret != 0)
        return ret;
    g_rtspclients[obj_id] = client;
    return client->jniInit(rtsp_port, transport_start_port);
}

JNIEXPORT jint JNICALL Java_sanp_javalon_network_protocol_RTSPClient_release
  (JNIEnv *jenv, jobject jobj, jint obj_id)
{
    ClientsMap::iterator itr = g_rtspclients.find(obj_id);
    if(itr == g_rtspclients.end()) {
        base::_error("had not inited");
        return -1;
    }

    std::shared_ptr<JniRTSPClient> &client = itr->second;
    client->deinit();
    client.reset();
    g_rtspclients.erase(itr);
    return 0;
}

#define GET_OBJ_CLIENT(ret) \
    ClientsMap::iterator itr = g_rtspclients.find(obj_id); \
    if(itr == g_rtspclients.end()) { \
        base::_error("had not inited"); \
        return ret; \
    } \
    std::shared_ptr<JniRTSPClient> &client = itr->second;

JNIEXPORT jint JNICALL Java_sanp_javalon_network_protocol_RTSPClient_connect
  (JNIEnv *jenv, jobject jobj, jint obj_id, jstring url, jint rtp_over_tcp, jint connect_timeout_ms, jint stream_timeout_ms)
{
    GET_OBJ_CLIENT(-1);

    const char *curl = jenv->GetStringUTFChars(url, JNI_FALSE);
    int ret = client->jniConnect(curl, rtp_over_tcp!=0, connect_timeout_ms, stream_timeout_ms);
    jenv->ReleaseStringUTFChars(url, (const char *)curl);
    return ret;
}

JNIEXPORT jint JNICALL Java_sanp_javalon_network_protocol_RTSPClient_play
  (JNIEnv *jenv, jobject jobj, jint obj_id)
{
    GET_OBJ_CLIENT(-1);
    return client->jniPlay();
}

JNIEXPORT jint JNICALL Java_sanp_javalon_network_protocol_RTSPClient_pause
  (JNIEnv *jenv, jobject jobj, jint obj_id)
{
    GET_OBJ_CLIENT(-1);
    return client->jniPause();
}

JNIEXPORT jint JNICALL Java_sanp_javalon_network_protocol_RTSPClient_stop
  (JNIEnv *jenv, jobject jobj, jint obj_id)
{
    GET_OBJ_CLIENT(-1);
    return client->jniStop();
}

JNIEXPORT jintArray JNICALL Java_sanp_javalon_network_protocol_RTSPClient_getAudioInfo
  (JNIEnv *jenv, jobject jobj, jint obj_id)
{
    GET_OBJ_CLIENT(NULL);
    network::RTSPPullClient::AudioInfo info;
    int ret = client->jniAudioInfo(info);
    if(ret != 0)
        return NULL;

    jint ainfo[4] = {0};
    ainfo[0] = media::Chars2CodecType(info.codec_name);
    ainfo[1] = info.sample_rate;
    ainfo[2] = info.channel_count;
    ainfo[3] = media::Chars2CodecProfile(info.profile);
    jintArray result = jenv->NewIntArray(4);
    jenv->SetIntArrayRegion(result, 0, 4, ainfo);
    return result;
}

JNIEXPORT jintArray JNICALL Java_sanp_javalon_network_protocol_RTSPClient_getVideoInfo
  (JNIEnv *jenv, jobject jobj, jint obj_id)
{
    GET_OBJ_CLIENT(NULL);
    network::RTSPPullClient::VideoInfo info;
    int ret = client->jniVideoInfo(info);
    if(ret != 0)
        return NULL;

    jint vinfo[5] = {0};
    vinfo[0] = media::Chars2CodecType(info.codec_name);
    vinfo[1] = info.width;
    vinfo[2] = info.height;
    vinfo[3] = info.fps;
    vinfo[4] = info.bitrate;
    jintArray result = jenv->NewIntArray(5);
    jenv->SetIntArrayRegion(result, 0, 5, vinfo);
    return result;
}

JNIEXPORT jintArray JNICALL Java_sanp_javalon_network_protocol_RTSPClient_getVideoExtInfo
  (JNIEnv *jenv, jobject jobj, jint obj_id)
{
    GET_OBJ_CLIENT(NULL);
    network::RTSPPullClient::VideoInfo info;
    int ret = client->jniVideoExtInfo(info);
    if(ret != 0)
        return NULL;

    jint vinfo[5] = {0};
    vinfo[0] = media::Chars2CodecType(info.codec_name);
    vinfo[1] = info.width;
    vinfo[2] = info.height;
    vinfo[3] = info.fps;
    vinfo[4] = info.bitrate;
    jintArray result = jenv->NewIntArray(5);
    jenv->SetIntArrayRegion(result, 0, 5, vinfo);
    return result;
}

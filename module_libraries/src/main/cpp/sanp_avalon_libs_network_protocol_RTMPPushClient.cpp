//
// Created by Tuyj on 2017/3/16.
//

#include <jni.h>
#include <memory>
#include <string>
#include <map>

#include "jansson.h"
#include "sanp_avalon_libs_network_protocol_RTMPPushClient.h"
#include "network/protocol/rtmp_client/rtmp_push_client.h"
#include "base/log.h"

//#define DEBUG_H264_VIDEO
#if defined(DEBUG_H264_VIDEO)
#include "base/debug.h"
BASE_DEBUG_INIT("jni-rtmp-h264");
static uint8_t nalSep[4] = { 0, 0, 0, 1 };
#endif

using namespace network;

#define MEDIA_TYPE_NAME_AUDIO       "audio"
#define MEDIA_TYPE_NAME_VIDEO       "video"

#define FORMAT_KEY_NAME_CODEC_TYPE  "codec_type"
#define FORMAT_KEY_NAME_MEDIA_TYPE  "media_type"
#define FORMAT_KEY_NAME_BIT_RATE    "bit_rate"
#define FORMAT_KEY_NAME_CHANNELS    "channels"
#define FORMAT_KEY_NAME_SAMPLE_RATE "sample_rate"
#define FORMAT_KEY_NAME_WIDTH       "width"
#define FORMAT_KEY_NAME_HEIGHT      "height"

#define CODEC_NAME_AAC              "AAC"
#define CODEC_NAME_G711A            "G711A"
#define CODEC_NAME_G711U            "G711U"
#define CODEC_NAME_H264             "H264"
#define CODEC_NAME_H265             "H265"
#define CODEC_NAME_VP8              "VP8"

#define GET_JSON_STRING(json_objs, key_name, out_value) \
    obj = json_object_get(json_objs, key_name); \
    if(obj == NULL) { \
        GOTO_QUIT(-1, "format_json is invalid: lost '%s'", key_name); \
    } \
    out_value = json_string_value(obj);

#define GET_JSON_INT(json_objs, key_name, out_value) \
    obj = json_object_get(json_objs, key_name); \
    if(obj == NULL) { \
        GOTO_QUIT(-1, "format_json is invalid: lost '%s'", key_name); \
    } \
    tmp_ = json_string_value(obj); \
    out_value = atoi(tmp_.c_str())

#define GOTO_QUIT(code, ...) \
    base::_error(__VA_ARGS__); \
    ret = code; \
    goto quit;

class JniRTMPPushClient: public RTMPPushClient::Callback
{
public:
    typedef enum {
        /*
        RTMP_EVT_CONNECTED    = 0,
        RTMP_EVT_DISCONNECTED = 1,
        */
        RTMP_EVT_CONNECTION_BROKEN = 2,
    } JNI_EVENT_ID;

    JniRTMPPushClient():
        m_jvm(NULL),m_jobj(NULL),
        m_method_id_on_event(NULL),m_client(NULL){
    }

    ~JniRTMPPushClient() {
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

        return 0;
    }

    void deinit() {
        jniClose();

        if(m_jvm) {
            JNIEnv *jenv = enterEnv(false);
            if(jenv)
                jenv->DeleteGlobalRef(m_jobj);
            exitEnv(jenv, false);

            m_jvm = NULL;
            m_jobj = NULL;
            m_method_id_on_event = NULL;
        }
    }

    int jniInit(const char *url) {
#ifdef DEBUG_H264_VIDEO
        BASE_DEBUG_FILE_PATH_OPEN("/sdcard");
#endif
        m_client.reset(RTMPPushClient::createAsyncClient(this));
        return m_client->init(url);
    }

    int jniClose() {
        m_client.reset();
        return 0;
    }

    int jniAddStream(const char *format_json, uint8_t *extradata, int len) {
        RTMPPushClient::Format format;
        if(loadFormat(format_json, extradata, len, format) != 0)
            return -1;

        base::_warning("add rtmp stream: codec-%d type-%d bitrate-%lld "
            "num-%d dem-%d extrasize-%d "
            "width-%d height-%d fix-%d",
            format.codec_type, format.media_type, format.bit_rate,
            format.time_base.num, format.time_base.den, format.extraDataSize(),
            format.spec.video.width, format.spec.video.height, format.spec.video.pix_fmt);

        return m_client->addStream(format);
    }

    int jniConnect() {
        return m_client->connect();
    }

    int jniDisconnect() {
#ifdef DEBUG_H264_VIDEO
        BASE_DEBUG_FILE_CLOSE();
        base::_info("close file: ty.%s.dbg", _base_local_prefix);
#endif
        return m_client->disconnect();
    }

    void jniWrite(int stream_index, std::shared_ptr<media::Packet> &pkt) {
#if defined(DEBUG_H264_VIDEO)
        //BASE_DEBUG_STREAM(1, 0, nalSep, 4);
        BASE_DEBUG_STREAM(1, 0, pkt->data(), pkt->size());
#endif
        //base::_warning("pts-%lld dts-%lld isKey-%d size-%d index-%d", pkt->pts(), pkt->dts(), pkt->isKeyFrame()?1:0, pkt->size(), stream_index);
        //BASE_DEBUG_BIN(pkt->data(), 100);
        m_client->write(stream_index, pkt);
    }

    void onEvent(RTMPPushClient::RTMPEVENT evt, int result) {
        JNIEnv *jenv = enterEnv();
        if(jenv == NULL) return;
        jenv->CallVoidMethod(m_jobj, m_method_id_on_event, (int)((JNI_EVENT_ID)evt), result);
        exitEnv(jenv);
    }

private:
    int loadFormat(const char *format_json, uint8_t *extradata, int len,
            RTMPPushClient::Format &format) {

        int ret = 0;
        json_error_t error;
        json_t *formats = json_loads(format_json, 0, &error);
        if(formats == NULL) {
            base::_error("load json from format_json failed:source(%s) text(%s)", error.source, error.text);
            return -1;
        }

        json_t *obj = NULL;
        std::string media_type,codec_type,tmp_;
        GET_JSON_STRING(formats, FORMAT_KEY_NAME_MEDIA_TYPE, media_type);
        GET_JSON_STRING(formats, FORMAT_KEY_NAME_CODEC_TYPE, codec_type);
        if(media_type == MEDIA_TYPE_NAME_AUDIO) {

            format.media_type = media::MEDIA_TYPE_AUDIO;
            if(codec_type == CODEC_NAME_AAC) {
                format.codec_type = media::AUDIO_CODEC_AAC;
            } else if(codec_type == CODEC_NAME_G711A) {
                format.codec_type = media::AUDIO_CODEC_G711A;
            } else if(codec_type == CODEC_NAME_G711U) {
                format.codec_type = media::AUDIO_CODEC_G711U;
            } else {
                GOTO_QUIT(-1, "non-supported codec type %s", codec_type.c_str());
            }
            GET_JSON_INT(formats, FORMAT_KEY_NAME_CHANNELS, format.spec.audio.channels);
            GET_JSON_INT(formats, FORMAT_KEY_NAME_SAMPLE_RATE, format.spec.audio.sample_rate);
            format.spec.audio.sample_fmt = media::AUDIO_SAMPLE_FORMAT_S16;

        } else if(media_type == MEDIA_TYPE_NAME_VIDEO) {

            format.media_type = media::MEDIA_TYPE_VIDEO;
            if(codec_type == CODEC_NAME_H264) {
                format.codec_type = media::VIDEO_CODEC_H264;
            } else if(codec_type == CODEC_NAME_H265) {
                format.codec_type = media::VIDEO_CODEC_H265;
            } else if(codec_type == CODEC_NAME_VP8) {
                format.codec_type = media::VIDEO_CODEC_VP8;
            } else {
                GOTO_QUIT(-1, "non-supported codec type %s", codec_type.c_str());
            }
            GET_JSON_INT(formats, FORMAT_KEY_NAME_WIDTH, format.spec.video.width);
            GET_JSON_INT(formats, FORMAT_KEY_NAME_HEIGHT, format.spec.video.height);
            format.spec.video.pix_fmt = media::VideoFrame::PixelFormat_I420;

        } else {
            GOTO_QUIT(-1, "non-supported media type %s", media_type.c_str());
        }

        GET_JSON_INT(formats, FORMAT_KEY_NAME_BIT_RATE, format.bit_rate);
        format.time_base = base::Rational{1, 1000}; /* milliseconds */
        format.setRawExtraData(extradata, len);

    quit:
        json_decref(formats);
        return ret;
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

    std::shared_ptr<RTMPPushClient>  m_client;
};

typedef std::map<jint, std::shared_ptr<JniRTMPPushClient>> ClientsMap;
ClientsMap g_rtmpclients;

JNIEXPORT jint JNICALL Java_sanp_avalon_libs_network_protocol_RTMPPushClient_init
  (JNIEnv *jenv, jobject jobj, jint obj_id, jstring url)
{
    std::shared_ptr<JniRTMPPushClient> client(NULL);
    ClientsMap::iterator itr = g_rtmpclients.find(obj_id);
    if(itr != g_rtmpclients.end()) {
        base::_error("had inited");
        return 0;
    }

    client.reset(new JniRTMPPushClient());
    int ret = client->init(jenv, jobj);
    if(ret != 0)
        return ret;
    g_rtmpclients[obj_id] = client;

    const char *curl = jenv->GetStringUTFChars(url, JNI_FALSE);
    ret = client->jniInit(curl);
    jenv->ReleaseStringUTFChars(url, curl);
    return ret;
}

JNIEXPORT jint JNICALL Java_sanp_avalon_libs_network_protocol_RTMPPushClient_close
  (JNIEnv *jenv, jobject jobj, jint obj_id)
{
    ClientsMap::iterator itr = g_rtmpclients.find(obj_id);
    if(itr == g_rtmpclients.end()) {
        base::_error("had not inited");
        return -1;
    }

    std::shared_ptr<JniRTMPPushClient> &client = itr->second;
    client->deinit();
    client.reset();
    g_rtmpclients.erase(itr);
    return 0;
}

#define GET_OBJ_CLIENT() \
    ClientsMap::iterator itr = g_rtmpclients.find(obj_id); \
    if(itr == g_rtmpclients.end()) { \
        base::_error("had not inited"); \
        return -1; \
    } \
    std::shared_ptr<JniRTMPPushClient> &client = itr->second;

JNIEXPORT jint JNICALL Java_sanp_avalon_libs_network_protocol_RTMPPushClient_addStream
  (JNIEnv *jenv, jobject jobj, jint obj_id, jstring format_json, jbyteArray jextradata)
{
    GET_OBJ_CLIENT();

    int len = 0;
    jbyte *jdata = NULL;
    if(jextradata != NULL) {
        len = jenv->GetArrayLength(jextradata);
        jdata = jenv->GetByteArrayElements(jextradata, JNI_FALSE);
    }

    const char *cformat = jenv->GetStringUTFChars(format_json, JNI_FALSE);
    int ret = client->jniAddStream(cformat, (uint8_t *)jdata, len);
    jenv->ReleaseStringUTFChars(format_json, cformat);
    return ret;
}

JNIEXPORT jint JNICALL Java_sanp_avalon_libs_network_protocol_RTMPPushClient_connect
  (JNIEnv *jenv, jobject jobj, jint obj_id)
{
    GET_OBJ_CLIENT();
    return client->jniConnect();
}

JNIEXPORT jint JNICALL Java_sanp_avalon_libs_network_protocol_RTMPPushClient_disconnect
  (JNIEnv *jenv, jobject jobj, jint obj_id)
{
    GET_OBJ_CLIENT();
    return client->jniDisconnect();
}

enum :uint32_t {
    WRITING_FLAG_KEYFRAME = 0x00000001,
};

JNIEXPORT jint JNICALL Java_sanp_avalon_libs_network_protocol_RTMPPushClient_write
  (JNIEnv *jenv, jobject jobj, jint obj_id, jint stream_index, jbyteArray datas, jint offset, jint length, jlong pts_us, jint flag)
{
    GET_OBJ_CLIENT();

    jbyte *jdata = jenv->GetByteArrayElements(datas, JNI_FALSE);
    std::shared_ptr<media::Packet> pkt(new media::Packet());
    pkt->append(((uint8_t *)jdata)+offset, length);  //jenv->GetArrayLength(datas);
    pkt->setPts(pts_us/1000);
    pkt->setDts(pkt->pts());
    pkt->setIsKeyFrame((flag & WRITING_FLAG_KEYFRAME) != 0);
    client->jniWrite(stream_index, pkt);
    return 0;
}



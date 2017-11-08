//
// Created by Tuyj on 2017/3/16.
//

#include <jni.h>
#include "sanp_javalon_network_protocol_RTMPBandwidthTest.h"
#include "network/protocol/rtmp_client/tools/rtmp_bandwidth_test.h"
#include "base/log.h"

static network::RTMPBandwidthTester g_tester;

JNIEXPORT jint JNICALL Java_sanp_javalon_network_protocol_RTMPBandwidthTest_jniInit
  (JNIEnv *jenv, jobject jobj, jstring filename, jint min_inva_ms, jint statis_period_ms)
{
    const char *stream_file = jenv->GetStringUTFChars(filename, JNI_FALSE);
    std::string ifile = stream_file; ifile += "_idx";
    std::string sfile = stream_file; sfile += "_sps";
    const char *index_file = ifile.c_str();
    const char *sps_file = sfile.c_str();
    int ret = g_tester.init(stream_file, index_file, sps_file, min_inva_ms, statis_period_ms);
    jenv->ReleaseStringUTFChars(filename, stream_file);
    return ret;
}

JNIEXPORT jint JNICALL Java_sanp_javalon_network_protocol_RTMPBandwidthTest_jniClose
  (JNIEnv *jenv, jobject jobj)
{
    return g_tester.close();
}

JNIEXPORT jint JNICALL Java_sanp_javalon_network_protocol_RTMPBandwidthTest_jniStart
  (JNIEnv *jenv, jobject jobj, jstring rtmp_server)
{
    const char *server = jenv->GetStringUTFChars(rtmp_server, JNI_FALSE);
    int ret = g_tester.start(server);
    jenv->ReleaseStringUTFChars(rtmp_server, server);
    base::_info("RTMPBandwidthTest start %d", ret);
    return ret;
}

JNIEXPORT jint JNICALL Java_sanp_javalon_network_protocol_RTMPBandwidthTest_jniStop
  (JNIEnv *jenv, jobject jobj)
{
    base::_info("RTMPBandwidthTest stop");
    return g_tester.stop();
}

JNIEXPORT jlongArray JNICALL Java_sanp_javalon_network_protocol_RTMPBandwidthTest_jniGetStatistics
  (JNIEnv *jenv, jobject jobj)
{
    network::RTMPBandwidthTester::SendingStatistics statis;
    g_tester.getStatistics(statis);
    jlong avg_bitrate[2] = {0};
    if(statis.period_duration_ms > 0)
        avg_bitrate[0] = (statis.period_bytes * 1000 / statis.period_duration_ms) * 8;
    if(statis.cumulative_duration_ms > 0)
        avg_bitrate[1] = (statis.cumulative_bytes * 1000 / statis.cumulative_duration_ms) * 8;

    jlongArray result = jenv->NewLongArray(2);
    jenv->SetLongArrayRegion(result, 0, 2, avg_bitrate);
    return result;
}

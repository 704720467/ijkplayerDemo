//
// Created by gwd on 2018/3/21.
//

#include <jni.h>
#include "JavaCallHelper.h"
#include "VNIFFmpeg.h"

extern "C" {
#include "resample.h"
}


JavaVM *javaVM = NULL;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    javaVM = vm;
    return JNI_VERSION_1_4;
}


extern "C"
JNIEXPORT jshortArray JNICALL
Java_com_zp_libvideoedit_utils_Resample_resample(JNIEnv *env, jobject instance, jint channel_count,
                                                 jshortArray inputBuffer_, jint src_sample_rate,
                                                 jint inputbuffer_count, jint dstSample_rate) {
    printf("世界你好");
    jshort *inputBuffer = env->GetShortArrayElements(inputBuffer_, NULL);

    jint outPutBufferCount = 0;
    //计算输出的buffer的大小
    outPutBufferCount = inputbuffer_count;
//    //创建输出的buffer
    float aspect = (float) dstSample_rate / src_sample_rate;
    outPutBufferCount = inputbuffer_count * aspect;
    short *outPutBuffer = (short *) malloc(outPutBufferCount * sizeof(short));
//    //重采样
    resample(channel_count, inputBuffer, src_sample_rate, dstSample_rate, inputbuffer_count,
             outPutBuffer, outPutBufferCount);
    jshortArray shortArray = env->NewShortArray(outPutBufferCount);
    env->SetShortArrayRegion(shortArray, 0, outPutBufferCount, outPutBuffer);
    env->ReleaseShortArrayElements(inputBuffer_, inputBuffer, 0);
    return shortArray;
}

/**
 * 初始化音频
 */

extern "C"
JNIEXPORT VNIFFmpeg *JNICALL
Java_com_zp_libvideoedit_utils_Resample_nativeAudioPrepare(JNIEnv *env, jobject instance,
                                                           jstring audioPath_, jlong startTime) {
    const char *audioPath = env->GetStringUTFChars(audioPath_, 0);
    JavaCallHelper *javaCallHelper = new JavaCallHelper(javaVM, env, instance);
    VNIFFmpeg *vnifFmpeg = new VNIFFmpeg(javaCallHelper, audioPath, startTime);
    vnifFmpeg->prepare();
    env->ReleaseStringUTFChars(audioPath_, audioPath);
    return vnifFmpeg;
}

//
//extern "C"
//JNIEXPORT VNIFFmpeg *JNICALL
//Java_com_zp_libvideoedit_utils_Resample_nativeAudioPrepare(JNIEnv *env, jobject instance,
//                                                           jstring audioPath_) {
//    const char *audioPath = env->GetStringUTFChars(audioPath_, 0);
//    JavaCallHelper *javaCallHelper = new JavaCallHelper(javaVM, env, instance);
//    VNIFFmpeg *vnifFmpeg = new VNIFFmpeg(javaCallHelper, audioPath);
//    vnifFmpeg->prepare();
//    env->ReleaseStringUTFChars(audioPath_, audioPath);
//    return vnifFmpeg;
//
//}



/**
 * 开始解码音频
 */

extern "C"
JNIEXPORT void JNICALL
Java_com_zp_libvideoedit_utils_Resample_native_1Audio_1Decode_1Start(JNIEnv *env, jobject instance,
                                                                     VNIFFmpeg *vnifFmpeg) {
    if (vnifFmpeg) {
        vnifFmpeg->start();
    }
}


extern "C"
JNIEXPORT void JNICALL
Java_com_zp_libvideoedit_utils_Resample_native_1Audio_1Decode_1Stop(JNIEnv *env, jobject instance) {

    // TODO

}

extern "C"
JNIEXPORT void JNICALL
Java_com_zp_libvideoedit_utils_Resample_native_1Audio_1Decode_1Pause(JNIEnv *env,
                                                                     jobject instance,
                                                                     VNIFFmpeg *vnifFmpeg) {
    if (vnifFmpeg) {
        vnifFmpeg->pause();
    }
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_zp_libvideoedit_utils_Resample_native_1Get_1Audio_1Data(JNIEnv *env, jobject instance,
                                                                 VNIFFmpeg *mVNIFFmpegId) {

    if (mVNIFFmpegId) {
        return mVNIFFmpegId->getAudioData();
    }
    return NULL;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_zp_libvideoedit_utils_Resample_native_1Audio_1Seek(JNIEnv *env, jobject instance,
                                                            jlong pts, VNIFFmpeg *mVNIFFmpegId) {

    if (mVNIFFmpegId) {
        mVNIFFmpegId->setSeekTime(pts);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_zp_libvideoedit_utils_Resample_native_1Audio_1Decode_1Release(JNIEnv *env,
                                                                       jobject instance,
                                                                       VNIFFmpeg *mVNIFFmpegId) {

    if (mVNIFFmpegId) {
        mVNIFFmpegId->release();
    }
}
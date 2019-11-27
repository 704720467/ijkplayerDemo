//
// Created by zp on 2019/6/5.
//

#ifndef FFMPEGTEST2_JAVACALLHELPER_H
#define FFMPEGTEST2_JAVACALLHELPER_H


#include <jni.h>

class JavaCallHelper {
public:
    JavaCallHelper(JavaVM *_javaVM, JNIEnv *_env, jobject &_jobj);

    ~JavaCallHelper();

    void onError(int thread, int code);

    void onParpare(int thread);

    void onProgress(int thread, int progress);

    void audioCallBack(int thread, uint8_t *out_buffer, int out_buffer_size, int pts);

    jshortArray getAudioData(int thread, uint8_t *out_buffer, int out_buffer_size, int pts);

    //isDecodeEnd 是否播放完毕 0 没有  1已经解码完毕
    jobject getAudioDataNew(int thread, uint8_t *out_buffer, int out_buffer_size, long pts,
                            int isDecodeEnd);

    void onFinish(int thread);


    JavaVM *javaVM;
    jmethodID jmid_audioCallBack;
    jmethodID jmid_audioCallBackForShort;
    jobject jobj;
    jbyteArray audioByteArray;
    jshortArray audioShortArray;

private:
    jmethodID jmid_prepare;
    jmethodID jmid_error;
    jmethodID jmid_progress;
    jmethodID jmid_finish;
    JNIEnv *env;

    int audioOldSize;
};


#endif //FFMPEGTEST2_JAVACALLHELPER_H

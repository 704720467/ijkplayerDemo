//
// Created by zp on 2019/6/5.
//

#include "JavaCallHelper.h"
#include "macro.h"

JavaCallHelper::JavaCallHelper(JavaVM *_javaVM, JNIEnv *_env, jobject &_jobj) : javaVM(_javaVM),
                                                                                env(_env) {
    jobj = env->NewGlobalRef(_jobj);
    jclass jclazz = env->GetObjectClass(jobj);
    jmid_error = env->GetMethodID(jclazz, "onError", "(I)V");
    jmid_finish = env->GetMethodID(jclazz, "onFinish", "()V");
    jmid_prepare = env->GetMethodID(jclazz, "onPrepare", "()V");
    jmid_progress = env->GetMethodID(jclazz, "onProgress", "(I)V");
    jmid_audioCallBack = env->GetMethodID(jclazz, "audioFrameCallBack", "([BJ)V");
    jmid_audioCallBackForShort = env->GetMethodID(jclazz, "audioFrameCallBackForShort", "([SJ)V");
}

JavaCallHelper::~JavaCallHelper() {

}

void JavaCallHelper::onError(int thread, int code) {
    if (thread == THREAD_CHILD) {
        JNIEnv *jniEnv;
        if (javaVM->AttachCurrentThread(&jniEnv, 0) != JNI_OK) {
            return;
        }
        jniEnv->CallVoidMethod(jobj, jmid_error, code);
        javaVM->DetachCurrentThread();
    } else {
        env->CallVoidMethod(jobj, jmid_error, code);
    }
}

/**
 * 播放结束回调
 * @param thread
 */
void JavaCallHelper::onFinish(int thread) {
    if (thread == THREAD_CHILD) {
        JNIEnv *jniEnv;
        if (javaVM->AttachCurrentThread(&jniEnv, 0) != JNI_OK) {
            return;
        }
        jniEnv->CallVoidMethod(jobj, jmid_finish);
        javaVM->DetachCurrentThread();
    } else {
        env->CallVoidMethod(jobj, jmid_finish);
    }
}

/**
 * 准备完成回调
 * @param thread
 */
void JavaCallHelper::onParpare(int thread) {
    if (thread == THREAD_CHILD) {
        JNIEnv *jniEnv;
        if (javaVM->AttachCurrentThread(&jniEnv, 0) != JNI_OK) {
            return;
        }
        jniEnv->CallVoidMethod(jobj, jmid_prepare);
        javaVM->DetachCurrentThread();
    } else {
        env->CallVoidMethod(jobj, jmid_prepare);
    }
}

/**
 * 播放进度回调
 * @param thread
 * @param progress
 */
void JavaCallHelper::onProgress(int thread, int progress) {
    if (thread == THREAD_CHILD) {
        JNIEnv *jniEnv;
        if (javaVM->AttachCurrentThread(&jniEnv, 0) != JNI_OK) {
            return;
        }
        jniEnv->CallVoidMethod(jobj, jmid_progress, progress);
        javaVM->DetachCurrentThread();
    } else {
        env->CallVoidMethod(jobj, jmid_progress, progress);
    }
}

/**
 * 音频解码数据回调
 * @param thread
 * @param out_buffer
 * @param out_buffer_size
 * @param pts
 */
void JavaCallHelper::audioCallBack(int thread, uint8_t *out_buffer, int out_buffer_size, int pts) {

    if (thread == THREAD_CHILD) {
        JNIEnv *jniEnv;
        if (javaVM->AttachCurrentThread(&jniEnv, 0) != JNI_OK) {
            return;
        }
        if (audioShortArray == NULL || audioOldSize != out_buffer_size) {
//            audioByteArray = jniEnv->NewByteArray(out_buffer_size);
            jshortArray audioShortArraytemp = jniEnv->NewShortArray(out_buffer_size / 2);
            audioShortArray = (jshortArray) jniEnv->NewGlobalRef(audioShortArraytemp);
            audioOldSize = out_buffer_size;
        }
        jniEnv->SetShortArrayRegion(audioShortArray, 0, out_buffer_size / 2,
                                    (const jshort *) (jbyte *) out_buffer);
        if (FFMPEG_PRINT_LOG)
            LOGE("音频播放中回调：pts=%d", pts);
        jniEnv->CallVoidMethod(jobj, jmid_audioCallBackForShort, audioShortArray, pts);
        javaVM->DetachCurrentThread();

    } else {
        if (audioByteArray == NULL || audioOldSize != out_buffer_size) {
            audioByteArray = env->NewByteArray(out_buffer_size);
            audioOldSize = out_buffer_size;
        }
        env->SetByteArrayRegion(audioByteArray, 0, audioOldSize,
                                (jbyte *) out_buffer);
        env->CallVoidMethod(jobj, jmid_audioCallBack, audioByteArray, pts);
    }
}

jshortArray
JavaCallHelper::getAudioData(int thread, uint8_t *out_buffer, int out_buffer_size, int pts) {

    if (thread == THREAD_CHILD) {
        JNIEnv *jniEnv;
        if (javaVM->AttachCurrentThread(&jniEnv, 0) != JNI_OK) {
            return NULL;
        }
        if (audioShortArray == NULL || audioOldSize != out_buffer_size) {
//            audioByteArray = jniEnv->NewByteArray(out_buffer_size);
            jshortArray audioShortArraytemp = jniEnv->NewShortArray(out_buffer_size / 2);
            audioShortArray = (jshortArray) jniEnv->NewGlobalRef(audioShortArraytemp);
            audioOldSize = out_buffer_size;
        }
        jniEnv->SetShortArrayRegion(audioShortArray, 0, out_buffer_size / 2,
                                    (const jshort *) (jbyte *) out_buffer);
        if (FFMPEG_PRINT_LOG)
            LOGE("getAudioData_音频播放中回调：pts=%d", pts);

    } else {
        if (audioByteArray == NULL || audioOldSize != out_buffer_size) {
            jshortArray audioShortArraytemp = env->NewShortArray(out_buffer_size / 2);
            audioShortArray = (jshortArray) env->NewGlobalRef(audioShortArraytemp);
            audioOldSize = out_buffer_size;
        }
        env->SetByteArrayRegion(audioByteArray, 0, audioOldSize,
                                (jbyte *) out_buffer);
        env->CallVoidMethod(jobj, jmid_audioCallBack, audioByteArray, pts);
    }
    return audioShortArray;
}

jobject
JavaCallHelper::getAudioDataNew(int thread, uint8_t *out_buffer, int out_buffer_size, long pts,
                                int isDecodeEnd) {

    if (thread == THREAD_CHILD) {
        JNIEnv *jniEnv;
        if (javaVM->AttachCurrentThread(&jniEnv, 0) != JNI_OK) {
            return NULL;
        }
        if (audioShortArray == NULL || audioOldSize != out_buffer_size) {
//            audioByteArray = jniEnv->NewByteArray(out_buffer_size);
            jshortArray audioShortArraytemp = jniEnv->NewShortArray(out_buffer_size / 2);
            audioShortArray = (jshortArray) jniEnv->NewGlobalRef(audioShortArraytemp);
            audioOldSize = out_buffer_size;
        }
        jniEnv->SetShortArrayRegion(audioShortArray, 0, out_buffer_size / 2,
                                    (const jshort *) (jbyte *) out_buffer);
        //返回数据
        jclass jclassAudioDataBack = env->FindClass(
                "com.zp.libvideoedit.EditCore/AudioDataPacket");
        jmethodID jmid_audioDataBack = env->GetMethodID(jclassAudioDataBack, "<init>", "([SJI)V");
//        jint dd = isDecodeEnd;
        if (FFMPEG_PRINT_LOG)
            LOGE("getAudioData_音频播放中回调：pts=%d，isDecodeEnd=%d", pts, isDecodeEnd);
        jobject jobject1 = jniEnv->NewObject(jclassAudioDataBack, jmid_audioDataBack,
                                             audioShortArray, (jlong) pts, (jint) isDecodeEnd);
        jniEnv->DeleteLocalRef(jclassAudioDataBack);
        return jobject1;
    }
    return NULL;
}


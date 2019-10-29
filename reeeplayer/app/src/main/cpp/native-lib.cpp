#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_cn_reee_reeeplayer_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
extern "C"
JNIEXPORT void JNICALL
Java_cn_reee_reeeplayer_MainActivity_native_1tsInitCacheManager(JNIEnv *env, jclass type,
                                                                jstring cachePath_,
                                                                jlong maxDurationS) {
    const char *cachePath = env->GetStringUTFChars(cachePath_, 0);

    // TODO

    env->ReleaseStringUTFChars(cachePath_, cachePath);
}extern "C"
JNIEXPORT void JNICALL
Java_cn_reee_reeeplayer_MainActivity_native_1tsDestoryHttpCache(JNIEnv *env, jclass type) {

    // TODO

}

extern "C"
JNIEXPORT void JNICALL
Java_cn_reee_reeeplayer_MainActivity_native_1TsGetTsFinished(JNIEnv *env, jclass type,
                                                             jstring ts_file_, jint size) {
    const char *ts_file = env->GetStringUTFChars(ts_file_, 0);

    // TODO

    env->ReleaseStringUTFChars(ts_file_, ts_file);
}extern "C"
JNIEXPORT void JNICALL
Java_cn_reee_reeeplayer_MainActivity_native_1ss(JNIEnv *env, jclass type, jlong begin_time_ms,
                                                jlong end_time_ms, jlong time_out_s,
                                                jobjectArray tsFileArray, jlong ts_start_time_ms,
                                                jlong ts_duration_ms) {
    char **ts_files = NULL;
    int count = ts_get_or_download_ts_files(begin_time_ms, end_time_ms, time_out_s, &ts_files,
                                            &ts_start_time_ms, &ts_duration_ms);
    jclass objClass = (*env)->FindClass((*env), "java/lang/String");
    tsFileArray = (*env)->NewObjectArray((*env), (jsize) count, objClass, 0);
    jstring jstr;
    for (int i = 0; i < count; i++) {
        jstr = (*env)->NewStringUTF((*env), ts_files[i]);
        (*env)->SetObjectArrayElement((*env), tsFileArray, i, jstr);
    }
    return;
}extern "C"
JNIEXPORT jint JNICALL
Java_tv_danmaku_ijk_media_player_IjkMediaPlayer_native_1tsGetOrDownloadTsFiles(JNIEnv *env,
                                                                               jclass type,
                                                                               jlong begin_time_ms,
                                                                               jlong end_time_ms,
                                                                               jlong time_out_s,
                                                                               jobjectArray ts_files,
                                                                               jlong ts_start_time_ms,
                                                                               jlong ts_duration_ms) {

    // TODO

}extern "C"
JNIEXPORT void JNICALL
Java_tv_danmaku_ijk_media_player_IjkMediaPlayer_native_1TsGetTsFinished(JNIEnv *env, jclass type,
                                                                        jobjectArray ts_files,
                                                                        jint size) {


}
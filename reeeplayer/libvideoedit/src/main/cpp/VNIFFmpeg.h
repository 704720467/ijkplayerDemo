//
// Created by zp on 2019/6/6.
//

#ifndef NEW_VNI_VNIFFMPEG_H
#define NEW_VNI_VNIFFMPEG_H

#include <sys/types.h>
#include "JavaCallHelper.h"
#include "AudioChannel.h"
#include "VideoChannel.h"


extern "C" {
#include <libavformat/avformat.h>
#include <libavutil/time.h>
}

/**
 * 控制层
 */
class VNIFFmpeg {
public:
    VNIFFmpeg(JavaCallHelper *javaCallHelper, const char *dataSource, long startTime);

    ~VNIFFmpeg();

    void prepare();

    void prepareFFmpeg();

    void start();

    void play();

    void pause();

    void release();

    jobject getAudioData();

    void setSeekTime(long seekTimeUs);

private:
    bool isPlaying = false;
    char *url;
    pthread_t pid_prepare;//销毁
    pthread_t pid_play;//知道播放完毕
    AudioChannel *audioChannel;
//    VideoChannel *videoChannel;
    AVFormatContext *formatContext;
    JavaCallHelper *javaCallHelper;
    long startTimeUs = 0;
    long seekTime = 0;
};


#endif //NEW_VNI_VNIFFMPEG_H

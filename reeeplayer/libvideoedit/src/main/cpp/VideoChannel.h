//
// Created by zp on 2019/6/5.
//

#ifndef FFMPEGTEST2_VIDEOCHANNEL_H
#define FFMPEGTEST2_VIDEOCHANNEL_H

#include "BaseChannel.h"
#include <pthread.h>
#include <android/native_window.h>
#include "JavaCallHelper.h"

typedef void (*RenderFrame)(uint8_t *, int, int, int);

class VideoChannel : public BaseChannel {
public:
    VideoChannel(int id, JavaCallHelper *javaCallHelper, AVCodecContext *avCodecContext,
                 AVRational avRational);

    virtual void play();

    virtual void stop();

    void decodePacket();

    void synchronizeFrame();

    void setRenderCallback(RenderFrame renderFrame);

private:
    pthread_t pid_video_play;
    pthread_t pid_synchronize;
    RenderFrame renderFrame;
};


#endif //FFMPEGTEST2_VIDEOCHANNEL_H

//
// Created by zp on 2019/6/4.
//

#ifndef PALYERWANGYI_AUDIOCHANNEL_H
#define PALYERWANGYI_AUDIOCHANNEL_H

extern "C" {
#include <libswresample/swresample.h>
}

#include "BaseChannel.h"

typedef void (*AudioRenderFrame)(uint8_t *, int, int);

class AudioChannel : public BaseChannel {

public:
    AudioChannel(int id, JavaCallHelper *javaCallHelper, AVCodecContext *avCodecContext,
                 AVRational avRational);

    virtual void play();

    virtual void pause();

    virtual void stop();

    void release();

    jobject getData();

    void decodePacketAudio();

    void synchronizeFrameAudio();

    void clearCache();//packet 和 frame

private:
    pthread_t pid_video_play;
    pthread_t pid_synchronize;
    int out_channels;
    int out_samplesize;
    int out_sample_rate;
    SwrContext *swrContext;

public:
    uint8_t *buffer;
};

#endif //PALYERWANGYI_AUDIOCHANNEL_H

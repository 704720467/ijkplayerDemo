//
// Created by zp on 2019/6/4.
//

#ifndef PALYERWANGYI_BASECHANNEL_H
#define PALYERWANGYI_BASECHANNEL_H
extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
};


#include "safe_queue.h"
#include "JavaCallHelper.h"
#include "macro.h"

class BaseChannel {

public:
    BaseChannel(int id, JavaCallHelper *javaCallHelper, AVCodecContext *avCodecContext,
                AVRational avRational
    ) : channelId(id),
        javaCallHelper(javaCallHelper),
        avCodecContext(avCodecContext),
        avRational(avRational) {
        pkt_queue.setReleaseHandle(releaseAvPacket);
        frame_queue.setReleaseHandle(releaseAvFrame);
    };

    static void releaseAvPacket(AVPacket *&packet) {
        if (packet) {
            av_packet_free(&packet);
            packet = 0;
        }
    }

    static void releaseAvFrame(AVFrame *&frame) {
        if (frame) {
            av_frame_free(&frame);
            frame = 0;
        }
    }

    void stopWork() {
        pkt_queue.setWork(0);
        frame_queue.setWork(0);
    }

    void startWork() {
        pkt_queue.setWork(1);
        frame_queue.setWork(1);
    }

    virtual ~BaseChannel() {
        if (avCodecContext) {
            avcodec_close(avCodecContext);
            avcodec_free_context(&avCodecContext);
            avCodecContext = 0;
        }
        pkt_queue.clear();
        frame_queue.clear();
        if (FFMPEG_PRINT_LOG)
            LOGE("释放channel:%d %d", pkt_queue.size(), frame_queue.size());
    };

    virtual void play()=0;

    virtual void stop()=0;

    SafeQueue<AVPacket *> pkt_queue;
    SafeQueue<AVFrame *> frame_queue;
    volatile int channelId;
    bool isDecode = false;//是否在解码
    AVCodecContext *avCodecContext;
    AVRational avRational;
    JavaCallHelper *javaCallHelper;
    bool isDecodeEnd = false;//解码结束 false 没有结束  true 已经结束
};

#endif //PALYERWANGYI_BASECHANNEL_H

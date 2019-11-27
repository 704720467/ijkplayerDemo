//
// Created by zp on 2019/6/4.
//


#include "AudioChannel.h"

extern "C" {
#include <libavutil/time.h>
#include <libswresample/swresample.h>
}


AudioChannel::AudioChannel(int id, JavaCallHelper *javaCallHelper, AVCodecContext *avCodecContext,
                           AVRational avRational)
        : BaseChannel(id, javaCallHelper, avCodecContext, avRational) {
    this->javaCallHelper = javaCallHelper;
    this->avCodecContext = avCodecContext;
    //根据布局获取声道数
    out_channels = av_get_channel_layout_nb_channels(AV_CH_LAYOUT_STEREO);
    out_samplesize = av_get_bytes_per_sample(AV_SAMPLE_FMT_S16);
    out_sample_rate = 44100;
    //CD音频标准
    //44100 双声道 2字节    out_samplesize  16位  2个字节   out_channels  2
    buffer = (uint8_t *) malloc(out_sample_rate * out_samplesize * out_channels);
    //avCodecContext  是我的解码器上下文
    if (avCodecContext->channels > 0 && avCodecContext->channel_layout == 0) { //有声道数没有声道布局，所以要设置声道布局
        avCodecContext->channel_layout = av_get_default_channel_layout(avCodecContext->channels);//设置声道布局
    } else if (avCodecContext->channels == 0 && avCodecContext->channel_layout > 0) {//有声道布局没有声道数，所以要设置声道数
        avCodecContext->channels = av_get_channel_layout_nb_channels(avCodecContext->channel_layout);
    }
    //格式转换配置
    swrContext = swr_alloc_set_opts(0, AV_CH_LAYOUT_STEREO, AV_SAMPLE_FMT_S16,
                                    out_sample_rate,
                                    avCodecContext->channel_layout,
                                    avCodecContext->sample_fmt,
                                    avCodecContext->sample_rate, 0, 0);
    //    初始化转换器其他的默认参数
    swr_init(swrContext);

}

void *decodeAudio(void *args) {
    AudioChannel *videoChannel = static_cast<AudioChannel *>(args);
    videoChannel->decodePacketAudio();
    return 0;
}

void *synchronizeAudio(void *args) {
    AudioChannel *videoChannel = static_cast<AudioChannel *>(args);
    videoChannel->synchronizeFrameAudio();
    return 0;
}

void AudioChannel::pause() {
//    isDecode = false;
//    pkt_queue.setWork(0);
//    frame_queue.setWork(0);
}

void AudioChannel::clearCache() {
    if (!isDecode)return;
    AVPacket *packet = 0;
    while (!pkt_queue.empty()) {
        int ret = pkt_queue.deQueue(packet);
        if (!ret) {
            continue;
        }
        releaseAvPacket(packet);
    }

    AVFrame *frame = 0;
    while (!frame_queue.empty()) {
        int ret = frame_queue.deQueue(frame);
        if (!ret) {
            continue;
        }
        releaseAvFrame(frame);
    }
}

void AudioChannel::play() {
    if (FFMPEG_PRINT_LOG)
        LOGE("=======》AudioChannel::play() 开始isDecode=%d", isDecode);
    if (isDecode)return;
    pkt_queue.setWork(1);
    frame_queue.setWork(1);
//    pkt_queue.clear();
//    frame_queue.clear();
    isDecode = true;
    if (FFMPEG_PRINT_LOG)
        LOGE("=======》AudioChannel::play() 启动成功isDecode=%d", isDecode);
    pthread_create(&pid_video_play, NULL, decodeAudio, this);
    if (FFMPEG_PRINT_LOG)
        LOGE("=======》AudioChannel::play() 结束");
//    pthread_create(&pid_synchronize, NULL, synchronizeAudio, this);
}

void AudioChannel::stop() {

}

void AudioChannel::release() {
    isDecode = false;
    clearCache();
    pkt_queue.setWork(0);
    frame_queue.setWork(0);
    if (swrContext)
        swr_free(&swrContext);
    if (avCodecContext)
        avcodec_close(avCodecContext);
    if (buffer) {
        free(buffer);
        buffer = NULL;
    }
}


void AudioChannel::decodePacketAudio() {
    if (FFMPEG_PRINT_LOG)
        LOGE("=======》decodePacketAudio()，开始解码");
//子线程
    AVPacket *packet = 0;
    while (isDecode) {
        if (!isDecode) {
            break;
        }
        while (frame_queue.size() > 50 && isDecode) {
            if (FFMPEG_PRINT_LOG)
                LOGE("Frame 解码过快 等 frame_queue.size()=%d", frame_queue.size());
            av_usleep(1000 * 10);
            continue;
        }
        int ret = pkt_queue.deQueue(packet);
        if (!ret) {
            releaseAvPacket(packet);
            continue;
        }
        if (FFMPEG_PRINT_LOG)
            LOGE("=======》decodePacketAudio()，解码中");
        ret = avcodec_send_packet(avCodecContext, packet);
        releaseAvPacket(packet);
        if (ret == AVERROR(EAGAIN)) {
            continue;
        } else if (ret < 0) {
            isDecode = false;
            if (FFMPEG_PRINT_LOG)
                LOGE("=======》decodePacketAudio()，退出解码~");
            break;
        }
        AVFrame *frame = av_frame_alloc();
        ret = avcodec_receive_frame(avCodecContext, frame);
        frame_queue.enQueue(frame);
    }
    releaseAvPacket(packet);
    if (FFMPEG_PRINT_LOG)
        LOGE("=======》decodePacketAudio()，解码结束");
//    pkt_queue.clear();
//    LOGE("pkt_queue 清空数据完毕。。。");
}


void AudioChannel::synchronizeFrameAudio() {
    AVFrame *frame = 0;
    float audioUnit = (float) avRational.num / (float) avRational.den;
    while (isDecode) {
        if (!isDecode) {
            break;
        }
        int ret = frame_queue.deQueue(frame);
        if (!ret) {
            continue;
        }
        uint64_t dst_nb_samples = av_rescale_rnd(
                swr_get_delay(swrContext, frame->sample_rate) + frame->nb_samples,
                out_sample_rate,
                frame->sample_rate,
                AV_ROUND_UP);
        // 转换，返回值为转换后的sample个数  buffer malloc（size）
        int nb = swr_convert(swrContext, &buffer, dst_nb_samples,
                             (const uint8_t **) frame->data, frame->nb_samples);
        double pts = audioUnit * frame->pts * 1000000;
        int data_size = nb * out_channels * out_samplesize;
        javaCallHelper->audioCallBack(THREAD_CHILD, buffer, data_size, pts);
        releaseAvFrame(frame);
    }
    //播放完成回调
    javaCallHelper->onFinish(THREAD_CHILD);
//    av_freep(&out_buffer);
//    avcodec_close(avCodecContext);
    isDecode = false;
    releaseAvFrame(frame);
    swr_free(&swrContext);
//    frame_queue.clear();
//    LOGE("frame_queue 清空数据完毕。。。");
}


jobject AudioChannel::getData() {
    //解码完成播放完毕回到
//    if (javaCallHelper && isDecodeEnd && pkt_queue.empty() && frame_queue.empty()) {
//        javaCallHelper->onFinish(THREAD_MAIN);
//    }
    AVFrame *frame = 0;
    int ret = frame_queue.deQueue(frame);
    if (!ret) {
        releaseAvFrame(frame);
        return NULL;
    }
    uint64_t dst_nb_samples = av_rescale_rnd(
            swr_get_delay(swrContext, frame->sample_rate) + frame->nb_samples,
            out_sample_rate,
            frame->sample_rate,
            AV_ROUND_UP);
    if (!buffer)
        return NULL;
//    // 转换，返回值为转换后的sample个数  buffer malloc（size）
    int nb = swr_convert(swrContext, &buffer, dst_nb_samples,
                         (const uint8_t **) frame->data, frame->nb_samples);
//    float audioUnit = (float) avRational.num / (float) avRational.den;
//    double pts = audioUnit * frame->pts * 1000000;
    double pts = frame->pts * av_q2d(avRational) * 1000000;
    int data_size = nb * out_channels * out_samplesize;
    releaseAvFrame(frame);
    int isDecodeEndState = 0;
    //解码完成播放完毕回到
    if (javaCallHelper && isDecodeEnd && pkt_queue.empty() && frame_queue.empty()) {
        isDecodeEndState = 1;
    }
    return javaCallHelper->getAudioDataNew(THREAD_CHILD, buffer, data_size, pts, isDecodeEndState);
}




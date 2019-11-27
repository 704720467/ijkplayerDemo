//
// Created by zp on 2019/6/6.
//

#include "VNIFFmpeg.h"

#include "JavaCallHelper.h"
#include "macro.h"

void *prepareFFmpeg_(void *args) {
    VNIFFmpeg *wangYiFFmpeg = static_cast<VNIFFmpeg *>(args);
    wangYiFFmpeg->prepareFFmpeg();
    return 0;
}

VNIFFmpeg::VNIFFmpeg(JavaCallHelper *javaCallHelper, const char *dataSource, long startTime) {
    url = new char[strlen(dataSource) + 1];
    this->javaCallHelper = javaCallHelper;
    this->startTimeUs = startTime;
    strcpy(url, dataSource);
}

VNIFFmpeg::~VNIFFmpeg() {

}

void VNIFFmpeg::setSeekTime(long seekTimeUs) {
    //需要重新播放
    this->seekTime = seekTimeUs;
//    if (isPlaying)
//        pthread_join(pid_play, 0);
    isPlaying = false;
    //清空释放缓存数据
    if (audioChannel) {
        audioChannel->stopWork();
//        audioChannel->clearCache();
        audioChannel->pkt_queue.clear();
        audioChannel->frame_queue.clear();
        audioChannel->startWork();
    }
}


void VNIFFmpeg::prepare() {
//
    pthread_create(&pid_prepare, NULL, prepareFFmpeg_, this);
}

void VNIFFmpeg::prepareFFmpeg() {
    avformat_network_init();
    formatContext = avformat_alloc_context();
    AVDictionary *opts = NULL;
    av_dict_set(&opts, "timeout", "3000000", 0);
    //强制指定AVFormatContext中AVInputFormat的。这个参数一般情况下可以设置为NULL，这样FFmpeg可以自动检测AVInputFormat。
    //输入文件的封装格式
    //av_find_input_format("avi")  rtmp
    int ret = avformat_open_input(&formatContext, url, NULL, &opts);
    if (ret != 0) {
        javaCallHelper->onError(THREAD_CHILD, FFMPEG_CAN_NOT_OPEN_URL);
        return;
    }
    //2.查找流
    if (avformat_find_stream_info(formatContext, NULL) < 0) {
        if (javaCallHelper) {
            javaCallHelper->onError(THREAD_CHILD, FFMPEG_CAN_NOT_FIND_STREAMS);
        }
        return;
    }

    for (int i = 0; i < formatContext->nb_streams; ++i) {
        AVCodecParameters *codecpar = formatContext->streams[i]->codecpar;
        AVCodec *dec = avcodec_find_decoder(codecpar->codec_id);
        if (!dec) {
            if (javaCallHelper) {
                javaCallHelper->onError(THREAD_CHILD, FFMPEG_FIND_DECODER_FAIL);
            }
            return;
        }
        AVCodecContext *codecContext = avcodec_alloc_context3(dec);
        if (!codecContext) {
            if (javaCallHelper)
                javaCallHelper->onError(THREAD_CHILD, FFMPEG_ALLOC_CODEC_CONTEXT_FAIL);
            return;
        }
        if (avcodec_parameters_to_context(codecContext, codecpar) < 0) {
            if (javaCallHelper)
                javaCallHelper->onError(THREAD_CHILD, FFMPEG_CODEC_CONTEXT_PARAMETERS_FAIL);
            return;
        }
        if (avcodec_open2(codecContext, dec, 0) != 0) {
            if (javaCallHelper)
                javaCallHelper->onError(THREAD_CHILD, FFMPEG_OPEN_DECODER_FAIL);
            return;
        }
        if (codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
            audioChannel = new AudioChannel(i, javaCallHelper, codecContext,
                                            formatContext->streams[i]->time_base);
        }
//        else if (codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
//            //视频
//            videoChannel = new VideoChannel(i, javaCallHelper, codecContext,
//                                            formatContext->streams[i]->time_base);
//            //            videoChannel->setRenderCallback(renderFrame);
//        }
    }

    //音视频都没有
//    if (!audioChannel && !videoChannel) {
//        if (javaCallHelper)
//            javaCallHelper->onError(THREAD_CHILD, FFMPEG_NOMEDIA);
//        return;
//    }

    if (!audioChannel) {
        if (javaCallHelper)
            javaCallHelper->onError(THREAD_CHILD, FFMPEG_NOMEDIA);
        return;
    }
    if (javaCallHelper)
        javaCallHelper->onParpare(THREAD_CHILD);

}

void *startThread(void *args) {

    VNIFFmpeg *ffmpeg = static_cast<VNIFFmpeg *>(args);
    ffmpeg->play();
    return 0;
}

void VNIFFmpeg::start() {
    if (isPlaying) return;
    isPlaying = true;
    if (audioChannel) {
        audioChannel->play();
    }
//    if (videoChannel) {
//        videoChannel->play();
//    }
    pthread_create(&pid_play, NULL, startThread, this);
}

void VNIFFmpeg::release() {
    isPlaying = false;
    if (audioChannel) {
        audioChannel->release();
    }
    avformat_close_input(&formatContext);
//    DELETE(audioChannel);
}


void VNIFFmpeg::pause() {
    if (audioChannel) {
        audioChannel->pause();
    }
    isPlaying = false;
}


jobject VNIFFmpeg::getAudioData() {
    if (audioChannel) {
        return audioChannel->getData();
    }
    return NULL;
}

void VNIFFmpeg::play() {
    int ret = 0;
    long realSeekTime = startTimeUs + seekTime;
    if (realSeekTime >= 0 && realSeekTime < formatContext->duration) {
        //除以1000000是因为传递过来的时间是微妙单位
        double seekTime = ((double) realSeekTime / (double) 1000000) * AV_TIME_BASE;
//                          + (double) formatContext->start_time;
        int state = av_seek_frame(formatContext, -1, seekTime, AVSEEK_FLAG_BACKWARD);
        LOGE("play seek state=%d，seekTime=%f", state, seekTime);
//        double seekTime = ((double) realSeekTime / (double) 1000000);
//        int64_t DstAudioDts = (int64_t) (seekTime /
//                                         av_q2d(formatContext->streams[audioChannel->channelId]->time_base));
//        int state = av_seek_frame(formatContext, audioChannel->channelId, DstAudioDts,
//                                  AVSEEK_FLAG_FRAME);
//        if (FFMPEG_PRINT_LOG)
//            LOGE("play seek state=%d，seekTime=%f,DstAudioDts=%d", state, seekTime, DstAudioDts);
    }
    seekTime = 0;
    while (isPlaying) {
        //        100帧
        if (audioChannel && audioChannel->pkt_queue.size() > 50) {
//            LOGE("packet 解码过快 等 pkt_queue.size()=%d", audioChannel->pkt_queue.size());
            av_usleep(1000 * 10);
            continue;
        }
//        if (videoChannel && videoChannel->pkt_queue.size() > 10) {
//            av_usleep(1000 * 10);
//            continue;
//        }
        //读取包
        AVPacket *packet = av_packet_alloc();
        ret = av_read_frame(formatContext, packet);
        audioChannel->isDecodeEnd = ret != 0;
        if (ret == 0) {
            if (audioChannel && packet->stream_index == audioChannel->channelId) {
                audioChannel->pkt_queue.enQueue(packet);
                if (FFMPEG_PRINT_LOG)
                    LOGE("解码出了一路：den=%d,num=%d", audioChannel->avRational.den,
                         audioChannel->avRational.num);
            } else {
                if (audioChannel)
                    audioChannel->releaseAvPacket(packet);
            }
//            else if (videoChannel && packet->stream_index == videoChannel->channelId) {
//                videoChannel->pkt_queue.enQueue(packet);
//            }
        } else if (ret == AVERROR_EOF) {
            if (audioChannel)
                audioChannel->releaseAvPacket(packet);
            //读取完毕 但是不一定播放完毕
//            if (videoChannel->pkt_queue.empty() && videoChannel->frame_queue.empty() &&
//                audioChannel->pkt_queue.empty() && audioChannel->frame_queue.empty()) {
            if (audioChannel->pkt_queue.empty() && audioChannel->frame_queue.empty()) {
                if (FFMPEG_PRINT_LOG)
                    LOGE("VNIFFmpeg 播放完毕。。。");
                break;
            }
            //因为seek 的存在，就算读取完毕，依然要循环 去执行av_read_frame(否则seek了没用...)
        } else {
            if (audioChannel)
                audioChannel->releaseAvPacket(packet);
            break;
        }
    }

    isPlaying = false;
    audioChannel->stop();
//    videoChannel->stop();

}

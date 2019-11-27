package com.zp.libvideoedit.modle;

import com.zp.libvideoedit.EditCore.VideoPlayerCoreManager;

/**
 * Create by zp on 2019-11-26
 */
public interface VideoManagerCallBack {
    void onVideoManagerReady(VideoPlayerCoreManager videoPlayerCoreManager);

    void onVideoPlaying(VideoPlayerCoreManager videoPlayerCoreManager, double percent, long currentPalyTimeInOrange);

    void onVideoPlayFinished(VideoPlayerCoreManager videoPlayerCoreManager);

    void onVideoPlayerPaused(VideoPlayerCoreManager videoPlayerCoreManager);

    //准备成功
    void onCompositionComplete();
}

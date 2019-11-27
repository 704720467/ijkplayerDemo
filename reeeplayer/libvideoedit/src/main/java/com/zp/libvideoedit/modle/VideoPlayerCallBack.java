package com.zp.libvideoedit.modle;

/**
 * Create by zp on 2019-11-26
 */
public interface VideoPlayerCallBack {
    public void onPlayerReady();

    /**
     * @param percent
     * @param currentPalyTimeInOrange 在原视频中对应的时间点
     */
    public void onPlaying(double percent, long currentPalyTimeInOrange);

    public void onPuased();

    public void onPlayFinished();
}

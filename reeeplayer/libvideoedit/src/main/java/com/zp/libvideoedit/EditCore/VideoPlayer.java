package com.zp.libvideoedit.EditCore;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.zp.libvideoedit.GPUImage.Core.GPUImageContext;
import com.zp.libvideoedit.GPUImage.Core.GPUSize;
import com.zp.libvideoedit.Time.CMTime;
import com.zp.libvideoedit.VideoEffect;
import com.zp.libvideoedit.modle.BuildType;
import com.zp.libvideoedit.modle.MediaComposition;
import com.zp.libvideoedit.modle.VideoCompositionCallBack;
import com.zp.libvideoedit.modle.VideoPlayerCallBack;
import com.zp.libvideoedit.modle.VideoTimer;

import static com.zp.libvideoedit.EditConstants.PLAY_AUDIO;
import static com.zp.libvideoedit.EditConstants.PLAY_VIDEO;
import static com.zp.libvideoedit.EditConstants.TAG;
import static com.zp.libvideoedit.EditConstants.VERBOSE;
import static com.zp.libvideoedit.EditConstants.VERBOSE_SEEK;
import static com.zp.libvideoedit.EditConstants.VERBOSE_V;
import static com.zp.libvideoedit.utils.FormatUtils.caller;
import static com.zp.libvideoedit.utils.FormatUtils.generateCallStack;

/**
 * Create by zp on 2019-11-26
 */
public class VideoPlayer implements VideoPlayerCoreManager.VideoManagerCallBack {
    private MediaComposition mediaComposition;
    private VideoTimer videoTimer; //播放器定时器
    private GLSurfaceView glSurfaceView;
    private VideoPlayerCoreManager videoPlayerCoreManager = null;
    private AudioPlayerCoreManager audioPlayerCoreManager = null;
    //    private AudioPlayerCoreManagerNew audioPlayerCoreManagernew = null;
    private Context context;
    private VideoCompositionCallBack2 compositionCallBack;
    private boolean timerStarted = false;
    private VideoEffect videoEffect;
    private VideoPlayerCallBack callBack;
    private boolean playing = false;
    private BuildType mBuildType = BuildType.BuildType_Default;//build的类型
    private int mBuildOkCount = -1;//音视频build成功标志 0 表示build成功,出发回调，用于同时build视频和音频，回调一次
    private MediaComposition composition;

    public VideoPlayer(Context context, GLSurfaceView playerView) {
        this.glSurfaceView = playerView;
        this.context = context;
        //初始化创建timer ,创建manager
        videoTimer = new VideoTimer();
        videoPlayerCoreManager = new VideoPlayerCoreManager(context, playerView, playerView.getWidth(), playerView.getHeight(), videoTimer, this);
        if (PLAY_AUDIO) audioPlayerCoreManager = new AudioPlayerCoreManager(videoTimer);
//        if (PLAY_AUDIO) audioPlayerCoreManagernew = new AudioPlayerCoreManagerNew(videoTimer, this);
    }

    public VideoPlayer(Context context, GLSurfaceView playerView, VideoPlayerCallBack callBack) {
        Log.e("", " ==================创建了！VideoPlayer");
        this.glSurfaceView = playerView;
        this.context = context;
        this.callBack = callBack;
        //初始化创建timer ,创建manager
        videoTimer = new VideoTimer();
        videoPlayerCoreManager = new VideoPlayerCoreManager(context, playerView, playerView.getWidth(), playerView.getHeight(), videoTimer, this);
        if (PLAY_AUDIO) audioPlayerCoreManager = new AudioPlayerCoreManager(videoTimer);
//        if (PLAY_AUDIO) audioPlayerCoreManagernew = new AudioPlayerCoreManagerNew(videoTimer, this);
    }

    public void setVideoSize(GPUSize size) {
        if (videoPlayerCoreManager != null) {
            videoPlayerCoreManager.setViewSize(size);
        }
    }

    /**
     * @param mediaComposition
     * @param videoEffect
     * @param compositionCallBack
     * @param buildType           build的类型  分为三种BuildType_AUDIO、BuildType_VIDEO、BuildType_Default
     */
    public void setMediaComposition(MediaComposition mediaComposition, VideoEffect videoEffect,
                                    VideoCompositionCallBack2 compositionCallBack, BuildType buildType) {
        this.mBuildType = buildType;
        setMediaComposition(mediaComposition, videoEffect, compositionCallBack);
    }

    @Deprecated
    public void setMediaComposition(MediaComposition mediaComposition, VideoEffect videoEffect) {
        setMediaComposition(mediaComposition, videoEffect, null);
    }

    @Deprecated
    public void setMediaComposition(MediaComposition mediaComposition, VideoEffect videoEffect,
                                    VideoCompositionCallBack2 compositionCallBack) {
        this.compositionCallBack = compositionCallBack;
        if (VERBOSE)
            Log.i(TAG, caller() + "VideoPlayer_setMediaComposition  PLAY_LIFECYCLE " + ", playing state:" + playing + generateCallStack());
        if (playing) {
            String errMsg = "invalidate playing status. playing should be false" + ", playing state:" + playing;
            if (VERBOSE) errMsg = caller() + errMsg + generateCallStack();
            Log.e(TAG, errMsg);
//            throw new EffectRuntimeException(errMsg);
            pause();

        }
        videoTimer.cancel();
        this.mediaComposition = mediaComposition;
        if (PLAY_AUDIO) if (audioPlayerCoreManager != null) {
            audioPlayerCoreManager.setMediaComposition(mediaComposition, videoTimer);
            //TODO 根据产品需求设置默认音量
//            audioPlayerCoreManager.setVolume(0.8f, 0.2f, 0f);
        }
        mBuildOkCount = mBuildType == BuildType.BuildType_Default ? 2 : 1;
//        if (PLAY_AUDIO && mBuildType != BuildType.BuildType_VIDEO && audioPlayerCoreManagernew != null) {
//            audioPlayerCoreManagernew.setMediaComposition(videoEffect.getAudioChunks(), videoTimer);
//            //TODO 根据产品需求设置默认音量
////            audioPlayerCoreManager.setVolume(0.8f, 0.2f, 0f);
//        }

        if (videoPlayerCoreManager != null && mBuildType != BuildType.BuildType_AUDIO) {
            videoPlayerCoreManager.setMediaComposition(mediaComposition, videoTimer, videoEffect);
        }
        timerStarted = false;

        if (VERBOSE) {
            mediaComposition.prettyPrintLog();
        }
    }

    /**
     * 视频滤镜截图
     *
     * @param width
     * @param callBack
     */
    public void screenshotForFilter(int width, VideoCompositionCallBack callBack) {
        if (videoPlayerCoreManager != null) {
            videoPlayerCoreManager.screenshotForFilter(width, callBack);
        }
    }

    /**
     * 正常播放
     */
    public synchronized void play() {
        toPlay(false);
    }


    /**
     * 静音播放
     */
    public synchronized void playQuiet() {
        toPlay(true);
    }

    private void toPlay(boolean beQuiet) {

        try {
            Thread.sleep(80);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (VERBOSE)
            Log.i(TAG, caller() + "VideoPlayer_play  PLAY_LIFECYCLE " + ", playing state:" + playing + ", " + generateCallStack());
        if (playing) {
            String errMsg = "invalidate playing status. playing should be false" + ", playing state:" + playing;
            if (VERBOSE) errMsg = caller() + errMsg + generateCallStack();
            Log.w(TAG, errMsg);
        }
        playing = true;
        if (PLAY_VIDEO) videoPlayerCoreManager.start();
        if (PLAY_AUDIO) {
            audioPlayerCoreManager.setBeQuiet(beQuiet);
            audioPlayerCoreManager.start();
//            audioPlayerCoreManagernew.setBeQuiet(beQuiet);
//            audioPlayerCoreManagernew.start();
        }
        timeStart();
    }


    private void timeStart() {
        if (timerStarted = false) {
            timerStarted = true;
            videoTimer.start();
        } else {
            videoTimer.resume();
        }
    }

    public synchronized void pause() {
        pause(false);
    }

    public synchronized void pause(boolean firstAudio) {
        if (VERBOSE)
            Log.i(TAG, caller() + "VideoPlayer_pause. " + ", playing state:" + playing + ", " + generateCallStack());
        if (!playing) {
            String errMsg = "invalidate playing status. playing should be true" + ", playing state:" + playing;
            if (VERBOSE) errMsg = caller() + errMsg + generateCallStack();
            Log.w(TAG, errMsg);
        }
        playing = false;
        videoTimer.pause();
        if (firstAudio) {
            if (PLAY_AUDIO) audioPlayerCoreManager.pause();
//            if (PLAY_AUDIO) audioPlayerCoreManagernew.pause();
            if (PLAY_VIDEO) videoPlayerCoreManager.pause();
        } else {
            if (PLAY_VIDEO) videoPlayerCoreManager.pause();
            if (PLAY_AUDIO) audioPlayerCoreManager.pause();
//            if (PLAY_AUDIO) audioPlayerCoreManagernew.pause();
        }
        if (callBack != null) callBack.onPuased();
    }


    public void seekTo(CMTime time) {
        seekTo(time, true);
    }

    public synchronized boolean isPlaying() {
        return playing;
    }

    public void seekTo(CMTime time, boolean updatePreview) {
        if (VERBOSE_SEEK)
            Log.i(TAG, caller() + "VideoPlayer_seekTo:" + time.getUs()
                    + " PLAY_LIFECYCLE | " + time.toString()
                    + ", playing state:" + playing
                    + ", updatePreview:" + updatePreview
                    + ", " + generateCallStack());
        pause();
        if (PLAY_VIDEO) videoPlayerCoreManager.seekTo(time, updatePreview);
        if (PLAY_AUDIO) audioPlayerCoreManager.seekTo(time);
//        if (PLAY_AUDIO) audioPlayerCoreManagernew.seekTo(time);
        videoTimer.seekTime(time.getMs());
    }

//    public synchronized void resume() {
//        if (VERBOSE)
//            Log.i(TAG, caller() + "VideoPlayer_resume " + ", playing state:" + playing + FormatUtils.generateCallStack());
//
//        playing = true;
//
//        videoTimer.start();
//        if (PLAY_VIDEO) videoPlayerCoreManager.resume();
//        if (PLAY_AUDIO) audioPlayerCoreManager.resume();
//
//    }

    public synchronized void stop() {
        if (VERBOSE)
            Log.i(TAG, "VideoPlayer_stop PLAY_LIFECYCLE " + ", playing state:" + playing);
        videoTimer.cancel();
        if (PLAY_VIDEO) videoPlayerCoreManager.stop();
        if (PLAY_AUDIO) audioPlayerCoreManager.stop();
//        if (PLAY_AUDIO) audioPlayerCoreManagernew.stop();

    }

//    /**
//     * 设置三个通道的音量。三个音量和为1
//     *
//     * @param mainVolume 原视频音量0.0~1.0
//     * @param bgmVolume  背景乐音量0.0~1.0
//     * @param recVolume  录音0.0~1.0
//     */
//    public void setVolume(float mainVolume, float bgmVolume, float recVolume) {
//        if (PLAY_AUDIO) audioPlayerCoreManager.setVolume(mainVolume, bgmVolume, recVolume);
//    }

    public void renderRequest() {
        videoPlayerCoreManager.requestRender();
    }

    public CMTime currentTime() {
        return videoTimer.getcCurrentTime();
    }

//    /**
//     * 音频build完毕回调
//     */
//    @Override
//    public void onAudioPrepareOk() {
//        //TODO  Build成功回调处理
//        if (VERBOSE_A)
//            Log.d(TAG_A, "所有音频准备完毕~");
//        mBuildOkCount--;
//        if (mBuildOkCount < 1 && compositionCallBack != null)
//            ((Activity) context).runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    if (compositionCallBack != null) compositionCallBack.setCompositionComplete();
//                }
//            });
//    }


    @Override
    public void onVideoManagerReady(VideoPlayerCoreManager videoPlayerCoreManager) {
//        if (VERBOSE)
        Log.i(TAG, caller() + "VideoPlayer_onReady, PLAY_LIFECYCLE callBack=" + callBack + "videoPlayerCoreManager:" + videoPlayerCoreManager + ", playing state:" + playing + generateCallStack());
//        seekTo(CMTime.zeroTime());
        if (callBack != null) callBack.onPlayerReady();
    }

    /**
     * @param videoPlayerCoreManager
     * @param percent                 进度百分比
     * @param currentPalyTimeInOrange 在原视频中对应的时间点
     */
    @Override
    public void onVideoPlaying(VideoPlayerCoreManager videoPlayerCoreManager, double percent, long currentPalyTimeInOrange) {
        if (VERBOSE_V)
            Log.d(TAG, caller() + "VideoPlayer_onVideoPlaying,percent:" + percent + ", playing state:" + playing + "callBack=" + callBack + generateCallStack());
        if (callBack != null) callBack.onPlaying(percent, currentPalyTimeInOrange);
//        audioPlayerCoreManagernew.wakeupDecoderThreadsIfNeed();
    }

    @Override
    public void onVideoPlayFinished(VideoPlayerCoreManager videoPlayerCoreManager) {
        if (VERBOSE)
            Log.i(TAG, caller() + "VideoPlayer_onVideoPlayFinishedcallBack=" + callBack + ", playing state:" + playing + generateCallStack());
        playing = false;
        if (callBack != null) callBack.onPlayFinished();


    }


    @Override
    public void onVideoPlayerPaused(VideoPlayerCoreManager videoPlayerCoreManager) {
        if (VERBOSE)
            Log.i(TAG, caller() + "VideoPlayer_onVideoPlayerPaused" + callBack + ", playing state:" + playing + generateCallStack());

        if (callBack != null) callBack.onPuased();
        playing = false;
    }

    @Override
    public void onCompositionComplete() {
        if (VERBOSE)
            Log.i(TAG, caller() + "VideoPlayer_onCompositionComplete" + compositionCallBack + generateCallStack());
        mBuildOkCount--;
//        if (mBuildOkCount < 1 && compositionCallBack != null)
        if (compositionCallBack != null)
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (compositionCallBack != null) compositionCallBack.setCompositionComplete();
                }
            });
    }

    @Override
    public void drawCostTime(long startTime, long endTime) {
        if (compositionCallBack != null) compositionCallBack.drawCostTime(startTime, endTime);
    }

    public interface VideoCompositionCallBack2 {
        void setCompositionComplete();

        void drawCostTime(long startTime, long endTime);
    }

    public void release() {
        if (glSurfaceView != null) {
            glSurfaceView.queueEvent(new Runnable() {
                @Override
                public void run() {
                    GPUImageContext.sharedFramebufferCache().purgeAllUnassignedFramebuffers();
                }
            });
        }

        if (videoPlayerCoreManager != null)
            videoPlayerCoreManager.release();
        if (audioPlayerCoreManager != null)
            audioPlayerCoreManager.release();

//        Observable.create(new Observable.OnSubscribe<String>() {
//            @Override
//            public void call(Subscriber<? super String> subscriber) {
//                if (videoPlayerCoreManager != null)
//                    videoPlayerCoreManager.release();
//                if (audioPlayerCoreManager != null)
//                    audioPlayerCoreManager.release();
////                if (audioPlayerCoreManagernew != null)
////                    audioPlayerCoreManagernew.release();
//            }
//        }).subscribeOn(Schedulers.io())
//                .observeOn(Schedulers.io())
//                .subscribe(new Subscriber<String>() {
//                    @Override
//                    public void onCompleted() {
//
//                    }
//
//                    @Override
//                    public void onError(Throwable e) {
//                        Log.e(TAG, "Close Manager Error!", e);
//                    }
//
//                    @Override
//                    public void onNext(String s) {
//
//                    }
//                });
    }
}
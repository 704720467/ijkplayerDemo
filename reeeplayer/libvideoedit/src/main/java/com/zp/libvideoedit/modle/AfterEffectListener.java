package com.zp.libvideoedit.modle;

import android.graphics.Bitmap;

import com.zp.libvideoedit.VideoEffect;
import com.zp.libvideoedit.exceptions.EffectException;

import java.util.ArrayList;


/**
 * SDK使用的回调接口。所有的回调方法在子线执行，需要使用handler处理
 *
 * @author guoxian
 * @author guoxian
 * @author guoxian
 * @author guoxian
 */
/**
 * @author guoxian
 *
 */

/**
 * @author guoxian
 *
 */
public interface AfterEffectListener {
    /**
     * 开始播放回调
     *
     * @param afterEffcet
     */
    public void onStartToPlay(VideoEffect afterEffcet);

    /**
     * 播放中更新进度回调。在子线程执行
     *
     * @param afterEffcet
     * @param currentSec
     *            当前播放的时间, 单位秒
     * @param totalSec
     *            总时长, 单位秒 *
     *
     * @param chunkIndex
     *            当前播放的chunk index
     *
     * @param currentChunkSec
     *            当前Chunk播放的时间。单位
     */

    public void onPlaying(VideoEffect afterEffcet, float currentSec, float totalSec, int chunkIndex, float currentChunkSec);

    /**
     * 此接口参数同onPlaying。在播放过程中，如果chunk播放完毕，发送的回调接口
     *
     * @param currentSec
     * @param totalSec
     * @param chunkIndex
     * @param currentChunkSec
     */
    public void onplayingChunkEnd(float currentSec, float totalSec, int chunkIndex, float currentChunkSec);

    /**
     * 播放完成时候回调
     *
     * @param afterEffcet
     */
    public void onPlayFinished(VideoEffect afterEffcet);

    /**
     * 播放暂停时候回调
     *
     * @param afterEffcet
     */
    public void onPlayPaused(VideoEffect afterEffcet);

    /**
     * 暂停后再次播放的回调
     *
     * @param afterEffect
     */
    public void onPlayResume(VideoEffect afterEffect);

    /**
     * 播放失败回调
     *
     * @param afterEffcet
     * @param e
     */
    public void onPlayingFailed(VideoEffect afterEffcet, EffectException e);

    /**
     * 开始导出回调
     *
     * @param afterEffcet
     */
    public void onStartToExport(VideoEffect afterEffcet);

    /**
     * 导出进度回调
     *
     * @param afterEffcet
     * @param rate
     *            导出的百分比
     */
    public void onExporting(VideoEffect afterEffcet, float rate);

    /**
     * 导出完成
     *
     * @param afterEffcet
     */
    /**
     * @param afterEffcet
     * @param path
     * @param successed
     *            true 正常的导出结束,非正常的导出结束(比如停止导出)
     */
    public void onExportFinished(VideoEffect afterEffcet, String path, boolean successed);

    /**
     * 导出失败回调
     *
     * @param afterEffcet
     * @param e
     */
    public void onExportFailed(VideoEffect afterEffcet, EffectException e);

    /**
     * 异步线程调用创建缩略图的回调
     *
     * @param ae
     * @param c
     * @param chunkThumbs
     */
    public void onGeneratedThumbs(VideoEffect ae, Chunk chunk);

    /**
     * 异步线程调用创建缩略图的回调
     *
     * @param ae
     * @param c
     * @param chunkThumbs
     */
    public void onGeneratedThumbsFailed(VideoEffect ae, Chunk chunk);

    /**
     * 当新增一个片段成功完成后的回调。新增片段会检查视频文件的gop,fps,等，如果从视频的元信息获取不了，需要解码一定数量的帧数来计算获得。
     * 回调方法在解码在子线程返回，需要在主线程处理
     *
     * @param self
     * @param project
     * @param chunk
     */
    public void onChunkAddedFinished(VideoEffect self, AVProject project, Chunk chunk);

    public void onChunkAddedFinished(VideoEffect self, AVProject project, ArrayList<Chunk> chunks);

    /**
     *
     * 添加一个视频片段失败的回调方法
     *
     * @param self
     * @param project
     * @param filePath
     */
    public void onChunkAddedFailed(VideoEffect self, AVProject project, String filePath);

    /**
     * 准备开始切换背景乐播放
     *
     * @param self
     */
    public void onReplayWithMusicStrarting(VideoEffect self);

    /**
     * 开始使用背景重新播放
     *
     * @param self
     */
    public void onReplayWithMusicStrarted(VideoEffect self);

    /**
     *
     * 使用背景重新播放失败
     *
     * @param self
     */
    public void onReplayWithMusicFailed(VideoEffect self);

    public interface VideoTrackListener {
        void onPlaying(float currentSec, float totalSec, int chunkIndex, float currentChunkSec);

        void onplayingChunkEnd(float currentSec, float totalSec, int chunkIndex, float currentChunkSec);

        void onPlayFinished();

        void onExporting(float rate);

        void onExportStarted();

        void onExportFailed(Exception e);

        void onExportFinished(String filePath, boolean successed);

        void setSum(long totalFrames);

        void onPlayPaused();

        void onPlayStart();

        void onPlayResume();

        void onPlayingFailed(EffectException e);
    }

    public interface ExportListener {
        void onExportStarted();

        void onExporting(float rate);

        void onExportFailed(Exception e);

        void onExportFinished(String filePath, boolean successed);
    }

    /**
     * 生成视频缩略图的回调接口，用于发布时选择封面
     * @author guoxian
     *
     */
    public interface GenerateCoverThumbsListener {

        void onGeneratedThumbs(VideoEffect self, String filePath, ArrayList<Bitmap> bitmapList);

        void onGeneratedThumbsFailed(VideoEffect self, String filePath);

    }

    public interface GenerateFilteredCoverListener {

        void onGeneratedFilteredCover(VideoEffect self, String filterName, Bitmap filteredBitmap);

        void onGeneratedFilteredCoverFailed(VideoEffect self, String filterName);

    }

}
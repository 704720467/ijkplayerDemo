package com.zp.libvideoedit;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;


import com.zp.libvideoedit.GPUImage.Core.AndroidDispatchQueue;
import com.zp.libvideoedit.GPUImage.Core.GPUImageContext;
import com.zp.libvideoedit.GPUImage.Core.GPUSize;
import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageFilter;
import com.zp.libvideoedit.Time.CMTime;
import com.zp.libvideoedit.Time.CMTimeMapping;
import com.zp.libvideoedit.Time.CMTimeRange;
import com.zp.libvideoedit.Transcoder.InvertedTranscoder;
import com.zp.libvideoedit.exceptions.InvalidVideoSourceException;
import com.zp.libvideoedit.modle.AVProject;
import com.zp.libvideoedit.modle.AudioChunk;
import com.zp.libvideoedit.modle.AudioFile;
import com.zp.libvideoedit.modle.AudioMixInputParameter;
import com.zp.libvideoedit.modle.AudioMixParam;
import com.zp.libvideoedit.modle.Chunk;
import com.zp.libvideoedit.modle.ChunkScreenActionType;
import com.zp.libvideoedit.modle.ChunkType;
import com.zp.libvideoedit.modle.MediaComposition;
import com.zp.libvideoedit.modle.MediaTrack;
import com.zp.libvideoedit.modle.MediaType;
import com.zp.libvideoedit.modle.MusicModelBean;
import com.zp.libvideoedit.modle.PaletteType;
import com.zp.libvideoedit.modle.RecodeModel;
import com.zp.libvideoedit.modle.Segment;
import com.zp.libvideoedit.modle.StickerConfig;
import com.zp.libvideoedit.modle.TimeScaleModel;
import com.zp.libvideoedit.modle.TrackType;
import com.zp.libvideoedit.modle.Transition.Origentation;
import com.zp.libvideoedit.modle.Transition.TransitionStyle;
import com.zp.libvideoedit.modle.Transition.VNITransitionFactory;
import com.zp.libvideoedit.modle.VideoFile;
import com.zp.libvideoedit.modle.VideoLastBean;
import com.zp.libvideoedit.modle.VideoSegment;
import com.zp.libvideoedit.modle.ViewportRange;
import com.zp.libvideoedit.modle.effectModel.EffectAdapter;
import com.zp.libvideoedit.modle.effectModel.EffectType;
import com.zp.libvideoedit.modle.script.ScriptVideoModel;
import com.zp.libvideoedit.utils.Common;
import com.zp.libvideoedit.utils.EffectAdapterSortByCMTime;
import com.zp.libvideoedit.utils.FrameExtratorUtil;
import com.zp.libvideoedit.utils.LogUtil;
import com.zp.libvideoedit.utils.SortCMTimeRangeByStartTime;
import com.zp.libvideoedit.utils.SortRecoderByAtTime;
import com.zp.libvideoedit.utils.SortSegmentByTargetTimeRange;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static com.zp.libvideoedit.EditConstants.MAX_TIME_DIFF_SEC;
import static com.zp.libvideoedit.EditConstants.VERBOSE;
import static com.zp.libvideoedit.EditConstants.VERBOSE_TR;

/**
 * Created by gwd on 2018/3/8.
 */

/*
                           _ooOoo_
                          o8888888o
                          88" . "88
                          (| -_- |)
                          O\  =  /O
                       ____/`---'\____
                     .'  \\|     |//  `.
                    /  \\|||  :  |||//  \
                   /  _||||| -:- |||||-  \
                   |   | \\\  -  /// |   |
                   | \_|  ''\---/''  |   |
                   \  .-\__  `-`  ___/-. /
                 ___`. .'  /--.--\  `. . __
              ."" '<  `.___\_<|>_/___.'  >'"".
             | | :  `- \`.;`\ _ /`;.`/ - ` : | |
             \  \ `-.   \_ __\ /__ _/   .-` /  /
        ======`-.____`-.___\_____/___.-`____.-'======
                           `=---='
        ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
                 佛祖保佑       永无BUG
        */

public class VideoEffect {
    private AVProject mProject = null;
    private MediaComposition mediaComposition = null;
    private MediaTrack firstVideoMediaTrack = null;
    private MediaTrack secondVideoMediaTrack = null;
    private MediaTrack audioMediaTrack = null;
    private MediaTrack transitionMediaTrack = null;
    private MediaTrack maskMediaTrack = null;
    private MediaTrack maskExtMediaTrack = null;
    private MediaTrack backGroundAudioTrack = null;
    private MediaTrack recodeAudioTrack = null;
    private Context mContext;
    private String TAG = this.getClass().getSimpleName();
    private Chunk blendVideo;
    private Chunk blendExtVideo;
    private CMTime blendVideoTime;
    private boolean needRebuildVideo;
    private boolean needRebuildAudio;
    private CMTime projectDuration;
    private boolean isMVVideoAE;
    //    private VideoExportManager exportSession;
//    private AVProjectVo projectVo;
    private VideoEffectSaveEventCallback saveEventCallback;
    private boolean needSave;
    private AudioMixParam audioMixParam;
    private CMTime originDuration;
    //脚本模式下视频源信息HashMap time-media
    private HashMap mediaInfo;
    //引用脚本的ID, 原创视频为nil
    private String scriptId;
    private Chunk tailerVideo;
    private VideoLastBean videoLastBean; //片尾的图
    private boolean isEditVideo;
    private boolean isScriptVideo;
    private static final String tailerFlag = "com.vni.VideoEffect.Tailer";
    public ArrayList<ScriptVideoModel> scriptVideoModels;
    private AndroidDispatchQueue scriptRebuildQueue = AndroidDispatchQueue.dispatchQueueCreate("scriptRebuild");
    private boolean isExporting = false;

    private VideoEffectReverseCallBack mVideoEffectReverseCallBack;
    private float mMinRecordAudioLength = 0.4f;//最小录音时长，单位秒

    /**
     * 初始化videoEffect
     *
     * @param context
     * @param projectId 项目id
     */
    public VideoEffect(Context context, String projectId, boolean needSave, VideoEffectSaveEventCallback saveEventCallback) {
        mContext = context;
        needRebuildVideo = true;
        needRebuildAudio = true;
        this.saveEventCallback = saveEventCallback;
        this.needSave = needSave;
        if (mediaComposition != null) {
            mediaComposition = null;
        }
        mediaComposition = new MediaComposition();
//        if (DBManage.getInstance().getProjectById(projectId) == null) {
//            mProject = new AVProject(context, projectId, needSave);
//            mProject.setRotation(Origentation.kVideo_Unknow);
//            projectVo = mProject.getProjectVo();
//        } else {
//            projectVo = DBManage.getInstance().getProjectById(projectId);
//            mProject = projectVo.avProject(context);
//            mProject.setProjectVo(projectVo);
//            Log.e("realm", "realmPath:" + DBManage.getInstance().getDefaultRealm().getPath() + " thread " + Thread.currentThread().getId() + "  重新创建");
//            updateCanvase();
//        }
        mProject = new AVProject(context, projectId, needSave);
        mProject.setRotation(Origentation.kVideo_Unknow);
        isEditVideo = true;
        isScriptVideo = false;

        chackFileExist();

    }

    /**
     * 判断项目中视频文件是否存在
     */
    private void chackFileExist() {
//        if (projectVo == null) return;
//        for (ChunkVo chunkVo : projectVo.getChunks()) {
//            if (!FileUtils.isFileExist(chunkVo.getFilePath())) {
//                Log.e("VideoEffect", "视频文件不存在：" + chunkVo.getFilePath());
//                throw new RuntimeException("视频文件不存在！FilePath:" + chunkVo.getFilePath());
//            }
//        }
    }

    /**
     * script 界面创建 ae
     *
     * @param context
     */
    public VideoEffect(Context context) {
        needRebuildAudio = true;
        needRebuildVideo = true;
        if (mediaComposition != null) {
            mediaComposition = null;
        }
        this.needSave = false;
        mediaComposition = new MediaComposition();
        isEditVideo = false;
        isScriptVideo = true;
    }

    public static VideoEffect advancedVideoAE() {
        return null;
    }


    /**
     * 返回所有的chunk数组
     *
     * @return
     */
    public ArrayList<Chunk> chunks() {
        return mProject.getChunks();
    }

    /**
     * 获取指定位置的chunk
     *
     * @param index
     * @return
     */
    public Chunk chunkAt(int index) {
        return mProject.getChunks().get(index);
    }

    /**
     * 获取project中chunk的个数
     *
     * @return
     */
    public int chunkCount() {
        if (mProject == null) {
            return 0;
        }
        return mProject.getChunks() == null ? 0 : mProject.getChunks().size();
    }

    /**
     * 项目中插入一个chunk
     *
     * @param filePath
     * @param atIndex
     * @throws InvalidVideoSourceException
     */
    public void insertChunk(String filePath, int atIndex) throws InvalidVideoSourceException {
        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
        mProject.insertChunk(filePath, atIndex);
        needRebuildVideo = true;
        needRebuildAudio = true;
        loadChunkCoverImage();
    }


    /**
     * 项目中插入一个chunk
     *
     * @param filePath
     * @param atIndex
     * @throws InvalidVideoSourceException
     */
    public void insertChunk(String filePath, int atIndex, ChunkType chunkType) throws InvalidVideoSourceException {
        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
        mProject.insertChunk(filePath, atIndex, chunkType);
        needRebuildVideo = true;
        needRebuildAudio = true;
        loadChunkCoverImage();
    }

    /**
     * @param filePath
     * @param atIndex
     * @param timeRange
     * @throws InvalidVideoSourceException
     */
    public void insertChunk(String filePath, int atIndex, CMTimeRange timeRange) throws InvalidVideoSourceException {
        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
        mProject.insertChunk(filePath, atIndex);
        Chunk tmpChunk = mProject.getChunks().get(atIndex);
        tmpChunk.setChunkEditTimeRange(timeRange);
        tmpChunk.setOrigonTimeRange(timeRange);
        needRebuildVideo = true;
        needRebuildAudio = true;
        loadChunkCoverImage();
    }


    /**
     * 往project中追加一个chunk
     *
     * @param filePath
     * @throws InvalidVideoSourceException
     */
    public void addChunk(String filePath) throws InvalidVideoSourceException {
        if (VERBOSE) Log.d(EditConstants.TAG, "VideoEffect_addChunk:" + filePath);
        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
        mProject.addChunk(filePath);
        needRebuildVideo = true;
        needRebuildAudio = true;
        loadChunkCoverImage();
    }

    /**
     * 获取项目的封面
     */
    private void loadChunkCoverImage() {
        if (getChunks() == null || getChunks().isEmpty()) return;
        Chunk tmpChunk = getChunks().get(0);
        loadChunkBitmap(tmpChunk.getFilePath());
    }

    /**
     * 生成小脚本
     *
     * @return
     */
//    public LeastScript getLeastScript() {
//        return mProject.getLeastScript();
//    }
    private void loadChunkBitmap(String filePath) {
        if (VERBOSE) Log.d(EditConstants.TAG, "VideoEffect_loadChunkBitmap:" + filePath);
        ArrayList<Long> list = new ArrayList<>();
        list.add((long) (0.1 * EditConstants.US_MUTIPLE));
        VideoFile videoFile = null;
        try {
            videoFile = VideoFile.getVideoFileInfo(filePath, mContext);
        } catch (InvalidVideoSourceException e) {
            e.printStackTrace();
        }
        FrameExtratorUtil.getInstance().frameExtrator(filePath, list, videoFile.getWidth(),
                new FrameExtratorUtil.FrameGenerated() {
                    @Override
                    public void onFrameGenerated(final Bitmap thumb, String videoPath, String chunkId, long pts) {
                        Common.runOnMainQueueWithoutDeadlocking(new Runnable() {
                            @Override
                            public void run() {
                                setCoverImg(thumb);
                            }
                        });
                    }

                    @Override
                    public void onSuccessed() {

                    }
                });
    }

    public void addChunk(String filePath, float jingdu, float weidu, CMTimeRange chunkEditTimeRange) throws InvalidVideoSourceException {
        if (VERBOSE)
            Log.d(EditConstants.TAG, "VideoEffect_addChunk_chunkEditTimeRange:" + filePath + ", timeRange:" + chunkEditTimeRange);
        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
        Chunk chunk = new Chunk(filePath, mContext, false);
        chunk.setChunkEditTimeRange(chunkEditTimeRange);
        chunk.setLongitude(jingdu);
        chunk.setLatitude(weidu);
        mProject.addChunk(chunk);
        loadChunkCoverImage();
    }

    public void addChunk(String filePath, VideoFile videoFile, AudioFile audioFile, float jingdu, float weidu) throws InvalidVideoSourceException {
        if (VERBOSE)
            Log.d(EditConstants.TAG, "VideoEffect_addChunk_withVideo_file_audioFile:" + filePath + ", videoFile:" + videoFile + ",audioFile:" + audioFile);
        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
        mProject.addChunk(filePath, videoFile, audioFile, jingdu, weidu);
        needRebuildVideo = true;
        needRebuildAudio = true;
        loadChunkCoverImage();
    }

    public void addChunk(String filePath, VideoFile videoFile, AudioFile audioFile, float jingdu, float weidu, CMTimeRange chunkEditTimeRange) throws InvalidVideoSourceException {
        if (VERBOSE)
            Log.d(EditConstants.TAG, "VideoEffect_addChunk_withVideo_file_audioFile:" + filePath + ", videoFile:" + videoFile + ",audioFile:" + audioFile);
        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
        Chunk chunk = mProject.addChunk(filePath, videoFile, audioFile, jingdu, weidu);
        chunk.setChunkEditTimeRange(chunkEditTimeRange);
        chunk.setOrigonTimeRange(chunkEditTimeRange);
        needRebuildVideo = true;
        needRebuildAudio = true;
        loadChunkCoverImage();
    }

    /**
     * 删除chunk
     *
     * @param chunkIndex
     */
    public void deleteChunk(int chunkIndex) {
        if (VERBOSE) Log.d(EditConstants.TAG, "VideoEffect_deleteChunk:" + chunkIndex);
        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
        mProject.deleteChunk(chunkIndex);
        needRebuildVideo = true;
        needRebuildAudio = true;
        if (!getChunks().isEmpty()) {
            loadChunkCoverImage();
        } else if (getChunks().size() == 0) {
            this.setProjectOrientation(Origentation.kVideo_Unknow);
        }

    }

    /**
     * 删除chunk
     *
     * @param chunk
     */
    public void deleteChunk(Chunk chunk) {
        if (VERBOSE) Log.d(EditConstants.TAG, "VideoEffect_deleteChunk:" + chunk);
        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
        mProject.deleteChunk(chunk);
        needRebuildVideo = true;
        needRebuildAudio = true;
        loadChunkCoverImage();
    }

    /**
     * chunk分割
     *
     * @param chunkIndex
     * @param atTime     当前chunk的时间位移
     * @return
     */
    public void spliteChunk(int chunkIndex, CMTime atTime) {
        if (VERBOSE)
            Log.d(EditConstants.TAG, "VideoEffect_spliteChunk:" + chunkIndex + ", atTime:" + atTime);
        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
        mProject.copyChunk(chunkIndex);
        Chunk source = mProject.getChunks().get(chunkIndex);
        Chunk newChunk = mProject.getChunks().get(chunkIndex + 1);
        float separatedTime = (float) CMTime.getSecond(atTime);

//        float duration = (float) (CMTime.getSecond(source.getEndTime()) - CMTime.getSecond(source.getStartTime()));
//        float timePercent = separatedTime / duration;
//        CMTime fixedTime = new CMTime(CMTime.getSecond(source.getChunkEditTimeRange().getDuration()) * timePercent);
        float duration = (float) CMTime.getSecond(source.getChunkEditTimeRange().getDuration());
        float timePercent = separatedTime / duration;
        CMTime fixedTime = new CMTime(duration * timePercent);
        CMTime absoluteSeparateTime = CMTime.addTime(source.getChunkEditTimeRange().getStartTime(), fixedTime);
        newChunk.setOrigonTimeRange(CMTimeRange.CMTimeRangeTimeToTime(absoluteSeparateTime, source.getOrigonTimeRange().getEnd()));
        newChunk.setChunkEditTimeRange(CMTimeRange.CMTimeRangeTimeToTime(absoluteSeparateTime, source.getChunkEditTimeRange().getEnd()));
        newChunk.setCropStart(0);
        newChunk.setCropEnd(1 - ((float) CMTime.getSecond(newChunk.getChunkEditTimeRange().getDuration()) * 1.0f / (float) CMTime.getSecond(newChunk.getOrigonTimeRange().getDuration())));
        source.setOrigonTimeRange(CMTimeRange.CMTimeRangeTimeToTime(source.getOrigonTimeRange().getStartTime(), absoluteSeparateTime));
        source.setChunkEditTimeRange(CMTimeRange.CMTimeRangeTimeToTime(source.getChunkEditTimeRange().getStartTime(), absoluteSeparateTime));
        float tmpCrop = (float) CMTime.getSecond(source.getChunkEditTimeRange().getStartTime()) - (float) CMTime.getSecond(source.getOrigonTimeRange().getStartTime());
        source.setCropStart(tmpCrop * 1.0f / (float) CMTime.getSecond(source.getOrigonTimeRange().getDuration()));
        this.needRebuildAudio = true;
        this.needRebuildVideo = true;
    }

    /**
     * chunk的裁剪
     *
     * @param chunkIndex
     * @param timeRange
     */
    public void resizeChunk(int chunkIndex, CMTimeRange timeRange) {
        if (VERBOSE)
            Log.d(EditConstants.TAG, "VideoEffect_resizeChunk:" + chunkIndex + ", timeRange:" + timeRange);
        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
        Chunk chunk = mProject.getChunks().get(chunkIndex);
        if (chunk.isReverseVideo()) {
            CMTime duration = timeRange.getDuration();
            CMTime startTime = CMTime.subTime(chunk.getVideoFile().getcDuration(), timeRange.getEnd());
            timeRange = new CMTimeRange(startTime, duration);
        }
        chunk.setChunkEditTimeRange(timeRange);
    }

    /**
     * chunk复制
     *
     * @param chunkIndex
     */
    public void cloneChunk(int chunkIndex) {
        if (VERBOSE) Log.d(EditConstants.TAG, "VideoEffect_cloneChunk:" + chunkIndex);
        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
        mProject.copyChunk(chunkIndex);
        needRebuildVideo = true;
        needRebuildAudio = true;
    }

    /**
     * chunk设置变焦
     *
     * @param chunkIndex
     * @param screenTypeValue
     */
    public void setChunkScreenAction(final int chunkIndex, int screenTypeValue) {
        Chunk chunk = mProject.getChunks().get(chunkIndex);
        if (chunk != null) {
            ChunkScreenActionType screenType = ChunkScreenActionType.getScreenType(screenTypeValue);
            chunk.setScreenType(screenType);
        }
    }

    /**
     * 交换两个chunk的位置
     *
     * @param sourceIndex
     * @param toIndex
     */
    public void exchangeChunk(int sourceIndex, int toIndex) {
        if (VERBOSE)
            Log.d(EditConstants.TAG, "VideoEffect_exchangeChunk_sourceIndex:" + sourceIndex + ", toIndex" + toIndex);
        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
        mProject.exchangeChunk(sourceIndex, toIndex);
        needRebuildAudio = true;
        needRebuildVideo = true;
        loadChunkCoverImage();
    }

    /**
     * chunk倒放
     *
     * @param chunkIndex
     * @param toRever    是否倒播 true倒播，false 正播
     * @param callBack   倒放的回调
     */
    public void revertChunk(int chunkIndex, boolean toRever, final ReverseCallBack callBack) {
        if (VERBOSE_TR)
            Log.d(EditConstants.TAG, "VideoEffect_revertChunk:" + chunkIndex + "；toRever=" + toRever);
        final Chunk chunk = mProject.getChunks().get(chunkIndex);
        //正播
        if (!toRever) {
            chunk.toRevertChunk(toRever, null);
            return;
        }
        //倒播
        String chunFilePath = chunk.getFilePath();
        final String reversePath = EditConstants.TEMP_REVERSE_PATH + "/" + chunFilePath.substring(chunFilePath.lastIndexOf("/") + 1).replace(".mp4", "_reverse.mp4");
        File file = new File(reversePath);
        if (!file.exists()) {
            if (VERBOSE_TR)
                Log.d(EditConstants.TAG, "VideoEffect_revertChunk_toTransCodes:reversePath=" + reversePath);
            mVideoEffectReverseCallBack = new VideoEffectReverseCallBack() {
                @Override
                public void onProcess(float process) {
                    if (callBack != null)
                        callBack.onProcess(process);
                }

                @Override
                public void onSuccess() {
                    chunk.toRevertChunk(true, reversePath);
                    if (callBack != null)
                        callBack.onSuccess();

                }

                @Override
                public void onFaild() {
                    if (callBack != null)
                        callBack.onFaild();

                }

                @Override
                public void onCancle() {
                    if (callBack != null)
                        callBack.onCancle();
                }
            };
            mVideoEffectReverseCallBack.setCancleReverse(false);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    InvertedTranscoder invertedTranscoder =
                            new InvertedTranscoder(mContext, chunk.getFilePath(), 0, (long) chunk.getVideoFile().getcDuration().getUs(), reversePath);
                    invertedTranscoder.setCallBack(mVideoEffectReverseCallBack);
                    invertedTranscoder.transCode();
                }
            }).start();

        } else {
            chunk.toRevertChunk(true, reversePath);
            if (callBack != null) {
                callBack.onProcess(1.f);
                callBack.onSuccess();
            }
        }
    }

    /**
     * 取消正在转换倒播
     */
    public void cancleRevertChunk() {
        if (mVideoEffectReverseCallBack == null) return;
        mVideoEffectReverseCallBack.setCancleReverse(true);
        if (VERBOSE_TR)
            Log.d(EditConstants.TAG, "VideoEffect_cancleRevertChunk");

    }

    public Chunk getTailerVideo() {
        return tailerVideo;
    }

    /**
     * 更新chunkcropStart
     *
     * @param chunkIndex
     * @param cropStart
     */
    public void updateChunkCropStart(int chunkIndex, float cropStart) {
        if (VERBOSE) Log.d(EditConstants.TAG, "VideoEffect_updateChunkCropStart:" + chunkIndex);
        Chunk chunk = mProject.getChunks().get(chunkIndex);
        chunk.setCropStart(cropStart);
        needRebuildVideo = true;
        needRebuildAudio = true;
    }

    /**
     * 更新chunkcropEnd
     *
     * @param chunkIndex
     * @param cropEnd
     */
    public void updateChunkCropEnd(int chunkIndex, float cropEnd) {
        Chunk chunk = mProject.getChunks().get(chunkIndex);
        chunk.setCropEnd(cropEnd);
        needRebuildVideo = true;
        needRebuildAudio = true;
    }

    /**
     * 通过时间获取chunk
     *
     * @param time
     * @return
     */
    public Chunk chunkAtTime(CMTime time) {
        ArrayList<Chunk> chunks = mProject.getChunks();
        for (Chunk chunk : chunks) {
            CMTimeRange timeRange = CMTimeRange.CMTimeRangeTimeToTime(chunk.getTransitionStart(), chunk.getTransitionEnd());
            if (CMTimeRange.CMTimeRangeContains(timeRange, time)) {
                return chunk;
            }
        }
        return null;
    }

    /**
     * 通过chunkID 获取chunk
     *
     * @param chunkId
     * @return
     */
    public Chunk chunkWithId(String chunkId) {
        ArrayList<Chunk> chunks = mProject.getChunks();
        for (Chunk chunk : chunks) {
            CMTimeRange timeRange = CMTimeRange.CMTimeRangeTimeToTime(chunk.getTransitionStart(), chunk.getTransitionEnd());
            if (chunk.getChunkId() == chunkId) return chunk;
        }
        return null;
    }

    /**
     * 设置转场
     *
     * @param chunkIndex 给制定chunkIndex前加转场,比如chunkIndex=1,在第一个和第二个chunk之间加转场
     * @param style
     */
    public void setTransition(int chunkIndex, TransitionStyle style, CMTime transitionDuration) {
        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
        Chunk chunk = mProject.getChunks().get(chunkIndex);
        chunk.setTransition(style, mProject.getRotation(), transitionDuration);
        if (EditConstants.VERBOSE_EDIT)
            Log.i(EditConstants.TAG_EDIT, "videoEffect_setTransition_project_call" + "  chunkIndex " + chunkIndex + " style: " + style + "  rotation:  " + mProject.getRotation());
    }

    /**
     * 更新调试的强度
     *
     * @param chunkIndex
     * @param paletteType
     * @param strength
     */
    public void updateColorPalette(int chunkIndex, PaletteType paletteType, float strength) {
        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
        Chunk chunk = mProject.getChunks().get(chunkIndex);
        chunk.paletteChunk(paletteType, strength);
    }

    /**
     * 跟新所有分段的调色的强度
     *
     * @param paletteType
     * @param strength
     */
    public void updateAllChunkColorPalette(PaletteType paletteType, float strength) {
        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
        for (Chunk chunk : mProject.getChunks()) {
            chunk.paletteChunk(paletteType, strength);
        }
    }

    /**
     * 跟新lookup滤镜的强度
     *
     * @param chunkIndex
     * @param strength
     */
    public void updateLutFilterStrength(int chunkIndex, float strength) {
        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
        Chunk chunk = mProject.getChunks().get(chunkIndex);
        chunk.setStrengthValue(strength);
    }

    /**
     * 更新说有分段lookup 滤镜的强度
     *
     * @param strength
     */
    public void updateAlllutFilter(float strength) {
        for (Chunk chunk : mProject.getChunks()) {
            chunk.setStrengthValue(strength);
        }
    }

    /**
     * 给chunk添加lookup滤镜
     *
     * @param chunkIndex
     * @param filterName
     * @param strength
     */
    public void addLutFilter(int chunkIndex, String filterName, float strength) {
        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
        Chunk chunk = mProject.getChunks().get(chunkIndex);
        chunk.addFilter(filterName, strength);
    }

    /**
     * 分段删除颜色lookup滤镜
     *
     * @param chunkIndex
     */
    public void removeLutFilter(int chunkIndex) {
        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
        Chunk chunk = mProject.getChunks().get(chunkIndex);
        chunk.addFilter(null, 0);
    }

    /**
     * 给所有分段添加Lut滤镜
     *
     * @param filterName
     * @param strength
     */
    public void addLutFilterAll(String filterName, float strength) {
        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
        mProject.setGloatFilterStrength(strength);
        mProject.setGlobalFilterName(filterName);
    }

    /**
     * 全局删除Lut滤镜
     */
    public void removeLutFilterAll() {
        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
        for (Chunk chunk : mProject.getChunks()) {
            chunk.addFilter(null, 0);
        }
        mProject.setGloatFilterStrength(0);
        mProject.setGlobalFilterName(null);
    }

    /**
     * 更新全局的Lut滤镜强度
     *
     * @param strength
     */
    public void updateLutStrength(float strength) {
        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
        for (Chunk chunk : mProject.getChunks()) {
            chunk.setStrengthValue(strength);
        }
        mProject.setGloatFilterStrength(strength);
    }

    /**
     * 获取全局的颜色滤镜名称
     *
     * @return
     */
    public String globalColorFilterName() {
        return mProject.getGlobalFilterName();
    }

    /**
     * 获取全局的颜色滤镜的强度
     *
     * @return
     */
    public float globalColorFilterStrength() {
        return mProject.getGloatFilterStrength();
    }


    /**
     * 删除全局 adapter
     *
     * @param flag
     */
    public void removeAdapter(String flag) {
        if (mProject.getEffectAdapters() == null) return;
        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
        EffectAdapter adapter = null;
        for (EffectAdapter effectAdapter : mProject.getEffectAdapters()) {
            if (effectAdapter.getEffectId().equals(flag)) {
                adapter = effectAdapter;
                break;
            }
        }
        if (adapter == null) return;
        mProject.removeEffect(adapter);
    }


    /**
     * 设置chunk是否禁用颜色滤镜
     *
     * @param chunkIndex
     * @param isDisable
     */
    public void setColorFilterDisable(int chunkIndex, boolean isDisable) {
        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
        Chunk chunk = mProject.getChunks().get(chunkIndex);
        chunk.setDisableColorFilter(isDisable);
    }

    /**
     * 设置全局调试滤镜是否开启
     *
     * @param isDisable
     */
    public void setAllColorFilterDisable(boolean isDisable) {
        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
        for (Chunk chunk : mProject.getChunks()) {
            chunk.setDisableColorFilter(isDisable);
        }
    }

    /**
     * 添加视频模板
     *
     * @param blendVideo
     * @param blendExtVideo
     * @param timeRange
     * @param flag
     */
    public void addVideoModule(String blendVideo, String blendExtVideo, CMTimeRange timeRange, String flag) {
        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
        try {
            Chunk blend = new Chunk(blendVideo, mContext, needSave);
            Chunk blendExt = new Chunk(blendExtVideo, mContext, needSave);
            EffectAdapter adapter = new EffectAdapter(flag, EffectType.EffectType_Video);
            adapter.setTimeRange(timeRange);
            adapter.setMaskExtVideoChunk(blendExt);
            adapter.setMaskVideoChunk(blend);
            mProject.addEffect(adapter);
        } catch (InvalidVideoSourceException e) {
            e.printStackTrace();
        }


    }

    /**
     * 更新排版模板的时间段
     *
     * @param timeRange
     * @param flag
     */
    public void updateEffectAdapterSettingTimeRange(final CMTimeRange timeRange, String flag) {
        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
        for (int i = 0; i < mProject.getEffectAdapters().size(); i++) {
            EffectAdapter adapter = mProject.getEffectAdapters().get(i);
            if (adapter.getEffectId().equals(flag)) {
                adapter.setTimeRange(timeRange);
                break;
            }
        }
        buildEffect();
//        for (EffectAdapterVo effectAdapterVo : projectVo.getEffectAdapters()) {
//            if (effectAdapterVo.getEffectId().equals(flag)) {
//                final EffectAdapterVo adapterVo = effectAdapterVo;
//                DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                    @Override
//                    public void execute(Realm realm) {
//                        adapterVo.getTimeRange().getStartTime().setValue(timeRange.getStartTime().getValue());
//                        adapterVo.getTimeRange().getStartTime().setTimeScale(timeRange.getStartTime().getTimeScale());
//                        adapterVo.getTimeRange().getDuration().setValue(timeRange.getDuration().getValue());
//                        adapterVo.getTimeRange().getDuration().setTimeScale(timeRange.getDuration().getTimeScale());
//                        realm.insertOrUpdate(adapterVo.getTimeRange());
//                    }
//                });
//                break;
//            }
//        }
    }


    /**
     * 音乐以及声音部分
     */

    /**
     * 设置chunk静音
     *
     * @param chunkIndex
     */
    public void setChunkAudioMute(int chunkIndex) {
        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
        Chunk chunk = mProject.getChunks().get(chunkIndex);
        chunk.setAudioMute(!chunk.isAudioMute());
        needRebuildAudio = true;
        needRebuildVideo = true;
    }

    /**
     * 设置某个chunk的 混音比
     *
     * @param chunkIndex
     * @param proportion
     */
    public void updateAudioMixProportionOfChunk(int chunkIndex, float proportion) throws Exception {
        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
        Chunk chunk = mProject.getChunks().get(chunkIndex);
        chunk.setAudioVolumeProportion(proportion);
        needRebuildAudio = true;
        if (mediaComposition != null) {
            buildAudioMix();
            mediaComposition.setAudioMixParam(getAudioMixParam());
        }
    }

    /**
     * 调整 全局的 调音量
     *
     * @param proportion
     */
    public void updateGlobalAudioMixProportion(float proportion) throws Exception {
        mProject.setVolumeProportion(proportion);
        for (Chunk chunk : getChunks()) {
            chunk.setAudioVolumeProportion(proportion);
        }
        needRebuildAudio = true;
        if (mediaComposition != null) {
            buildAudioMix();
            mediaComposition.setAudioMixParam(getAudioMixParam());
        }
    }

    /**
     * 添加或者修改背景音乐
     *
     * @param musicPath
     * @throws InvalidVideoSourceException
     */
    public void addBackGroundMusic(String musicPath) throws InvalidVideoSourceException {
        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
        if (musicPath == null || musicPath.length() == 0) {
            mProject.addBackGroundMusic(null);
            needRebuildAudio = true;
            needRebuildVideo = false;
            return;
        }
        mProject.addBackGroundMusic(musicPath);
        needRebuildVideo = false;
        needRebuildAudio = true;
    }

    public void addBackGroundMusic(MusicModelBean musicModel) throws InvalidVideoSourceException {
        mProject.setMusicModel(musicModel);
        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
//        if (musicModel == null) {
//            mProject.addBackGroundMusic(null);
//            needRebuildVideo = false;
//            needRebuildAudio = true;
//            return;
//        }
//        mProject.addBackGroundMusic(musicModel.getUrl());
//        needRebuildVideo = false;
//        needRebuildAudio = true;
    }

    /**
     * 获取背景音地址
     *
     * @return
     */
    public String getBackGroundMusic() {
        return mProject.getBackGroundMusicPath();
    }

    /**
     * 设置chunk的混音大小
     *
     * @param chunkIndex
     * @param proportion
     */
    public void updateChunkVolueProportion(int chunkIndex, float proportion) {
        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
        Chunk chunk = mProject.getChunks().get(chunkIndex);
        chunk.setAudioVolumeProportion(proportion);
        needRebuildAudio = true;
    }

    /**
     * 获取chunk的混音大小
     *
     * @param chunkIndex
     * @return
     */
    public float getChunkVolumeProportion(int chunkIndex) {
        Chunk chunk = mProject.getChunks().get(chunkIndex);
        return chunk.getAudioVolumeProportion();
    }

    /**
     * 设置全局的混音大小
     *
     * @param proportion
     */
    public void setGlobalVolumeProportion(float proportion) {
        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
        mProject.setVolumeProportion(proportion);
        needRebuildAudio = true;
    }

    /**
     * 获取全局的 混音大小
     */
    public float getGlobalVolumeProportion() {
        return mProject.getVolumeProportion();
    }

    /**
     * 预处理
     */
    private void preBuild() {
        if (needRebuildAudio || needRebuildVideo) {
            if (mediaComposition != null) {
                mediaComposition.removeAllTrack();
            }
            mediaComposition = new MediaComposition();
            firstVideoMediaTrack = mediaComposition.addTrack(MediaType.MEDIA_TYPE_Video, TrackType.TrackType_Video_Main);
            secondVideoMediaTrack = mediaComposition.addTrack(MediaType.MEDIA_TYPE_Video, TrackType.TrackType_Video_Second);
            transitionMediaTrack = mediaComposition.addTrack(MediaType.MEDIA_TYPE_Video, TrackType.TrackType_Video_Transition);
            audioMediaTrack = mediaComposition.addTrack(MediaType.MEDIA_TYPE_Audio, TrackType.TrackType_Main_Audio);
            backGroundAudioTrack = mediaComposition.addTrack(MediaType.MEDIA_TYPE_Audio, TrackType.TrackType_Audio_BackGround);
            maskExtMediaTrack = mediaComposition.addTrack(MediaType.MEDIA_TYPE_Video, TrackType.TrackType_Video_Mask_Ext);
            maskMediaTrack = mediaComposition.addTrack(MediaType.MEDIA_TYPE_Video, TrackType.TrackType_Video_Mask);
            recodeAudioTrack = mediaComposition.addTrack(MediaType.MEDIA_TYPE_Audio, TrackType.TrackType_Audio_Recoder);
        }
    }


    /**
     * 补全VideoTrack,插入一个空的segment,使firstVideoMediaTrack,secondVideoMediaTrack 长度一致
     *
     * @guoxian
     */
    private void complementVideoTrack() {
        if (firstVideoMediaTrack == null || firstVideoMediaTrack.getSegments() == null || firstVideoMediaTrack.getSegments().size() == 0 || secondVideoMediaTrack == null || secondVideoMediaTrack.getSegments() == null || secondVideoMediaTrack.getSegments().size() == 0) {
            //有一个track为空
            return;
        }
        if (firstVideoMediaTrack.getDuration().getSecond() - secondVideoMediaTrack.getDuration().getSecond() <= MAX_TIME_DIFF_SEC) {
            //长度一致
            return;
        }
        //TODO guoxian
    }


    public void setCoverImg(Bitmap bitmap) {
        Common.runOnMainQueueWithoutDeadlocking(new Runnable() {
            @Override
            public void run() {
                if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
            }
        });
        mProject.setCoverImage(bitmap);
    }

    //videos重新组装
    public void buildVideos() {
        GPUImageContext.sharedFramebufferCache().purgeAllUnassignedFramebuffers();
        Chunk preChunk = null;
        Chunk nextChunk = null;
        int inverter = 1;
        CMTime cursorTime = CMTime.zeroTime();
        int count = mProject.getChunks().size();
        ArrayList<MediaTrack> videoTracks = new ArrayList<MediaTrack>();
        videoTracks.add(firstVideoMediaTrack);
        videoTracks.add(secondVideoMediaTrack);
        for (int i = 0; i < count; i++) {
            inverter = 1 - inverter;
            Chunk chunk = mProject.getChunks().get(i);
            nextChunk = null;
            if (i < count - 1) {
                nextChunk = mProject.getChunks().get(i + 1);
            }
            if (i > 0) {
                preChunk = mProject.getChunks().get(i - 1);
            }
            chunk.chunkIndex = i;
            chunk.originStartTime = cursorTime;
            chunk.setStartTime(cursorTime);
            chunk.startTimeBeforeSpeed = cursorTime;
            CMTime duration = chunk.getChunkEditTimeRange().getDuration();
            //contact
            MediaTrack currentTrack = videoTracks.get(inverter);
            currentTrack.insertTrack(chunk.getVideoFile(), chunk.getChunkEditTimeRange(), cursorTime);
            if (chunk.getAudioFile() != null) {
                CMTime insertTime = cursorTime;
                CMTimeRange insertTimeRange = chunk.getChunkEditTimeRange();
                if (chunk.getTransitionStyle() != TransitionStyle.VNITransitionTypeNone) {
                    CMTime transitionTime = chunk.getChunkTransitionTime();
                    insertTime = CMTime.addTime(insertTime, new CMTime(CMTime.getSecond(transitionTime) / 2.f));
                    CMTime insertStart = CMTime.addTime(insertTimeRange.getStartTime(), new CMTime(CMTime.getSecond(transitionTime) / 2.f));
                    CMTime insertDuration = CMTime.subTime(insertTimeRange.getDuration(), new CMTime(CMTime.getSecond(transitionTime) / 2.f));
                    insertTimeRange = new CMTimeRange(insertStart, insertDuration);
                }
                if (nextChunk != null && nextChunk.getTransitionStyle() != TransitionStyle.VNITransitionTypeNone) {
                    CMTime transitionTime = nextChunk.getChunkTransitionTime();
                    CMTime insertStart = insertTimeRange.getStartTime();
                    CMTime insertDuration = CMTime.subTime(insertTimeRange.getDuration(), new CMTime(CMTime.getSecond(transitionTime) / 2.f));
                    insertTimeRange = new CMTimeRange(insertStart, insertDuration);
                }
                if (audioMediaTrack != null && chunk.getAudioFile() != null) {
                    CMTime videoEndTime = insertTimeRange.getEnd();
                    //如果插入的结束时间的位置 大于 原视频视频中音频总时长的结束位置，需要插入empty 数据
                    if (videoEndTime.getSecond() > (chunk.getAudioFile().getcDuration().getSecond() + 0.1)) {


                        //先插入音频时长 ，然后再插入一个empty时长
                        CMTime emptyDuration = CMTime.subTime(videoEndTime, chunk.getAudioFile().getcDuration());
                        CMTime realInsertDuration = CMTime.subTime(chunk.getAudioFile().getcDuration(), insertTimeRange.getStartTime());
                        CMTimeRange realAudioTimeRange = new CMTimeRange(insertTimeRange.getStartTime(), realInsertDuration);
                        audioMediaTrack.insertTrack(chunk.getAudioFile(), realAudioTimeRange, insertTime);
                        CMTime emptyStartTime = CMTime.addTime(insertTime, realAudioTimeRange.getDuration());
                        CMTimeRange emptyInsertTimeRange = new CMTimeRange(emptyStartTime, emptyDuration);
                        audioMediaTrack.insertEmpy(emptyInsertTimeRange);
                    } else {
                        audioMediaTrack.insertTrack(chunk.getAudioFile(), insertTimeRange, insertTime);
                    }
                }
            }
            cursorTime = CMTime.addTime(cursorTime, chunk.getChunkEditTimeRange().getDuration());
            chunk.setEndTime(cursorTime);
            chunk.endTimeBeforeSpeed = cursorTime;
            if (nextChunk != null && nextChunk.getTransitionStyle() != TransitionStyle.VNITransitionTypeNone) {
                CMTime transitionTime = nextChunk.getChunkTransitionTime();
                cursorTime = CMTime.subTime(cursorTime, transitionTime);
            }
            chunk.originTransitionTailTime = nextChunk != null ? nextChunk.getChunkTransitionTime() : CMTime.zeroTime();
            chunk.originTransitionHeadTime = nextChunk != null ? nextChunk.getChunkTransitionTime() : CMTime.zeroTime();
        }
        //处理变速
        float totalSec = (float) CMTime.getSecond(mediaComposition.getDuration());
        originDuration = mediaComposition.getDuration();
        //TODO 注意变速完成以后需要重新修正startTime和EndTime
//        int speedCount = speedPointsCount() - 2;
//        ArrayList<TimeScaleModel> timeScaleModels = new ArrayList<>();
//        for (int i = speedCount; i >= 0; i--) {
//            TimeScaleModel model = speedPointWithIndex(i);
//            timeScaleModels.add(model);
//        }
        if (mProject.getSpeedPoints().size() != 0) {
            mediaComposition.scaleTimeRanage(mProject.getSpeedPoints());
            //变速后segment 时间顺序调整，如果变速添加了过场会导致segment的开始结束时间和排序播放问题
//            adjustSegment();
//            adjustSegment2();
        }

        //变速处理完毕
        checkChunkStartTime();
        //处理模板
        buildEffect();

        projectDuration = mediaComposition.getDuration();
        mProject.setProjectDuration(projectDuration.getMs());
//        if (VERBOSE) mediaComposition.prettyPrintLog();
    }

    /**
     * 整体仅仅是时间后移不打乱Segment
     */
    private void adjustSegment2() {
        if (secondVideoMediaTrack == null || secondVideoMediaTrack.getSegments() == null
                || secondVideoMediaTrack.getSegments().isEmpty()) return;

        int fristTrackCurrentPosition = 0;
        int secondTrackCurrentPosition = 0;
        ArrayList<Segment> fristTrackSegmentsOld = new ArrayList<>();
        ArrayList<Segment> secondTrackSegmentsOld = new ArrayList<>();
        ArrayList<Segment> fristTrackSegmentsNew = new ArrayList<>();
        ArrayList<Segment> secondTrackSegmentsNew = new ArrayList<>();
        for (Object o : firstVideoMediaTrack.getSegments()) {
            if (!((Segment) o).isEmpty())
                fristTrackSegmentsOld.add((Segment) o);
        }
        for (Object o : secondVideoMediaTrack.getSegments()) {
            if (!((Segment) o).isEmpty())
                secondTrackSegmentsOld.add((Segment) o);
        }
        for (int i = 0; i < getChunks().size() - 1; i++) {
            if (i % 2 == 0) {
                if (i != 0) {
                    //第二路作为对比
                    fristTrackCurrentPosition = getTrackCurrentPosition(fristTrackCurrentPosition, secondTrackSegmentsNew, fristTrackSegmentsOld, fristTrackSegmentsNew);
                } else {
                    fristTrackSegmentsNew.add(fristTrackSegmentsOld.get(fristTrackCurrentPosition));
                    fristTrackCurrentPosition++;
                }
            } else {
                //第一路数据作为对比
                secondTrackCurrentPosition = getTrackCurrentPosition(secondTrackCurrentPosition, fristTrackSegmentsNew, secondTrackSegmentsOld, secondTrackSegmentsNew);
            }
        }
    }

    /**
     * @param currentPosition          当前面调整的位置
     * @param contrastTrackSegmentsNew 对比路
     * @param currentTrackSegmentsOld  当前旧数据
     * @param currentTrackSegmentsNew  当前新数据
     * @return
     */
    private int getTrackCurrentPosition(int currentPosition, ArrayList<Segment> contrastTrackSegmentsNew,
                                        ArrayList<Segment> currentTrackSegmentsOld,
                                        ArrayList<Segment> currentTrackSegmentsNew) {
        //第二路的老数据
        Segment secondTrackCurrentSegment = currentTrackSegmentsOld.get(currentPosition);
        //第一路新数据的最后一个
        Segment fristTrackEndSegment = contrastTrackSegmentsNew.get(contrastTrackSegmentsNew.size() - 1);
        //请一个分段的结束时间
        double beforeEndTime = fristTrackEndSegment.getTimeMapping().getTargetTimeRange().getEnd().getSecond();
        //现在分段的开始时间
        double currentStartTime = secondTrackCurrentSegment.getTimeMapping().getTargetTimeRange().getStartTime().getSecond();
        double newTargetRangeStartTime = 0;
        if (Math.abs(beforeEndTime - currentStartTime) == 0 || Math.abs(beforeEndTime - currentStartTime) == 0.8)
            newTargetRangeStartTime = beforeEndTime;
        else
            newTargetRangeStartTime = beforeEndTime - 0.8;
        CMTimeRange newTargetRange = new CMTimeRange(newTargetRangeStartTime,
                secondTrackCurrentSegment.getTimeMapping().getTargetTimeRange().getDuration().getSecond());
        //调整后的Segment
        Segment newSegment = new VideoSegment(false, ((VideoSegment) secondTrackCurrentSegment).getVideoFile(),
                secondTrackCurrentSegment.getTrackId(), new CMTimeMapping(secondTrackCurrentSegment.getTimeMapping().getSourceTimeRange(),
                newTargetRange), UUID.randomUUID().toString());
        currentTrackSegmentsNew.add(newSegment);
        //调整自身的分段
        currentPosition = adjustOneself(currentPosition, currentTrackSegmentsOld, currentTrackSegmentsNew);
        return currentPosition;
    }

    /**
     * 自己内部紧挨着的 调整时间
     *
     * @param currentPosition
     * @param trackSegmentsOld
     * @param trackSegmentsNew
     * @return
     */
    private int adjustOneself(int currentPosition, ArrayList<Segment> trackSegmentsOld, ArrayList<Segment> trackSegmentsNew) {
        while (true) {
            currentPosition++;
            if (trackSegmentsOld.size() <= currentPosition) break;
            //当前的前一个 已经调整过的
            Segment segmentBefore = trackSegmentsOld.get(currentPosition - 1);
            //目前需要调整的
            Segment segmentCurrent = trackSegmentsOld.get(currentPosition);
            if (CMTime.compare(segmentBefore.getTimeMapping().getTargetTimeRange().getEnd(),
                    segmentCurrent.getTimeMapping().getTargetTimeRange().getStartTime()) == 0) {
                Segment last = trackSegmentsNew.get(trackSegmentsNew.size() - 1);

                CMTimeRange newTargetRangeNext = new CMTimeRange(last.getTimeMapping().getTargetTimeRange().getEnd().getSecond(),
                        segmentCurrent.getTimeMapping().getTargetTimeRange().getDuration().getSecond());
                //调整后的Segment
                Segment newSegmentNext = new VideoSegment(false, ((VideoSegment) segmentCurrent).getVideoFile(),
                        segmentCurrent.getTrackId(), new CMTimeMapping(segmentCurrent.getTimeMapping().getSourceTimeRange(),
                        newTargetRangeNext), UUID.randomUUID().toString());

                trackSegmentsNew.add(newSegmentNext);
            } else {
                break;
            }
        }
        return currentPosition;
    }

    /**
     * 打乱Segment 重新调整原先Segment 之间的间隙和 顺序
     * by zp
     */

    private void adjustSegment() {
        if (secondVideoMediaTrack == null || secondVideoMediaTrack.getSegments() == null
                || secondVideoMediaTrack.getSegments().isEmpty()) return;

        ArrayList<Segment> segmentsAllOld = new ArrayList<>();
        for (Object o : firstVideoMediaTrack.getSegments()) {
            if (!((Segment) o).isEmpty())
                segmentsAllOld.add((Segment) o);
        }
        for (Object o : secondVideoMediaTrack.getSegments()) {
            if (!((Segment) o).isEmpty())
                segmentsAllOld.add((Segment) o);
        }
        Collections.sort(segmentsAllOld, new SortSegmentByTargetTimeRange());

        //清空原有的
        firstVideoMediaTrack.getSegments().clear();
        secondVideoMediaTrack.getSegments().clear();
        //添加新的segment
        for (int i = 0; i < segmentsAllOld.size(); i++) {
            Segment segmentOld = segmentsAllOld.get(i);
            MediaTrack mediaTrack = (i % 2 == 0) ? firstVideoMediaTrack : secondVideoMediaTrack;
            CMTimeRange oldTargetRange = segmentOld.getTimeMapping().getTargetTimeRange();
            CMTimeRange newTargetRange = oldTargetRange;
            double newTargetRangeStartTime = 0;
            if (i != 0) {//第一个数据直接添加到firstVideoMediaTrack，不需要处理
                //取另一路 中的track
                MediaTrack otherMediaTrack = (i % 2 == 0) ? secondVideoMediaTrack : firstVideoMediaTrack;
                //请一个分段的结束时间
                double beforeEndTime = segmentsAllOld.get(i - 1).getTimeMapping().getTargetTimeRange().getEnd().getSecond();
                //现在分段的开始时间
                double currentStartTime = segmentOld.getTimeMapping().getTargetTimeRange().getStartTime().getSecond();

                //需不需要调整以老数据为准
                if (Math.abs(beforeEndTime - currentStartTime) == 0 || Math.abs(beforeEndTime - currentStartTime) == 0.8) {
                    //不需要调整时间只需要紧接着上一个Track的最后一个Segment的后面
                    if (otherMediaTrack.getSegments().size() > 0) {
                        Segment segmentTem = (Segment) otherMediaTrack.getSegments().get(otherMediaTrack.getSegments().size() - 1);
                        newTargetRangeStartTime = segmentTem.getTimeMapping().getTargetTimeRange().getEnd().getSecond();
                    } else {
                        newTargetRangeStartTime = currentStartTime;
                    }
//                    Log.e("zppp", "时间不需要调整：beforeEndTime：" + beforeEndTime +
//                            ";currentStartTime=" + currentStartTime + ";newTargetRange=" + newTargetRange.toString());
                } else {
                    //需要调整
                    if (otherMediaTrack.getSegments().size() > 0) {
                        Segment segmentTem = (Segment) otherMediaTrack.getSegments().get(otherMediaTrack.getSegments().size() - 1);
                        newTargetRangeStartTime = segmentTem.getTimeMapping().getTargetTimeRange().getEnd().getSecond() - 0.8;
                    } else {
                        //这种情况不存在
                        newTargetRangeStartTime = currentStartTime;
                    }
//                    Log.e("zppp", "调整了时间：beforeEndTime：" + beforeEndTime +
//                            ";currentStartTime=" + currentStartTime + ";newTargetRange=" + newTargetRange.toString());
                }
            }
            newTargetRange = new CMTimeRange(newTargetRangeStartTime, oldTargetRange.getDuration().getSecond());
            //调整后的Segment
            Segment newSegment = new VideoSegment(false, ((VideoSegment) segmentOld).getVideoFile(),
                    segmentOld.getTrackId(), new CMTimeMapping(segmentOld.getTimeMapping().getSourceTimeRange(),
                    newTargetRange), UUID.randomUUID().toString());

            if (mediaTrack.getSegments().size() > 0) {
                //最后一个已经存在的Segment
                Segment lastExistSegment = (Segment) mediaTrack.getSegments().get(mediaTrack.getSegments().size() - 1);
                if (CMTime.compare(lastExistSegment.getTimeMapping().getTargetTimeRange().getEnd(),
                        newSegment.getTimeMapping().getTargetTimeRange().getStartTime()) != 0) {
                    CMTime duration = CMTime.subTime(newSegment.getTimeMapping().getTargetTimeRange().getStartTime()
                            , lastExistSegment.getTimeMapping().getTargetTimeRange().getEnd());
                    mediaTrack.insertEmpy(new CMTimeRange(lastExistSegment.getTimeMapping().getTargetTimeRange().getEnd(), duration));
                    Log.e("zppp", "添加空Segment：getEnd：" + lastExistSegment.getTimeMapping().getTargetTimeRange().getEnd() +
                            ";getStartTime=" + newSegment.getTimeMapping().getTargetTimeRange().getStartTime() + ";duration=" + duration);
                }
            } else {
                if (newSegment.getTimeMapping().getTargetTimeRange().getStartTime().getSecond() > 0)
                    mediaTrack.insertEmpy(new CMTimeRange(CMTime.zeroTime(), newSegment.getTimeMapping().getTargetTimeRange().getStartTime()));
            }
            mediaTrack.getSegments().add(newSegment);
        }
    }

    /**
     * 初始化排版信息
     */
    private void buildEffect() {
        if (maskMediaTrack != null) {
            if (!maskMediaTrack.getSegments().isEmpty())
                maskMediaTrack.getSegments().clear();
        }
        if (maskExtMediaTrack != null) {
            if (!maskExtMediaTrack.getSegments().isEmpty())
                maskExtMediaTrack.getSegments().clear();
        }
        if (!mProject.getEffectAdapters().isEmpty())
            Collections.sort(mProject.getEffectAdapters(), new EffectAdapterSortByCMTime());
        CMTime scaledProjectDuration = mediaComposition.getDuration();
        //处理模板完毕 如果视频排版 需要裁剪时间  ，如果是图片超过composition时长也需要 裁剪
        for (int i = 0; i < mProject.getEffectAdapters().size(); i++) {
            EffectAdapter adapter = mProject.getEffectAdapters().get(i);
            CMTimeRange timeRange = adapter.getTimeRange();
            if (timeRange.getEnd().getSecond() > scaledProjectDuration.getSecond()) {
                CMTime duration = CMTime.subTime(scaledProjectDuration, timeRange.getStartTime());
                timeRange = new CMTimeRange(timeRange.getStartTime(), duration);
                adapter.setTimeRange(timeRange);
            }
            if (timeRange.getStartTime().getSecond() < scaledProjectDuration.getSecond()) {
                if (adapter.getEffectType() == EffectType.EffectType_Pic) {
                    //Nothing TODO
                } else if (adapter.getEffectType() == EffectType.EffectType_Video) {
                    if (adapter.getMaskVideoChunk() != null && adapter.getMaskExtVideoChunk() != null) {
                        Chunk chunk = adapter.getMaskVideoChunk();
                        Chunk chunkext = adapter.getMaskExtVideoChunk();
                        if (adapter.getEffectId() == "") {
                            timeRange = new CMTimeRange(CMTime.zeroTime(), mediaComposition.getDuration());
                        }
                        if (maskMediaTrack == null) {
                            maskMediaTrack = mediaComposition.addTrack(MediaType.MEDIA_TYPE_Video, TrackType.TrackType_Video_Mask);
                        }
                        if (maskExtMediaTrack == null) {
                            maskExtMediaTrack = mediaComposition.addTrack(MediaType.MEDIA_TYPE_Video, TrackType.TrackType_Video_Mask_Ext);
                        }

                        //如果脚本中的timerange总时长大于视频文件的总时长，设置最大值为视频时间总时长
                        CMTimeRange maskMediaTrackCMTimeRange = new CMTimeRange(CMTime.zeroTime(), timeRange.getDuration());
                        CMTimeRange maskExtMediaTrackCMTimeRange = new CMTimeRange(CMTime.zeroTime(), timeRange.getDuration());
                        if (chunk.getVideoFile().getcDuration().getUs() < maskMediaTrackCMTimeRange.getDuration().getUs()) {
                            maskMediaTrackCMTimeRange = new CMTimeRange(CMTime.zeroTime(), chunk.getVideoFile().getcDuration());
                        }
                        if (chunkext.getVideoFile().getcDuration().getUs() < maskExtMediaTrackCMTimeRange.getDuration().getUs()) {
                            maskExtMediaTrackCMTimeRange = new CMTimeRange(CMTime.zeroTime(), chunkext.getVideoFile().getcDuration());
                        }

                        maskMediaTrack.insertTrack(chunk.getVideoFile(), maskMediaTrackCMTimeRange, timeRange.getStartTime());
                        maskExtMediaTrack.insertTrack(chunkext.getVideoFile(), maskExtMediaTrackCMTimeRange, timeRange.getStartTime());
                    }
                }
            }

        }
    }

    /**
     * 检查track
     */
    public void checkTracks() {
        if (firstVideoMediaTrack.getSegments().size() < 1) {
            mediaComposition.removeTrack(firstVideoMediaTrack);
        }
        if (secondVideoMediaTrack.getSegments().size() < 1) {
            mediaComposition.removeTrack(secondVideoMediaTrack);
        }
        if (maskMediaTrack.getSegments().size() < 1) {
            mediaComposition.removeTrack(maskMediaTrack);
        }
        if (maskExtMediaTrack.getSegments().size() < 1) {
            mediaComposition.removeTrack(maskExtMediaTrack);
        }
        if (transitionMediaTrack.getSegments().size() < 1) {
            mediaComposition.removeTrack(transitionMediaTrack);
        }
        if (audioMediaTrack.getSegments().size() < 1) {
            mediaComposition.removeTrack(audioMediaTrack);
        }
        if (backGroundAudioTrack.getSegments().size() < 1) {
            mediaComposition.removeTrack(backGroundAudioTrack);
        }
        if (recodeAudioTrack.getSegments().size() < 1) {
            mediaComposition.removeTrack(recodeAudioTrack);
        }
    }

    /**
     * 添加背景音乐
     */
    private void buildAudio() {
//        if (mProject.getBackGroundMusic() != null) {
////            addBackGroundMusic(CMTimeRange.CMTimeRangeTimeToTime(CMTime.zeroTime(), mProject.getBackGroundMusic().getcDuration()), mProject.getBackGroundMusic());
//            addBackGroundMusic(mProject.getMusicModel().getCMTimeRange(), mProject.getBackGroundMusic());
//        } else {
//            mediaComposition.removeTrack(backGroundAudioTrack);
//        }
        //移除旧的视频原音
        mProject.removeMainAudio();
        for (Chunk chunk : getChunks()) {
            AudioChunk audioChunk = null;
            if (chunk.isExistAudio()) {
                audioChunk = new AudioChunk(chunk.getAudioFile().getFilePath(), chunk.getChunkId(),
                        mContext, TrackType.TrackType_Main_Audio, needSave);
                if (!audioChunk.isAudioPrepare()) continue;
            } else {//添加空AudioChunk
                audioChunk = new AudioChunk("", chunk.getChunkId(),
                        mContext, TrackType.TrackType_Main_Audio, needSave);
                audioChunk.setAudioChunkEmpty(true);
            }
            audioChunk.setInsertTime(chunk.getStartTime());
            audioChunk.setChunkEditTimeRange(chunk.getChunkEditTimeRange());
            audioChunk.setVolume(chunk.getAudioVolumeProportion());
            audioChunk.setSpeedPoints(mProject.getSpeedPoints());
            mProject.addAudioChunk(audioChunk);
        }
    }

    private void buildRecoder() {
        List<RecodeModel> recodeModelList = getRecoderList();
        if (recodeModelList == null || recodeModelList.isEmpty()) return;
        if (mediaComposition == null || recodeAudioTrack == null) return;
        Collections.sort(recodeModelList, new SortRecoderByAtTime());
        try {
            for (RecodeModel recodeModel : recodeModelList) {
                AudioFile audioFile = AudioFile.getAudioFileInfo(recodeModel.getFilePath(), mContext);
                recodeAudioTrack.insertTrack(audioFile, recodeModel.getTimeRange(), recodeModel.getAtTime());
            }
        } catch (InvalidVideoSourceException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取整理后的有序的录音列表
     * 整理逻辑：后面添加的录音，可以覆盖前面的录音
     *
     * @return
     */
    private List<RecodeModel> getRecoderList() {
        if (!mProject.isExistRecodeModel()) return null;
        List<RecodeModel> recodeModelList = new ArrayList<>();
        List<CMTimeRange> mInsertCMTimeRange = new ArrayList<>();//不可被插入的时间
        int recoderModelSize = mProject.getRecodeModels().size();

        for (int i = recoderModelSize - 1; i >= 0; i--) {

            RecodeModel recodeModel = mProject.getRecodeModels().get(i);
            RecodeModel tempRecoderModel = new RecodeModel(recodeModel.getTimeRange(),
                    mProject.getRecodeModels().get(i).getAtTime(), recodeModel.getFilePath());

            CMTime startTime = tempRecoderModel.getAtTime();
            CMTimeRange timeRange = tempRecoderModel.getTimeRange();
            CMTime endTime = CMTime.addTime(startTime, timeRange.getDuration());

            //最小音频的时间大于1秒
            if (timeRange.getDuration().getSecond() <= mMinRecordAudioLength) continue;

            //1.最后一个直接添加，作为基础
            if (i == recoderModelSize - 1 || recodeModelList.isEmpty()) {
                recodeModelList.add(tempRecoderModel);
                mInsertCMTimeRange.add(CMTimeRange.CMTimeRangeTimeToTime(startTime, endTime));
                continue;
            }
            //2.对数据排序方便后续查询
            Collections.sort(mInsertCMTimeRange, new SortCMTimeRangeByStartTime());
            //3.记录本次要插入的时间段
            List<CMTimeRange> tempInsertCMTimeRange = new ArrayList<>();//插入的时间段
            //4.获取已经插入的音频在时间轴上的最大最小值
            CMTime nowMinStartTime = mInsertCMTimeRange.get(0).getStartTime();//当前最小的开始时间
            CMTime nowMaxEndTime = CMTime.addTime(nowMinStartTime, mInsertCMTimeRange.get(mInsertCMTimeRange.size() - 1).getDuration()); //当前最大的结束时间

            //5.准备插入数据，分两种情况 ，交叉部分和未交叉部分
            //5.1 未交叉部分：在已经添加录音的最左边或者最右边，可完全添加
            if (CMTime.compare(endTime, nowMinStartTime) <= 0 || CMTime.compare(startTime, nowMaxEndTime) >= 0) {
                recodeModelList.add(tempRecoderModel);
                tempInsertCMTimeRange.add(CMTimeRange.CMTimeRangeTimeToTime(startTime, endTime));
            } else {
                //5.2交叉部分（分为三种）
                List<CMTimeRange> canSplitCMTimeRange = new ArrayList<>();
                //找到可以切割的时间段，这段录音的时间段
                for (int j = 0; j < mInsertCMTimeRange.size(); j++) {
                    CMTimeRange tempTimeRange = mInsertCMTimeRange.get(j);
                    CMTime tempStartTime = tempTimeRange.getStartTime();
                    CMTime tempEndTime = tempTimeRange.getEnd();
                    if ((CMTime.compare(tempStartTime, startTime) >= 0 && CMTime.compare(tempStartTime, endTime) < 0)
                            || (CMTime.compare(tempEndTime, startTime) >= 0 && CMTime.compare(tempEndTime, endTime) < 0)) {
                        canSplitCMTimeRange.add(CMTimeRange.CMTimeRangeTimeToTime(tempStartTime, tempEndTime));
                    }
                }
                if (!canSplitCMTimeRange.isEmpty()) {
                    CMTimeRange fristCMTimeRange = canSplitCMTimeRange.get(0);
                    CMTimeRange lastCMTimeRange = canSplitCMTimeRange.get(canSplitCMTimeRange.size() - 1);
                    //5.2.1当前录音的最小位置<第一个一个片段的录音最小位置，填充数据
                    if ((CMTime.compare(startTime, fristCMTimeRange.getStartTime()) < 0)) {
                        CMTime addRecoderAtTime = startTime;
                        CMTime addRecoderEndTime = fristCMTimeRange.getStartTime();
                        //真实的时间轴时间
                        CMTimeRange addRecoderTimeRange = CMTimeRange.CMTimeRangeTimeToTime(addRecoderAtTime, addRecoderEndTime);
                        //对应录音文件的时间轴
                        CMTime newRecoderTimeRangeStartTime = tempRecoderModel.getTimeRange().getStartTime();
                        CMTime newRecoderTimeRangeEndTime = CMTime.addTime(newRecoderTimeRangeStartTime, addRecoderTimeRange.getDuration());
                        CMTimeRange newRecoderTimeRange = CMTimeRange.CMTimeRangeTimeToTime(newRecoderTimeRangeStartTime, newRecoderTimeRangeEndTime);
                        recodeModelList.add(new RecodeModel(newRecoderTimeRange, addRecoderAtTime, recodeModel.getFilePath()));

                        tempInsertCMTimeRange.add(addRecoderTimeRange);
                    }
                    //5.2.2当前录音的最大位置大于最后一个片段的录音最大位置，填充数据
                    if ((CMTime.compare(endTime, lastCMTimeRange.getEnd()) > 0)) {
                        CMTime addRecoderAtTime = lastCMTimeRange.getEnd();
                        CMTime addRecoderEndTime = endTime;
                        //真实的时间轴时间
                        CMTimeRange addRecoderTimeRange = CMTimeRange.CMTimeRangeTimeToTime(addRecoderAtTime, addRecoderEndTime);
                        //对应录音文件的时间轴
                        CMTime newRecoderTimeRangeStartTime = CMTime.addTime(tempRecoderModel.getTimeRange().getStartTime()
                                , CMTime.subTime(addRecoderAtTime, startTime));
                        CMTime newRecoderTimeRangeEndTime = CMTime.addTime(newRecoderTimeRangeStartTime, addRecoderTimeRange.getDuration());

                        CMTimeRange newRecoderTimeRange = CMTimeRange.CMTimeRangeTimeToTime(newRecoderTimeRangeStartTime, newRecoderTimeRangeEndTime);
                        recodeModelList.add(new RecodeModel(newRecoderTimeRange, addRecoderAtTime, recodeModel.getFilePath()));
                        tempInsertCMTimeRange.add(addRecoderTimeRange);
                    }
                    //5.2.3查看中间是否有空的片段存在如果存在就填充当前的相应录音数据
                    if (canSplitCMTimeRange.size() > 1) {
                        for (int j = 0; j < canSplitCMTimeRange.size() - 1; j++) {
                            CMTime beforeEndTime = CMTime.addTime(canSplitCMTimeRange.get(j).getStartTime(), canSplitCMTimeRange.get(j).getDuration());
                            CMTime afterStartTime = canSplitCMTimeRange.get(j + 1).getStartTime();
                            if (Math.abs(beforeEndTime.getSecond() - afterStartTime.getSecond()) > mMinRecordAudioLength) {
                                CMTime addRecoderAtTime = beforeEndTime;
                                CMTime addRecoderEndTime = afterStartTime;
                                //真实的时间轴时间
                                CMTimeRange addRecoderTimeRange = CMTimeRange.CMTimeRangeTimeToTime(addRecoderAtTime, addRecoderEndTime);
                                //对应录音文件的时间轴
                                CMTime newRecoderTimeRangeStartTime = CMTime.addTime(tempRecoderModel.getTimeRange().getStartTime()
                                        , CMTime.subTime(addRecoderAtTime, startTime));
                                CMTime newRecoderTimeRangeEndTime = CMTime.addTime(newRecoderTimeRangeStartTime, addRecoderTimeRange.getDuration());
                                CMTimeRange newRecoderTimeRange = CMTimeRange.CMTimeRangeTimeToTime(newRecoderTimeRangeStartTime, newRecoderTimeRangeEndTime);
                                recodeModelList.add(new RecodeModel(newRecoderTimeRange, addRecoderAtTime, recodeModel.getFilePath()));

                                tempInsertCMTimeRange.add(addRecoderTimeRange);
                            }
                        }
                    }
                }
            }

            //6.整理本次添加的数据，方便下次循环
            mInsertCMTimeRange.addAll(tempInsertCMTimeRange);
            Collections.sort(mInsertCMTimeRange, new SortCMTimeRangeByStartTime());
            //合并已经添加的录音片段的，提高下次遍历速度
            List<CMTimeRange> mInsertCMTimeRangeTemp = new ArrayList<>();
            for (int j = 0; j < mInsertCMTimeRange.size() - 1; j++) {
                CMTime beforeEndTime = CMTime.addTime(mInsertCMTimeRange.get(j).getStartTime(), mInsertCMTimeRange.get(j).getDuration());
                CMTime afterStartTime = mInsertCMTimeRange.get(j + 1).getStartTime();
                if (Math.abs(beforeEndTime.getSecond() - afterStartTime.getSecond()) <= mMinRecordAudioLength) {
                    CMTimeRange addRecoderTimeRange = CMTimeRange.CMTimeRangeTimeToTime(mInsertCMTimeRange.get(j).getStartTime()
                            , CMTime.addTime(afterStartTime, mInsertCMTimeRange.get(j + 1).getDuration()));
                    mInsertCMTimeRangeTemp.add(addRecoderTimeRange);
                }
            }
            if (!mInsertCMTimeRangeTemp.isEmpty()) {
                mInsertCMTimeRange.clear();
                mInsertCMTimeRange.addAll(mInsertCMTimeRangeTemp);
            }
        }
        return recodeModelList;
    }

    private void buildAudioMix() throws Exception {
        ArrayList<AudioMixInputParameter> inputParameters = new ArrayList<AudioMixInputParameter>();
        AudioMixInputParameter audioMixInputParameters = null;
        if (this.audioMediaTrack != null && this.audioMediaTrack.getSegments().size() != 0) {
            audioMixInputParameters = AudioMixInputParameter.audioMixInputParametersWithTrack(this.audioMediaTrack.getTrackType());
        }
        AudioMixInputParameter backGroundAudioMixInputParameter = null;
        if (this.backGroundAudioTrack != null && mProject.getBackGroundMusicPath() != null && mProject.getBackGroundMusicPath().length() != 0) {
            backGroundAudioMixInputParameter = AudioMixInputParameter.audioMixInputParametersWithTrack(this.backGroundAudioTrack.getTrackType());
        }
        AudioMixInputParameter recodeAudioMixInputParameter = null;
        if (recodeAudioTrack != null && mProject.isExistRecodeModel()) {
            recodeAudioMixInputParameter = AudioMixInputParameter.audioMixInputParametersWithTrack(this.recodeAudioTrack.getTrackType());
        }

        for (int i = 0; i < getChunks().size(); i++) {
            Chunk chunk = getChunk(i);
            float audioProp = 0;
            float musicProp = 0;
//            if(this.backGroundAudioTrack!=null&&mProject.getBackGroundMusicPath()!=null &&mProject.getBackGroundMusicPath().length()!=0){
//
//            }
            if (recodeAudioTrack != null && mProject.isExistRecodeModel()) {
                audioProp = 0.5f - chunk.getAudioVolumeProportion() * 0.5f;
                musicProp = chunk.getAudioVolumeProportion() * 0.5f;
            } else {
                audioProp = 1.f - chunk.getAudioVolumeProportion();
                musicProp = chunk.getAudioVolumeProportion();
            }
            if (audioMixInputParameters != null) {
                audioMixInputParameters.setVolumeAtTime(audioProp, chunk.getStartTime());
            }
            if (backGroundAudioMixInputParameter != null) {
                backGroundAudioMixInputParameter.setVolumeAtTime(musicProp, chunk.getStartTime());
            }
        }

        if (audioMixInputParameters != null) {
            inputParameters.add(audioMixInputParameters);
        }
        if (backGroundAudioMixInputParameter != null) {
            inputParameters.add(backGroundAudioMixInputParameter);
        }
        if (recodeAudioMixInputParameter != null && mProject.isExistRecodeModel()) {
            recodeAudioMixInputParameter.setVolumeAtTime(1f, CMTime.zeroTime());
            inputParameters.add(recodeAudioMixInputParameter);
        }
        audioMixParam = AudioMixParam.audioMix();
        audioMixParam.setInputParameters(inputParameters);
    }


    /**
     * 获取projectDuration 单位秒
     *
     * @return
     */
    public float getProjectDuration() {
        return (float) projectDuration.getSecond();
    }

    public long getProjectDuration1() {
        return projectDuration.getMs();
    }

    /**
     * 检查chunk是否能加转场
     */
    private void checkTransition() {
        Chunk preChunk = null;
        Chunk nextChunk = null;
        for (int i = 0; i < mProject.getChunks().size(); i++) {
            Chunk chunk = mProject.getChunks().get(i);
            preChunk = null;
            if (i > 0) {
                preChunk = mProject.getChunks().get(i - 1);
            }
            nextChunk = null;
            if (i < mProject.getChunks().size() - 1) {
                nextChunk = mProject.getChunks().get(i + 1);
            }
            chunk.setAddTrans(canAddTrans(chunk, preChunk, nextChunk));
            if (!chunk.isAddTrans()) {
                Chunk chunkTmp = mProject.getChunks().get(i);
                chunkTmp.setTransition(TransitionStyle.VNITransitionTypeNone, mProject.getRotation(), CMTime.zeroTime());
                if (EditConstants.VERBOSE_EDIT)
                    Log.i(EditConstants.TAG_EDIT, "videoEffect_checkTransition set None");
                chunkTmp.setTransIndex(i);
            }
            chunk.setChunkTransitionHeadTime(CMTime.zeroTime());
            chunk.setChunkTransitionTailTime(CMTime.zeroTime());
            chunk.setOriginTransitionHeadTime(CMTime.zeroTime());
            chunk.setOriginTransitionTailTime(CMTime.zeroTime());
        }
    }

    /**
     * 判断是否能加转场
     *
     * @param current
     * @param preChunk
     * @param nextChunk
     * @return
     */
    private boolean canAddTrans(Chunk current, Chunk preChunk, Chunk nextChunk) {
        CMTime maxDuration = VNITransitionFactory.maxTransitionDuration();
        float tmpTime = 0;
        float maxTime = (float) CMTime.getSecond(maxDuration);
        if (isMVVideoAE) return false;
        if (preChunk == null) return false;
        //前一个片段时长小于转场时长,不能添加转场
        if (CMTime.getSecond(preChunk.getChunkEditTimeRange().getDuration()) < maxTime) {
            return false;
        }
        //当前片段时长小于转场时长,不能添加转场
        if (CMTime.getSecond(current.getChunkEditTimeRange().getDuration()) < maxTime) {
            return false;
        }
        //前一个片段已经有转场了,除去已添加的转场时间后小于转场时长,不能再添加转场
        if (CMTime.getSecond(preChunk.getChunkEditTimeRange().getDuration()) - CMTime.getSecond(preChunk.getChunkTransitionTime()) <= maxTime) {
            return false;
        }
        //当前片段已经有转场了,除去已添加的转场时间后小于转场时长,不能再添加转场
        if (nextChunk == null) {
            tmpTime = 0;
        } else {
            tmpTime = (float) CMTime.getSecond(nextChunk.getChunkTransitionTime());
        }
        if (CMTime.getSecond(current.getChunkEditTimeRange().getDuration()) - tmpTime < maxTime) {
            return false;
        }
        return true;
    }

    /**
     * 添加排版
     *
     * @param videoString
     * @param maskString
     * @param atTime
     */
    public void setBlendVideo(String videoString, String maskString, CMTime atTime, String flag) {
        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
        try {
            Chunk chunk = new Chunk(videoString, mContext, needSave);
            Chunk chunk1 = new Chunk(maskString, mContext, needSave);
            this.blendVideoTime = atTime;
            EffectAdapter adapter = new EffectAdapter(flag, EffectType.EffectType_Video);
            adapter.setTimeRange(new CMTimeRange(atTime, chunk.getVideoFile().getcDuration()));
            adapter.setMaskVideoChunk(chunk);
            adapter.setMaskExtVideoChunk(chunk1);
            mProject.addEffect(adapter);
        } catch (InvalidVideoSourceException e) {
            e.printStackTrace();
        }
    }


    /**
     * 添加 图片排版
     *
     * @param bitmap
     * @param timeRange
     * @param flag
     */
    public void setBlendPic(Bitmap bitmap, CMTimeRange timeRange, String flag) {
        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
        if (bitmap == null) return;
        EffectAdapter adapter = new EffectAdapter(flag, EffectType.EffectType_Pic);
        adapter.setTimeRange(timeRange);
        adapter.setBitmap(bitmap);
        mProject.addEffect(adapter);
    }

    /**
     * 添加 贴纸
     *
     * @param timeRange     贴纸开始结束时间
     * @param flag          贴纸id
     * @param stickerConfig 贴纸配置
     */
    public void setBlendSticker(CMTimeRange timeRange, String flag, StickerConfig stickerConfig) {
        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
        if (stickerConfig == null) return;
        EffectAdapter adapter = new EffectAdapter(flag, EffectType.EffectType_Sticker);
        adapter.setTimeRange(timeRange);
        adapter.setStickerConfig(stickerConfig);
        mProject.addEffect(adapter);
    }

    /**
     * 添加 特效
     *
     * @param timeRange         特效有效范围
     * @param flag              贴纸id
     * @param specialEffectJson 特效json
     */
    public void setBlendSpecialEffect(CMTimeRange timeRange, String flag, String specialEffectJson) {
        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
        if (TextUtils.isEmpty(specialEffectJson)) return;
        EffectAdapter adapter = new EffectAdapter(flag, EffectType.EffectType_Special_Effect);
        adapter.setTimeRange(timeRange);
        adapter.configSpecialFilter(specialEffectJson);
        mProject.addEffect(adapter);
    }


    public ArrayList<EffectAdapter> getEffects() {
        if (mProject != null) {
            return mProject.getEffectAdapters();
        }
        return null;
    }

    /**
     * 建议视频方向(根据chunk里较多的方向确定, 优先横屏)
     *
     * @return
     */

    public Origentation getSuggestOrientation() {
        int hCount = 0;
        int vCount = 0;
        for (Chunk chunk : mProject.getChunks()) {
            if (chunk.getVideoOrigentation() == Origentation.kVideo_Horizontal) {
                hCount++;
            }
            if (chunk.getVideoOrigentation() == Origentation.kVideo_Vertical) {
                vCount++;
            }
        }
        Origentation origentation = null;
//        if (!mProject.getChunks().isEmpty()) {
//            origentation = mProject.getChunks().get(0).getVideoOrigentation();
//        }
        if (vCount > hCount) {
            origentation = Origentation.kVideo_Vertical;
        } else if (vCount <= hCount) {
            origentation = Origentation.kVideo_Horizontal;
        }
        return origentation;
    }

    /**
     * 获取project方向
     *
     * @return
     */
    public Origentation getProjectOrientation() {
        Origentation orienta = getSuggestOrientation();
        if (mProject.getRotation() == Origentation.kVideo_Unknow) {
            mProject.setRotation(orienta);
        }
        this.updateCanvase();
        return mProject.getRotation();
    }

    /**
     * 强制设置project方向
     *
     * @param origentation
     */
    public void setProjectOrientation(Origentation origentation) {
        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
        mProject.setRotation(origentation);
        this.updateCanvase();

    }

    /**
     * 更新视频方向
     */
    private void updateProjectOrientation() {
        //如果为unknow 则选用建议的方向 更新renderSize
        if (mProject.getRotation() == Origentation.kVideo_Unknow) {
            mProject.setRotation(getSuggestOrientation());
            this.updateCanvase();
        }
    }

    /**
     * 获取chunk的开始结束时间
     */
    private void checkChunkStartTime() {
        if (firstVideoMediaTrack != null) {
            checkTrackSegment(firstVideoMediaTrack, 0);
        }
        if (secondVideoMediaTrack != null) {
            checkTrackSegment(secondVideoMediaTrack, 1);
        }
        for (int i = 0; i < mProject.getChunks().size(); i++) {
            Chunk currentChunk = mProject.getChunks().get(i);
            Chunk nextChunk = null;
            if (i < mProject.getChunks().size() - 1) {
                nextChunk = mProject.getChunks().get(i + 1);
            }
            if (i == 0) {
                currentChunk.setChunkTransitionHeadTime(CMTime.zeroTime());
            }
            if (nextChunk != null) {
                CMTime transitionTime = CMTime.subTime(currentChunk.getEndTime(), nextChunk.getStartTime());
                currentChunk.setChunkTransitionTailTime(transitionTime);
                nextChunk.setChunkTransitionHeadTime(transitionTime);
            } else {
                currentChunk.setChunkTransitionTailTime(CMTime.zeroTime());
            }
            Log.d(EditConstants.TAG_AE, "片段: " + i + " 开始时间: " + CMTime.getSecond(currentChunk.getStartTime()) + " 结束时间: " + CMTime.getSecond(currentChunk.getEndTime()) + " 片段时长：  " + CMTime.getSecond(CMTime.subTime(currentChunk.getEndTime(), currentChunk.getStartTime())) + " 转场时间： " + CMTime.getSecond(currentChunk.getChunkTransitionTime()) + " 转场Header: " + CMTime.getSecond(currentChunk.getChunkTransitionHeadTime()) + " 转场tail: " + CMTime.getSecond(currentChunk.getChunkTransitionTailTime()));
        }

    }

    /**
     * 检查segment确定chunk的开始和结束时间
     *
     * @param mediaTrack
     * @param startIndex
     */
    private void checkTrackSegment(MediaTrack mediaTrack, int startIndex) {
        int chunkIndex = startIndex;
        CMTime tmpStartTime = CMTime.zeroTime();
        ArrayList<Segment> arrayList = new ArrayList<Segment>();
        for (int i = startIndex; i < mediaTrack.getSegments().size(); i++) {
            Segment segment = (Segment) mediaTrack.getSegments().get(i);
            Segment nextSegment = null;
            Segment preSegment = null;
            if (i < mediaTrack.getSegments().size() - 1) {
                nextSegment = (Segment) mediaTrack.getSegments().get(i + 1);
            }
            if (!segment.isEmpty()) {
                arrayList.add(segment);
            }
            if ((segment.isEmpty() && nextSegment != null && !nextSegment.isEmpty()) || nextSegment == null) {
                CMTimeMapping timeMapping = arrayList.get(0).getTimeMapping();
                CMTimeMapping lastTimeMapping = arrayList.get(arrayList.size() - 1).getTimeMapping();

                CMTime startTime = timeMapping.getTargetTimeRange().getStartTime();
                CMTime endTime = lastTimeMapping.getTargetTimeRange().getEnd();
                arrayList.clear();
                if (chunkIndex < chunks().size()) {
                    Chunk chunk = chunks().get(chunkIndex);
                    chunk.setStartTime(startTime);
                    chunk.setEndTime(endTime);
                }
                chunkIndex += 2;
            }
        }
    }

    public ArrayList<Chunk> getChunksWithSecond(float second) {
        return mProject.getChunkWithSecond(second);
    }

    /**
     * project 添加背景音乐
     *
     * @param timeRange
     * @param backGroundMusic
     * @return
     */
    private boolean addBackGroundMusic(CMTimeRange timeRange, AudioFile backGroundMusic) {
        if (backGroundMusic == null || mediaComposition == null) return false;
        if (backGroundMusic.getDuration() > 0.f) {
            //调整时间，如果时长为小于等于0
            if (timeRange.getDuration().getValue() == 0) {
                CMTime convertTime = CMTime.convertTimeScale(timeRange.getStartTime(), backGroundMusic.getcDuration().getTimeScale());
                if (backGroundMusic.getcDuration().getValue() < convertTime.getValue())
                    convertTime = CMTime.zeroTime();
                CMTime durationTime = CMTime.subTime(backGroundMusic.getcDuration(), convertTime);
                timeRange = new CMTimeRange(convertTime, durationTime);
            }
            if (CMTime.compare(timeRange.getDuration(), mediaComposition.getDuration()) > 0) {
//                CMTime tmpTime = CMTime.convertTimeScale(timeRange.getStartTime(), mediaComposition.getDuration().getTimeScale());
                backGroundAudioTrack.insertTrack(backGroundMusic, new CMTimeRange(timeRange.getStartTime(), mediaComposition.getDuration()), CMTime.zeroTime());
            } else {
                int insertCount = (int) (CMTime.getSecond(mediaComposition.getDuration()) / (CMTime.getSecond(timeRange.getDuration())));
                double remainTime = CMTime.getSecond(mediaComposition.getDuration()) - insertCount * (CMTime.getSecond(timeRange.getDuration()));
                CMTime insertTime = CMTime.zeroTime();
                for (int i = 0; i < insertCount; i++) {
                    backGroundAudioTrack.insertTrack(backGroundMusic, timeRange, insertTime);
                    insertTime = CMTime.addTime(insertTime, timeRange.getDuration());
//                    }
                }
                //最后一段音乐不能小于1秒
                if (remainTime >= 1.0) {
//                if (remainTime > 0) {
                    backGroundAudioTrack.insertTrack(backGroundMusic, new CMTimeRange(timeRange.getStartTime(), new CMTime((remainTime), 1000)), insertTime);
                }
            }
        }

        return true;
    }


    public void destory() {
        mediaComposition = null;
        releaseTrack();
    }

    public String getTailorListStr() {
        return mProject.getTailorListStr();
    }

    public void setTailorListStr(String tailorListStr) {
        mProject.setTailorListStr(tailorListStr);
    }

    public String getAllResolveMapStr() {
        return mProject.getAllResolveMapStr();
    }

    public void setAllResolveMapStr(String allResolveMapStr) {
        mProject.setAllResolveMapStr(allResolveMapStr);
    }

    public String getAllLvjingMapStr() {
        return mProject.getAllLvjingMapStr();
    }

    public void setAllLvjingMapStr(String allLvjingMapStr) {
        mProject.setAllLvjingMapStr(allLvjingMapStr);
    }

    public String getAllLvjingToningMapStr() {
        return mProject.getAllLvjingToningMapStr();
    }

    public void setAllLvjingToningMapStr(String allLvjingToningMapStr) {
        mProject.setAllLvjingToningMapStr(allLvjingToningMapStr);
    }

    public String getOtherObject() {
        return mProject.getOtherObject();
    }

    public void setOtherObject(String objJson) {
        mProject.setOtherObject(objJson);
    }

    public String getProjectId() {
        return mProject.getProjectId();
    }

    public void releaseTrack() {
        firstVideoMediaTrack = null;
        audioMediaTrack = null;
        secondVideoMediaTrack = null;
        transitionMediaTrack = null;
        maskMediaTrack = null;
        maskExtMediaTrack = null;
        backGroundAudioTrack = null;
        recodeAudioTrack = null;
    }


    /**
     * 给 chunk 添加滤镜
     *
     * @param filter     filter
     * @param chunkIndex
     */
    public void setChunkFilter(GPUImageFilter filter, int chunkIndex) {

    }

    /**
     * 设置滤镜强度
     *
     * @param chunkIndex
     * @param strength
     */
    public void setChunkFilterStrength(int chunkIndex, float strength) {

    }


    /**
     * 获取chunk序列帧
     *
     * @param times
     * @param size
     * @param chunk
     * @retur
     */
    public ArrayList<Bitmap> chunkBitmaps(ArrayList<CMTime> times, GPUSize size, Chunk chunk) {
        return null;
    }

    public ArrayList<Bitmap> chunkBitmaps(ArrayList<CMTime> times, GPUSize size, int chunkIndex) {
        //TODO
        return null;
    }
    //提供chunk能不能加转场的回调

    /**
     * 调色强度
     *
     * @param type
     * @param strength
     */
    public void paletteChunk(PaletteType type, float strength, int chunkIndex) {
        Chunk chunk = this.mProject.getChunks().get(chunkIndex);
    }


    /**
     * 调整chunk的声音的占比
     *
     * @param chunkIndex
     * @param percent    原音的占比
     */
    public void setVolumePercent(int chunkIndex, float percent) {

    }


    /**
     * 设置chunk静音
     *
     * @param chunkIndex
     * @param mute
     */
    public void setChunkMute(int chunkIndex, boolean mute) {

    }

    public void setBackGroundVolumePersent(float persent) {

    }

    /**
     * 设置文字位置接口
     *
     * @param bitMap
     * @param timeRange
     */
    public void setTextBitMap(Bitmap bitMap, CMTimeRange timeRange) {

    }


    /**
     * 加载mv
     *
     * @param dirPath
     * @param composition
     * @param timeRange
     */
    public void loadMv(String dirPath, MediaComposition composition, CMTimeRange timeRange) {

    }


    /**
     * 导出当前的视频,默认加片尾
     */
    public boolean exportCurrtProject(String outPutPath, ExportCallBack callBack) {
        return exportCurrtProject(outPutPath, callBack, true, true);
    }


    /**
     * 导出当前视频，可选择是否提取音乐
     *
     * @param canExtralMusic true 提取音乐，false不提取音乐
     * @param outPutPath
     * @param callBack
     * @return
     */
    public boolean exportCurrtProject(boolean canExtralMusic, String outPutPath, ExportCallBack callBack) {
        return exportCurrtProject(outPutPath, callBack, false, canExtralMusic);
    }

    /**
     * 导出当前视频
     *
     * @param outPutPath
     * @param callBack
     * @param withTrailer    true 添加片尾，false 不添加片尾
     * @param canExtralMusic true 提取音乐，false不提取音乐
     * @return
     */
    public boolean exportCurrtProject(String outPutPath, final ExportCallBack callBack, boolean withTrailer, boolean canExtralMusic) {
//        if (outPutPath == null) throw new RuntimeException("请设置导出mp4路径");
//        if (VERBOSE_EN)
//            Log.e("AE_COMMON", "exportPAth:" + outPutPath + ", isExporting:" + isExporting);
//        if (isExporting) {
//            if (VERBOSE_EN)
//                Log.e("AE_COMMON", "exportPAth:" + outPutPath + "重复导出或者正在导出项目");
//            return false;
//        }
//        isExporting = true;
//        mediaComposition = getProjectMediaComposition(withTrailer);
//        if (VERBOSE)
//            mediaComposition.prettyPrintLog();
//        WeakReference<VideoEffect> wVideoEffect = new WeakReference<VideoEffect>(this);
//        exportSession = new VideoExportManager(mContext, mediaComposition, mProject.getProjectRenderSize(), mProject, outPutPath, wVideoEffect, new ExportCallBack() {
//            @Override
//            public void onExporting(float persent) {
//                if (callBack != null) callBack.onExporting(persent);
//            }
//
//            @Override
//            public void onExportSuccess(String outputFilePath) {
//                GPUImageContext.sharedFramebufferCache().purgeAllUnassignedFramebuffers();
//                if (callBack != null) callBack.onExportSuccess(outputFilePath);
//                isExporting = false;
//            }
//
//            @Override
//            public void onExportFaild(String outputFilePath) {
//                GPUImageContext.sharedFramebufferCache().purgeAllUnassignedFramebuffers();
//                if (callBack != null) callBack.onExportFaild(outputFilePath);
//                isExporting = false;
//            }
//
//            @Override
//            public void onExportCancel() {
//                GPUImageContext.sharedFramebufferCache().purgeAllUnassignedFramebuffers();
//                if (callBack != null) callBack.onExportCancel();
//                isExporting = false;
//            }
//        });
////        exportSession.setCanExtractOriginalVideoMusic(canExtralMusic);
//        exportSession.doExport();
        return true;

    }


    public void cancleExport() {
//        if (exportSession != null)
//            exportSession.stopExport();
    }

    public void relaseExportManeger() {
//        if (exportSession != null)
//            exportSession.release();
    }

    /**
     * 获取project 时长
     *
     * @return
     */
    public CMTime getCurrentProjectDuration() {
        return null;
    }

    /**
     * 获取chunk的原始时长
     *
     * @param chunkIndex
     * @return 时长 单位 秒
     */
    public double getOriginChunkDuration(int chunkIndex) {
        if (mProject.getChunks().size() < chunkIndex || mProject.getChunks().size() == 0) return 0;
        Chunk chunk = mProject.getChunks().get(chunkIndex);
        return chunk.getOrigonTimeRange().getDuration().getSecond();
    }

    /**
     * 获取chunk的时长
     *
     * @param chunkIndex
     * @return 时长 单位 秒
     */
    public double getChunkEditDuration(int chunkIndex) {
        if (mProject.getChunks().size() < chunkIndex || mProject.getChunks().size() == 0) return 0;
        Chunk chunk = mProject.getChunks().get(chunkIndex);
        return chunk.getChunkEditTimeRange().getDuration().getSecond();
    }

    /**
     * 添加录音
     *
     * @param filePath
     * @param timeRange
     * @param atTime
     */
    public void addRecoderAudio(String filePath, CMTimeRange timeRange, CMTime atTime) {
        if (TextUtils.isEmpty(filePath)) return;

        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
        RecodeModel recodeModel = new RecodeModel(filePath);
        recodeModel.setTimeRange(timeRange);
        recodeModel.setAtTime(atTime);
        mProject.setRecodeModel(recodeModel, true);
    }

    /**
     * 删除录音
     *
     * @param filePath 录音地址
     */
    public void deleteRecoderAudio(String filePath) {
        if (TextUtils.isEmpty(filePath)) return;

        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
        for (RecodeModel recodeModel : mProject.getRecodeModels()) {
            if (recodeModel.getFilePath().equals(filePath)) {
                mProject.setRecodeModel(recodeModel, false);
                break;
            }
        }
    }

    /**
     * 更新录音
     *
     * @param atTime
     */
    public void updateRecodeAudio(String filePath, CMTimeRange timeRange, CMTime atTime) {
        if (mProject == null || !mProject.isExistRecodeModel()) return;
        // mProject.isExistRecodeModel().setAtTime(atTime);
        needRebuildAudio = true;
        needRebuildVideo = false;
        for (RecodeModel recodeModel : mProject.getRecodeModels()) {
            if (recodeModel.getFilePath().equals(filePath)) {
                recodeModel.setTimeRange(timeRange);
                recodeModel.setAtTime(atTime);
                mProject.setRecodeModel(recodeModel, true);
                break;
            }
        }
        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
    }

    public void updateRecodeAudio(String filePath, CMTime atTime) {
        if (mProject == null || !mProject.isExistRecodeModel()) return;
        // mProject.isExistRecodeModel().setAtTime(atTime);
        needRebuildAudio = true;
        needRebuildVideo = false;
        for (RecodeModel recodeModel : mProject.getRecodeModels()) {
            if (recodeModel.getFilePath().equals(filePath)) {
                recodeModel.setAtTime(atTime);
                mProject.setRecodeModel(recodeModel, true);
                break;
            }
        }
        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
    }


    /**
     * 获取chunk
     *
     * @param chunkIndex
     * @return
     */
    public Chunk getChunk(int chunkIndex) {
        return mProject.getChunks().get(chunkIndex);
    }

    public Chunk getChunk(String chunkId) {
        for (Chunk chunk : mProject.getChunks()) {
            if (chunk.getChunkId().equals(chunkId)) {
                return chunk;
            }
        }
        return null;
    }

    public int getChunkIndex(String chunkId) {
        for (int i = 0; i < mProject.getChunks().size(); i++) {
            if (mProject.getChunks().get(i).getChunkId().equals(chunkId)) {
                return i;
            }
        }
        return -1;
    }

    public ArrayList<Chunk> getChunks() {
        return mProject.getChunks();
    }

    public ArrayList<AudioChunk> getAudioChunks() {
        return mProject.getAudioChunks();
    }

    /**
     * 获取原始总时长
     *
     * @return
     */
    public float getAllOriginChunkDuration() {
        float duration = 0;
        for (int i = 0; i < mProject.getChunks().size(); i++) {
            duration += getOriginChunkDuration(i);
        }
        return duration;
    }

    public int speedPointsCount() {
        this.checkSpeedPoint();
        int count = mProject.getSpeedPoints().size();
        return count;
    }

    public void checkSpeedPoint() {
        if (mProject.getSpeedPoints().size() <= 0 && getChunks().size() > 0) {
            TimeScaleModel startModel = new TimeScaleModel(CMTime.zeroTime(), 1.f);
            mProject.addSpeedPoint(startModel);
            Chunk lastChunk = getChunks().get(getChunks().size() - 1);
            CMTime timeposition = timeWithOriginTime(lastChunk.getChunkTransitionEndTime());
            TimeScaleModel endModel = new TimeScaleModel(timeposition, 1);
            mProject.addSpeedPoint(endModel);
        }
    }

    public TimeScaleModel speedPointWithIndex(int index) {
        this.checkSpeedPoint();
        int count = this.mProject.getSpeedPoints().size();
        if (index == count - 1) {
            TimeScaleModel model = mProject.getSpeedPoints().get(mProject.getSpeedPoints().size() - 1);
            Chunk chunk = getChunks().get(getChunks().size() - 1);
            CMTime time = chunk.chunkTimeRangeBeforeSpeed().getEnd();
            model.setTimePosition(time);
            return model;
        }
        return mProject.getSpeedPoints().get(index);
    }


    public CMTime timeWithOriginTime(CMTime time) {
        float timeValue = (float) CMTime.getSecond(time);
        CMTime originTime = time;
        int count = this.speedPointsCount();
        CMTime timeFlag = CMTime.zeroTime();
        CMTime realTimeFlag = CMTime.zeroTime();
        for (int i = 1; i < count; i++) {
            TimeScaleModel model = this.speedPointWithIndex(i);
            float addSec = (float) CMTime.getSecond(CMTime.subTime(model.getTimePosition(), timeFlag));
            float duration = addSec / model.getSpeedScale();
            CMTime durationTime = new CMTime(duration);
            CMTimeRange range = new CMTimeRange(timeFlag, CMTime.subTime(model.getTimePosition(), timeFlag));
            CMTimeRange realRange = new CMTimeRange(realTimeFlag, durationTime);
            float start = (float) CMTime.getSecond(timeFlag);
            float end = start + addSec;
            timeValue = Math.max(0, Math.min(timeValue, (float) CMTime.getSecond(originDuration)));
            if (timeValue >= start && timeValue <= end) {
                CMTime lessTime = CMTime.subTime(time, timeFlag);
                float lt = (float) CMTime.getSecond(lessTime);
                lt = lt / model.getSpeedScale();
                float st = (float) CMTime.getSecond(realTimeFlag);
                float t = st + lt;
                CMTime timePosition = new CMTime(t);
                originTime = timePosition;
                break;
            }
            realTimeFlag = realRange.getEnd();
            timeFlag = range.getEnd();
        }
        return originTime;
    }

    public CMTime originTimeWithTime(CMTime time) {
        time = CMTime.Minimum(time, projectDuration);
        float timeValue = (float) CMTime.getSecond(time);
        CMTime originTime = time;
        int count = this.speedPointsCount();
        CMTime timeFlag = CMTime.zeroTime();
        CMTime realTimeFlag = CMTime.zeroTime();
        for (int i = 1; i < count; i++) {
            TimeScaleModel model = speedPointWithIndex(i);
            float addSec = (float) CMTime.getSecond(CMTime.subTime(model.getTimePosition(), realTimeFlag));
            float duration = addSec / model.getSpeedScale();
            CMTime durationTime = new CMTime(duration);
            CMTimeRange range = new CMTimeRange(timeFlag, durationTime);
            CMTimeRange realRange = new CMTimeRange(realTimeFlag, CMTime.subTime(model.getTimePosition(), realTimeFlag));
            float start = (float) CMTime.getSecond(timeFlag);
            float end = start + duration;
            if (timeValue >= start
                    && timeValue <= end) {
                CMTime lessTime = CMTime.subTime(time, timeFlag);
                float lt = (float) CMTime.getSecond(lessTime);
                lt = lt * model.getSpeedScale();
                float st = (float) CMTime.getSecond(realTimeFlag);
                float t = st + lt;
                CMTime timePosition = new CMTime(t);
                originTime = timePosition;
                break;
            }
            realTimeFlag = realRange.getEnd();
            timeFlag = range.getEnd();
        }
        return originTime;
    }

    public void addSpeedPoint(CMTime time) {
        time = this.originTimeWithTime(time);
        float timeValue = (float) CMTime.getSecond(time);
        int count = speedPointsCount();
        CMTime timeFlag = CMTime.zeroTime();
        CMTime realTimeFlag = CMTime.zeroTime();
        for (int i = 1; i < count; i++) {
            TimeScaleModel model = speedPointWithIndex(i);
            float addSec = (float) CMTime.getSecond(CMTime.subTime(model.getTimePosition(), realTimeFlag));
            float duration = addSec;
            CMTime durationTime = new CMTime(duration);
            CMTimeRange range = new CMTimeRange(timeFlag, durationTime);
            CMTimeRange realRange = new CMTimeRange(realTimeFlag, CMTime.subTime(model.getTimePosition(), realTimeFlag));
            float start = (float) CMTime.getSecond(timeFlag);
            float end = start + duration;
            if (timeValue >= start
                    && timeValue <= end) {
                CMTime lessTime = CMTime.subTime(time, timeFlag);
                float lt = (float) CMTime.getSecond(lessTime);
                float st = (float) CMTime.getSecond(realTimeFlag);
                float t = st + lt;
                CMTime timePosition = new CMTime(t);
                TimeScaleModel curreantModel = new TimeScaleModel(timePosition, model.getSpeedScale());
                this.insertSpeedPoint(curreantModel, i);
                if (model.getSpeedScale() != 1) {
                    needRebuildVideo = true;
                    needRebuildAudio = true;
                }
                return;
            }
            realTimeFlag = realRange.getEnd();
            timeFlag = range.getEnd();
        }
    }

    /**
     * 增加变速点
     */
    public void insertSpeedPoint(TimeScaleModel point, int index) {
        mProject.insertSpeedPoint(point, index);
        this.needRebuildVideo = true;
        this.needRebuildAudio = true;
    }

    public void updateSpeedPointAtIndex(int index, float speedScale) {
        mProject.updateSpeedPointAtIndex(index, speedScale);
        this.needRebuildVideo = true;
        this.needRebuildAudio = true;
    }

    /**
     * 更新变速点时间
     */
    public void updateSpeedPointAtIndex(int index, CMTime timePosition) {
        mProject.updateSpeedPointAtIndex(index, timePosition);
        this.needRebuildVideo = true;
        this.needRebuildAudio = true;
    }

    /**
     * 删除变速点
     */
    public void removeSpeedPoint(TimeScaleModel point) {
        mProject.removeSpeedPoint(point);
        this.needRebuildVideo = true;
        this.needRebuildAudio = true;
    }

    /**
     * 删除变速点
     */
    public void removeSpeedPointAtIndex(int index) {
        mProject.removeSpeedPointAtIndex(index);
        this.needRebuildVideo = true;
        this.needRebuildAudio = true;
    }

    /**
     * 清空变速点
     */
    public void clearAllSpeedPoints() {
        mProject.removeAllSpeedPoints();
        this.needRebuildVideo = true;
        this.needRebuildAudio = true;
    }


    /**
     * 获取总时长
     *
     * @return
     */
    public float getAllChunkEditTimeRange() {
        float duration = 0;
        for (int i = 0; i < mProject.getChunks().size(); i++) {
            duration += getChunkEditDuration(i);
        }
        return duration;
    }

    /**
     * 获取chunk 原始视频的时长
     *
     * @param chunkIndex
     * @return
     */
    public CMTime getOrigenChunkDuration(int chunkIndex) {
        return null;
    }

    public MediaComposition getProjectMediaComposition() {
        return getProjectMediaComposition(false);
    }


    public MediaComposition getProjectMediaComposition(boolean withTailer) {
        withTailer = false;
        destory();
        preBuild();
        updateProjectOrientation();
        checkTransition();
        buildVideos();
        buildAudio();
        complementVideoTrack();
        try {
            buildAudioMix();
        } catch (Exception e) {
            e.printStackTrace();
        }
        buildRecoder();
        if (withTailer) {
            //给视频添加一个chunk，黑色的
            addTailter();
            buildTailer();
        }
        checkTracks();
        if (getAudioMixParam() != null) {
            mediaComposition.setAudioMixParam(getAudioMixParam());
        }

        MediaComposition checkedComposition = checkCompositionTime(mediaComposition);
        return checkedComposition;
    }

    /**
     * //TODO 如果出现播放卡顿，导出卡顿，需要捋一遍mediatrack,保证下一个的开始时间大于等于前一个的结束时间
     *
     * @param mediaComposition
     * @return
     */
    private MediaComposition checkCompositionTime(MediaComposition mediaComposition) {
        for (MediaTrack mediaTrack : mediaComposition.getTracks()) {
            ArrayList<Segment> segments = mediaTrack.getSegments();
            if (segments == null || segments.size() < 2) continue;
            for (int i = 1; i < segments.size(); i++) {
                Segment preSegment = segments.get(i - 1);
                Segment curSegment = segments.get(i);
                long starting = curSegment.getTimeMapping().getTargetTimeRange().getStartTime().getUs();
                long endTime = preSegment.getTimeMapping().getTargetTimeRange().getEnd().getUs();
                long dif = endTime - starting;
                if (dif > 0) {
                    curSegment.getTimeMapping().getTargetTimeRange().offset(dif);
                    curSegment.getTimeMapping().getTargetTimeRange().resize(curSegment.getTimeMapping().getTargetTimeRange().getDuration().getUs() - dif);
                }

            }
        }


        return mediaComposition;
    }

    public AudioMixParam getAudioMixParam() {
        return audioMixParam;
    }

    public AVProject getmProject() {
        return mProject;
    }

    public interface VideoEffectCallback {

    }

    public void setProjectRenderSize(GPUSize size) {
        if (mProject != null) {
            mProject.setProjectRenderSize(size);
        }
    }

    public interface ReverseCallBack {

        public void onProcess(float process);

        public void onSuccess();

        public void onFaild();

        public void onCancle();
    }

    public abstract class VideoEffectReverseCallBack implements ReverseCallBack {
        public boolean cancleReverse = false;

        public boolean isCancleReverse() {
            return cancleReverse;
        }

        public void setCancleReverse(boolean cancleReverse) {
            this.cancleReverse = cancleReverse;
        }

    }


    public interface VideoEffectSaveEventCallback {
        public void videoEffectSaveEvent();
    }

    public interface ExportCallBack {
        public void onExporting(float persent);

        public void onExportSuccess(String outputFilePath);

        public void onExportFaild(String outputFilePath);

        public void onExportCancel();
    }

    private void resetProject() {
        if (mProject != null && mProject.getChunks().size() != 0) {
            mProject.getChunks().clear();

        }
        GPUImageContext.sharedFramebufferCache().purgeAllUnassignedFramebuffers();
        releaseTrack();

        mediaComposition = new MediaComposition();

    }


    /**
     * script 删除片段
     *
     * @param index
     */
    public void scriptDeleteVideo(final int index) {
        runOnScriptThreadQueue(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < mProject.getChunks().size(); i++) {
                    Chunk chunk = mProject.getChunks().get(i);
                    if (chunk.getVideoIndex() == index) {
                        chunk.resetScriptVideo();
                    }
                }
            }
        });

    }

    /**
     * script 跟新时长
     *
     * @param index
     * @param timeRange
     */
    public void scriptUpdateTimeRange(final int index, final CMTimeRange timeRange, final LoadScriptCallBack callBack) {
        if (index > scriptVideoModels.size() - 1) return;
        Runnable loadVideoRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    ScriptVideoModel scriptVideoModel = scriptVideoModels.get(index);
                    Log.e("VideoEffect", "scriptUpdateTimeRange_range_pre_start:  " + scriptVideoModel.getInsertTimeRange().getStartTime().getSecond() + "   duration:  " + scriptVideoModel.getInsertTimeRange().getDuration().getSecond() + " end:   " + scriptVideoModel.getInsertTimeRange().getEnd().getSecond() + "after: " + timeRange.getStartTime().getSecond() + "   duration:  " + timeRange.getDuration().getSecond() + " end:   " + timeRange.getEnd().getSecond());
                    scriptVideoModel.setInsertTimeRange(timeRange);
                    for (int i = 0; i < mProject.getChunks().size(); i++) {
                        Chunk chunk = mProject.getChunks().get(i);
                        if (chunk.getVideoIndex() == index)
                            toLoadVideo(chunk);
                    }
                    if (callBack != null) callBack.loadSuccess(checkScriptCanPlay());
                } catch (Exception e) {
                    if (callBack != null) callBack.loadFaild(e);
                }
            }
        };
        runOnScriptThreadQueue(loadVideoRunnable);
    }

    /***
     * 对视频进行倒播转码
     * @param scriptVideoModel
     * @throws InvalidVideoSourceException
     */
    private void invertedTranscoder(ScriptVideoModel scriptVideoModel) throws InvalidVideoSourceException {
        String chunFilePath = scriptVideoModel.getVideoPath();
        final String reversePath = EditConstants.TEMP_REVERSE_PATH + "/" + chunFilePath.substring(chunFilePath.lastIndexOf("/") + 1).replace(".mp4", "_reverse.mp4");
        File file = new File(reversePath);
        if (!file.exists()) {
            VideoFile videoFile = VideoFile.getVideoFileInfo(scriptVideoModel.getVideoPath(), mContext);
            InvertedTranscoder invertedTranscoder = new InvertedTranscoder(mContext, scriptVideoModel.getVideoPath(),
                    0, videoFile.getcDuration().getUs(), reversePath);
            invertedTranscoder.transCode();
        }
        scriptVideoModel.setReverseVideoPath(reversePath);
    }

    /**
     * 插入videoModel
     *
     * @param scriptVideoModel
     * @param index
     */
    public void scriptAddVideo(final ScriptVideoModel scriptVideoModel, final int index, final LoadScriptCallBack callBack) {
        Runnable loadVideoRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    ScriptVideoModel newModel = scriptVideoModels.get(index);
                    newModel.setVideoPath(scriptVideoModel.getVideoPath());
                    for (int i = 0; i < mProject.getChunks().size(); i++) {
                        Chunk chunk = mProject.getChunks().get(i);
                        if (chunk.getVideoIndex() == index)
                            toLoadVideo(chunk);
                    }
                    if (callBack != null) callBack.loadSuccess(checkScriptCanPlay());
                } catch (Exception e) {
                    if (callBack != null) callBack.loadFaild(e);
                }
            }
        };
        runOnScriptThreadQueue(loadVideoRunnable);
    }

    /**
     * 重进加载chunk中视频数据
     *
     * @param chunk
     * @throws InvalidVideoSourceException
     */
    private void toLoadVideo(Chunk chunk) throws InvalidVideoSourceException {
        ScriptVideoModel videoModel = scriptVideoModels.get(chunk.getVideoIndex());
        if (mProject.needReverseVideoOfVideoIndex(chunk.getVideoIndex())) {
            videoModel = videoModel.copyModel();
            //做倒播处理
            invertedTranscoder(videoModel);
            videoModel.setReverseVideo(true);
        }
        chunk.loadVideo(mContext, videoModel);
    }

//    /**
//     * 脚本相关
//     *
//     * @param extraMusicScriptId 适用于本地音乐和录音的脚本
//     */
//    public void loadVideoScript(final Context context, final ScriptJsonBean scriptJsonBean, final List<ScriptVideoModel> videoFiles, final String extraMusicScriptId, final LoadScriptCallBack scriptCallBack) {
//        scriptVideoModels = new ArrayList<>(videoFiles);
//        resetProject();
//        mProject = AVProject.projectFromScriptBean(context, scriptJsonBean);
//        Runnable loadScriptRunnable = new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    updateCanvase();
//                    addVideoTypesetting();
//                    for (int i = 0; i < mProject.getChunks().size(); i++) {
//                        Chunk chunk = mProject.getChunks().get(i);
//                        if (chunk.getChunkType() == ChunkType.ChunkType_Black) {
//                            String videoPath = "assert://blackScreen.mp4";
//                            VideoFile videoFile = null;
//                            videoFile = VideoFile.getVideoFileInfo(videoPath, context);
//                            ScriptVideoModel model = new ScriptVideoModel();
//                            model.setVideoPath(videoPath);
//                            model.setInsertTimeRange(new CMTimeRange(CMTime.zeroTime(), videoFile.getcDuration()));
//                            model.setReverseVideo(false);
//                            try {
//                                chunk.setReverseVideo(false);
//                                chunk.loadVideo(context, model);
//                            } catch (InvalidVideoSourceException e) {
//                                e.printStackTrace();
//                            }
//                        } else if (chunk.getChunkType() == ChunkType.ChunkType_White) {
//                            String videoPath = "assert://whiteScreen.mp4";
//                            VideoFile videoFile = null;
//                            videoFile = VideoFile.getVideoFileInfo(videoPath, context);
//                            ScriptVideoModel model = new ScriptVideoModel();
//                            model.setVideoPath(videoPath);
//                            model.setReverseVideo(false);
//                            model.setInsertTimeRange(new CMTimeRange(CMTime.zeroTime(), videoFile.getcDuration()));
//                            try {
//                                chunk.setReverseVideo(false);
//                                chunk.loadVideo(context, model);
//                            } catch (InvalidVideoSourceException e) {
//                                e.printStackTrace();
//                            }
//                        } else {
//                            toLoadVideo(chunk);
//                        }
//                    }
//                    try {
//                        addBackGroundMusic(mProject.getMusicModel());
//                    } catch (InvalidVideoSourceException e) {
//                        e.printStackTrace();
//                    }
//                    if (scriptCallBack != null) scriptCallBack.loadSuccess(checkScriptCanPlay());
//                } catch (Exception e) {
//                    if (EditConstants.VERBOSE_EDIT)
//                        Log.e(EditConstants.TAG_EDIT, "error " + e.getMessage());
//                    e.printStackTrace();
//                    if (scriptCallBack != null) scriptCallBack.loadFaild(e);
//                }
//            }
//
//            /**
//             * 注意：添加视频排版 按照时间的先后顺序添加
//             * 此步骤耗时较长，所以和文字排版（主线程）放在了不同线程
//             */
//
//            private void addVideoTypesetting() {
//                if (scriptJsonBean.getModules() == null) return;
//                List<ScriptJsonBean.ModulesBean> videoTypesetingArray = new ArrayList<>();
//
//                for (int i = 0; i < scriptJsonBean.getModules().size(); i++) {
//                    ScriptJsonBean.ModulesBean modulesBean = scriptJsonBean.getModules().get(i);
//                    if (modulesBean.getType() == EffectType.EffectType_Video.getValue())
//                        videoTypesetingArray.add(modulesBean);
//                }
//                try {
//                    Collections.sort(videoTypesetingArray, new SortByCMTime());
//                } catch (Exception e) {
//                    Log.e("", "==========>排序异常" + e.getMessage());
//                }
//
//                for (int i = 0; i < videoTypesetingArray.size(); i++) {
//                    final ScriptJsonBean.ModulesBean modulesBean = videoTypesetingArray.get(i);
//                    final String[] dirPathArray = {null};
//                    Common.runOnMainQueueWithoutDeadlocking(new Runnable() {
//                        @Override
//                        public void run() {
//                            dirPathArray[0] = TypesetUtils.getVideoTypesetPath(modulesBean.getUrl());
//                        }
//                    });
//                    String dirPath = dirPathArray[0];
//                    EffectAdapter adapter = new EffectAdapter(UUID.randomUUID().toString(), EffectType.EffectType_Video);
//                    CMTimeRange timeRange = new CMTimeRange(new CMTime(modulesBean.getTimeRange().get(0), modulesBean.getTimeRange().get(1)), new CMTime(modulesBean.getTimeRange().get(2), modulesBean.getTimeRange().get(3)));
//                    String maskPath = dirPath + "/t";
//                    String maskExtPath = dirPath + "/t_m";
//                    String masktmp = maskPath + ".mp4";
//                    String masktmpExt = maskExtPath + ".mp4";
//                    if (Common.fileExist(masktmp)) {
//                        maskPath = masktmp;
//                        maskExtPath = masktmpExt;
//                        if (!Common.fileExist(masktmpExt)) {
//                            maskExtPath = masktmp;
//                        }
//                    } else {
//                        String oriStr = "_v";
//                        if (mProject.getRotation() == Origentation.kVideo_Horizontal) {
//                            oriStr = "_h";
//                        }
//                        maskPath = maskPath + oriStr + ".mp4";
//                        maskExtPath = maskExtPath + oriStr + ".mp4";
//                        if (!Common.fileExist(maskExtPath)) {
//                            maskExtPath = maskPath;
//                        }
//                    }
//                    try {
//                        if (VERBOSE)
//                            Log.i("Check_Typesetting_Path", "projectFromScriptBean_Typesetting Path maskPath：" + maskPath + "；Is Exists=" + FileUtils.fileIsExists(maskPath));
//                        if (VERBOSE)
//                            Log.i("Check_Typesetting_Path", "projectFromScriptBean_Typesetting Path maskExtPath：" + maskExtPath + "；Is Exists=" + FileUtils.fileIsExists(maskExtPath));
//                        adapter.setMaskVideoChunk(new Chunk(maskPath, context, false));
//                        adapter.setMaskExtVideoChunk(new Chunk(maskExtPath, context, false));
//                    } catch (InvalidVideoSourceException e) {
//                        e.printStackTrace();
//                    }
//                    adapter.setTimeRange(timeRange);
//                    mProject.getEffectAdapters().add(adapter);
//
//                }
//            }
//        };
//        runOnScriptThreadQueue(loadScriptRunnable);
//    }

    public void clearBitmap() {
        if (mProject != null) {
            ArrayList<EffectAdapter> effectAdapters = mProject.getEffectAdapters();
            if (effectAdapters != null) {
                for (EffectAdapter effectAdapter : effectAdapters) {
                    effectAdapter.clearBitmap();
                }
            }
        }

//        if (exportSession != null) {
//            exportSession.clearBitmap();
//        }

    }

    private boolean checkScriptCanPlay() {
        if (mProject == null && mProject.getChunks() == null) return false;
        boolean canplaye = true;
        for (int i = 0; i < mProject.getChunks().size(); i++) {
            Chunk chunk = mProject.getChunks().get(i);
            if (chunk.scriptVideoLoaded() == false) {
                canplaye = false;
                break;
            }
        }
        return canplaye;
    }

    private void runOnScriptThreadQueue(Runnable runnable) {
        if (AndroidDispatchQueue.isSameDispatchQueue(scriptRebuildQueue)) {
            runnable.run();
        } else {
            scriptRebuildQueue.dispatchAsync(runnable);
        }
    }

    private void updateCanvase() {
        if (mProject == null) return;
        Origentation origentation = mProject.getRotation();
        GPUSize size = origentation == Origentation.kVideo_Horizontal ? new GPUSize(1280, 720) : new GPUSize(720, 1280);
        mProject.setProjectRenderSize(size);
    }

//    private AVProject scriptToProject(ScriptJsonBean jsonBean) {
//        return null;
//    }
//
//    //设置modelbine
//    public void setResourceModel(ArrayList<ScriptJsonBean.ModulesBean> models) {
//        mProject.setModulesBeans(models);
//    }

//    //返回脚本对象
//    public String projectVideoScript() {
//        return mProject.projectToJsonBean();
//    }

    public void resetVideoScript() {
        //TODO 需要加载模型


    }

    public int getSourceVideoCount() {
        return 0;
    }

    public float getSourceVideoTotalDuration() {
        return 0;
    }

    public float getSourceVideoDuration(int index) {
        return 0;
    }

    public VideoLastBean getVideoLastBean() {
        return videoLastBean;
    }

    public void addTailter() {
//        if (isEditVideo) {
//            this.videoLastBean = EditorActivity.getVideoLastBean();
////            this.videoLastBean = TestMediaCodecActivity.getVideoLastBean();
//        } else if (isScriptVideo) {
//            this.videoLastBean = SelectActivity.getVideoLastBean();
//        } else {
//            return;
//        }
//        //tailchunk init
//        if (this.tailerVideo == null) {
//            try {
//                if (mProject.getRotation() == Origentation.kVideo_Horizontal) {
//                    this.tailerVideo = new Chunk(TAILER_H_MP4, mContext, false);
//                } else {
//                    this.tailerVideo = new Chunk(TAILER_V_MP4, mContext, false);
//                }
//            } catch (InvalidVideoSourceException e) {
//                Log.e(TAG_M, "VideoEffect_addTailer_error:" + TAILER_H_MP4 + ", e", e);
//            }
//        }
//        if (this.tailerVideo != null) {
//            this.tailerVideo.chunkIndex = mProject.getChunks().size();
//        }
//        //calc 时间
//        CMTime time = this.projectDuration;
//        float second = (float) this.projectDuration.getSecond();
//        if (second > EditConstants.VIDEOMAXDURATION) {
//            time = new CMTime(EditConstants.VIDEOMAXDURATION);
//        }
//        //加滤镜
////        VNITailBureFilter bureFilter = new VNITailBureFilter();
////        bureFilter.setDurationTime(CMTime.subTime(time, new CMTime(0.32f)));
////        EffectAdapter adapter = new EffectAdapter(tailerFlag, EffectType.EffectType_Filter);
////        adapter.setTimeRange(new CMTimeRange(CMTime.subTime(time, new CMTime(1.f)), new CMTime(1.f)));
////        adapter.setFilter(bureFilter);
////        getEffects().add(adapter);
////        if (VERBOSE)
////            Log.d(TAG_M, "VideoEffec_addTailter ok");


    }

    public void removeTailer() {
        EffectAdapter adapter = null;
        for (EffectAdapter effectAdapter : mProject.getEffectAdapters()) {
            if (effectAdapter.getEffectId().equals(tailerFlag)) {
                adapter = effectAdapter;
                break;
            }
        }
        if (adapter == null) return;
        mProject.getEffectAdapters().remove(adapter);
    }

    public void buildTailer() {
        float subTime = EditConstants.VIDEOMAXDURATION - 0.1f;
        if (projectDuration.getSecond() > subTime) {
            if (mediaComposition != null) {
                //从片尾的位置裁掉剩余的部分
                mediaComposition.removeFromTime(new CMTime(subTime));
            }

        }

        CMTime atTime = mediaComposition.getVideoDuration();
        int trackIndex = mProject.getChunks().size() % 2;
        MediaTrack mediaTrack = trackIndex == 0 ? firstVideoMediaTrack : secondVideoMediaTrack;
        if (mediaTrack == null && getChunks().size() == 1) {
            secondVideoMediaTrack = mediaComposition.addTrack(MediaType.MEDIA_TYPE_Video, TrackType.TrackType_Video_Second);
            mediaTrack = secondVideoMediaTrack;
        }
        if (audioMediaTrack == null) {
            audioMediaTrack = mediaComposition.addTrack(MediaType.MEDIA_TYPE_Audio, TrackType.TrackType_Main_Audio);
        }
        mediaTrack.insertTrack(this.tailerVideo.getVideoFile(), this.tailerVideo.getChunkEditTimeRange(), atTime);
        audioMediaTrack.insertTrack(this.tailerVideo.getAudioFile(), this.tailerVideo.getChunkEditTimeRange(), atTime);
        this.tailerVideo.setStartTime(atTime);
        this.tailerVideo.setEndTime(CMTime.addTime(atTime, this.tailerVideo.getVideoFile().getcDuration()));

    }

    /**
     * 获取要选择的Video的数目,不包括黑白长
     *
     * @return
     */
    public int getVideoCount() {
        if (mProject == null || mProject.getChunks() == null || mProject.getChunks().isEmpty())
            return 0;
        List<String> pathArray = new ArrayList<>();
        for (Chunk chunk : mProject.getChunks()) {
            if (chunk.getChunkType() == ChunkType.ChunkType_Default && !pathArray.contains(chunk.getFilePath()))
                pathArray.add(chunk.getFilePath());
        }
        return pathArray.size();
    }

    /**
     * 返回已有的文字排版，视频排版
     *
     * @return
     */
    public ArrayList<EffectAdapter> getAllEffect() {
        if (mProject == null) return new ArrayList<>();
        return mProject.getEffectAdapters();
    }

    /**
     * 返回所有的录音
     *
     * @return
     */
    public ArrayList<RecodeModel> getAllRecodes() {
        if (mProject == null) return new ArrayList<>();
        return mProject.getRecodeModels();
    }

    /**
     * 设置指定chunk的可视范围
     *
     * @param newViewportRange
     */
    public void updateViewportRange(int chunkIndex, ViewportRange newViewportRange) {
        if (mProject == null || mProject.getChunks().size() <= chunkIndex) return;
        mProject.getChunks().get(chunkIndex).updateViewportRange(newViewportRange);
    }

    //====================音频处理接口==================================================================

    /**
     * 添加音频
     *
     * @param path      音频路径
     * @param startTime 插入时间
     * @param trackType 两种类型  TrackType_Audio_BackGround、TrackType_Audio_Recoder
     */
    public void addAudioChunk(String path, String id, CMTime startTime, TrackType trackType) {

        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();

        mProject.addAudioChunk(path, id, startTime, trackType);
    }

    /**
     * 添加音频
     *
     * @param path               音频路径
     * @param startTime          插入时间
     * @param chunkEditTimeRange 可播放区间
     * @param trackType          音频类型
     */
    public void addAudioChunk(String path, String id, CMTime startTime, CMTimeRange chunkEditTimeRange, TrackType trackType) {
        mProject.addAudioChunk(path, id, startTime, chunkEditTimeRange, trackType);

        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();

        LogUtil.e("audioChunk", startTime.toString());
        LogUtil.e("audioChunk", chunkEditTimeRange.toString());
    }

    /**
     * 调整音频音量
     *
     * @param id
     * @param volume 音量
     */
    public void adjustAudioChunk(String id, final float volume) {
        if (VERBOSE)
            Log.d(EditConstants.TAG, "VideoEffect_adjustAudioChunk:" + id + ", volume:" + volume);

        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();

        AudioChunk chunk = mProject.getAudioChunkById(id);
        if (chunk == null) return;
        chunk.setVolume(volume);
        if (chunk.getAudioChunkType() == TrackType.TrackType_Main_Audio) {
            for (Chunk chunk1 : mProject.getChunks()) {
                if (chunk1.getChunkId().equals(id)) {
                    chunk1.setAudioVolumeProportion(volume);
                    break;
                }
            }
        }
//        if (needSave) {
//            for (final AudioChunkVo audioChunkVo : projectVo.getAudioChunkVos()) {
//                if (audioChunkVo.getChunkId().equals(id)) {
//                    DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                        @Override
//                        public void execute(Realm realm) {
//                            audioChunkVo.setVolume(volume);
//                            realm.insertOrUpdate(audioChunkVo);
//                        }
//                    });
//                    break;
//                }
//            }
//        }
    }

    /**
     * 对音频进行裁剪 或者位移
     *
     * @param id
     * @param startTime
     */
    public void adjustAudioChunkStartTime(String id, final CMTime startTime, final CMTimeRange timeRange) {
        if (VERBOSE)
            Log.d(EditConstants.TAG, "VideoEffect_adjustAudioChunkStartTime:" + id + ", startTime:" + startTime);

        LogUtil.e("audioChunk", startTime.toString());
        LogUtil.e("audioChunk", timeRange.toString());

        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();

        AudioChunk chunk = mProject.getAudioChunkById(id);
        if (chunk == null) return;
        if (startTime != null)
            chunk.setInsertTime(startTime);
        if (timeRange != null)
            chunk.setChunkEditTimeRange(timeRange);
//        if (needSave) {
//            for (final AudioChunkVo audioChunkVo : projectVo.getAudioChunkVos()) {
//                if (audioChunkVo.getChunkId().equals(id)) {
//                    DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                        @Override
//                        public void execute(Realm realm) {
//                            if (startTime != null) {
//                                audioChunkVo.getInsertTime().setValue(startTime.getValue());
//                                audioChunkVo.getInsertTime().setTimeScale(startTime.getTimeScale());
//                            }
//                            if (timeRange != null) {
//                                audioChunkVo.getChunkEditTimeRange().getStartTime().setValue(timeRange.getStartTime().getValue());
//                                audioChunkVo.getChunkEditTimeRange().getStartTime().setTimeScale(timeRange.getStartTime().getTimeScale());
//                                audioChunkVo.getChunkEditTimeRange().getDuration().setValue(timeRange.getDuration().getValue());
//                                audioChunkVo.getChunkEditTimeRange().getDuration().setValue(timeRange.getDuration().getTimeScale());
//                            }
//                            realm.insertOrUpdate(audioChunkVo);
//                        }
//                    });
//                    break;
//                }
//            }
//        }
    }

    /**
     * 删除音频
     *
     * @param id
     */
    public void deleteAudioChunkById(String id) {
        if (saveEventCallback != null) saveEventCallback.videoEffectSaveEvent();
        AudioChunk chunk = mProject.getAudioChunkById(id);
        if (chunk == null) return;
        mProject.getAudioChunks().remove(chunk);
        if (needSave) {
//            for (final AudioChunkVo audioChunkVo : projectVo.getAudioChunkVos()) {
//                if (audioChunkVo.getChunkId().equals(id)) {
//                    DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                        @Override
//                        public void execute(Realm realm) {
//                            projectVo.getAudioChunkVos().remove(audioChunkVo);
//                            realm.insertOrUpdate(projectVo);
//                        }
//                    });
//                    break;
//                }
//            }
        }
    }

    /**
     * 根据id 设置 EffectAdapter  下标
     */
    public void setEffectAdapterPositionById(String id, final int sortPosition) {
        if (mProject.getEffectAdapters() == null || mProject.getEffectAdapters().isEmpty()) return;
        for (EffectAdapter effectAdapter : mProject.getEffectAdapters()) {
            if (effectAdapter.getEffectId().equals(id)) {
                effectAdapter.setSortPosition(sortPosition);
                return;
            }
        }
        if (needSave) {
//            for (final EffectAdapterVo effectAdapterVo : projectVo.getEffectAdapters()) {
//                if (effectAdapterVo.getEffectId().equals(id)) {
//                    DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                        @Override
//                        public void execute(Realm realm) {
//                            effectAdapterVo.setSortPosition(sortPosition);
//                            realm.insertOrUpdate(effectAdapterVo);
//                        }
//                    });
//                }
//                break;
//            }
        }
    }


    public String scaleVideo(String videoPath, CMTime toDuration) {
        return null;
    }

    public interface LoadScriptCallBack {
        public void loadProcess(float progress);

        public void loadSuccess(boolean canPlay);

        public void loadFaild(Exception e);
    }

}

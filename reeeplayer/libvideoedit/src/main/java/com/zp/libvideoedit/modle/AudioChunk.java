package com.zp.libvideoedit.modle;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.zp.libvideoedit.Time.CMTime;
import com.zp.libvideoedit.Time.CMTimeRange;

import java.util.ArrayList;
import java.util.UUID;

import static com.zp.libvideoedit.Constants.TAG_A;


/**
 * 音频Chunk
 * Created by zp on 2019/6/12.
 */

public class AudioChunk {
//    private AudioChunkVo audioChunkVo;
    private String chunkId;
    private String filePath;
    private Context context;
    private int mPosition = -1;//片段位置
    private CMTime insertTime;//插入位置
    private CMTime endTime;//结束的位置
    private AudioFile audioFile;
    private CMTimeRange origonTimeRange; //音频的总时长区间
    private CMTimeRange chunkEditTimeRange;//音频可播放的区间
    private float volume = 1;//音量 默认为1；
    private TrackType audioChunkType;//音频类型 视频原音、背景音、录音
    private boolean audioPrepare = false;
    private ArrayList<TimeScaleModel> speedPoints;//只有主视频才有变速
    private boolean needSave = false;
    private boolean isAudioChunkEmpty = false;//是否是个空Chunk

    public AudioChunk(String filePath, String chunkId, Context context, TrackType audioChunkType, boolean needSave) {
        this.needSave = needSave;
        audioPrepare = false;
        this.filePath = filePath;
        this.context = context;
        this.audioChunkType = audioChunkType;
        this.chunkId = TextUtils.isEmpty(chunkId) ? UUID.randomUUID().toString() : chunkId;
        try {
            audioFile = AudioFile.getAudioFileInfo(filePath, context);
            CMTime minDuration = audioFile.getcDuration();
            insertTime = CMTime.zeroTime();
            chunkEditTimeRange = new CMTimeRange(CMTime.zeroTime(), minDuration);
            origonTimeRange = new CMTimeRange(CMTime.zeroTime(), minDuration);
            audioPrepare = audioFile != null;
        } catch (Exception e) {
            audioPrepare = false;
//            e.printStackTrace();
            Log.w(TAG_A, "Add AudioChunk Error~\n" + e.getMessage(), e);
        }
        initSaveDb();
    }

    private void initSaveDb() {
        if (!needSave || !audioPrepare) return;
//        audioChunkVo = new AudioChunkVo();
//        audioChunkVo.setChunkId(chunkId);
//        audioChunkVo.setFilePath(filePath);
//        audioChunkVo.setmPosition(mPosition);
//        audioChunkVo.setInsertTime(insertTime.timeVo());
//        audioChunkVo.setChunkEditTimeRange(chunkEditTimeRange.timeRangeVo());
//        audioChunkVo.setAudioChunkType(audioChunkType.getValue());
    }

    public AudioChunk(String filePath, String chunkId, Context context, TrackType audioChunkType) {
        this(filePath, chunkId, context, audioChunkType, false);
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileName() {
        if (audioFile == null || !audioPrepare) return null;
        return audioFile.getFileName();
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getChunkId() {
        return chunkId;
    }

    public void setChunkId(String chunkId) {
        this.chunkId = chunkId;
    }

    public CMTimeRange getOrigonTimeRange() {
        return origonTimeRange;
    }


    public CMTimeRange getChunkEditTimeRange() {
        return chunkEditTimeRange;
    }

    /**
     * 更新真是可播放时间段
     *
     * @param chunkEditTimeRange
     */
    public void setChunkEditTimeRange(final CMTimeRange chunkEditTimeRange) {
        this.chunkEditTimeRange = chunkEditTimeRange;
//        if (audioChunkVo == null || audioChunkType == TrackType.TrackType_Main_Audio) return;
//
//        if (audioChunkVo.getChunkEditTimeRange() == null)
//            audioChunkVo.setChunkEditTimeRange(chunkEditTimeRange.timeRangeVo());
//        else {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    audioChunkVo.getChunkEditTimeRange().getStartTime().setValue(chunkEditTimeRange.getStartTime().getValue());
//                    audioChunkVo.getChunkEditTimeRange().getStartTime().setTimeScale(chunkEditTimeRange.getStartTime().getTimeScale());
//                    audioChunkVo.getChunkEditTimeRange().getDuration().setValue(chunkEditTimeRange.getDuration().getValue());
//                    audioChunkVo.getChunkEditTimeRange().getDuration().setTimeScale(chunkEditTimeRange.getDuration().getTimeScale());
//                    realm.insertOrUpdate(audioChunkVo);
//                }
//            });
//        }
    }

    public float getVolume() {
        return volume;
    }

    /**
     * 设置对应的音量
     *
     * @param volume
     */
    public void setVolume(final float volume) {
        this.volume = volume;
//        if (audioChunkVo == null) return;
//        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    audioChunkVo.setVolume(volume);
//                    realm.insertOrUpdate(audioChunkVo);
//                }
//            });
//        }
    }

    public TrackType getAudioChunkType() {
        return audioChunkType;
    }

    public void setAudioChunkType(TrackType audioChunkType) {
        this.audioChunkType = audioChunkType;
    }

    public CMTime getInsertTime() {
        return insertTime;
    }

    public void setInsertTime(final CMTime insertTime) {
        this.insertTime = insertTime;
//        if (audioChunkVo == null || audioChunkType == TrackType.TrackType_Main_Audio) return;
//        if (audioChunkVo.getInsertTime() == null)
//            audioChunkVo.setInsertTime(insertTime.timeVo());
//        else {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    audioChunkVo.getInsertTime().setValue(insertTime.getValue());
//                    audioChunkVo.getInsertTime().setTimeScale(insertTime.getTimeScale());
//                    realm.insertOrUpdate(audioChunkVo);
//                }
//            });
//        }
    }

    public int getmPosition() {
        return mPosition;
    }

    public void setmPosition(int mPosition) {
        this.mPosition = mPosition;
//        if (audioChunkVo != null)
//            audioChunkVo.setmPosition(mPosition);
    }

    /**
     * 在播放总时间上的结束时间
     *
     * @return
     */
    public CMTime getEndTime() {
        CMTime endTime = new CMTime(chunkEditTimeRange.getDuration().getSecond() + insertTime.getSecond());
        return endTime;
    }

    /**
     * 在原始音频上的结束时间
     * <p>
     * 进去0.2秒防止音频，超出指定UI界面
     *
     * @return
     */
    public CMTime getOrigonEndTime() {
        CMTime endTime = new CMTime(chunkEditTimeRange.getDuration().getSecond() + chunkEditTimeRange.getStartTime().getSecond() - 0.2d);
        return endTime;
    }

    public void setEndTime(CMTime endTime) {
        this.endTime = endTime;
    }

    public boolean isAudioPrepare() {
        return audioPrepare;
    }

    public void setAudioPrepare(boolean audioPrepare) {
        this.audioPrepare = audioPrepare;
    }

    public ArrayList<TimeScaleModel> getSpeedPoints() {
        return speedPoints;
    }

    public void setSpeedPoints(ArrayList<TimeScaleModel> speedPoints) {
        this.speedPoints = speedPoints;
    }

//    public AudioChunkVo getAudioChunkVo() {
//        return audioChunkVo;
//    }
//
//    /**
//     * 草稿恢复的时候用
//     *
//     * @param audioChunkVo
//     */
//    public void setAudioChunkVo(AudioChunkVo audioChunkVo) {
//        this.audioChunkVo = audioChunkVo;
//    }

    public void setNeedSave(boolean needSave) {
        this.needSave = needSave;
    }

    public boolean isAudioChunkEmpty() {
        return isAudioChunkEmpty;
    }

    public void setAudioChunkEmpty(boolean audioChunkEmpty) {
        isAudioChunkEmpty = audioChunkEmpty;
    }
}

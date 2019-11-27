package com.zp.libvideoedit.modle;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.renderscript.Matrix4f;
import android.util.Log;


import com.zp.libvideoedit.Constants;
import com.zp.libvideoedit.GPUImage.Core.GPURect;
import com.zp.libvideoedit.GPUImage.Core.GPUSize;
import com.zp.libvideoedit.GPUImage.Filter.GPUImageTransitionFilter;
import com.zp.libvideoedit.Time.CMTime;
import com.zp.libvideoedit.Time.CMTimeRange;
import com.zp.libvideoedit.exceptions.InvalidVideoSourceException;
import com.zp.libvideoedit.modle.Transition.Origentation;
import com.zp.libvideoedit.modle.Transition.TransitionStyle;
import com.zp.libvideoedit.modle.Transition.VNITransitionFactory;
import com.zp.libvideoedit.modle.script.ScriptVideoModel;
import com.zp.libvideoedit.utils.Common;
import com.zp.libvideoedit.utils.LogUtil;
import com.zp.libvideoedit.utils.MatrixUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import static com.zp.libvideoedit.Constants.TAG_DRAF;
import static com.zp.libvideoedit.Constants.VERBOSE;


/**
 * Created by gwd on 2018/3/8.
 */

public class Chunk {
    private String filePath; //needsave  finish
    private VideoFile videoFile;
    private AudioFile audioFile;
    private String chunkId; //needsave finish
    private boolean AddTrans;
    private TransitionStyle transitionStyle; //needsave finish
    private CMTimeRange origonTimeRange; //needsave finish
    private Origentation videoAspectOrigentation;
    private MediaMetadataRetriever retriever;
    private String filterName = ""; //needsave finish
    private float lightValue; //needsavefinish
    private float contrastValue;//needsavefinish
    private float saturabilityValue;//needsavefinish
    private float shadowValue;//needsavefinish
    private float highlightValue;//needsavefinish
    private float colortemperatureValue;//needsavefinish
    private boolean isDisableColorFilter;//needsavefinish
    private float strengthValue;//need save finish
    private Context context;
    private CMTime startTime;
    private CMTime endTime;
    private CMTimeRange chunkEditTimeRange;     //needsave finish
    private float cropStart;
    private float cropEnd;
    private CMTime transitionStart;
    private CMTime transitionEnd;
    private float audioVolumeProportion; //need   save finish
    private static final String TAG = "Chunk";
    public int chunkIndex = 0;
    public CMTime originStartTime; //减去转场的时间
    //变速前开始时间,结束时间
    public CMTime startTimeBeforeSpeed;
    public CMTime endTimeBeforeSpeed;
    private GPUImageTransitionFilter transitionFilter;
    private CMTime chunkTransitionTime;
    public CMTime chunkTransitionHeadTime;
    public CMTime chunkTransitionTailTime;//变速后的转场时间
    public CMTime originTransitionHeadTime;
    public CMTime originTransitionTailTime;//变速前的转场时间
    private int transIndex;   //need   save finish
    private float[] preferredTransform;
    private float longitude = 1000; //经度 //need   save finish
    private float latitude = 1000; //纬度  //need   save finish
    private float videoRotation;
    private float[] videoRotationMatrix;
    private GPUSize videoSize;
    private GPUSize fixedSize;
    //    private ChunkVo chunkVo;
    private boolean needSave;
    private boolean isReverseVideo;
    private Matrix4f rotateTransform;
    private VideoRotateType rotateType;
    private ChunkScreenActionType screenType;//推移类型
    private Matrix4f fillTransform;   //视频填充的矩阵
    private ChunkType chunkType;
    private int videoIndex;
    private String reverseVideoPath;
    private boolean audioMute;


    /**
     * 脚本相关
     */
    private float timePoint; // 段引 原视频的百分 时间点(timePoint和duration确定 chunk的editTimeRange)
    private float duration;// 段引 原视频的时 (timePoint和duration确定 chunk的editTimeRange)
    private float minVideoDuration; //满  段的最 源视频时
    private float videoDuration; //// 段引 的源视频真实时
    //使 脚本的时候,先把 户选择的视频合并成 个composition,然后按照时间切分成分段视频
    // 为 使分段视频能正常处  向信息,需要在合并的时候索引各个源视频的 向信息和时间区间
    // render的时候根据时间查询索引信息,得到视频 向信息
    private Map<String, Object> scriptVideoMediaInfo;
    private CMTimeRange scriptVideoRange;//分段视频的时间区间
    private ViewportRange mViewportRange;//视口范围

    public Chunk(Context context) {
        this.context = context;
        chunkId = UUID.randomUUID().toString();
    }

    public Chunk(String filePath, Context context, boolean needSave) throws InvalidVideoSourceException {
        this.filePath = filePath;
        this.needSave = needSave;
        videoFile = VideoFile.getVideoFileInfo(filePath, context);
        audioFile = AudioFile.getAudioFileInfo(filePath, context);
        //db
        if (needSave) {
//            chunkVo = new ChunkVo();
//            chunkVo.setFilePath(filePath);
        }
        this.context = context;
        init();
        if (VERBOSE) Log.d(TAG, "Chunk created:" + filePath);
    }

    public Chunk(String filePath, Context context, boolean needSave, ChunkType chunkType) throws InvalidVideoSourceException {
        this.filePath = filePath;
        this.needSave = needSave;
        videoFile = VideoFile.getVideoFileInfo(filePath, context);
        audioFile = AudioFile.getAudioFileInfo(filePath, context);
        //db
        if (needSave) {
//            chunkVo = new ChunkVo();
//            chunkVo.setFilePath(filePath);
        }
        this.context = context;
        init();
        if (VERBOSE) Log.d(TAG, "Chunk created:" + filePath);
        this.setChunkType(chunkType);
    }


    public boolean scriptVideoLoaded() {
        return this.videoFile != null;
    }

    public void resetScriptVideo() {
        this.videoFile = null;
        this.audioFile = null;
    }

    public void loadVideo(Context context, ScriptVideoModel videoModel) throws InvalidVideoSourceException {
        if (isReverseVideo()) {
            this.filePath = videoModel.getReverseVideoPath();
        } else {
            this.filePath = videoModel.getVideoPath();
        }
        this.needSave = false;
        videoFile = VideoFile.getVideoFileInfo(this.filePath, context);
        audioFile = AudioFile.getAudioFileInfo(this.filePath, context);
        CMTimeRange timeRange = videoModel.getInsertTimeRange();
        origonTimeRange = new CMTimeRange(CMTime.zeroTime(), this.videoFile.getcDuration());
        if (!isReverseVideo()) {
            CMTime startTime = CMTime.addTime(timeRange.getStartTime(), CMTime.multiply(timeRange.getDuration(), timePoint));
            CMTime duration = new CMTime(getDuration());
            setChunkEditTimeRange(new CMTimeRange(startTime, duration));
        } else {
            //在原始（不是倒播视频）视频中对应的开始点
            CMTime originalStartTime = CMTime.addTime(timeRange.getStartTime(), CMTime.multiply(timeRange.getDuration(), timePoint));
            CMTime duration = new CMTime(getDuration());
            //计算在倒播视频中的结束时间点
            CMTime endTme = CMTime.subTime(origonTimeRange.getDuration(), originalStartTime);
            //计算在倒播视频中的开始时间点
            CMTime startTime = CMTime.subTime(endTme, duration);
            setChunkEditTimeRange(new CMTimeRange(startTime, duration));
        }
        setReverseVideoPath(videoModel.getReverseVideoPath());
        this.videoRotation = videoFile.getRotation();
        this.videoRotationMatrix = videoFile.getRatationMatrix();
        fixedSize = new GPUSize(videoFile.getWidth(), videoFile.getHeight());
    }


    public Chunk(String filePath, Context context) throws InvalidVideoSourceException {
//        this.chunkVo = chunkVo;
        this.filePath = filePath;
        this.needSave = true;
        videoFile = VideoFile.getVideoFileInfo(filePath, context);
        audioFile = AudioFile.getAudioFileInfo(filePath, context);
        this.context = context;
    }

    public Chunk(String filePath, Context context, VideoFile videoFile, AudioFile audioFile, boolean needSave) throws InvalidVideoSourceException {
        this.filePath = filePath;
        this.needSave = needSave;
        this.videoFile = videoFile;
        this.audioFile = audioFile;
        this.context = context;
        if (needSave) {
//            chunkVo = new ChunkVo();
//            chunkVo.setFilePath(filePath);
        }
        init();
        if (VERBOSE) Log.d(TAG, "Chunk created:" + videoFile.getFilePath());
    }

    public Chunk(String filePath, Context context, VideoFile videoFile, AudioFile audioFile, float jingdu, float weidu, boolean needSave) throws InvalidVideoSourceException {
        this.filePath = videoFile.getFilePath();
        this.needSave = needSave;
        this.videoFile = videoFile;
        this.audioFile = audioFile;
        this.context = context;
        this.longitude = jingdu;
        this.latitude = weidu;
        if (needSave) {
//            chunkVo = new ChunkVo();
//            chunkVo.setFilePath(filePath);
        }
        init();
        if (VERBOSE) Log.d(TAG, "Chunk created:" + videoFile.getFilePath());
    }

    private void init() {
        transitionStyle = TransitionStyle.VNITransitionTypeNone;
        CMTime minDuration = videoFile.getcDuration();
//        if (audioFile != null) {
//            if(audioFile.getcDuration().getSecond()>videoFile.getcDuration().getSecond()){
//                minDuration = videoFile.getcDuration();
//            }
//        }
        this.chunkEditTimeRange = new CMTimeRange(CMTime.zeroTime(), minDuration);
        origonTimeRange = new CMTimeRange(CMTime.zeroTime(), minDuration);
        chunkTransitionTime = CMTime.zeroTime();
        chunkId = UUID.randomUUID().toString();
        this.videoRotation = videoFile.getRotation();
        this.videoRotationMatrix = videoFile.getRatationMatrix();
        fixedSize = new GPUSize(videoFile.getWidth(), videoFile.getHeight());
        Matrix4f videoRotation = new Matrix4f();
        videoRotation.loadIdentity();
        this.rotateTransform = videoRotation;
        Matrix4f fillTransform = new Matrix4f();
        fillTransform.loadIdentity();
        this.fillTransform = fillTransform;
        Matrix4f rotationMatrix = new Matrix4f();
        rotationMatrix.loadIdentity();
        this.rotateTransform = rotationMatrix;
        this.rotateType = VideoRotateType.VideoRotateTypeNone;
        this.setChunkType(ChunkType.ChunkType_Default);
        this.screenType = ChunkScreenActionType.ChunkScreenActionType_None;
        //db
        if (needSave) {
//            chunkVo.setTransitionStyle(transitionStyle.getValue());
//            Log.d(TAG_DRAF, "setTransitionStyle " + transitionStyle);
//            chunkVo.setOrigonTimeRange(origonTimeRange.timeRangeVo());
//            Log.d(TAG_DRAF, "setOrigonTimeRange " + origonTimeRange.getStartTime() + "  " + origonTimeRange.getDuration());
//            chunkVo.setChunkEditTimeRange(getChunkEditTimeRange().timeRangeVo());
//            Log.d(TAG_DRAF, "setChunkEditTimeRange " + getChunkEditTimeRange().getStartTime() + "  " + getChunkEditTimeRange().getDuration());
//            chunkVo.setChunkTransitionTime(chunkTransitionTime.timeVo());
//            chunkVo.setChunkId(chunkId);
//            Log.d(TAG_DRAF, "setChunkId " + chunkId);
//            GPUSizeVo vo = new GPUSizeVo();
//            vo.setWidth(fixedSize.width);
//            vo.setHeight(fixedSize.height);
//            chunkVo.setFixedSize(vo);
//            chunkVo.setScreenType(screenType.getValue());
        }
    }

    public void setChunkId(final String chunkId) {
        this.chunkId = chunkId;
        Log.d(TAG_DRAF, "setChunkId " + chunkId);
//        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    chunkVo.setChunkId(chunkId);
//                }
//            });
//        }
    }

    public GPUImageTransitionFilter getTransitionFilter() {

        return transitionFilter;
    }

    public VideoFile getVideoFile() {
        return videoFile;
    }

    public AudioFile getAudioFile() {
        return audioFile;
    }

    public boolean isAddTrans() {
        return AddTrans;
    }

    public void setAddTrans(boolean addTrans) {
        AddTrans = addTrans;
    }

    public TransitionStyle getTransitionStyle() {
        return transitionStyle;
    }

    public float getLongitude() {
        return longitude;
    }

    public void setLongitude(final float longitude) {
        this.longitude = longitude;
        Log.d(TAG_DRAF, "setLongitude " + longitude);
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    chunkVo.setLongitude(longitude);
//                    realm.insertOrUpdate(chunkVo);
//
//                }
//            });
        }
    }


    public float getLatitude() {
        return latitude;
    }

    public void setLatitude(final float latitude) {
        this.latitude = latitude;
        Log.d(TAG_DRAF, "setLatitude " + latitude);
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    chunkVo.setLatitude(latitude);
//                    realm.insertOrUpdate(chunkVo);
//
//                }
//            });
        }
    }

    public Origentation getVideoOrigentation() {
        if (videoFile == null) {
            return Origentation.kVideo_Unknow;
        }
        return videoFile.getWidth() > videoFile.getHeight() ? Origentation.kVideo_Horizontal : Origentation.kVideo_Vertical;
    }

    public GPUSize getVideoSize() {
        if (this.videoFile != null) {
            if (this.videoRotation == 90 || this.videoRotation == 270) {
                return new GPUSize(videoFile.getHeight(), videoFile.getWidth());
            } else {
                return new GPUSize(videoFile.getWidth(), videoFile.getHeight());
            }
        }
        return new GPUSize(0, 0);
    }


    public void setVideoSize(GPUSize videoSize) {
        this.videoSize = videoSize;
    }

    public GPUSize getFixedSize() {
        return fixedSize;
    }

    public void setFixedSize(GPUSize fixedSize) {
        this.fixedSize = fixedSize;
    }

    public float getVideoRotation() {
        return videoRotation;
    }

    public void setVideoRotation(final float videoRotation) {
        Log.d(TAG_DRAF, "setVideoRotation " + videoRotation);
        this.videoRotation = videoRotation;
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    chunkVo.setVideoRotation(videoRotation);
//                    realm.insertOrUpdate(chunkVo);
//
//                }
//            });
        }
    }

    public float[] getVideoRotationMatrix() {
        return videoRotationMatrix;
    }

    public void setVideoRotationMatrix(float[] videoRotationMatrix) {
        this.videoRotationMatrix = videoRotationMatrix;
    }

    public float[] getPreferredTransform() {

        return this.getVideoFile().getRatationMatrix();
    }

    public void setPreferredTransform(final float[] preferredTransform) {
        this.preferredTransform = preferredTransform;
    }

    ArrayList<Long> decodeTimes = new ArrayList<>();


    public interface ChunkAudioWaveListener {
        void onDecode(byte[] decodedBytes, double progress);
    }

    /**
     * 获取audio wave
     *
     * @param listener
     * @throws IOException
     */
    public void chunkAudioWave(ChunkAudioWaveListener listener) throws IOException {

        AudioFile audioFile = getAudioFile();
        String filePath = audioFile.getFilePath();
        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(filePath);

        MediaFormat mediaFormat = audioFile.getFormart();
        String mediaMime = mediaFormat.getString(MediaFormat.KEY_MIME);
        MediaCodec codec = MediaCodec.createDecoderByType(mediaMime);
        codec.configure(mediaFormat, null, null, 0);
        codec.start();

        ByteBuffer[] codecInputBuffers = codec.getInputBuffers();
        ByteBuffer[] codecOutputBuffers = codec.getOutputBuffers();

        final double audioDurationUs = mediaFormat.getLong(MediaFormat.KEY_DURATION);
        final long kTimeOutUs = 10000;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        int totalRawSize = 0;
        try {
            while (!sawOutputEOS) {
                if (!sawInputEOS) {
                    int inputBufIndex = codec.dequeueInputBuffer(kTimeOutUs);
                    if (inputBufIndex >= 0) {
                        ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
                        int sampleSize = extractor.readSampleData(dstBuf, 0);
                        if (sampleSize < 0) {
                            sawInputEOS = true;
                            codec.queueInputBuffer(inputBufIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        } else {
                            long presentationTimeUs = extractor.getSampleTime();
                            codec.queueInputBuffer(inputBufIndex, 0, sampleSize, presentationTimeUs, 0);
                            extractor.advance();
                        }
                    }
                }
                int res = codec.dequeueOutputBuffer(info, kTimeOutUs);
                if (res >= 0) {

                    int outputBufIndex = res;
                    if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        LogUtil.i(TAG, "audio encoder: codec onAudioFormatChanged buffer");
                        codec.releaseOutputBuffer(outputBufIndex, false);
                        continue;
                    }

                    if (info.size != 0) {
                        ByteBuffer outBuf = codecOutputBuffers[outputBufIndex];
                        outBuf.position(info.offset);
                        outBuf.limit(info.offset + info.size);
                        byte[] data = new byte[info.size];
                        outBuf.get(data);
                        totalRawSize += data.length;
                        if (listener != null)
                            listener.onDecode(data, info.presentationTimeUs / audioDurationUs);
                        LogUtil.i(TAG, filePath + " presentationTimeUs : " + info.presentationTimeUs);
                    }

                    codec.releaseOutputBuffer(outputBufIndex, false);

                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        LogUtil.i(TAG, "saw output EOS.");
                        sawOutputEOS = true;
                    }

                } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    codecOutputBuffers = codec.getOutputBuffers();
                    LogUtil.i(TAG, "output buffers have changed.");
                } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat oformat = codec.getOutputFormat();
                    LogUtil.i(TAG, "output format has changed to " + oformat);
                }
            }

            if (listener != null)
                listener.onDecode(null, 1);

        } finally {
            codec.stop();
            codec.release();
            extractor.release();
        }
    }

    public boolean isDisableColorFilter() {
        return isDisableColorFilter;
    }

    public void setDisableColorFilter(boolean disableColorFilter) {
        isDisableColorFilter = disableColorFilter;
    }

    public void paletteChunk(PaletteType type, float strength) {
        Log.d("调色", "调色value = " + strength);
        switch (type) {
            case PaletteType_Brightness:
                setLightValue(strength);
                break;
            case PaletteType_Contrast:
                setContrastValue(strength);
                break;
            case PaletteType_Saturation:
                setSaturabilityValue(strength);
                break;
            case PaletteType_Shadow:
                setShadowValue(strength);
                break;
            case PaletteType_HighLight:
                setHighlightValue(strength);
                break;
            case PaletteType_WhiteBalance:
                setColortemperatureValue(strength);
                break;
            default:
                break;
        }
    }

    public void updateAspectFillTransFormCanvaSize(GPUSize canvaSize) {
        //每个chunk的此处来处理方向问题
        //考虑视频本身还是带着一个方向的
        //考虑视频会被旋转的情况
        //1.获取视频的宽高，考虑视频的方向
//        Log.e("AspectVideo", "canvsize" + " width: " + canvaSize.width + " height: " + canvaSize.height);
        GPUSize displaySize = canvaSize;
        GPUSize videoSize = getVideoSize();
//        Log.e("AspectVideo", "videoSize" + " width: " + videoSize.width + " height: " + videoSize.height);
        GPURect aspectFiltRect = MatrixUtils.AVMakeRectWithAspectRatioInsideRect(videoSize, new GPURect(0, 0, displaySize.width, displaySize.height), getmViewportRange());
        float[] videoMatrix = getVideoFile().getRatationMatrix();
        float scaleWidth = displaySize.width / (1.0f * aspectFiltRect.getWidth());
        float scaleHeight = displaySize.height / (1.0f * aspectFiltRect.getHeight());
        float scale = Math.max(scaleWidth, scaleHeight);
//        Log.e("AspectVideo" ,"aspectFiltRect"+" width: "+scaleWidth+" height: "+scaleHeight+" scale: "+scale);
        Matrix4f newMatrix = new Matrix4f(videoMatrix);
        newMatrix.scale(scale, scale, 1.0f);
        videoRotationMatrix = newMatrix.getArray();
//        Log.e("renderrender", "canvasize = " + canvaSize.width + " " + canvaSize.height + " videoSize " + videoSize.width + " " + videoSize.height + " aspect " + aspectFiltRect.getWidth() + " " + aspectFiltRect.getHeight() + " scale " + scale);
    }

    public CMTime getChunkTransitionEndTime() {
        if (CMTime.getSecond(getChunkTransitionTailTime()) > 0) {
            return CMTime.subTime(getEndTime(), CMTime.multiply(getChunkTransitionTailTime(), 0.5f));
        }
        return endTime;
    }

    public CMTime getChunkTransitionStartTime() {
        if (chunkTransitionHeadTime.getSecond() > 0) {
            return CMTime.addTime(getStartTime(), CMTime.multiply(getChunkTransitionHeadTime(), 0.5f));
        }
        return getStartTime();
    }

    public CMTimeRange chunkTimeRanageBeforSpeed() {
        CMTime halfHeadTransitionTime = CMTime.zeroTime();
        if (getChunkTransitionHeadTime() != null) {
            halfHeadTransitionTime = CMTime.multiply(getOriginTransitionHeadTime(), 0.5f);
        }
        CMTime halfOfTailTransitionTime = CMTime.zeroTime();
        if (getChunkTransitionTailTime() != null) {
            halfOfTailTransitionTime = CMTime.multiply(getOriginTransitionTailTime(), 0.5f);
        }
        CMTimeRange range = new CMTimeRange(CMTime.addTime(startTimeBeforeSpeed, halfHeadTransitionTime), CMTime.subTime(endTimeBeforeSpeed, halfOfTailTransitionTime));
        return range;
    }


    public void setTransition(final TransitionStyle style, Origentation rotation, CMTime transitionDuration) {
        this.transitionStyle = style;
        this.videoAspectOrigentation = rotation;
        setChunkTransitionTime(transitionDuration);
        this.transitionFilter = VNITransitionFactory.transitionFilterWithType(style, rotation);
        if (Constants.VERBOSE_EDIT)
            Log.i(Constants.TAG_EDIT, "videoEffect_chunk_setTransition : " + style + " rotation " + rotation + "  duration : " + transitionDuration.getSecond());
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    chunkVo.setTransitionStyle(style.getValue());
//                    realm.insertOrUpdate(chunkVo);
//                    if (Constants.VERBOSE_EDIT)
//                        Log.i(Constants.TAG_EDIT, "videoEffect_chunk_draft : " + " chunkIndex" + chunkIndex + style + " value : " + style.getValue() + "  chunkvo.transitionstyle: " + chunkVo.getTransitionStyle());
//
//                }
//            });
        }
    }

    public void setVideoAspectOrigentation(Origentation videoAspectOrigentation) {
        this.videoAspectOrigentation = videoAspectOrigentation;
    }

    public Origentation getVideoAspectOrigentation() {
        return videoAspectOrigentation;
    }

    public void addFilter(String filterName, float strength) {
        setFilterName(filterName);
        setStrengthValue(strength);
    }

    public CMTime getChunkTransitionHeadTime() {
        return chunkTransitionHeadTime;
    }

    public CMTime getChunkTransitionTailTime() {
        return chunkTransitionTailTime;
    }

    public CMTime getOriginTransitionHeadTime() {
        return originTransitionHeadTime;
    }

    public CMTime getOriginTransitionTailTime() {
        return originTransitionTailTime;
    }

    public void setChunkTransitionHeadTime(CMTime chunkTransitionHeadTime) {
        this.chunkTransitionHeadTime = chunkTransitionHeadTime;
    }

    public void setChunkTransitionTailTime(CMTime chunkTransitionTailTime) {
        this.chunkTransitionTailTime = chunkTransitionTailTime;
    }

    public String getReverseVideoPath() {
        return reverseVideoPath;
    }

    public void setReverseVideoPath(final String reverseVideoPath) {
        this.reverseVideoPath = reverseVideoPath;
        if (needSave) {
//            Common.runOnMainQueueWithoutDeadlocking(new Runnable() {
//                @Override
//                public void run() {
//                    DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                        @Override
//                        public void execute(Realm realm) {
//                            chunkVo.setReverseVideoPath(reverseVideoPath);
//                            realm.insertOrUpdate(chunkVo);
//
//                        }
//                    });
//                }
//            });
        }
    }

    public void setOriginTransitionHeadTime(CMTime originTransitionHeadTime) {
        this.originTransitionHeadTime = originTransitionHeadTime;
    }

    public CMTimeRange chunkTimeRangeBeforeSpeed() {
        CMTime halfOfHeadTransitionTime = CMTime.zeroTime();
        if (chunkTransitionHeadTime != null) {
            halfOfHeadTransitionTime = CMTime.multiply(originTransitionHeadTime, 0.5f);
        }
        CMTime halfOfTailTransitionTime = CMTime.zeroTime();
        if (chunkTransitionTailTime != null) {
            halfOfTailTransitionTime = CMTime.multiply(originTransitionTailTime, 0.5f);
        }
        CMTime startTime = CMTime.addTime(startTimeBeforeSpeed, halfOfHeadTransitionTime);
        CMTimeRange range = CMTimeRange.RangeFromTimeToTime(startTime, CMTime.subTime(endTimeBeforeSpeed, halfOfTailTransitionTime));
        return range;
    }

    public void setOriginTransitionTailTime(CMTime originTransitionTailTime) {
        this.originTransitionTailTime = originTransitionTailTime;
    }

    public Chunk mutableCopy() {
        Chunk newChunk = null;
        try {
            newChunk = new Chunk(this.getFilePath(), context, this.videoFile, this.audioFile, needSave);
            newChunk.setOrigonTimeRange(this.getOrigonTimeRange());
            newChunk.setChunkEditTimeRange(this.getChunkEditTimeRange());
            newChunk.setFilterName(this.filterName);
            newChunk.setStrengthValue(this.strengthValue);
            newChunk.setAudioVolumeProportion(this.audioVolumeProportion);
            newChunk.setPreferredTransform(this.preferredTransform);
            newChunk.setShadowValue(this.shadowValue);
            newChunk.setHighlightValue(this.highlightValue);
            newChunk.setContrastValue(this.contrastValue);
            newChunk.setColortemperatureValue(this.colortemperatureValue);
            newChunk.setSaturabilityValue(this.saturabilityValue);
            newChunk.setLightValue(this.lightValue);
            newChunk.setLightValue(this.lightValue);
            newChunk.setCropStart(this.cropStart);
            newChunk.setCropEnd(this.cropEnd);
            newChunk.setLatitude(this.latitude);
            newChunk.setLongitude(this.longitude);
            newChunk.setReverseVideo(this.isReverseVideo);
            newChunk.setReverseVideoPath(this.reverseVideoPath);
            newChunk.setChunkType(this.chunkType);
            newChunk.setScreenType(this.screenType);
        } catch (InvalidVideoSourceException e) {
            e.printStackTrace();
        }
        return newChunk;
    }

    public void setStartTime(CMTime startTime) {
        this.startTime = startTime;
        if (this.transitionFilter != null) {
            this.transitionFilter.setTransitionStart(startTime);
            this.transitionFilter.setTransitionEnd(CMTime.addTime(startTime, getChunkTransitionTime()));
        }
    }

    public void setEndTime(CMTime endTime) {
        this.endTime = endTime;
    }

    public CMTime getStartTime() {
        return startTime;
    }

    public CMTime getEndTime() {
        return endTime;
    }

    public CMTimeRange getOrigonTimeRange() {
        return origonTimeRange;
    }

    public void setOrigonTimeRange(final CMTimeRange origonTimeRange) {
        this.origonTimeRange = origonTimeRange;
        Log.d(TAG_DRAF, "setOrigonTimeRange " + CMTime.getSecond(origonTimeRange.getStartTime()) + "  duration " + CMTime.getSecond(origonTimeRange.getDuration()));
        if (needSave) {
//            Common.runOnMainQueueWithoutDeadlocking(new Runnable() {
//                @Override
//                public void run() {
//                    DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                        @Override
//                        public void execute(Realm realm) {
//                            if (chunkVo.getOrigonTimeRange() == null) {
//                                CMTimeRangeVo rangeVo = realm.createObject(CMTimeRangeVo.class);
//                                CMTimeVo startvo = realm.createObject(CMTimeVo.class);
//                                startvo.setValue(origonTimeRange.getStartTime().getValue());
//                                startvo.setTimeScale(origonTimeRange.getStartTime().getTimeScale());
//                                CMTimeVo durationVo = realm.createObject(CMTimeVo.class);
//                                durationVo.setValue(origonTimeRange.getDuration().getValue());
//                                durationVo.setTimeScale(origonTimeRange.getDuration().getTimeScale());
//                                chunkVo.setOrigonTimeRange(rangeVo);
//                            } else {
//                                chunkVo.getOrigonTimeRange().getStartTime().setValue(origonTimeRange.getStartTime().getValue());
//                                chunkVo.getOrigonTimeRange().getStartTime().setTimeScale(origonTimeRange.getStartTime().getTimeScale());
//                                chunkVo.getOrigonTimeRange().getDuration().setValue(origonTimeRange.getDuration().getValue());
//                                chunkVo.getOrigonTimeRange().getDuration().setTimeScale(origonTimeRange.getDuration().getTimeScale());
//                                realm.insertOrUpdate(chunkVo.getOrigonTimeRange());
//                            }
//                        }
//                    });
//                }
//            });
        }
    }

    public CMTimeRange getChunkEditTimeRange() {
        return chunkEditTimeRange;
    }

    public void setChunkEditTimeRange(final CMTimeRange chunkEditTimeRange) {
        this.chunkEditTimeRange = chunkEditTimeRange;
        Log.d(TAG_DRAF, "setChunkEditTimeRange " + CMTime.getSecond(chunkEditTimeRange.getStartTime()) + "  duration " + CMTime.getSecond(chunkEditTimeRange.getDuration()));
        if (needSave) {
//            Common.runOnMainQueueWithoutDeadlocking(new Runnable() {
//                @Override
//                public void run() {
//                    DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                        @Override
//                        public void execute(Realm realm) {
//                            if (chunkVo.getOrigonTimeRange() == null) {
//                                CMTimeRangeVo rangeVo = realm.createObject(CMTimeRangeVo.class);
//                                CMTimeVo startvo = realm.createObject(CMTimeVo.class);
//                                startvo.setValue(chunkEditTimeRange.getStartTime().getValue());
//                                startvo.setTimeScale(chunkEditTimeRange.getStartTime().getTimeScale());
//                                CMTimeVo durationVo = realm.createObject(CMTimeVo.class);
//                                durationVo.setValue(chunkEditTimeRange.getDuration().getValue());
//                                durationVo.setTimeScale(chunkEditTimeRange.getDuration().getTimeScale());
//                                chunkVo.setChunkEditTimeRange(rangeVo);
//                            } else {
//                                chunkVo.getChunkEditTimeRange().getStartTime().setValue(chunkEditTimeRange.getStartTime().getValue());
//                                chunkVo.getChunkEditTimeRange().getStartTime().setTimeScale(chunkEditTimeRange.getStartTime().getTimeScale());
//                                chunkVo.getChunkEditTimeRange().getDuration().setValue(chunkEditTimeRange.getDuration().getValue());
//                                chunkVo.getChunkEditTimeRange().getDuration().setTimeScale(chunkEditTimeRange.getDuration().getTimeScale());
//                                realm.insertOrUpdate(chunkVo.getChunkEditTimeRange());
//                            }
//                        }
//                    });
//                }
//            });
        }
    }

    public CMTime getChunkTransitionTime() {
        return chunkTransitionTime;
    }

    public void setChunkTransitionTime(final CMTime chunkTransitionTime) {
        this.chunkTransitionTime = chunkTransitionTime;
        Log.d(TAG_DRAF, "setChunkTransitionTime " + CMTime.getSecond(chunkTransitionTime));
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    if (chunkVo.getChunkTransitionTime() == null) {
//                        CMTimeVo timeVo = realm.createObject(CMTimeVo.class);
//                        timeVo.setValue(chunkTransitionTime.getValue());
//                        timeVo.setTimeScale(chunkTransitionTime.getTimeScale());
//                        chunkVo.setChunkTransitionTime(timeVo);
//                    } else {
//                        chunkVo.getChunkTransitionTime().setValue(chunkTransitionTime.getValue());
//                        chunkVo.getChunkTransitionTime().setTimeScale(chunkTransitionTime.getTimeScale());
//                        realm.insertOrUpdate(chunkVo.getChunkTransitionTime());
//                    }
//
//                }
//            });
        }
    }

    public float getCropEnd() {
        return cropEnd;
    }

    public float getCropStart() {

        return cropStart;
    }

    public int getVideoIndex() {
        return videoIndex;
    }

    public void setVideoIndex(int videoIndex) {
        this.videoIndex = videoIndex;
    }

    public void setCropStart(final float cropStart) {
        this.cropStart = cropStart;
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    chunkVo.setCropStart(cropStart);
//                    realm.insertOrUpdate(chunkVo);
//
//                }
//            });
        }

    }

    public void setCropEnd(final float cropEnd) {
        this.cropEnd = cropEnd;
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    chunkVo.setCropEnd(cropEnd);
//                    realm.insertOrUpdate(chunkVo);
//
//                }
//            });
        }

    }

    public CMTime getTransitionStart() {
        return transitionStart;
    }

    public CMTime getTransitionEnd() {
        return transitionEnd;
    }

    public void setTransitionStart(CMTime transitionStart) {
        this.transitionStart = transitionStart;
    }

    public void setTransitionEnd(CMTime transitionEnd) {
        this.transitionEnd = transitionEnd;
    }


    public float getAudioVolumeProportion() {
        return audioVolumeProportion;
    }


    public void setAudioVolumeProportion(final float audioVolumeProportion) {
        this.audioVolumeProportion = audioVolumeProportion;
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    chunkVo.setAudioVolumeProportion(audioVolumeProportion);
//                    realm.insertOrUpdate(chunkVo);
//
//                }
//            });
        }
    }

    public float getLightValue() {
        return lightValue;
    }

    public void setLightValue(final float lightValue) {
        this.lightValue = lightValue;
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    chunkVo.setLightValue(lightValue);
//                    realm.insertOrUpdate(chunkVo);
//
//                }
//            });
        }
    }


    public float getContrastValue() {
        return contrastValue;
    }

    public void setContrastValue(final float contrastValue) {
        this.contrastValue = contrastValue;
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    chunkVo.setContrastValue(contrastValue);
//                    realm.insertOrUpdate(chunkVo);
//                }
//            });
        }
    }

    public float getSaturabilityValue() {
        return saturabilityValue;
    }

    public void setSaturabilityValue(final float saturabilityValue) {
        this.saturabilityValue = saturabilityValue;
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    chunkVo.setSaturabilityValue(saturabilityValue);
//                    realm.insertOrUpdate(chunkVo);
//                }
//            });
        }

    }

//    public ChunkVo getChunkVo() {
//        return chunkVo;
//    }
//
//    public void setChunkVo(ChunkVo chunkVo) {
//        this.chunkVo = chunkVo;
//    }

    public float getShadowValue() {
        return shadowValue;
    }

    public void setShadowValue(final float shadowValue) {
        this.shadowValue = shadowValue;
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    chunkVo.setShadowValue(shadowValue);
//                    realm.insertOrUpdate(chunkVo);
//                }
//            });
        }
    }

    public float getHighlightValue() {
        return highlightValue;
    }

    public void setHighlightValue(final float highlightValue) {
        this.highlightValue = highlightValue;
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    chunkVo.setHighlightValue(highlightValue);
//                    realm.insertOrUpdate(chunkVo);
//
//                }
//            });
        }
    }

    public float getColortemperatureValue() {
        return colortemperatureValue;
    }

    public void setColortemperatureValue(final float colortemperatureValue) {
        this.colortemperatureValue = colortemperatureValue;
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    chunkVo.setColortemperatureValue(colortemperatureValue);
//                    realm.insertOrUpdate(chunkVo);
//                }
//            });
        }
    }


    public String getChunkId() {
        return chunkId;
    }

    public String getFilterName() {
        return filterName;
    }

    public void setFilterName(final String filterName) {
        this.filterName = filterName;
        Log.d(TAG_DRAF, "setFilterName " + filterName);
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    chunkVo.setFilterName(filterName);
//                    realm.insertOrUpdate(chunkVo);
//                }
//            });
        }

    }

    public float getStrengthValue() {
        return strengthValue;
    }

    public void setStrengthValue(final float strengthValue) {
        this.strengthValue = strengthValue;
        Log.d(TAG_DRAF, "setStrengthValue " + strengthValue);
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    chunkVo.setStrengthValue(strengthValue);
//                    realm.insertOrUpdate(chunkVo);
//                }
//            });
        }
    }

    public int getTransIndex() {
        return transIndex;
    }

    public void setTransIndex(final int transIndex) {
        this.transIndex = transIndex;
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    chunkVo.setTransIndex(transIndex);
//                    realm.insertOrUpdate(chunkVo);
//                }
//            });
        }
    }

    @Override
    public String toString() {
        return "Chunk{" +
                ", chunkIndex=" + chunkIndex +
                ", chunkId='" + chunkId + '\'' +
                "filePath='" + filePath + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", videoFile=" + videoFile +
                ", audioFile=" + audioFile +
                ", cropStart=" + cropStart +
                ", cropEnd=" + cropEnd +
                ", originStartTime=" + originStartTime +
                ", duration=" + duration +
                ", videoDuration=" + videoDuration +
                '}';
    }

    /**
     * 脚本相关
     */
    //加载视频(使 脚本)
    public void loadVideoAsset(String filePath) {

    }

    //满足段的最小视频时
    public float scriptMinimumVideoDuration() {
        return 0;
    }

    public void updateScriptVideoTransformWithTime(CMTime time) {

    }

    public Matrix4f scriptVideoTransformWithTime(CMTime time) {
        return null;
    }

    public float getDuration() {
        return duration;
    }

    public void setDuration(float duration) {
        this.duration = duration;
    }

    public boolean isReverseVideo() {
        return isReverseVideo;
    }

    public void setReverseVideo(final boolean reverseVideo) {
        isReverseVideo = reverseVideo;
        try {
            videoFile = VideoFile.getVideoFileInfo(isReverseVideo ? this.reverseVideoPath : filePath, context);
            audioFile = AudioFile.getAudioFileInfo(isReverseVideo ? this.reverseVideoPath : filePath, context);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (needSave) {
//            Common.runOnMainQueueWithoutDeadlocking(new Runnable() {
//                @Override
//                public void run() {
//                    DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                        @Override
//                        public void execute(Realm realm) {
//                            chunkVo.setReverseVideo(reverseVideo);
//                            realm.insertOrUpdate(chunkVo);
//                        }
//                    });
//                }
//            });
        }
    }

    public float getMinVideoDuration() {
        return minVideoDuration;
    }

    public void setMinVideoDuration(float minVideoDuration) {
        this.minVideoDuration = minVideoDuration;
    }

    public Matrix4f getRotateTransform() {
        return rotateTransform;
    }

    public void setRotateTransform(Matrix4f rotateTransform) {
        this.rotateTransform = rotateTransform;
    }

    public VideoRotateType getRotateType() {
        return rotateType;
    }

    public void setRotateType(VideoRotateType rotateType) {
        this.rotateType = rotateType;
    }

    public ChunkScreenActionType getScreenType() {
        if (screenType == null)
            return ChunkScreenActionType.ChunkScreenActionType_None;
        return screenType;
    }

    public void setScreenType(final ChunkScreenActionType screenType) {
        this.screenType = screenType;
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    chunkVo.setScreenType(screenType.getValue());
//                    realm.insertOrUpdate(chunkVo);
//                }
//            });
        }
    }

    public float getTimePoint() {
        return timePoint;
    }

    public void setTimePoint(float timePoint) {
        this.timePoint = timePoint;
    }

    public Matrix4f getFillTransform() {
        return fillTransform;
    }

    public void setFillTransform(Matrix4f fillTransform) {
        this.fillTransform = fillTransform;
    }

//    public static Chunk chunkFromeBean(Context context, ScriptJsonBean.ChunksBean chunksBean) {
//        Chunk chunk = new Chunk(context);
//        chunk.setDuration((float) chunksBean.getDuration());
//        chunk.setAudioVolumeProportion((float) chunksBean.getAudioMixProportion());
//        chunk.setColortemperatureValue((float) chunksBean.getColortemperatureValue());
//        chunk.setContrastValue((float) chunksBean.getContrastValue());
//        chunk.setDuration((float) chunksBean.getDuration());
//        Matrix4f matrix4f = new Matrix4f();
//        matrix4f.loadIdentity();
//        matrix4f.set(0, 0, 0);
//        //这个字段真的需要么？
////        chunk.setVideoRotationMatrix(matrix4f.getArray());
//
//        chunk.setTransition(TransitionStyle.getStyle(chunksBean.getTransitionStyle()), Origentation.kVideo_Unknow, new CMTime(chunksBean.getTransitionDuration().get(0), chunksBean.getTransitionDuration().get(1)));
//        chunk.setFilterName(chunksBean.getFilterName());
//        chunk.setStrengthValue((float) chunksBean.getFilterStrength());
//        chunk.setHighlightValue((float) chunksBean.getHighlightValue());
//        chunk.setReverseVideo(chunksBean.isIsReverseVideo());
//        chunk.setLightValue((float) chunksBean.getLightValue());
//        chunk.setMinVideoDuration((float) chunksBean.getMinVideoDuration());
//        chunk.setRotateTransform(MatrixUtils.Matrix4fMakeAffineTransform(chunksBean.getFillTransform()));
//        chunk.setFillTransform(MatrixUtils.Matrix4fMakeAffineTransform(chunksBean.getFillTransform()));
//        chunk.setRotateType(VideoRotateType.getrotateType(chunksBean.getRotateType()));
//        chunk.setSaturabilityValue((float) chunksBean.getSaturabilityValue());
//        chunk.setScreenType(ChunkScreenActionType.getScreenType(chunksBean.getScreenType()));
//        chunk.setShadowValue((float) chunksBean.getShadowValue());
//        chunk.setTimePoint((float) chunksBean.getTimePoint());
//        chunk.setVideoIndex(chunksBean.getVideoIndex());
//        chunk.setDuration((float) chunksBean.getDuration());
//        chunk.setChunkType(ChunkType.chunkType(chunksBean.getChunkType()));
//        return chunk;
//    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public float getVideoDuration() {
        return videoDuration;
    }

    public void setVideoDuration(float videoDuration) {
        this.videoDuration = videoDuration;
    }

    public ChunkType getChunkType() {
        return chunkType;
    }

    public void setChunkType(final ChunkType chunkType) {
        this.chunkType = chunkType;
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    chunkVo.setChunkType(chunkType.getValue());
//                    realm.insertOrUpdate(chunkVo);
//                }
//            });
        }
    }

    public boolean isAudioMute() {
        return audioMute;
    }

    public void setAudioMute(final boolean audioMute) {
        this.audioMute = audioMute;
        if (needSave) {
            if (needSave) {
//                DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                    @Override
//                    public void execute(Realm realm) {
//                        chunkVo.setAudioMute(audioMute);
//                        realm.insertOrUpdate(chunkVo);
//                    }
//                });
            }
        }
    }

    public void setNeedSave(boolean needSave) {
        this.needSave = needSave;
    }

    //脚本相关
    public CMTimeRange editTimeRangeForVideoScript() {
        return this.chunkEditTimeRange;
//        CMTimeRange editTimeRange = CMTimeRange.zeroTimeRange();
//        if (!this.isReverseVideo) {
//            editTimeRange = this.chunkEditTimeRange;
//        } else {
//            CMTimeRange originTimeRange = new CMTimeRange(CMTime.subTime(videoFile.getcDuration(), this.origonTimeRange.getEnd()), this.origonTimeRange.getDuration());
//            float duration = (float) CMTime.getSecond(originTimeRange.getDuration());
////            float cropStart = this.cropEnd;
////            float cropEnd = this.cropStart;
//            CMTime cropStartTime = CMTime.addTime(originTimeRange.getStartTime(), new CMTime(duration * cropStart));
//            CMTime cropEndTime = CMTime.addTime(originTimeRange.getStartTime(), new CMTime(duration * (1.f - cropEnd)));
//            editTimeRange = new CMTimeRange(cropStartTime, CMTime.subTime(cropEndTime, cropStartTime));
//        }
//        return editTimeRange;
    }

    /**
     * 倒播正播转换
     */
    public void toRevertChunk(boolean toReverChunk, String reverseVideoPath) {
        setReverseVideoPath(reverseVideoPath);
        if (isReverseVideo != toReverChunk) {
            CMTime newStartTime = new CMTime(videoFile.getcDuration().getValue() - chunkEditTimeRange.getEnd().getValue());
            CMTimeRange newChunkEditTimeRange = new CMTimeRange(newStartTime, chunkEditTimeRange.getDuration());
            setChunkEditTimeRange(newChunkEditTimeRange);
            CMTime duration = getOrigonTimeRange().getDuration();
            CMTime startTime = CMTime.subTime(videoFile.getcDuration(), getOrigonTimeRange().getEnd());
            setOrigonTimeRange(new CMTimeRange(startTime, duration));
        }
        setReverseVideo(toReverChunk);
    }

    /**
     * 设置可视区域
     *
     * @param newViewportRange 长度为4的数组
     */
    public void updateViewportRange(final ViewportRange newViewportRange) {
        if (newViewportRange == null) return;
        if (this.mViewportRange == null)
            this.mViewportRange = new ViewportRange();
        this.mViewportRange.upDataViewportRange(newViewportRange);
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    ViewportRangeVo mViewportRangeVo = chunkVo.getmViewportRangeVo();
//                    if (mViewportRangeVo == null) {
//                        mViewportRangeVo = realm.createObject(ViewportRangeVo.class);
//                        chunkVo.setmViewportRangeVo(mViewportRangeVo);
//                    }
//                    mViewportRangeVo.upDataViewportRangeVo(newViewportRange.getViewportRangeVo());
//                    realm.insertOrUpdate(chunkVo);
//
//                }
//            });
        }
    }

    public ViewportRange getmViewportRange() {
        if (this.mViewportRange == null)
            this.mViewportRange = new ViewportRange();
        return mViewportRange;
    }

    /**
     * 是否存在音频
     *
     * @return true 存在 false：不存在
     */
    public boolean isExistAudio() {
        return audioFile != null;
    }
}

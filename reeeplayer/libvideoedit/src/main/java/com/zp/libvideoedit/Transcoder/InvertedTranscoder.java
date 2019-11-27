package com.zp.libvideoedit.Transcoder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;

import com.zp.libvideoedit.Constants;
import com.zp.libvideoedit.Time.CMTime;
import com.zp.libvideoedit.VideoEffect;
import com.zp.libvideoedit.modle.VideoFile;
import com.zp.libvideoedit.utils.CodecUtils;
import com.zp.libvideoedit.utils.FileUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import static com.zp.libvideoedit.Constants.TAG_TR;
import static com.zp.libvideoedit.Constants.TIMEOUT_USEC;
import static com.zp.libvideoedit.Constants.US_MUTIPLE;
import static com.zp.libvideoedit.Constants.VERBOSE_TR;
import static com.zp.libvideoedit.utils.CodecUtils.createExtractor;
import static com.zp.libvideoedit.utils.CodecUtils.getAndSelectVideoTrackIndex;
import static com.zp.libvideoedit.utils.FormatUtils.caller;


/**
 * Created by gx on 2018/6/6.
 */

public class InvertedTranscoder {

    private Context context;
    /**
     * 导出视频绝对路径。文件格式为mp4;保证app已经有sd卡读写权限;如果文件夹不存在将创建文件夹。
     */
    private String outPutFilePath;
    /**
     * 输入文件或者资源.assert:// 为前缀，会从assert中读取，否则认为是绝对路径
     */
    private String inPutFilePath;

    private String cacheFilePath;


    /**
     * 如果不制定，将使用原视频的宽高
     */
    private int height = -1;
    private int width = -1;

    private int rotation = 0;
    /**
     * /**
     * 建议为 width*height*8
     */
    private int bitRate;
    /**
     * 如果不指定fps,并且原视频的fps<60,将使用原视频的fpgs
     */
    private int fps = -1;
    /**
     * 每秒的关键帧数量。全关键帧为0，-1只有第一帧率为关键帧,
     */
    private int keyIntervalPerSec = 0;
    /**
     * 某些手机(华为系列)需要设置此参数，建议设置此参数为true
     */
    private boolean forceAllKeyFrame = true;
    /**
     * 回调接口
     */
    private Callback callback;
    /**
     * 异步生成图片的数量。默认为0，不生成。否则将根据视频长度，均匀的生成.回调接口生成的图片必须手动释放s
     */
    private int countOfThumb = 0;
    /**
     * 导出音频的码率
     */
    //接口实现的变量
    private int lastProgress = -1;
    private long durationUs;
    private int thumbnIndex = 0;
    private long thumbIntervalMs;
    private long beginUs;
    private long endUs;
    //一些常量参数

    private Thread transCodeThread;
    private boolean doing = false;
    private boolean stoping = false;
    //编解码中相关中间临时变量
    private MediaFormat decoderInputVideoFormat = null;
    private MediaFormat decoderOutputVideoFormat = null;
    private MediaFormat encoderInputVideoFormat = null;
    private MediaFormat encoderOutputVideoFormat = null;

    private boolean videoExtractorDone = false;
    private boolean videoDecoderDone = false;
    private boolean videoBufferReadDone = false;
    private boolean videoEncoderDone = false;
    ByteBuffer[] videoDecoderInputBuffers = null;
    ByteBuffer[] videoDecoderOutputBuffers = null;
    ByteBuffer[] videoEncoderInputBuffers = null;
    ByteBuffer[] videoEncoderOutputBuffers = null;
    private boolean isMuxing = false;
    private int outputVideoTrack = -1;

    private int videoExtractedFrameCount = 0;
    private int videoDecodedFrameCount = 0;
    private int videoEncodedFrameCount = 0;
    private int audioExtractedFrameCount = 0;
    private int audioDecodedFrameCount = 0;
    private int audioEncodedFrameCount = 0;
    //编解码器
    private MediaExtractor videoExtractor = null;
    private MediaCodec videoDecoder = null;
    private MediaCodec videoEncoder = null;
    private MediaMuxer muxer = null;
    private int colorFormat;
    private InvertVideoExtractor invertVideoExtractor;
    //
    private ArrayList<Long> cutPoints;

    private ArrayList<BufferIndex> bufferIndices;
    private AbstractFrameStackCache bufferCacheFile;

    private String tempOutFilePath;
    private VideoEffect.VideoEffectReverseCallBack mReverseCallBack;


    public InvertedTranscoder(Context context, String inPutFilePath, long beginUs, long endUs, String outPutFilePath) {
        this.context = context;
        this.inPutFilePath = inPutFilePath;
        this.outPutFilePath = outPutFilePath;
        tempOutFilePath = outPutFilePath + "_t.mp4";
        this.beginUs = beginUs;
        this.endUs = endUs;

        this.thumbIntervalMs = 0;
        this.forceAllKeyFrame = true;
        this.keyIntervalPerSec = 0;

//        this.inPutFilePath = "/storage/emulated/0/Movies/Camera/temp_uuid_58824e72e265adbc4d5648fd2dd7508b.mp4";
    }

    public void setCallBack(VideoEffect.VideoEffectReverseCallBack mReverseCallBack) {
        this.mReverseCallBack = mReverseCallBack;
    }

    public void transCode() {
        if (VERBOSE_TR)
            Log.i(TAG_TR, caller() + "InvertTransCode...." + inPutFilePath + ",beginUs:" + beginUs + ", endUs:" + endUs + ", ToutPath:" + outPutFilePath);
        if (doing)
            throw new TranscodeRunTimeException("cant' trancode while transcoder is doing now...");
        doing = true;
        this.callback = null;
        final long beginTime = System.currentTimeMillis();
        prepare();
        try {
            boolean result = doTranscode();
            if (result && !CodecUtils.detectIFrameInterval(tempOutFilePath)) {

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        transCodeToAllKeyFrame();
                    }
                }).start();
                synchronized (trancodeLock) {
                    while (!trancodeDone) {
                        try {
                            trancodeLock.wait();
                        } catch (InterruptedException ie) {
                            Log.e(TAG_TR, "InvertedTranscoder_transCode_DEAD_transCodeToAllKeyFrame");
                        }
                    }
                }
                if (VERBOSE_TR)
                    Log.d(TAG_TR, caller() + "DEAD_transCodeToAllKeyFrame");

            } else {
                FileUtils.renameFile(tempOutFilePath, outPutFilePath);
                if ((mReverseCallBack != null)) {
                    if (!mReverseCallBack.isCancleReverse())
                        mReverseCallBack.onSuccess();
                    else {//取消倒播，删除文件
                        mReverseCallBack.onCancle();
                        FileUtils.deleteAllFile(new File(outPutFilePath));
                    }
                }
                trancodeDone = true;
            }
            long elapseTime = System.currentTimeMillis() - beginTime;
            if (VERBOSE_TR)
                Log.d(TAG_TR, caller() + "InvertedTranscoder_successed finished. elapseTime:" + elapseTime + ", outPutFile:" + outPutFilePath);
        } catch (Exception e) {
            Log.e(TAG_TR, caller() + "error by transcode,e:" + e.getMessage(), e);
            FileUtils.deleteAllFile(new File(outPutFilePath));
            if (mReverseCallBack != null)
                mReverseCallBack.onFaild();
        } finally {
            FileUtils.deleteAllFile(new File(tempOutFilePath));
            FileUtils.deleteAllFile(new File(cacheFilePath));
            doing = false;
        }
    }

    final private Object trancodeLock = new Object();
    private boolean trancodeDone = false;

    private void transCodeToAllKeyFrame() {
        trancodeDone = false;
        Transcoder transcoder = new Transcoder(context);
        transcoder.setForceAllKeyFrame(true);
        transcoder.setInPutFilePath(tempOutFilePath);
        transcoder.setOutPutFilePath(outPutFilePath);
        transcoder.setCallback(new Transcoder.Callback() {
            @Override
            public void onThumbGenerated(Transcoder transCoder, Bitmap thumb, int index, long pts) {

            }

            @Override
            public void onProgress(Transcoder transCoder, float percent) {

            }

            @Override
            public void OnSuccessed(Transcoder transCoder, String outPutFilePath) {
                synchronized (trancodeLock) {
                    trancodeDone = true;
                    trancodeLock.notifyAll();
                    if (mReverseCallBack != null)
                        mReverseCallBack.onSuccess();
                }
            }

            @Override
            public void onError(Transcoder transCoder, String errmsg) {
                synchronized (trancodeLock) {
                    trancodeDone = true;
                    trancodeLock.notifyAll();
                    //倒播异常删除倒播文件
                    FileUtils.deleteAllFile(new File(outPutFilePath));
                    if (mReverseCallBack != null)
                        mReverseCallBack.onFaild();
                }
            }
        });

        transcoder.transCode();
    }

    /**
     * @param callbackListener
     */
    private void transCode(Callback callbackListener) {
        if (VERBOSE_TR)
            Log.d(TAG_TR, caller() + "transCode...." + inPutFilePath + ",beginUs:" + beginUs + ", endUs:" + endUs + ", outPath:" + outPutFilePath);
        if (doing)
            throw new TranscodeRunTimeException("cant' trancode while transcoder is doing now...");
        doing = true;
        this.callback = callbackListener;
        final long beginTime = System.currentTimeMillis();
        prepare();
        final InvertedTranscoder self = this;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (doTranscode()) {
                        VideoFile videoFile = generateVideoFile();

                        long elapseTime = System.currentTimeMillis() - beginTime;
                        if (VERBOSE_TR)
                            Log.d(TAG_TR, caller() + "InvertedTranscoder_successed finished. elapseTime:" + elapseTime + ", outPutFile:" + outPutFilePath);
                        callback.OnSuccessed(self, videoFile);
                    }
                } catch (Exception e) {
                    Log.e(TAG_TR, caller() + "error by transcode,e:" + e.getMessage(), e);
                    callback.onError(self, e.getMessage());
                } finally {
                    doing = false;
                }
            }
        }, "ITCD").start();

    }

    @NonNull
    private VideoFile generateVideoFile() {
        VideoFile videoFile = null;
        videoFile = new VideoFile();
        videoFile.setFilePath(outPutFilePath);
        videoFile.setBitrate(bitRate);
        CMTime durationCM = new CMTime(durationUs, US_MUTIPLE);

        videoFile.setcDuration(durationCM);
        videoFile.setDefective(false);
        videoFile.setDuration((float) durationCM.getSecond());

        videoFile.setFps((float) (videoEncodedFrameCount / durationCM.getSecond()));
        videoFile.setFrameCounts(videoEncodedFrameCount);
        videoFile.setHeight(height);
        videoFile.setWidth(width);
        videoFile.setIframesIntevalSec(keyIntervalPerSec);
//                        rotation为原视频的方向。转码后对方向做了调整，统一位0
        videoFile.setRotation(0);
        videoFile.setVideoFormat(encoderOutputVideoFormat);
        return videoFile;
    }


    private void prepare() throws TranscodeRunTimeException {
        // input file
        try {
            CodecUtils.checkMediaExist(context, inPutFilePath);
        } catch (Exception e) {
            throw new TranscodeRunTimeException(e.getMessage());
        }

        if (outPutFilePath == null || outPutFilePath.length() == 0)
            throw new TranscodeRunTimeException("输出文件为空");
        //outputFile
        File outputFile = new File(outPutFilePath);
        String suffix = outPutFilePath.substring(outPutFilePath.lastIndexOf(".") + 1);
        if (!suffix.equalsIgnoreCase("mp4")) {
            throw new TranscodeRunTimeException("输出文件扩展名仅能为mp4");
        }
        if (outputFile.exists())
            Log.w(TAG_TR, Thread.currentThread().getName() + "|_file will be overwite");
        File parent = outputFile.getParentFile();
        if (!parent.exists()) parent.mkdirs();

        cacheFilePath = outputFile + ".cache";

        //video track
        try {
            videoExtractor = createExtractor(context, inPutFilePath);
        } catch (IOException e) {
            throw new TranscodeRunTimeException("invalidate video of input file.", e);
        }
        int videoInputTrack = getAndSelectVideoTrackIndex(videoExtractor);

        invertVideoExtractor = new InvertVideoExtractor(videoExtractor, beginUs, endUs);

        if (videoInputTrack == -1) {
            throw new TranscodeRunTimeException("missing video track in test video");
        }

        decoderInputVideoFormat = videoExtractor.getTrackFormat(videoInputTrack);
        if (VERBOSE_TR) {
            Log.d(TAG_TR, "Video_FROMART_decoderInputVideoFormat: " + decoderInputVideoFormat);
        }
//        durationUs = decoderInputVideoFormat.getLong(MediaFormat.KEY_DURATION);
        if (decoderInputVideoFormat.containsKey(MediaFormat.KEY_DURATION))
            durationUs = decoderInputVideoFormat.getLong(MediaFormat.KEY_DURATION);
        else {
            durationUs = CodecUtils.getDurationMS(context, inPutFilePath) * 1000;
        }


        if (beginUs < 0 || endUs <= 0 || endUs <= beginUs) {
            throw new TranscodeRunTimeException("invalidate beging and end args. begin:" + beginUs + ", end:" + endUs);
        }
        if (endUs - beginUs > durationUs) {
//            throw new TranscodeRunTimeException("invalidate beging and end args. input duration :" + (endUs - beginUs) + ", durationOfFile:" + durationUs);
            endUs = durationUs;
        }
        if (decoderInputVideoFormat.containsKey(MediaFormat.KEY_FRAME_RATE))
            fps = decoderInputVideoFormat.getInteger(MediaFormat.KEY_FRAME_RATE);
        else fps = Math.round(CodecUtils.detectFps(inPutFilePath));
        if (fps <= 0)
            fps = Constants.DEFAULT_FPS;


        width = decoderInputVideoFormat.getInteger(MediaFormat.KEY_WIDTH);
        height = decoderInputVideoFormat.getInteger(MediaFormat.KEY_HEIGHT);
        if (decoderInputVideoFormat.containsKey("crop-left") && decoderInputVideoFormat.containsKey("crop-right")) {
            width = decoderInputVideoFormat.getInteger("crop-right") + 1 - decoderInputVideoFormat.getInteger("crop-left");
        }
        if (decoderInputVideoFormat.containsKey("crop-top") && decoderInputVideoFormat.containsKey("crop-bottom")) {
            height = decoderInputVideoFormat.getInteger("crop-bottom") + 1 - decoderInputVideoFormat.getInteger("crop-top");
        }
        int capability = CodecUtils.getCodecCapability();
        Size size = CodecUtils.reduceSize(width, height, capability);
        width = size.getWidth();
        height = size.getHeight();

        if (decoderInputVideoFormat.containsKey(MediaFormat.KEY_ROTATION))
            rotation = decoderInputVideoFormat.getInteger(MediaFormat.KEY_ROTATION);
        if (rotation == 90 || rotation == 270) {
            int h = width;
            width = height;
            height = h;


        }
        // 转为全关键帧视频，所有码率要大一些
        bitRate = CodecUtils.calcBitRate(width, height, fps, 2.f);
        if (bitRate > 60 * 1024 * 1024) bitRate = 60 * 1024 * 1024;
        int totalFrame = (int) (fps * durationUs / Constants.US_MUTIPLE);

        if (countOfThumb > totalFrame && countOfThumb > 0) {
            throw new TranscodeRunTimeException("预览图过多,总共只有:" + totalFrame + "帧,却需要" + countOfThumb + "张图");
        }
        if (countOfThumb > 0) {
            thumbnIndex = 0;
            thumbIntervalMs = durationUs / countOfThumb;
        }

        if (VERBOSE_TR)
            Log.i(TAG_TR, caller() + "|_FORMAT:decoderInputVideoFormat:" + decoderInputVideoFormat);
        if (VERBOSE_TR) {
            Log.d(TAG_TR, String.format("transoder file %s to %s", inPutFilePath, outPutFilePath));
        }


    }


    private boolean doTranscode() throws Exception {
        if (VERBOSE_TR) Log.d(TAG_TR, caller());
        doing = true;
        try {
            decoderOutputVideoFormat = null;
            encoderOutputVideoFormat = null;

            videoExtractorDone = false;
            videoDecoderDone = false;
            videoEncoderDone = false;
            videoBufferReadDone = false;
            isMuxing = false;
            videoExtractedFrameCount = 0;
            videoDecodedFrameCount = 0;
            videoEncodedFrameCount = 0;
            audioExtractedFrameCount = 0;
            audioDecodedFrameCount = 0;
            audioEncodedFrameCount = 0;

            makeCutPoints();
            bufferCacheFile = new AbstractFrameStackCache.StackBufferCacheFile(cacheFilePath);


            MediaCodecInfo videoCodecInfo = CodecUtils.selectCodec(MediaFormat.MIMETYPE_VIDEO_AVC);
            if (videoCodecInfo == null) {
                throw new TranscodeRunTimeException("Unable to find an appropriate codec for " + MediaFormat.MIMETYPE_VIDEO_AVC);
            }
            if (VERBOSE_TR)
                Log.d(TAG_TR, Thread.currentThread().getName() + "|_video found codec: " + videoCodecInfo.getName());


            muxer = new MediaMuxer(tempOutFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            encoderInputVideoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
            colorFormat = selectColorFormat(videoCodecInfo, MediaFormat.MIMETYPE_VIDEO_AVC);
            encoderInputVideoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
            encoderInputVideoFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
            encoderInputVideoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
            encoderInputVideoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, keyIntervalPerSec);

            if (VERBOSE_TR)
                Log.i(TAG_TR, Thread.currentThread().getName() + "|_FORMAT:outputVideoFormat: " + encoderInputVideoFormat);
            if (VERBOSE_TR)
                Log.i(TAG_TR, Thread.currentThread().getName() + "|_FORMAT:decoderInputVideoFormat: " + decoderInputVideoFormat);


            videoEncoder = MediaCodec.createByCodecName(videoCodecInfo.getName());

            videoDecoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            videoDecoder.configure(decoderInputVideoFormat, null, null, 0);
            videoDecoder.start();

            videoDecoderInputBuffers = videoDecoder.getInputBuffers();
            videoDecoderOutputBuffers = videoDecoder.getOutputBuffers();

            int count = 0;
            number = 0;
            for (int i = cutPoints.size() - 1; i >= 0; i--) {
                if (VERBOSE_TR) Log.d(TAG_TR, "trancode__part:" + i);
//                if (count > 1) break;
                decoder(i);

                encoder(i);
                count++;
                float process = ((float) cutPoints.size() - i) / (float) cutPoints.size();
                if (VERBOSE_TR)
                    Log.i(TAG_TR, "InvertedTranscoder_doTranscode  process=" + process);
                if (process < 1 && mReverseCallBack != null) {
                    mReverseCallBack.onProcess(process);
                    if (mReverseCallBack.isCancleReverse())
                        return false;
                }
            }
            return true;

        } catch (Exception e) {
            Log.e(TAG_TR, caller() + "error by transcode:" + e.getMessage(), e);
            if (callback != null) callback.onError(this, e.getMessage());
            return false;
        } finally {
            doing = false;
            if (VERBOSE_TR)
                Log.d(TAG_TR, Thread.currentThread().getName() + "|_releasing extractor, decoder, encoder, and muxer");

            try {
                bufferCacheFile.release();
            } catch (Exception e) {
                Log.w(TAG_TR, Thread.currentThread().getName() + "|_error while delete cacheFile", e);
            }
            try {
                if (videoExtractor != null) {
                    videoExtractor.release();
                }
            } catch (Exception e) {
                Log.w(TAG_TR, Thread.currentThread().getName() + "|_error while releasing videoExtractor", e);
            }
            try {
                if (videoDecoder != null) {
                    videoDecoder.reset();
                    videoDecoder.release();
                }
            } catch (Exception e) {
                Log.w(TAG_TR, Thread.currentThread().getName() + "|_error while releasing videoDecoder", e);
            }
            try {
                if (videoEncoder != null) {
                    videoEncoder.reset();
                    videoEncoder.release();
                }
            } catch (Exception e) {
                Log.w(TAG_TR, Thread.currentThread().getName() + "|_error while releasing videoEncoder", e);
            }
            try {
                if (muxer != null) {
                    if (isMuxing) muxer.stop();
                    muxer.release();
                }
            } catch (Exception e) {
                Log.w(TAG_TR, Thread.currentThread().getName() + "|_error while releasing muxer", e);
            }
            videoExtractor = null;
            videoDecoder = null;
            videoEncoder = null;
            muxer = null;
        }

    }

    private static int selectColorFormat(MediaCodecInfo codecInfo, String mimeType) {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int colorFormat = capabilities.colorFormats[i];
            if (isRecognizedFormat(colorFormat)) {
                return colorFormat;
            }
        }
        return 0;   // not reached
    }

    private static boolean isRecognizedFormat(int colorFormat) {
        switch (colorFormat) {
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
                return true;
            default:
                return false;
        }
    }

    private void makeCutPoints() {
        if (VERBOSE_TR) Log.d(TAG_TR, caller());
        cutPoints = new ArrayList<Long>();
        int count = 0;
        for (long pts = beginUs; pts < endUs; pts += US_MUTIPLE) {
            count++;
            videoExtractor.seekTo(pts, MediaExtractor.SEEK_TO_NEXT_SYNC);
            long point = videoExtractor.getSampleTime();
            if (!cutPoints.contains(point) && point >= 0) {
                cutPoints.add(point);
            }
            if (VERBOSE_TR)
                Log.d(TAG_TR, caller() + "=====>begin=" + beginUs + ", endUs=" + endUs + ", pts=" + pts + "， point=" + point + ", count=" + count);

        }

        invertVideoExtractor.setCutPoints(cutPoints);
        if (VERBOSE_TR)
            Log.d(TAG_TR, caller() + "begin:" + beginUs + ", endUs:" + endUs + ", 分割点" + cutPoints.toString());

    }


    int frameWidth;
    int frameHeight;

    private void decoder(int partIndex) {
        if (VERBOSE_TR) Log.d(TAG_TR, caller() + "partIndex:" + partIndex);
        videoDecoderDone = false;
        videoExtractorDone = false;
        bufferCacheFile.clean();
        invertVideoExtractor.reset(partIndex);
        MediaCodec.BufferInfo videoDecoderOutputBufferInfo = videoDecoderOutputBufferInfo = new MediaCodec.BufferInfo();
        if (partIndex != cutPoints.size() - 1) {
            if (VERBOSE_TR) Log.d(TAG_TR, caller() + "set decoder..., partIndex:" + partIndex);
            videoDecoder.flush();
            if (VERBOSE_TR) Log.d(TAG_TR, caller() + "reset_ok, partIndex:" + partIndex);
        }
        while (!videoDecoderDone) {
            while (!videoExtractorDone) {
                int decoderInputBufferIndex = videoDecoder.dequeueInputBuffer(TIMEOUT_USEC);
                if (decoderInputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (VERBOSE_TR) Log.d(TAG_TR, "no video decoder input buffer");
                    break;
                }
                if (VERBOSE_TR) {
                    Log.d(TAG_TR, "video decoder: returned input buffer: " + decoderInputBufferIndex);
                }
                ByteBuffer decoderInputBuffer = videoDecoderInputBuffers[decoderInputBufferIndex];
                ExcractResult result = invertVideoExtractor.next(decoderInputBuffer, 0);
                if (VERBOSE_TR) {
                    Log.d(TAG_TR, "video extractor: returned buffer :" + result);
                }
                if (result != null && result.size > 0) {
                    videoDecoder.queueInputBuffer(decoderInputBufferIndex, 0, result.size, result.pts, result.flag);
                } else {
                    videoExtractorDone = true;
                    if (videoExtractorDone) {
                        if (VERBOSE_TR) Log.d(TAG_TR, "video extractor: EOS");
                        videoDecoder.queueInputBuffer(decoderInputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    }
                }
                videoExtractedFrameCount++;
                // We extracted a frame, let's try something else next.
                break;
            }

            while (!videoDecoderDone) {
                int decoderOutputBufferIndex = videoDecoder.dequeueOutputBuffer(
                        videoDecoderOutputBufferInfo, TIMEOUT_USEC);
                if (decoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (VERBOSE_TR) Log.d(TAG_TR, "no video decoder output buffer");
                    break;
                }
                if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    if (VERBOSE_TR) Log.d(TAG_TR, "video decoder: output buffers changed");
                    videoDecoderOutputBuffers = videoDecoder.getOutputBuffers();
                    break;
                }
                if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    decoderOutputVideoFormat = videoDecoder.getOutputFormat();
                    if (VERBOSE_TR) {
                        Log.d(TAG_TR, "Video_FROMART_video decoder: output format changed,decoderOutputVideoFormat: " + decoderOutputVideoFormat);
                    }
                    if (decoderOutputVideoFormat.containsKey(MediaFormat.KEY_COLOR_FORMAT)) {
                        if (partIndex == cutPoints.size() - 1) {
                            encoderInputVideoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, decoderOutputVideoFormat.getInteger(MediaFormat.KEY_COLOR_FORMAT));
//                            crop-top=0, crop-right=1919, color-format=21, height=1088, max_capacity=17694720, color-standard=1, crop-left=0, color-transfer=3, stride=1920, mime=video/raw, slice-height=1088, remained_resource=15621120, width=1920, color-range=2, crop-bottom=1079
                            frameWidth = decoderOutputVideoFormat.getInteger(MediaFormat.KEY_WIDTH);
                            frameHeight = decoderOutputVideoFormat.getInteger(MediaFormat.KEY_HEIGHT);
                            if (Build.VERSION.SDK_INT < 23) {
                                encoderInputVideoFormat.setInteger(MediaFormat.KEY_WIDTH, width);
                                encoderInputVideoFormat.setInteger(MediaFormat.KEY_HEIGHT, height);
                            } else {
                                encoderInputVideoFormat.setInteger(MediaFormat.KEY_WIDTH, frameWidth);
                                encoderInputVideoFormat.setInteger(MediaFormat.KEY_HEIGHT, frameHeight);
                            }

                            if (decoderOutputVideoFormat.containsKey("crop-top"))
                                encoderInputVideoFormat.setInteger(" crop-top", decoderOutputVideoFormat.getInteger("crop-top"));

                            if (decoderOutputVideoFormat.containsKey("crop-right"))
                                encoderInputVideoFormat.setInteger(" crop-right", decoderOutputVideoFormat.getInteger("crop-right"));

                            if (decoderOutputVideoFormat.containsKey("crop-bottom"))
                                encoderInputVideoFormat.setInteger(" crop-bottom", decoderOutputVideoFormat.getInteger("crop-bottom"));

                            if (decoderOutputVideoFormat.containsKey("crop-left"))
                                encoderInputVideoFormat.setInteger(" crop-left", decoderOutputVideoFormat.getInteger("crop-left"));

                            if (decoderOutputVideoFormat.containsKey("stride"))
                                encoderInputVideoFormat.setInteger(" stride", decoderOutputVideoFormat.getInteger("stride"));

                            if (decoderOutputVideoFormat.containsKey("slice-height"))
                                encoderInputVideoFormat.setInteger(" slice-height", decoderOutputVideoFormat.getInteger("slice-height"));
                            //此处opple 5.1机器不能设置该数值，其它手机默认值太小
                            if (Build.VERSION.SDK_INT > 22) {
                                encoderInputVideoFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, frameWidth * frameHeight * 4 + 1024);
                            }
                            if (VERBOSE_TR) {
                                Log.d(TAG_TR, "Video_FROMART_reconfig_encoderInputVideoFormat: " + encoderInputVideoFormat);
                            }
                            try {
                                videoEncoder.configure(encoderInputVideoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            videoEncoder.start();
                            videoEncoderInputBuffers = videoEncoder.getInputBuffers();
                            videoEncoderOutputBuffers = videoEncoder.getOutputBuffers();
                            if (VERBOSE_TR)
                                Log.d(TAG_TR, caller() + "set_videoEncoder_firstPart_init_partIndex" + partIndex);
                        } else {
                            videoEncoder.stop();
                            videoEncoder.configure(encoderInputVideoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                            videoEncoder.start();
                            if (VERBOSE_TR)
                                Log.d(TAG_TR, caller() + "set_videoEncoder_reset_ecoder_partIndex" + partIndex);
                        }
                    }
                    break;
                }
                if (VERBOSE_TR) {
                    Log.d(TAG_TR, "video decoder: returned output buffer index: " + decoderOutputBufferIndex + ", buffer:" + CodecUtils.toString(videoDecoderOutputBufferInfo));
                }
                ByteBuffer decoderOutputBuffer = videoDecoderOutputBuffers[decoderOutputBufferIndex];
                if ((videoDecoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    if (VERBOSE_TR) Log.d(TAG_TR, "video decoder: codec config buffer");
                    videoDecoder.releaseOutputBuffer(decoderOutputBufferIndex, false);
                    break;
                }
                boolean render = videoDecoderOutputBufferInfo.size != 0 && invertVideoExtractor.isValidFrame(videoDecoderOutputBufferInfo.presentationTimeUs);

                if (render) {
                    ByteBuffer outputFrame = videoDecoderOutputBuffers[decoderOutputBufferIndex];
                    outputFrame.position(videoDecoderOutputBufferInfo.offset);
                    outputFrame.limit(videoDecoderOutputBufferInfo.offset + videoDecoderOutputBufferInfo.size);
                    long pts = invertVideoExtractor.getInverPts(videoDecoderOutputBufferInfo.presentationTimeUs);
                    bufferCacheFile.push(outputFrame, pts);
//                    yuv420spToBitmap(bytes, width, height);
                    if (VERBOSE_TR)
                        Log.d(TAG_TR, "video decoder: codec config buffer_videoDecoderOutputBufferInfo.presentationTimeUs="
                                + videoDecoderOutputBufferInfo.presentationTimeUs + ";\tpts=" + pts);
                }
                if ((videoDecoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (VERBOSE_TR) Log.d(TAG_TR, "video decoder: EOS");
                    videoDecoderDone = true;
                }
                videoDecoder.releaseOutputBuffer(decoderOutputBufferIndex, render);
                videoDecodedFrameCount++;
                break;
            }
        }
    }

    private void encoder(int partIndex) {
        if (VERBOSE_TR) Log.d(TAG_TR, caller() + "encoder_partIndex:" + partIndex);

        if (partIndex != cutPoints.size() - 1) {
//            videoEncoder.stop();
            videoEncoder.reset();
            videoEncoder.configure(encoderInputVideoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            videoEncoder.start();
            if (VERBOSE_TR)
                Log.d(TAG_TR, caller() + "set_videoEncoder_reset_ecoder_partIndex" + partIndex);
        }

        MediaCodec.BufferInfo videoEncoderOutputBufferInfo = new MediaCodec.BufferInfo();
        videoEncoderDone = false;
        videoBufferReadDone = false;
        videoEncoderInputBuffers = videoEncoder.getInputBuffers();
        videoEncoderOutputBuffers = videoEncoder.getOutputBuffers();


        while (!videoEncoderDone) {

            while (!videoBufferReadDone) {
//                if (forceAllKeyFrame) {
//                    Bundle bundle = new Bundle();
//                    bundle.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
//                    videoEncoder.setParameters(bundle);
//                }
                int inputBufIndex = videoEncoder.dequeueInputBuffer(-1);
                if (VERBOSE_TR)
                    Log.d(TAG_TR, caller() + "inputBufIndex=" + inputBufIndex + " _partIndex:" + partIndex);
                if (inputBufIndex >= 0) {

                    AbstractFrameStackCache.BufferFrame bufferFrame = bufferCacheFile.pop();
                    if (VERBOSE_TR)
                        Log.d(TAG_TR, caller() + "encoder_decoder\t" + bufferFrame);

                    if (bufferFrame != null) {

                        byte[] outData;
                        if (Build.VERSION.SDK_INT < 23 && (width != frameWidth || height != frameHeight)) {
                            //解码之后大小不一样，获取原始图像数据
                            outData = getNewYUVData(bufferFrame);
                        } else {
                            outData = bufferFrame.buffer;
                        }


                        ByteBuffer inputBuf = videoEncoderInputBuffers[inputBufIndex];
                        if (VERBOSE_TR)
                            Log.d(TAG_TR, caller() + "bufferSize:" + bufferFrame.buffer.length + ", videoEncoderInputBuffer=" + inputBuf + ", path" + inPutFilePath);
                        inputBuf.clear();
                        inputBuf.put(outData);

                        videoEncoder.queueInputBuffer(inputBufIndex, 0, outData.length, bufferFrame.pts, 1);
                        if (VERBOSE_TR)
                            Log.d(TAG_TR, caller() + "encoder_queueInputBuffer, lastPts" + bufferFrame.pts + " _partIndex:" + partIndex);
                    } else {
                        videoBufferReadDone = true;
                        videoEncoder.queueInputBuffer(inputBufIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);

                    }
                    break;
                }

            }

            while (!videoEncoderDone) {
                int encoderOutputBufferIndex = videoEncoder.dequeueOutputBuffer(videoEncoderOutputBufferInfo, -1);
                if (VERBOSE_TR)
                    Log.d(TAG_TR, caller() + "outputBufIndex=" + encoderOutputBufferIndex + " _partIndex:" + partIndex);
                if (encoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (VERBOSE_TR)
                        Log.d(TAG_TR, caller() + "no video encoder output buffer" + " _partIndex:" + partIndex);
                    break;
                }
                if (encoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    if (VERBOSE_TR)
                        Log.d(TAG_TR, caller() + "video encoder: output buffers changed" + " _partIndex:" + partIndex);
                    videoEncoderOutputBuffers = videoEncoder.getOutputBuffers();
                    break;
                }
                if (encoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (VERBOSE_TR)
                        Log.d(TAG_TR, caller() + "video encoder: output format changed" + " _partIndex:" + partIndex);
                    encoderOutputVideoFormat = videoEncoder.getOutputFormat();
                    startMuxer(encoderOutputVideoFormat);
                    break;
                }
                if (VERBOSE_TR) {
                    Log.d(TAG_TR, caller() + "video encoder: returned output buffer: "
                            + encoderOutputBufferIndex + ", bufferInfo:" + CodecUtils.toString(videoEncoderOutputBufferInfo) + " _partIndex:" + partIndex);
                }
                ByteBuffer encoderOutputBuffer = videoEncoderOutputBuffers[encoderOutputBufferIndex];
                if ((videoEncoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    if (VERBOSE_TR)
                        Log.d(TAG_TR, caller() + "video encoder: codec config buffer" + " _partIndex:" + partIndex);
                    videoEncoder.releaseOutputBuffer(encoderOutputBufferIndex, false);
                    break;
                }
                if (videoEncoderOutputBufferInfo.size != 0) {
                    muxer.writeSampleData(outputVideoTrack, encoderOutputBuffer, videoEncoderOutputBufferInfo);
                    if (VERBOSE_TR)
                        Log.d(TAG_TR, caller() + "encoder_decoder&writebuffer, Pts" + CodecUtils.toString(videoEncoderOutputBufferInfo) + " _partIndex:" + partIndex);
                }
                if ((videoEncoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (VERBOSE_TR)
                        Log.d(TAG_TR, caller() + "video encoder: EOS" + " _partIndex:" + partIndex + " ,pts:" + videoEncoderOutputBufferInfo.presentationTimeUs + " ,flag:" + (videoEncoderOutputBufferInfo.flags));
                    videoEncoderDone = true;
                }
                videoEncoder.releaseOutputBuffer(encoderOutputBufferIndex, false);
                videoEncodedFrameCount++;
                // We enqueued an encoded frame, let's try something else next.
                break;
            }
        }


    }


    private static void yuvCopy(byte[] src, int offset, int inWidth, int inHeight, byte[] dest, int outWidth, int outHeight) {
        for (int h = 0; h < inHeight; h++) {
            if (h < outHeight) {
                System.arraycopy(src, offset + h * inWidth, dest, h * outWidth, outWidth);
            }
        }
    }

    /**
     * 组装yuv数据
     *
     * @param bufferFrame
     * @return
     */
    private byte[] getNewYUVData(AbstractFrameStackCache.BufferFrame bufferFrame) {
        byte[] yData = new byte[width * height];
        byte[] uData = new byte[width * height / 4];
        byte[] vData = new byte[width * height / 4];

        yuvCopy(bufferFrame.buffer, 0, frameWidth, frameHeight, yData, width, height);
        yuvCopy(bufferFrame.buffer, frameWidth * frameHeight,
                frameWidth / 2, frameHeight / 2,
                uData, width / 2, height / 2);
        yuvCopy(bufferFrame.buffer, frameWidth * frameHeight * 5 / 4,
                frameWidth / 2, frameHeight / 2,
                vData, width / 2, height / 2);

        int pixelLength = width * height;
        byte[] outData = new byte[pixelLength * 3 / 2];
        System.arraycopy(yData, 0, outData, 0, pixelLength);

        /**
         * 大部分视频解码器的输出的原始图像都是 I420 格式（例如安卓下的图像通常都是 I420 或 NV21），
         * 而多数硬解码器中使用的都是 NV12 格式（例如 Intel MSDK、NVIDIA 的 cuvid、IOS 硬解码）
         */
        switch (colorFormat) {
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
                //I420
                for (int i = 0; i < pixelLength / 4; i++) {
                    outData[pixelLength + i] = uData[i];
                }
                for (int i = 0; i < pixelLength / 4; i++) {
                    outData[pixelLength + pixelLength / 4 + i] = vData[i];
                }
                break;
//            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
//                //NV12
//                for (int i = 0; i < pixelLength / 4; i++) {
//                    outData[pixelLength + i * 2] = uData[i];
//                    outData[pixelLength + i * 2 + 1] = vData[i];
//                }
//                break;
        }
        return outData;
    }

    private boolean startMuxer(MediaFormat format) {
        if (VERBOSE_TR) Log.d(TAG_TR, caller());
        if (isMuxing) return true;
        outputVideoTrack = muxer.addTrack(format);
        muxer.start();
        isMuxing = true;
        return true;
    }

    private void logState() {
        if (VERBOSE_TR) {
//            Log.d(TAG_TR, String.format("%s|_loop: " + "V(%b){" + "extracted:%d(done:%b) " + "decoded:%d(done:%b) " + "encoded:%d(done:%b)} " + "A(%b){" + "extracted:%d(done:%b) " + "decoded:%d(done:%b) " + "encoded:%d(done:%b) " + "muxing:%b(V:%d,A:%d)", Thread.currentThread().getName(), mCopyVideo, videoExtractedFrameCount, videoExtractorDone, videoDecodedFrameCount, videoDecoderDone, videoEncodedFrameCount, videoEncoderDone, mCopyAudio, audioExtractedFrameCount, audioExtractorDone, audioDecodedFrameCount, audioDecoderDone, audioEncodedFrameCount, audioEncoderDone, isMuxing, outputVideoTrack, outputAudioTrack));
        }
    }


    public int getHeight() {
        return height;
    }


    public int getWidth() {
        return width;
    }


    public int getBitRate() {
        return bitRate;
    }

    public int getFps() {
        return fps;
    }


    public void setKeyIntervalPerSec(int keyIntervalPerSec) {
        if (forceAllKeyFrame == true && keyIntervalPerSec != 0) {
            Log.w(TAG_TR, Thread.currentThread().getName() + "|_强制关键帧，设置非零的setKeyIntervalPerSec无效");
            return;
        }
        this.keyIntervalPerSec = keyIntervalPerSec;
    }

    public boolean isForceAllKeyFrame() {
        return forceAllKeyFrame;
    }

    public void setForceAllKeyFrame(boolean forceAllKeyFrame) {
        this.forceAllKeyFrame = forceAllKeyFrame;
        if (forceAllKeyFrame)
            keyIntervalPerSec = CodecUtils.getEnCodeKeyIFrameInterval();

    }


    public void setCountOfThumb(int countOfThumb) {
        this.countOfThumb = countOfThumb;
    }

    public interface Callback {
        /**
         * 生成缩略图回调
         *
         * @param transCoder
         * @param thumb      Bitmap 需要listener手动recycle
         * @param index
         * @param pts
         */
        public void onThumbGenerated(InvertedTranscoder transCoder, Bitmap thumb, int index, long pts);

        /**
         * 转码进度回调
         *
         * @param transCoder
         * @param percent    导出完成百分比
         */
        public void onProgress(InvertedTranscoder transCoder, float percent);

        /**
         * 转码完成功成回调
         *
         * @param transCoder
         */
        public void OnSuccessed(InvertedTranscoder transCoder, VideoFile videoFile);

        /**
         * 转码失败回调
         *
         * @param transCoder
         * @param errmsg
         */
        public void onError(InvertedTranscoder transCoder, String errmsg);

    }


    class BufferIndex {
        int begin;
        int length;

        public BufferIndex(int begin, int length) {
            this.begin = begin;
            this.length = length;
        }
    }

    class ExcractResult {
        public int size;
        public int flag;
        public long pts;

        public ExcractResult(int size, int flag, long pts) {
            this.size = size;
            this.flag = flag;
            this.pts = pts;
        }

        @Override
        public String toString() {
            return "ExcractResult{" + "size=" + size + ", flag=" + flag + ", pts=" + pts + '}';
        }
    }

    class InvertVideoExtractor {
        private MediaExtractor extractor;
        private ArrayList<Long> cutPoints;
        private long beginUs;
        private long endUS;

        private long currentPts;
        private long segBegin;
        private long segEnd;

        public InvertVideoExtractor(MediaExtractor extractor, long beginUs, long endUs) {
            this.extractor = extractor;
            this.beginUs = beginUs;
            this.endUS = endUs;
        }

        public void reset(int cutPointIndex) {
            currentPts = -1;
            segBegin = cutPoints.get(cutPointIndex);
            if (cutPointIndex >= cutPoints.size() - 1)
                segEnd = endUS;
            else segEnd = cutPoints.get(cutPointIndex + 1);
            //倒数第二次从新创建提取器,防止小米6手机提取到最后提取器异常问题
            if (cutPointIndex == cutPoints.size() - 2) {
                try {
                    if (extractor != null) {
                        extractor.release();
                        extractor = null;
                    }
                    extractor = createExtractor(context, inPutFilePath);
                    getAndSelectVideoTrackIndex(extractor);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            extractor.seekTo(segBegin, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
        }

        public void setCutPoints(ArrayList<Long> cutPoints) {
            this.cutPoints = cutPoints;
        }

        public ExcractResult next(ByteBuffer byteBuffer, int offset) {
            boolean hasNext = true;
            if (currentPts != -1)
                hasNext = extractor.advance();
            if (!hasNext) return null;

            currentPts = extractor.getSampleTime();
            if (currentPts >= segEnd && currentPts != endUS) {
                return null;
            }
            int size = extractor.readSampleData(byteBuffer, offset);
            int flag = extractor.getSampleFlags();
            return new ExcractResult(size, flag, currentPts);
        }

        public long getInverPts(long presentationTimeUs) {
            return endUS - presentationTimeUs;
        }

        public boolean isValidFrame(long pts) {
            return (pts >= beginUs && pts <= endUS);
        }

        public void release() {
//TODO

        }


//    private String logString(String method, String arg) {
//        return String.format("VideoTrackExtractor_%s_%d_%s\t%s", track.getTrackType().getName(), track.getTrackId(), method, arg);
//    }
//    private String logEString(String method, String arg) {
//        return FormatUtils.deviceInfo()+String.format("VideoTrackExtractor_%s_%d_%s\t%s", track.getTrackType().getName(), track.getTrackId(), method, arg);
//    }

    }


    /**
     * @param data   yuv420sp数据；
     * @param width  视频的宽度；
     * @param height 视频的高度；
     * @return
     */
    int number = 0;

    public Bitmap yuv420spToBitmap(byte[] data, int width, int height) {
        Bitmap bitmap = null;
        YuvImage image = new YuvImage(data, ImageFormat.NV21, width, height, null);
        if (image != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            image.compressToJpeg(new Rect(0, 0, width, height), 100, stream);
            bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        File file = new File("/sdcard/debugpic/" + (number++) + ".jpg");
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return bitmap;
    }

}

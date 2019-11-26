package com.zp.libvideoedit.Transcoder;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;

import com.zp.libvideoedit.R;
import com.zp.libvideoedit.utils.CodecUtils;
import com.zp.libvideoedit.utils.LogUtil;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static com.zp.libvideoedit.Constants.TAG;
import static com.zp.libvideoedit.Constants.TAG_TR;
import static com.zp.libvideoedit.Constants.VERBOSE_TR;
import static com.zp.libvideoedit.utils.FormatUtils.caller;

/**
 * 转码 管理器
 * Create by zp on 2019-11-18
 */
public class TranscodeManager implements TranscodeListener {
    //业务参数
    //音视频格式参数
    public static final String OUTPUT_VIDEO_MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;
    // parameters for the audio encoder
    public static final String OUTPUT_AUDIO_MIME_TYPE = MediaFormat.MIMETYPE_AUDIO_AAC; //"audio/mp4a-latm"; // Advanced Audio Coding
    public static final int OUTPUT_AUDIO_AAC_PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectHE;

    private Context context;
    private String inputFile;
    private String outFile;
    private boolean forceAllKeyFrame = true;
    private TranscoderNew transcoderNew;

    private MediaCodec videoEncoder = null;
    private MediaCodec audioEncoder = null;
    private boolean audioEncoderDone = false;
    private int audioEncodedFrameCount = 0;
    private MediaMuxer muxer = null;
    private boolean isMuxing = false;
    //一些常量参数
    private boolean mCopyVideo = true;
    private boolean mCopyAudio = true;
    private TranscodeInputSurface inputSurface = null;
    /**
     * 每秒的关键帧数量。全关键帧为0，-1只有第一帧率为关键帧,
     */
    private int keyIntervalPerSec = 0;
    /**
     * 导出的音频采样率.
     */
    private int outPutAudioSampleRate = -1;
    /**
     * 导出音频的码率
     */
    private int outPutAudioBitRate = -1;
    /**
     * 导出的音频音轨数量
     */
    private int outPutAudioChannelCount = -1;
    private MediaFormat encoderInputAudioFormat = null;
    private int outputVideoTrack = -1;
    private int outputAudioTrack = -1;
    private MediaFormat encoderOutputVideoFormat = null;
    private MediaFormat encoderOutputAudioFormat = null;
    private LinkedList<Integer> pendingAudioEncoderInputBufferIndices;
    private LinkedList<Integer> pendingVideoEncoderOutputBufferIndices;
    private LinkedList<MediaCodec.BufferInfo> pendingVideoEncoderOutputBufferInfos;
    private LinkedList<Integer> pendingAudioEncoderOutputBufferIndices;
    private LinkedList<MediaCodec.BufferInfo> pendingAudioEncoderOutputBufferInfos;
    private LinkedList<Integer> pendingAudioDecoderOutputBufferIndices;
    private LinkedList<MediaCodec.BufferInfo> pendingAudioDecoderOutputBufferInfos;

    private int videoEncodedFrameCount = 0;
    private boolean videoEncoderDone = false;
    private int lastProgress = -1;
    private long lastTimestampUsForAudio;//记录音频上一次的pts

    private TranscodeManagerCallback transcodeManagerCallback;
    private long durationUs;
    private Object lockObject = new Object();

    private TranscodeModle transcodeModle;

    public TranscodeManager(Context context, String inputFile, String outFile) {
        pendingAudioEncoderInputBufferIndices = new LinkedList<Integer>();
        pendingVideoEncoderOutputBufferIndices = new LinkedList<Integer>();
        pendingVideoEncoderOutputBufferInfos = new LinkedList<MediaCodec.BufferInfo>();
        pendingAudioEncoderOutputBufferIndices = new LinkedList<Integer>();
        pendingAudioEncoderOutputBufferInfos = new LinkedList<MediaCodec.BufferInfo>();
        pendingAudioDecoderOutputBufferIndices = new LinkedList<Integer>();
        pendingAudioDecoderOutputBufferInfos = new LinkedList<MediaCodec.BufferInfo>();

        this.context = context;
        this.inputFile = inputFile;
        this.outFile = outFile;
        transcoderNew = new TranscoderNew(context, inputFile, pendingAudioDecoderOutputBufferIndices, pendingAudioDecoderOutputBufferInfos);
        transcoderNew.setCallback(this);

        //验证 文件输出位置
        if (outFile == null || outFile.length() == 0)
            throw new TranscodeRunTimeException("输出文件为空");
        File outputFile = new File(outFile);
        String suffix = outFile.substring(outFile.lastIndexOf(".") + 1);
        if (!suffix.equalsIgnoreCase("mp4")) {
            throw new TranscodeRunTimeException("输出文件扩展名仅能为mp4");
        }
        if (outputFile.exists())
            Log.w(TAG_TR, Thread.currentThread().getName() + "|_file will be overwite");
        File parent = outputFile.getParentFile();
        if (!parent.exists()) parent.mkdirs();
    }

    public TranscodeManager(Context context, ArrayList<String> inputFiles, String outFile) {
        pendingAudioEncoderInputBufferIndices = new LinkedList<Integer>();
        pendingVideoEncoderOutputBufferIndices = new LinkedList<Integer>();
        pendingVideoEncoderOutputBufferInfos = new LinkedList<MediaCodec.BufferInfo>();
        pendingAudioEncoderOutputBufferIndices = new LinkedList<Integer>();
        pendingAudioEncoderOutputBufferInfos = new LinkedList<MediaCodec.BufferInfo>();
        pendingAudioDecoderOutputBufferIndices = new LinkedList<Integer>();
        pendingAudioDecoderOutputBufferInfos = new LinkedList<MediaCodec.BufferInfo>();

        this.context = context;
        this.inputFile = inputFiles.get(0);
        this.outFile = outFile;
        transcodeModle = new TranscodeModle(context, inputFiles,
                pendingAudioDecoderOutputBufferInfos, pendingAudioDecoderOutputBufferIndices, this);

        //验证 文件输出位置
        if (outFile == null || outFile.length() == 0)
            throw new TranscodeRunTimeException("输出文件为空");
        File outputFile = new File(outFile);
        String suffix = outFile.substring(outFile.lastIndexOf(".") + 1);
        if (!suffix.equalsIgnoreCase("mp4")) {
            throw new TranscodeRunTimeException("输出文件扩展名仅能为mp4");
        }
        if (outputFile.exists())
            Log.w(TAG_TR, Thread.currentThread().getName() + "|_file will be overwite");
        File parent = outputFile.getParentFile();
        if (!parent.exists()) parent.mkdirs();
    }

    public void setCallback(TranscodeManagerCallback callback) {
        this.transcodeManagerCallback = callback;
    }

    public void transCode() {
        transcoderNew.transCode();
        awaitEncode();
        if (transcodeManagerCallback != null) {
            if (lastProgress > 98) {
                transcodeManagerCallback.OnSuccessed(outFile);
            } else {//转码不完整
                transcodeManagerCallback.onError(context.getString(R.string.video_transcode_imperfect));
            }
        }
        release();
    }

    public void transCodes() {
        try {
            long startTime = System.currentTimeMillis();
            initEncode();
            transcodeModle.startTransCode();
            awaitEncode();
            if (transcodeManagerCallback != null) {
                if (lastProgress > 98) {
                    transcodeManagerCallback.OnSuccessed(outFile);
                } else {//转码不完整
                    transcodeManagerCallback.onError(context.getString(R.string.video_transcode_imperfect));
                }
            }
            LogUtil.e(TAG, "===================>转码完毕耗时：" + (System.currentTimeMillis() - startTime));
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        } finally {
            release();
        }
    }

    private void initEncode() throws Exception {
        outputVideoTrack = -1;
        outputAudioTrack = -1;
        encoderOutputVideoFormat = null;
        encoderOutputAudioFormat = null;
        audioEncoderDone = false;
        audioEncodedFrameCount = 0;
        videoEncoderDone = false;
        lastTimestampUsForAudio = 0;
        int height = transcodeModle.getmVideoHeight();
        int width = transcodeModle.getmVideoWidth();
        int bitRate = transcodeModle.getBitRate();
        int fps = transcodeModle.getFps();
        durationUs = transcodeModle.getDurationUs();

        muxer = createMuxer();
        MediaCodecInfo videoCodecInfo = CodecUtils.selectCodec(OUTPUT_VIDEO_MIME_TYPE);
        if (videoCodecInfo == null) {
            throw new TranscodeRunTimeException("Unable to find an appropriate codec for " + OUTPUT_VIDEO_MIME_TYPE);
        }
        if (VERBOSE_TR)
            Log.d(TAG_TR, Thread.currentThread().getName() + "|_video found codec: " + videoCodecInfo.getName());

        MediaCodecInfo audioCodecInfo = CodecUtils.selectCodec(OUTPUT_AUDIO_MIME_TYPE);
        if (audioCodecInfo == null) {
            // Don't fail CTS if they don't have an AAC codec (not here, anyway).
            throw new TranscodeRunTimeException("Unable to find an appropriate codec for " + OUTPUT_AUDIO_MIME_TYPE);
        }
        if (VERBOSE_TR)
            Log.d(TAG_TR, Thread.currentThread().getName() + "|_audio found codec: " + audioCodecInfo.getName());

        // Creates a muxer but do not start or add tracks just yet.

        if (VERBOSE_TR)
            Log.i(TAG_TR, Thread.currentThread().getName() + "|_invalid bitRate,use " + bitRate + "kbps");

        if (mCopyVideo) {
            MediaFormat outputVideoFormat = MediaFormat.createVideoFormat(OUTPUT_VIDEO_MIME_TYPE, width, height);

            outputVideoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            outputVideoFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
            outputVideoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
            outputVideoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, keyIntervalPerSec);
            if (VERBOSE_TR)
                Log.i(TAG_TR, Thread.currentThread().getName() + "|Transcode_FORMART1:outputVideoFormat: " + outputVideoFormat);

            AtomicReference<Surface> inputSurfaceReference = new AtomicReference<Surface>();
            videoEncoder = createVideoEncoder(videoCodecInfo, outputVideoFormat, inputSurfaceReference);
            inputSurface = new TranscodeInputSurface(inputSurfaceReference.get());
//            inputSurface.makeCurrent();
//            inputSurface.releaseEGLContext();
        }
        if (mCopyAudio) {

            MediaFormat srcAudioFormat = CodecUtils.detectAudioFormat(context, inputFile);
            outPutAudioChannelCount = srcAudioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            if (outPutAudioChannelCount > 2) outPutAudioChannelCount = 2;
            outPutAudioBitRate = (outPutAudioChannelCount == 1 ? 64 : 128) * 1024;
            outPutAudioSampleRate = srcAudioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            int maxInputSize = srcAudioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);

            encoderInputAudioFormat = MediaFormat.createAudioFormat(OUTPUT_AUDIO_MIME_TYPE, outPutAudioSampleRate, outPutAudioChannelCount);
            encoderInputAudioFormat.setInteger(MediaFormat.KEY_BIT_RATE, outPutAudioBitRate);
            encoderInputAudioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, OUTPUT_AUDIO_AAC_PROFILE);
            encoderInputAudioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, maxInputSize);

            if (VERBOSE_TR)
                Log.d(TAG_TR, Thread.currentThread().getName() + "|_AFORMAT:encoderInputAudioFormat: " + encoderInputAudioFormat);

            audioEncoder = createAudioEncoder(audioCodecInfo, encoderInputAudioFormat);
        }
    }

    /**
     * Creates a muxer to write the encoded frames.
     * <p>
     * <p>The muxer is not started as it needs to be started only after all streams have been added.
     */
    private MediaMuxer createMuxer() throws IOException {
        return new MediaMuxer(outFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
    }

    /**
     * Creates an encoder for the given format using the specified codec, taking input from a
     * surface.
     * <p>
     * <p>The surface to use as input is stored in the given reference.
     *
     * @param codecInfo        of the codec to use
     * @param format           of the stream to be produced
     * @param surfaceReference to store the surface to use as input
     */
    private MediaCodec createVideoEncoder(MediaCodecInfo codecInfo, MediaFormat format, AtomicReference<Surface> surfaceReference) throws IOException {


        MediaCodec encoder = MediaCodec.createByCodecName(codecInfo.getName());
        encoder.setCallback(new MediaCodec.Callback() {
            public void onError(MediaCodec codec, MediaCodec.CodecException exception) {
            }

            public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
                if (VERBOSE_TR)
                    Log.i(TAG_TR, Thread.currentThread().getName() + "|_FORMAT:video encoder: output format changed:" + format);
                if (outputVideoTrack >= 0) {
                    throw new TranscodeRunTimeException("should never happen video encoder changed its output format again?");
                }
                encoderOutputVideoFormat = codec.getOutputFormat();
                setupMuxer();
            }

            public void onInputBufferAvailable(MediaCodec codec, int index) {
                pendingAudioEncoderInputBufferIndices.add(index);
            }

            public void onOutputBufferAvailable(MediaCodec codec, int index, MediaCodec.BufferInfo info) {
                if (VERBOSE_TR) {
                    Log.d(TAG_TR, caller() + "|_DEAD_LOCK_video encoder: returned output buffer: " + index + "\tsize:" + info.size);
                }
                muxVideo(index, info);
            }
        });
        if (VERBOSE_TR) {
            Log.d(TAG_TR, Thread.currentThread().getName() + "|_video encoder:encoder.configure ：" + format);
        }
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        surfaceReference.set(encoder.createInputSurface());
        encoder.start();
        return encoder;
    }

    /**
     * Creates an encoder for the given format using the specified codec.
     *
     * @param codecInfo of the codec to use
     * @param format    of the stream to be produced
     */
    private MediaCodec createAudioEncoder(MediaCodecInfo codecInfo, MediaFormat format) throws IOException {
        MediaCodec encoder = MediaCodec.createByCodecName(codecInfo.getName());
        encoder.setCallback(new MediaCodec.Callback() {
            public void onError(MediaCodec codec, MediaCodec.CodecException exception) {
            }

            public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
                if (outputAudioTrack >= 0) {
                    throw new RuntimeException("should never happen.audio encoder changed its output format again?");
                }

                encoderOutputAudioFormat = codec.getOutputFormat();
                if (VERBOSE_TR) {
                    Log.i(TAG_TR, Thread.currentThread().getName() + "|Transcode_FORMART_changeed:encoderOutputAudioFormat: " + encoderOutputAudioFormat);
                }
                setupMuxer();
            }

            public void onInputBufferAvailable(MediaCodec codec, int index) {
                if (VERBOSE_TR) {
                    Log.d(TAG_TR, Thread.currentThread().getName() + "|_audio encoder: returned input buffer: " + index);
                }
                pendingAudioEncoderInputBufferIndices.add(index);
                encodeAudio();
            }

            public void onOutputBufferAvailable(MediaCodec codec, int index, MediaCodec.BufferInfo info) {
                if (VERBOSE_TR) {
                    Log.d(TAG_TR, Thread.currentThread().getName() + "|_audio encoder: returned output buffer: " + index + "\tsize " + info.size);
                }
                muxAudio(index, info);
            }
        });
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        encoder.start();
        return encoder;
    }

    private void setupMuxer() {
        if (!isMuxing && (!mCopyAudio || encoderOutputAudioFormat != null) && (!mCopyVideo || encoderOutputVideoFormat != null)) {
            if (mCopyVideo) {
                if (VERBOSE_TR)
                    Log.d(TAG_TR, Thread.currentThread().getName() + "|_muxer: adding video track.");
                outputVideoTrack = muxer.addTrack(encoderOutputVideoFormat);
            }
            if (mCopyAudio) {
                if (VERBOSE_TR)
                    Log.d(TAG_TR, Thread.currentThread().getName() + "|_muxer: adding audio track.");
                outputAudioTrack = muxer.addTrack(encoderOutputAudioFormat);
            }
            if (VERBOSE_TR) Log.d(TAG_TR, Thread.currentThread().getName() + "|_muxer: starting");
            muxer.start();
            isMuxing = true;

            MediaCodec.BufferInfo info;
            while ((info = pendingVideoEncoderOutputBufferInfos.poll()) != null) {
                int index = pendingVideoEncoderOutputBufferIndices.poll().intValue();
                muxVideo(index, info);
            }
            while ((info = pendingAudioEncoderOutputBufferInfos.poll()) != null) {
                int index = pendingAudioEncoderOutputBufferIndices.poll().intValue();
                muxAudio(index, info);
            }
        }
    }

    private void muxVideo(int index, MediaCodec.BufferInfo info) {
        if (!isMuxing) {
            pendingVideoEncoderOutputBufferIndices.add(new Integer(index));
            pendingVideoEncoderOutputBufferInfos.add(info);
            return;
        }
        ByteBuffer encoderOutputBuffer = videoEncoder.getOutputBuffer(index);
        if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            if (VERBOSE_TR)
                Log.d(TAG_TR, Thread.currentThread().getName() + "|_video encoder: codec onAudioFormatChanged buffer");
            // Simply ignore codec onAudioFormatChanged buffers.
            videoEncoder.releaseOutputBuffer(index, false);
            return;
        }
        if (VERBOSE_TR) {
            Log.d(TAG_TR, Thread.currentThread().getName() + "|_video encoder: returned buffer for time " + info.presentationTimeUs);
        }
        if (info.size != 0) {
            muxer.writeSampleData(outputVideoTrack, encoderOutputBuffer, info);
        }
        videoEncoder.releaseOutputBuffer(index, false);
        videoEncodedFrameCount++;

        int percent = (int) (info.presentationTimeUs * 100 / durationUs);
        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            if (VERBOSE_TR)
                Log.d(TAG_TR, Thread.currentThread().getName() + "|_video encoder: EOS");
            synchronized (this) {
                videoEncoderDone = true;
                notifyAll();
                Log.d(TAG_TR, Thread.currentThread().getName() + "======> muxVideo notifyAll");
            }
            percent = 100;
        }
        if (percent > lastProgress) {
            if (transcodeManagerCallback != null)
                transcodeManagerCallback.onProgress((float) percent / 100.0f);
            if (VERBOSE_TR)
                Log.d(TAG_TR, Thread.currentThread().getName() + "\ttrancode_percent:" + percent);
            lastProgress = percent;
        }
    }

    private void muxAudio(int index, MediaCodec.BufferInfo info) {
        if (!isMuxing) {
            pendingAudioEncoderOutputBufferIndices.add(new Integer(index));
            pendingAudioEncoderOutputBufferInfos.add(info);
            return;
        }
        ByteBuffer encoderOutputBuffer = audioEncoder.getOutputBuffer(index);
        if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            if (VERBOSE_TR)
                Log.d(TAG_TR, Thread.currentThread().getName() + "|_audio encoder: codec onAudioFormatChanged buffer");
            // Simply ignore codec onAudioFormatChanged buffers.
            audioEncoder.releaseOutputBuffer(index, false);
            return;
        }
        if (VERBOSE_TR) {
            Log.d(TAG_TR, Thread.currentThread().getName() + "|_audio encoder: returned buffer for time " + info.presentationTimeUs);
        }
        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0 && transcodeModle.haseNext()) {
            synchronized (this) {
                audioEncoderDone = true;
                notifyAll();
                Log.d(TAG_TR, Thread.currentThread().getName() + "======>还有TS不能写入结束语句 muxAudio notifyAll");
            }
            audioEncoder.releaseOutputBuffer(index, false);
            audioEncodedFrameCount++;
            return;
        }

        if (info.size != 0) {
            try {
                if (lastTimestampUsForAudio > info.presentationTimeUs)
                    info.presentationTimeUs = lastTimestampUsForAudio + 1l;
                muxer.writeSampleData(outputAudioTrack, encoderOutputBuffer, info);
                lastTimestampUsForAudio = info.presentationTimeUs;
            } catch (Exception e) {
                Log.e(TAG, "TSTranscoder muxAudio error:" + e.getMessage());
            }
        }
        audioEncoder.releaseOutputBuffer(index, false);
        audioEncodedFrameCount++;
        if (VERBOSE_TR)
            Log.d(TAG_TR, Thread.currentThread().getName() + "|_audio encoder: returned buffer for time " + info.presentationTimeUs + "============>1");
        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            if (VERBOSE_TR)
                Log.d(TAG_TR, Thread.currentThread().getName() + "|_audio encoder: EOS");
            synchronized (this) {
                audioEncoderDone = true;
                notifyAll();
                Log.d(TAG_TR, Thread.currentThread().getName() + "======> muxAudio notifyAll");

            }
        }
        if (VERBOSE_TR)
            Log.d(TAG_TR, Thread.currentThread().getName() + "|_audio encoder: returned buffer for time " + info.presentationTimeUs + "============>2");
//        logState();
    }

    private void release() {
        try {
            if (muxer != null) {
                if (isMuxing) muxer.stop();
                muxer.release();
            }
            muxer = null;

            try {
                if (videoEncoder != null) {
                    videoEncoder.stop();
                    videoEncoder.release();
                }
            } catch (Exception e) {
                Log.w(TAG_TR, Thread.currentThread().getName() + "|_error while releasing videoEncoder", e);
            }

            try {
                if (audioEncoder != null) {
                    audioEncoder.stop();
                    audioEncoder.release();
                }
            } catch (Exception e) {
                Log.w(TAG_TR, Thread.currentThread().getName() + "|_error while releasing audioEncoder", e);
            }

            try {
                if (inputSurface != null) {
                    inputSurface.release();
                }
            } catch (Exception e) {
                Log.w(TAG_TR, Thread.currentThread().getName() + "|_error while releasing inputSurface", e);
            }

            inputSurface = null;
            videoEncoder = null;
            audioEncoder = null;

        } catch (Exception e) {
            Log.w(TAG_TR, Thread.currentThread().getName() + "|_error while releasing muxer", e);
        }
    }

    public int getKeyIntervalPerSec() {
        return keyIntervalPerSec;
    }

    public void setKeyIntervalPerSec(int keyIntervalPerSec) {
        if (forceAllKeyFrame == true && keyIntervalPerSec != 0) {
            Log.w(TAG_TR, Thread.currentThread().getName() + "|_强制关键帧，设置非零的setKeyIntervalPerSec无效");
            return;
        }
        this.keyIntervalPerSec = keyIntervalPerSec;
    }

    public void setForceAllKeyFrame(boolean forceAllKeyFrame) {
        this.forceAllKeyFrame = forceAllKeyFrame;
        if (forceAllKeyFrame)
            keyIntervalPerSec = CodecUtils.getEnCodeKeyIFrameInterval();
    }

    private void awaitEncode() {
        synchronized (this) {
            while ((mCopyVideo && !videoEncoderDone) || (mCopyAudio && !audioEncoderDone)) {
                try {
                    Log.d(TAG_TR, Thread.currentThread().getName() + "======>去等待！mCopyVideo=" + mCopyVideo + " \t videoEncoderDone="
                            + videoEncoderDone + " \t mCopyAudio=" + mCopyAudio + " \t audioEncoderDone=" + audioEncoderDone);
                    wait();
                    Log.d(TAG_TR, Thread.currentThread().getName() + "======>唤醒了！mCopyVideo=" + mCopyVideo + " \t videoEncoderDone="
                            + videoEncoderDone + " \t mCopyAudio=" + mCopyAudio + " \t audioEncoderDone=" + audioEncoderDone);
                    if (videoEncoderDone && audioEncoderDone && transcodeModle.haseNext()) {
                        Log.d(TAG_TR, Thread.currentThread().getName() + "解码完毕，继续下一个解码！");
                        VERBOSE_TR = true;
                        videoEncoderDone = false;
                        audioEncoderDone = false;
                        transcodeModle.doNextTs();
                    }
                } catch (InterruptedException ie) {
                }
            }
        }
        Log.d(TAG_TR, "解码完毕");

    }

    @Override
    public void validateParamsCallBack(int height, int width, int bitRate, int fps,
                                       long durationUs) {
//        try {
//            this.durationUs = durationUs;
//            initEncode(height, width, bitRate, fps);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    /**
     * 音频回调
     */
    @Override
    public void encodeAudio() {
        if (pendingAudioEncoderInputBufferIndices.size() == 0 || pendingAudioDecoderOutputBufferIndices.size() == 0)
            return;
        int decoderIndex = pendingAudioDecoderOutputBufferIndices.poll();
        int encoderIndex = pendingAudioEncoderInputBufferIndices.poll();
        MediaCodec.BufferInfo info = pendingAudioDecoderOutputBufferInfos.poll();

        ByteBuffer encoderInputBuffer = null;
        try {
            encoderInputBuffer = audioEncoder.getInputBuffer(encoderIndex);
        } catch (Exception e) {
            audioEncoder.release();
            if (e != null && e.getMessage() != null)
                Log.e("TSTranscoder", "tryEncodeAudio:" + e.getMessage());
            MediaCodecInfo audioCodecInfo = CodecUtils.selectCodec(OUTPUT_AUDIO_MIME_TYPE);
            if (audioCodecInfo == null) {
                // Don't fail CTS if they don't have an AAC codec (not here, anyway).
                throw new TranscodeRunTimeException("Unable to find an appropriate codec for " + OUTPUT_AUDIO_MIME_TYPE);
            }
            try {
                audioEncoder = createAudioEncoder(audioCodecInfo, encoderInputAudioFormat);
            } catch (IOException e1) {
                throw new TranscodeRunTimeException("Create Audio Encoder Error:" + e1.getMessage());
            }
        }

        int size = info.size;
        long presentationTime = info.presentationTimeUs;
        if (VERBOSE_TR) {
            Log.d(TAG_TR, Thread.currentThread().getName() + "|_audio decoder: processing pending buffer: " + decoderIndex);
        }
        if (VERBOSE_TR) {
            Log.d(TAG_TR, Thread.currentThread().getName() + "|_audio decoder: pending buffer of size " + size + "\tpts:" + presentationTime);
        }
        if (size >= 0) {
            ByteBuffer decoderOutputBuffer = transcodeModle.getAudioDecoder().getOutputBuffer(decoderIndex).duplicate();
            decoderOutputBuffer.position(info.offset);
            decoderOutputBuffer.limit(info.offset + size);
            encoderInputBuffer.position(0);

            if (VERBOSE_TR)
                Log.d(TAG_TR, Thread.currentThread().getName() + "|_decoderOutputBuffer:" + decoderOutputBuffer);
            if (VERBOSE_TR)
                Log.i(TAG_TR, Thread.currentThread().getName() + "|_encoderInputBuffer:" + encoderInputBuffer);
            int srcChannelCount = transcodeModle.getDecoderOutputAudioFormat().getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            int destChannelCount = encoderInputAudioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

            if (srcChannelCount > 3 && destChannelCount == 2) {
                ShortBuffer srcPCM = decoderOutputBuffer.order(ByteOrder.nativeOrder()).asShortBuffer();
                srcPCM.rewind();
                short[] pcm = new short[srcPCM.remaining() / srcChannelCount * 2];
                for (int i = 0; i < pcm.length / 2; i++) {
                    pcm[i * 2] = srcPCM.get(i * srcChannelCount + 1);
                    pcm[i * 2 + 1] = srcPCM.get(i * srcChannelCount + 2);
                }
                encoderInputBuffer.clear();
                encoderInputBuffer.order(ByteOrder.nativeOrder()).asShortBuffer().put(pcm);
                encoderInputBuffer.rewind();
                encoderInputBuffer.limit(pcm.length * 2);
                size = pcm.length * 2;
            } else {
                encoderInputBuffer.put(decoderOutputBuffer);
            }
            audioEncoder.queueInputBuffer(encoderIndex, 0, size, presentationTime, info.flags);
        }
        transcodeModle.getAudioDecoder().releaseOutputBuffer(decoderIndex, false);
    }

    @Override
    public void onVideoBufferAvailable(long nsecs) {
        inputSurface.setPresentationTime(nsecs);
        if (VERBOSE_TR)
            Log.d(TAG_TR, Thread.currentThread().getName() + "|_input surface: swap buffers");
        inputSurface.swapBuffers();
        if (VERBOSE_TR)
            Log.d(TAG_TR, Thread.currentThread().getName() + "|_video encoder: notified of new frame");
        releaseEGLContext();
    }

    @Override
    public void releaseEGLContext() {
        if (inputSurface != null)
            inputSurface.releaseEGLContext();
    }

    @Override
    public void makeEGLContext(boolean doSetEncoder) {
        if (forceAllKeyFrame && doSetEncoder) {
            Bundle bundle = new Bundle();
            bundle.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
            videoEncoder.setParameters(bundle);
        }
        inputSurface.makeCurrent();
    }

    @Override
    public void onVideoDecoderDone() {
        Log.d(TAG_TR, "====>解码结束回调:transcodeModle.haseNext()=" + transcodeModle.haseNext());
        if (videoEncoder != null && !transcodeModle.haseNext())
            videoEncoder.signalEndOfInputStream();
        if (transcodeModle.haseNext()) {
            synchronized (this) {
                videoEncoderDone = true;
                notifyAll();
                Log.d(TAG_TR, Thread.currentThread().getName() + "======> muxVideo notifyAll");
            }
        }
    }

    @Override
    public void onThumbGenerated(Bitmap thumb, int index, long pts) {

    }

    @Override
    public void onProgress(float percent) {

    }

    @Override
    public void OnSuccessed(String outPutFilePath) {
        release();
    }

    @Override
    public void onError(String errmsg) {
        release();
    }
}

package com.zp.libvideoedit.Transcoder;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import com.zp.libvideoedit.Constants;
import com.zp.libvideoedit.R;
import com.zp.libvideoedit.utils.CodecUtils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.zp.libvideoedit.Constants.TAG;
import static com.zp.libvideoedit.Constants.TAG_TR;
import static com.zp.libvideoedit.Constants.US_MUTIPLE;
import static com.zp.libvideoedit.Constants.VERBOSE_EN;
import static com.zp.libvideoedit.Constants.VERBOSE_TR;
import static com.zp.libvideoedit.utils.FormatUtils.caller;

/**
 * Created by guoxian on 2018/4/26.<p/>
 * 视频转码。参数说明:
 * <ul>
 * <li>{@link #width width}{@link #height height}如果不制定，将使用原视频的宽高</li>
 * <li>{@link #fps fps}如果不指定，或者fps>60 或者fps<=0,如果原视频的fps在0-30之间，将使用原视频的fps,否则默认25</li>
 * <li>{@link #bitRate bitRate}如果不指定，或者fps>width * height * 20 或者fps<=1k,bitRate将使用默认值width * height * 8</li>
 * <li>{@link #outPutAudioSampleRate outPutAudioSampleRate}导出音频的采样率，与原视频相同</li>
 * <li>{@link #outPutAudioBitRate outPutAudioBitRate}</li>单声道为64k，多声道为28k
 * <li>{@link #outPutAudioChannelCount outPutAudioChannelCount}导出音频的音轨数量。如原视频单声道，导出也为单声道。如果2声道，保持不变。如果>3的立体声，将转为双声道</li>
 * </ul>
 * 调用方式参见:测试用例 TransCodeTest
 */

@TargetApi(21)
public class TSTranscoder {
    //业务参数
    //音视频格式参数
    private static final String OUTPUT_VIDEO_MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;
    // parameters for the audio encoder
    private static final String OUTPUT_AUDIO_MIME_TYPE = MediaFormat.MIMETYPE_AUDIO_AAC; //"audio/mp4a-latm"; // Advanced Audio Coding
    private static final int OUTPUT_AUDIO_AAC_PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectHE;


    Context context;
    /**
     * 导出视频绝对路径。文件格式为mp4;保证app已经有sd卡读写权限;如果文件夹不存在将创建文件夹。
     */
    private String outPutFilePath;
    /**
     * 输入文件或者资源.assert:// 为前缀，会从assert中读取，否则认为是绝对路径
     */
//    private String inPutFilePath;
    private List<String> inputFiles;
    //剪裁,剪裁只支持单片段
    private long trimBeginUs=-1;
    private long trimEndUs=-1;

    /**
     * 如果不制定，将使用原视频的宽高
     */
    private int height = -1;
    private int width = -1;

    private int rotation = 0;
    /**
     * /**
     * 如果转为全关键帧视频，码率要大一些 fps * width * height*1.5
     */
    private int bitRate;
    /**
     * 如果不指定fps,并且原视频的fps<60,将使用原视频的fpgs
     */
    private float fps = -1;

//    private boolean cannotGetFps = false;
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
    //接口实现的变量
    private int lastProgress = -1;
    private long durationUs;
    private int thumbnIndex;
    private long thumbIntervalMs;
    //一些常量参数
    private boolean mCopyVideo = true;
    private boolean mCopyAudio = true;
    // 线程控制
    private Thread transCodeThread;
    private boolean doing = false;
    private boolean stoping = false;
    private HandlerThread videoDecoderHandlerThread;
    private CallbackHandler videoDecoderHandler;
    //编解码中相关中间临时变量
    private MediaFormat decoderInputVideoFormat = null;
    private MediaFormat decoderInputAudioFormat = null;
    private MediaFormat decoderOutputVideoFormat = null;
    private MediaFormat decoderOutputAudioFormat = null;
    private MediaFormat encoderOutputVideoFormat = null;
    private MediaFormat encoderInputAudioFormat = null;
    private MediaFormat encoderOutputAudioFormat = null;
    private int outputVideoTrack = -1;
    private int outputAudioTrack = -1;
    private boolean videoExtractorDone = false;
    private boolean videoDecoderDone = false;
    private boolean videoEncoderDone = false;
    private boolean audioExtractorDone = false;
    private boolean audioDecoderDone = false;
    private boolean audioEncoderDone = false;
    private LinkedList<Integer> pendingAudioDecoderOutputBufferIndices;
    private LinkedList<MediaCodec.BufferInfo> pendingAudioDecoderOutputBufferInfos;
    private LinkedList<Integer> pendingAudioEncoderInputBufferIndices;
    private LinkedList<Integer> pendingVideoEncoderOutputBufferIndices;
    private LinkedList<MediaCodec.BufferInfo> pendingVideoEncoderOutputBufferInfos;
    private LinkedList<Integer> pendingAudioEncoderOutputBufferIndices;
    private LinkedList<MediaCodec.BufferInfo> pendingAudioEncoderOutputBufferInfos;
    private boolean isMuxing = false;
    private int videoExtractedFrameCount = 0;
    private int videoDecodedFrameCount = 0;
    private int videoEncodedFrameCount = 0;
    private int audioExtractedFrameCount = 0;
    private int audioDecodedFrameCount = 0;
    private int audioEncodedFrameCount = 0;
    //编解码器
//    private MediaExtractor tsVideoExtractor = null;
    private TSMediaExtractor tsVideoExtractor =null;
//    private MediaExtractor audioExtractor = null;
    private TSMediaExtractor tsAudioExtractor=null;
    private TranscodeInputSurface inputSurface = null;
    private TranscodeOutputSurface outputSurface = null;
    private MediaCodec videoDecoder = null;
    private MediaCodec audioDecoder = null;
    private MediaCodec videoEncoder = null;
    private MediaCodec audioEncoder = null;
    private MediaMuxer muxer = null;
    private long lastTimestampUsForAudio;//记录音频上一次的pts


    public TSTranscoder(Context context) {
        this.context = context;
    }

    private void validateParams() throws TranscodeRunTimeException {
//
        if(trimBeginUs>=0 && trimEndUs>=0 && inputFiles.size()>1){
            throw new TranscodeRunTimeException("剪裁只能应用于单片段");
        }
        if(trimBeginUs>=0 && trimEndUs>=trimBeginUs && trimEndUs-trimBeginUs<1.8*US_MUTIPLE){
            throw new TranscodeRunTimeException("剪裁必须大小大于2秒");
        }

       List<TSMediaExtractor.TSSegemnt> tsSegemntList=TSMediaExtractor.generateTsSegemnt(context, inputFiles);


        if (outPutFilePath == null || outPutFilePath.length() == 0)
            throw new TranscodeRunTimeException("输出文件为空");
        File outputFile = new File(outPutFilePath);
        String suffix = outPutFilePath.substring(outPutFilePath.lastIndexOf(".") + 1);
        if (!suffix.equalsIgnoreCase("mp4")) {
            throw new TranscodeRunTimeException("输出文件扩展名仅能为mp4");
        }
        if (outputFile.exists())
            Log.w(TAG_TR, Thread.currentThread().getName() + "|_file will be overwite");
        File parent = outputFile.getParentFile();
        if (!parent.exists()) parent.mkdirs();

        if (mCopyVideo) {

                tsVideoExtractor = new TSMediaExtractor(context,tsSegemntList, TSMediaExtractor.MediaTrackType.video,trimBeginUs,trimEndUs);

            decoderInputVideoFormat = tsVideoExtractor.getTrackFormat();
            if (VERBOSE_EN)
                Log.i(TAG_TR, Thread.currentThread().getName() + "Transcode_FORMART_decoderInputVideoFormat:" + decoderInputVideoFormat);
            if (decoderInputVideoFormat.containsKey(MediaFormat.KEY_DURATION))
                durationUs = decoderInputVideoFormat.getLong(MediaFormat.KEY_DURATION);
            durationUs=tsVideoExtractor.getDurationUs();



            if (decoderInputVideoFormat.containsKey(MediaFormat.KEY_ROTATION))
                rotation = decoderInputVideoFormat.getInteger(MediaFormat.KEY_ROTATION);
            if (VERBOSE_EN)
                Log.d(TAG_TR, Thread.currentThread().getName() + "|_rotation:" + rotation);

            if (width <= 0 || height <= 0) {
                width = decoderInputVideoFormat.getInteger(MediaFormat.KEY_WIDTH);
                height = decoderInputVideoFormat.getInteger(MediaFormat.KEY_HEIGHT);
                if (decoderInputVideoFormat.containsKey("crop-left") && decoderInputVideoFormat.containsKey("crop-right")) {
                    width = decoderInputVideoFormat.getInteger("crop-right") + 1 - decoderInputVideoFormat.getInteger("crop-left");
                }
                if (decoderInputVideoFormat.containsKey("crop-top") && decoderInputVideoFormat.containsKey("crop-bottom")) {
                    height = decoderInputVideoFormat.getInteger("crop-bottom") + 1 - decoderInputVideoFormat.getInteger("crop-top");
                }
                if (rotation == 90 || rotation == 270) {
                    int h = width;
                    width = height;
                    height = h;
                }

                int capability = CodecUtils.getCodecCapability();
                Size size = CodecUtils.reduceSize(width, height, capability);
                width = size.getWidth();
                height = size.getHeight();

                if (VERBOSE_TR)
                    Log.i(TAG_TR, Thread.currentThread().getName() + "|Transcode_FORMART width or height.use input vedio formart:" + width + "x" + height);
            }

            float inputFps = tsVideoExtractor.getFps();
            if (fps > 70 || fps <= 0)
                if (inputFps <= 70 && inputFps > 0) {
                    fps = inputFps;
                    Log.i(TAG_TR, Thread.currentThread().getName() + "|_invalid fps,use input video fps:" + fps);
                } else {
                    Log.i(TAG_TR, Thread.currentThread().getName() + "|_invalid fps,use default video fps:" + fps);
                    fps = 25;
                }
            // 转为全关键帧视频，所有码率要大一些
            bitRate = CodecUtils.calcBitRate(forceAllKeyFrame,width, height, fps);
            if (bitRate > 60 * 1024 * 1024) bitRate = 60 * 1024 * 1024;
            if (VERBOSE_TR)
                Log.i(TAG_TR, Thread.currentThread().getName() + "|_invalid bitRate,use " + bitRate + "kbps");

            int totalFrame = (int) (inputFps * durationUs / Constants.US_MUTIPLE);
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
                Log.d(TAG_TR, String.format("transoder file %s to %s", inputFiles, outPutFilePath));
            }
        }
        if (mCopyAudio) {
            tsAudioExtractor=new TSMediaExtractor(context,tsSegemntList, TSMediaExtractor.MediaTrackType.audio,trimBeginUs,trimEndUs);
            decoderInputAudioFormat=tsAudioExtractor.getTrackFormat();
        }

    }




    public void transCode() {
        if (doing)
            throw new TranscodeRunTimeException("cant' trancode while transcoder is doing now...");
        if (VERBOSE_TR)
            Log.i(TAG_TR, caller() + "transCode...." + inputFiles + ", ToutPath:" + outPutFilePath);
        validateParams();


        final TSTranscoder self = this;
        long startTime = System.currentTimeMillis();
        try {
            doTranscode();

            if (callback != null) {
                if (lastProgress > 98) {
                    callback.OnSuccessed(outPutFilePath);
                } else {//转码不完整
                    callback.onError(context.getString(R.string.video_transcode_imperfect));
                }
            }
        } catch (Exception e) {
            Log.e(TAG_TR, Thread.currentThread().getName() + "|_", e);
            if (callback != null)
                callback.onError(context.getString(R.string.video_transcode_error));
        } finally {
            long elapseTime = System.currentTimeMillis() - startTime;
            if (VERBOSE_TR)
                Log.d(TAG_TR, Thread.currentThread().getName() + "|_transcode elapse_time:" + elapseTime);
        }

    }

    private void doTranscode() throws Exception {
        doing = true;
        try {
            decoderOutputVideoFormat = null;
            decoderOutputAudioFormat = null;
            encoderOutputVideoFormat = null;
            encoderOutputAudioFormat = null;

            outputVideoTrack = -1;
            outputAudioTrack = -1;
            videoExtractorDone = false;
            videoDecoderDone = false;
            videoEncoderDone = false;
            audioExtractorDone = false;
            audioDecoderDone = false;
            audioEncoderDone = false;
            pendingAudioDecoderOutputBufferIndices = new LinkedList<Integer>();
            pendingAudioDecoderOutputBufferInfos = new LinkedList<MediaCodec.BufferInfo>();
            pendingAudioEncoderInputBufferIndices = new LinkedList<Integer>();
            pendingVideoEncoderOutputBufferIndices = new LinkedList<Integer>();
            pendingVideoEncoderOutputBufferInfos = new LinkedList<MediaCodec.BufferInfo>();
            pendingAudioEncoderOutputBufferIndices = new LinkedList<Integer>();
            pendingAudioEncoderOutputBufferInfos = new LinkedList<MediaCodec.BufferInfo>();
            isMuxing = false;
            videoExtractedFrameCount = 0;
            videoDecodedFrameCount = 0;
            videoEncodedFrameCount = 0;
            audioExtractedFrameCount = 0;
            audioDecodedFrameCount = 0;
            audioEncodedFrameCount = 0;
            lastTimestampUsForAudio = 0;

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
            muxer = createMuxer();

            if (mCopyVideo) {

                MediaFormat outputVideoFormat = MediaFormat.createVideoFormat(OUTPUT_VIDEO_MIME_TYPE, width, height);

                outputVideoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
                outputVideoFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
                outputVideoFormat.setFloat(MediaFormat.KEY_FRAME_RATE, fps);

                outputVideoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, keyIntervalPerSec);
                if (VERBOSE_TR)
                    Log.i(TAG_TR, Thread.currentThread().getName() + "|Transcode_FORMART1:outputVideoFormat: " + outputVideoFormat);

                AtomicReference<Surface> inputSurfaceReference = new AtomicReference<Surface>();
                videoEncoder = createVideoEncoder(videoCodecInfo, outputVideoFormat, inputSurfaceReference);
                inputSurface = new TranscodeInputSurface(inputSurfaceReference.get());
                inputSurface.makeCurrent();
                // Create a MediaCodec for the decoder, based on the extractor's format.
                outputSurface = new TranscodeOutputSurface(width, height);

                videoDecoder = createVideoDecoder(decoderInputVideoFormat, outputSurface.getSurface());
                inputSurface.releaseEGLContext();

            }

            if (mCopyAudio) {

                int channelCount = decoderInputAudioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                int sampleRate = decoderInputAudioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                MediaFormat srcAudioFormat = CodecUtils.detectAudioFormat(context, inputFiles.get(0));
//                 格式化音频

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
                if (VERBOSE_TR)
                    Log.d(TAG_TR, Thread.currentThread().getName() + "|_AFORMAT:inputAudioFormat: " + decoderInputAudioFormat);

                audioEncoder = createAudioEncoder(audioCodecInfo, encoderInputAudioFormat);
                audioDecoder = createAudioDecoder(decoderInputAudioFormat);
            }
            if (VERBOSE_TR)
                Log.d(TAG_TR, caller() + "DEAD_LOCK_awaitEncode");

            awaitEncode();


        } finally {
            doing = false;
            if (VERBOSE_TR)
                Log.d(TAG_TR, Thread.currentThread().getName() + "|_releasing extractor, decoder, encoder, and muxer");
            try {
                if (tsVideoExtractor != null) {
                    tsVideoExtractor.release();
                }
            } catch (Exception e) {
                Log.w(TAG_TR, Thread.currentThread().getName() + "|_error while releasing tsVideoExtractor", e);
            }
            try {
                if (tsAudioExtractor != null) {
                    tsAudioExtractor.release();
                }
            } catch (Exception e) {
                Log.w(TAG_TR, Thread.currentThread().getName() + "|_error while releasing tsAudioExtractor", e);
            }
            try {
                if (videoDecoder != null) {
                    videoDecoder.stop();
                    videoDecoder.release();
                }
            } catch (Exception e) {
                Log.w(TAG_TR, Thread.currentThread().getName() + "|_error while releasing videoDecoder", e);
            }
            try {
                if (outputSurface != null) {
                    outputSurface.release();
                }
            } catch (Exception e) {
                Log.w(TAG_TR, Thread.currentThread().getName() + "|_error while releasing outputSurface", e);
            }
            try {
                if (videoEncoder != null) {
                    videoEncoder.stop();
                    videoEncoder.release();
                }
            } catch (Exception e) {
                Log.w(TAG_TR, Thread.currentThread().getName() + "|_error while releasing videoEncoder", e);
            }
            try {
                if (audioDecoder != null) {
                    audioDecoder.stop();
                    audioDecoder.release();
                }
            } catch (Exception e) {
                Log.w(TAG_TR, Thread.currentThread().getName() + "|_error while releasing audioDecoder", e);
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
                if (muxer != null) {
                    if (isMuxing) muxer.stop();
                    muxer.release();
                }
            } catch (Exception e) {
                Log.w(TAG_TR, Thread.currentThread().getName() + "|_error while releasing muxer", e);
            }
            try {
                if (inputSurface != null) {
                    inputSurface.release();
                }
            } catch (Exception e) {
                Log.w(TAG_TR, Thread.currentThread().getName() + "|_error while releasing inputSurface", e);
            }
            if (videoDecoderHandlerThread != null) {
                videoDecoderHandlerThread.quitSafely();
            }
            tsVideoExtractor = null;
            tsAudioExtractor = null;
            outputSurface = null;
            inputSurface = null;
            videoDecoder = null;
            audioDecoder = null;
            videoEncoder = null;
            audioEncoder = null;
            muxer = null;
            videoDecoderHandlerThread = null;
        }

    }

    private void logState() {
        if (VERBOSE_TR) {
            Log.d(TAG_TR, String.format("%s|_loop: " + "V(%b){" + "extracted:%d(done:%b) " + "decoded:%d(done:%b) " + "encoded:%d(done:%b)} " + "A(%b){" + "extracted:%d(done:%b) " + "decoded:%d(done:%b) " + "encoded:%d(done:%b) " + "muxing:%b(V:%d,A:%d)", Thread.currentThread().getName(), mCopyVideo, videoExtractedFrameCount, videoExtractorDone, videoDecodedFrameCount, videoDecoderDone, videoEncodedFrameCount, videoEncoderDone, mCopyAudio, audioExtractedFrameCount, audioExtractorDone, audioDecodedFrameCount, audioDecoderDone, audioEncodedFrameCount, audioEncoderDone, isMuxing, outputVideoTrack, outputAudioTrack));
        }
    }

    private void awaitEncode() {
        synchronized (this) {
            while ((mCopyVideo && !videoEncoderDone) || (mCopyAudio && !audioEncoderDone)) {
                try {
                    wait();
                } catch (InterruptedException ie) {
                }
            }
        }

        // Basic sanity checks.
        if (mCopyVideo) {
            boolean allFrameEncoded = videoDecodedFrameCount == videoEncodedFrameCount;
            if (!allFrameEncoded) {
                if (VERBOSE_TR)
                    Log.w(TAG_TR, Thread.currentThread().getName() + "|_encoded and decoded video frame counts not match:encoded_frame:" + videoDecodedFrameCount + "decoded Count" + videoEncodedFrameCount);
            } else {
                if (VERBOSE_TR)
                    Log.d(TAG_TR, Thread.currentThread().getName() + "|_transcode finished:encoded_frame:" + videoDecodedFrameCount + "decoded Count" + videoEncodedFrameCount);
            }
            boolean allDecoded = videoDecodedFrameCount > videoExtractedFrameCount;
            if (allDecoded) {
                Log.w(TAG_TR, Thread.currentThread().getName() + "|_decoded frame count should be less than extracted frame count:encoded_frame:" + videoDecodedFrameCount + "extracted Count" + videoExtractedFrameCount);
            }
        }
        if (mCopyAudio) {
            if (pendingAudioDecoderOutputBufferIndices.size() != 0) {
                Log.w(TAG_TR, Thread.currentThread().getName() + "|_has audio frame was pending. size" + pendingAudioDecoderOutputBufferIndices.size());
            }
        }

    }


    private MediaCodec createVideoDecoder(MediaFormat inputFormat, Surface surface) throws IOException {
        videoDecoderHandlerThread = new HandlerThread("TR_D");
        videoDecoderHandlerThread.start();
        videoDecoderHandler = new CallbackHandler(videoDecoderHandlerThread.getLooper());
        final TSTranscoder self = this;
        final MediaCodec.Callback videoDecoderCallback = new MediaCodec.Callback() {
            public void onError(MediaCodec codec, MediaCodec.CodecException exception) {
            }

            public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
                decoderOutputVideoFormat = codec.getOutputFormat();
                if (VERBOSE_TR) {
                    Log.d(TAG_TR, Thread.currentThread().getName() + "|Transcode_FORMART_changeed:video decoder: output format changed: " + decoderOutputVideoFormat);
                }
            }

            public void onInputBufferAvailable(MediaCodec codec, int index) {
                ByteBuffer decoderInputBuffer = codec.getInputBuffer(index);
                while (!videoExtractorDone) {
                    int size = tsVideoExtractor.readSampleData(decoderInputBuffer, 0);
                    long presentationTime = tsVideoExtractor.getSampleTime();

                    if (size >= 0) {
                        codec.queueInputBuffer(index, 0, size, presentationTime, tsVideoExtractor.getSampleFlags());
                    }
                    if (VERBOSE_TR) {
                        Log.d(TAG_TR, Thread.currentThread().getName() + "|_video --extractor--: returned buffer of size " + size + "\t" + presentationTime+"\t"+tsVideoExtractor.getSampleFlags());
                    }
                    videoExtractorDone = !tsVideoExtractor.advance();
                    if (videoExtractorDone) {
                        if (VERBOSE_TR)
                            Log.d(TAG_TR, Thread.currentThread().getName() + "|_video --extractor--: EOS");
                        codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    }
                    videoExtractedFrameCount++;
                    logState();
                    if (size >= 0) break;
                }
            }

            public void onOutputBufferAvailable(MediaCodec codec, int index, MediaCodec.BufferInfo info) {
                if (VERBOSE_TR) {
                    Log.d(TAG_TR, Thread.currentThread().getName() + "|_video decoder: returned output buffer: " + index + "\t" + info.size);
                }
                if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    if (VERBOSE_TR)
                        Log.d(TAG_TR, Thread.currentThread().getName() + "|_video decoder: codec onAudioFormatChanged buffer");
                    codec.releaseOutputBuffer(index, false);
                    return;
                }
                if (VERBOSE_TR) {
                    Log.d(TAG_TR, Thread.currentThread().getName() + "|_video decoder: returned buffer for time " + info.presentationTimeUs);
                }
                boolean render = info.size != 0;
                codec.releaseOutputBuffer(index, render);
                if (render ) {
                    if (forceAllKeyFrame) {
                        Bundle bundle = new Bundle();
                        bundle.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
                        videoEncoder.setParameters(bundle);
                    }
                    inputSurface.makeCurrent();
                    if (VERBOSE_TR)
                        Log.d(TAG_TR, caller() + "|DEAD_LOCK_output surface: await new image");
                    outputSurface.awaitNewImage();
                    // Edit the frame and send it to the encoder.
                    if (VERBOSE_TR)
                        Log.d(TAG_TR, Thread.currentThread().getName() + "|_output surface: draw image");
                    outputSurface.drawImage();

                    if (countOfThumb > 0 && thumbnIndex < countOfThumb && info.presentationTimeUs > thumbIntervalMs * thumbnIndex) {
                        if (VERBOSE_TR)
                            Log.d(TAG_TR, Thread.currentThread().getName() + "|_output surface: copy bitmap\t" + thumbnIndex + "\t" + info.presentationTimeUs);
                        Bitmap thumbBmp = outputSurface.copyBitmap();
                        if (callback != null)
                            callback.onThumbGenerated(thumbBmp, thumbnIndex, info.presentationTimeUs);
                        thumbnIndex++;

                    }

                    inputSurface.setPresentationTime(info.presentationTimeUs * 1000);
                    if (VERBOSE_TR)
                        Log.d(TAG_TR, Thread.currentThread().getName() + "|_input surface: swap buffers");
                    inputSurface.swapBuffers();
                    if (VERBOSE_TR)
                        Log.d(TAG_TR, Thread.currentThread().getName() + "|_video encoder: notified of new frame");
                    inputSurface.releaseEGLContext();

                }

                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (VERBOSE_TR)
                        Log.d(TAG_TR, Thread.currentThread().getName() + "|_video decoder: EOS");
                    videoDecoderDone = true;
                    videoEncoder.signalEndOfInputStream();
                }

                videoDecodedFrameCount++;
                logState();
            }
        };

        videoDecoderHandler.create(false, CodecUtils.getMimeTypeFor(inputFormat), videoDecoderCallback);
        MediaCodec decoder = videoDecoderHandler.getCodec();
        decoder.configure(inputFormat, surface, null, 0);
        decoder.start();
        return decoder;
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
     * Creates a decoder for the given format.
     *
     * @param inputFormat the format of the stream to decode
     */
    private MediaCodec createAudioDecoder(MediaFormat inputFormat) throws IOException {
        MediaCodec decoder = MediaCodec.createDecoderByType(CodecUtils.getMimeTypeFor(inputFormat));
        decoder.setCallback(new MediaCodec.Callback() {
            public void onError(MediaCodec codec, MediaCodec.CodecException exception) {
            }

            public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
                decoderOutputAudioFormat = codec.getOutputFormat();
                if (VERBOSE_TR) {
                    Log.i(TAG_TR, Thread.currentThread().getName() + "|_AFORMAT:decoderOutputAudioFormat audio decoder: output format changed: " + decoderOutputAudioFormat);
                }
            }

            public void onInputBufferAvailable(MediaCodec codec, int index) {
                ByteBuffer decoderInputBuffer = codec.getInputBuffer(index);
                while (!audioExtractorDone) {
                    int size = tsAudioExtractor.readSampleData(decoderInputBuffer, 0);
                    long presentationTime = tsAudioExtractor.getSampleTime();
                    if (VERBOSE_TR) {
                        Log.d(TAG_TR, Thread.currentThread().getName() + "|_audio extractor: returned buffer of size " + size + "\tpts:" + presentationTime);
                    }
                    if (size >= 0) {
                        codec.queueInputBuffer(index, 0, size, presentationTime, tsAudioExtractor.getSampleFlags());
                    }
                    audioExtractorDone = !tsAudioExtractor.advance();
                    if (audioExtractorDone) {
                        if (VERBOSE_TR)
                            Log.d(TAG_TR, Thread.currentThread().getName() + "|_audio extractor: EOS");
                        codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    }
                    audioExtractedFrameCount++;
                    logState();
                    if (size >= 0) break;
                }
            }

            public void onOutputBufferAvailable(MediaCodec codec, int index, MediaCodec.BufferInfo info) {
                if (VERBOSE_TR) {
                    Log.d(TAG_TR, Thread.currentThread().getName() + "|_audio decoder: returned output buffer: " + index + "\tsize:" + info.size);
                }
                ByteBuffer decoderOutputBuffer = codec.getOutputBuffer(index);
                if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    if (VERBOSE_TR)
                        Log.d(TAG_TR, Thread.currentThread().getName() + "|_audio decoder: codec onAudioFormatChanged buffer");
                    codec.releaseOutputBuffer(index, false);
                    return;
                }
                if (VERBOSE_TR) {
                    Log.d(TAG_TR, Thread.currentThread().getName() + "|_audio decoder: returned buffer for time " + info.presentationTimeUs);
                }
                pendingAudioDecoderOutputBufferIndices.add(index);
                pendingAudioDecoderOutputBufferInfos.add(info);
                audioDecodedFrameCount++;
                logState();
                tryEncodeAudio();
            }
        });
        decoder.configure(inputFormat, null, null, 0);
        decoder.start();
        return decoder;
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
                tryEncodeAudio();
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

    // No need to have synchronization around this, since both audio encoder and
    // decoder callbacks are on the same thread.
    private void tryEncodeAudio() {
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
            ByteBuffer decoderOutputBuffer = audioDecoder.getOutputBuffer(decoderIndex).duplicate();
            decoderOutputBuffer.position(info.offset);
            decoderOutputBuffer.limit(info.offset + size);
            encoderInputBuffer.position(0);

            if (VERBOSE_TR)
                Log.d(TAG_TR, Thread.currentThread().getName() + "|_decoderOutputBuffer:" + decoderOutputBuffer);
            if (VERBOSE_TR)
                Log.i(TAG_TR, Thread.currentThread().getName() + "|_encoderInputBuffer:" + encoderInputBuffer);
            int srcChannelCount = decoderOutputAudioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
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
        audioDecoder.releaseOutputBuffer(decoderIndex, false);
        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            if (VERBOSE_TR)
                Log.d(TAG_TR, Thread.currentThread().getName() + "|_audio decoder: EOS");
            audioDecoderDone = true;
        }
        logState();
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
            }
            percent = 100;
        }
        if (percent > lastProgress) {
            if (callback != null) callback.onProgress((float) percent / 100.0f);
            if (VERBOSE_TR)
                Log.d(TAG_TR, Thread.currentThread().getName() + "trancode_percent:" + percent);
            lastProgress = percent;
        }
        logState();
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
        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            if (VERBOSE_TR)
                Log.d(TAG_TR, Thread.currentThread().getName() + "|_audio encoder: EOS");
            synchronized (this) {
                audioEncoderDone = true;
                notifyAll();
            }
        }
        logState();
    }

    /**
     * Creates a muxer to write the encoded frames.
     * <p>
     * <p>The muxer is not started as it needs to be started only after all streams have been added.
     */
    private MediaMuxer createMuxer() throws IOException {
        return new MediaMuxer(outPutFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
    }


    public String getOutPutFilePath() {
        return outPutFilePath;
    }

    public void setOutPutFilePath(String outPutFilePath) {
        File file = new File(outPutFilePath);
        String parentPath=file.getParent();
        File parent=new File(parentPath);
        if( !parent.exists()){
            parent.mkdirs();
        }else if(!parent.isDirectory()){
            throw new TranscodeRunTimeException("output dir exist as file");
        }
        this.outPutFilePath = outPutFilePath;
    }

//    public String getInPutFilePath() {
//        return inPutFilePath;
//    }
//
//    public void setInPutFilePath(String inPutFilePath) {
//        this.inPutFilePath = inPutFilePath;
//    }


    public void setInputFiles(List<String> inputFiles) {
        this.inputFiles = inputFiles;
    }

    public List<String> getInputFiles() {
        return inputFiles;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getBitRate() {
        return bitRate;
    }

    public void setBitRate(int bitRate) {
        this.bitRate = bitRate;
    }

    public float getFps() {
        return fps;
    }

    public void setFps(int fps) {
        this.fps = fps;
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

    public boolean isForceAllKeyFrame() {
        return forceAllKeyFrame;
    }

    public void setForceAllKeyFrame(boolean forceAllKeyFrame) {
        this.forceAllKeyFrame = forceAllKeyFrame;
//        if (Build.VERSION.SDK_INT >= 23) {
//            if (forceAllKeyFrame) keyIntervalPerSec = 1;
//        } else {
//            if (forceAllKeyFrame) keyIntervalPerSec = 0;
//        }
        if (forceAllKeyFrame)
            keyIntervalPerSec = CodecUtils.getEnCodeKeyIFrameInterval();//0
        else  keyIntervalPerSec = 1;//1
    }

    public Callback getCallback() {
        return callback;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public int getCountOfThumb() {
        return countOfThumb;
    }

    public void setCountOfThumb(int countOfThumb) {
        this.countOfThumb = countOfThumb;
    }

    public interface Callback {
        /**
         * 生成缩略图回调
         *
         * @param thumb Bitmap 需要listener手动recycle
         * @param index
         * @param pts
         */
        public void onThumbGenerated(Bitmap thumb, int index, long pts);

        /**
         * 转码进度回调
         *
         * @param percent 导出完成百分比
         */
        public void onProgress(float percent);

        /**
         * 转码完成功成回调
         */
        public void OnSuccessed(String outPutFilePath);

        /**
         * 转码失败回调
         *
         * @param errmsg
         */
        public void onError(String errmsg);

    }

    static class CallbackHandler extends Handler {
        private MediaCodec mCodec;
        private boolean mEncoder;
        private MediaCodec.Callback mCallback;
        private String mMime;
        private boolean mSetDone;

        CallbackHandler(Looper l) {
            super(l);
        }

        @Override
        public void handleMessage(Message msg) {
            try {
                mCodec = mEncoder ? MediaCodec.createEncoderByType(mMime) : MediaCodec.createDecoderByType(mMime);
            } catch (IOException ioe) {
            }
            mCodec.setCallback(mCallback);
            synchronized (this) {
                mSetDone = true;
                notifyAll();
            }
        }

        void create(boolean encoder, String mime, MediaCodec.Callback callback) {
            mEncoder = encoder;
            mMime = mime;
            mCallback = callback;
            mSetDone = false;
            sendEmptyMessage(0);
            synchronized (this) {
                while (!mSetDone) {
                    try {
                        wait();
                    } catch (InterruptedException ie) {
                    }
                }
            }
        }

        MediaCodec getCodec() {
            return mCodec;
        }
    }

    public long getTrimBeginUs() {
        return trimBeginUs;
    }

    public void setTrimBeginUs(long trimBeginUs) {
        this.trimBeginUs = trimBeginUs;
    }

    public long getTrimEndUs() {
        return trimEndUs;
    }

    public void setTrimEndUs(long trimEndUs) {
        this.trimEndUs = trimEndUs;
    }
}


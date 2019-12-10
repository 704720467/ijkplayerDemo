package com.zp.libvideoedit.Transcoder;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import com.zp.libvideoedit.EditConstants;
import com.zp.libvideoedit.R;
import com.zp.libvideoedit.utils.CodecUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;

import static com.zp.libvideoedit.EditConstants.TAG_TR;
import static com.zp.libvideoedit.EditConstants.VERBOSE_EN;
import static com.zp.libvideoedit.EditConstants.VERBOSE_TR;
import static com.zp.libvideoedit.utils.CodecUtils.createExtractor;
import static com.zp.libvideoedit.utils.CodecUtils.getAndSelectVideoTrackIndex;
import static com.zp.libvideoedit.utils.FormatUtils.caller;

/**
 * 解码 文件
 * Create by zp on 2019-11-18
 */
public class TranscoderNew {


    Context context;
    private String inPutFilePath;

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
    private int fps = -1;

//    private boolean cannotGetFps = false;

    /**
     * 某些手机(华为系列)需要设置此参数，建议设置此参数为true
     */
    private boolean forceAllKeyFrame = true;
    /**
     * 回调接口
     */
    private TranscodeListener callback;
    /**
     * 异步生成图片的数量。默认为0，不生成。否则将根据视频长度，均匀的生成.回调接口生成的图片必须手动释放s
     */
    private int countOfThumb = 0;

    //接口实现的变量
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


    private boolean videoExtractorDone = false;
    private boolean videoDecoderDone = false;
    private boolean audioExtractorDone = false;
    private boolean audioDecoderDone = false;

    private int videoExtractedFrameCount = 0;
    private int videoDecodedFrameCount = 0;

    private int audioExtractedFrameCount = 0;
    private int audioDecodedFrameCount = 0;
    //编解码器
    private MediaExtractor videoExtractor = null;
    private MediaExtractor audioExtractor = null;

    private TranscodeOutputSurface outputSurface = null;
    private MediaCodec videoDecoder = null;
    private MediaCodec audioDecoder = null;

    private LinkedList<Integer> pendingAudioDecoderOutputBufferIndices;
    private LinkedList<MediaCodec.BufferInfo> pendingAudioDecoderOutputBufferInfos;

    public TranscoderNew(Context context) {
        this.context = context;
    }

    public TranscoderNew(Context context, String inPutFilePath,
                         LinkedList<Integer> pendingAudioDecoderOutputBufferIndices,
                         LinkedList<MediaCodec.BufferInfo> pendingAudioDecoderOutputBufferInfos) {
        this.context = context;
        this.inPutFilePath = inPutFilePath;
        this.pendingAudioDecoderOutputBufferIndices = pendingAudioDecoderOutputBufferIndices;
        this.pendingAudioDecoderOutputBufferInfos = pendingAudioDecoderOutputBufferInfos;
    }


    public void validateParams() throws TranscodeRunTimeException {
//
        try {
            CodecUtils.checkMediaExist(context, inPutFilePath);
        } catch (Exception e) {
            throw new TranscodeRunTimeException(e.getMessage());
        }

        if (mCopyVideo) {
            try {
                videoExtractor = createExtractor(context, inPutFilePath);
            } catch (IOException e) {
                throw new TranscodeRunTimeException("invalidate video of input file.", e);
            }
            int videoInputTrack = getAndSelectVideoTrackIndex(videoExtractor);
            if (videoInputTrack == -1) {
                throw new TranscodeRunTimeException("missing video track in test video, file:" + inPutFilePath);
            }
            decoderInputVideoFormat = videoExtractor.getTrackFormat(videoInputTrack);
            if (VERBOSE_EN)
                Log.i(TAG_TR, Thread.currentThread().getName() + "Transcode_FORMART_decoderInputVideoFormat:" + decoderInputVideoFormat);
            if (decoderInputVideoFormat.containsKey(MediaFormat.KEY_DURATION))
                durationUs = decoderInputVideoFormat.getLong(MediaFormat.KEY_DURATION);
            else {
                durationUs = CodecUtils.getDurationMS(context, inPutFilePath) * 1000;
            }

            int inputFps = 0;
            if (decoderInputVideoFormat.containsKey(MediaFormat.KEY_FRAME_RATE))
                inputFps = decoderInputVideoFormat.getInteger(MediaFormat.KEY_FRAME_RATE);
            else inputFps = Math.round(CodecUtils.detectFps(inPutFilePath));

            if (inputFps <= 0)
                inputFps = EditConstants.DEFAULT_FPS;


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

                if (VERBOSE_EN)
                    Log.i(TAG_TR, Thread.currentThread().getName() + "|Transcode_FORMART width or height.use input vedio formart:" + width + "x" + height);
            }


            if (fps > 70 || fps <= 0)
                if (inputFps <= 70 && fps > 0) {
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

            int totalFrame = (int) (inputFps * durationUs / EditConstants.US_MUTIPLE);
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
                Log.d(TAG_TR, String.format("transoder file %s", inPutFilePath));
            }
        }
        if (mCopyAudio) {
            try {
                audioExtractor = createExtractor(context, inPutFilePath);
            } catch (IOException e) {
                throw new TranscodeRunTimeException("invalidate audio of input file.", e);
            }
            int audioInputTrack = CodecUtils.getAndSelectAudioTrackIndex(audioExtractor);
            if (audioInputTrack == -1) {
                Log.w(TAG_TR, Thread.currentThread().getName() + "|_missing audio track in  video,ignore audio");
                mCopyAudio = false;
            } else {
                decoderInputAudioFormat = audioExtractor.getTrackFormat(audioInputTrack);
            }
        }
        if (callback != null)
            callback.validateParamsCallBack(height, width, bitRate, fps, durationUs);
    }


    public void transCode() {
        if (doing)
            throw new TranscodeRunTimeException("cant' trancode while transcoder is doing now...");
//        if (VERBOSE_TR)
        Log.i(TAG_TR, caller() + "transCode...." + inPutFilePath);
//        validateParams();
        long startTime = System.currentTimeMillis();
        try {
            doTranscode();
        } catch (Exception e) {
            Log.e(TAG_TR, Thread.currentThread().getName() + "|_", e);
            if (callback != null)
                callback.onError(context.getString(R.string.video_transcode_error));
        } finally {
            long elapseTime = System.currentTimeMillis() - startTime;
//            if (VERBOSE_TR)
            Log.d(TAG_TR, Thread.currentThread().getName() + "|_transcode elapse time:" + elapseTime);
        }
    }

    private void doTranscode() throws Exception {
        doing = true;
        try {
            decoderOutputVideoFormat = null;
            decoderOutputAudioFormat = null;
            videoExtractorDone = false;
            videoDecoderDone = false;
            audioExtractorDone = false;
            audioDecoderDone = false;
            videoExtractedFrameCount = 0;
            videoDecodedFrameCount = 0;
            audioExtractedFrameCount = 0;
            audioDecodedFrameCount = 0;
            if (mCopyVideo) {
                if (callback != null)
                    callback.makeEGLContext(false);
                outputSurface = new TranscodeOutputSurface(width, height);
                videoDecoder = createVideoDecoder(decoderInputVideoFormat, outputSurface.getSurface());
                if (callback != null)
                    callback.releaseEGLContext();
            }
            if (mCopyAudio) {
                audioDecoder = createAudioDecoder(decoderInputAudioFormat);
            }
//            if (VERBOSE_TR)
            Log.d(TAG_TR, caller() + "DEAD_LOCK_awaitEncode");
        } catch (Exception e) {
            releae();
        }
    }

    private void logState() {
        if (VERBOSE_TR) {
            Log.d(TAG_TR, String.format("%s|_loop: " + "V(%b){" + "extracted:%d(done:%b) " + "decoded:%d(done:%b) " + "A(%b){" + "extracted:%d(done:%b) " + "decoded:%d(done:%b) ",
                    Thread.currentThread().getName(), mCopyVideo, videoExtractedFrameCount, videoExtractorDone, videoDecodedFrameCount, videoDecoderDone, mCopyAudio, audioExtractedFrameCount, audioExtractorDone, audioDecodedFrameCount, audioDecoderDone));
        }
    }


    private MediaCodec createVideoDecoder(MediaFormat inputFormat, Surface surface) throws IOException {
        videoDecoderHandlerThread = new HandlerThread("TR_D");
        videoDecoderHandlerThread.start();
        videoDecoderHandler = new CallbackHandler(videoDecoderHandlerThread.getLooper());
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
                    int size = videoExtractor.readSampleData(decoderInputBuffer, 0);
                    long presentationTime = videoExtractor.getSampleTime();
                    if (VERBOSE_TR) {
                        Log.d(TAG_TR, Thread.currentThread().getName() + "|_video extractor: returned buffer of size " + size + "\t" + presentationTime);
                    }
                    if (size >= 0) {
                        codec.queueInputBuffer(index, 0, size, presentationTime, videoExtractor.getSampleFlags());
                    }
                    videoExtractorDone = !videoExtractor.advance();
                    if (videoExtractorDone) {
                        if (VERBOSE_TR)
                            Log.d(TAG_TR, Thread.currentThread().getName() + "|_video extractor: EOS");
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
                if (render) {
                    if (callback != null)
                        callback.makeEGLContext(true);
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
                    //回调
                    if (callback != null)
                        callback.onVideoBufferAvailable(info.presentationTimeUs * 1000);
                }
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (VERBOSE_TR)
                        Log.d(TAG_TR, Thread.currentThread().getName() + "|_video decoder: EOS");
                    videoDecoderDone = true;
                    if (callback != null)
                        callback.onVideoDecoderDone();
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
                    int size = audioExtractor.readSampleData(decoderInputBuffer, 0);
                    long presentationTime = audioExtractor.getSampleTime();
                    if (VERBOSE_TR) {
                        Log.d(TAG_TR, Thread.currentThread().getName() + "|_audio extractor: returned buffer of size " + size + "\tpts:" + presentationTime);
                    }
                    if (size >= 0) {
                        codec.queueInputBuffer(index, 0, size, presentationTime, audioExtractor.getSampleFlags());
                    }
                    audioExtractorDone = !audioExtractor.advance();
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
                if (callback != null)
                    callback.encodeAudio();
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (VERBOSE_TR)
                        Log.d(TAG_TR, Thread.currentThread().getName() + "|_audio decoder: EOS");
                    audioDecoderDone = true;
                }
            }
        });
        decoder.configure(inputFormat, null, null, 0);
        decoder.start();
        return decoder;
    }


    public TranscodeListener getCallback() {
        return callback;
    }

    public void setCallback(TranscodeListener callback) {
        this.callback = callback;
    }

    public int getCountOfThumb() {
        return countOfThumb;
    }

    public void setCountOfThumb(int countOfThumb) {
        this.countOfThumb = countOfThumb;
    }

    private void releae() {
        doing = false;
        if (VERBOSE_TR)
            Log.d(TAG_TR, Thread.currentThread().getName() + "|_releasing extractor, decoder, encoder, and muxer");
        try {
            if (videoExtractor != null) {
                videoExtractor.release();
            }
        } catch (Exception e) {
            Log.w(TAG_TR, Thread.currentThread().getName() + "|_error while releasing videoExtractor", e);
        }
        try {
            if (audioExtractor != null) {
                audioExtractor.release();
            }
        } catch (Exception e) {
            Log.w(TAG_TR, Thread.currentThread().getName() + "|_error while releasing audioExtractor", e);
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
            if (audioDecoder != null) {
                audioDecoder.stop();
                audioDecoder.release();
            }
        } catch (Exception e) {
            Log.w(TAG_TR, Thread.currentThread().getName() + "|_error while releasing audioDecoder", e);
        }

        if (videoDecoderHandlerThread != null) {
            videoDecoderHandlerThread.quitSafely();
        }
        videoExtractor = null;
        audioExtractor = null;
        outputSurface = null;
        videoDecoder = null;
        audioDecoder = null;
        videoDecoderHandlerThread = null;
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

    public MediaFormat getDecoderOutputAudioFormat() {
        return decoderOutputAudioFormat;
    }

    public MediaCodec getAudioDecoder() {
        return audioDecoder;
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

    public long getDurationUs() {
        return durationUs;
    }
}

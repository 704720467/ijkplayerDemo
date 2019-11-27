package com.zp.libvideoedit.EditCore;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;


import com.zp.libvideoedit.Constants;
import com.zp.libvideoedit.GPUImage.Core.AndroidDispatchQueue;
import com.zp.libvideoedit.exceptions.EffectRuntimeException;
import com.zp.libvideoedit.modle.DecoderThreadState;
import com.zp.libvideoedit.modle.DequeueState;
import com.zp.libvideoedit.modle.ExtractResult;
import com.zp.libvideoedit.modle.ExtractState;
import com.zp.libvideoedit.modle.HasNextResult;
import com.zp.libvideoedit.modle.InqueueState;
import com.zp.libvideoedit.modle.MediaTrack;
import com.zp.libvideoedit.modle.VideoSegment;
import com.zp.libvideoedit.modle.VideoTimer;
import com.zp.libvideoedit.utils.CodecUtils;
import com.zp.libvideoedit.utils.FormatUtils;

import java.nio.ByteBuffer;
import java.util.LinkedList;

import static com.zp.libvideoedit.Constants.TAG;
import static com.zp.libvideoedit.Constants.TAG_EN;
import static com.zp.libvideoedit.Constants.TAG_V;
import static com.zp.libvideoedit.Constants.US_MUTIPLE;
import static com.zp.libvideoedit.Constants.VERBOSE_EN;
import static com.zp.libvideoedit.Constants.VERBOSE_LOOP_V;
import static com.zp.libvideoedit.Constants.VERBOSE_SEEK;
import static com.zp.libvideoedit.Constants.VERBOSE_V;
import static com.zp.libvideoedit.modle.DecoderThreadState.idle;
import static com.zp.libvideoedit.modle.DecoderThreadState.readly;
import static com.zp.libvideoedit.modle.DecoderThreadState.runing;
import static com.zp.libvideoedit.modle.DecoderThreadState.stoped;
import static com.zp.libvideoedit.modle.DecoderThreadState.stoping;
import static com.zp.libvideoedit.modle.DecoderThreadState.unconfig;
import static com.zp.libvideoedit.utils.FormatUtils.caller;
import static com.zp.libvideoedit.utils.FormatUtils.generateCallStack;


public class VideoDecoderThread extends Thread {
    private final Object readlyLock = new Object();
    private final Object waitLock = new Object();
    private final Object stopLock = new Object();
    private final Object pauseLock = new Object();
    //    private final Object runloopOnceLock = new Object();
    //解码器相关
//    ByteBuffer[] videoDecoderInputBuffers = null;
    MediaCodec.BufferInfo videoDecoderOutputBufferInfo;
    private boolean runloopOnce = false;
    private boolean stopFlag = false;
    private MediaTrack<VideoSegment> mediaTrack;
    private Surface decoderOutputSurface;
    private MediaCodec videoDecoder;
    private VideoTrackExtractor trackExtractor = null;
    private int width;
    private int height;
    private DecoderThreadState threadState;
    private VideoTimer videoTimer;
    private DecoderCallBack decoderCallBack;
    private int textureId;
    private long loopIndex = 0;
    private long extractIndex = 0;
    private long decodecIndex = 0;
    private long lastDecodPts = 0;
    private ExtractState hasNextState = null;
    private InqueueState inqueueState = null;
    private DequeueState dequeueState = null;
    private SeekState seekState = null;
    private LinkedList<Runnable> seekRunnables;
    private boolean mMediaCodecReadying = false;//解码器准备中


    private AndroidDispatchQueue mContextQueue = AndroidDispatchQueue.dispatchQueueCreate("VDTDQ");

    public VideoDecoderThread(MediaTrack mediaTrack, Surface surface, int width, int height, VideoTimer timer, int textureId, DecoderCallBack decoderCallBack) {
        super("VDT_" + mediaTrack.getTrackType().getName());
        this.width = width;
        this.height = height;
        this.videoTimer = timer;
        this.mediaTrack = mediaTrack;
        this.textureId = textureId;
        this.decoderOutputSurface = surface;
        this.trackExtractor = new VideoTrackExtractor(mediaTrack);
        this.decoderCallBack = decoderCallBack;
        threadState = stoped;
        seekRunnables = new LinkedList<Runnable>();

    }

    /**
     * 开始线程，等待初始化完毕后返回
     */
    @Override
    public void start() {
        if (VERBOSE_V) Log.d(TAG_V + TAG_EN, formartLog("DecoderThread_start", "start", "..."));
        if (threadState != stoped)
            throw new EffectRuntimeException("illegaleState should be stoped");
        super.start();
        if (VERBOSE_V) Log.d(TAG_V, formartLog("DecoderThread_start", "starting", "...."));
        if (videoTimer != null) threadState = idle;
        else {
            threadState = runing;
        }
        //waite for init
        synchronized (readlyLock) {
            if (threadState != readly) {
                try {
                    readlyLock.wait(100);
                } catch (InterruptedException e) {
                    Log.w(TAG_V, formartELog("DecoderThread_start", "readlyLock time out", ""), e);
                }
                threadState = readly;
            }
        }


        if (VERBOSE_V) Log.d(TAG_V + TAG_EN, formartLog("DecoderThread_start", "started", "ok"));
    }

    /**
     * 暂停
     */
    public void pause() {
        if (VERBOSE_V)
            Log.d(TAG_V, formartLog("DecoderThread_pause", "PLAY_LIFECYCLE pause", "pause...."));
        if (this.threadState == stoped) {
            Log.w(TAG_V, formartLog("DecoderThread_pause", "PLAY_LIFECYCLEpause", "decoder thread already stoped"));
            return;
        }
//            throw new EffectRuntimeException();
        synchronized (pauseLock) {
            if (this.threadState != idle) {
                if (VERBOSE_V)
                    Log.d(TAG_V, formartLog("DecoderThread_pause", "PLAY_LIFECYCLEpause", "wait...."));
                this.threadState = idle;
                try {
                    pauseLock.wait(100);
                } catch (InterruptedException e) {
                    Log.w(TAG, formartELog("DecoderThread_pause", "PLAY_LIFECYCLEpause", " pauseLock.wait error" + e.getMessage()), e);
                }
            }
            decoderCallBack.onDecoderPaused(this);
        }


        if (VERBOSE_V)
            Log.d(TAG_V, formartLog("DecoderThread_pause", "PLAY_LIFECYCLEpause", "pause ok"));
    }

    public void resetDecode(MediaTrack mediaTrack) {
        if (VERBOSE_V)
            Log.d(TAG_V, formartLog("DecoderThread", "PLAY_LIFECYCLE resetDecode", "resetDecode...."));

        if (threadState != idle) {
            Log.e(TAG_V, formartLog("DecoderThread", "reset", "illegaleState should be idle"));
            pause();
        }
        this.mediaTrack = mediaTrack;
        if (trackExtractor != null) {
            trackExtractor.setMediaTrack(mediaTrack);
        } else {
            trackExtractor = new VideoTrackExtractor(mediaTrack);
        }
        threadState = unconfig;
        if (VERBOSE_V) Log.d(TAG_V, formartLog("DecoderThread", "resetDecode ok"));

    }

    public void seek(final long secUs, boolean updatePreview) {
        if (VERBOSE_V)
            Log.d(TAG_V, formartLog("DecoderThread", mediaTrack.getTrackType().getName() + "aaaaseekto:" + String.format("%,d, size", secUs) + seekRunnables.size() + ", updatePreview:" + updatePreview + " ,runloopOnce:" + runloopOnce + " ...." + generateCallStack()));
        if (!runloopOnce) return;
        if (threadState == stoped || threadState == stoping) {
            Log.w(TAG_V, formartLog("DecoderThread_seek", "seek", "decoder thread already stoped.return ..."));
            return;
        }
//        pause();
        lastDecodPts = -1;
        if (updatePreview) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    exactSeek(secUs);
                }
            };
            if (VERBOSE_V)
                Log.d(TAG_V, formartLog("DecoderThread", mediaTrack.getTrackType().getName() + "aaaaseekto_add:" + String.format("%,d, size:", secUs) + seekRunnables.size() + ", updatePreview:" + updatePreview + " ,runloopOnce:" + runloopOnce + " ...." + generateCallStack()));
            seekRunnables.add(runnable);
            if (seekRunnables.size() == 3) {
                mContextQueue.remove(seekRunnables.getFirst());
                seekRunnables.removeFirst();
                if (VERBOSE_V)
                    Log.d(TAG_V, formartLog("DecoderThread", mediaTrack.getTrackType().getName() + "aaaaseekto_remove:" + String.format("%,d size:", secUs) + seekRunnables.size() + ", updatePreview:" + updatePreview + " ,runloopOnce:" + runloopOnce + " ...." + generateCallStack()));
            }
            AndroidDispatchQueue.runAsynchronouslyOnVideoProcessingQueue(mContextQueue, runnable);
        } else {
            trackExtractor.seek(secUs);
//            inqueueState=InqueueState.;
            dequeueState = null;

            try {
                videoDecoder.flush();
            } catch (Exception e) {
                Log.w(TAG, e.getMessage(), e);
            }

        }
        if (VERBOSE_V)
            Log.d(TAG_V, formartLog("DecoderThread_seekto", "seekto:" + String.format("%,d", secUs) + " ok"));
    }

    public void seek(final long secUs) {
        seek(secUs, true);
    }

    private void exactSeek(final long secUs) {
        if (VERBOSE_SEEK)
            Log.d(TAG_V, formartLog("asy_exactSeek|" + Thread.currentThread().getName(), mediaTrack.getTrackType().getName() + "aaaaseekto", "seekUs:" + String.format("%,d", secUs)));

        try {
            if (threadState == stoped || threadState == stoping) {
                Log.w(TAG_V, formartLog("DecoderThread_seek", "exactSeek", "decoder thread already stoped"));
                return;
            }
            if (!runloopOnce) {
                Log.w(TAG, formartELog("asy_exactSeek", "runloopOnce", "pass_exactSeek"));
                return;
            }
            if (threadState == runing) {
                Log.w(TAG, formartELog("asy_exactSeek", "threadState==runing", "pass_exactSeek"));
                return;

            }
            trackExtractor.seek(secUs);
            HasNextResult hasNext = trackExtractor.hasNext();
            if (VERBOSE_SEEK)
                Log.d(TAG_V, formartLog("asy_exactSeek|" + Thread.currentThread().getName(), "|hasNext", "seekUs:" + String.format("%,d", secUs) + ", " + hasNext.toString()) + FormatUtils.generateCallStack());
            dequeueState = null;
            switch (hasNext.getState()) {
                case segEos:
                    break;
                case segBegin:
//                    if (videoDecoder != null) {
//                        if (VERBOSE_SEEK)
//                            Log.i(TAG_V, formartLog("asy_exactSeek__reset_codec" + Thread.currentThread().getName(), "hasNext", "seekUs:" + String.format("%,d", secUs) + ", segBegin reset decoder, " + hasNext.toString()));
////                    videoDecoder.flush();
//                        videoDecoder.reset();
//                        videoDecoder.configure(trackExtractor.getInputMediaFormat(), decoderOutputSurface, null, 0);
//                        videoDecoder.start();
//                    } else {
//                        safeCreateDecoder(trackExtractor.getInputMediaFormat());
//                    }
//                    if (videoDecoder == null)
                    safeCreateDecoder(trackExtractor.getInputMediaFormat());
                    if (!decodeAndRenderSeekFrame(secUs) && secUs == 0)
                        decodeAndRenderSeekFrame(secUs);
                    break;
                case empty:
                    clearInputOrOutputBuffer(secUs);

                    break;
                case hasNext:
//                if(videoDecoder!=null) {
//                    videoDecoder.flush();
//                }else{
//                    safeCreateDecoder(trackExtractor.getInputMediaFormat());
//                }
                    try {
                        videoDecoder.flush();
                        if (!decodeAndRenderSeekFrame(secUs) && secUs == 0)
                            decodeAndRenderSeekFrame(secUs);
                    } catch (Exception e) {
                        Log.e(TAG, formartELog("asy_exactSeek_hasNext", "error", e.getMessage()), e);
                        if (videoDecoder == null)
                            safeCreateDecoder(trackExtractor.getInputMediaFormat());
                    }

                    break;
            }
        } catch (Exception e) {
            Log.w(TAG, formartELog("asy_exactSeek", "error", e.getMessage()), e);
        }
    }

    private boolean decodeAndRenderSeekFrame(long secUs) {
        if (VERBOSE_SEEK)
            Log.d(TAG_V, formartLog("asy_seek_" + Thread.currentThread().getName(), "decodeAndRenderSeekFrame", "seekMs:" + secUs));

        while (true) {
//            videoDecoderInputBuffers = videoDecoder.getInputBuffers();
            int decoderInputBufferIndex = videoDecoder.dequeueInputBuffer(Constants.TIMEOUT_USEC);
            if (VERBOSE_SEEK)
                Log.d(TAG_V, formartLog("asy_seek_" + Thread.currentThread().getName(), "decodeAndRenderSeekFrame", "decoderInputBufferIndex:" + decoderInputBufferIndex));
            int retryCount = 0;
            while (decoderInputBufferIndex < 0) {
                if (VERBOSE_SEEK)
                    Log.d(TAG_V, formartLog("asy_seek_" + Thread.currentThread().getName(), "decodeAndRenderSeekFrame", "seekMs:" + secUs + ", decoderInputBufferIndex < 0"));
                retryCount++;
//               if(retryCount%10==0)
//                   clearInputOrOutputBuffer(0);
                if (retryCount > 20) {
                    Log.w(TAG_V, formartELog("asy_seek_" + Thread.currentThread().getName(), "decodeAndRenderSeekFrame", "seekMs:" + secUs + ",retry 20 times decoderInputBufferIndex < 0"));
                    return false;
                }
                continue;
            }

//            ByteBuffer decoderInputBuffer = videoDecoderInputBuffers[decoderInputBufferIndex];
            ByteBuffer decoderInputBuffer = videoDecoder.getInputBuffer(decoderInputBufferIndex);
            ExtractResult extractResult = trackExtractor.seekNext(decoderInputBuffer);
            if (VERBOSE_SEEK)
                Log.d(TAG_V, formartLog("asy_seek_" + Thread.currentThread().getName(), "decodeAndRenderSeekFrame", "extractResult:" + extractResult));
            if (extractResult.getSize() <= 0) {
                videoDecoder.queueInputBuffer(decoderInputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                if (VERBOSE_SEEK)
                    Log.d(TAG_V, formartLog("asy_seek_" + Thread.currentThread().getName(), "queueInputBuffer_sendEOS", "seekMs:" + secUs + ",next:" + extractResult));

            } else {
                long ptsInTrack = extractResult.getSegment().getTargetUs(extractResult.getPtsInFile());
//                ptsInTrack = computePresentationTimeNsec((VideoSegment) extractResult.getSegment(), extractResult.getPtsInFile());

                if (ptsInTrack < 0) {
                    if (VERBOSE_SEEK)
                        Log.e(TAG_V, formartLog("asy_seek_" + Thread.currentThread().getName(), "ptsInTrack<0,seekMs:" + secUs + ", ptsInTrack:" + ptsInTrack));
                    ptsInTrack = 0;
                }
                if (VERBOSE_SEEK)
                    Log.e(TAG_V, formartLog("asy_seek_" + Thread.currentThread().getName(), "queueInputBuffer,seekMs:" + secUs + ", ptsInTrack:" + ptsInTrack));
                videoDecoder.queueInputBuffer(decoderInputBufferIndex, 0, extractResult.getSize(), ptsInTrack, extractResult.getFlag());
            }

            int outputBufferIndex = videoDecoder.dequeueOutputBuffer(videoDecoderOutputBufferInfo, Constants.TIMEOUT_USEC);
            if (VERBOSE_SEEK)
                Log.d(TAG_V, formartLog("asy_seek_" + Thread.currentThread().getName(), "decodeAndRenderSeekFrame", "outputBufferIndex:" + outputBufferIndex));
            if (outputBufferIndex < 0) {
                if (VERBOSE_SEEK)
                    Log.d(TAG_V, formartLog("asy_seek_" + Thread.currentThread().getName(), "decodeAndRenderSeekFrame_xxxrendere", "seekMs:" + secUs + ", outputBufferIndex < 0 ||videoDecoderOutputBufferInfo.size<=0"));

                continue;
            }

            if ((videoDecoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                videoDecoder.releaseOutputBuffer(outputBufferIndex, false);
                if (VERBOSE_SEEK) {
                    Log.d(Constants.TAG_V, dequeueLog("decodeAndRenderSeekFrame_result:", "BUFFER_FLAG_CODEC_CONFIG"));
                }
                continue;
            }

            //当前segment解码结束
            if ((videoDecoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0 || videoDecoderOutputBufferInfo.size <= 0) {
                if (VERBOSE_SEEK) {
                    Log.d(Constants.TAG_V, dequeueLog("decodeAndRenderSeekFramer_result", "decoder_EOS BUFFER_FLAG_END_OF_STREAM"));
                }
                videoDecoder.releaseOutputBuffer(outputBufferIndex, true);
                break;
            }

            inqueueState = InqueueState.inqueued;
            videoDecoder.releaseOutputBuffer(outputBufferIndex, true);
            if (VERBOSE_SEEK)
                Log.d(TAG_V, formartLog("asy_seek_" + Thread.currentThread().getName(), "decodeAndRenderSeekFrame_rendere", "seekMs:" + secUs + ", releaseOutputBuffer info:" + CodecUtils.toString(videoDecoderOutputBufferInfo)));
            break;
        }
        return true;

    }

    private void clearInputOrOutputBuffer(long secUs) {
        if (VERBOSE_LOOP_V)
            Log.d(TAG_V, caller() + formartLog("asy_seek_" + Thread.currentThread().getName(), "clearInputOrOutputBuffer", "seekMs:" + secUs + ", decoder:" + videoDecoder + "..."));
        if (videoDecoder == null) return;
        while (true) {
            try {
                int outputBufferIndex = videoDecoder.dequeueOutputBuffer(videoDecoderOutputBufferInfo, Constants.TIMEOUT_USEC);
                if (outputBufferIndex > 0) {
                    if (VERBOSE_SEEK)
                        Log.d(TAG_V, formartLog("asy_seek_" + Thread.currentThread().getName(), "clearInputOrOutputBuffer释放output缓存区", "seekMs:" + secUs + ", outputBufferIndex > 0"));
                    videoDecoder.releaseOutputBuffer(outputBufferIndex, false);
                    continue;
                } else {
                    if (VERBOSE_SEEK)
                        Log.d(TAG_V, formartLog("asy_seek_" + Thread.currentThread().getName(), "clearInputOrOutputBuffer释放output缓存区完毕", "seekMs:" + secUs + ", outputBufferIndex <= 0"));
                    break;
                }
            } catch (Exception e) {
                safeCreateDecoder(trackExtractor.getInputMediaFormat());
                if (VERBOSE_SEEK)
                    Log.w(TAG_V, caller() + extractELog("safeCreateDecoder", "error by stop codec" + e.getMessage()), e);

            }
        }
    }


    ////////

    public void resumeDecode() {
        if (VERBOSE_V) Log.d(TAG_V, formartLog("DecoderThread", "PLAY_LIFECYCLE resumeDecode...."));
        if (threadState == idle) {
            threadState = runing;
            synchronized (waitLock) {
                if (VERBOSE_V)
                    Log.d(TAG_V, formartLog("DecoderThread", " PLAY_LIFECYCLEwaitLock.notify...."));
                waitLock.notify();
                if (VERBOSE_V)
                    Log.d(TAG_V, formartLog("DecoderThread", " PLAY_LIFECYCLEwaitLock.notify ok"));
            }
        } else if (threadState == unconfig) {
            threadState = readly;
        }
        if (VERBOSE_V) Log.d(TAG_V, formartLog("DecoderThread", "PLAY_LIFECYCLE resumeDecode ok"));

    }

    public long wakeupIfNeed(long targetPts, float preStartTime) {
        try {
            if (VERBOSE_LOOP_V || VERBOSE_EN)
                Log.i(TAG_V, caller() + formartLog("DecoderThread", "wakeupIfNeed", "targetPts:" + String.format("%,2d", targetPts) + ", wakeuping...."));
            synchronized (waitLock) {
                if (threadState == runing || threadState == stoping || threadState == stoped || threadState == null) {
                    if (VERBOSE_LOOP_V || VERBOSE_EN)
                        Log.i(TAG_V, caller() + formartLog("DecoderThread", "wakeupIfNeed", "targetPts:" + String.format("%,2d", targetPts) + ",dont_need wakeup_state_not_idel,threadState:" + threadState + ",targetPts:" + String.format("%,d", targetPts)));
                    return -1 * threadState.getValue();
                }
                long nextWakeUpPts = mediaTrack.shouleBeWakeup(targetPts, preStartTime);
                if (nextWakeUpPts < 0) {
                    if (VERBOSE_LOOP_V || VERBOSE_EN)
                        Log.i(TAG_V, caller() + formartLog("DecoderThread", "wakeupIfNeed", "targetPts:" + String.format("%,2d", targetPts) + ",wakeup_state_right_time_nextWakeUpPts:" + String.format("%,d", nextWakeUpPts) + ", threadState:" + threadState + ",targetPts:" + String.format("%,d", targetPts)));
                    return -1;
                }
                trackExtractor.wakeup(nextWakeUpPts);
                threadState = runing;
                dequeueState = null;
                waitLock.notifyAll();
                if (VERBOSE_V || VERBOSE_EN)
                    Log.i(TAG_V, caller() + formartLog("DecoderThread", "PLAY_LIFECYCLEwakeupIfNeed", "targetPts:" + targetPts + "wake up ok!!. state:" + threadState + ",targetPts:" + String.format("%,d", targetPts) + ", nextWakeUpPts:" + String.format("%,d", nextWakeUpPts)));
                return nextWakeUpPts;
            }
        } catch (Exception e) {
            Log.i(TAG_V, caller() + formartLog("DecoderThread", "wakeupIfNeed", "error etargetPts:" + targetPts + "wake up ok!!. state:" + threadState + ",targetPts:" + String.format("%,d", targetPts)), e);

            return -5;
        }
    }

    public void stopDecode() {
        if (VERBOSE_V) Log.d(TAG_V, formartLog("DecoderThread", caller() + "stopDecode...."));
        threadState = stoping;
        stopFlag = true;
        synchronized (waitLock) {
            waitLock.notifyAll();
        }
        synchronized (stopLock) {
            try {
                stopLock.wait(500);
            } catch (InterruptedException e) {
                Log.w(TAG_V, formartELog("DecoderThread", "PLAY_LIFECYCLEstopDecode timeOut 50ms", ""), e);
            } finally {
                threadState = stoped;
            }

        }
        if (VERBOSE_V)
            Log.d(TAG_V, formartLog("DecoderThread", caller() + "PLAY_LIFECYCLEstopDecode ok"));
    }

    @Override
    public void run() {
        try {
            preLoop();
            loop();
        } catch (Exception e) {
            Log.e(TAG_V, formartELog("DecoderThread", "run_loop error", e.getMessage()), e);
        } finally {
            postLoop();
        }

    }

    private void preLoop() throws Exception {
        try {
            initCodec();
            synchronized (readlyLock) {
                threadState = readly;
                readlyLock.notifyAll();
            }
        } catch (Exception e) {
            throw e;
        }
    }

    public void loop() throws InterruptedException {
        while (true) {
            try {
                loopIndex++;

                if (VERBOSE_LOOP_V)
                    Log.d(TAG_V, caller() + loopString("begin_loop" + loopIndex, FormatUtils.deviceInfo()));
                //判断停止解码
                if (threadState == stoping || threadState == stoped || stopFlag) {
                    if (VERBOSE_LOOP_V)
                        Log.d(Constants.TAG_V, caller() + loopString("stoping...", ""));
                    threadState = stoping;
                    return;
                }

                if (threadState == idle && runloopOnce && !isExport() || isExport() && threadState == idle) {
                    if (VERBOSE_LOOP_V)
                        Log.w(Constants.TAG_V, caller() + loopString("waitLock.waite...", ""));

                    synchronized (pauseLock) {
                        pauseLock.notifyAll();
                    }
                    synchronized (waitLock) {
                        waitLock.wait();
                    }
                    if (VERBOSE_LOOP_V)
                        Log.d(Constants.TAG_V, caller() + loopString("waitLock.continue...", ""));
                    continue;
                }
//                synchronized (pauseLock) {
                threadState = runing;
                if (mMediaCodecReadying) continue;
                try {
                    extratorAndInqueueFrame();
                } catch (Exception e) {
                    Log.e(TAG_V, caller() + formartELog("loop", "extratorAndInqueueFrame", e.getMessage()), e);
                }
                if (VERBOSE_LOOP_V)
                    Log.d(Constants.TAG_V, caller() + loopString("extract", "result:" + inqueueState));

                boolean skip = shouldSkipDequeueAndRender();
                if (VERBOSE_LOOP_V)
                    Log.d(Constants.TAG_V, caller() + loopString("shouldSkipDequeueAndRender", "skip:" + skip));
                try {
                    if (!skip) dequeueAndRender();
                } catch (Exception e) {
                    Log.e(TAG_V, caller() + formartELog("loop", "dequeueAndRender", e.getMessage()), e);
                }

                if (VERBOSE_LOOP_V)
                    Log.d(Constants.TAG_V, caller() + loopString("dequeue", "result:" + dequeueState));

                boolean shouldIdel = shouldIdelRunloop();
                if (VERBOSE_LOOP_V)
                    Log.d(Constants.TAG_V, caller() + loopString("shouldIdelRunloop", "idel:" + shouldIdel));

                if (shouldIdel) {
                    threadState = idle;
                }
                if (!runloopOnce) {
                    if (!isExport()) threadState = idle;
                    runloopOnce = true;
                    try {
                        if (VERBOSE_SEEK)
                            Log.d(TAG_V, caller() + loopString("onDecoderThreadReady", ""));
                        decoderCallBack.onDecoderThreadReady(this);
                    } catch (Exception e) {
                        Log.e(Constants.TAG_V, caller() + loopEString("decoderCallBack", "onDecoderThreadReady:" + e.getMessage()), e);
                    }
                }
//                pauseLock.notifyAll();
//                }
                if (VERBOSE_LOOP_V)
                    Log.d(TAG_V, caller() + loopString("end:loop_" + loopIndex, ""));
            } catch (IllegalStateException e) {
                Log.w(Constants.TAG_V, caller() + loopEString("loop_inner_error", "IllegalStateException:" + e.getMessage()), e);
            } catch (Exception e) {
                Log.e(Constants.TAG_V, caller() + loopEString("loop_inner_error", "error:" + e.getMessage()), e);
                throw new EffectRuntimeException(e);
            } finally {

            }
        }
    }

    private void initCodec() {
        videoDecoderOutputBufferInfo = new MediaCodec.BufferInfo();
        if (VERBOSE_V) Log.d(TAG_V, formartLog("INIT", "initCodec", ""));
    }

    private void safeFlushOrStopDecoder() {
        try {
            if (videoDecoder != null) {
                videoDecoder.stop();
                videoDecoder = null;
            }
            Log.d(TAG_V, extractLog("safeFlushOrStopDecoder", "release decoder"));
        } catch (Exception e) {
            Log.w(TAG_V, extractELog("safeFlushOrStopDecoder:", "error:" + e.getMessage()), e);
        }
    }

    private synchronized void safeCreateDecoder(MediaFormat inputVideoFormart) {
        Log.w(TAG, "safeCreateDecoder\t" + FormatUtils.generateCallStack());
        try {
            mMediaCodecReadying = true;
            if (VERBOSE_V)
                Log.d(TAG_V, caller() + extractLog("safeCreateDecoder", inputVideoFormart.toString()));

            if (videoDecoder != null) {
                try {
//                    videoDecoder.stop();
                    videoDecoder.reset();
                } catch (Exception e) {
                    Log.w(TAG_V, caller() + extractELog("safeCreateDecoder", "error by stop codec: " + e.getMessage()), e);
                    try {
                        videoDecoder.release();
                    } catch (Exception e1) {
                        Log.w(TAG_V, caller() + extractELog("safeCreateDecoder", "error by reset codec: " + e.getMessage()), e1);
                    }
                    videoDecoder = null;
                    videoDecoder = MediaCodec.createDecoderByType(inputVideoFormart.getString(MediaFormat.KEY_MIME));
                    if (VERBOSE_V)
                        Log.w(TAG_V, caller() + extractLog("safeCreateDecoder", "Create VideoDecoder Sucess!"));
                }
                videoDecoder.configure(inputVideoFormart, decoderOutputSurface, null, 0);
                videoDecoder.start();
                if (VERBOSE_V)
                    Log.w(TAG_V, caller() + extractLog("safeCreateDecoder", "Configure VideoDecoder Sucess!"));
            } else {
                videoDecoder = MediaCodec.createDecoderByType(inputVideoFormart.getString(MediaFormat.KEY_MIME));
                videoDecoder.configure(inputVideoFormart, decoderOutputSurface, null, 0);
                videoDecoder.start();
                if (VERBOSE_V)
                    Log.w(TAG_V, caller() + extractLog("safeCreateDecoder", "Create And Configure VideoDecoder Sucess!"));
            }
//            videoDecoderInputBuffers = videoDecoder.getInputBuffers();
        } catch (Exception e) {
            Log.w(TAG_V, extractELog("err by safeCreateDecoder,format:", inputVideoFormart.toString()), e);
        }
        mMediaCodecReadying = false;

    }

    private void postLoop() {
        release();
        threadState = stoped;
        synchronized (stopLock) {
            stopLock.notifyAll();
        }
    }

    public void release() {
        if (trackExtractor != null) {
            try {
                trackExtractor.release();
                if (VERBOSE_V)
                    Log.d(Constants.TAG_V, formartLog("Release", "release trackExtractor"));
            } catch (Exception e) {
                Log.e(Constants.TAG_V, formartELog("Release", "release trackExtractor", e.getMessage()), e);
            }
        }
        trackExtractor = null;


        if (videoDecoder != null) {
            try {
                videoDecoder.stop();
                if (VERBOSE_V) Log.d(Constants.TAG_V, formartLog("Release", "videoDecoder.stop()"));
            } catch (Exception e) {
                Log.w(Constants.TAG_V, formartELog("Release", "videoDecoder.stop error:", e.getMessage()), e);
            }
            try {
                videoDecoder.release();
                if (VERBOSE_V)
                    Log.d(Constants.TAG_V, formartLog("Release", "videoDecoder.release()"));
            } catch (Exception e) {
                Log.w(Constants.TAG_V, formartELog("Release", "videoDecoder.release error:", e.getMessage()), e);
            }
        }
        videoDecoder = null;

        if (mContextQueue != null) {
            AndroidDispatchQueue.dispatchQueueDestroy(mContextQueue);
        }
    }

    private boolean shouldSkipDequeueAndRender() {
        if (inqueueState == InqueueState.segBegin) return true;
        if (inqueueState == InqueueState.unconfig) return true;
        //解码结束 编码也是结束的时候就跳过本次解码过程
        if (inqueueState == InqueueState.segEos && dequeueState == DequeueState.segEos) return true;

        //读取为空,缓冲区没有有buffer
        if (inqueueState == InqueueState.empty && (dequeueState == DequeueState.segEos || dequeueState == null)) {
            return true;
        }
        if (inqueueState == InqueueState.tryAgain) {
            //TODO
            return false;
        }
//       eos,inqueued,empty renderEOS!=true
        return false;

    }

    private boolean isExport() {
        return videoTimer == null;
    }

    private boolean shouldIdelRunloop() {
//        if(isExport()) return false;
        if (dequeueState == DequeueState.segEos && hasNextState != ExtractState.segBegin) {
            return true;
        }
        if (inqueueState == InqueueState.empty && dequeueState == null) {
            return true;
        }
        if (seekState == SeekState.seekEnd) {
            seekState = null;
            return true;
        } else return false;
    }


    private InqueueState extratorAndInqueueFrame() {
        HasNextResult result = null;
        if (inqueueState != InqueueState.tryAgain) {
            result = trackExtractor.hasNext();
            if (VERBOSE_LOOP_V)
                Log.d(Constants.TAG_V, caller() + extractLog("hasNextresult", result.toString()));

            hasNextState = result.getState();
        }


        if (hasNextState == ExtractState.empty) {
            inqueueState = InqueueState.empty;
            if (seekState == SeekState.seekBegin) {
                seekState = SeekState.seekEnd;
            }
            return inqueueState;
        }

        if (hasNextState == ExtractState.segBegin) {
            MediaFormat inputFormat = trackExtractor.getInputMediaFormat();
            safeCreateDecoder(inputFormat);
            inqueueState = InqueueState.segBegin;
            return inqueueState;
        }

//        if (hasNextState == ExtractState.flushEos) {
//            safeFlushOrStopDecoder();
//            inqueueState = InqueueState.unconfig;
//            return inqueueState;
//        }
        if (hasNextState == ExtractState.segEos && inqueueState == InqueueState.segEos) {
            return inqueueState;
        }

        if (hasNextState == ExtractState.segEos || hasNextState == ExtractState.hasNext) {
            int decoderInputBufferIndex = -1;
//            try {
            decoderInputBufferIndex = videoDecoder.dequeueInputBuffer(Constants.TIMEOUT_USEC);
//            } catch (Exception e) {
//                Log.e(TAG_V, caller() + "extratorAndInqueueFrame_java.lang.IllegalStateException:" + e.getMessage());
//                safeCreateDecoder(trackExtractor.getInputMediaFormat());
//                decoderInputBufferIndex = videoDecoder.dequeueInputBuffer(Constants.TIMEOUT_USEC);
//            }
            if (decoderInputBufferIndex < 0) {
                if (VERBOSE_LOOP_V)
                    Log.d(TAG_V, caller() + extractLog("decoderInputBufferIndex<0_RETRY", "decoderInputBufferIndex=" + decoderInputBufferIndex));
                inqueueState = InqueueState.tryAgain;
                return inqueueState;
            }
            if (hasNextState == ExtractState.segEos) {
                videoDecoder.queueInputBuffer(decoderInputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                if (VERBOSE_LOOP_V)
                    Log.d(Constants.TAG_V, caller() + extractLog("videoDecoder_SEND_EOS", ""));
                inqueueState = InqueueState.segEos;
                return inqueueState;
            }
            if (hasNextState == ExtractState.hasNext) {
//                ByteBuffer decoderInputBuffer = videoDecoderInputBuffers[decoderInputBufferIndex];
                ByteBuffer decoderInputBuffer = videoDecoder.getInputBuffer(decoderInputBufferIndex);
                ExtractResult nextResult = trackExtractor.next(decoderInputBuffer, 0);
                lastDecodPts = nextResult.getSegment().getTargetUs(nextResult.getPtsInFile());
                if (VERBOSE_LOOP_V)
                    Log.e(TAG_V, caller() + "Speed After Time：" + lastDecodPts + "；Speed Before Time：" + nextResult.getSegment().getSrcUsNew(lastDecodPts));
                long targetPts = computePresentationTimeNsec((VideoSegment) nextResult.getSegment(), nextResult.getPtsInFile());

                if (nextResult.getSize() <= 0) {
                    videoDecoder.queueInputBuffer(decoderInputBufferIndex, 0, 0, targetPts, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    if (VERBOSE_LOOP_V)
                        Log.d(TAG_V, caller() + extractLog("videoDecoder_SEND_EOS", "nextResult.getSize()<=0"));
                    inqueueState = InqueueState.segEos;
                    hasNextState = ExtractState.segEos;
                    return inqueueState;
                }
                extractIndex++;
                videoDecoder.queueInputBuffer(decoderInputBufferIndex, 0, nextResult.getSize(), targetPts, nextResult.getFlag());
                if (VERBOSE_LOOP_V)
                    Log.d(Constants.TAG_V, caller() + extractLog("videoDecoder_queueInputBuffer_SurfaceTexture_getTimestamp", "ptsInFile:" + String.format("%,d", nextResult.getPtsInFile()) + ", targetPts:" + String.format("%,d", targetPts) + ", size:" + nextResult.getSize()));
                inqueueState = InqueueState.inqueued;
                if (seekState == SeekState.seekBegin) seekState = SeekState.seeking;
                return inqueueState;
            }
        }
        Log.e(TAG_V, extractELog("return", "extratorAndInqueueFrame_should never happen"));
        inqueueState = InqueueState.error;
        throw new EffectRuntimeException(extractLog("return", "extratorAndInqueueFrame_should never happen"));
    }

    private DequeueState dequeueAndRender() {

        int decoderOutputBufferIndex = videoDecoder.dequeueOutputBuffer(videoDecoderOutputBufferInfo, Constants.TIMEOUT_USEC);
        if (decoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
            if (VERBOSE_LOOP_V) {
                Log.d(Constants.TAG_V, caller() + dequeueLog("dequeueOutputBuffer_result:", "INFO_TRY_AGAIN_LATER_no video decoder output buffer"));
            }
            dequeueState = DequeueState.tryAgain;
            return dequeueState;
        }
        if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
//            videoDecoderInputBuffers = videoDecoder.getInputBuffers();
            if (VERBOSE_LOOP_V) {
                Log.d(Constants.TAG_V, caller() + dequeueLog("dequeueOutputBuffer_result:", "INFO_OUTPUT_BUFFERS_CHANGED"));
            }
            dequeueState = DequeueState.tryAgain;
            return dequeueState;
        }
        if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            if (VERBOSE_LOOP_V) {
                Log.d(Constants.TAG_V, caller() + dequeueLog("dequeueOutputBuffer_result:", "INFO_OUTPUT_FORMAT_CHANGED"));
            }
            dequeueState = DequeueState.tryAgain;
            return dequeueState;
        }
        if ((videoDecoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            videoDecoder.releaseOutputBuffer(decoderOutputBufferIndex, false);
            if (VERBOSE_LOOP_V) {
                Log.d(Constants.TAG_V, caller() + dequeueLog("dequeueOutputBuffer_result:", "BUFFER_FLAG_CODEC_CONFIG"));
            }
            dequeueState = DequeueState.tryAgain;
            return dequeueState;
        }

        //当前segment解码结束
        if ((videoDecoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            if (VERBOSE_V) {
                Log.d(Constants.TAG_V, caller() + dequeueLog("dequeueOutputBuffer_result_eos", "decoder_EOS BUFFER_FLAG_END_OF_STREAM"));
            }
            videoDecoder.releaseOutputBuffer(decoderOutputBufferIndex, true);
            dequeueState = DequeueState.segEos;


            if (lastDecodPts >= mediaTrack.getDuration().getUs() - 0.5 * US_MUTIPLE//正常接近结尾
                    || mediaTrack.isInLastSegment(lastDecodPts) //最后一个segment
                    || lastDecodPts < 0)//直接seek最后一个segment后直接结束
                decoderCallBack.onTrackEos(this, mediaTrack, lastDecodPts);
            return dequeueState;

        }
        if (videoDecoderOutputBufferInfo.size <= 0) {
            if (VERBOSE_V) {
                Log.d(Constants.TAG_V, dequeueLog("dequeueOutputBuffer_result_eos2", "decoder_EOS , buffersize<=0"));
            }
            videoDecoder.releaseOutputBuffer(decoderOutputBufferIndex, true);
            dequeueState = DequeueState.segEos;
            if (lastDecodPts >= mediaTrack.getDuration().getUs() - 0.5 * US_MUTIPLE || mediaTrack.isInLastSegment(lastDecodPts))
                decoderCallBack.onTrackEos(this, mediaTrack, lastDecodPts);
            return dequeueState;

        }

        decodecIndex++;
//        lastDecodPts = videoDecoderOutputBufferInfo.presentationTimeUs;

        while (videoTimer != null && lastDecodPts / 1000 >= videoTimer.getCurrentTimeMs()) {
            if (seekState == SeekState.seeking) break;
            if (threadState == idle) break;
            try {
                sleep(10);
                if (VERBOSE_LOOP_V)
                    Log.d(Constants.TAG_V, caller() + dequeueLog("DISPLAY_sleep_10", "lastDecodPts:" + String.format("%,d", lastDecodPts) + "\tvideoTimer.getCurrentTimeMs:" + String.format("%,d", videoTimer.getCurrentTimeMs())));
            } catch (InterruptedException e) {
                Log.w(TAG_V, dequeueLog("DISPLAY_sleep_10", "error:" + e.getMessage()), e);
                break;
            }
        }
        if (VERBOSE_LOOP_V)
            Log.d(Constants.TAG_V, caller() + dequeueLog("decoder_render_SurfaceTexture_getTimestamp", CodecUtils.toString(videoDecoderOutputBufferInfo)));
        VideoSegment segment = mediaTrack.getSegmentByUs(lastDecodPts);
        decoderCallBack.onFrameArrive(this, mediaTrack, segment, decodecIndex, lastDecodPts);
        videoDecoder.releaseOutputBuffer(decoderOutputBufferIndex, true);

        if (VERBOSE_LOOP_V && videoTimer != null)
            Log.d(Constants.TAG_V, caller() + dequeueLog("DISPLAY_render_releaseOutputBuffer", "lastDecodPts:" + String.format("%,d", lastDecodPts) + "\t" + String.format("%,d", videoTimer.getCurrentTimeMs())));

        dequeueState = DequeueState.render;
        if (seekState == SeekState.seeking) {
            seekState = SeekState.seekEnd;
        }
        return dequeueState;
    }

    private long computePresentationTimeNsec(VideoSegment segment, Long ptsInFile) {
        if (segment == null) return 0;
        long ptsInSegment = Math.round((ptsInFile - segment.getTimeMapping().getSourceTimeRange().getStartTime().getUs()) * segment.getScale());

        long targetPts = ptsInSegment + segment.getTimeMapping().getTargetTimeRange().getStartTime().getUs();
        if (targetPts < 0) {
            targetPts = 0;
        }
        if (VERBOSE_LOOP_V)
            Log.d(Constants.TAG_V, caller() + dequeueLog("computePresentationTimeNsec", "targetPts:" + String.format("%,d", targetPts) + "\t:ptsInFile:" + String.format("%,d", ptsInFile)) + String.format("reduceSize: %f", segment.getScale()) + "\t" + segment.toString());

        return targetPts;
    }


    public MediaTrack getMediaTrack() {
        return mediaTrack;
    }

    private String loopString(String method, String logString) {
        return formartLog("run_loop", method, logString);
    }

    private String loopEString(String method, String logString) {
        return formartELog("run_loop", method, logString);
    }

    private String dequeueLog(String method, String arg) {
        return formartLog("dequeueAndRender", method, arg);
    }

    private String extractLog(String method, String arg) {
        return formartLog("extratorAndInqueueFrame", method, arg);
    }

    private String extractELog(String method, String arg) {
        return formartELog("extratorAndInqueueFrame", method, arg);
    }

    private String formartLog(String stage, String method) {
        return formartLog(stage, method, "");
    }

    private String formartELog(String stage, String method, String arg) {
        stage = FormatUtils.deviceInfo() + stage;
        return formartLog(stage, method, arg);
    }

    private String formartLog(String stage, String method, String arg) {
        if (stage == null) stage = "";
        stage = stage + "___" + method;
        stage = FormatUtils.rightPad(stage, 32);
        stage = stage + "||" + arg;
        stage = FormatUtils.rightPad(stage, 64);
        return String.format(
                (videoTimer == null ? "EXPORT" : "PLAY") + "_VideoDecoderThread_|" + Thread.currentThread().getName() + "|_|%s_%d" + "|%s||LIndex:%d, EIndex:%d,DIndex:%d,pts:%,d, Tstate:%s, hasNext:%s, InState:%s, DState:%s, SeekState:%s ", mediaTrack.getTrackType().getName(), mediaTrack.getTrackId(), stage, loopIndex, extractIndex, decodecIndex, lastDecodPts, threadState, hasNextState, inqueueState, dequeueState, seekState);
    }

    enum SeekState {
        seekBegin, seeking, seekEnd;
    }


    public interface DecoderCallBack {
        public void onFrameArrive(VideoDecoderThread decoderThread, MediaTrack mediaTrack, VideoSegment segment, long frameIndexInchannel, long decodePts);

        public void onTrackEos(VideoDecoderThread decoderThread, MediaTrack mediaTrack, long decodePts);

        public void onDecoderThreadReady(VideoDecoderThread decoderThread);

        public void onDecoderPaused(VideoDecoderThread decoderThread);

    }
}





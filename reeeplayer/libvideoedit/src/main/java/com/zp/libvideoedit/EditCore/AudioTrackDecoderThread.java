package com.zp.libvideoedit.EditCore;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;


import com.zp.libvideoedit.EditConstants;
import com.zp.libvideoedit.exceptions.EffectRuntimeException;
import com.zp.libvideoedit.exceptions.InvalidVideoSourceException;
import com.zp.libvideoedit.modle.AudioSegment;
import com.zp.libvideoedit.modle.DecoderThreadState;
import com.zp.libvideoedit.modle.DequeueState;
import com.zp.libvideoedit.modle.ExtractResult;
import com.zp.libvideoedit.modle.ExtractState;
import com.zp.libvideoedit.modle.HasNextResult;
import com.zp.libvideoedit.modle.InqueueState;
import com.zp.libvideoedit.modle.MediaTrack;
import com.zp.libvideoedit.modle.VideoTimer;
import com.zp.libvideoedit.utils.CodecUtils;
import com.zp.libvideoedit.utils.FormatUtils;
import com.zp.libvideoedit.utils.Resample;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import static com.zp.libvideoedit.EditConstants.DEFAULT_AUDIO_BUFFER_SIZE;
import static com.zp.libvideoedit.EditConstants.DEFAULT_AUDIO_CHANNEL_COUNT;
import static com.zp.libvideoedit.EditConstants.DEFAULT_AUDIO_SAMPLE_RATE;
import static com.zp.libvideoedit.EditConstants.TAG_A;
import static com.zp.libvideoedit.EditConstants.TIMEOUT_USEC;
import static com.zp.libvideoedit.EditConstants.US_MUTIPLE;
import static com.zp.libvideoedit.EditConstants.VERBOSE_A;
import static com.zp.libvideoedit.EditConstants.VERBOSE_LOOP_A;
import static com.zp.libvideoedit.modle.DecoderThreadState.idle;
import static com.zp.libvideoedit.modle.DecoderThreadState.readly;
import static com.zp.libvideoedit.modle.DecoderThreadState.runing;
import static com.zp.libvideoedit.modle.DecoderThreadState.stoped;
import static com.zp.libvideoedit.modle.DecoderThreadState.stoping;
import static com.zp.libvideoedit.modle.DecoderThreadState.unconfig;

public class AudioTrackDecoderThread extends Thread {
    //线程锁
    private final Object readlyLock = new Object();
    private final Object waitLock = new Object();
    private final Object stopLock = new Object();
    //构造参数
    private MediaTrack mediaTrack;

    private AudioTrackExtractor trackExtractor;
    private VideoTimer timer;
    private CallBack callBack;
    //状态控制
    private DecoderThreadState threadState;
    private ExtractState hasNextState = null;
    private InqueueState inqueueState = null;
    private DequeueState dequeueState = null;
    private boolean stopFlag = false;
    //解码相关
    private MediaCodec audioDecoder;
    private MediaCodec.BufferInfo decoderOutputBufferInfo;
    private MediaFormat audioOutputFormat = null;
    private ByteBuffer[] decoderInputBuffers = null;
    private ByteBuffer[] decoderOutputBuffers = null;

    private long loopIndex = 0;
    private long extractIndex = 0;
    private long decodecIndex = 0;
    private long lastDecodPts = 0;
    private Map<Long, AudioSegment> ptsSegmentsMap;
    private boolean beQuiet = false;
    private AudioMixer audioMixer;//此处仅用来存储数据，混合数据在AudioPlayerCoreManager
    private int sampleRatePer = DEFAULT_AUDIO_SAMPLE_RATE;//之前的码率
    private Resample audioResample;


    public AudioTrackDecoderThread(MediaTrack track, VideoTimer timer, AudioMixer audioMixer, CallBack callBack) {
        super("ADT_" + track.getTrackType().getName());
        if (VERBOSE_A) Log.d(TAG_A, "AudioTrackDecoderThread_createThread" + track);
        this.mediaTrack = track;
        this.timer = timer;
        this.trackExtractor = new AudioTrackExtractor(mediaTrack);
        this.callBack = callBack;
        threadState = stoped;
        ptsSegmentsMap = new HashMap<Long, AudioSegment>();
        this.audioMixer = audioMixer;
        audioResample = new Resample(DEFAULT_AUDIO_SAMPLE_RATE);
//        safeCreateTrackPlayer();

    }

    /**
     * 开始线程，等待初始化完毕后返回
     */
    @Override
    public void start() {
        if (VERBOSE_A) Log.d(TAG_A, formartLog("DecoderThread", "start", "..."));
        if (threadState != stoped)
            throw new IllegalStateException("illegaleState should be stoped");
        super.start();
        if (VERBOSE_A) Log.d(TAG_A, formartLog("DecoderThread", "starting", "...."));
        //waite for init
        synchronized (readlyLock) {
            if (threadState != readly) {
                try {
                    readlyLock.wait(100);
                } catch (InterruptedException e) {
                    Log.w(TAG_A, formartLog("DecoderThread", "readlyLock time out", ""), e);
                }
                threadState = readly;
            }
        }

        if (timer == null) threadState = runing;
        else threadState = idle;
        if (VERBOSE_A) Log.d(TAG_A, formartLog("DecoderThread", "started", "ok"));
    }

    /**
     * 暂停
     */
    public void pause() {
        if (VERBOSE_A) Log.d(TAG_A, formartLog("DecoderThread", "pause", "pause...."));
        if (this.threadState == stoped || this.threadState == stoping) {
            Log.w(TAG_A, formartLog("DecoderThread", "pause", "audioDecoder thread already stoped"));
            return;
        }

        this.threadState = idle;
        if (VERBOSE_A) Log.d(TAG_A, formartLog("DecoderThread", "pause", "pause ok"));

//        try {
//            if (audioTrackPlayer != null && audioTrackPlayer.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
//                audioTrackPlayer.stop();
//                audioTrackPlayer.flush();
//            }
//        } catch (Exception e) {
//            if (VERBOSE_A)
//                Log.d(TAG_A, formartLog("AudioDecoderThread", "seek error:" + e.getMessage()));
//            safeCreateTrackPlayer();
//        }

    }

    public void resetDecode(MediaTrack mediaTrack) {
        if (VERBOSE_A) Log.d(TAG_A, formartLog("DecoderThread", "resetDecode", "resetDecode...."));

        if (threadState != idle)
            throw new IllegalStateException(formartLog("DecoderThread", "reset", "illegaleState shouldn be idle"));
        this.mediaTrack = mediaTrack;

        threadState = unconfig;
        if (VERBOSE_A) Log.d(TAG_A, formartLog("DecoderThread", "resetDecode ok"));

    }

    private boolean isSeek = false;

    public void seek(long secUs) {
        if (VERBOSE_A) Log.d(TAG_A, formartLog("DecoderThread", "seekto:" + secUs + "..."));
//        if (this.threadState == stoped || this.threadState == stoping) {
//            Log.w(TAG_A, formartLog("DecoderThread", FormatUtils.generateCallStack() + "seek:" + secUs, "audioDecoder thread already stoped"));
//            return;
//        }
        pause();
        lastDecodPts = -1;
        if (trackExtractor == null)
            trackExtractor = new AudioTrackExtractor(mediaTrack);

        trackExtractor.seek(secUs);
        if (dequeueState == DequeueState.trackEos)
            dequeueState = null;
        ptsSegmentsMap.clear();
        if (VERBOSE_A) Log.d(TAG_A, formartLog("DecoderThread", "seekto:" + secUs + " ok"));
        isSeek = true;

    }

    public void resumeDecode() {
        if (VERBOSE_A) Log.i(TAG_A, formartLog("DecoderThread", "resumeDecode...."));
        if (shouldIdelRunloop()) {
            if (VERBOSE_A) Log.i(TAG_A, formartLog("DecoderThread", "shouldIdel,waite ...."));
        } else {
            if (threadState == idle) {
                threadState = runing;
                synchronized (waitLock) {
                    waitLock.notify();
                }
            } else if (threadState == unconfig) {
                threadState = readly;
            }
        }
        if (VERBOSE_A) Log.i(TAG_A, formartLog("DecoderThread", "resumeDecode ok"));
//        if (audioTrackPlayer != null) {
//            if (audioTrackPlayer.getState() == AudioTrack.STATE_INITIALIZED)
//                audioTrackPlayer.play();
//            else
//                safeCreateTrackPlayer();
//        }

    }

    public void stopDecode() {
        if (VERBOSE_A) Log.i(TAG_A, formartLog("DecoderThread", "stopDecode...."));
        threadState = stoping;
        stopFlag = true;
//        relaseAudioTrackPlayer();
        synchronized (waitLock) {
            waitLock.notifyAll();
        }
        synchronized (stopLock) {
            try {
                stopLock.wait(50);
                threadState = stoped;
            } catch (InterruptedException e) {
                Log.w(TAG_A, formartLog("DecoderThread", "stopDecode timeOut 50ms"), e);
            }
        }
        if (VERBOSE_A) Log.d(TAG_A, formartLog("DecoderThread", "stopDecode ok"));
    }

    @Override
    public void run() {
        try {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            preLoop();
            loop();
        } catch (Exception e) {
            Log.e(TAG_A, formartELog("DecoderThread", "run_loop error", e.getMessage()), e);

        } finally {
            postLoop();
        }
    }

    public void preLoop() throws InvalidVideoSourceException {
        synchronized (readlyLock) {
            initCodec();
            threadState = readly;
            readlyLock.notifyAll();
            Log.d(TAG_A, "AudioTrackDecoderThread_selfStartedLock notifyAll");
        }
    }


    public void loop() {
        while (true) {
            try {
                loopIndex++;

                if (VERBOSE_A) Log.d(TAG_A, loopString("begin_:loop" + loopIndex, ""));
                //判断停止解码
                if (threadState == stoping || threadState == stoped || stopFlag) {
                    if (VERBOSE_LOOP_A) Log.d(TAG_A, loopString("stoping...", ""));
                    threadState = stoping;
                    return;
                }
                if (threadState == idle) {
                    if (VERBOSE_A) Log.d(TAG_A, loopString("waitLock.waite...", ""));
                    synchronized (waitLock) {
                        waitLock.wait();
                    }
                    if (VERBOSE_A) Log.d(TAG_A, loopString("waitLock.continue...", ""));
                    continue;
                }
                threadState = runing;
                extratorAndInqueueFrame();

                if (VERBOSE_LOOP_A) {
                    Log.d(TAG_A, loopString("extrator_hasNExt", "hasNextState:" + hasNextState));
                    Log.d(TAG_A, loopString("extrator_result", "inqueueState:" + inqueueState));
                }

                boolean skip = shouldSkipDequeueAndRender();
                if (VERBOSE_LOOP_A)
                    Log.d(TAG_A, loopString("shouldSkip", "skip:" + skip));
                if (!skip) dequeueAndRender();

                if (VERBOSE_LOOP_A)
                    Log.d(TAG_A, loopString("render_result", "result:" + dequeueState));

                boolean shouldIdel = shouldIdelRunloop();
                if (VERBOSE_LOOP_A)
                    Log.d(TAG_A, loopString("shouldIdel", "idel:" + shouldIdel));

                if (shouldIdel) {
                    threadState = idle;
                }
                if (VERBOSE_LOOP_A) Log.d(TAG_A, loopString("end:loop_" + loopIndex, ""));
            } catch (Exception e) {
                Log.e(TAG_A, loopEString("loop_inner_error", "error:" + e.getMessage()), e);
                throw new EffectRuntimeException(e);
            } finally {
                if (VERBOSE_A) Log.d(TAG_A, loopString("loop_finally_" + loopIndex, ""));
            }
        }

    }

    private void postLoop() {
        if (VERBOSE_A) Log.d(TAG_A, loopString("postLoop" + loopIndex, ""));
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
                if (VERBOSE_A) Log.d(TAG_A, formartLog("Release", "release trackExtractor"));
            } catch (Exception e) {
                Log.e(TAG_A, formartELog("Release", "release trackExtractor" + e.getMessage()), e);
            }
        }
        trackExtractor = null;


        if (audioDecoder != null) {
            try {
                audioDecoder.stop();
                if (VERBOSE_A) Log.d(TAG_A, formartLog("Release", "audioDecoder.stop()"));
            } catch (Exception e) {
                Log.e(TAG_A, formartELog("Release", "audioDecoder.stop error:" + e.getMessage()), e);
            }
            try {
                audioDecoder.release();
                if (VERBOSE_A) Log.d(TAG_A, formartLog("Release", "audioDecoder.release()"));
            } catch (Exception e) {
                Log.e(TAG_A, formartELog("Release", "audioDecoder.release error:" + e.getMessage()), e);
            }
        }

        audioDecoder = null;
    }


    private void initCodec() {
        decoderOutputBufferInfo = new MediaCodec.BufferInfo();
        if (VERBOSE_A) Log.d(TAG_A, formartLog("INIT", "initCodec", ""));
    }


    private void safeFlushOrStopDecoder() {
        try {
            if (audioDecoder != null) {
                audioDecoder.stop();
                audioDecoder = null;
            }
            Log.d(TAG_A, extractLog("safeFlushOrStopDecoder", "release decoder"));
        } catch (Exception e) {
            Log.w(TAG_A, extractLog("safeFlushOrStopDecoder:", "error:" + e.getMessage()), e);
        }
    }

    private void safeCreateDecoder(MediaFormat inputAudioFormart) {
        try {
            if (audioDecoder != null) {
                audioDecoder.reset();
//                audioDecoder = MediaCodec.createDecoderByType(inputAudioFormart.getString(MediaFormat.KEY_MIME));
                audioDecoder.configure(inputAudioFormart, null, null, 0);
                audioDecoder.start();
//                if (VERBOSE_A)
//                    Log.d(TAG_A, extractLog("safeCreateDecoder_resuse", inputAudioFormart.toString()));
            } else {
                audioDecoder = MediaCodec.createDecoderByType(inputAudioFormart.getString(MediaFormat.KEY_MIME));
                audioDecoder.configure(inputAudioFormart, null, null, 0);
                audioDecoder.start();
            }
            decoderInputBuffers = audioDecoder.getInputBuffers();
            decoderOutputBuffers = audioDecoder.getOutputBuffers();

        } catch (Exception e) {
            Log.e(TAG_A, extractLog("err by safeCreateDecoder,format:", "inputAudioFormart=" + inputAudioFormart), e);
        }
    }

    private boolean shouldSkipDequeueAndRender() {
        if (inqueueState == InqueueState.segBegin) return true;
        if (inqueueState == InqueueState.unconfig) return true;

        //读取为空,缓冲区没有有buffer
        if (inqueueState == InqueueState.empty && (dequeueState == DequeueState.segEos || dequeueState == null)) {
            return true;
        }
        if (inqueueState == InqueueState.tryAgain) {
            //TODO
            return false;
        }
//       eos,inqueued,empty
        return false;

    }

    private boolean isExport() {
        return timer == null;
    }

    private boolean shouldIdelRunloop() {
        if (dequeueState == DequeueState.trackEos || inqueueState == InqueueState.empty && dequeueState == null) {
            return true;
        } else return false;
    }

    private String loopString(String method, String logString) {
        return formartLog("run_loop", method, logString);
    }

    private String loopEString(String method, String logString) {
        return formartELog("run_loop", method, logString);
    }


    private String dequeueLog(String method, String arg) {
        return formartLog("Render", method, arg);
    }

    private String extractLog(String method, String arg) {
        return formartLog("extrator", method, arg);
    }

    private String extractELog(String method, String arg) {
        return formartELog("extrator", method, arg);
    }

    private String formartLog(String stage, String method) {
        return formartLog(stage, method, "");
    }

    private String formartELog(String stage, String method) {
        return formartLog(FormatUtils.deviceInfo() + stage, method, "");
    }

    private String formartELog(String stage, String method, String arg) {
        return formartLog(FormatUtils.deviceInfo() + stage, method, arg);
    }

    private String formartLog(String stage, String method, String arg) {
        if (stage == null) stage = "";
        stage = stage + "___" + method;
        stage = FormatUtils.rightPad(stage, 32);
        stage = stage + "||" + arg;
        stage = FormatUtils.rightPad(stage, 64);
        return String.format((timer == null ? "EXPORT" : "PLAY") + "_AudioDecoderThread_|" + Thread.currentThread().getName() + "|_|%s_%d" + "|%s||LIndex:%d, EIndex:%d,DIndex:%d,pts:%,d, Tstate:%s, hasNext:%s, InState:%s, DState:%s  ,segcache:" + ptsSegmentsMap.size(), mediaTrack.getTrackType().getName(), mediaTrack.getTrackId(), stage, loopIndex, extractIndex, decodecIndex, lastDecodPts, threadState, hasNextState, inqueueState, dequeueState, ptsSegmentsMap.size());
    }

    private InqueueState extratorAndInqueueFrame() {
        HasNextResult result = null;
        if (inqueueState != InqueueState.tryAgain) {
            result = trackExtractor.hasNext();
            if (VERBOSE_LOOP_A)
                Log.d(TAG_A, extractLog("hasNext:" + result.getState(), result.toString()));

            hasNextState = result.getState();
        }

        if (hasNextState == ExtractState.pading) {
            inqueueState = InqueueState.inqueued;
            return inqueueState;
        }
        if (hasNextState == ExtractState.empty) {
            inqueueState = InqueueState.empty;
            return inqueueState;
        }

        if (hasNextState == ExtractState.segBegin) {
            MediaFormat inputFormat = trackExtractor.getInputMediaFormat();
            safeCreateDecoder(inputFormat);
            inqueueState = InqueueState.segBegin;
            return inqueueState;
        }

//        if (hasNextState == ExtractState.flushEos) {
//            MediaFormat inputFormat = trackExtractor.getInputMediaFormat();
//            safeCreateDecoder(inputFormat);
//            inqueueState = InqueueState.segBegin;
//            return inqueueState;
//        }

        if (hasNextState == ExtractState.segEos && audioDecoder == null) {
            inqueueState = InqueueState.segEos;
            return inqueueState;
        }

        if (hasNextState == ExtractState.segEos || hasNextState == ExtractState.hasNext) {
            int decoderInputBufferIndex = audioDecoder.dequeueInputBuffer(TIMEOUT_USEC);
            if (decoderInputBufferIndex < 0) {
                if (VERBOSE_LOOP_A)
                    Log.d(TAG_A, extractLog("decoderInputBufferIndex<0_RETRY", "decoderInputBufferIndex=" + decoderInputBufferIndex));
                inqueueState = InqueueState.tryAgain;
                return inqueueState;
            }
            if (hasNextState == ExtractState.segEos) {
                audioDecoder.queueInputBuffer(decoderInputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                if (VERBOSE_LOOP_A) Log.d(TAG_A, extractLog("audioDecoder_SEND_EOS", ""));
                inqueueState = InqueueState.segEos;
                return inqueueState;
            }
            if (hasNextState == ExtractState.hasNext) {
                ByteBuffer decoderInputBuffer = decoderInputBuffers[decoderInputBufferIndex];

                ExtractResult nextResult = trackExtractor.next(decoderInputBuffer, 0);
                lastDecodPts = nextResult.getSegment().getTargetUsForAudio(nextResult.getPtsInFile());

                if (nextResult.getSize() <= 0) {
                    audioDecoder.queueInputBuffer(decoderInputBufferIndex, 0, 0, lastDecodPts, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    if (VERBOSE_LOOP_A)
                        Log.d(TAG_A, extractLog("audioDecoder_SEND_EOS", "nextResult.getSize()<=0"));
                    inqueueState = InqueueState.segEos;
                    hasNextState = ExtractState.segEos;
                    return inqueueState;
                }
//                long extractPts = computePresentationTimeNsec((AudioSegment) nextResult.getSegment(), nextResult.getPtsInFile());
                extractIndex++;
                audioDecoder.queueInputBuffer(decoderInputBufferIndex, 0, nextResult.getSize(), lastDecodPts, nextResult.getFlag());
                if (VERBOSE_LOOP_A)
                    Log.d(TAG_A, extractLog("audioDecoder_queueInputBuffer——AUDIO__PTS——", "ptsInFile:" + String.format("%,d", nextResult.getPtsInFile()) + ", extractPts:" + String.format("%,d", lastDecodPts) + ", size:" + nextResult.getSize()));
                inqueueState = InqueueState.inqueued;

                ptsSegmentsMap.put(lastDecodPts, (AudioSegment) nextResult.getSegment());
                return inqueueState;
            }
        }

        Log.e(TAG_A, extractELog("return", "extratorAndInqueueFrame_should never happen"));
        inqueueState = InqueueState.error;
        throw new EffectRuntimeException(extractLog("return", "extratorAndInqueueFrame_should never happen"));
    }

    final short[] emptyAudioBuffer = new short[DEFAULT_AUDIO_BUFFER_SIZE];

    private DequeueState dequeueAndRender() {
        if (hasNextState == ExtractState.pading) {
            if (VERBOSE_LOOP_A) {
                Log.d(TAG_A, dequeueLog("dequeueAndRender_pading:", "..."));
            }
            ExtractResult extractResult = trackExtractor.next(null, -1);
            lastDecodPts = extractResult.getSegment().getTargetUs(extractResult.getPtsInFile());
            if (VERBOSE_LOOP_A)
                Log.d(TAG_A, dequeueLog("dequeueAndRender_pading_render AUDIO_CODEC_PTS--AUDIO__PTS--,", "pts:" + lastDecodPts));
            callBack.onFrameArrive(this, (AudioSegment) extractResult.getSegment(), emptyAudioBuffer, DEFAULT_AUDIO_SAMPLE_RATE, DEFAULT_AUDIO_CHANNEL_COUNT, extractResult.getPtsInFile());
            dequeueState = DequeueState.render;
            return dequeueState;

        }

        if (hasNextState == ExtractState.segEos && audioDecoder == null) {
            dequeueState = DequeueState.trackEos;
            callBack.onAudioDecoderFinish(this, mediaTrack);
            return dequeueState;
        }

        int decoderOutputBufferIndex = audioDecoder.dequeueOutputBuffer(decoderOutputBufferInfo, TIMEOUT_USEC);
        if (decoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
            if (VERBOSE_LOOP_A) {
                Log.d(TAG_A, dequeueLog("dequeueOutputBuffer_result:", "INFO_TRY_AGAIN_LATER_no video decoder output buffer"));
            }
            //之前解码已经结束，当前segment为null，也就是后tr面在没有数据了，说明mediaTrack已经结束
            if (hasNextState == ExtractState.segEos || trackExtractor.hasNext().getSegment() == null) {
                dequeueState = DequeueState.trackEos;
                callBack.onAudioDecoderFinish(this, mediaTrack);
                return dequeueState;
            }
            dequeueState = DequeueState.tryAgain;
            return dequeueState;
        }
        if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            decoderOutputBuffers = audioDecoder.getInputBuffers();
            if (VERBOSE_LOOP_A) {
                Log.d(TAG_A, dequeueLog("dequeueOutputBuffer_result:", "INFO_OUTPUT_BUFFERS_CHANGED"));
            }
            dequeueState = DequeueState.tryAgain;
            return dequeueState;
        }
        if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            audioOutputFormat = audioDecoder.getOutputFormat();
//            sonic = new Sonic(audioOutputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE), audioOutputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT));
            if (VERBOSE_LOOP_A) {
                Log.d(TAG_A, dequeueLog("dequeueOutputBuffer_result:", "INFO_OUTPUT_FORMAT_CHANGED:" + audioOutputFormat));
            }
            dequeueState = DequeueState.tryAgain;
            int sampleRate = audioOutputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            int channelCount = audioOutputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            // syn onAudioFormatChanged resample
            callBack.onAudioFormatChanged(this, sampleRate, channelCount);
            return dequeueState;
        }
        if ((decoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            audioDecoder.releaseOutputBuffer(decoderOutputBufferIndex, false);
            if (VERBOSE_LOOP_A) {
                Log.d(TAG_A, dequeueLog("dequeueOutputBuffer_result:", "BUFFER_FLAG_CODEC_CONFIG"));
            }
            dequeueState = DequeueState.tryAgain;
            return dequeueState;
        }

        //当前segment解码结束
        if ((decoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            if (VERBOSE_A) {
                Log.e(TAG_A, dequeueLog("dequeueOutputBuffer_result", "decoder_EOS BUFFER_FLAG_END_OF_STREAM" + "|lastDecodPts=" + lastDecodPts + ";isLastSegment" + (mediaTrack.isLastSegment(lastDecodPts))));
            }
            audioDecoder.releaseOutputBuffer(decoderOutputBufferIndex, true);
            if (mediaTrack.isLastSegment(lastDecodPts)) dequeueState = DequeueState.trackEos;
            else dequeueState = DequeueState.segEos;
            if (lastDecodPts >= mediaTrack.getDuration().getUs() - 0.5 * US_MUTIPLE)
                callBack.onAudioDecoderFinish(this, mediaTrack);
            return dequeueState;

        }
        if (decoderOutputBufferInfo.size <= 0) {
            if (VERBOSE_A) {
                Log.e(TAG_A, dequeueLog("dequeueOutputBuffer_result", "decoder_EOS , buffersize<=0" + "|lastDecodPts=" + lastDecodPts + ";isLastSegment" + (mediaTrack.isLastSegment(lastDecodPts))));
            }
            audioDecoder.releaseOutputBuffer(decoderOutputBufferIndex, true);

//            if (mediaTrack.isLastSegment(lastDecodPts)) dequeueState = DequeueState.trackEos;
//            else
            dequeueState = DequeueState.segEos;
            if (lastDecodPts >= mediaTrack.getDuration().getUs() - 0.5 * US_MUTIPLE)
                callBack.onAudioDecoderFinish(this, mediaTrack);
            return dequeueState;

        }

        decodecIndex++;


        ByteBuffer outputBuffer = audioDecoder.getOutputBuffer(decoderOutputBufferIndex);
//        outputBuffer.rewind();

        AudioSegment segment = ptsSegmentsMap.remove(lastDecodPts);

//        if (!Segment.isEmpty(segment))//适配空数据时pds对不上问题。
//            lastDecodPts = decoderOutputBufferInfo.presentationTimeUs;

        if (VERBOSE_LOOP_A)
            Log.d(TAG_A, dequeueLog("render AUDIO_CODEC_PTS--AUDIO__PTS--, bufferInfo", CodecUtils.toString(decoderOutputBufferInfo)));

        //获取变速后的buffer
//        outputBuffer = getSpeedByteBuffer(decoderOutputBufferIndex, outputBuffer, segment);

        if (VERBOSE_LOOP_A)
            Log.d(TAG_A, dequeueLog("render AUDIO_CODEC_PTS--AUDIO__PTS--, ============>", "lastDecodPts=" + lastDecodPts));

//        short[] audioBuffer = new short[decoderOutputBufferInfo.size / 2];
        short[] audioBuffer = new short[outputBuffer.remaining() / 2];
        outputBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(audioBuffer, 0, audioBuffer.length);

        audioDecoder.releaseOutputBuffer(decoderOutputBufferIndex, false);


        int sampleRate = audioOutputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int channelCount = audioOutputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        if (segment != null)
            sampleRatePer = (int) Math.round(sampleRate / segment.getScale());
        callBack.onFrameArrive(this, segment, audioBuffer, sampleRatePer, channelCount, lastDecodPts);
        //TODO TIME 新版本移除，更换方式
//        if (isExport()) {
//        callBack.onFrameArrive(this, segment, audioBuffer, sampleRate, channelCount, lastDecodPts);
//                } else {
//            int pos = 0;
//            short[] outBuffer = audioResample.resampleAudio(audioBuffer, channelCount, sampleRate);
//            while (outBuffer.length - pos > 0) {
//                int writingSize = outBuffer.length - pos;
//                audioTrackPlayer.setVolume((segment == null || beQuiet) ? 0 : segment.getVolume());
//                int writeResult = audioTrackPlayer.write(outBuffer, pos, writingSize);
//                pos += writeResult;
//            }
//        }
//        int pos = 0;
//        short[] outBuffer = audioResample.resampleAudio(audioBuffer, channelCount, (segment == null) ? sampleRate : (int) Math.round(sampleRate / segment.getScale()));
//        while (outBuffer.length - pos > 0) {
//            int writingSize = outBuffer.length - pos;
//            audioTrackPlayer.setVolume((segment == null || beQuiet) ? 0 : segment.getVolume());
//            int writeResult = audioTrackPlayer.write(outBuffer, pos, writingSize);
//            pos += writeResult;
//        }

        dequeueState = DequeueState.render;
        return dequeueState;
    }

    int count = -1;


    private long computePresentationTimeNsec(AudioSegment segment, Long ptsInFile) {

        if (segment == null) return 0;
        long ptsInSegment = Math.round((ptsInFile - segment.getTimeMapping().getSourceTimeRange().getStartTime().getUs()) * segment.getScale());

        long targetPts = ptsInSegment + segment.getTimeMapping().getTargetTimeRange().getStartTime().getUs();
        if (targetPts < 0) {
            targetPts = 0;
        }
        if (VERBOSE_LOOP_A)
            Log.d(EditConstants.TAG_A, dequeueLog("computePresentationTimeNsec", "targetPts:" + String.format("%,d", targetPts) + "\t:ptsInFile:" + String.format("%,d", ptsInFile)) + String.format("reduceSize: %f", segment.getScale()) + "\t" + segment.toString());

        return targetPts;


//        long pts = 0;
//        long desTime = 0;
//        long offsetTime = 0;
//        if (!segment.isEmpty()) {
//            CMTime dstTmp = segment.getTimeMapping().getTargetTimeRange().getStartTime();
//            dstTmp = CMTime.convertTimeScale(dstTmp, US_MUTIPLE);
//            desTime = dstTmp.getValue();
//        }
//        if (!segment.isEmpty()) {
//            CMTime srcBegin = segment.getTimeMapping().getSourceTimeRange().getStartTime();
//            srcBegin = CMTime.convertTimeScale(srcBegin, US_MUTIPLE);
//            offsetTime = ptsInFile - srcBegin.getValue();
//        }
//        pts = Math.round(desTime + offsetTime);
//        if (pts < 0) {
//            pts = 0;
//        }
//        if (VERBOSE_LOOP_A)
//            Log.d(TAG_A, "AudioTrackDecoderThread.computePresentationTimeNsec.pts:" + pts + ",\t" + ptsInFile + "\t" + segment);
//        return pts;
    }

    public MediaTrack getMediaTrack() {
        return mediaTrack;
    }

    public interface CallBack {
        public void onAudioFormatChanged(AudioTrackDecoderThread decoderThread, int sampleRate, int channelCount);

        public void onFrameArrive(AudioTrackDecoderThread decoderThread, AudioSegment segment, short[] audioBuffer, int sampleRate, int channelCount, long pts);

        public void onAudioDecoderFinish(AudioTrackDecoderThread decoderThread, MediaTrack mediaTrack);

    }

//    // =========@Sonic
//    private Sonic sonic;
//    private float speed = 1.0f;
//    private float pitch = 1.0f;
//    private float rate = 1.0f;
//    // =========@Buffer
//    private byte[] sonicBuffer;
//    private int bufferIndex;
//    private ByteBuffer bufferSub;
//
//    /**
//     * 初始化
//     *
//     * @param bufferSize
//     * @return
//     */
//    private boolean initSonic(int bufferSize) {
//        //Sonic
//        if (sonic != null) {
//            sonic.setSpeed(speed);
//            sonic.setPitch(pitch);
//            sonic.setRate(rate);
//
//        }
//        //Buffer
//        if (sonic != null) {
//            if (sonicBuffer == null) {
//                if (4096 >= bufferSize)
//                    sonicBuffer = new byte[4096];//AAC 1024*2(16BIT)*2(stereo) 4096Byte
//                else
//                    sonicBuffer = new byte[bufferSize];
//            } else if (sonicBuffer.length < bufferSize) {
//                sonicBuffer = new byte[bufferSize];
//            }
//        }
//        return sonic != null && sonic.isDo && sonicBuffer != null && sonicBuffer.length >= bufferSize;
//    }

//    /**
//     * 获取倍速播放buffer
//     *
//     * @param decoderOutputBufferIndex
//     * @param outputBuffer
//     * @param segment
//     * @return
//     */
//    private ByteBuffer getSpeedByteBuffer(int decoderOutputBufferIndex, ByteBuffer outputBuffer, AudioSegment segment) {
//        if (segment == null) return outputBuffer;
//        speed = (float) (1.0f / segment.getScale());
////        pitch = speed;
////        rate = (float) segment.getScale();
//        if (bufferIndex != decoderOutputBufferIndex) {
//            this.bufferIndex = decoderOutputBufferIndex;
////            if ((speed != 1.0f || pitch != 1.0f || rate != 1.0f) && initSonic(outputBuffer.remaining())) {
//            if (initSonic(outputBuffer.remaining())) {
//                // =========@Sonic@=========
//                int sonicProcessingSize;
//                int position = outputBuffer.position();
//
//                // =========@Get the data and processing
//                sonicProcessingSize = outputBuffer.remaining();
//                outputBuffer.get(sonicBuffer, 0, sonicProcessingSize);
//                sonic.writeBytesToStream(sonicBuffer, sonicProcessingSize);
//                sonicProcessingSize = sonic.readBytesFromStream(sonicBuffer, sonicBuffer.length);
//
//                // =========@Put the sonic processing data
//                if (!outputBuffer.isReadOnly()) {
//                    outputBuffer.position(position);
//                    outputBuffer.limit(position + sonicProcessingSize);
//                    outputBuffer.put(sonicBuffer, 0, sonicProcessingSize);
//                    outputBuffer.position(position);
//                } else {//Use bufferSub replace buffer
//                    if (bufferSub == null || bufferSub.capacity() != sonicBuffer.length)
//                        bufferSub = ByteBuffer.wrap(sonicBuffer, 0, 0);
//                    bufferSub.position(0);
//                    bufferSub.limit(sonicProcessingSize);
//                }
//            } else if (bufferSub != null)
//                bufferSub = null;
//        }
//
//        if (bufferSub != null && outputBuffer.isReadOnly())
//            outputBuffer = bufferSub;
//
//        return outputBuffer;
//    }


//    private AudioTrack audioTrackPlayer;

//    private int safeCreateTrackPlayerCount = 0;
//
//    private void safeCreateTrackPlayer() {
//        if (VERBOSE)
//            Log.d(TAG, "AudioPlayerCoreManager_safeCreateTrackPlayer..safeCreateTrackPlayerCount=" + safeCreateTrackPlayerCount);
//        relaseAudioTrackPlayer();
//        safeCreateTrackPlayerCount++;
//        if (safeCreateTrackPlayerCount > 5) {
//            if (VERBOSE)
//                Log.d(TAG, "AudioPlayerCoreManager_safeCreateTrackPlayer..failed，safeCreateTrackPlayerCount=" + safeCreateTrackPlayerCount);
//            return;
//        }
//        audioTrackPlayer = new AudioTrack(AudioManager.STREAM_MUSIC, DEFAULT_AUDIO_SAMPLE_RATE,
//                DEFAULT_AUDIO_CHANNEL_CONFIG, AudioFormat.ENCODING_PCM_16BIT,
//                AudioTrack.getMinBufferSize(DEFAULT_AUDIO_SAMPLE_RATE, DEFAULT_AUDIO_CHANNEL_CONFIG, AudioFormat.ENCODING_PCM_16BIT) * 2,
//                AudioTrack.MODE_STREAM);
//        try {
//            if (audioTrackPlayer.getState() == AudioTrack.STATE_INITIALIZED) {
//                audioTrackPlayer.play();
//                safeCreateTrackPlayerCount = 0;
//            } else {
//                safeCreateTrackPlayer();
//            }
//            if (VERBOSE)
//                Log.d(TAG, "AudioPlayerCoreManager_safeCreateTrackPlayer oooook,safeCreateTrackPlayerCount=" + safeCreateTrackPlayerCount);
//        } catch (Exception e) {
//            if (VERBOSE)
//                Log.e(TAG, "AudioTrackDecoderThread_safeCreateTrackPlayer_error" + e.getMessage());
//        }
//    }

//    private void relaseAudioTrackPlayer() {
//        if (VERBOSE) Log.d(TAG, "AudioPlayerCoreManager_relaseAudioTrackPlayer..");
//        try {
//            if (audioTrackPlayer != null) {
//                if (audioTrackPlayer.getState() == AudioTrack.STATE_INITIALIZED)
//                    audioTrackPlayer.stop();
//                audioTrackPlayer.release();
//                audioTrackPlayer = null;
//            }
//        } catch (Exception e) {
//            Log.w(TAG_A, "AudioPlayerCoreManager_relaseAudioTrackPlayer error by release" + e.getMessage(), e);
//        }
//    }

    public boolean isBeQuiet() {
        return beQuiet;
    }

    public void setBeQuiet(boolean beQuiet) {
        this.beQuiet = beQuiet;
    }
}

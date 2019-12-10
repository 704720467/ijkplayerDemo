package com.zp.libvideoedit.EditCore;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;


import com.zp.libvideoedit.EditConstants;
import com.zp.libvideoedit.modle.AudioSegment;
import com.zp.libvideoedit.modle.ExtractResult;
import com.zp.libvideoedit.modle.ExtractState;
import com.zp.libvideoedit.modle.HasNextResult;
import com.zp.libvideoedit.modle.MediaTrack;
import com.zp.libvideoedit.modle.Segment;
import com.zp.libvideoedit.utils.CodecUtils;
import com.zp.libvideoedit.utils.FormatUtils;
import com.zp.libvideoedit.utils.MediaUtils;

import java.io.IOException;
import java.nio.ByteBuffer;

import static com.zp.libvideoedit.EditConstants.DEFAULT_AUDIO_BUFFER_SIZE;
import static com.zp.libvideoedit.EditConstants.TAG_A;
import static com.zp.libvideoedit.EditConstants.US_MUTIPLE;
import static com.zp.libvideoedit.EditConstants.VERBOSE_A;
import static com.zp.libvideoedit.EditConstants.VERBOSE_LOOP_A;
import static com.zp.libvideoedit.modle.ExtractState.hasNext;
import static com.zp.libvideoedit.modle.ExtractState.pading;
import static com.zp.libvideoedit.modle.ExtractState.segBegin;
import static com.zp.libvideoedit.modle.ExtractState.segEos;


/**
 * 音频提取器。
 */
public class AudioTrackExtractor {
    boolean seeking = false;
    boolean waking = false;
    private MediaTrack<AudioSegment> track;
    private MediaExtractor extractor;
    private AudioSegment lastSegment;
    private long pts;

    public AudioTrackExtractor(MediaTrack track) {
        if (VERBOSE_A)
            Log.d(TAG_A, "AudioTrackExtractor_AudioTrackExtractor" + track);
        this.track = track;
        pts = 0;

    }

    public void seek(long targetUs) {
        pts = targetUs;
        seeking = true;
    }

    public void wakeup(Long targetUs) {
        pts = targetUs;
        waking = true;

    }

    public HasNextResult hasNext() {
        if (VERBOSE_LOOP_A)
            Log.d(TAG_A, formartLog("hasNext_before", "pts:" + String.format("%,d", pts) + ",  extractor:" + extractor + ", seeking:" + seeking + ", waking:" + waking + ", lastSegment:" + lastSegment));

        if (pts < 0) {
            seeking = false;
            waking = false;
            lastSegment = null;
            releaseExtractor();
            if (VERBOSE_LOOP_A)
                Log.d(TAG_A, formartLog("hasNext_result", "eos!state:" + segEos + ", pts:" + String.format("%,d", pts) + ",\textractor:" + extractor + ", seeking:" + seeking + ", waking:" + waking + ", lastSegment:" + lastSegment));
            return new HasNextResult(segEos, pts, null);
        }

        AudioSegment segment = track.getSegmentByUs(pts);
        ExtractState state = null;

        if (Segment.isEmpty(lastSegment) && Segment.isEmpty(segment)) {
            if (segment != null)
                state = pading;
            else
                state = segEos;
//                state = empty;
        }

        if (!Segment.isEmpty(segment) && Segment.isEmpty(lastSegment)) {
            createExtractor(segment, pts);
            state = segBegin;
        }

        if (Segment.isEmpty(segment) && !Segment.isEmpty(lastSegment)) {
            if (segment != null)
                state = pading;
            else if (seeking) {
                state = segBegin;//flushEos;
            } else {
                state = segEos;
            }

            if (pts >= track.getDuration().getUs())
                state = segEos;
        }
        if (!Segment.isEmpty(segment) && !Segment.isEmpty(lastSegment)) {
            if (!segment.equalSegmentId(lastSegment)) {
                createExtractor(segment, pts);
                state = segBegin;
            } else if (seeking) {
                long scrUs = segment.getSrcUsForAudio(pts);
                extractor.seekTo(scrUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
                state = hasNext;
            } else {
                state = hasNext;
            }
        }
        if (VERBOSE_LOOP_A) {
            String lastSegmentId = lastSegment != null ? lastSegment.getSegmentId() : "x";
            String currentSegmentId = segment != null ? segment.getSegmentId() : "x";
            Log.d(TAG_A, formartLog("hasNext_result", "state:" + state + ", pts:" + String.format("%,d", pts) + ",\textractor:" + extractor + ", seeking:" + seeking + ", waking:" + waking + ", lastSegId:" + lastSegmentId + ",currentSegId" + currentSegmentId + "\tcurrentSegment:" + segment + "\tlastSegment:" + lastSegment));
        }

        lastSegment = segment;
        seeking = false;
        waking = false;
        return new HasNextResult(state, pts, lastSegment);

    }

    private final int ptsOffset = Math.round(DEFAULT_AUDIO_BUFFER_SIZE * US_MUTIPLE / EditConstants.DEFAULT_AUDIO_CHANNEL_COUNT / EditConstants.DEFAULT_AUDIO_SAMPLE_RATE);

    /**
     * 注意:音频存在advance=true，size<0的情况
     */
    public ExtractResult next(ByteBuffer byteBuffer, int offset) {
        if (lastSegment != null && lastSegment.isEmpty() && byteBuffer == null && offset == -1) {
            if (VERBOSE_LOOP_A)
                Log.d(TAG_A, formartLog("next_pading", "nextPts:" + String.format("%,d", pts) + ",lastSegment:" + lastSegment));
            if (pts < 0) pts = 0;
            pts += Math.round((lastSegment.getScale() * ptsOffset));
            ExtractResult extractResult = new ExtractResult(lastSegment, DEFAULT_AUDIO_BUFFER_SIZE, ptsOffset, pts);
            if (VERBOSE_LOOP_A)
                Log.d(TAG_A, formartLog("next_pading_result", "nextPts:" + String.format("%,d", pts) + ", " + extractResult.toString()));
            return extractResult;
        }

        int size = extractor.readSampleData(byteBuffer, offset);
        int flag = extractor.getSampleFlags();
        long ptsInFile = extractor.getSampleTime();
        long targetPts = lastSegment.getTargetUs(ptsInFile);

        ExtractResult extractResult = new ExtractResult(lastSegment, size, flag, ptsInFile);

        boolean advanceResult = extractor.advance();
        long nextPtsInFile = extractor.getSampleTime();
        if (nextPtsInFile < 0) advanceResult = false;
        if (advanceResult) {
            this.pts = lastSegment.getTargetUs(nextPtsInFile);
        }
        if (VERBOSE_LOOP_A)
            Log.d(TAG_A, formartLog("next_next", "nextPts:" + String.format("%,d", pts) + ", nextPtsInFile:" + nextPtsInFile + " , advanceResult:" + advanceResult + "\t" + extractResult.toString()) + ",lastSegment:" + lastSegment);

        if (!advanceResult || advanceResult && pts < 0) {
            //改chunk已经结束，下一个Seg紧密相连，但恰好在缝里，需要填空
            Segment nextSegment = track.getNextSegmentByUs(lastSegment, targetPts + (long) (0.3 * US_MUTIPLE));
            if (VERBOSE_LOOP_A)
                Log.d(TAG_A, formartLog("next_nextSegment", "targetPts:" + String.format("%,d", targetPts) + ",nextSegment:" + nextSegment));

            if (nextSegment != null) {
                this.pts = nextSegment.getTimeMapping().getTargetTimeRange().getStartTime().getUs();
                if (VERBOSE_LOOP_A)
                    Log.d(TAG_A, formartLog("next_nextSegment_pts", "nextPts:" + String.format("%,d", pts) + ", nextPtsInFile:" + nextPtsInFile + " , advanceResult:" + advanceResult + "\t" + extractResult.toString()) + ",lastSegment:" + lastSegment);

            } else pts = -1;
        }

        if (VERBOSE_LOOP_A)
            Log.d(TAG_A, formartLog("next", "nextPts:" + String.format("%,d", pts) + ", " + extractResult.toString()));

        return extractResult;


    }

    public MediaFormat getInputMediaFormat() {
        MediaFormat mediaFormat = null;
        if (lastSegment != null) {
            mediaFormat = lastSegment.getAudioFile().getFormart();
            if (VERBOSE_A) Log.d(TAG_A, formartLog("getInputMediaFormat", mediaFormat.toString()));
        }
        if (mediaFormat == null) {
            Log.e(TAG_A, formartLog("getInputMediaFormat_error", "lastSegment==null"));
        }
        return mediaFormat;


    }

    private MediaExtractor createExtractor(AudioSegment segment, long targetMs) {
        releaseExtractor();
        try {
            extractor = new MediaExtractor();
            MediaUtils.getInstance().setDataSource(extractor, segment.getAudioFile().getFilePath());
            CodecUtils.getAndSelectAudioTrackIndex(extractor);

            long scrUs = segment.getSrcUsForAudio(targetMs);
            extractor.seekTo(scrUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        } catch (IOException e) {
            Log.e(TAG_A, formartLog("createExtractor_error", e.getMessage()), e);
        }
        return extractor;
    }


    private void releaseExtractor() {
        if (extractor != null) {
            try {
                extractor.release();
                extractor = null;
            } catch (Exception e) {
                Log.e(TAG_A, formartLog("release  Extractor error:", e.getMessage()), e);
            }
        }
    }

    public void release() {
        releaseExtractor();
    }


    //    private String logString(String method, String arg) {
//        return String.format("AudioTrackExtractor%s_%d_%s\t%s", track.getTrackType().getName(), track.getTrackId(), method, arg);
//    }
    private String formartLog(String method, String arg) {
        String stage = "ATExtractor";
        stage = stage + "___" + method;
        stage = FormatUtils.rightPad(stage, 32);
        stage = stage + "||" + arg;
        stage = FormatUtils.rightPad(stage, 64);
        return String.format("EXTR_AudioDecoderThread_|" + Thread.currentThread().getName() + "|_|%s_%d" + "|%s||", track.getTrackType().getName(), track.getTrackId(), stage);
    }

    //// end

}

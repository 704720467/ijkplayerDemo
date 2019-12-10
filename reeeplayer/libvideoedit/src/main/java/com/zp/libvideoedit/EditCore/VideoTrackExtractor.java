package com.zp.libvideoedit.EditCore;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;


import com.zp.libvideoedit.EditConstants;
import com.zp.libvideoedit.Time.CMTime;
import com.zp.libvideoedit.modle.ExtractResult;
import com.zp.libvideoedit.modle.ExtractState;
import com.zp.libvideoedit.modle.HasNextResult;
import com.zp.libvideoedit.modle.MediaTrack;
import com.zp.libvideoedit.modle.Segment;
import com.zp.libvideoedit.modle.VideoSegment;
import com.zp.libvideoedit.utils.CodecUtils;
import com.zp.libvideoedit.utils.FormatUtils;
import com.zp.libvideoedit.utils.MediaUtils;

import java.io.IOException;
import java.nio.ByteBuffer;

import static com.zp.libvideoedit.EditConstants.TAG_V;
import static com.zp.libvideoedit.EditConstants.US_MUTIPLE;
import static com.zp.libvideoedit.EditConstants.VIDEO_PRE_START_TIME;
import static com.zp.libvideoedit.modle.ExtractState.empty;
import static com.zp.libvideoedit.modle.ExtractState.hasNext;
import static com.zp.libvideoedit.modle.ExtractState.segBegin;
import static com.zp.libvideoedit.modle.ExtractState.segEos;

/**
 *
 */
public class VideoTrackExtractor {
    boolean seeking = false;
    boolean waking = false;
    private MediaTrack<VideoSegment> track;
    private MediaExtractor extractor;
    private VideoSegment lastSegment;
    private int mCreateExtractorMaxCount = 20;
    private long pts;
    private int createTryMaxTimes = 5;//重试最大次数

    public VideoTrackExtractor(MediaTrack track) {
        this.track = track;
        pts = 0;
    }

    public void setMediaTrack(MediaTrack track) {
        this.track = track;
        lastSegment = null;
        extractor = null;
        seeking = false;
        waking = false;
        lastSegment = null;
        pts = 0;
    }

    public void seek(long targetUs) {
        pts = targetUs;
        seeking = true;
    }

    public void wakeup(Long targetUs) {
        pts = targetUs;
        waking = true;
        seeking = true;
    }

    public HasNextResult hasNext() {
        if (EditConstants.VERBOSE_LOOP_V)
            Log.d(TAG_V, logString("hasNext_before", "pts:" + String.format("%,d", pts) + ",  extractor:" + extractor + ", seeking:" + seeking + ", waking:" + waking + ", lastSegment:" + lastSegment));
        long hasNextPts = pts;
        if (hasNextPts < 0) {
            seeking = false;
            waking = false;
            lastSegment = null;
            releaseExtractor();
            return new HasNextResult(segEos, hasNextPts, null);
        }
        if (waking == true) {
            Log.d(TAG_V, "waking");
        }
        VideoSegment segment = (VideoSegment) track.getSegmentByUs(hasNextPts, waking);
        ExtractState state = null;

        if (Segment.isEmpty(lastSegment) && Segment.isEmpty(segment)) {
            state = empty;
            //修复seek到最后的一点时间的时候不能唤醒别的线程bug，很接近后面有效的segment
            long nextPts = (long) (hasNextPts + VIDEO_PRE_START_TIME * US_MUTIPLE);
            VideoSegment segmentNext = (VideoSegment) track.getSegmentByUs(nextPts, waking);
            if (!Segment.isEmpty(segmentNext)) {
                segment = segmentNext;
                pts = segmentNext.getTimeMapping().getTargetTimeRange().getStartTime().getUs();
                hasNextPts = pts;
                state = hasNext;
            }
            if (EditConstants.VERBOSE_LOOP_V)
                Log.d(TAG_V, "用于seek后在最后的时间内唤起别的解码线程，nextPts=" + nextPts + ",hasNextPts="
                        + hasNextPts + ",state=" + state + ",segmentNext=" + segmentNext);
            if (hasNextPts >= track.getDuration().getUs())
                state = segEos;
        }

        if (!Segment.isEmpty(segment) && Segment.isEmpty(lastSegment)) {
            createExtractorCount = 0;
            createExtractor(segment, hasNextPts);
            state = segBegin;
        }

        if (Segment.isEmpty(segment) && !Segment.isEmpty(lastSegment)) {
            if (seeking) {
                state = empty;
            } else {
                state = segEos;
            }
            if (hasNextPts >= track.getDuration().getUs())
                state = segEos;
        }

        if (!Segment.isEmpty(segment) && !Segment.isEmpty(lastSegment)) {
            //判断两个相同的segment之间真实的pts是否挨着不挨着，要seek到指定的位置
            CMTime startTime = segment.getTimeMapping().getSourceTimeRange().getStartTime();
            CMTime endTime = lastSegment.getTimeMapping().getSourceTimeRange().getEnd();
            boolean canToseek = (CMTime.subTime(startTime, endTime).getUs() > 0.5 * US_MUTIPLE);
            if (EditConstants.VERBOSE_LOOP_V)
                Log.d(TAG_V, "======segment=" + segment + ";lastSegment=" + lastSegment
                        + ";startTime=" + startTime
                        + ";endTime=" + endTime
                        + ";subTime=" + (CMTime.subTime(startTime, endTime))
                        + ";subTimeUs" + (CMTime.subTime(startTime, endTime).getUs()));

            if (segment.equalSegmentId(lastSegment)) {

                if (seeking) {
                    long scrUs = segment.getSrcUs(hasNextPts);
                    if (segment.getVideoFile().getDuration() * US_MUTIPLE - scrUs < 1.5 * US_MUTIPLE) {
                        //小米6对最后的一段视频seek有问题
                        createExtractorCount = 0;
                        createExtractor(segment, hasNextPts);
                    } else {
                        extractor.seekTo(scrUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                    }

                    long ptsInFile = extractor.getSampleTime();
                    if (EditConstants.VERBOSE_LOOP_V)
                        Log.d(TAG_V, logString("hasNext_extractor_seekTox", "scrMs:" + String.format("%,d_%d", scrUs, extractor.getCachedDuration()) + ",ptsInFile:" + String.format("%,d", ptsInFile) + ", hasNextPts:" + String.format("%,d", hasNextPts) + ",\textractor:" + extractor + ", seeking:" + seeking + ", waking:" + waking + "\tcurrentSegment:" + segment + "\tlastSegment:" + lastSegment));
                }
                state = hasNext;
            } else if (!(segment.equalSegmentId(lastSegment))) {
                if (seeking || canToseek) {
                    createExtractorCount = 0;
                    createExtractor(segment, hasNextPts);
                    long ptsInFile = extractor.getSampleTime();
                    if (EditConstants.VERBOSE_LOOP_V)
                        Log.d(TAG_V, logString("hasNext_extractor_seekTo2", "scrMs:" + String.format("%,d", hasNextPts) + ",ptsInFile:" + String.format("%,d", ptsInFile) + ", pts:" + String.format("%,d", hasNextPts) + ",\textractor:" + extractor + ", seeking:" + seeking + ", waking:" + waking + "\tcurrentSegment:" + segment + "\tlastSegment:" + lastSegment));

                    state = segBegin;
                } else {
                    //should never happen
                    state = segBegin;
                    if (EditConstants.VERBOSE_LOOP_V)
                        Log.d(TAG_V, logString("hasNext_extractor_seekTo_neverHappen", "scrMs:" + String.format("%,d", hasNextPts) + ",\textractor:" + extractor + ", seeking:" + seeking + ", waking:" + waking + "\tcurrentSegment:" + segment + "\tlastSegment:" + lastSegment));
                }
            }

        }

        if (EditConstants.VERBOSE_LOOP_V) {
            String lastSegmentId = lastSegment != null ? lastSegment.getSegmentId() : "x";
            String currentSegmentId = segment != null ? segment.getSegmentId() : "x";
            if (EditConstants.VERBOSE_LOOP_V)
                Log.d(TAG_V, logString("hasNext_result", "state:" + state + ", pts:" + String.format("%,d", hasNextPts) + ",\textractor:" + extractor + ", seeking:" + seeking + ", waking:" + waking + ", lastSegId:" + lastSegmentId + ",currentSegId" + ";" + currentSegmentId + "\tcurrentSegment:" + segment + "\tlastSegment:" + lastSegment));
        }

        //防止末尾seek带显示，然后快速seek不带显示播放状态问题
        if (hasNextPts == pts) {
            lastSegment = segment;
            seeking = false;
            waking = false;
            return new HasNextResult(state, hasNextPts, segment);
        } else {
            if (EditConstants.VERBOSE_LOOP_V)
                Log.d(TAG_V, logString("hasNext_result", "pts 异常回调~hasNextPts=" + hasNextPts + ";pts=" + pts));
            return hasNext();
        }

    }

    private int needTryTimes = 0;

    public ExtractResult next(ByteBuffer byteBuffer, int offset) {
        if (extractor == null || needTryTimes != 0) {
            if (EditConstants.VERBOSE_LOOP_V)
                Log.w(TAG_V, "VideoTrackExtractor next createExtractor!");
            createExtractor(lastSegment, pts);
        }
//        needTryTimes++;
        int size = extractor.readSampleData(byteBuffer, offset);
        if (EditConstants.VERBOSE_LOOP_V)
            Log.d(TAG_V, logString("next_size", "nextPts:" + String.format("%,d", pts)
                    + ", size:" + size + ", lastSegment:" + lastSegment));

        int flag = extractor.getSampleFlags();
        long ptsInFile = extractor.getSampleTime();
        long targetPts = lastSegment.getTargetUs(ptsInFile);


        ExtractResult extractResult = new ExtractResult(lastSegment, size, flag, ptsInFile);
        if (EditConstants.VERBOSE_LOOP_V)
            Log.d(TAG_V, logString("next_next_begin", "nextPts:" + String.format("%,d", pts) + ", nextPtsInFile:" + ptsInFile + ", targetPts:" + targetPts + ",flag:" + String.format("%,d", flag) + ", lastSegment:" + lastSegment));


        boolean advanceResult = extractor.advance();
        long nextPtsInFile = extractor.getSampleTime();

        //主动校验视频内容不正常情况,
//        long segmentEndTime = lastSegment.getTimeMapping().getSourceTimeRange().getEnd().getUs();
//        long nowPtsInFile = ptsInFile;
//        int doWhileTimes = 0;
//        while (nowPtsInFile < segmentEndTime && nextPtsInFile < 0) {
//            doWhileTimes++;
//            boolean advanceResultTemp = extractor.advance();
//            long nextPtsInFileTemp = extractor.getSampleTime();
//            if (EditConstants.VERBOSE_LOOP_V)
//                Log.d(TAG_V, logString("next_next_next_start", "nextPts:" + String.format("%,d", pts)
//                        + ", nextPtsInFile:" + nextPtsInFile + ", advanceResult:" + advanceResult)
//                        + ", nextPtsInFileTemp:" + nextPtsInFileTemp + ", advanceResultTemp:" + advanceResult
//                        + "，beforePts=" + ptsInFile + "，doWhileTimes=" + doWhileTimes);
//
//            if (nextPtsInFileTemp > 0 && nextPtsInFileTemp <= segmentEndTime) {//成功找到可继续渲染的帧
//                advanceResult = advanceResultTemp;
//                nextPtsInFile = nextPtsInFileTemp;
//                if (EditConstants.VERBOSE_LOOP_V)
//                    Log.d(TAG_V, logString("next_next_next_find", "nextPts:" + String.format("%,d", pts)
//                            + ", nextPtsInFile:" + nextPtsInFile + ", advanceResult:" + advanceResult)
//                            + ", nextPtsInFileTemp:" + nextPtsInFileTemp + ", advanceResultTemp:" + advanceResult);
//                break;
//            }
//            if (nextPtsInFileTemp > segmentEndTime || doWhileTimes > 10) {//没有找到正确的帧
//                if (EditConstants.VERBOSE_LOOP_V)
//                    Log.d(TAG_V, logString("next_next_next_stop", "nextPts:" + String.format("%,d", pts)
//                            + ", nextPtsInFile:" + nextPtsInFile + ", advanceResult:" + advanceResult)
//                            + ", nextPtsInFileTemp:" + nextPtsInFileTemp + ", advanceResultTemp:" + advanceResult
//                            + "，beforePts=" + ptsInFile + "，doWhileTimes=" + doWhileTimes);
//                advanceResult = advanceResultTemp;
//                nextPtsInFile = -1;
//                break;
//            }
//        }

        if (nextPtsInFile < 0) {
            if (EditConstants.VERBOSE_LOOP_V)
                Log.d(TAG_V, logString("next_next_no", "nextPts:" + String.format("%,d", pts) + ", nextPtsInFile:" + nextPtsInFile + ", advanceResult:" + advanceResult));
            advanceResult = false;
        }
        if (advanceResult) {
            this.pts = lastSegment.getTargetUs(nextPtsInFile);

        } else this.pts = -2;


        if (EditConstants.VERBOSE_LOOP_V)
            Log.d(TAG_V, logString("next_next_has", "nextPts:" + String.format("%,d", pts) + ", nextPtsInFile:" + nextPtsInFile + ", advanceResult：" + advanceResult));


        if (EditConstants.VERBOSE_LOOP_V)
            Log.d(TAG_V, logString("next_result", "nextPts:" + String.format("%,d", pts) + ", nextPtsInFile:" + nextPtsInFile + ", " + extractResult.toString()));
        needTryTimes = 0;
        return extractResult;
    }

    /**
     * seek的时候使用
     *
     * @param byteBuffer
     * @return
     */
    public ExtractResult seekNext(ByteBuffer byteBuffer) {
        if (extractor == null) {
            ExtractResult extractResult = new ExtractResult(lastSegment, 0, -1, -1);
            Log.e(TAG_V, logString(
                    "这种情况不应该存在seekNext_next", "nextPts:" + String.format("%,d", pts)
                            + ", lastSegment:" + lastSegment + "Extractor=" + extractor));
            return extractResult;
        }
        int size = extractor.readSampleData(byteBuffer, 0);
        int flag = extractor.getSampleFlags();
        long ptsInFile = extractor.getSampleTime();
        long targetPts = lastSegment.getTargetUs(ptsInFile);


        ExtractResult extractResult = new ExtractResult(lastSegment, size, flag, ptsInFile);
        if (EditConstants.VERBOSE_LOOP_V)
            Log.d(TAG_V, logString("seekNext_next_begin", "nextPts:" + String.format("%,d", pts) + ", nextPtsInFile:" + ptsInFile + ", targetPts" + targetPts + ",flag:" + String.format("%,d", flag) + ", lastSegment:" + lastSegment));

        boolean advanceResult = extractor.advance();
        long nextPtsInFile = extractor.getSampleTime();
        if (nextPtsInFile < 0) {

            if (EditConstants.VERBOSE_LOOP_V)
                Log.d(TAG_V, logString("seekNext_next_no", "nextPts:" + String.format("%,d", pts) + ", nextPtsInFile:" + nextPtsInFile + ", advanceResult:" + advanceResult));
            advanceResult = false;
        }

        if (EditConstants.VERBOSE_LOOP_V)
            Log.d(TAG_V, logString("seekNext_next_has", "nextPts:" + String.format("%,d", pts) + ", nextPtsInFile:" + nextPtsInFile + ", advanceResult" + advanceResult));


        if (EditConstants.VERBOSE_LOOP_V)
            Log.d(TAG_V, logString("seekNext_result", "nextPts:" + String.format("%,d", pts) + ", nextPtsInFile:" + nextPtsInFile + ", " + extractResult.toString()));

        return extractResult;


    }

    public MediaFormat getInputMediaFormat() {
        MediaFormat mediaFormat = null;
        if (lastSegment != null) {
            mediaFormat = lastSegment.getVideoFile().getVideoFormat();
            if (EditConstants.VERBOSE_V)
                Log.d(TAG_V, logString("getInputMediaFormat:", mediaFormat.toString()));
        }
        if (mediaFormat == null) {
            Log.e(TAG_V, logEString("getInputMediaFormat_error", "lastSegment==null"));
        }
        return mediaFormat;
    }

    private int createExtractorCount = 0;

    private MediaExtractor createExtractor(VideoSegment segment, long targetMs) {
        releaseExtractor();
        try {
            extractor = new MediaExtractor();
            MediaUtils.getInstance().setDataSource(extractor, segment.getVideoFile().getFilePath());
            CodecUtils.getAndSelectVideoTrackIndex(extractor);

            long scrMs = segment.getSrcUs(targetMs);
            if (EditConstants.VERBOSE_V)
                Log.d(TAG_V, logString("createExtractor:", String.format("scrMs:%,d,targetMs:%d ", scrMs, targetMs)));
            extractor.seekTo(scrMs, MediaExtractor.SEEK_TO_NEXT_SYNC);
            return extractor;
//            int flag = extractor.getSampleFlags();
//            long ptsInFile = extractor.getSampleTime();

//            if (flag == -1 && ptsInFile == -1) {
//                if (createExtractorCount > mCreateExtractorMaxCount) {
//                    if (EditConstants.VERBOSE_V)
//                        Log.d(TAG_V, logString("createExtractor:", "重新创建_MediaExtractor 已经达到最大值！:targetMs=" + targetMs + ";createExtractorCount=" + createExtractorCount + ";segment=" + segment));
//                    return extractor;
//                }
//                try {
//                    Thread.sleep(10);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                createExtractorCount++;
//                if (EditConstants.VERBOSE_V)
//                    Log.d(TAG_V, logString("createExtractor:", "重新创建_MediaExtractor:targetMs=" + targetMs + ";createExtractorCount=" + createExtractorCount + ";segment=" + segment));
//                createExtractor(segment, targetMs);
//            } else {
//                createExtractorCount = 0;
//            }

        } catch (IOException e) {
            Log.e(TAG_V, logEString("createExtractor_error", e.getMessage()), e);
        }
        return extractor;
    }


    private void releaseExtractor() {
        if (extractor != null) {
            try {
                extractor.release();
                extractor = null;
                if (EditConstants.VERBOSE_V)
                    Log.d(TAG_V, logString("createExtractor:", "releaseExtractor Sucess!"));
            } catch (Exception e) {
                Log.e(TAG_V, logEString("release  Extractor error:", e.getMessage()), e);
            }
        }
    }

    public void release() {
        releaseExtractor();
    }


    private String logString(String method, String arg) {
        return String.format("VideoDecoderThread_VideoTrackExtractor_|%s|_%s_%d_%s\t%s", Thread.currentThread().getName(), track.getTrackType().getName(), track.getTrackId(), method, arg);
    }

    private String logEString(String method, String arg) {
        return FormatUtils.deviceInfo() + String.format("VideoDecoderThread_VideoTrackExtractor_|%s|_%s_%d_%s\t%s", Thread.currentThread().getName(), track.getTrackType().getName(), track.getTrackId(), method, arg);
    }

}

package com.zp.libvideoedit.modle;

import android.util.Log;


import com.zp.libvideoedit.Time.CMTimeMapping;
import com.zp.libvideoedit.Time.CMTimeRange;
import com.zp.libvideoedit.utils.FormatUtils;

import java.util.Arrays;
import java.util.List;

import static com.zp.libvideoedit.Constants.MAX_TIME_DIFF_SEC;


/**
 * Created by gwd on 2018/3/9.
 */

public abstract class Segment {
    static final int CharContPerSec = 50;// 27;
    static final int TimePrecision = 6;//2
    static final boolean printFullPath = false;
    protected boolean empty = true;
    protected int trackId;
    protected String segmentId;
    protected CMTimeMapping timeMapping;
    protected MediaType mediaType;
    // 0.0~1.0
    protected float volume = 1;

    public void print() {
        Log.e("Segment", "trackId: " + this.trackId + " empty: " + this.empty + " timemaping: " + this.timeMapping.print());
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public boolean isEmpty() {
        return empty;
    }

    public int getTrackId() {
        return trackId;
    }

    public CMTimeMapping getTimeMapping() {
        return timeMapping;
    }

    public String getSegmentId() {
        return segmentId;
    }

    public void setSegmentId(String segmentId) {
        this.segmentId = segmentId;
    }

    public void setTimeMapping(CMTimeMapping timeMapping) {
        this.timeMapping = timeMapping;
    }

    public void setSourceTimeRange(CMTimeRange timeRange) {
        this.timeMapping = new CMTimeMapping(timeRange, this.timeMapping.getTargetTimeRange());
    }

    public float getVolume() {
        if (isEmpty()) return 0;
        return volume;
    }

    public void setVolume(float volume) {
        this.volume = volume;
    }

    public void setTargetTimeRange(CMTimeRange timeRange) {
        this.timeMapping = new CMTimeMapping(this.timeMapping.getSourceTimeRange(), timeRange);
    }
//    public boolean isVideoSegment(){
//        if(trackId!=TrackType.TrackType_Audio_BackGround&&trackId!=TrackType.TrackType_Audio_Recoder)
//    }

    public abstract String getFileName();

    public abstract String getFullFileName();

    public boolean isSlowSpeed() {
        return timeMapping.getTargetTimeRange().getDuration().getSecond() > MAX_TIME_DIFF_SEC + timeMapping.getSourceTimeRange().getDuration().getSecond();
    }

    public boolean isFastSpeed() {
        return timeMapping.getTargetTimeRange().getDuration().getSecond() + MAX_TIME_DIFF_SEC < timeMapping.getSourceTimeRange().getDuration().getSecond();
    }

    public List<String> prettyLines() {
        //targetBegin/srcBegin--------srctEnd/targentEnd
        String begingTimes = String.format("%." + TimePrecision + "f/%." + TimePrecision + "f", timeMapping.getTargetTimeRange().getStartTime().getSecond(), timeMapping.getSourceTimeRange().getStartTime().getSecond());
        String endTimes = String.format("%." + TimePrecision + "f/%." + TimePrecision + "f", timeMapping.getSourceTimeRange().getEnd().getSecond(), timeMapping.getTargetTimeRange().getEnd().getSecond());
        String duration = String.format("%." + TimePrecision + "f", timeMapping.getTargetTimeRange().getDuration().getSecond());
        char padChar = '-';
        if (empty) padChar = '.';
        else if (isSlowSpeed()) padChar = '~';
        else if (isFastSpeed()) padChar = '*';

        int length = (int) Math.max(Math.round(timeMapping.getTargetTimeRange().getDuration().getSecond() * CharContPerSec), CharContPerSec);


        String line1 = FormatUtils.fill(begingTimes, endTimes, length, padChar);

        String line2 = empty ? "empty" : (printFullPath ? getFullFileName() : getFileName());
        if (!printFullPath)
            line2 = line2.substring(Math.max(0, line2.length() - 14));
        line2 = String.format("%s_%s", duration, line2);
        line2 = FormatUtils.pad(line2, length, padChar);

        return Arrays.asList(new String[]{line1, line2});
    }

    public boolean equalSegmentId(Segment segment) {
        if (segment == null) return false;
        return getSegmentId().equalsIgnoreCase(segment.getSegmentId());

    }

    /**
     * 获取原视频的时间戳
     *
     * @param targetUs
     */
//    public long getSrcMs(long targetMs) {
//        return targetMs - timeMapping.getTargetTimeRange().getStartTime().getMs() + timeMapping.getSourceTimeRange().getStartTime().getMs();
//    }
    // TIME ok
    public long getSrcUs(long targetUs) {
        double offset = targetUs - timeMapping.getTargetTimeRange().getStartTime().getUs();
        offset = offset / getScale();
        return Math.round(offset) + timeMapping.getSourceTimeRange().getStartTime().getUs();
    }

    /**
     * 根据播放的时间，计算原始时间,如果没有变速两个时间是一样的
     *
     * @param targetUs
     * @return
     */
    public long getSrcUsNew(long targetUs) {
        double offset = targetUs - timeMapping.getTargetTimeRange().getStartTime().getUs();
        offset = offset / getScale();
        return Math.round(offset) + timeMapping.getSourceTimeRange().getStartTime().getUs();
    }

    public long getSrcUsForAudio(long targetUs) {
        double offset = targetUs - timeMapping.getTargetTimeRange().getStartTime().getUs();
        offset = offset / getScale();
        return Math.round(offset) + timeMapping.getSourceTimeRange().getStartTime().getUs();
    }

    // TIME
    public boolean containUs(long srcPtsUs) {
        return timeMapping != null && timeMapping.getSourceTimeRange().containUs(srcPtsUs);
    }

    /**
     * 根据chunk的pts,获取track的pts。单位 us
     *
     * @param srcPtsUs
     * @return
     */
    // TIME
    public long getTargetUs(long srcPtsUs) {
        double offset = srcPtsUs - timeMapping.getSourceTimeRange().getStartTime().getUs();
        offset = offset * getScale();
        return Math.round(offset) + timeMapping.getTargetTimeRange().getStartTime().getUs();
    }


    public long getTargetUsForAudio(long srcPtsUs) {
        double offset = srcPtsUs - timeMapping.getSourceTimeRange().getStartTime().getUs();
        offset = offset * getScale();
        return Math.round(offset) + timeMapping.getTargetTimeRange().getStartTime().getUs();
    }

    public static boolean isEmpty(Segment segment) {
        return segment == null || segment.isEmpty();
    }

    public double getScale() {
        if (isEmpty()) return 1;
        double scale = (timeMapping.getTargetTimeRange().getDuration().getUs() * 1.0d) / (double) timeMapping.getSourceTimeRange().getDuration().getUs();
        return scale;
    }

    public static Segment cloneSegment(Segment segment, double srcBegin, double srcDuration, double targentbegin, double targetDuration) {
        if (segment.mediaType == MediaType.MEDIA_TYPE_Audio) {
            AudioSegment cs = (AudioSegment) segment;
            AudioSegment newSeg = new AudioSegment(segment.isEmpty(), cs.getAudioFile(), segment.getTrackId(), new CMTimeMapping(new CMTimeRange(srcBegin, srcDuration), new CMTimeRange(targentbegin, targetDuration)), cs.getSegmentId());
            return newSeg;
        } else if (segment.mediaType == MediaType.MEDIA_TYPE_Video) {
            VideoSegment cs = (VideoSegment) segment;
            VideoSegment newSeg = new VideoSegment(segment.isEmpty(), cs.getVideoFile(), segment.getTrackId(), new CMTimeMapping(new CMTimeRange(srcBegin, srcDuration), new CMTimeRange(targentbegin, targetDuration)), cs.getSegmentId());
            return newSeg;
        }
        return null;
    }
}


package com.zp.libvideoedit.modle;

import android.graphics.SurfaceTexture;
import android.util.Log;


import androidx.annotation.NonNull;

import com.zp.libvideoedit.EditConstants;
import com.zp.libvideoedit.Time.CMTime;
import com.zp.libvideoedit.Time.CMTimeMapping;
import com.zp.libvideoedit.Time.CMTimeRange;
import com.zp.libvideoedit.exceptions.EffectRuntimeException;
import com.zp.libvideoedit.utils.FormatUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.zp.libvideoedit.EditConstants.TAG;
import static com.zp.libvideoedit.EditConstants.US_MUTIPLE;
import static com.zp.libvideoedit.EditConstants.VERBOSE;
import static com.zp.libvideoedit.utils.FormatUtils.generateCallStack;
import static com.zp.libvideoedit.utils.FormatUtils.repeat;
import static com.zp.libvideoedit.utils.FormatUtils.rightPad;


/**
 * Created by gwd on 2018/3/8.
 */

public class MediaTrack<T extends Segment> {
    private MediaType mediaType;
    private int trackId;
    private ArrayList<T> segments;
    private CMTime duration;
    private TrackType trackType;
    private int surfaceTextrureid;
    private SurfaceTexture surfaceTexture;
    private boolean decodeEos;
    final float pading = 0.1f;//防止产生特别小的segment
    private AudioMixInputParameter inputParameter;

    public MediaTrack(MediaType type, TrackType trackType) {
        this.mediaType = type;
        segments = new ArrayList<T>();
        this.trackType = trackType;
        this.decodeEos = false;
    }

    public boolean insertTrack(VideoFile videoFile, CMTimeRange srcTimeRange, CMTime insertAtTime) {
        CMTime lastDesTime = CMTime.zeroTime();
        if (segments.size() != 0) {
            Segment lastsegment = segments.get(segments.size() - 1);
            lastDesTime = lastsegment.timeMapping.getTargetTimeRange().getEnd();
            if (EditConstants.VERBOSE_V)
                Log.d(this.getClass().getSimpleName(), "MediaTrack_insertTrack_nomal_src" + " srcStart: " + srcTimeRange.getStartTime().getSecond() + "  end " + srcTimeRange.getEnd().getSecond());
        }
        //说明插入的时间比当前的segments的终点时间还要大需要自动插入空segment
        if (CMTime.compare(insertAtTime, lastDesTime) > 0) {
            CMTimeRange timeRange = new CMTimeRange(lastDesTime, CMTime.subTime(insertAtTime, lastDesTime, US_MUTIPLE));
            insertEmpy(timeRange);
            if (EditConstants.VERBOSE_V)
                Log.d(this.getClass().getSimpleName(), "MediaTrack_insertTrack_empty_src" + " srcStart: " + timeRange.getStartTime().getSecond() + "  end " + timeRange.getEnd().getSecond());
        } else if (CMTime.compare(insertAtTime, lastDesTime) < 0) {
            //如果后插入的 开始时间小于上一个的结束时间，修改上一个展示时间，（待定：小于0.1秒自动移除）
            Segment lastsegment = segments.get(segments.size() - 1);
            CMTimeRange newTimeRange = CMTimeRange.CMTimeRangeTimeToTime(lastsegment.timeMapping.getTargetTimeRange().getStartTime(), insertAtTime);
            lastsegment.setTargetTimeRange(newTimeRange);
        }
        CMTimeRange timeRange = new CMTimeRange(insertAtTime, srcTimeRange.getDuration());
        Log.d(this.getClass().getSimpleName(), "MediaTrack_insertTrack_dst" + " srcStart: " + timeRange.getStartTime().getSecond() + "  end " + timeRange.getEnd().getSecond());

        CMTimeMapping timeMapping = new CMTimeMapping(srcTimeRange, timeRange);
        VideoSegment segment = new VideoSegment(false, videoFile, trackId, timeMapping, UUID.randomUUID().toString());
        segments.add((T) segment);
        return true;
    }

    public boolean insertTrack(AudioFile audioFile, CMTimeRange srcTimeRange, CMTime insertAtTime) {
        CMTime lastDesTime = CMTime.zeroTime();
        if (segments.size() != 0) {
            Segment lastsegment = segments.get(segments.size() - 1);
            lastDesTime = lastsegment.timeMapping.getTargetTimeRange().getEnd();
        }
        //说明插入的时间比当前的segments的终点时间还要大需要自动插入空segment
        if (CMTime.compare(insertAtTime, lastDesTime) > 0) {
            CMTimeRange timeRange = new CMTimeRange(lastDesTime, CMTime.subTime(insertAtTime, lastDesTime));
            insertEmpy(timeRange);
        }
        CMTimeRange timeRange = new CMTimeRange(insertAtTime, srcTimeRange.getDuration());
        CMTimeMapping timeMapping = new CMTimeMapping(srcTimeRange, timeRange);
        AudioSegment segment = new AudioSegment(false, audioFile, trackId, timeMapping, UUID.randomUUID().toString());
        segments.add((T) segment);
        return true;
    }

    public boolean insertEmpy(CMTimeRange timeRange) {
        Segment segment = null;
        CMTimeMapping timeMapping = new CMTimeMapping(CMTimeRange.zeroTimeRange(), timeRange);
        if (mediaType == MediaType.MEDIA_TYPE_Video) {
            segment = new VideoSegment(true, null, trackId, timeMapping, UUID.randomUUID().toString());
        } else {
            segment = new AudioSegment(true, null, trackId, timeMapping, UUID.randomUUID().toString());
        }
        segments.add((T) segment);
        return true;
    }

    public T getSegment(double targetSec) {
        if (targetSec < 0) return null;
        Segment tmpSegment = null;
        for (Segment segment : this.segments) {
            CMTime start = segment.getTimeMapping().getTargetTimeRange().getStartTime();
            CMTime end = segment.getTimeMapping().getTargetTimeRange().getEnd();
            if (targetSec >= CMTime.getSecond(start) && targetSec < CMTime.getSecond(end)) {
                tmpSegment = segment;
                break;
            }
        }
        return (T) tmpSegment;
    }

    public T getSegmentByUs(long targetUs) {
        if (targetUs < 0) return null;
        Segment tmpSegment = null;
        for (Segment segment : this.segments) {
            long startUs = segment.getTimeMapping().getTargetTimeRange().getStartTime().getUs();
            long endUs = segment.getTimeMapping().getTargetTimeRange().getEnd().getUs();
            if (targetUs >= startUs && targetUs < endUs) {
                tmpSegment = segment;
                break;
            }
        }
        return (T) tmpSegment;
    }

    public T getSegmentByUs(long targetUs, boolean forward) {
        if (targetUs < 0) return null;
        if (forward) return getLastSegmentByUs(targetUs);
        else return getSegmentByUs(targetUs);
    }

    public T getNextSegmentByUs(Segment currentSeg, long targetUs) {
        if (targetUs < 0) return null;
        Segment tmpSegment = null;
        T nextSeg = getLastSegmentByUs(targetUs);
        if (currentSeg == null && nextSeg == null) {
            return null;
        }
        if (currentSeg == nextSeg || nextSeg == null) {
            int currentIndex = segments.indexOf(currentSeg);
            if (currentIndex + 1 < segments.size()) {
                nextSeg = segments.get(currentIndex + 1);
            } else nextSeg = null;
        }

        return nextSeg;
    }

    public T getLastSegmentByUs(long targetUs) {
        if (targetUs < 0) return null;
        Segment tmpSegment = null;
        for (int i = segments.size() - 1; i >= 0; i--) {
            Segment segment = segments.get(i);
            long startUs = segment.getTimeMapping().getTargetTimeRange().getStartTime().getUs();
            long endUs = segment.getTimeMapping().getTargetTimeRange().getEnd().getUs();
            if (targetUs >= startUs && targetUs < endUs) {
                tmpSegment = segment;
                break;
            }
        }
        return (T) tmpSegment;
    }

    public void removeFromTime(CMTime atTime) {
        if (EditConstants.VERBOSE_EDIT)
            Log.e(EditConstants.TAG_AE, "MediaTrack_removeFromTime" + " track :  " + trackType + " atTime: " + atTime.getSecond() + "   duration:   " + getDuration().getSecond());
        //如果裁剪的时间大于mediaTrack的时长直接return
        if (atTime.getSecond() > getDuration().getSecond()) return;
        for (int i = segments.size() - 1; i >= 0; --i) {
            Segment segment = segments.get(i);
            if (segment.getTimeMapping().getTargetTimeRange().getEnd().getUs() <= atTime.getUs()) {
                break;
            } else if (segment.getTimeMapping().getTargetTimeRange().getStartTime().getUs() >= atTime.getUs()) {
                segments.remove(segment);
            } else if (segment.getTimeMapping().getTargetTimeRange().getStartTime().getUs() < atTime.getUs() && segment.getTimeMapping().getTargetTimeRange().getEnd().getUs() > atTime.getUs()) {
                CMTime targetDuration = CMTime.subTime(atTime, segment.getTimeMapping().getTargetTimeRange().getStartTime());
                float persent = (float) (targetDuration.getUs() / (1.0 * segment.getTimeMapping().getTargetTimeRange().getDuration().getUs()));
                CMTime sourceDuration = new CMTime(segment.getTimeMapping().getSourceTimeRange().getDuration().getSecond() * persent);
                CMTimeRange target = new CMTimeRange(segment.getTimeMapping().getTargetTimeRange().getStartTime(), targetDuration);
                CMTimeRange source = new CMTimeRange(segment.getTimeMapping().getSourceTimeRange().getStartTime(), sourceDuration);
                segment.setTimeMapping(new CMTimeMapping(source, target));
            }
        }
    }

    public boolean isInLastSegment(long lastDecodPts) {
        if (segments == null || segments.size() == 0) return true;
        Segment lastSegment = segments.get(segments.size() - 1);
        if (lastDecodPts - 0.1 > lastSegment.getTimeMapping().getTargetTimeRange().getStartTime().getUs()) {
            return true;
        }
        return false;
    }


    class CutPoint implements Comparable {
        double point;

        public CutPoint(double point) {
            this.point = point;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CutPoint cutPoint = (CutPoint) o;
            if (Math.abs(cutPoint.point - this.point) <= pading) return true;
            else return false;
        }

        @Override
        public int compareTo(@NonNull Object o) {
            CutPoint other = (CutPoint) o;
            return Double.compare(this.point, other.point);
        }

        @Override
        public String toString() {
            return String.valueOf(point);
        }
    }

    public void scaleTimeRange(ArrayList<TimeScaleModel> timeScaleModels) {

        Collections.sort(timeScaleModels);
        if (VERBOSE) Log.d(TAG, "scaleTimeRange:" + timeScaleModels);
        if (timeScaleModels == null || timeScaleModels.size() == 0) {
            if (VERBOSE)
                Log.w(TAG, "empty timeScaleModels");
            return;
        }
        if (segments.size() == 0) {
            if (VERBOSE)
                Log.w(TAG, "empty segments. " + this.toString() + generateCallStack());
            return;
        }


        ArrayList<CutPoint> cutPoints = new ArrayList<>();
        for (Segment segment : segments) {
            CutPoint point = new CutPoint(segment.getTimeMapping().getTargetTimeRange().getStartTime().getSecond());
            if (!cutPoints.contains(point)) {
                cutPoints.add(point);
            }
            point = new CutPoint(segment.getTimeMapping().getTargetTimeRange().getEnd().getSecond());
            if (!cutPoints.contains(point)) {
                cutPoints.add(point);
            }
        }

        for (TimeScaleModel scaleModel : timeScaleModels) {
            if (scaleModel.position() > getDuration().getSecond()) continue;
            CutPoint point = new CutPoint(scaleModel.position());
            if (!cutPoints.contains(point)) {
                cutPoints.add(point);
            }
        }
        Collections.sort(cutPoints);

        if (VERBOSE) Log.d(TAG, "scaleTimeRange:" + cutPoints);
        if (cutPoints.size() <= 1)
            throw new EffectRuntimeException("wrong value");

        float offset = 0f;


        ArrayList<T> newSegments = new ArrayList<T>();
        double lastSegTargetEnd = 0;
        for (int i = 1; i < cutPoints.size(); i++) {
            double begin = cutPoints.get(i - 1).point;
            double end = cutPoints.get(i).point;

            Segment curSeg = findSegment(begin, end);
            double currentScale = findScale(timeScaleModels, begin, end);

            double curSegSrcBegin = curSeg.getTimeMapping().getSourceTimeRange().getStartTime().getSecond();
            double curSegSrcEnd = curSeg.getTimeMapping().getSourceTimeRange().getEnd().getSecond();
            double curSegTargetBegin = curSeg.getTimeMapping().getTargetTimeRange().getStartTime().getSecond();
            double curSegTargetEnd = curSeg.getTimeMapping().getTargetTimeRange().getEnd().getSecond();

            double newSegSrcBegin = begin - curSegTargetBegin + curSegSrcBegin;
            double newSegSrcDuration = end - begin;

            double newSegTargetBeging = Math.max(begin + offset, lastSegTargetEnd);

            double newSegTargetDuration = (end - begin) * currentScale;

            Segment newSeg = Segment.cloneSegment(curSeg, newSegSrcBegin, newSegSrcDuration, newSegTargetBeging, newSegTargetDuration);
            offset += ((end - begin) * (currentScale - 1));
            lastSegTargetEnd = newSegTargetBeging + newSegTargetDuration;

            newSegments.add((T) newSeg);

        }
        //merge emptyed segment
        ArrayList<T> scaledSegments = new ArrayList<>();

        if (newSegments.size() <= 1) {
            segments = newSegments;
            return;
        }

        for (int i = 0; i < newSegments.size(); ) {
            Segment segment = newSegments.get(i);
            if (!segment.isEmpty()) {
                scaledSegments.add((T) segment);
                i++;
                continue;
            }
            if (i == newSegments.size() - 1)
                break;
            for (int j = i + 1; j < newSegments.size(); j++) {
                Segment nextSegment = newSegments.get(j);
                if (!nextSegment.isEmpty()) {
                    scaledSegments.add((T) segment);
                    i = j;
                    break;
                }
                segment.getTimeMapping().getSourceTimeRange().stretch(nextSegment.getTimeMapping().getSourceTimeRange().getDuration().getSecond());
                segment.getTimeMapping().getTargetTimeRange().stretch(nextSegment.getTimeMapping().getTargetTimeRange().getDuration().getSecond());
            }
        }
        segments = scaledSegments;
    }

    private Segment findSegment(double begin, double end) {
        for (Segment seg : segments) {
            double segBegin = seg.getTimeMapping().getTargetTimeRange().getStartTime().getSecond();
            double segEnd = seg.getTimeMapping().getTargetTimeRange().getEnd().getSecond();
            if (segBegin - pading < begin && segEnd + pading > end) return seg;
        }
        return null;
    }

    private double findScale(ArrayList<TimeScaleModel> scaleModels, double begin, double end) {
        if (scaleModels == null || scaleModels.size() == 0)
            return 1.0d;


        for (int i = 0; i < scaleModels.size(); i++) {
            TimeScaleModel scale = scaleModels.get(i);
            TimeScaleModel next = null;
            if (i < scaleModels.size() - 1)
                next = scaleModels.get(i + 1);
            if (begin > scale.position() - pading) {
                if (next == null) return scale.getSpeedScale();
                else if (end < next.position() + pading) return scale.getSpeedScale();
            }
        }
        return 1.0d;

    }


    public MediaType getMediaType() {
        return mediaType;
    }

    public int getTrackId() {
        return trackId;
    }

    public void setTrackId(int trackId) {
        this.trackId = trackId;
    }

    public long getTimeScale() {
        return US_MUTIPLE;
    }

    public ArrayList<T> getSegments() {
        return segments;
    }

    public CMTime getDuration() {
        if (segments == null || segments.size() == 0) {
            return CMTime.zeroTime();
        }
        CMTimeMapping timeMapping = segments.get(segments.size() - 1).timeMapping;
        return timeMapping.getTargetTimeRange().getEnd();
    }

    public int getSurfaceTextrureid() {
        return surfaceTextrureid;
    }

    public void setSurfaceTextrureid(int surfaceTextrureid) {
        this.surfaceTextrureid = surfaceTextrureid;
    }

    public SurfaceTexture getSurfaceTexture() {
        return surfaceTexture;
    }

    public void setSurfaceTexture(SurfaceTexture surfaceTexture) {
        this.surfaceTexture = surfaceTexture;
    }

    public TrackType getTrackType() {
        return trackType;
    }

    public List<String> getPrettyLines() {

        char separator = '|';//trackType.getValue()%2==0?'|':'I';
        StringBuffer sb1 = new StringBuffer();
        StringBuffer sb2 = new StringBuffer();

        sb1.append(rightPad(String.valueOf(trackId), 4));
        sb2.append(repeat(' ', 4));

        sb1.append(rightPad(trackType.getName(), 8));
        sb2.append(rightPad(String.format("%.2f", getDuration().getSecond()), 8));

        sb1.append(rightPad(mediaType.getName(), 8));
        sb2.append(rightPad(String.valueOf(segments.size()), 4));
        sb2.append(rightPad(String.valueOf(surfaceTextrureid), 4));

        String line1 = sb1.toString();
        String line2 = sb2.toString();

        int maxLength = Math.max(line1.length(), line2.length());

        sb1 = new StringBuffer(rightPad(line1, maxLength + 2));
        sb2 = new StringBuffer(rightPad(line2, maxLength + 2));

        sb1.append(separator);
        sb2.append(separator);
        for (Segment segment : segments) {
            List<String> segmentLines = segment.prettyLines();
            sb1.append(segmentLines.get(0));
            sb2.append(segmentLines.get(1));
            sb1.append(separator);
            sb2.append(separator);
        }

        return Arrays.asList(new String[]{sb1.toString(), sb2.toString()});
    }

    @Override
    public String toString() {
        return "MediaTrack{" + "mediaType=" + mediaType + ", trackId=" + trackId + ", duration=" + duration + ", trackType=" + trackType + ", timeScale=" + US_MUTIPLE + ", segments=" + segments + ", surfaceTextrureid=" + surfaceTextrureid + ", surfaceTexture=" + surfaceTexture + ", decodeEos=" + decodeEos + '}';
    }

    public void prettyPrintLog() {
        List<String> lines = this.getPrettyLines();
        for (String line : lines) {
            Log.i(FormatUtils.generateStackTraceTag(TAG), line);
        }
    }

    public VideoSegment nextSegment(VideoSegment currentSegment) {
        if (currentSegment == null) return null;
        if (segments == null || segments.size() <= 1) return null;
        int index = segments.indexOf(currentSegment);
        if (index >= segments.size() - 1) return null;
        return (VideoSegment) segments.get(index + 1);
    }

    /**
     * 返回满足以下条件的segment的开始时间
     * <ul>
     * <li>decodePts时间段对应的segment=empty</li>
     * </ul>向后0.6秒对应的segment!=empty</>
     *
     * @param decodeTargetPts
     * @return 下一个segment的开始时间，-1,不满足以上条件
     */
    public long shouleBeWakeup(long decodeTargetPts, float preStartTime) {
        Segment currentSegment = this.getSegmentByUs(decodeTargetPts);
//        if (!Segment.isEmpty(currentSegment)) return -1;
        if (this.decodeEos) return -3;
        Segment nextSegment = this.getSegmentByUs(decodeTargetPts + (long) (preStartTime * US_MUTIPLE));

        if (Segment.isEmpty(nextSegment)) return -1;
        return nextSegment.getTimeMapping().getTargetTimeRange().getStartTime().getUs();
    }


    public boolean isLastSegment(long lastDecodPts) {
        if (lastDecodPts <= 0) return false;
        T lastSegment = segments.get((segments.size() - 1));
        if (lastDecodPts > lastSegment.getTimeMapping().getTargetTimeRange().getStartTime().getUs())
            return true;
        else return false;
    }

    public boolean isDecodeEos() {
        return decodeEos;
    }

    public void setDecodeEos(boolean decodeEos) {
        this.decodeEos = decodeEos;
    }

    public AudioMixInputParameter getInputParameter() {
        return inputParameter;
    }

    public void setInputParameter(AudioMixInputParameter inputParameter) {
        this.inputParameter = inputParameter;
        if (this.inputParameter == null) return;
        for (int i = 0; i < inputParameter.getParametersInternals().size(); i++) {
            ParametersInternal parametersInternal = inputParameter.getParametersInternals().get(i);
            for (int j = 0; j < segments.size(); j++) {
                Segment segment = segments.get(j);
                if (segment.getTimeMapping().getTargetTimeRange().getStartTime().getSecond() >= parametersInternal.getAtTime().getSecond()) {
                    segment.setVolume(parametersInternal.getVolume());
                }

            }
        }
        if (EditConstants.VERBOSE_A && this.mediaType == MediaType.MEDIA_TYPE_Audio) {
            for (Segment segment : segments) {
                Log.e("setInputParameter", "setInputParameter: " + segment.getVolume() + "   trckType " + trackType);
            }
        }

    }

    /**
     * segment 首+0.02 尾-0.1
     *
     * @param pts
     * @return
     */
    public boolean hasSegmentWithOffset(long pts) {
        if (pts < 0) return false;
        long offsetHeader = (long) (0.02 * US_MUTIPLE);
        long offsetTailer = (long) (0.1 * US_MUTIPLE);
        Segment tmpSegment = null;
        for (Segment segment : this.segments) {
            long startUs = segment.getTimeMapping().getTargetTimeRange().getStartTime().getUs();
            long endUs = segment.getTimeMapping().getTargetTimeRange().getEnd().getUs();
            if (pts >= startUs + offsetHeader && pts < endUs - offsetTailer) {
                return true;
            }
        }
        return false;
    }
}

package com.zp.libvideoedit.modle;

import android.util.Log;


import com.zp.libvideoedit.Time.CMTime;
import com.zp.libvideoedit.Time.CMTimeRange;
import com.zp.libvideoedit.utils.FormatUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.zp.libvideoedit.EditConstants.TAG;


/**
 * Created by gwd on 2018/3/8.
 */

public class MediaComposition {
    private ArrayList<MediaTrack> tracks;
    private int trackIdindex = 0;
    private CMTime duration = CMTime.zeroTime();
    private AudioMixParam audioMixParam;

    public MediaComposition() {
        super();
        tracks = new ArrayList<MediaTrack>();
    }

    public MediaTrack addTrack(MediaType type, TrackType trackType) {
        MediaTrack track = new MediaTrack(type, trackType);
        track.setTrackId(trackIdindex);
        tracks.add(track);
        trackIdindex++;
        return track;
    }

    public ArrayList<MediaTrack> trackOfType(MediaType type) {

        ArrayList<MediaTrack> typetracks = new ArrayList<MediaTrack>();
        for (MediaTrack track : tracks) {
            if (track.getMediaType() == type) {
                typetracks.add(track);
            }
        }
        return typetracks;
    }

    public MediaTrack trackOfTrackType(TrackType trackType) {
        MediaTrack mediaTrack = null;
        for (MediaTrack track : tracks) {
            if (track.getTrackType() == trackType) {
                mediaTrack = track;
                break;
            }
        }
        return mediaTrack;
    }

    public boolean removeTrackById(int mediaId) {
        boolean flag = false;
        for (MediaTrack track : tracks) {
            if (track.getTrackId() == mediaId) {
                tracks.remove(track);
                flag = true;
            }
        }
        if (flag) {
            return true;
        }
        return false;
    }

    public void removeTrack(MediaTrack mediaTrack) {
        for (MediaTrack track : tracks) {
            if (track == mediaTrack) {
                tracks.remove(mediaTrack);
                break;
            }
        }
    }

    public void removeAllTrack() {
        if (tracks != null) {
            tracks.clear();

        }
    }

    public MediaTrack longestVideoTrack() {
        MediaTrack mainMediaTrack = null, secMediaTrack = null;
        for (MediaTrack track : tracks) {
            if (track.getTrackType() == TrackType.TrackType_Video_Main) {
                mainMediaTrack = track;
            }
            if (track.getTrackType() == TrackType.TrackType_Video_Second) {
                secMediaTrack = track;
            }
        }
        if (mainMediaTrack != null && secMediaTrack != null) {
            if (mainMediaTrack.getDuration().getUs() > secMediaTrack.getDuration().getUs()) {
                return mainMediaTrack;
            } else return secMediaTrack;
        } else if (mainMediaTrack != null) {
            return mainMediaTrack;
        } else if (secMediaTrack != null) {
            return secMediaTrack;
        } else return null;

    }

    /**
     * 获取当前composition的最大时长
     *
     * @return
     */
    public CMTime getDuration() {
        CMTime tmpTime = CMTime.zeroTime();
        for (MediaTrack track : tracks) {
            Segment segment = null;
            CMTime end = null;
            if (track.getSegments().size() > 0) {
                segment = (Segment) track.getSegments().get(track.getSegments().size() - 1);
                CMTimeRange timeRange = segment.timeMapping.getTargetTimeRange();
                end = timeRange.getEnd();
            } else {
                end = CMTime.zeroTime();
            }
            if (CMTime.compare(end, tmpTime) > 0) {
                tmpTime = end;
            }
        }
        duration = tmpTime;
        return duration;
    }

    public CMTime getVideoDuration() {
        CMTime tmpTime = CMTime.zeroTime();
        for (MediaTrack track : tracks) {
            if (track.getTrackType() == TrackType.TrackType_Video_Main || track.getTrackType() == TrackType.TrackType_Video_Second) {
                Segment segment = null;
                CMTime end = null;
                if (track.getSegments().size() > 0) {
                    segment = (Segment) track.getSegments().get(track.getSegments().size() - 1);
                    CMTimeRange timeRange = segment.timeMapping.getTargetTimeRange();
                    end = timeRange.getEnd();
                } else {
                    end = CMTime.zeroTime();
                }
                if (CMTime.compare(end, tmpTime) > 0) {
                    tmpTime = end;
                }
            }
        }
        duration = tmpTime;
        return duration;
    }

    //获取当前composition的trcks
    public ArrayList<MediaTrack> getTracks() {
        return tracks;
    }


    public List<String> prettyLines() {
        List<String> lines = new ArrayList<>();
        String line1 = "MediaComposition size:" + tracks.size() + ", duration:" + duration.prettyString();
        lines.add(line1);
        ArrayList<MediaTrack> trackList = new ArrayList<>(tracks);
        Collections.sort(trackList, new Comparator<MediaTrack>() {
            @Override
            public int compare(MediaTrack o1, MediaTrack o2) {
                return o1.getTrackType().getValue() - o2.getTrackType().getValue();
            }
        });

        for (MediaTrack mediaTrack : trackList) {
            lines.addAll(mediaTrack.getPrettyLines());
            lines.add(" ");
        }
        return lines;
    }


    public void scaleTimeRanage(ArrayList<TimeScaleModel> timeScaleModels) {
        for (MediaTrack mediaTrack : tracks) {
            if (mediaTrack.getTrackType() == TrackType.TrackType_Video_Mask || mediaTrack.getTrackType() == TrackType.TrackType_Video_Mask_Ext
                    || mediaTrack.getTrackType() == TrackType.TrackType_Audio_BackGround) {
                continue;
            }
            mediaTrack.scaleTimeRange(timeScaleModels);
        }
    }

    public void prettyPrintLog() {
        prettyPrintLog(true);

    }

    public void prettyPrintLog(boolean infoOrError) {
        Log.i(TAG, this.toString());
        List<String> lines = this.prettyLines();
        for (String line : lines) {
            if (infoOrError)
                Log.i(FormatUtils.generateStackTraceTag(TAG), line);
            else
                Log.e(FormatUtils.generateStackTraceTag(TAG), line);
        }


    }

    public boolean isLongest(MediaTrack mediaTrack) {
        for (MediaTrack track : tracks) {
            if (track == mediaTrack) continue;
            if (track.getTrackType() == TrackType.TrackType_Video_Main || track.getTrackType() == TrackType.TrackType_Video_Second) {
                if (mediaTrack.getDuration().getUs() < track.getDuration().getUs()) {
                    return false;
                }
            }
        }
        return true;
    }

    public AudioMixParam getAudioMixParam() {
        return audioMixParam;
    }

    public void setAudioMixParam(AudioMixParam audioMixParam) {
        this.audioMixParam = audioMixParam;
        if (this.audioMixParam == null) return;

        for (AudioMixInputParameter inputParameter : audioMixParam.getInputParameters()) {
            MediaTrack mediaTrack = this.trackOfTrackType(inputParameter.getTrackType());
            if (mediaTrack != null)
                mediaTrack.setInputParameter(inputParameter);
        }
    }

    public void removeFromTime(CMTime atTime) {
        for (int i = 0; i < tracks.size(); i++) {
            MediaTrack mediaTrack = tracks.get(i);
            mediaTrack.removeFromTime(atTime);
        }
    }

    @Override
    public String toString() {
        return "MediaComposition{" + "tracks=" + tracks + ", trackIdindex=" + trackIdindex + ", duration=" + duration + '}';
    }

    public boolean maskHasPts(long decodePts) {
        for (MediaTrack mediaTrack : getTracks()) {
            if (mediaTrack.getTrackType() == TrackType.TrackType_Video_Mask || mediaTrack.getTrackType() == TrackType.TrackType_Video_Mask_Ext) {
                if (mediaTrack.hasSegmentWithOffset(decodePts))
                    return true;
            }
        }
        return false;
    }
}

package com.zp.libvideoedit.modle;


import com.zp.libvideoedit.Time.CMTimeMapping;

/**
 * Created by gwd on 2018/3/9.
 */

public class VideoSegment extends Segment {
    private VideoFile videoFile;

    public VideoSegment(boolean empty, VideoFile sourceVideoFile, int trackId, CMTimeMapping timeMapping, String segmentId){
        this.empty = empty;
        this.videoFile = sourceVideoFile;
        this.trackId = trackId;
        this.timeMapping = timeMapping;
        this.segmentId = segmentId;
        this.mediaType = MediaType.MEDIA_TYPE_Video;
    }

    public VideoFile getVideoFile() {
        return videoFile;
    }
    public String prettyString() {

        return videoFile.getFileName();
    }
    @Override
    public String getFullFileName() {
        if(videoFile!=null) return videoFile.getFilePath();
        else return "_";
    }
    @Override
    public String getFileName() {
        return videoFile.getFileName();
    }

    @Override
    public String toString() {
        return "VideoSegment{" +
                "videoFile=" + videoFile +
                ", empty=" + empty +
                ", trackId=" + trackId +
                ", segmentId='" + segmentId + '\'' +
                ", timeMapping=" + timeMapping +
                ", mediaType=" + mediaType +
                ", volume=" + volume +
                '}';
    }
}

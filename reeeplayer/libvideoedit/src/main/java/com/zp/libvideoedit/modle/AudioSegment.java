package com.zp.libvideoedit.modle;


import com.zp.libvideoedit.Time.CMTimeMapping;

/**
 * Created by gwd on 2018/3/9.
 */

public class AudioSegment extends Segment {
    public AudioFile audioFile;

    public AudioSegment(boolean empty, AudioFile sourceAudioFile, int trackId, CMTimeMapping timeMapping, String segmentId) {
        this.empty = empty;
        this.audioFile = sourceAudioFile;
        this.trackId = trackId;
        this.timeMapping = timeMapping;
        this.segmentId = segmentId;
        this.mediaType = MediaType.MEDIA_TYPE_Audio;

    }

    public AudioFile getAudioFile() {
        return audioFile;
    }

    @Override
    public String getFileName() {
        return audioFile.getFileName();
    }

    @Override
    public String getFullFileName() {
        if(audioFile!=null) return audioFile.getFilePath();
        else return "_";
    }

    @Override
    public String toString() {
        return "AudioSegment{" + "audioFile=" + (audioFile != null ? audioFile.getFilePath() : "null") + ", empty=" + empty + ", trackId=" + trackId + ", timeMapping=" + timeMapping + '}';
    }
}

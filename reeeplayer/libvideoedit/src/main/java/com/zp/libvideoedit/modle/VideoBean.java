package com.zp.libvideoedit.modle;

/**
 * Created by qin on 2018/8/1.
 */

public class VideoBean {

    private String path;
    private VideoFile videoFile;
    private AudioFile audioFile;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public VideoFile getVideoFile() {
        return videoFile;
    }

    public void setVideoFile(VideoFile videoFile) {
        this.videoFile = videoFile;
    }

    public AudioFile getAudioFile() {
        return audioFile;
    }

    public void setAudioFile(AudioFile audioFile) {
        this.audioFile = audioFile;
    }
}

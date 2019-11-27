package com.zp.libvideoedit.modle.script;


import com.zp.libvideoedit.Time.CMTimeRange;

import java.io.Serializable;

/**
 * Created by gwd on 2018/6/10.
 */

public class ScriptVideoModel implements Serializable {
    private String videoPath;
    private CMTimeRange insertTimeRange;
    private boolean reverseVideo;
    private String reverseVideoPath;

    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }

    public CMTimeRange getInsertTimeRange() {
        return insertTimeRange;
    }

    public void setInsertTimeRange(CMTimeRange insertTimeRange) {
        this.insertTimeRange = insertTimeRange;
    }

    public boolean isReverseVideo() {
        return reverseVideo;
    }

    public void setReverseVideo(boolean reverseVideo) {
        this.reverseVideo = reverseVideo;
    }

    public String getReverseVideoPath() {
        return reverseVideoPath;
    }

    public void setReverseVideoPath(String reverseVideoPath) {
        this.reverseVideoPath = reverseVideoPath;
    }

    /**
     * 复制对象
     *
     * @return
     */
    public ScriptVideoModel copyModel() {
        ScriptVideoModel scriptVideoModel = new ScriptVideoModel();
        scriptVideoModel.setVideoPath(this.videoPath);
        scriptVideoModel.setInsertTimeRange(this.insertTimeRange);
        scriptVideoModel.setReverseVideo(this.reverseVideo);
        scriptVideoModel.setReverseVideoPath(this.reverseVideoPath);
        return scriptVideoModel;
    }
}

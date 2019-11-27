package com.zp.libvideoedit.modle;

import com.zp.libvideoedit.Time.CMTime;
import com.zp.libvideoedit.Time.CMTimeRange;
import com.zp.libvideoedit.utils.StrUtils;
import com.zp.libvideoedit.utils.StringUtil;

import java.io.Serializable;
import java.util.List;

/**
 * Create by zp on 2019-11-26
 */
public class MusicModelBean implements Serializable {
    /**
     * auto_beat :
     * id : PY8
     * url : http://media.vnision.com/a09194d49392a4f3700048eebb3b3398.mp3
     * copyMusic : false
     * musicType : 0
     * title : Full Ripple Effect
     * type : 0
     * localResource : false
     * desc : Max Brodi
     * cover : 165156a0167c5b17ff0089cdbff8596b.jpg
     * duration : 00:32
     * selected : true
     * openAutoBeats : false
     * selectTimeRange :{{211081996190, 1000000000}, {32849432381, 1000000000}}
     */

    private String auto_beat;
    private String id;
    private String url;
    private boolean copyMusic;
    private int musicType;
    private String title;
    private int type;
    private boolean localResource;
    private String desc;
    private String cover;
    private String duration;
    private boolean selected;
    private boolean openAutoBeats;
    private String localMusicPath;
    private String selectTimeRange;
    private String fileName;


    public String getAuto_beat() {
        return auto_beat;
    }

    public void setAuto_beat(String auto_beat) {
        this.auto_beat = auto_beat;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public boolean isCopyMusic() {
        return copyMusic;
    }

    public void setCopyMusic(boolean copyMusic) {
        this.copyMusic = copyMusic;
    }

    public int getMusicType() {
        return musicType;
    }

    public void setMusicType(int musicType) {
        this.musicType = musicType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean isLocalResource() {
        return localResource;
    }

    public void setLocalResource(boolean localResource) {
        this.localResource = localResource;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isOpenAutoBeats() {
        return openAutoBeats;
    }

    public void setOpenAutoBeats(boolean openAutoBeats) {
        this.openAutoBeats = openAutoBeats;
    }

    public String getLoaclMusicPath() {
        return localMusicPath;
    }

    public void setLoaclMusicPath(String localMusicPath) {
        this.localMusicPath = localMusicPath;
    }

    public List<Long> getSelectTimeRange() {
        if (selectTimeRange == null) {
            return null;
        }
        return StrUtils.stringToLongs(selectTimeRange);
    }

    public CMTimeRange getCMTimeRange() {
        if (getSelectTimeRange() == null) {
            CMTime start = new CMTime(0, 1000);
            CMTime end = new CMTime(0, 1000);
            CMTimeRange timeRangevo = new CMTimeRange(start, end);
            return timeRangevo;
        } else {
            List<Long> timeRange = getSelectTimeRange();
            CMTime start = new CMTime(timeRange.get(0), timeRange.get(1));
            CMTime end = new CMTime(timeRange.get(2), timeRange.get(3));
            CMTimeRange timeRangevo = new CMTimeRange(start, end);
            return timeRangevo;
        }
    }

    public void setTSelectTimeRange(String selectTimeRange) {
        this.selectTimeRange = selectTimeRange;
    }

    public void setTSelectTimeRange(CMTimeRange selectTimeRange) {
        this.selectTimeRange = selectTimeRange != null ? StringUtil.stringFromCMTimeRange(selectTimeRange) : null;
    }

}

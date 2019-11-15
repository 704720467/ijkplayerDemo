package com.zp.libvideoedit.modle;


import com.zp.libvideoedit.Time.CMTimeRange;

/**
 * 贴纸模型
 * Created by zp on 2019/5/8.
 */

public class StickerModel {
    private CMTimeRange timeRange;
    private String picPath;//图片路径

    public StickerModel(CMTimeRange timeRange, String picPath) {
        this.timeRange = timeRange;
        this.picPath = picPath;
    }

    public CMTimeRange getTimeRange() {
        return timeRange;
    }

    public void setTimeRange(CMTimeRange timeRange) {
        this.timeRange = timeRange;
    }

    public String getPicPath() {
        return picPath;
    }

    public void setPicPath(String picPath) {
        this.picPath = picPath;
    }

//    public StickerModelVo getStickerModelVo() {
//        StickerModelVo stickerModelVo = new StickerModelVo();
//        stickerModelVo.setOrigonTimeRange(timeRange.timeRangeVo());
//        stickerModelVo.setPicPath(picPath);
//        return stickerModelVo;
//    }
}

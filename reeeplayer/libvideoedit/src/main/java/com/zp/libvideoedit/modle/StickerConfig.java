package com.zp.libvideoedit.modle;

import android.util.Log;

import com.zp.libvideoedit.Time.CMTime;

import java.util.List;


/**
 * 贴纸配置
 * Created by zp on 2019/5/8.
 */

public class StickerConfig {
    private float left;
    private float top;
    private float right;
    private float bottom;
    private float rotationAngle = 0f;//贴纸用 旋转角度
    private float scale = 1f;//贴纸用 缩放倍数

    private List<StickerModel> stickerModels;
    private float onceStickerDuration = -1;//一次贴纸的总时长

    /**
     * @param top
     * @param left
     * @param bottom
     * @param right
     * @param rotationAngle 旋转角度
     * @param scale         放大倍数
     * @param stickerModels 贴纸集合
     */

    public StickerConfig(float left, float top, float right, float bottom, float rotationAngle, float scale, List<StickerModel> stickerModels) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.rotationAngle = rotationAngle;
        this.scale = scale;
        this.stickerModels = stickerModels;
    }

    public float getTop() {
        return top;
    }

    public void setTop(float top) {
        this.top = top;
    }

    public float getLeft() {
        return left;
    }

    public void setLeft(float left) {
        this.left = left;
    }

    public float getBottom() {
        return bottom;
    }

    public void setBottom(float bottom) {
        this.bottom = bottom;
    }

    public float getRight() {
        return right;
    }

    public void setRight(float right) {
        this.right = right;
    }

    public float getRotationAngle() {
        return rotationAngle;
    }

    public void setRotationAngle(float rotationAngle) {
        this.rotationAngle = rotationAngle;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public List<StickerModel> getStickerModels() {
        return stickerModels;
    }

    public void setStickerModels(List<StickerModel> stickerModels) {
        this.stickerModels = stickerModels;
    }

    /**
     * 获取对应的贴图数据
     *
     * @param second 指定时间
     * @return
     */
    public StickerModel getStickerModelByTime(float second) {
        if (stickerModels == null || stickerModels.isEmpty()) return null;
        float adjustTime = second > getOnceStickerDuration() ? (second % getOnceStickerDuration()) : second;
        for (StickerModel model : stickerModels) {
            if (CMTime.getSecond(model.getTimeRange().getStartTime()) <= adjustTime && CMTime.getSecond(model.getTimeRange().getEnd()) > adjustTime) {
                return model;
            }
        }
        return null;
    }


//    public StickerConfigVo getStickerConfigVo() {
//        StickerConfigVo stickerModelVo = new StickerConfigVo();
//        stickerModelVo.setTop(top);
//        stickerModelVo.setLeft(left);
//        stickerModelVo.setBottom(bottom);
//        stickerModelVo.setRight(right);
//        stickerModelVo.setRotationAngle(rotationAngle);
//        stickerModelVo.setScale(scale);
//        try {
//            if (stickerModelVo.getStickerModelVos() == null)
//                stickerModelVo.setStickerModelVos(new RealmList<StickerModelVo>());
//            for (StickerModel stickerModel : stickerModels) {
//                stickerModelVo.getStickerModelVos().add(stickerModel.getStickerModelVo());
//            }
//        } catch (Exception e) {
//            Log.e("=====", e.getMessage());
//        }
//        return stickerModelVo;
//    }

    /**
     * 获取一次完整播放用时,只去计算一次
     *
     * @return
     */
    public float getOnceStickerDuration() {
        if (onceStickerDuration != -1) return onceStickerDuration;
        onceStickerDuration = 0;
        if (stickerModels == null && stickerModels.isEmpty()) return onceStickerDuration = 0;
        for (StickerModel stickerModel : stickerModels)
            onceStickerDuration += stickerModel.getTimeRange().getDuration().getSecond();
        return onceStickerDuration;
    }
}

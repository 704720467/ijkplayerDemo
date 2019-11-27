package com.zp.libvideoedit.modle.effectModel;

import android.graphics.Bitmap;

import com.zp.libvideoedit.GPUImage.Filter.VNIStickerFilter;
import com.zp.libvideoedit.GPUImage.Filter.VNiImageAlphaBlendFilter;
import com.zp.libvideoedit.GPUImage.Filter.VNiVideoBlendFilter;
import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageFilter;
import com.zp.libvideoedit.GPUImage.FilterCore.GPUImagePicture;
import com.zp.libvideoedit.Time.CMTimeRange;
import com.zp.libvideoedit.modle.Chunk;
import com.zp.libvideoedit.modle.StickerConfig;


/**
 * Created by gwd on 2018/4/28.
 */

public class EffectAdapter {
    private String effectId;
    private CMTimeRange timeRange;
    private GPUImageFilter filter;

    private Chunk maskVideoChunk;
    private Chunk maskExtVideoChunk;
    private EffectType effectType;
    private Bitmap bitmap;
    private GPUImagePicture picture;

    private StickerConfig stickerConfig;

    private int position = -1;

    private String specialEffectJson;
    private int sortPosition = -1;//用来排序的position

    public EffectAdapter(String effectId, EffectType effectType) {
        this.effectId = effectId;
        this.effectType = effectType;
        if (effectType == EffectType.EffectType_Pic) {
            this.filter = new VNiImageAlphaBlendFilter();
        } else if (effectType == EffectType.EffectType_Video) {
            this.filter = new VNiVideoBlendFilter();
        } else if (effectType == EffectType.EffectType_Sticker) {
            this.filter = new VNIStickerFilter();
        }
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        if (bitmap == null || this.bitmap == bitmap) return;
        this.bitmap = bitmap;
//        Bitmap newBitmap = Common.convert(this.bitmap, false, true, 0);
        if (picture == null)
            picture = new GPUImagePicture(bitmap);
        picture.setBitmap(bitmap);
        picture.reInit();
        picture.addTarget(filter, 1);
//        if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();

    }

    public boolean canSetBitMap() {
        if (bitmap == null || bitmap.isRecycled()) return true;
        if (picture == null || picture.getBitmap() == null || picture.getBitmap().isRecycled())
            return true;
        return false;
    }

    public void clearBitmap() {
        if (picture != null) {
            Bitmap bitmap = picture.getBitmap();
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
                picture.setBitmap(null);
            }
        }
    }

    public GPUImagePicture getPicture() {
        return picture;
    }

    public EffectType getEffectType() {
        return effectType;
    }

    public void setEffectType(EffectType effectType) {
        this.effectType = effectType;
    }

    public String getEffectId() {
        return effectId;
    }

    public CMTimeRange getTimeRange() {
        return timeRange;
    }

    public GPUImageFilter getFilter() {
        return filter;
    }

    public Chunk getMaskVideoChunk() {
        return maskVideoChunk;
    }

    public Chunk getMaskExtVideoChunk() {
        return maskExtVideoChunk;
    }

    public void setTimeRange(CMTimeRange timeRange) {
        this.timeRange = timeRange;
    }

    public void setFilter(GPUImageFilter filter) {
        this.filter = filter;
    }

    public void setMaskVideoChunk(Chunk maskVideoChunk) {
        this.maskVideoChunk = maskVideoChunk;
    }

    public void setMaskExtVideoChunk(Chunk maskExtVideoChunk) {
        this.maskExtVideoChunk = maskExtVideoChunk;
    }

    public void release() {
        if (this.filter != null) {
            this.filter.removeAllTargets();
            if (this.effectType == EffectType.EffectType_Pic) {
                this.filter = new VNiImageAlphaBlendFilter();
            } else if (this.effectType == EffectType.EffectType_Video) {
                this.filter = new VNiVideoBlendFilter();
            }
        }

    }

    public void setEffectId(String effectId) {
        this.effectId = effectId;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public StickerConfig getStickerConfig() {
        return stickerConfig;
    }

    public void setStickerConfig(StickerConfig stickerConfig) {
        this.stickerConfig = stickerConfig;
    }

    /**
     * 配置特效参数
     */
    public void configSpecialFilter(String specialEffectJson) {
        this.specialEffectJson = specialEffectJson;
    }

    public String getSpecialEffectJson() {
        return specialEffectJson;
    }

    public int getSortPosition() {
        return sortPosition;
    }

    public void setSortPosition(int sortPosition) {
        this.sortPosition = sortPosition;
    }
}

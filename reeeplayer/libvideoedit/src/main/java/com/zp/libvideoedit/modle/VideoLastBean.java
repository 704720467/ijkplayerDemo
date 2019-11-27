package com.zp.libvideoedit.modle;

import android.content.Context;
import android.graphics.Bitmap;

import com.zp.libvideoedit.GPUImage.Core.AndroidResourceManager;
import com.zp.libvideoedit.GPUImage.Core.EglCore;
import com.zp.libvideoedit.GPUImage.Core.OffscreenSurface;
import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageAlphaTwoPassInput;
import com.zp.libvideoedit.GPUImage.FilterCore.GPUImagePicture;
import com.zp.libvideoedit.utils.BitmapUtil;
import com.zp.libvideoedit.utils.Common;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ZC on 2018/6/26.
 */


public class VideoLastBean {
    private Bitmap nickName;
    private String bgBitmapPath;
    private Bitmap bgBitmap;
    private double[] bounds;
    private double[] center;
    private List<Bitmap> bitmaps;
    private ArrayList<Bitmap> dstBitmaps;
    private EglCore eglCore = null;
    private OffscreenSurface offscreenSurface;
    private GPUImageAlphaTwoPassInput alphaBlendFilter;
    private GPUImagePicture bgPicture = null;

    /**
     * @param context
     * @param nickName
     * @param bgBitmapPath
     * @param videDoirection 0竖屏 1横屏
     * @param bounds
     * @param center
     */
    public VideoLastBean(Context context, Bitmap nickName, String bgBitmapPath, int videDoirection, double[] bounds, double[] center) {
        this.nickName = nickName;
        this.bgBitmapPath = bgBitmapPath;
        this.bounds = bounds;
        this.center = center;
        this.bgBitmap = AndroidResourceManager.getAndroidResourceManager(context).readBitmapFromAssets((videDoirection == 0) ? "black.png" : "black_h.png");
        if (bgBitmapPath != null && bgBitmapPath.length() != 0) {
            Bitmap bitmap = BitmapUtil.loadFileToBitmap(bgBitmapPath);
            this.bgBitmap = bitmap;
        }
    }


    public Bitmap getNickName() {
        return nickName;
    }

    public Bitmap getNickNameBitmap() {
        return Common.convert(this.nickName, false, true, 0);
    }


    public void setNickName(Bitmap nickName) {
        this.nickName = nickName;
    }


    public String getBgBitmapPath() {
        return bgBitmapPath;
    }

    public void setBgBitmapPath(String bgBitmapPath) {
        this.bgBitmapPath = bgBitmapPath;
    }

    public Bitmap getBgBitmap() {
        return Common.convert(this.bgBitmap, false, true, 0);
    }

    public Bitmap createRendererdBitmap(Long pts, float duration) {

        return null;
    }

    public double[] getBounds() {
        return bounds;
    }

    public void setBounds(double[] bounds) {
        this.bounds = bounds;
    }

    public double[] getCenter() {
        return center;
    }

    public void setCenter(double[] center) {
        this.center = center;
    }
}

package com.zp.libvideoedit.GPUImage.Core;

/**
 * Created by gwd on 2018/2/7.
 */

public class GPUSize  {
    public int width ;
    public int  height ;
    /**
     *
     * @param width 宽
     * @param height 高
     */
    public GPUSize(int width, int  height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public String toString() {
        return "GPUSize{" +
                "width=" + width +
                ", height=" + height +
                '}';
    }
}

package com.zp.libvideoedit.GPUImage.Carma.Core;

import android.graphics.Bitmap;

/**
 * Created by gwd on 2018/3/5.
 */

public class FilterSelectModel {
    private String filterName;
    private Bitmap bitmap;
    private VNIFilterType type;
    private String lookupPath;

    public FilterSelectModel(String filterName, String lookupPath, VNIFilterType type) {
        this.filterName = filterName;
        this.lookupPath = lookupPath;
        this.type = type;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public String getFilterName() {
        return filterName;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public VNIFilterType getType() {
        return type;
    }

}

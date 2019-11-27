package com.zp.libvideoedit.modle;

import java.util.ArrayList;

/**
 * Create by zp on 2019-11-26
 */
public interface VideoCompositionCallBack {
    public void finishInitGLEnv(boolean isInited);

    public void finishedPlay();

    public void finishedRenderFilter(ArrayList<FilterCateModel> filterCateModels);
}

package com.zp.libvideoedit.Transcoder;

import android.graphics.Bitmap;

/**
 * Create by zp on 2019-11-18
 */
public interface TranscodeManagerCallback {
    /**
     * 生成缩略图回调
     *
     * @param thumb Bitmap 需要listener手动recycle
     * @param index
     * @param pts
     */
    public void onThumbGenerated(Bitmap thumb, int index, long pts);

    /**
     * 转码进度回调
     *
     * @param percent 导出完成百分比
     */
    public void onProgress(float percent);

    /**
     * 转码完成功成回调
     */
    public void OnSuccessed(String outPutFilePath);

    /**
     * 转码失败回调
     *
     * @param errmsg
     */
    public void onError(String errmsg);
}

package com.zp.libvideoedit.Transcoder;

import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaFormat;

/**
 * Create by zp on 2019-11-18
 */
public interface TranscodeListener extends TranscodeManagerCallback {

    public void validateParamsCallBack(int height, int width, int bitRate, int fps, long durationUs);

    public void encodeAudio();

    public void onVideoBufferAvailable(long l);

    public void onVideoDecoderDone();

    public void releaseEGLContext();

    /**
     * 设置GL当前上下文
     *
     * @param doSetEncoder
     */
    public void makeEGLContext(boolean doSetEncoder);
}

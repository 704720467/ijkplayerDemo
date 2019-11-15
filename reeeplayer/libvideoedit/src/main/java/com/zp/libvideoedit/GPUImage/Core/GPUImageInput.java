package com.zp.libvideoedit.GPUImage.Core;


import com.zp.libvideoedit.modle.ViewportRange;

/**
 * Created by gwd on 2018/2/8.
 */

public interface GPUImageInput {

    public void newFrameReadyAtTime(long frameTime, int textureIndex);

    public void setInputFramebuffer(GPUImageFrameBuffer newInputFramebuffer, int textureIndex);

    public int nextAvailableTextureIndex();

    public void setInputSize(GPUSize newSize, int index);

    public void setInputRotation(GPUImageRotationMode newInputRotation, int textureIndex);

    public GPUSize maximumOutputSize();

    public void endProcessing();

    public boolean shouldIgnoreUpdatesToThisTarget();

    public boolean enabled();

    public boolean wantsMonochromeInput();

    public void setCurrentlyReceivingMonochromeInput(boolean newValue);

    public void setViewportRange(ViewportRange viewportRange);
}

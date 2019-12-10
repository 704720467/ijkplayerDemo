package com.zp.libvideoedit.GPUImage.Filter;

import android.util.Log;

import com.zp.libvideoedit.EditConstants;
import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageTwoInput;
import com.zp.libvideoedit.Time.CMTime;

/**
 * Created by gwd on 2018/7/25.
 */

public class GPUImageTransitionFilter extends GPUImageTwoInput {
    protected float progress;
    protected int progressUniform;

    public GPUImageTransitionFilter(String fragmentShader) {
        super(fragmentShader);
    }

    public GPUImageTransitionFilter(String vertextShader, String fragmentShader) {
        super(vertextShader, fragmentShader);
    }

    public void setProgress(float progress) {

    }

    @Override
    public void newFrameReadyAtTime(long frameTime, int textureIndex) {
        if (EditConstants.VERBOSE_GL)
            Log.d("GPUImageTwoInput", "newFrameReadyAtTime  ,textureIndex:  " + textureIndex);
        currentFrameIndex = frameTime;
        currentTime = new CMTime(frameTime, EditConstants.NS_MUTIPLE);
        if (hasReceivedFirstFrame && hasReceivedSecondFrame) return;
        if (textureIndex == 0) {
            hasReceivedFirstFrame = true;
            if (secondFrameCheckDisabled) {
                hasReceivedSecondFrame = true;
            }
        } else {
            hasReceivedSecondFrame = true;
            if (firstFrameCheckDisabled) {
                hasReceivedFirstFrame = true;
            }
        }
        if ((hasReceivedFirstFrame && hasReceivedSecondFrame)) {
            renderAtTime(frameTime, textureIndex);
            hasReceivedFirstFrame = false;
            hasReceivedSecondFrame = false;

        }
    }
}

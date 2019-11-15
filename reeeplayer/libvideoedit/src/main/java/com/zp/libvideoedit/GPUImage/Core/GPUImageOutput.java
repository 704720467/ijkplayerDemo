package com.zp.libvideoedit.GPUImage.Core;

import android.graphics.Bitmap;
import android.util.Log;

import com.zp.libvideoedit.GPUImage.Carma.Core.TextureRotationUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by gwd on 2018/2/8.
 */

public class GPUImageOutput {


    protected GPUImageFrameBuffer mOutputFramebuffer = null;
    protected List<GPUImageInput> mTargets = null;
    protected List<Integer> mTargetTextureIndices = null;
    protected GPUSize mInputTextureSize = null;
    private GPUSize mCachedMaximumOutputSize = null;
    protected GPUSize mForcedMaximumSize = null;
    protected boolean mOverrideInputSize = false;
    private boolean mAllTargetsWantMonochromeData = false;
    private boolean mUsingNextFrameForImageCapture = false;

    private boolean mShouldSmoothlyScaleOutput = false;
    private boolean mShouldIgnoreUpdatesToThisTarget = false;
    private GPUImageInput mTargetToIgnoreForUpdates = null;

    private boolean mEnabled = false;
    protected GPUTextureOptions mOutputTextureOptions = null;
    private static String TAG = GPUImageOutput.class.getSimpleName();
    protected FloatBuffer mGLCubeBuffer;
    protected FloatBuffer mGLTextureBuffer;

    public GPUImageOutput() {
        mTargets = new ArrayList<GPUImageInput>();
        mTargetTextureIndices = new ArrayList<Integer>();
        mEnabled = true;
        mAllTargetsWantMonochromeData = true;
        mUsingNextFrameForImageCapture = false;
        mOutputTextureOptions = new GPUTextureOptions();

        mGLCubeBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLCubeBuffer.put(TextureRotationUtil.CUBE).position(0);

        mGLTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLTextureBuffer.put(TextureRotationUtil.getRotation(Rotation.NORMAL, false, true)).position(0);

    }

    public void setInputFramebufferForTarget(GPUImageInput target, int inputTextureIndex) {
        target.setInputFramebuffer(this.framebufferForOutput(), inputTextureIndex);
    }


    public GPUImageFrameBuffer framebufferForOutput() {
        return mOutputFramebuffer;
    }

    public void removeOutputFramebuffer() {
        mOutputFramebuffer = null;
    }

    public void notifyTargetsAboutNewOutputTexture() {
        for (int i = 0; i < mTargets.size(); ++i) {
            GPUImageInput currentTarget = mTargets.get(i);
            int textureIndex = mTargetTextureIndices.get(i).intValue();
            setInputFramebufferForTarget(currentTarget, textureIndex);
        }
    }

    public List<GPUImageInput> getTargets() {
        return mTargets;
    }

    public void addTarget(GPUImageInput newTarget) {
        int nextAvailableTextureIndex = newTarget.nextAvailableTextureIndex();
        addTarget(newTarget, nextAvailableTextureIndex);
        if (newTarget.shouldIgnoreUpdatesToThisTarget()) {
            mTargetToIgnoreForUpdates = newTarget;
        }

    }


    public void addTarget(final GPUImageInput newTarget, final int textureLocation) {
        if (newTarget == null) return;
        if (mTargets.contains(newTarget)) {
            return;
        }

        mCachedMaximumOutputSize = null;
//        runSynchronouslyOnVideoProcessingQueue(new Runnable() {
//            @Override
//            public void run() {
        setInputFramebufferForTarget(newTarget, textureLocation);
        mTargets.add(newTarget);
        mTargetTextureIndices.add(Integer.valueOf(textureLocation));
        mAllTargetsWantMonochromeData = mAllTargetsWantMonochromeData && newTarget.wantsMonochromeInput();
//            }
//        });
    }

    public void removeTarget(final GPUImageInput targetToRemove) {
        if (!mTargets.contains(targetToRemove)) {
            return;
        }

        if (mTargetToIgnoreForUpdates == targetToRemove) {
            mTargetToIgnoreForUpdates = null;
        }

        mCachedMaximumOutputSize = null;

        final int finalIndex = mTargets.indexOf(targetToRemove);
        final int textureIndexOfTarget = mTargetTextureIndices.get(finalIndex).intValue();
//        runSynchronouslyOnVideoProcessingQueue(new Runnable() {
//            @Override
//            public void run() {
        targetToRemove.setInputSize(null, textureIndexOfTarget);
        targetToRemove.setInputRotation(GPUImageRotationMode.kGPUImageNoRotation, textureIndexOfTarget);
        mTargetTextureIndices.remove(finalIndex);
        mTargets.remove(targetToRemove);
        targetToRemove.endProcessing();
//            }
//        });
    }

    public void removeAllTargets() {
        mCachedMaximumOutputSize = null;
        runSynchronouslyOnVideoProcessingQueue(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < mTargets.size(); ++i) {
                    GPUImageInput targetToRemove = mTargets.get(i);

                    int textureIndexOfTarget = mTargetTextureIndices.get(i).intValue();

                    targetToRemove.setInputSize(new GPUSize(0, 0), textureIndexOfTarget);
                    targetToRemove.setInputRotation(GPUImageRotationMode.kGPUImageNoRotation, textureIndexOfTarget);
                }
                mTargets.clear();
                mTargetTextureIndices.clear();
                mAllTargetsWantMonochromeData = true;
            }
        });
    }


    public void forceProcessingAtSize(GPUSize frameSize) {

    }

    public void forceProcessingAtSizeRespectingAspectRatio(GPUSize frameSize) {

    }

    public void useNextFrameForImageCapture() {

    }

    public Bitmap newBitMapFromCurrentlyProcessedOutput() {
        if (mOutputFramebuffer != null) {
            return this.mOutputFramebuffer.newBitMapFromFramebufferContents();
        }
        return null;
    }

    public Bitmap newBitMapByFilteringCGImage(Bitmap imageToFilter) {
        return null;
    }

    public boolean providesMonochromeOutput() {
        return false;
    }

    public Bitmap imageFromCurrentFramebuffer() {
        return null;
    }

    public Bitmap imageFromCurrentFramebufferWithOrientation(GPUImageOrientation imageOrientation) {
        return null;
    }

    public Bitmap imageByFilteringImage(Bitmap imageToFilter) {
        return null;
    }

    public static void runOnMainQueueWithoutDeadlocking(Runnable runnable) {
        runnable.run();
//        if (AndroidDispatchQueue.isMainThread()) {
//            runnable.run();
//        }else {
//            AndroidDispatchQueue.getMainDispatchQueue().dispatchSync(runnable);
//        }
    }

    public static void runSynchronouslyOnVideoProcessingQueue(Runnable runnable) {
        runnable.run();
//        AndroidDispatchQueue dispatchQueue = GPUImageContext.sharedImageProcessingContexts().getContextQueue();
//        if (AndroidDispatchQueue.isSameDispatchQueue(dispatchQueue)) {
//            runnable.run();
//        }else {
//            dispatchQueue.dispatchSync(runnable);
//        }
    }

    public static void runAsynchronouslyOnVideoProcessingQueue(Runnable runnable) {
        runnable.run();
//        AndroidDispatchQueue dispatchQueue = GPUImageContext.sharedImageProcessingContexts().getContextQueue();
//        if (AndroidDispatchQueue.isSameDispatchQueue(dispatchQueue)) {
//            runnable.run();
//        }else {
//            dispatchQueue.dispatchAsync(runnable);
//        }
    }

    public static void runSynchronouslyOnContextQueue(GPUImageContext context, Runnable runnable) {
//        AndroidDispatchQueue dispatchQueue = context.getContextQueue();
//        if (AndroidDispatchQueue.isSameDispatchQueue(dispatchQueue)) {
//            runnable.run();
//        }else {
//            dispatchQueue.dispatchSync(runnable);
//        }
        runnable.run();
    }

    public static void runAsynchronouslyOnContextQueue(GPUImageContext context, Runnable runnable) {
        runnable.run();
//        AndroidDispatchQueue dispatchQueue = context.getContextQueue();
//        if (AndroidDispatchQueue.isSameDispatchQueue(dispatchQueue)) {
//            runnable.run();
//        }else {
//            dispatchQueue.dispatchAsync(runnable);
//        }
    }

    public static void reportAvailableMemoryForGPUImage(String tag) {

    }


    public String dumpTargets(GPUImageOutput output, String dumpString) {
        GPUImageOutput outtmp = output;
        String string = "";
        if (dumpString == null) {
            dumpString = "";
        }
        List<GPUImageInput> filters = outtmp.getTargets();
        int maxIndex = filters.size() - 1;
        for (int i = 0; i < filters.size(); i++) {
            GPUImageInput input = filters.get(i);
            String name = "";
            boolean last = (i == maxIndex);
            if (input instanceof GPUImageOutput) {
                outtmp = (GPUImageOutput) filters.get(i);
                name = outtmp.toString();
            } else {
                GPUImageInput inputtmp = (GPUImageInput) filters.get(i);
                name = inputtmp.toString();
                outtmp = null;
            }
            string  = string +(dumpString+ String.format("%s%s\n",((last)?"└" : "├"),name));
            if (outtmp != null && outtmp instanceof GPUImageOutput) {
                string += dumpTargets(outtmp, String.format("%s%s",dumpString,(last)?"    ":"|  "));
            }
        }
        return string;
    }

    public void dump() {
        String dumpstr = dumpTargets(this, "");
        Log.e("dumpTargets", String.format("%s\n%s", this.getClass().getSimpleName(), dumpstr));
    }


}


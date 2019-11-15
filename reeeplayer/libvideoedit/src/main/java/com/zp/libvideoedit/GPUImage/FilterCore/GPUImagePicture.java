package com.zp.libvideoedit.GPUImage.FilterCore;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import com.zp.libvideoedit.GPUImage.Core.AndroidResourceManager;
import com.zp.libvideoedit.GPUImage.Core.GPUImageContext;
import com.zp.libvideoedit.GPUImage.Core.GPUImageFrameBuffer;
import com.zp.libvideoedit.GPUImage.Core.GPUImageInput;
import com.zp.libvideoedit.GPUImage.Core.GPUImageOutput;
import com.zp.libvideoedit.GPUImage.Core.GPUSize;

import java.util.concurrent.Semaphore;

/**
 * Created by gwd on 2018/2/9.
 */

public class GPUImagePicture extends GPUImageOutput {

    private Bitmap bitmap;

    private GPUSize mPixelSizeOfImage = null;
    private boolean mHasProcessedImage = false;
    private Semaphore mImageUpdateSemaphore = null;
    private boolean isInited = false;


    public GPUImagePicture(final String filePath, Context context) {

        this(AndroidResourceManager.getAndroidResourceManager(context).readBitmapFromAssets(filePath));
    }

    public GPUImagePicture(final Bitmap bitmap) {

        mHasProcessedImage = false;
        mImageUpdateSemaphore = new Semaphore(0);
        mImageUpdateSemaphore.release();
        this.bitmap = bitmap;
        int widthOfImage = bitmap.getWidth();
        int heightOfImage = bitmap.getHeight();
        mPixelSizeOfImage = new GPUSize(widthOfImage, heightOfImage);
    }

    public void reInit() {
        isInited = false;
    }

    public void init() {
        if (!isInited) {
            mPixelSizeOfImage = new GPUSize(mPixelSizeOfImage.width, mPixelSizeOfImage.height);
            if (mOutputFramebuffer != null) {
                release();
                mOutputFramebuffer.destoryFramebuffer();
            }
            mOutputFramebuffer = new GPUImageFrameBuffer(mPixelSizeOfImage, this.mOutputTextureOptions, true); // GPUImageContext.sharedFramebufferCache().fetchFramebufferForSize(mPixelSizeOfImage, true);
            mOutputFramebuffer.disableReferenceCounting();
            mOutputFramebuffer.activeFramebuffer();
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mOutputFramebuffer.getTexture());
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
//            if (!bitmap.isRecycled()) {
//                bitmap.recycle();
//            }
            GLES20.glFlush();
            isInited = true;
        }
    }

    public void processImage() {
        processImageWithCompletionHandler(0);
    }

    public void processImage(long currentTime) {
        processImageWithCompletionHandler(currentTime);
    }

    public GPUSize outputImageSize() {
        return mPixelSizeOfImage;

    }

    private boolean processImageWithCompletionHandler(long currentTime) {
        mHasProcessedImage = true;

        try {
            mImageUpdateSemaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

//        GPUImageOutput.runAsynchronouslyOnVideoProcessingQueue(new Runnable(){
//            @Override
//            public void run() {
        for (int i = 0; i < mTargets.size(); ++i) {
            GPUImageInput currentTarget = mTargets.get(i);
            int textureIndexOfTarget = mTargetTextureIndices.get(i).intValue();

            currentTarget.setCurrentlyReceivingMonochromeInput(false);
            currentTarget.setInputSize(mPixelSizeOfImage, textureIndexOfTarget);
            currentTarget.setInputFramebuffer(mOutputFramebuffer, textureIndexOfTarget);
            currentTarget.newFrameReadyAtTime(currentTime, textureIndexOfTarget);
        }

        mImageUpdateSemaphore.release();
//
//        if (runnable != null) {
//            runnable.run();
//        }
////            }
////        });

        return true;
    }

    public void release() {
        if (mOutputFramebuffer != null) {
            mOutputFramebuffer.enableReferenceCounting();
            mOutputFramebuffer.unlock();
            GPUImageContext.sharedFramebufferCache().returnFramebufferToCache(mOutputFramebuffer);
        }
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }
}

package com.zp.libvideoedit.GPUImage.Core;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.util.Log;

import java.nio.IntBuffer;


/**
 * Created by gwd on 2018/2/7.
 */

public class GPUImageFrameBuffer {

    private GPUTextureOptions defaultOpetion = new GPUTextureOptions();
    private GPUSize textureSize;
    private int[] texture;
    private int[] frameBuffer;
    private boolean isMissingFramebuffer;
    int framebufferReferenceCount;
    boolean referenceCountingDisabled;


    public GPUImageFrameBuffer(int textureid, GPUSize size) {
        this.texture = new int[1];
        this.texture[0] = textureid;
        this.textureSize = size;
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, this.texture[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, defaultOpetion.getMinFilter());
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, defaultOpetion.getMagFilter());
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, defaultOpetion.getWrapS());
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, defaultOpetion.getWrapT());
    }

    /**
     * @param textureSize 纹理大小
     * @param options     纹理参数
     * @param onlyTexture 是否只创建纹理对象
     */
    public GPUImageFrameBuffer(GPUSize textureSize, GPUTextureOptions options, boolean onlyTexture) {
        this.textureSize = textureSize;
        this.defaultOpetion = options;
        this.isMissingFramebuffer = onlyTexture;
        framebufferReferenceCount = 0;
        referenceCountingDisabled = false;
        //create frame buffer
        if (onlyTexture) {
            GPUImageOutput.runSynchronouslyOnVideoProcessingQueue(new Runnable() {
                @Override
                public void run() {
//                    GPUImageContext.sharedImageProcessingContexts().useAsCurrentContext();
                    generateTexture();
                    if (frameBuffer != null) {
                        frameBuffer[0] = 0;
                    }

                }
            });
        } else {
            generateFramebuffer();
            int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
            if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
                Log.e("GLError", "frame buffer error code  " + status);
            }
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    /**
     * 生成纹理对象
     */
    private void generateTexture() {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        this.texture = new int[1];
        GLES20.glGenTextures(1, this.texture, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, this.texture[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, defaultOpetion.getMinFilter());
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, defaultOpetion.getMagFilter());
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, defaultOpetion.getWrapS());
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, defaultOpetion.getWrapT());

    }

    private void generateFramebuffer() {
        frameBuffer = new int[1];
        GLES20.glGenFramebuffers(1, frameBuffer, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[0]);
        generateTexture();
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, defaultOpetion.getInternalFormat(), textureSize.width, textureSize.height, 0, defaultOpetion.getFormat(), defaultOpetion.getType(), null);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, texture[0], 0);

    }

    public void destoryFramebuffer() {
        if (frameBuffer != null && frameBuffer[0] != 0) {
            GLES20.glDeleteFramebuffers(1, frameBuffer, 0);
            frameBuffer[0] = 0;
        }
        if (texture != null && texture[0] != 0) {
            GLES20.glDeleteTextures(1, texture, 0);
            texture[0] = 0;
        }
    }


    public void activeFramebuffer() {
        if (this.frameBuffer != null) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, this.frameBuffer[0]);
        }
        GLES20.glViewport(0, 0, textureSize.width, textureSize.height);
    }


    public GPUSize getTextureSize() {
        return textureSize;
    }

    public GPUTextureOptions getDefaultOpetion() {
        return defaultOpetion;
    }

    public boolean isMissingFramebuffer() {
        return isMissingFramebuffer;
    }

    public int getTexture() {
        return this.texture[0];
    }


    public Bitmap newBitMapFromFramebufferContents() {
        this.activeFramebuffer();
        IntBuffer ib = IntBuffer.allocate(textureSize.width * textureSize.height * 4);
        GLES20.glReadPixels(0, 0, textureSize.width, textureSize.height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib);
        Bitmap result = Bitmap.createBitmap(textureSize.width, textureSize.height, Bitmap.Config.ARGB_8888);
        result.copyPixelsFromBuffer(IntBuffer.wrap(ib.array()));
        this.unlock();
        return result;
    }


    public void lock() {
        if (referenceCountingDisabled) {
            return;
        }

        framebufferReferenceCount++;
    }

    public void unlock() {
        if (referenceCountingDisabled) {
            return;
        }
        framebufferReferenceCount--;
        if (framebufferReferenceCount < 1) {
            GPUImageContext.sharedFramebufferCache().returnFramebufferToCache(this);
        }
    }

    public void clearAllLocks() {
        framebufferReferenceCount = 0;
    }

    public void disableReferenceCounting() {
        referenceCountingDisabled = true;
    }

    public void enableReferenceCounting() {
        referenceCountingDisabled = false;
    }
}

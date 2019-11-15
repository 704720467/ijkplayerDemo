package com.zp.libvideoedit.GPUImage.Core;

import android.opengl.GLES20;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.util.HashMap;
import java.util.Map;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

/**
 * Created by gwd on 2018/2/7.
 */

public class GPUImageContext {

    private static GPUImageContext mImageProcessingContexts = null;
    private static SurfaceHolder mSurfaceHolder = null;
    private AndroidDispatchQueue mContextQueue = null;
    private GLProgram mCurrentShaderProgram = null;
    private Map<String, GLProgram> mShaderProgramCache = null;
    private GPUImageFramebufferCache mFramebufferCache = null;
    private GPUImageEGLContext mEGLContext = null;
    private static GPUImageEGLContext mCurrentEGLContext = null;
    private static Surface viewSurface;


    private GPUImageContext() {
        mContextQueue = AndroidDispatchQueue.dispatchQueueCreate();
        mShaderProgramCache = new HashMap<String, GLProgram>();
    }

    public AndroidDispatchQueue getContextQueue() {
        return mContextQueue;
    }

    public void destroyContext() {
        if (mContextQueue != null)
            AndroidDispatchQueue.dispatchQueueDestroy(mContextQueue);
    }

    public static synchronized GPUImageContext sharedImageProcessingContexts() {
        if (mImageProcessingContexts == null) {
            mImageProcessingContexts = new GPUImageContext();
        }
        return mImageProcessingContexts;
    }

    public static void useImageProcessingContext() {
        GPUImageContext.sharedImageProcessingContexts().useAsCurrentContext();
    }

    public static void unUseImageProcessingContext() {
        GPUImageContext.sharedImageProcessingContexts().unUseAsCurrentContext();
    }

    public static GPUImageFramebufferCache sharedFramebufferCache() {
        return GPUImageContext.sharedImageProcessingContexts().framebufferCache();
    }

    public static void setActiveShaderProgram(GLProgram shaderProgram) {
        GPUImageContext sharedContext = GPUImageContext.sharedImageProcessingContexts();
        sharedContext.setContextShaderProgram(shaderProgram);
    }

    public void setmSurfaceHolder(SurfaceHolder holder) {
        mSurfaceHolder = holder;
    }

    public static void setViewSurface(Surface viewSurface) {
        GPUImageContext.viewSurface = viewSurface;
    }

    public void useAsCurrentContext() {
        GPUImageEGLContext imageProcessingContext = getEGLContext();

        if (mCurrentEGLContext != imageProcessingContext) {
            mCurrentEGLContext = imageProcessingContext;
            mCurrentEGLContext.setCurrentContext();
        }
    }

    public void unUseAsCurrentContext() {
        GPUImageEGLContext imageProcessingContext = getEGLContext();

        if (mCurrentEGLContext == imageProcessingContext) {
            imageProcessingContext.unSetCurrentContext();
            mCurrentEGLContext = null;
        }
    }

    public void presentBufferForDisplay() {
        mEGLContext.presentRenderBuffer();
    }

    public void setContextShaderProgram(GLProgram shaderProgram) {

        if (mCurrentShaderProgram != shaderProgram) {
            mCurrentShaderProgram = shaderProgram;
            shaderProgram.use();
        }
    }

    public GPUImageFramebufferCache framebufferCache() {
        if (mFramebufferCache == null) {
            mFramebufferCache = new GPUImageFramebufferCache();
        }
        return mFramebufferCache;
    }


    public GLProgram programForVertexShaderStringFragmentShaderString(String vertexShaderString, String fragmentShaderString) {

        String lookupKeyForShaderProgram = "V: ";
        lookupKeyForShaderProgram += vertexShaderString;
        lookupKeyForShaderProgram += " - ";
        lookupKeyForShaderProgram += "F: ";
        lookupKeyForShaderProgram += fragmentShaderString;

        GLProgram programFromCache = mShaderProgramCache.get(lookupKeyForShaderProgram);
        if (programFromCache == null) {
            programFromCache = new GLProgram().initWithVertexVShaderStringFShaderString(vertexShaderString, fragmentShaderString);
            mShaderProgramCache.put(lookupKeyForShaderProgram, programFromCache);
        }
        return programFromCache;
    }

    public void destroyProgrames() {
        if (mShaderProgramCache != null) {
            mShaderProgramCache.clear();
        }
    }


    public GPUImageEGLContext getEGLContext() {
        if (mEGLContext == null) {
            mEGLContext = new GPUImageEGLContext().init();

            mEGLContext.setCurrentContext();
            GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        }
        return mEGLContext;
    }

    public static class GPUImageEGLContext {
        private static final String TAG = GPUImageEGLContext.class.getSimpleName();

        private EGLContext mGLContext = null;
        private EGLDisplay mDisplay = null;
        private EGLSurface mSurface = null;
        private EGL10 mEGL = null;
        private EglCore mEglCore = null;
        private WindowSurface windowSurface;
        private OffscreenSurface offScreenSurface;

        private int[] configSpec = {EGL10.EGL_RED_SIZE, 5,
                EGL10.EGL_GREEN_SIZE, 6,
                EGL10.EGL_BLUE_SIZE, 5,
                EGL10.EGL_DEPTH_SIZE, 16,
                EGL10.EGL_NONE};

        public GPUImageEGLContext init() {

            //mEglCore = new EglCore(null, EglCore.FLAG_RECORDABLE);
//            windowSurface = new WindowSurface(mEglCore,viewSurface,false);

            //offScreenSurface = new OffscreenSurface(mEglCore, 1920, 1200);

            return this;
        }

        public void setCurrentContext() {
//            windowSurface.makeCurrent();
//            offScreenSurface.makeCurrent();
//            mCurrentEGLContext = this;
        }

        public void unSetCurrentContext() {
//            mEGL.eglMakeCurrent(mDisplay,
//                    EGL10.EGL_NO_SURFACE,
//                    EGL10.EGL_NO_SURFACE,
//                    EGL10.EGL_NO_CONTEXT);
        }

        public void presentRenderBuffer() {
//            windowSurface.swapBuffers();
//            offScreenSurface.swapBuffers();
        }

    }


}

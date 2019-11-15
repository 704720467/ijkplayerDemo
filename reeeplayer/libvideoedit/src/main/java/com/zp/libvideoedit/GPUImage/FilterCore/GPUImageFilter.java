package com.zp.libvideoedit.GPUImage.FilterCore;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.opengl.GLES20;
import android.util.Log;

import com.zp.libvideoedit.Constants;
import com.zp.libvideoedit.GPUImage.Core.GLProgram;
import com.zp.libvideoedit.GPUImage.Core.GPUImageContext;
import com.zp.libvideoedit.GPUImage.Core.GPUImageFrameBuffer;
import com.zp.libvideoedit.GPUImage.Core.GPUImageInput;
import com.zp.libvideoedit.GPUImage.Core.GPUImageOutput;
import com.zp.libvideoedit.GPUImage.Core.GPUImageRotationMode;
import com.zp.libvideoedit.GPUImage.Core.GPUImageTextureCoordinates;
import com.zp.libvideoedit.GPUImage.Core.GPUSize;
import com.zp.libvideoedit.GPUImage.Core.GPUUtiles;
import com.zp.libvideoedit.Time.CMTime;
import com.zp.libvideoedit.modle.ViewportRange;
import com.zp.libvideoedit.utils.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by gwd on 2018/2/8.
 */

public class GPUImageFilter extends GPUImageOutput implements GPUImageInput {

    public static final String TAG = GPUImageFilter.class.getSimpleName();
    protected GPUImageFrameBuffer mFirstInputFramebuffer = null;
    protected GLProgram mFilterProgram = null;
    protected int mFilterPositionAttribute = -1;
    protected int mFilterTextureCoordinateAttribute = -1;
    protected int mFilterInputTextureUniform = -1;
    protected float mBackgroundColorRed = 0.0f;
    protected float mBackgroundColorGreen = 0.0f;
    protected float mBackgroundColorBlue = 0.0f;
    protected float mBackgroundColorAlpha = 0.0f;
    protected boolean mIsEndProcessing = false;
    protected GPUImageRotationMode mInputRotation = null;
    protected boolean mCurrentlyReceivingMonochromeInput = false;
    protected Map<Integer, Runnable> mUniformStateRestorationBlocks = null;
    protected boolean mPreventRendering = false;
    public long currentFrameIndex = 0;
    protected String vertextShaderString;
    protected String fragmentShaderString;
    protected int surfaceWidth;
    protected int surfaceHeight;
    protected boolean filterInited;
    protected boolean usingNextFrameForImageCapture = false;
    protected CMTime currentTime;
    protected CMTime transitionStart;
    protected CMTime transitionEnd;
    protected ViewportRange viewportRange;

    public static final String kGPUImageVertexShaderString = "" +
            "attribute vec4 position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            " \n" +
            "varying vec2 textureCoordinate;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = position;\n" +
            "    textureCoordinate = inputTextureCoordinate.xy;\n" +
            "}";
    public static final String kGPUImagePassthroughFragmentShaderString = "" +
            "varying highp vec2 textureCoordinate;\n" +
            " \n" +
            "uniform sampler2D inputImageTexture;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "     gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "}";

    public GPUImageFilter() {
        this(kGPUImageVertexShaderString, kGPUImagePassthroughFragmentShaderString);
    }

    public GPUImageFilter(final String vertexShaderString, final String fragmentShaderString) {
        super();
        this.vertextShaderString = vertexShaderString;
        this.fragmentShaderString = fragmentShaderString;
    }

    protected int getProgram() {
        if (mFilterProgram != null) {
            return mFilterProgram.getmProgram();
        }
        return -1;
    }

    public void init() {
        if (!filterInited) {
            mUniformStateRestorationBlocks = new HashMap<Integer, Runnable>();
            mPreventRendering = false;
            mCurrentlyReceivingMonochromeInput = false;
            mInputRotation = GPUImageRotationMode.kGPUImageNoRotation;
            mBackgroundColorRed = 0.0f;
            mBackgroundColorGreen = 0.0f;
            mBackgroundColorBlue = 0.0f;
            mBackgroundColorAlpha = 0.0f;
            mFilterProgram = new GLProgram().initWithVertexVShaderStringFShaderString(this.vertextShaderString, this.fragmentShaderString);
            initializeAttributes();
            if (!mFilterProgram.link()) {
                Log.e(TAG, "Program link log: " + mFilterProgram.getmProgramLog());
                Log.e(TAG, "Fragment shader compile log: " + mFilterProgram.getmFragmentShaderLog());
                Log.e(TAG, "Vertex shader compile log: " + mFilterProgram.getmVertexShaderLog());
                mFilterProgram = null;
            }
            mFilterPositionAttribute = mFilterProgram.attributeIndex("position");
            mFilterTextureCoordinateAttribute = mFilterProgram.attributeIndex("inputTextureCoordinate");
            mFilterInputTextureUniform = mFilterProgram.uniformIndex("inputImageTexture");
            GPUImageContext.setActiveShaderProgram(mFilterProgram);
            GLES20.glEnableVertexAttribArray(mFilterPositionAttribute);
            GLES20.glEnableVertexAttribArray(mFilterTextureCoordinateAttribute);
            filterInited = true;
        } else {
            Log.e("GPUImageFilter ", "GPUImageFilter init Error!");
        }
    }

    public boolean isFilterInited() {
        return filterInited;
    }

    public GPUImageFilter(String fragmentShaderString) {
        this(kGPUImageVertexShaderString, fragmentShaderString);
    }

    protected void initializeAttributes() {
        mFilterProgram.addAttribute("position");
        mFilterProgram.addAttribute("inputTextureCoordinate");
    }

    public void setupFilterForSize(GPUSize filterFrameSize) {

    }


    public GPUSize rotatedSize(GPUSize sizeToRotate, int textureIndex) {
        GPUSize rotatedSize = sizeToRotate;

        return rotatedSize;
    }

    public GPUSize rotatedPoint(GPUSize pointToRotate, GPUImageRotationMode rotation) {
        return null;
    }

    public GPUSize sizeOfFBO() {
        GPUSize outputSize = maximumOutputSize();
        if (outputSize == null || outputSize.width < mInputTextureSize.width) {
            return mInputTextureSize;
        }
        return outputSize;
    }

    public static FloatBuffer textureCoordinatesForRotation(GPUImageRotationMode rotationMode) {
        switch (rotationMode) {
            case kGPUImageNoRotation:
                return GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.noRotationTextureCoordinates);
            case kGPUImageRotateLeft:
                return GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.rotateLeftTextureCoordinates);
            case kGPUImageRotateRight:
                return GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.rotateRightTextureCoordinates);
            case kGPUImageFlipVertical:
                return GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.verticalFlipTextureCoordinates);
            case kGPUImageFlipHorizonal:
                return GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.horizontalFlipTextureCoordinates);
            case kGPUImageRotateRightFlipVertical:
                return GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.rotateRightVerticalFlipTextureCoordinates);
            case kGPUImageRotateRightFlipHorizontal:
                return GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.rotateRightHorizontalFlipTextureCoordinates);
            case kGPUImageRotate180:
                return GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.rotate180TextureCoordinates);
        }
        return GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.noRotationTextureCoordinates);
    }


    public void forceProcessingAtSize(GPUSize frameSize) {
        if (frameSize == null) {
            mOverrideInputSize = false;
        } else {
            mOverrideInputSize = true;
            mInputTextureSize = frameSize;
            mForcedMaximumSize = null;
        }
    }

    public void forceProcessingAtSizeRespectingAspectRatio(GPUSize frameSize) {
        if (frameSize == null) {
            mOverrideInputSize = false;
            mInputTextureSize = null;
            mForcedMaximumSize = null;
        } else {
            mOverrideInputSize = true;
            mForcedMaximumSize = frameSize;
        }
    }

    public void renderToTextureWithVertices(FloatBuffer vertices, FloatBuffer textureCoordinaes, long frameIndex) {
        mOutputFramebuffer = GPUImageContext.sharedFramebufferCache().fetchFramebufferForSize(sizeOfFBO(), false);
        mOutputFramebuffer.activeFramebuffer();
        mFilterProgram.use();
        if (usingNextFrameForImageCapture) {
            mOutputFramebuffer.lock();
        }
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        if (!mFilterProgram.ismInitialized()) {
            return;
        }
        GLES20.glVertexAttribPointer(mFilterPositionAttribute, 2, GLES20.GL_FLOAT, false, 0, vertices);
        GLES20.glEnableVertexAttribArray(mFilterPositionAttribute);
        GLES20.glVertexAttribPointer(mFilterTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, textureCoordinaes);
        GLES20.glEnableVertexAttribArray(mFilterTextureCoordinateAttribute);
        setTextureDefaultConfig();
        if (mFirstInputFramebuffer != null) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFirstInputFramebuffer.getTexture());
            GLES20.glUniform1i(mFilterInputTextureUniform, 2);
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        if (mFirstInputFramebuffer != null) {
            mFirstInputFramebuffer.unlock();
        }
        GLES20.glDisableVertexAttribArray(mFilterPositionAttribute);
        GLES20.glDisableVertexAttribArray(mFilterTextureCoordinateAttribute);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }


    protected void setTextureDefaultConfig() {
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, this.mOutputTextureOptions.getMinFilter());
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, this.mOutputTextureOptions.getMagFilter());
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, this.mOutputTextureOptions.getWrapS());
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, this.mOutputTextureOptions.getWrapT());

    }

    protected GPUSize outputFrameSize() {
        return mInputTextureSize;
    }

    public void informTargetsAboutNewFrameAtTime(long frameIndex) {
        for (int i = 0; i < mTargets.size(); i++) {
            GPUImageInput currentTarget = mTargets.get(i);
            int indexOfobj = mTargets.indexOf(currentTarget);
            int textureIndex = mTargetTextureIndices.get(indexOfobj);
            setInputFramebufferForTarget(currentTarget, textureIndex);
            currentTarget.setInputSize(outputFrameSize(), textureIndex);

        }
        if (mOutputFramebuffer != null) {
            mOutputFramebuffer.unlock();
        }
        if (usingNextFrameForImageCapture) {

        } else {
            this.removeOutputFramebuffer();
        }
        for (int i = 0; i < mTargets.size(); i++) {
            GPUImageInput currentTarget = mTargets.get(i);
            int indexOfobj = mTargets.indexOf(currentTarget);
            int textureIndex = mTargetTextureIndices.get(indexOfobj);
            currentTarget.newFrameReadyAtTime(frameIndex, textureIndex);
        }
    }


    @Override
    public void newFrameReadyAtTime(long frameTime, int textureIndex) {
        currentFrameIndex = frameTime;
        currentTime = new CMTime(frameTime, Constants.NS_MUTIPLE);
//       if(Constants.VERBOSE_GL) Log.d(Constants.TAG_GL, "GPUImageFilter CurrentTime: " + CMTime.getSecond(currentTime));
        this.renderToTextureWithVertices(GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.squareVertices), GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.textureCoordinates), frameTime);
        this.informTargetsAboutNewFrameAtTime(frameTime);
    }

    //专门提供给carma用
    public void newFrameReadyAtTime(long frameTime, int textureIndex, int texureId) {
        currentFrameIndex = frameTime;
        mOutputFramebuffer = GPUImageContext.sharedFramebufferCache().fetchFramebufferForSize(sizeOfFBO(), false);
        mOutputFramebuffer.activeFramebuffer();
        mFilterProgram.use();
        if (!mFilterProgram.ismInitialized()) {
            return;
        }
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glVertexAttribPointer(mFilterPositionAttribute, 2, GLES20.GL_FLOAT, false, 0, GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.squareVertices));
        GLES20.glEnableVertexAttribArray(mFilterPositionAttribute);
        GLES20.glVertexAttribPointer(mFilterTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.noRotationTextureCoordinates));
        GLES20.glEnableVertexAttribArray(mFilterTextureCoordinateAttribute);
        setTextureDefaultConfig();
        if (texureId >= 0) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texureId);
            GLES20.glUniform1i(mFilterInputTextureUniform, 2);
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(mFilterPositionAttribute);
//        if (mFirstInputFramebuffer != null) {
//            mFirstInputFramebuffer.unlock();
//        }
        GLES20.glDisableVertexAttribArray(mFilterTextureCoordinateAttribute);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        this.informTargetsAboutNewFrameAtTime(frameTime);
    }

    @Override
    public void setInputFramebuffer(GPUImageFrameBuffer newInputFramebuffer, int textureIndex) {
        mFirstInputFramebuffer = newInputFramebuffer;
        if (mFirstInputFramebuffer != null) {
            mFirstInputFramebuffer.lock();
        }
    }

    @Override
    public int nextAvailableTextureIndex() {
        return 0;
    }

    @Override
    public void setInputSize(GPUSize newSize, int index) {
        mInputTextureSize = newSize;
        setupFilterForSize(sizeOfFBO());
    }

    @Override
    public void setInputRotation(GPUImageRotationMode newInputRotation, int textureIndex) {

    }

    @Override
    public GPUSize maximumOutputSize() {
        return new GPUSize(0, 0);
    }

    @Override
    public void endProcessing() {
        if (!mIsEndProcessing) {
            mIsEndProcessing = true;
            for (GPUImageInput currentTarget : mTargets) {
                currentTarget.endProcessing();
            }
        }

    }


    @Override
    public boolean shouldIgnoreUpdatesToThisTarget() {
        return false;
    }

    @Override
    public boolean enabled() {
        return false;
    }

    @Override
    public boolean wantsMonochromeInput() {
        return false;
    }

    @Override
    public void setCurrentlyReceivingMonochromeInput(boolean newValue) {

    }

    @Override
    public void setViewportRange(ViewportRange viewportRange) {
        this.viewportRange = viewportRange;
    }

    public void onDisplaySizeChanged(int width, int height) {
        surfaceHeight = height;
        surfaceWidth = width;
    }

    public void destroy() {

    }

    protected void setInteger(final int location, final int intValue) {
        if (mFilterProgram != null) {
            GLES20.glUseProgram(mFilterProgram.getmProgram());
            GLES20.glUniform1i(location, intValue);
        }
    }

    protected void setFloat(final int location, final float floatValue) {
        if (mFilterProgram != null) {
            GLES20.glUseProgram(mFilterProgram.getmProgram());
            GLES20.glUniform1f(location, floatValue);
        }
    }

    protected void setFloatVec2(final int location, final float[] arrayValue) {
        if (mFilterProgram != null) {
            GLES20.glUseProgram(mFilterProgram.getmProgram());
            GLES20.glUniform2fv(location, 1, FloatBuffer.wrap(arrayValue));
        }
    }

    protected void setFloatVec3(final int location, final float[] arrayValue) {
        if (mFilterProgram != null) {
            GLES20.glUseProgram(mFilterProgram.getmProgram());
            GLES20.glUniform3fv(location, 1, FloatBuffer.wrap(arrayValue));
        }
    }

    protected void setFloatVec4(final int location, final float[] arrayValue) {
        if (mFilterProgram != null) {
            GLES20.glUseProgram(mFilterProgram.getmProgram());
            GLES20.glUniform4fv(location, 1, FloatBuffer.wrap(arrayValue));
        }
    }

    protected void setFloatArray(final int location, final float[] arrayValue) {
        if (mFilterProgram != null) {
            GLES20.glUseProgram(mFilterProgram.getmProgram());
            GLES20.glUniform1fv(location, arrayValue.length, FloatBuffer.wrap(arrayValue));
        }
    }

    protected void setPoint(final int location, final PointF point) {
        if (mFilterProgram != null) {
            GLES20.glUseProgram(mFilterProgram.getmProgram());
            float[] vec2 = new float[2];
            vec2[0] = point.x;
            vec2[1] = point.y;
            GLES20.glUniform2fv(location, 1, vec2, 0);
        }
    }

    protected void setUniformMatrix3f(final int location, final float[] matrix) {
        if (mFilterProgram != null) {
            GLES20.glUseProgram(mFilterProgram.getmProgram());
            GLES20.glUniformMatrix3fv(location, 1, false, matrix, 0);
        }
    }

    protected void setUniformMatrix4f(final int location, final float[] matrix) {
        if (mFilterProgram != null) {
            GLES20.glUseProgram(mFilterProgram.getmProgram());
            GLES20.glUniformMatrix4fv(location, 1, false, matrix, 0);
        }
    }

    public CMTime getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(CMTime currentTime) {
        this.currentTime = currentTime;
    }

    public CMTime getTransitionStart() {
        return transitionStart;
    }

    public void setTransitionStart(CMTime transitionStart) {
        this.transitionStart = transitionStart;
    }

    public CMTime getTransitionEnd() {
        return transitionEnd;
    }

    public void setTransitionEnd(CMTime transitionEnd) {
        this.transitionEnd = transitionEnd;
    }

    @Override
    public void useNextFrameForImageCapture() {
        usingNextFrameForImageCapture = true;
    }

    @Override
    public Bitmap newBitMapFromCurrentlyProcessedOutput() {
        usingNextFrameForImageCapture = false;
        mOutputFramebuffer.activeFramebuffer();
        IntBuffer ib = IntBuffer.allocate(sizeOfFBO().width * sizeOfFBO().height * 4);
        GLES20.glReadPixels(0, 0, sizeOfFBO().width, sizeOfFBO().height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib);
        Bitmap result = Bitmap.createBitmap(sizeOfFBO().width, sizeOfFBO().height, Bitmap.Config.ARGB_8888);
        result.copyPixelsFromBuffer(IntBuffer.wrap(ib.array()));
        mOutputFramebuffer.unlock();
        this.removeOutputFramebuffer();
        return result;
    }

    private int number = 0;

    protected Bitmap debugCurrentBitmap(boolean save) {
        mOutputFramebuffer.activeFramebuffer();
        IntBuffer ib = IntBuffer.allocate(sizeOfFBO().width * sizeOfFBO().height * 4);
        GLES20.glReadPixels(0, 0, sizeOfFBO().width, sizeOfFBO().height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib);
        Bitmap result = Bitmap.createBitmap(sizeOfFBO().width, sizeOfFBO().height, Bitmap.Config.ARGB_8888);
        result.copyPixelsFromBuffer(IntBuffer.wrap(ib.array()));
        if (save) {
            File file = new File("/sdcard/debugpic/" + (number++) + ".jpg");
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file);
                result.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.flush();
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    protected Bitmap debugCurrentBitmap(GPUImageFrameBuffer frameBuffer, GPUSize size, String dir, boolean save) {
        frameBuffer.activeFramebuffer();
        IntBuffer ib = IntBuffer.allocate(size.width * size.height * 4);
        GLES20.glReadPixels(0, 0, size.width, size.height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib);
        Bitmap result = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888);
        result.copyPixelsFromBuffer(IntBuffer.wrap(ib.array()));
        if (save) {
            String path = "/sdcard/" + dir;
            boolean success = FileUtils.createDir(path);
            if (!success) {
                Log.e("debugCurrentBitmap", "debugCurrentBitmap Create Dir Faild!!!");
                return null;
            }
            File file = new File(path + "/" + UUID.randomUUID().toString() + ".jpg");
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file);
                result.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.flush();
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }


    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}

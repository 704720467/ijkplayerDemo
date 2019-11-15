package com.zp.libvideoedit.GPUImage.FilterCore;

import android.graphics.Bitmap;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.renderscript.Matrix4f;
import android.util.Log;

import com.zp.libvideoedit.GPUImage.Carma.Core.GPUSurfaceCameraView;
import com.zp.libvideoedit.GPUImage.Carma.Core.OpenGlUtils;
import com.zp.libvideoedit.GPUImage.Carma.Core.TextureRotationUtil;
import com.zp.libvideoedit.GPUImage.Carma.LogUtils;
import com.zp.libvideoedit.GPUImage.Core.GLProgram;
import com.zp.libvideoedit.GPUImage.Core.GPUImageContext;
import com.zp.libvideoedit.GPUImage.Core.GPUImageInput;
import com.zp.libvideoedit.GPUImage.Core.GPUImageOutput;
import com.zp.libvideoedit.GPUImage.Core.GPUImageTextureCoordinates;
import com.zp.libvideoedit.GPUImage.Core.GPURect;
import com.zp.libvideoedit.GPUImage.Core.GPUSize;
import com.zp.libvideoedit.GPUImage.Core.GPUUtiles;
import com.zp.libvideoedit.modle.ViewportRange;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

public class GPUImageCameraInputFilter extends GPUImageOutput {

    public static final String kGPUImageVertexShaderString = "" +
            "attribute vec4 position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            " \n" +
            "varying vec2 textureCoordinate;\n" +
            "uniform mat4 textureTransform;\n" +

            " \n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = position;\n" +
            "    textureCoordinate =(textureTransform*inputTextureCoordinate).xy;\n" +
            "}";
    public static final String kGPUImagePassthroughFragmentShaderString = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            "varying highp vec2 textureCoordinate;\n" +
            " \n" +
            "uniform samplerExternalOES inputImageTexture;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "     lowp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "     gl_FragColor = textureColor; \n" +
            "}";


    private float[] mTextureTransformMatrix;
    private int mTextureTransformMatrixLocation;

    private int mFrameWidth = -1;
    private int mFrameHeight = -1;
    private String TAG = this.getClass().getSimpleName();

    protected GLProgram mFilterProgram = null;
    protected int mFilterPositionAttribute = -1;
    protected int mFilterTextureCoordinateAttribute = -1;
    protected int mFilterInputTextureUniform = -1;
    private int mInputWidth;
    private int mInputHeight;
    private int mdisplayWidth;
    private int mdisplayHeight;
    private Object tackPhoto = new Object();
    private boolean isTakeePhto = false;
    private GPUSurfaceCameraView.TakePhtoListener mtakephotoListener;
    private boolean usingNextFrameForImageCapture;
    private FloatBuffer mTextureBuffer = null;
    private FloatBuffer mVertexBuffer = null;
    private GPURect viewPrint;
    private ViewportRange viewportRange;

    public GPUImageCameraInputFilter() {
        super();
        mVertexBuffer = GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.squareVertices);
        mTextureBuffer = GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.noRotationTextureCoordinates);
    }

    public void init() {
        mFilterProgram = new GLProgram().initWithVertexVShaderStringFShaderString(kGPUImageVertexShaderString, kGPUImagePassthroughFragmentShaderString);
        if (!mFilterProgram.ismInitialized()) {
            initializeAttributes();
            if (!mFilterProgram.link()) {
                Log.e(TAG, "Program link log: " + mFilterProgram.getmProgramLog());
                Log.e(TAG, "Fragment shader compile log: " + mFilterProgram.getmFragmentShaderLog());
                Log.e(TAG, "Vertex shader compile log: " + mFilterProgram.getmVertexShaderLog());
                mFilterProgram = null;
            }
        }
        usingNextFrameForImageCapture = false;
        mFilterPositionAttribute = mFilterProgram.attributeIndex("position");
        mFilterTextureCoordinateAttribute = mFilterProgram.attributeIndex("inputTextureCoordinate");
        mFilterInputTextureUniform = mFilterProgram.uniformIndex("inputImageTexture");
        GPUImageContext.setActiveShaderProgram(mFilterProgram);
        GLES20.glEnableVertexAttribArray(mFilterPositionAttribute);
        GLES20.glEnableVertexAttribArray(mFilterTextureCoordinateAttribute);
        mTextureTransformMatrixLocation = mFilterProgram.uniformIndex("textureTransform");
        Matrix4f matrix4f = new Matrix4f();
        matrix4f.loadIdentity();
        mTextureTransformMatrix = matrix4f.getArray();
    }

    protected void initializeAttributes() {
        mFilterProgram.addAttribute("position");
        mFilterProgram.addAttribute("inputTextureCoordinate");
    }

    public void setmTextureTransformMatrix(float[] mTextureTransformMatrix) {
        this.mTextureTransformMatrix = mTextureTransformMatrix;
    }

    public int onDrawToTexture(final int textureId) {
        if (mOutputFramebuffer == null) {
            mOutputFramebuffer = GPUImageContext.sharedFramebufferCache().fetchFramebufferForSize(new GPUSize(mInputWidth, mInputHeight), false);
        }
        mOutputFramebuffer.activeFramebuffer();
        if (viewPrint != null) {
            GLES20.glViewport((int) viewPrint.getX(), (int) viewPrint.getY(), (int) viewPrint.getWidth(), (int) viewPrint.getHeight());
        }
        mFilterProgram.use();

        if (usingNextFrameForImageCapture) {
            mOutputFramebuffer.lock();
        }
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        if (!mFilterProgram.ismInitialized()) {
            return OpenGlUtils.NOT_INIT;
        }

        GLES20.glVertexAttribPointer(mFilterPositionAttribute, 2, GLES20.GL_FLOAT, false, 0, mVertexBuffer);
        GLES20.glEnableVertexAttribArray(mFilterPositionAttribute);
        GLES20.glVertexAttribPointer(mFilterTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, mTextureBuffer);
        GLES20.glEnableVertexAttribArray(mFilterTextureCoordinateAttribute);
        GLES20.glUniformMatrix4fv(mTextureTransformMatrixLocation, 1, false, mTextureTransformMatrix, 0);

        if (textureId != OpenGlUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
            GLES20.glUniform1i(mFilterInputTextureUniform, 0);
        }
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glFlush();
        if (isTakeePhto) {
            mOutputFramebuffer.activeFramebuffer();
            IntBuffer ib = IntBuffer.allocate(mInputWidth * mInputHeight * 4);
            GLES20.glReadPixels(0, 0, mInputWidth, mInputHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib);
            Bitmap result = Bitmap.createBitmap(mInputWidth, mInputHeight, Bitmap.Config.ARGB_8888);
            result.copyPixelsFromBuffer(IntBuffer.wrap(ib.array()));

            if (result != null) {
                if (mtakephotoListener != null) mtakephotoListener.takePhtoComplete(result);
            }
            isTakeePhto = false;
        }
        int tempTexutre = mOutputFramebuffer.getTexture();
        GLES20.glDisableVertexAttribArray(mFilterPositionAttribute);
        GLES20.glDisableVertexAttribArray(mFilterTextureCoordinateAttribute);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
//        informTargetsAboutNewFrameAtTime(0);
        return tempTexutre;
    }


    public int onDrawToTexture(final int textureId, long currentTime) {
        mOutputFramebuffer = GPUImageContext.sharedFramebufferCache().fetchFramebufferForSize(new GPUSize(mInputWidth, mInputHeight), false);
        mOutputFramebuffer.activeFramebuffer();
        mFilterProgram.use();
        if (usingNextFrameForImageCapture) {
            mOutputFramebuffer.lock();
        }
        if (!mFilterProgram.ismInitialized()) {
            return OpenGlUtils.NOT_INIT;
        }

        GLES20.glVertexAttribPointer(mFilterPositionAttribute, 2, GLES20.GL_FLOAT, false, 0, GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.squareVertices));
        GLES20.glEnableVertexAttribArray(mFilterPositionAttribute);
        GLES20.glVertexAttribPointer(mFilterTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.noRotationTextureCoordinates));
        GLES20.glEnableVertexAttribArray(mFilterTextureCoordinateAttribute);
        GLES20.glUniformMatrix4fv(mTextureTransformMatrixLocation, 1, false, mTextureTransformMatrix, 0);
        setTextureDefaultConfig();
        if (textureId != OpenGlUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
            GLES20.glUniform1i(mFilterInputTextureUniform, 0);
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glFlush();

        if (isTakeePhto) {
            mOutputFramebuffer.activeFramebuffer();
            IntBuffer ib = IntBuffer.allocate(mInputWidth * mInputHeight * 4);
            GLES20.glReadPixels(0, 0, mInputWidth, mInputHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib);
            Bitmap result = Bitmap.createBitmap(mInputWidth, mInputHeight, Bitmap.Config.ARGB_8888);
            result.copyPixelsFromBuffer(IntBuffer.wrap(ib.array()));

            if (result != null) {
                if (mtakephotoListener != null) mtakephotoListener.takePhtoComplete(result);
            }
            isTakeePhto = false;
        }
        int tempTexutre = mOutputFramebuffer.getTexture();
        GLES20.glDisableVertexAttribArray(mFilterPositionAttribute);
        GLES20.glDisableVertexAttribArray(mFilterTextureCoordinateAttribute);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        informTargetsAboutNewFrameAtTime(currentTime);
        return tempTexutre;
    }

    private void setTextureDefaultConfig() {
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }


    public void setTakeePhto(boolean takeePhto, GPUSurfaceCameraView.TakePhtoListener listener) {
        isTakeePhto = takeePhto;
        mtakephotoListener = listener;
    }


    public void informTargetsAboutNewFrameAtTime(long currentPts) {
        for (int i = 0; i < mTargets.size(); i++) {
            GPUImageInput currentTarget = mTargets.get(i);
            int indexOfobj = mTargets.indexOf(currentTarget);
            int textureIndex = mTargetTextureIndices.get(indexOfobj);
            setInputFramebufferForTarget(currentTarget, textureIndex);
            currentTarget.setInputSize(new GPUSize(mInputWidth, mInputHeight), textureIndex);
            currentTarget.setViewportRange(viewportRange);

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
            currentTarget.newFrameReadyAtTime(currentPts, textureIndex);
        }
    }

    @Override
    public void useNextFrameForImageCapture() {
        usingNextFrameForImageCapture = true;
    }

    public void onInputSizeChanged(int width, int height) {
        mInputHeight = height;
        mInputWidth = width;
    }

    public void onDisplaySizeChanged(int width, int height) {
        mdisplayWidth = width;
        mdisplayHeight = height;
    }

    public void destroyFramebuffers() {
        mFrameWidth = -1;
        mFrameHeight = -1;
    }

    public void adjustTextureBuffer(int orientation, boolean flipVertical) {
        float[] textureCords = TextureRotationUtil.getRotation(orientation, true, flipVertical);
        LogUtils.d(TAG, "==========rotation: " + orientation + " flipVertical: " + flipVertical
                + " texturePos: " + Arrays.toString(textureCords));
        if (mTextureBuffer == null) {
            mTextureBuffer = ByteBuffer.allocateDirect(textureCords.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
        }
        mTextureBuffer.clear();

        mTextureBuffer.put(textureCords).position(0);
    }

    /**
     * 用来计算贴纸渲染的纹理最终需要的顶点坐标
     */
    public void calculateVertexBuffer(int displayW, int displayH, int imageW, int imageH) {
        int outputHeight = displayH;
        int outputWidth = displayW;

        float ratio1 = (float) outputWidth / imageW;
        float ratio2 = (float) outputHeight / imageH;
        float ratioMin = Math.min(ratio1, ratio2);
        int imageWidthNew = Math.round(imageW * ratioMin);
        int imageHeightNew = Math.round(imageH * ratioMin);

        float ratioWidth = imageWidthNew / (float) outputWidth;
        float ratioHeight = imageHeightNew / (float) outputHeight;

        float[] cube = new float[]{
                TextureRotationUtil.CUBE[0] / ratioHeight, TextureRotationUtil.CUBE[1] / ratioWidth,
                TextureRotationUtil.CUBE[2] / ratioHeight, TextureRotationUtil.CUBE[3] / ratioWidth,
                TextureRotationUtil.CUBE[4] / ratioHeight, TextureRotationUtil.CUBE[5] / ratioWidth,
                TextureRotationUtil.CUBE[6] / ratioHeight, TextureRotationUtil.CUBE[7] / ratioWidth,
        };

        if (mVertexBuffer == null) {
            mVertexBuffer = ByteBuffer.allocateDirect(cube.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
        }
        mVertexBuffer.clear();
        mVertexBuffer.put(cube).position(0);
    }

    /**
     * 重新计算可视窗口
     */
    public void calculateTextureBuffer(ViewportRange viewportRange) {
        float[] cube = new float[]{
                viewportRange.getLeft(), viewportRange.getBottom(),
                viewportRange.getRight(), viewportRange.getBottom(),
                viewportRange.getLeft(), viewportRange.getTop(),
                viewportRange.getRight(), viewportRange.getTop(),
        };
        if (mTextureBuffer == null) {
            mTextureBuffer = ByteBuffer.allocateDirect(cube.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
        }
        mTextureBuffer.clear();
        mTextureBuffer.put(cube).position(0);
    }


    @Override
    public Bitmap newBitMapFromCurrentlyProcessedOutput() {
        usingNextFrameForImageCapture = false;
        if (mOutputFramebuffer != null) {
            Bitmap bitmap = mOutputFramebuffer.newBitMapFromFramebufferContents();
            return bitmap;
        }
        return null;
    }

    public void destroy() {
        if (mOutputFramebuffer != null) {
            mOutputFramebuffer.destoryFramebuffer();
            mOutputFramebuffer = null;
        }
    }

    public void setViewPrint(GPURect viewPrint) {
        this.viewPrint = viewPrint;
    }

    /**
     * 设置可视窗口位置
     *
     * @param viewportRange
     */
    public void setViewportRange(ViewportRange viewportRange) {
        this.viewportRange = viewportRange;
    }
}

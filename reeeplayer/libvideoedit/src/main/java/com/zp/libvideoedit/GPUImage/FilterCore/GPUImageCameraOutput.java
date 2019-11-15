package com.zp.libvideoedit.GPUImage.FilterCore;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.util.Log;

import com.zp.libvideoedit.GPUImage.Core.GLProgram;
import com.zp.libvideoedit.GPUImage.Core.GPUImageContext;
import com.zp.libvideoedit.GPUImage.Core.GPUImageFrameBuffer;
import com.zp.libvideoedit.GPUImage.Core.GPUImageInput;
import com.zp.libvideoedit.GPUImage.Core.GPUImageRotationMode;
import com.zp.libvideoedit.GPUImage.Core.GPUImageTextureCoordinates;
import com.zp.libvideoedit.GPUImage.Core.GPURect;
import com.zp.libvideoedit.GPUImage.Core.GPUSize;
import com.zp.libvideoedit.GPUImage.Core.GPUUtiles;
import com.zp.libvideoedit.modle.ViewportRange;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.IntBuffer;

/**
 * Created by gwd on 2018/2/27.
 */

public class GPUImageCameraOutput implements GPUImageInput {
    public GPUImageCameraOutput() {
    }

    public void init() {
        mFilterProgram = new GLProgram().initWithVertexVShaderStringFShaderString(NO_FILTER_VERTEX_SHADER, NO_FILTER_FRAGMENT_SHADER);
        if (!mFilterProgram.ismInitialized()) {
            initializeAttributes();
            if (!mFilterProgram.link()) {
                Log.e(TAG, "Program link log: " + mFilterProgram.getmProgramLog());
                Log.e(TAG, "Fragment shader compile log: " + mFilterProgram.getmFragmentShaderLog());
                Log.e(TAG, "Vertex shader compile log: " + mFilterProgram.getmVertexShaderLog());
                mFilterProgram = null;
            }
        }
        mFilterPositionAttribute = mFilterProgram.attributeIndex("position");
        mFilterTextureCoordinateAttribute = mFilterProgram.attributeIndex("inputTextureCoordinate");
        mFilterInputTextureUniform = mFilterProgram.uniformIndex("inputImageTexture");
        GPUImageContext.setActiveShaderProgram(mFilterProgram);
        GLES20.glEnableVertexAttribArray(mFilterPositionAttribute);
        GLES20.glEnableVertexAttribArray(mFilterTextureCoordinateAttribute);
    }

    protected void initializeAttributes() {
        mFilterProgram.addAttribute("position");
        mFilterProgram.addAttribute("inputTextureCoordinate");
    }

    @Override
    public void newFrameReadyAtTime(long frameTime, int textureIndex) {
        mFilterProgram.use();
        if (!mFilterProgram.ismInitialized()) {
            return;
        }
        GLES20.glViewport(0, 0, mInputWidth, mInputHeight);
        if (this.viewpoint != null) {
            GLES20.glViewport((int) this.viewpoint.getX(), (int) this.viewpoint.getY(), (int) this.viewpoint.getWidth(), (int) this.viewpoint.getHeight());
        }
        GLES20.glVertexAttribPointer(mFilterPositionAttribute, 2, GLES20.GL_FLOAT, false, 0, GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.squareVertices));
        GLES20.glEnableVertexAttribArray(mFilterPositionAttribute);
        GLES20.glVertexAttribPointer(mFilterTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0,
                GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.textureCoordinates));
        GLES20.glEnableVertexAttribArray(mFilterTextureCoordinateAttribute);
        if (firstFramebuffer != null) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, firstFramebuffer.getTexture());
            cameraOutputTextureId = firstFramebuffer.getTexture();
            GLES20.glUniform1i(mFilterInputTextureUniform, 2);
        }
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glFlush();
        firstFramebuffer.unlock();
        GLES20.glDisableVertexAttribArray(mFilterPositionAttribute);
        GLES20.glDisableVertexAttribArray(mFilterTextureCoordinateAttribute);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }
    static  int number = 0 ;
    protected Bitmap debugCurrentBitmap(GPUImageFrameBuffer frameBuffer , GPUSize size , boolean save) {
        frameBuffer.activeFramebuffer();
        IntBuffer ib = IntBuffer.allocate(size.width * size.height * 4);
        GLES20.glReadPixels(0, 0, size.width, size.height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib);
        Bitmap result = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888);
        result.copyPixelsFromBuffer(IntBuffer.wrap(ib.array()));
        if (save) {
            File file = new File("/sdcard/outputpic/" + (number++) + ".jpg");
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

    public void newFrameReadyAtTime(int textureId, int inputWidth, int inputHeight) {
        mFilterProgram.use();
        if (!mFilterProgram.ismInitialized()) {
            return;
        }
        GLES20.glViewport(0, 0, inputWidth, inputHeight);
        if (this.viewpoint != null) {
            GLES20.glViewport((int) this.viewpoint.getX(), (int) this.viewpoint.getY(), (int) this.viewpoint.getWidth(), (int) this.viewpoint.getHeight());
        }
        GLES20.glVertexAttribPointer(mFilterPositionAttribute, 2, GLES20.GL_FLOAT, false, 0, GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.squareVertices));
        GLES20.glEnableVertexAttribArray(mFilterPositionAttribute);
        GLES20.glVertexAttribPointer(mFilterTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0,
                GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.textureCoordinates));
        GLES20.glEnableVertexAttribArray(mFilterTextureCoordinateAttribute);
        if (textureId >= 0) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glUniform1i(mFilterInputTextureUniform, 2);
        }
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glFlush();
        GLES20.glDisableVertexAttribArray(mFilterPositionAttribute);
        GLES20.glDisableVertexAttribArray(mFilterTextureCoordinateAttribute);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }


    public int getCameraOutputTextureId() {
        return cameraOutputTextureId;
    }

//    public void setDisplaySize(GPUSize size) {
//        mDisplayHeight = size.height;
//        mDisplayWidth = size.width;
//    }


    @Override
    public void setInputFramebuffer(GPUImageFrameBuffer newInputFramebuffer, int textureIndex) {
        firstFramebuffer = newInputFramebuffer;
        if (firstFramebuffer != null) {
            firstFramebuffer.lock();
        }
    }

    @Override
    public int nextAvailableTextureIndex() {
        return 0;
    }

    @Override
    public void setInputSize(GPUSize newSize, int index) {
        mInputHeight = newSize.height;
        mInputWidth = newSize.width;
    }

    @Override
    public void setInputRotation(GPUImageRotationMode newInputRotation, int textureIndex) {

    }

    @Override
    public GPUSize maximumOutputSize() {
        return null;
    }

    @Override
    public void endProcessing() {

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

    }

    public void destory() {

    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    public void setViewpoint(GPURect viewpoint) {
        this.viewpoint = viewpoint;
    }

    public static final String NO_FILTER_VERTEX_SHADER = "" +
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
    public static final String NO_FILTER_FRAGMENT_SHADER = "" +
            "varying highp vec2 textureCoordinate;\n" +
            " \n" +
            "uniform sampler2D inputImageTexture;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "     gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "}";

    private String TAG = this.getClass().getSimpleName();

    protected GLProgram mFilterProgram = null;
    protected int mFilterPositionAttribute = -1;
    protected int mFilterTextureCoordinateAttribute = -1;
    protected int mFilterInputTextureUniform = -1;
    private GPUImageFrameBuffer firstFramebuffer;
    private int mInputWidth, mInputHeight;
    private int mDisplayWidth, mDisplayHeight;
    private int cameraOutputTextureId;
    private GPURect viewpoint;


}

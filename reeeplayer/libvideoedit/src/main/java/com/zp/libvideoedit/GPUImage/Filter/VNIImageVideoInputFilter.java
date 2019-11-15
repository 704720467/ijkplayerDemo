package com.zp.libvideoedit.GPUImage.Filter;

import android.opengl.GLES20;
import android.renderscript.Matrix4f;

import com.zp.libvideoedit.GPUImage.Core.GPUImageContext;
import com.zp.libvideoedit.GPUImage.Core.GPUImageFrameBuffer;
import com.zp.libvideoedit.GPUImage.Core.GPUImageTextureCoordinates;
import com.zp.libvideoedit.GPUImage.Core.GPURect;
import com.zp.libvideoedit.GPUImage.Core.GPUSize;
import com.zp.libvideoedit.GPUImage.Core.GPUUtiles;
import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageFilter;
import com.zp.libvideoedit.utils.MatrixUtils;

import java.nio.FloatBuffer;

/**
 * Created by gwd on 2018/4/9.
 */

public class VNIImageVideoInputFilter extends GPUImageFilter {

    public static final String videoInputVertexShader =
            " attribute vec4 position;\n" +
                    " attribute vec4 inputTextureCoordinate;\n" +
                    " \n" +
                    " uniform mat4 transformMatrix;\n" +
                    "\n" +
                    " varying vec2 textureCoordinate;\n" +
                    " \n" +
                    " void main()\n" +
                    " {\n" +
                    "     gl_Position = transformMatrix * vec4(position.xyz, 1.0) ;\n" +
                    "     textureCoordinate = inputTextureCoordinate.xy;\n" +
                    " }";

    //视频本身的transform
    private Matrix4f preferredTransform;
    //旋转的transform
    private Matrix4f rotateTransform;

    private GPUSize videoSize;

    private int transformLocation;

    public VNIImageVideoInputFilter() {
        super(videoInputVertexShader, kGPUImagePassthroughFragmentShaderString);
        Matrix4f matrix4f = new Matrix4f();
        matrix4f.loadIdentity();
        this.preferredTransform = matrix4f;
    }

    @Override
    public void init() {
        super.init();
        transformLocation = mFilterProgram.uniformIndex("transformMatrix");
    }

    @Override
    public void renderToTextureWithVertices(FloatBuffer vertices, FloatBuffer textureCoordinaes, long frameIndex) {

        mOutputFramebuffer = GPUImageContext.sharedFramebufferCache().fetchFramebufferForSize(sizeOfFBO(), false);
        mOutputFramebuffer.activeFramebuffer();
        mFilterProgram.use();
        if (!mFilterProgram.ismInitialized()) {
            return;
        }
        GLES20.glClearColor(0, 0, 0, 0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GPURect handelRect = handelViewPort();
        GLES20.glViewport((int) handelRect.getX(), (int) handelRect.getY(), (int) handelRect.getWidth(), (int) handelRect.getHeight());
        GLES20.glVertexAttribPointer(mFilterPositionAttribute, 2, GLES20.GL_FLOAT, false, 0, GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.squareVertices));
        GLES20.glEnableVertexAttribArray(mFilterPositionAttribute);
        GLES20.glVertexAttribPointer(mFilterTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.textureCoordinates));
        GLES20.glEnableVertexAttribArray(mFilterTextureCoordinateAttribute);
        setTextureDefaultConfig();
        if (mFirstInputFramebuffer != null) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFirstInputFramebuffer.getTexture());
            GLES20.glUniform1i(mFilterInputTextureUniform, 2);
        }
        GLES20.glUniformMatrix4fv(transformLocation, 1, false, this.preferredTransform.getArray(), 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        if (mFirstInputFramebuffer != null) {
            mFirstInputFramebuffer.unlock();
        }
        GLES20.glDisableVertexAttribArray(mFilterPositionAttribute);
        GLES20.glDisableVertexAttribArray(mFilterTextureCoordinateAttribute);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

    }

    @Override
    public void setInputFramebuffer(GPUImageFrameBuffer newInputFramebuffer, int textureIndex) {
        mFirstInputFramebuffer = newInputFramebuffer;
        if (mFirstInputFramebuffer != null) {
            mFirstInputFramebuffer.lock();
        }
    }

    private GPURect handelViewPort() {
        GPUSize bufferSize = videoSize;
        GPUSize newSize = MatrixUtils.SizeApplyAffineTransform2D(bufferSize, this.preferredTransform);
        newSize = new GPUSize(Math.abs(newSize.width), Math.abs(newSize.height));
        GPURect originRect = new GPURect(0, 0, sizeOfFBO().width, sizeOfFBO().height);
        GPURect fitRect = MatrixUtils.AVMakeRectWithAspectRatioInsideRect(bufferSize, originRect, viewportRange);
        return fitRect;
    }

    @Override
    protected GPUSize outputFrameSize() {
        return super.outputFrameSize();
    }

    @Override
    public GPUSize sizeOfFBO() {
        return super.sizeOfFBO();
    }

    public Matrix4f getPreferredTransform() {
        return preferredTransform;
    }

    public void setPreferredTransform(Matrix4f preferredTransform, GPUSize videoSize) {
        this.preferredTransform = preferredTransform;
        this.videoSize = videoSize;
    }


}

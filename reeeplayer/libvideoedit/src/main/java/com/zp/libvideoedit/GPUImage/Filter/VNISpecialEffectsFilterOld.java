package com.zp.libvideoedit.GPUImage.Filter;

import android.opengl.GLES20;
import android.text.TextUtils;
import android.util.Log;

import com.zp.libvideoedit.EditConstants;
import com.zp.libvideoedit.GPUImage.Core.GPUImageContext;
import com.zp.libvideoedit.GPUImage.Core.GPUImageTextureCoordinates;
import com.zp.libvideoedit.GPUImage.Core.GPUUtiles;
import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageFilter;
import com.zp.libvideoedit.utils.JsonUtil;

import java.nio.FloatBuffer;

/**
 * 特效
 * Created by zp on 2019/5/10.
 */

public class VNISpecialEffectsFilterOld extends GPUImageFilter {

    protected static final String AlphaBlendFilterVertexShaderString = "" +
            "attribute vec4 position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            "varying vec2 textureCoordinate;\n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = position;\n" +
            "    textureCoordinate = inputTextureCoordinate.xy;\n" +
            "}";

    private static final String AlphaBlendFilterFragment =
            " varying highp vec2 textureCoordinate;\n" +
                    " \n" +
                    " uniform sampler2D inputImageTexture;\n" +
                    " \n" +
                    " void main()\n" +
                    " {\n" +
                    "     gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
                    " }";
    private String key = "time";
    protected int parameterLocal = -1;
    private float parameterValue = 0;
    private SpecialEffectModel specialEffectModel;

    public VNISpecialEffectsFilterOld(String effectJson) {
        super();
        specialEffectModel = JsonUtil.parseJsonToBean(effectJson, SpecialEffectModel.class);
        vertextShaderString = TextUtils.isEmpty(specialEffectModel.getVsh()) ? AlphaBlendFilterVertexShaderString : specialEffectModel.getVsh();
        fragmentShaderString = TextUtils.isEmpty(specialEffectModel.getFsh()) ? AlphaBlendFilterFragment : specialEffectModel.getFsh();
        this.key = specialEffectModel.getKey();
    }

    @Override
    public void init() {
        super.init();
        if (!TextUtils.isEmpty(key))
            parameterLocal = mFilterProgram.uniformIndex(key);
    }


    @Override
    public void renderToTextureWithVertices(FloatBuffer vertices, FloatBuffer textureCoordinaes, long frameIndex) {

        mOutputFramebuffer = GPUImageContext.sharedFramebufferCache().fetchFramebufferForSize(sizeOfFBO(), false);

        mOutputFramebuffer.activeFramebuffer();
        mFilterProgram.use();
        if (!mFilterProgram.ismInitialized()) {
            return;
        }
        if (usingNextFrameForImageCapture) {
            mOutputFramebuffer.lock();
        }
        GLES20.glClearColor(0, 0, 0, 0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glVertexAttribPointer(mFilterPositionAttribute, 2, GLES20.GL_FLOAT, false, 0, GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.squareVertices));
        GLES20.glEnableVertexAttribArray(mFilterPositionAttribute);
        GLES20.glVertexAttribPointer(mFilterTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, GPUUtiles.directFloatBufferFromFloatArray(GPUImageTextureCoordinates.textureCoordinates));
        GLES20.glEnableVertexAttribArray(mFilterTextureCoordinateAttribute);

        setTextureDefaultConfig();
        if (mFirstInputFramebuffer != null) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFirstInputFramebuffer.getTexture());
            if (parameterLocal != -1)
                GLES20.glUniform1f(parameterLocal, parameterValue);
            GLES20.glUniform1i(mFilterInputTextureUniform, 2);
        }

        if (EditConstants.VERBOSE_GL)
            Log.d(EditConstants.TAG_GL, "GPUImageAlphaBlendFilter_renderToTextureWithVertices" + ",Size: " + sizeOfFBO());
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glFlush();

        if (mFirstInputFramebuffer != null)
            mFirstInputFramebuffer.unlock();
        GLES20.glDisableVertexAttribArray(mFilterPositionAttribute);
        GLES20.glDisableVertexAttribArray(mFilterTextureCoordinateAttribute);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    /**
     * 设置特效的时间
     *
     * @param parameterValue
     */
    public void setParameterValue(float parameterValue) {
        this.parameterValue = parameterValue;
    }

    protected class SpecialEffectModel {


        /**
         * id : 2333
         * fsh : precision highp float;
         * <p>
         * uniform sampler2D inputImageTexture;
         * varying vec2 textureCoordinate;
         * <p>
         * uniform float time;
         * <p>
         * const float PI = 3.1415926;
         * <p>
         * float rand(float n) {
         * return fract(sin(n) * 43758.5453123);
         * }
         * <p>
         * void main (void) {
         * float maxJitter = 0.1;
         * float duration = 0.3;
         * float colorROffset = 0.01;
         * float colorBOffset = -0.025;
         * <p>
         * float time = mod(time, duration * 2.0);
         * float amplitude = max(sin(time * (PI / duration)), 0.0);
         * <p>
         * float jitter = rand(textureCoordinate.y) * 2.0 - 1.0;
         * bool needOffset = abs(jitter) < maxJitter * amplitude;
         * <p>
         * float textureX = textureCoordinate.x + (needOffset ? jitter : (jitter * amplitude * 0.006));
         * vec2 textureCoords = vec2(textureX, textureCoordinate.y);
         * <p>
         * vec4 mask = texture2D(inputImageTexture, textureCoords);
         * vec4 maskR = texture2D(inputImageTexture, textureCoords + vec2(colorROffset * amplitude, 0.0));
         * vec4 maskB = texture2D(inputImageTexture, textureCoords + vec2(colorBOffset * amplitude, 0.0));
         * <p>
         * gl_FragColor = vec4(maskR.r, mask.g, maskB.b, mask.a);
         * }
         * <p>
         * vsh : attribute vec4 position; attribute vec4 inputTextureCoordinate; varying vec2 textureCoordinate; uniform lowp float time; const float PI = 3.1415926; void main() { float duration = 0.6; float maxAmplitude = 0.3; float time = mod(time, duration); float amplitude = 1.0 + maxAmplitude * abs(sin(time * (PI / duration))); gl_Position = vec4(position.x * amplitude, position.y * amplitude, position.zw); textureCoordinate = inputTextureCoordinate.xy; }
         * key : time
         */

        private String id;
        private String fsh;
        private String vsh;
        private String key;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getFsh() {
            return fsh;
        }

        public void setFsh(String fsh) {
            this.fsh = fsh;
        }

        public String getVsh() {
            return vsh;
        }

        public void setVsh(String vsh) {
            this.vsh = vsh;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }
    }
}

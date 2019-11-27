package com.zp.libvideoedit.GPUImage.Filter;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.text.TextUtils;
import android.util.Log;


import com.zp.libvideoedit.Constants;
import com.zp.libvideoedit.GPUImage.Core.GPUImageContext;
import com.zp.libvideoedit.GPUImage.Core.GPUImageFrameBuffer;
import com.zp.libvideoedit.GPUImage.Core.GPUImageTextureCoordinates;
import com.zp.libvideoedit.GPUImage.Core.GPUSize;
import com.zp.libvideoedit.GPUImage.Core.GPUUtiles;
import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageFilter;
import com.zp.libvideoedit.VideoEditUtils;
import com.zp.libvideoedit.utils.BitmapUtil;
import com.zp.libvideoedit.utils.JsonUtil;

import java.nio.FloatBuffer;

/**
 * 特效 支持五参数
 * time：当前时间单位秒、type：给定参数、effectvalue:可调参数值、inputSize：画布的宽高、noiseTexture:采样器；
 * Created by zp on 2019/5/16.
 */

public class VNISpecialEffectsFilter extends GPUImageFilter {

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
    protected int miTimeLocal = -1;
    protected int mTypeLocal = -1;
    private float miTimegVlaue;
    protected int mEffectValueLocal = -1;
    protected int mInputSizeLocal = -1;
    protected int mNoiseTextureLocal = -1;
    private SpecialEffectModel specialEffectModel;

    public VNISpecialEffectsFilter(String effectJson) {
        super();
        if (!TextUtils.isEmpty(effectJson)) {
            specialEffectModel = JsonUtil.parseJsonToBean(effectJson, SpecialEffectModel.class);
            vertextShaderString = TextUtils.isEmpty(specialEffectModel.getVsh()) ? AlphaBlendFilterVertexShaderString : specialEffectModel.getVsh();
            fragmentShaderString = TextUtils.isEmpty(specialEffectModel.getFsh()) ? AlphaBlendFilterFragment : specialEffectModel.getFsh();
        }
    }

    @Override
    public void init() {
        super.init();
        if (specialEffectModel == null) return;
        miTimeLocal = specialEffectModel.isTimeRelated() ? mFilterProgram.uniformIndex("time") : -1;
        mTypeLocal = !TextUtils.isEmpty(specialEffectModel.getType()) ? mFilterProgram.uniformIndex("type") : -1;
        mEffectValueLocal = !TextUtils.isEmpty(specialEffectModel.getEffectValue()) ? mFilterProgram.uniformIndex("effectValue") : -1;
        mInputSizeLocal = specialEffectModel.isSizeRelated() ? mFilterProgram.uniformIndex("inputSize") : -1;
        mNoiseTextureLocal = !TextUtils.isEmpty(specialEffectModel.getNoise()) ? mFilterProgram.uniformIndex("noiseTexture") : -1;
        if (mNoiseTextureLocal != -1) {
            bitmap = BitmapUtil.getImageFromAssetsFile(VideoEditUtils.instance, "effect/" + specialEffectModel.getNoise());
            mPixelSizeOfImage = new GPUSize(bitmap.getWidth(), bitmap.getHeight());
            initPic();
        }
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
        setShaderValue();
        if (mFirstInputFramebuffer != null) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFirstInputFramebuffer.getTexture());
            GLES20.glUniform1i(mFilterInputTextureUniform, 2);
        }

        if (picGp != null && mNoiseTextureLocal != -1) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, picGp.getTexture());
            GLES20.glUniform1i(mNoiseTextureLocal, 3);
        }

        if (Constants.VERBOSE_GL)
            Log.d(Constants.TAG_GL, "GPUImageAlphaBlendFilter_renderToTextureWithVertices" + ",Size: " + sizeOfFBO());
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glFlush();

        if (mFirstInputFramebuffer != null)
            mFirstInputFramebuffer.unlock();
        GLES20.glDisableVertexAttribArray(mFilterPositionAttribute);
        GLES20.glDisableVertexAttribArray(mFilterTextureCoordinateAttribute);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    private void setShaderValue() {
        if (miTimeLocal != -1)
            GLES20.glUniform1f(miTimeLocal, miTimegVlaue);
        if (mTypeLocal != -1)
            GLES20.glUniform1i(mTypeLocal, Integer.parseInt(specialEffectModel.getType()));
        if (mEffectValueLocal != -1)
            GLES20.glUniform1f(mEffectValueLocal, Float.parseFloat(specialEffectModel.getEffectValue()));
        if (mInputSizeLocal != -1)
            setFloatVec2(mInputSizeLocal, new float[]{sizeOfFBO().width, sizeOfFBO().height});
    }

    /**
     * 设置特效的时间
     *
     * @param miTimegVlaue 当前时间 单位秒
     */
    public void setMiTimegVlaue(float miTimegVlaue) {
        this.miTimegVlaue = miTimegVlaue;
    }

    private boolean isInited = false;
    private Bitmap bitmap;
    private GPUSize mPixelSizeOfImage = null;
    private GPUImageFrameBuffer picGp;

    public void initPic() {
        if (!isInited) {
            mPixelSizeOfImage = new GPUSize(mPixelSizeOfImage.width, mPixelSizeOfImage.height);
            picGp = new GPUImageFrameBuffer(mPixelSizeOfImage, this.mOutputTextureOptions, true);
            picGp.disableReferenceCounting();
            picGp.activeFramebuffer();
            GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, picGp.getTexture());
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            if (!bitmap.isRecycled()) {
                bitmap.recycle();
            }
            bitmap = null;
            GLES20.glFlush();
            isInited = true;
        }
    }

    protected class SpecialEffectModel {
        /**
         * id : soulout
         * fsh : precision highp float; varying highp vec2 textureCoordinate; uniform sampler2D inputImageTexture; uniform float time; void main() { float duration = 0.7; float maxAlpha = 0.4; float maxScale = 1.8; float progress = mod(time, duration) / duration; float alpha = maxAlpha * (1.0 - progress); float scale = 1.0 + (maxScale - 1.0) * progress; float weakX = 0.5 + (textureCoordinate.x - 0.5) / scale; float weakY = 0.5 + (textureCoordinate.y - 0.5) / scale; vec2 weakTextureCoords = vec2(weakX, weakY); vec4 weakMask = texture2D(inputImageTexture, weakTextureCoords); vec4 mask = texture2D(inputImageTexture, textureCoordinate); gl_FragColor = mask * (1.0 - alpha) + weakMask * alpha; }
         * vsh : attribute vec4 position; attribute vec4 inputTextureCoordinate; varying vec2 textureCoordinate; uniform lowp float time; const float PI = 3.1415926; void main() { float duration = 0.6; float maxAmplitude = 0.3; float time = mod(time, duration); float amplitude = 1.0 + maxAmplitude * abs(sin(time * (PI / duration))); gl_Position = vec4(position.x * amplitude, position.y * amplitude, position.zw); textureCoordinate = inputTextureCoordinate.xy; }
         * timeRelated : true
         * sizeRelated : false
         * type :
         * effectValue :
         * noise :
         */

        private String id;
        private String fsh;
        private String vsh;
        private boolean timeRelated;
        private boolean sizeRelated;
        private String type;
        private String effectValue;
        private String noise;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getFsh() {
            if (!TextUtils.isEmpty(fsh) && fsh.contains("}"))
                fsh = fsh.substring(0, fsh.lastIndexOf("}") + 1);
            return fsh;
        }

        public void setFsh(String fsh) {
            this.fsh = fsh;
        }

        public String getVsh() {
            if (!TextUtils.isEmpty(vsh) && vsh.contains("}"))
                vsh = vsh.substring(0, vsh.lastIndexOf("}") + 1);
            return vsh;
        }

        public void setVsh(String vsh) {
            this.vsh = vsh;
        }

        public boolean isTimeRelated() {
            return timeRelated;
        }

        public void setTimeRelated(boolean timeRelated) {
            this.timeRelated = timeRelated;
        }

        public boolean isSizeRelated() {
            return sizeRelated;
        }

        public void setSizeRelated(boolean sizeRelated) {
            this.sizeRelated = sizeRelated;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getEffectValue() {
            return effectValue;
        }

        public void setEffectValue(String effectValue) {
            this.effectValue = effectValue;
        }

        public String getNoise() {
            return noise;
        }

        public void setNoise(String noise) {
            this.noise = noise;
        }
    }
}

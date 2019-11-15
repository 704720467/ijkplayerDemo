package com.zp.libvideoedit.GPUImage.Filter;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import com.zp.libvideoedit.Constants;
import com.zp.libvideoedit.GPUImage.Core.GPUImageContext;
import com.zp.libvideoedit.GPUImage.Core.GPUImageFrameBuffer;
import com.zp.libvideoedit.GPUImage.Core.GPUImageTextureCoordinates;
import com.zp.libvideoedit.GPUImage.Core.GPUSize;
import com.zp.libvideoedit.GPUImage.Core.GPUUtiles;
import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageFilter;
import com.zp.libvideoedit.utils.BitmapUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;

/**
 * 特效
 * Created by zp on 2019/5/10.
 */

public class VNISpecialEffectsFilterTest extends GPUImageFilter {

    protected static final String AlphaBlendFilterVertexShaderString = "" +
            "attribute vec4 position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            "varying vec2 textureCoordinate;\n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = position;\n" +
            "    textureCoordinate = inputTextureCoordinate.xy;\n" +
            "}";

    //    private static final String AlphaBlendFilterFragment =
//            " varying highp vec2 textureCoordinate;\n" +
//                    " \n" +
//                    " uniform sampler2D inputImageTexture;\n" +
//                    " \n" +
//                    " void main()\n" +
//                    " {\n" +
//                    "     gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
//                    " }";
    private static final String AlphaBlendFilterFragment =
            "////// Fragment Shader\n" +
                    "varying highp vec2 textureCoordinate;\n" +
                    "uniform sampler2D inputImageTexture;\n" +
                    "uniform highp float iTime;\n" +
                    "uniform highp vec2 inputSize;\n" +
                    "uniform sampler2D noiseTexture;\n" +
                    "\n" +
                    "uniform highp float effectValue;\n" +
                    "\n" +
                    "const highp float tau = 6.28318530717958647692;\n" +
                    "\n" +
                    "// Gamma correction\n" +
                    "#define GAMMA (2.2)\n" +
                    "\n" +
                    "highp vec3 ToLinear(in highp vec3 col)\n" +
                    "{\n" +
                    "    // simulate a monitor, converting colour values into light values\n" +
                    "    return pow(abs(col), vec3(GAMMA));\n" +
                    "}\n" +
                    "\n" +
                    "highp vec3 ToGamma(in highp vec3 col)\n" +
                    "{\n" +
                    "    // convert back into colour values, so the correct light will come out of the monitor\n" +
                    "    return pow(abs(col), vec3(1.0/GAMMA));\n" +
                    "}\n" +
                    "\n" +
                    "highp vec4 Noise(in ivec2 x)\n" +
                    "{\n" +
                    "    return texture2D(noiseTexture, fract((vec2(x)+0.5)/256.0));\n" +
                    "}\n" +
                    "\n" +
                    "highp vec4 Rand(in int x)\n" +
                    "{\n" +
                    "    highp vec2 uv;\n" +
                    "    uv.x = (float(x)+0.5)/1.0;\n" +
                    "    uv.y = (floor(uv.x)+0.5)/1.0;\n" +
                    "    return texture2D(noiseTexture, uv);\n" +
                    "}\n" +
                    "\n" +
                    "void main()\n" +
                    "{\n" +
                    "    highp vec3 ray;\n" +
                    "    ray.xy = 2.0*(inputSize*textureCoordinate -inputSize.xy*.5)/inputSize.x;\n" +
                    "    ray.z = 1.0;\n" +
                    "\n" +
                    "    highp float offset = iTime*.5;\n" +
                    "    highp  float speed2 = (cos(offset)+1.0)*2.0;\n" +
                    "    highp float speed = speed2+.1;\n" +
                    "    offset += sin(offset)*.96;\n" +
                    "    offset *= 2.0;\n" +
                    "    highp vec3 col = vec3(0);\n" +
                    "    highp vec3 stp = ray/max(abs(ray.x), abs(ray.y));\n" +
                    "    int count = 5;\n" +
                    "    highp vec3 pos = 2.0*stp+.5;\n" +
                    "    for (int i=0; i < count; i++)\n" +
                    "    {\n" +
                    "        highp float z = Noise(ivec2(pos.xy)).x;\n" +
                    "        z = fract(z-offset);\n" +
                    "        highp float d = 50.0*z-pos.z;\n" +
                    "        highp float w = pow(max(0.0, 1.0-12.0*length(fract(pos.xy)-.5)), 2.0);\n" +
                    "        highp vec3 c = vec3(0.0);\n" +
                    "        c = max(vec3(0), vec3(1.0-abs(d+speed2*.5)/speed, 1.0-abs(d)/speed, 1.0-abs(d-speed2*.5)/speed));\n" +
                    "\n" +
                    "        col += 1.5*(1.0-z)*c*w;\n" +
                    "        pos += stp;\n" +
                    "    }\n" +
                    "\n" +
                    "    col = ToGamma(col);\n" +
                    "    highp vec3 c = texture2D(inputImageTexture, textureCoordinate).rgb;\n" +
                    "\n" +
                    "    gl_FragColor.rgb = c + col;\n" +
                    "    gl_FragColor.a = 1.0;\n" +
                    "}";


    private String key = "time";
    protected int parameterLocal = -1;
    private float parameterValue = 0;
    protected int matterTexture;

    private int effectValue;
    private int inputSize;
    private Context context;

    public VNISpecialEffectsFilterTest(String effectJson, Context context) {
        super(AlphaBlendFilterVertexShaderString, AlphaBlendFilterFragment);
        this.context = context;
//        if (!TextUtils.isEmpty(effectJson)) {
//            specialEffectModel = JsonUtil.parseJsonToBean(effectJson, SpecialEffectModel.class);
//            vertextShaderString = TextUtils.isEmpty(specialEffectModel.getVsh()) ? AlphaBlendFilterVertexShaderString : specialEffectModel.getVsh();
//            fragmentShaderString = TextUtils.isEmpty(specialEffectModel.getFsh()) ? AlphaBlendFilterFragment : specialEffectModel.getFsh();
//            this.key = specialEffectModel.getKey();
//        }
    }

    @Override
    public void init() {
        super.init();
//        if (!TextUtils.isEmpty(key))
        parameterLocal = mFilterProgram.uniformIndex("iTime");
        effectValue = mFilterProgram.uniformIndex("effectValue");
        setFloat(effectValue, 0.5f);
        inputSize = mFilterProgram.uniformIndex("inputSize");

        if (true) {//只有包含了"noiseTexture"并且有对应的纹理，采取加载
            matterTexture = mFilterProgram.uniformIndex("noiseTexture");
            bitmap = BitmapUtil.getImageFromAssetsFile(context, "effect/filter_snow_noise.png");
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
        setFloatVec2(inputSize, new float[]{mPixelSizeOfImage.width, mPixelSizeOfImage.height});
        if (mFirstInputFramebuffer != null) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFirstInputFramebuffer.getTexture());
            if (parameterLocal != -1)
                GLES20.glUniform1f(parameterLocal, parameterValue);
            GLES20.glUniform1i(mFilterInputTextureUniform, 2);
        }
        if (picGp != null) {
//            GLES20.glEnable(GLES20.GL_BLEND);
//            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, picGp.getTexture());
            GLES20.glUniform1i(matterTexture, 3);
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
        GLES20.glDisable(GLES20.GL_BLEND);
    }

    /**
     * 设置特效的时间
     *
     * @param parameterValue
     */
    public void setParameterValue(float parameterValue) {
        this.parameterValue = parameterValue;
    }

    private GPUSize mPixelSizeOfImage = null;
    private Bitmap bitmap;
    private boolean isInited = false;
    private GPUImageFrameBuffer picGp;

    public void initPic() {
        if (!isInited) {
            mPixelSizeOfImage = new GPUSize(mPixelSizeOfImage.width, mPixelSizeOfImage.height);
            picGp = new GPUImageFrameBuffer(mPixelSizeOfImage, this.mOutputTextureOptions, true); // GPUImageContext.sharedFramebufferCache().fetchFramebufferForSize(mPixelSizeOfImage, true);
            picGp.disableReferenceCounting();
            picGp.activeFramebuffer();
            GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, picGp.getTexture());
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            GLES20.glFlush();
            isInited = true;
        }
    }

    /**
     * 根据路径获取Bitmap图片
     *
     * @param context
     * @param path
     * @return
     */
    public static Bitmap getAssetsBitmap(Context context, String path) {
        AssetManager am = context.getAssets();
        InputStream inputStream = null;
        try {
            inputStream = am.open(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        return bitmap;
    }

}

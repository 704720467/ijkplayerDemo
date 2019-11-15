package com.zp.libvideoedit.GPUImage.Filter;

import android.opengl.GLES20;

import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageFilter;

/**
 * Created by gwd on 2018/4/9.
 */

public class GPUImageBrightnessFilter extends GPUImageFilter {
    public static final String BRIGHTNESS_FRAGMENT_SHADER = "" +
            "varying highp vec2 textureCoordinate;\n" +
            " \n" +
            " uniform sampler2D inputImageTexture;\n" +
            " uniform lowp float brightness;\n" +
            " \n" +
            " void main()\n" +
            " {\n" +
            "     lowp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "     \n" +
            "     gl_FragColor = vec4((textureColor.rgb + vec3(brightness)), textureColor.w);\n" +
            " }";

    private int mBrightnessLocation;
    private float mBrightness;

    public GPUImageBrightnessFilter() {
        this(0.0f);
    }

    public GPUImageBrightnessFilter(final float brightness) {
        super(kGPUImageVertexShaderString, BRIGHTNESS_FRAGMENT_SHADER);
        mBrightness = brightness;
    }

    @Override
    public void init() {
        super.init();
        mBrightnessLocation = GLES20.glGetUniformLocation(getProgram(), "brightness");
        setBrightness(mBrightness);

    }

    public void setBrightness(final float brightness) {
        mBrightness = brightness;
        setFloat(mBrightnessLocation, mBrightness);
    }
}


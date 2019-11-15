package com.zp.libvideoedit.GPUImage.Filter;

import android.opengl.GLES20;

import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageFilter;

/**
 * Created by gwd on 2018/4/9.
 */

public class GPUImageSaturationFilter extends GPUImageFilter {
    public static final String SATURATION_FRAGMENT_SHADER = "" +
            " varying highp vec2 textureCoordinate;\n" +
            " \n" +
            " uniform sampler2D inputImageTexture;\n" +
            " uniform lowp float saturation;\n" +
            " \n" +
            " // Values from \"Graphics Shaders: Theory and Practice\" by Bailey and Cunningham\n" +
            " const mediump vec3 luminanceWeighting = vec3(0.2125, 0.7154, 0.0721);\n" +
            " \n" +
            " void main()\n" +
            " {\n" +
            "    lowp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "    lowp float luminance = dot(textureColor.rgb, luminanceWeighting);\n" +
            "    lowp vec3 greyScaleColor = vec3(luminance);\n" +
            "    \n" +
            "    gl_FragColor = vec4(mix(greyScaleColor, textureColor.rgb, saturation), textureColor.w);\n" +
            "     \n" +
            " }";

    private int mSaturationLocation;
    private float mSaturation;

    public GPUImageSaturationFilter() {
        this(1.0f);
    }

    public GPUImageSaturationFilter(final float saturation) {
        super(kGPUImageVertexShaderString, SATURATION_FRAGMENT_SHADER);
        mSaturation = saturation;
    }

    @Override
    public void init() {
        super.init();
        mSaturationLocation = GLES20.glGetUniformLocation(getProgram(), "saturation");
        setSaturation(mSaturation);
    }

    public void setSaturation(final float saturation) {
        mSaturation = saturation;
        setFloat(mSaturationLocation, mSaturation);
    }

}

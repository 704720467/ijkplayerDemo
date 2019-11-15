package com.zp.libvideoedit.GPUImage.Filter;

import android.opengl.GLES20;

import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageFilter;


/**
 * Created by gwd on 2018/4/9.
 */

public class GPUImageContrastFilter extends GPUImageFilter {
    public static final String CONTRAST_FRAGMENT_SHADER = "" +
            "varying highp vec2 textureCoordinate;\n" +
            " \n" +
            " uniform sampler2D inputImageTexture;\n" +
            " uniform lowp float contrast;\n" +
            " \n" +
            " void main()\n" +
            " {\n" +
            "     lowp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "     \n" +
            "     gl_FragColor = vec4(((textureColor.rgb - vec3(0.5)) * contrast + vec3(0.5)), textureColor.w);\n" +
            " }";

    private int mContrastLocation;
    private float mContrast;

    public GPUImageContrastFilter() {
        this(1.2f);
    }

    public GPUImageContrastFilter(float contrast) {
        super(kGPUImageVertexShaderString, CONTRAST_FRAGMENT_SHADER);
        mContrast = contrast;
    }

    @Override
    public void init() {
        super.init();
        mContrastLocation = GLES20.glGetUniformLocation(getProgram(), "contrast");
        setContrast(mContrast);
    }

    public void setContrast(final float contrast) {
        mContrast = contrast;
        setFloat(mContrastLocation, mContrast);
    }
}

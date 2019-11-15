package com.zp.libvideoedit.GPUImage.Filter;

/**
 * Created by gwd on 2018/7/25.
 */

public class VNICrossDissolveTransitionFilter extends GPUImageTransitionFilter {


    public VNICrossDissolveTransitionFilter() {
        super(kVNICrossDissolveTransitionFilterFragmentShaderString);
    }

    @Override
    public void init() {
        super.init();
        progressUniform = mFilterProgram.uniformIndex("progress");
        setProgress(0.f);
    }

    private static final String kVNICrossDissolveTransitionFilterFragmentShaderString = "varying highp vec2 textureCoordinate;\n" +
            " varying highp vec2 textureCoordinate2;\n" +
            " \n" +
            " uniform sampler2D inputImageTexture;\n" +
            " uniform sampler2D inputImageTexture2;\n" +
            " \n" +
            " uniform lowp float progress;\n" +
            " \n" +
            " void main()\n" +
            " {\n" +
            "     lowp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "     lowp vec4 textureColor2 = texture2D(inputImageTexture2, textureCoordinate2);\n" +
            "     gl_FragColor = mix(textureColor, textureColor2, progress);\n" +
            " }";

    @Override
    public void setProgress(float progress) {
        this.progress = Math.max(0.f, progress);
        this.progress = Math.min(1.f, this.progress);
        setFloat(progressUniform, this.progress);
    }

}

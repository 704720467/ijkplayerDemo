package com.zp.libvideoedit.GPUImage.Filter;

/**
 * Created by gwd on 2018/7/25.
 */

public class VNITransitionTypeBlackFade extends GPUImageTransitionFilter {
    public VNITransitionTypeBlackFade() {
        super(kVNIColorFadeTransitionFilterShaderString);
    }


    @Override
    public void init() {
        super.init();
        progressUniform = mFilterProgram.uniformIndex("progress");
        fadeColorUniform = mFilterProgram.uniformIndex("fadeColor");
        setProgress(0.f);
        float[] defaultColor = {0.f, 0.f, 0.f, 0.f};
        setFadeColor(defaultColor);
    }

    public void setFadeColor(float[] fadeColor) {
        this.fadeColor = fadeColor;
        setFloatVec4(fadeColorUniform, this.fadeColor);
    }

    @Override
    public void setProgress(float progress) {
        this.progress = Math.max(0.f, progress);
        this.progress = Math.min(1.f, this.progress);
        setFloat(progressUniform, this.progress);

    }

    public static final String kVNIColorFadeTransitionFilterShaderString = "varying highp vec2 textureCoordinate;\n" +
            " varying highp vec2 textureCoordinate2;\n" +
            " uniform sampler2D inputImageTexture;\n" +
            " uniform sampler2D inputImageTexture2;\n" +
            " uniform highp float progress;\n" +
            " uniform highp vec4 fadeColor;\n" +
            " \n" +
            " void main()\n" +
            "{\n" +
            "    if (progress < 0.5) {\n" +
            "        gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "        gl_FragColor = fadeColor*(1.-(0.5-progress)*2.) + (0.5-progress)*2.*gl_FragColor;\n" +
            "    }else{\n" +
            "        gl_FragColor = texture2D(inputImageTexture2, textureCoordinate2);\n" +
            "        gl_FragColor = fadeColor*(1.-(progress-0.5)*2.) + (progress-0.5)*2.*gl_FragColor;\n" +
            "    }\n" +
            "}";

    private int fadeColorUniform;
    private float[] fadeColor;
}

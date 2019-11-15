package com.zp.libvideoedit.GPUImage.Filter;

import android.opengl.GLES20;

import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageThreeInputFilter;

/**
 * Created by gwd on 2018/5/10.
 */

public class VNiVideoBlendFilter extends GPUImageThreeInputFilter {
    public VNiVideoBlendFilter(String fragmentShader) {
        super(fragmentShader);
    }
    public VNiVideoBlendFilter(){
        super(VNIImageVideoBlendMattingFragmentShaderStringss);
    }

    @Override
    public void init() {
        super.init();
        maskAlphaUniform = mFilterProgram.uniformIndex("maskAlpha");
        GLES20.glUniform1f(maskAlphaUniform,1.0f);
    }



    protected static final String VNIImageVideoBlendMattingFragmentShaderStringss =
            " varying highp vec2 textureCoordinate;\n" +
            " varying highp vec2 textureCoordinate2;\n" +
            " varying highp vec2 textureCoordinate3;\n" +
            " \n" +
            " uniform sampler2D inputImageTexture;\n" +
            " uniform sampler2D inputImageTexture2;\n" +
            " uniform sampler2D inputImageTexture3;\n" +
            " uniform lowp float maskAlpha;\n" +
            " \n" +
            " void main()\n" +
            " {\n" +
            "     lowp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate); //素材视频\n" +
            "     lowp vec4 textureColor2 = texture2D(inputImageTexture2, textureCoordinate2);//原来mask视频\n" +
            "     lowp vec4 textureColor3 = texture2D(inputImageTexture3, textureCoordinate3);//黑白视频\n" +
            "     lowp vec4 tmpColor = vec4(\n" +
            "                               (textureColor3.r==0.0?0.0:min((textureColor2.r/textureColor3.r), 1.0)),\n" +
            "                               (textureColor3.g==0.0?0.0:min((textureColor2.g/textureColor3.r), 1.0)),\n" +
            "                               (textureColor3.b==0.0?0.0:min((textureColor2.b/textureColor3.r), 1.0)),\n" +
            "                               textureColor3.r );\n" +
            "     gl_FragColor = mix(textureColor, tmpColor, tmpColor.a) ;\n" +
//                    "     gl_FragColor = vec4(0.0,0.0,0.0,0.0) ;\n" +


                    " }";
    private int maskAlphaUniform ;
}

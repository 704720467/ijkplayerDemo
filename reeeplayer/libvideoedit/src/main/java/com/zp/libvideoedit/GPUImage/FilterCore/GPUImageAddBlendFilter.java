package com.zp.libvideoedit.GPUImage.FilterCore;

/**
 * Created by gwd on 2018/3/17.
 */

public class GPUImageAddBlendFilter extends GPUImageTwoInput {

    public GPUImageAddBlendFilter() {
        super(kGPUImageAddBlendFragmentShaderString);
    }
    public static final String kGPUImageAddBlendFragmentShaderString = "" +
            "varying highp vec2 textureCoordinate;\n"+
            "varying highp vec2 textureCoordinate2;\n"+
            "uniform sampler2D inputImageTexture;\n"+
            "uniform sampler2D inputImageTexture2;\n"+
            "uniform lowp float intensity;\n"+
            " void main()\n"+
            "{\n"+
            "highp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n"+
            "highp vec4 textureColor2 = texture2D(inputImageTexture2, textureCoordinate2);\n"+
            " gl_FragColor = vec4(textureColor.r+textureColor2.r,textureColor.g+textureColor2.g,textureColor.b+textureColor2.b,textureColor.a*0.5+textureColor2.a*0.5);\n"+
            "}\n";



}

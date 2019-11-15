package com.zp.libvideoedit.GPUImage.FilterCore;

/**
 * Created by gwd on 2018/2/27.
 */

public class GPUImageLookupFilter extends GPUImageTwoInput {

    public GPUImageLookupFilter() {
        super(kGPUImagePassthroughFragmentShaderString);
    }

    public int intensityUniform ;
    public static final String kGPUImagePassthroughFragmentShaderString = "" +
            "varying highp vec2 textureCoordinate;\n"+
            "varying highp vec2 textureCoordinate2;\n"+
            "uniform sampler2D inputImageTexture;\n"+
            "uniform sampler2D inputImageTexture2;\n"+
            "uniform lowp float intensity;\n"+
            " void main()\n"+
            "{\n"+
            "highp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n"+
            "highp vec4 textureColor2 = texture2D(inputImageTexture2, textureCoordinate2);\n"+

            "highp float redColor = textureColor.r * 63.0;\n"+

            "highp vec2 quad1;\n"+
            "quad1.y = floor(floor(redColor) / 8.0) ;\n"+
            " quad1.x = floor(redColor) - (quad1.y * 8.0) ;\n"+
            "highp vec2 quad2;\n"+
            " quad2.y = floor(ceil(redColor) / 8.0);\n"+
            "quad2.x = ceil(redColor) - (quad2.y * 8.0);\n"+
            "highp vec2 texPos1;\n"+
            "texPos1.x = (quad1.x * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.g);\n"+
            "texPos1.y = (quad1.y * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.b);\n"+
            " highp vec2 texPos2;\n"+
            "texPos2.x = (quad2.x * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.g);\n"+
            "texPos2.y = (quad2.y * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.b);\n"+
            "lowp vec4 newColor1 = texture2D(inputImageTexture2, texPos1);\n"+
            "lowp vec4 newColor2 = texture2D(inputImageTexture2, texPos2);\n"+
            "lowp vec4 newColor = mix(newColor1, newColor2, fract(redColor));\n"+
            " gl_FragColor = mix(textureColor, vec4(newColor.rgb, textureColor.w), 1.0);\n"+
            "}\n";
}

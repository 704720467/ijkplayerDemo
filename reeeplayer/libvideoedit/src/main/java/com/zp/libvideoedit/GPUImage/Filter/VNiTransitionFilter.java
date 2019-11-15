package com.zp.libvideoedit.GPUImage.Filter;

import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageThreeInputFilter;

/**
 * Created by gwd on 2018/5/11.
 */

public class VNiTransitionFilter extends GPUImageThreeInputFilter {
    public VNiTransitionFilter(String fragmentShader) {
        super(fragmentShader);
    }

    public VNiTransitionFilter() {
        super(kTransitionFilterVertexShaderString);
    }

    private static final String kTransitionFilterVertexShaderString =
            " varying highp vec2 textureCoordinate;\n" +
                    " varying highp vec2 textureCoordinate2;\n" +
                    "  varying highp vec2 textureCoordinate3;\n" +
                    " \n" +
                    " uniform sampler2D inputImageTexture;\n" +
                    " uniform sampler2D inputImageTexture2;\n" +
                    "  uniform sampler2D inputImageTexture3;\n" +
                    " void main()\n" +
                    " {\n" +
                    "     highp vec4 color  = texture2D(inputImageTexture, textureCoordinate);\n" +
                    "     highp vec4 color2  = texture2D(inputImageTexture2, textureCoordinate2);\n" +
                    "     highp vec4 color3  = texture2D(inputImageTexture3, textureCoordinate3);\n" +
                    "     gl_FragColor = mix(color2, color, 1.0 -color3.r);\n" +
                    "\n" +
                    "}";
}

package com.zp.libvideoedit.GPUImage.Filter;

import com.zp.libvideoedit.modle.Transition.VNITransitionRotateType;
import com.zp.libvideoedit.utils.VNIInterpolator;

/**
 * Created by gwd on 2018/7/25.
 */

public class VNISmoothRotateTransitionFilter extends GPUImageTransitionFilter {
    VNIInterpolator vniInterpolator;

    public VNISmoothRotateTransitionFilter() {
        super(kVNISmoothRotateTransitionFilterShaderString);
        vniInterpolator = new VNIInterpolator();

    }

    @Override
    public void init() {
        super.init();

        progressUniform = mFilterProgram.uniformIndex("progress");
        typeUniform = mFilterProgram.uniformIndex("type");
        ratioUniform = mFilterProgram.uniformIndex("ratio");
        setProgress(0.f);
        setRatio(16.f / 9.f);
        setType(VNITransitionRotateType.VNITransitionRotateTypeRight);
    }

    private float beforeVvalue = 0;
    private float afterValue = 0;


    @Override
    public void setProgress(float progress) {
        this.progress = Math.max(0.f, progress);
        this.progress = vniInterpolator.valueForX(this.progress);
        this.progress = Math.min(1.f, this.progress);
        setFloat(progressUniform, this.progress);
    }

    public void setType(VNITransitionRotateType type) {
        this.type = type;
        setInteger(typeUniform, type.getValue());
    }

    public void setRatio(float ratio) {
        this.ratio = ratio;
        setFloat(ratioUniform, ratio);

    }

    private static final String kVNISmoothRotateTransitionFilterShaderString = "precision highp float;\n" +
            " varying highp vec2 textureCoordinate;\n" +
            " varying highp vec2 textureCoordinate2;\n" +
            " uniform highp float progress;\n" +
            " uniform highp int type;\n" +
            " uniform float ratio;\n" +
            " uniform sampler2D inputImageTexture;\n" +
            " uniform sampler2D inputImageTexture2;\n" +
            " \n" +
            " const float PI = 3.141592653589793;\n" +
            " \n" +
            " float Sinusoidal_easeInOut(in float begin, in float change, in float duration, in float time) {\n" +
            "     if (time <= duration){\n" +
            "         return time*change/duration;\n" +
            "     }else{\n" +
            "         return (duration*change*2.0 - change*time)/duration;\n" +
            "     }\n" +
            " }\n" +
            " \n" +
            " float random(in vec3 scale, in float seed) {\n" +
            "     return fract(sin(dot(gl_FragCoord.xyz + seed, scale)) * 43758.5453 + seed);\n" +
            " }\n" +
            " \n" +
            " vec3 crossFade(in vec2 uv, in float pr) {\n" +
            "     uv = vec2(abs(1.0 - abs(uv.x - 1.0)),abs(1.0 - abs(uv.y - 1.0)));\n" +
            "     return mix(texture2D(inputImageTexture, uv).rgb, texture2D(inputImageTexture2, uv).rgb, step(0.5, pr));\n" +
            " }\n" +
            " \n" +
            " void main()\n" +
            "{\n" +
            "    float strength = Sinusoidal_easeInOut(0.0, 0.3, 0.5, progress);\n" +
            "    vec3 color = vec3(0.0);\n" +
            "    float total = 0.0;\n" +
            "    float offset = random(vec3(12.9898, 78.233, 151.7182), 0.0);\n" +
            "    \n" +
            "    vec2 coord = textureCoordinate;\n" +
            "    vec2 co = vec2(100.0,100.0/ratio);\n" +
            "    vec2 fco = vec2(co.x*coord.x,co.y*coord.y);\n" +
            "    float sin_factor;\n" +
            "    float cos_factor;\n" +
            "    \n" +
            "    if (type == 0) {\n" +
            "        if(progress <= 0.5) {\n" +
            "            sin_factor = sin(-90.0*progress*2.0/180.0*PI);\n" +
            "            cos_factor = cos(-90.0*progress*2.0/180.0*PI);\n" +
            "        }else {\n" +
            "            sin_factor = sin(-90.0*(-1.0 + (progress-0.5)*2.0)/180.0*PI);\n" +
            "            cos_factor = cos(-90.0*(-1.0 + (progress-0.5)*2.0)/180.0*PI);\n" +
            "        }\n" +
            "    }else {\n" +
            "        if(progress <= 0.5) {\n" +
            "            sin_factor = sin(90.0*progress*2.0/180.0*PI);\n" +
            "            cos_factor = cos(90.0*progress*2.0/180.0*PI);\n" +
            "        }else {\n" +
            "            sin_factor = sin(90.0*(-1.0 + (progress-0.5)*2.0)/180.0*PI);\n" +
            "            cos_factor = cos(90.0*(-1.0 + (progress-0.5)*2.0)/180.0*PI);\n" +
            "        }\n" +
            "    }\n" +
            "    \n" +
            "    coord = vec2((fco.x - co.x/2.0)*cos_factor - (fco.y - co.y/2.0)*sin_factor + co.x/2.0, (fco.x - co.x/2.0)*sin_factor + (fco.y - co.y/2.0)*cos_factor + co.y/2.0);\n" +
            "    \n" +
            "    for (float t = 0.0; t <= 20.0; t++) {\n" +
            "        float percent = (t + offset) / 20.0;\n" +
            "        float weight = 4.0 * (percent - percent * percent);\n" +
            "        float sin = sin(90.0* percent * strength*2.0/180.0*PI);\n" +
            "        float cos = cos(90.0* percent * strength*2.0/180.0*PI);\n" +
            "        vec2 nextCoord = vec2((coord.x - co.x/2.0)*cos - (coord.y - co.y/2.0)*sin + co.x/2.0, (coord.x - co.x/2.0)*sin + (coord.y - co.y/2.0)*cos + co.y/2.0);\n" +
            "        nextCoord = vec2(nextCoord.x/co.x,nextCoord.y/co.y);\n" +
            "        color += crossFade(nextCoord, progress) * weight;\n" +
            "        total += weight;\n" +
            "    }\n" +
            "    \n" +
            "    gl_FragColor = vec4(color / total, 1.0);\n" +
            "}";
    private int typeUniform;
    private int ratioUniform;
    public VNITransitionRotateType type;
    public float ratio;
}

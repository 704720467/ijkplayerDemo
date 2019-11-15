package com.zp.libvideoedit.GPUImage.Filter;


import com.zp.libvideoedit.modle.Transition.VNITransitionZoomType;
import com.zp.libvideoedit.utils.VNIInterpolator;

/**
 * Created by gwd on 2018/7/25.
 */

public class VNISmoothZoomTransitionFilter extends GPUImageTransitionFilter {
    VNIInterpolator vniInterpolator;

    public VNISmoothZoomTransitionFilter() {
        super(kVNISmoothZoomTransitionFilterShaderString);
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
        setType(VNITransitionZoomType.getStyle(0));
    }


    public void setRatio(float ratio) {
        this.ratio = ratio;
        setFloat(ratioUniform, ratio);
    }

    public void setType(VNITransitionZoomType type) {
        this.type = type;
        setInteger(typeUniform, type.getValue());
    }

    @Override
    public void setProgress(float progress) {
        this.progress = Math.max(0.f, progress);
        this.progress = vniInterpolator.valueForX(this.progress);
        this.progress = Math.min(1.f, this.progress);
        setFloat(progressUniform, this.progress);
    }


    private static final String kVNISmoothZoomTransitionFilterShaderString = "varying highp vec2 textureCoordinate;\n" +
            " varying highp vec2 textureCoordinate2;\n" +
            " \n" +
            " uniform sampler2D inputImageTexture;\n" +
            " uniform sampler2D inputImageTexture2;\n" +
            "\n" +
            " precision highp float;\n" +
            " uniform highp float progress;\n" +
            " uniform highp int type;\n" +
            " uniform float ratio;\n" +
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
            "    vec2 coord = textureCoordinate;\n" +
            "    vec2 co = vec2(100.0,100.0/ratio);\n" +
            "    vec2 fco = vec2(co.x*coord.x,co.y*coord.y);\n" +
            "    \n" +
            "    if (type == 0) {\n" +
            "        float zoomOut = 0.5;\n" +
            "        float zoomIn = 1.5;\n" +
            "        \n" +
            "        if(progress <= 0.5) {\n" +
            "            if(fco.x <= co.x/2.0) {\n" +
            "                fco.x=co.x/2.0 - (co.x/2.0 - fco.x)/(1.0 - (1.0 - zoomOut)*progress*2.0);\n" +
            "            }else{\n" +
            "                fco.x=co.x/2.0 + (fco.x - co.x/2.0)/(1.0 - (1.0 - zoomOut)*progress*2.0);\n" +
            "            }\n" +
            "            if(fco.y <= co.y/2.0) {\n" +
            "                fco.y=co.y/2.0 - (co.y/2.0 - fco.y)/(1.0 - (1.0 - zoomOut)*progress*2.0);\n" +
            "            }else{\n" +
            "                fco.y=co.y/2.0 + (fco.y - co.y/2.0)/(1.0 - (1.0 - zoomOut)*progress*2.0);\n" +
            "            }\n" +
            "            coord = fco;\n" +
            "            coord = vec2(coord.x/co.x,coord.y/co.y);\n" +
            "        }else {\n" +
            "            if(fco.x <= co.x/2.0) {\n" +
            "                fco.x=co.x/2.0 - (co.x/2.0 - fco.x)/(zoomIn - (zoomIn - 1.0)*(progress-0.5)/0.5);\n" +
            "            }else{\n" +
            "                fco.x=co.x/2.0 + (fco.x - co.x/2.0)/(zoomIn - (zoomIn - 1.0)*(progress-0.5)/0.5);\n" +
            "            }\n" +
            "            if(fco.y <= co.y/2.0) {\n" +
            "                fco.y=co.y/2.0 - (co.y/2.0 - fco.y)/(zoomIn - (zoomIn - 1.0)*(progress-0.5)/0.5);\n" +
            "            }else{\n" +
            "                fco.y=co.y/2.0 + (fco.y - co.y/2.0)/(zoomIn - (zoomIn - 1.0)*(progress-0.5)/0.5);\n" +
            "            }\n" +
            "            coord = fco;\n" +
            "            coord = vec2(coord.x/co.x,coord.y/co.y);\n" +
            "        }\n" +
            "    }else{\n" +
            "        float zoomOut = 0.5;\n" +
            "        float zoomIn = 1.3;\n" +
            "        \n" +
            "        if(progress <= 0.5) {\n" +
            "            if(fco.x <= co.x/2.0) {\n" +
            "                fco.x=co.x/2.0 - (co.x/2.0 - fco.x)/(1.0 + (zoomIn - 1.0)*progress*2.0);\n" +
            "            }else{\n" +
            "                fco.x=co.x/2.0 + (fco.x - co.x/2.0)/(1.0 + (zoomIn - 1.0)*progress*2.0);\n" +
            "            }\n" +
            "            if(fco.y <= co.y/2.0) {\n" +
            "                fco.y=co.y/2.0 - (co.y/2.0 - fco.y)/(1.0 + (zoomIn - 1.0)*progress*2.0);\n" +
            "            }else{\n" +
            "                fco.y=co.y/2.0 + (fco.y - co.y/2.0)/(1.0 + (zoomIn - 1.0)*progress*2.0);\n" +
            "            }\n" +
            "            coord = fco;\n" +
            "            coord = vec2(coord.x/co.x,coord.y/co.y);\n" +
            "        }else {\n" +
            "            if(fco.x <= co.x/2.0) {\n" +
            "                fco.x=co.x/2.0 - (co.x/2.0 - fco.x)/(zoomOut + (1.0 - zoomOut)*(progress-0.5)/0.5);\n" +
            "            }else{\n" +
            "                fco.x=co.x/2.0 + (fco.x - co.x/2.0)/(zoomOut + (1.0 - zoomOut)*(progress-0.5)/0.5);\n" +
            "            }\n" +
            "            if(fco.y <= co.y/2.0) {\n" +
            "                fco.y=co.y/2.0 - (co.y/2.0 - fco.y)/(zoomOut + (1.0 - zoomOut)*(progress-0.5)/0.5);\n" +
            "            }else{\n" +
            "                fco.y=co.y/2.0 + (fco.y - co.y/2.0)/(zoomOut + (1.0 - zoomOut)*(progress-0.5)/0.5);\n" +
            "            }\n" +
            "            coord = fco;\n" +
            "            coord = vec2(coord.x/co.x,coord.y/co.y);\n" +
            "        }\n" +
            "    }\n" +
            "    \n" +
            "    vec2 toCenter = vec2(0.5,0.5) - coord;\n" +
            "    \n" +
            "    for (float t = 0.0; t <= 20.0; t++) {\n" +
            "        float percent = (t + offset) / 20.0;\n" +
            "        float weight = 4.0 * (percent - percent * percent);\n" +
            "        color += crossFade(coord + toCenter * percent * strength, progress) * weight;\n" +
            "        total += weight;\n" +
            "    }\n" +
            "    gl_FragColor = vec4(color / total, 1.0);\n" +
            "}";
    private int typeUniform;
    private int ratioUniform;
    public VNITransitionZoomType type;
    public float ratio;
}

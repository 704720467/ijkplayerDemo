package com.zp.libvideoedit.GPUImage.Filter;

import android.content.Context;
import android.renderscript.Matrix4f;
import android.text.TextUtils;
import android.util.Log;


import com.zp.libvideoedit.Effect.VNiImageFilter;
import com.zp.libvideoedit.GPUImage.Carma.Core.GPUSurfaceCameraView;
import com.zp.libvideoedit.GPUImage.Core.GPUImageFilterPipeline;
import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageFilter;
import com.zp.libvideoedit.GPUImage.FilterCore.GPUImageTransformFilter;
import com.zp.libvideoedit.Time.CMTime;
import com.zp.libvideoedit.modle.Chunk;
import com.zp.libvideoedit.modle.effectModel.EffectAdapter;
import com.zp.libvideoedit.modle.effectModel.EffectType;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by gwd on 2018/4/9.
 */

public class VNiFilterManager extends GPUImageFilterPipeline {
    public VNIImageVideoInputFilter videoInputFilter = null;
    public GPUImageTransformFilter transformFilter = null;
    public GPUImageSaturationFilter saturationFilter = null;
    public GPUImageWhiteBalanceFilter whiteBalanceFilter = null;
    public GPUImageBrightnessFilter brightnessFilter = null;
    public GPUImageContrastFilter contrastFilter = null;
    public GPUImageHighlightShadowFilter highLightShadowFilter = null;
    public VNiImageFilter colorFilter;
    public GPUImageFilter outputFilter;
    public String colorFilterName = "";
    private Context context;
    private boolean isInited = false;

    private HashMap<String, VNISpecialEffectsFilter> specialEffectsFilterHashMap;
    private ArrayList<EffectAdapter> effectAdapters;


    public VNiFilterManager(Context context) {
        super();
        this.videoInputFilter = new VNIImageVideoInputFilter();
        this.transformFilter = new GPUImageTransformFilter();
        this.outputFilter = new GPUImageFilter();
        this.saturationFilter = new GPUImageSaturationFilter();
        this.whiteBalanceFilter = new GPUImageWhiteBalanceFilter();
        this.brightnessFilter = new GPUImageBrightnessFilter();
        this.contrastFilter = new GPUImageContrastFilter();
        this.highLightShadowFilter = new GPUImageHighlightShadowFilter();
        this.inputfilter = this.videoInputFilter;
        this.output = this.outputFilter;
        this.context = context;
    }

    public void removeAllTargets() {
        this.output.removeAllTargets();
    }

    private void release() {
        this.removeAllTargets();
        this.inputfilter = null;
        this.output = null;
    }

    private void removeAllFilters() {
        this.filters.clear();
        this.refreshFilters();
    }

    public void createCurrentBitmap(GPUSurfaceCameraView.TakePhtoListener takePhtoListener) {
        if (this.transformFilter != null) {
            this.transformFilter.createCurrentBitmap(takePhtoListener);
        }
    }

    public void init() {
        if (!isInited) {
            videoInputFilter.init();
            transformFilter.init();
            saturationFilter.init();
            whiteBalanceFilter.init();
            brightnessFilter.init();
            contrastFilter.init();
            highLightShadowFilter.init();
            output.init();
            if (colorFilter != null) {
                colorFilter.init();
            }
            isInited = true;
            specialEffectsFilterHashMap = new HashMap<>();
            effectAdapters = new ArrayList<>();
        }
    }

    public void updateFilter(Chunk chunk, CMTime currentTime) {
        if (chunk == null) return;
        this.removeAllFilters();
        addFilter(transformFilter);
        Matrix4f matrix4fForVideoFilter = new Matrix4f(chunk.getPreferredTransform());
        matrix4fForVideoFilter.multiply(actionTransformWithTime(currentTime, chunk));
        this.videoInputFilter.setPreferredTransform(matrix4fForVideoFilter, chunk.getVideoSize());
        transformFilter.setIgnoreAspectRatio(true);
        float[] matrix = new float[16];
        if (chunk.getVideoRotationMatrix() == null) {
            Matrix4f matrix4f = new Matrix4f();
            matrix4f.loadIdentity();
            matrix = matrix4f.getArray();
        } else {
            matrix = chunk.getVideoRotationMatrix();
        }
        transformFilter.setTransform3D(matrix);
        if (chunk.isDisableColorFilter()) return;
        if (chunk.getLightValue() != 0) {
            this.addFilter(this.brightnessFilter);
            float strength = chunk.getLightValue() * 0.5f;
            this.brightnessFilter.setBrightness(strength);
        }
        if (chunk.getContrastValue() != 0) {
            this.addFilter(this.contrastFilter);
            float strength = chunk.getContrastValue();
            if (strength < 0) {
                strength = strength * 0.6f;
                strength = 1 + strength;
            } else {
                strength = 1 + strength;
            }
            this.contrastFilter.setContrast(strength);
        }
        if (chunk.getSaturabilityValue() != 0) {
            this.addFilter(saturationFilter);
            float strength = chunk.getSaturabilityValue();
            strength = strength + 1;
            this.saturationFilter.setSaturation(strength);
        }
        if (chunk.getHighlightValue() != 0 || chunk.getShadowValue() != 0) {
            this.addFilter(this.highLightShadowFilter);
            float shadow = chunk.getShadowValue();
            float highlight = chunk.getHighlightValue();
            highlight = 1 - highlight;
            this.highLightShadowFilter.setShadows(shadow);
            this.highLightShadowFilter.setHighlights(highlight);
        }
        if (chunk.getColortemperatureValue() != 0) {
            this.addFilter(this.whiteBalanceFilter);
            this.whiteBalanceFilter.setTemperature(chunk.getColortemperatureValue());
        }
        updataSpecialEffect(currentTime.getSecond());
//        updataSpecialEffectTest(currentTime.getSecond());
        updataLutFilter(chunk);
    }

    /**
     * 添加滤镜
     *
     * @param chunk
     */
    private void updataLutFilter(Chunk chunk) {
        if (chunk.getFilterName() != null && chunk.getFilterName().length() > 0) {
            if (chunk.getFilterName() != colorFilterName) {
                if (this.colorFilter != null) {
                    this.colorFilter.unload();
                    this.removeFilter(this.colorFilter);
                }
                this.colorFilterName = chunk.getFilterName();
                this.colorFilter = new VNiImageFilter(context, colorFilterName);
                this.colorFilter.init();
            }
            this.addFilter(this.colorFilter);
            this.colorFilter.setLevelValue(chunk.getStrengthValue());
        }
    }

    private Matrix4f actionTransformWithTime(CMTime cmTime, Chunk chunk) {
        float duration = (float) CMTime.getSecond(chunk.getChunkEditTimeRange().getDuration());
        float dtTime = (float) (CMTime.getSecond(cmTime) - CMTime.getSecond(chunk.getStartTime()));
        Matrix4f transform = new Matrix4f();
        transform.loadIdentity();
        float maxValue = 1.05f;
        float scaleValue = 0.0f;
        if (duration >= 2.0f) {
            maxValue = 1.05f;
        } else {
            maxValue = 1f + 0.05f * duration / 2.0f;
        }
        scaleValue = (maxValue - 1.0f) / 2f;
        float minValue = 1.0f;
        //##################
        float scale = 2.0f;
        float top = (1.0f * chunk.getVideoFile().getHeight() / chunk.getVideoFile().getWidth());
        float bottom = (-1.0f * chunk.getVideoFile().getHeight() / chunk.getVideoFile().getWidth());
        float topBottomValue = scale / (top - bottom);
        if (chunk == null || chunk.getScreenType() == null)//TOdo 不应该为null
            return transform;
        //##################
        switch (chunk.getScreenType()) {
            case ChunkScreenActionType_None:
                break;
            case ChunkScreenActionType_Zoom_Out: {
                float k = -((maxValue - minValue) / duration);
                float b = maxValue;
                float value = k * dtTime + b;
                value = Math.max(1.f, Math.min(1.05f, value));
//                transform = CGAffineTransformMakeScale(value, value);
                transform.scale(value, value, 1.0f);
                break;
            }
            case ChunkScreenActionType_Zoom_In: {
                float k = (maxValue - minValue) / duration;
                float b = minValue;
                float value = k * dtTime + b;
                value = Math.max(1.f, Math.min(1.05f, value));
                transform.scale(value, value, 1.0f);
            }
            break;
            case ChunkScreenActionType_Translate_Up: {
                transform.scale(maxValue, maxValue, 1.0f);
                scaleValue = scaleValue / topBottomValue;
                float k = scaleValue / duration;
                float value = k * dtTime;
                transform.translate(0, value, 0);
                break;
            }
            case ChunkScreenActionType_Translate_Down: {
                transform.scale(maxValue, maxValue, 1.0f);
                scaleValue = scaleValue / topBottomValue;
                float k = scaleValue / duration;
                float value = k * dtTime;
                transform.translate(0, -value, 0);
                break;
            }
            case ChunkScreenActionType_Translate_Left: {
                transform.scale(maxValue, maxValue, 1.0f);
                float k = scaleValue / duration;
                float value = k * dtTime;
                transform.translate(-value, 0, 0);
                break;
            }
            case ChunkScreenActionType_Translate_Right: {
                transform.scale(maxValue, maxValue, 1.0f);
                float k = scaleValue / duration;
                float value = k * dtTime;
                transform.translate(value, 0, 0);
                break;
            }
            default:
                break;
        }
        return transform;

    }

    /**
     * 设置各种特效
     *
     * @param effectAdapters
     */
    public void setEffectAdapters(ArrayList<EffectAdapter> effectAdapters) {
        if (this.effectAdapters == null)
            this.effectAdapters = new ArrayList<>();
        this.effectAdapters.clear();
        if (effectAdapters == null || effectAdapters.isEmpty()) return;
        this.effectAdapters.addAll(effectAdapters);
    }

    /**
     * 添加特效
     */
    public void updataSpecialEffect(double currentTime) {
        for (EffectAdapter effectAdapter : effectAdapters) {
            if (effectAdapter.getEffectType() != EffectType.EffectType_Special_Effect) continue;
            if (CMTime.getSecond(effectAdapter.getTimeRange().getStartTime()) <= currentTime
                    && CMTime.getSecond(effectAdapter.getTimeRange().getEnd()) > currentTime) {
                if (TextUtils.isEmpty(effectAdapter.getSpecialEffectJson())) continue;
                VNISpecialEffectsFilter vniSpecialEffectsFilter = null;
                if (specialEffectsFilterHashMap.containsKey(effectAdapter.getEffectId())) {
                    vniSpecialEffectsFilter = specialEffectsFilterHashMap.get(effectAdapter.getEffectId());
                } else {
                    vniSpecialEffectsFilter = new VNISpecialEffectsFilter(effectAdapter.getSpecialEffectJson());
                    specialEffectsFilterHashMap.put(effectAdapter.getEffectId(), vniSpecialEffectsFilter);
                    try {
                        vniSpecialEffectsFilter.init();
                    } catch (Exception e) {
                        specialEffectsFilterHashMap.remove(effectAdapter.getEffectId());
                        Log.e("", "VniSpecialEffectsFilter init error:" + e.getMessage());
                        continue;
                    }
                }
                vniSpecialEffectsFilter.setMiTimegVlaue((float) (currentTime - CMTime.getSecond(effectAdapter.getTimeRange().getStartTime())));
                this.addFilter(vniSpecialEffectsFilter);
            }
        }
    }

    public void clearSpecialEffect() {
        if (specialEffectsFilterHashMap != null)
            specialEffectsFilterHashMap.clear();
        if (effectAdapters != null)
            effectAdapters.clear();
    }


    /**
     * 添加特效
     */
//    public void updataSpecialEffectTest(double currentTime) {
//        if (1 <= currentTime
//                && 20 > currentTime) {
//            String keys = "test";
//            String json1 = "{\n" +
//                    "    \"id\": \"2333\",\n" +
//                    "    \"vsh\": \"attribute vec4 position; attribute vec4 inputTextureCoordinate; varying vec2 textureCoordinate; uniform lowp float time; const float PI = 3.1415926; void main() { float duration = 0.6; float maxAmplitude = 0.3; float time = mod(time, duration); float amplitude = 1.0 + maxAmplitude * abs(sin(time * (PI / duration))); gl_Position = vec4(position.x * amplitude, position.y * amplitude, position.zw); textureCoordinate = inputTextureCoordinate.xy; }\",\n" +
//                    "    \"key\": \"time\"\n" +
//                    "}";
//
//            String json = "{\n" +
//                    "    \"id\": \"2333\",\n" +
//                    "    \"fsh\": \"////// Fragment Shader\n" +
//                    "varying highp vec2 textureCoordinate;\n" +
//                    "uniform sampler2D inputImageTexture;\n" +
//                    "uniform highp float iTime;\n" +
//                    "uniform highp vec2 inputSize;\n" +
//                    "uniform sampler2D noiseTexture;\n" +
//                    "\n" +
//                    "uniform highp float effectValue;\n" +
//                    "\n" +
//                    "const highp float tau = 6.28318530717958647692;\n" +
//                    "\n" +
//                    "// Gamma correction\n" +
//                    "#define GAMMA (2.2)\n" +
//                    "\n" +
//                    "highp vec3 ToLinear(in highp vec3 col)\n" +
//                    "{\n" +
//                    "    // simulate a monitor, converting colour values into light values\n" +
//                    "    return pow(abs(col), vec3(GAMMA));\n" +
//                    "}\n" +
//                    "\n" +
//                    "highp vec3 ToGamma(in highp vec3 col)\n" +
//                    "{\n" +
//                    "    // convert back into colour values, so the correct light will come out of the monitor\n" +
//                    "    return pow(abs(col), vec3(1.0/GAMMA));\n" +
//                    "}\n" +
//                    "\n" +
//                    "highp vec4 Noise(in ivec2 x)\n" +
//                    "{\n" +
//                    "    return texture2D(noiseTexture, fract((vec2(x)+0.5)/256.0));\n" +
//                    "}\n" +
//                    "\n" +
//                    "highp vec4 Rand(in int x)\n" +
//                    "{\n" +
//                    "    highp vec2 uv;\n" +
//                    "    uv.x = (float(x)+0.5)/1.0;\n" +
//                    "    uv.y = (floor(uv.x)+0.5)/1.0;\n" +
//                    "    return texture2D(noiseTexture, uv);\n" +
//                    "}\n" +
//                    "\n" +
//                    "void main()\n" +
//                    "{\n" +
//                    "    highp vec3 ray;\n" +
//                    "    ray.xy = 2.0*(inputSize*textureCoordinate -inputSize.xy*.5)/inputSize.x;\n" +
//                    "    ray.z = 1.0;\n" +
//                    "\n" +
//                    "    highp float offset = iTime*.5;\n" +
//                    "    highp  float speed2 = (cos(offset)+1.0)*2.0;\n" +
//                    "    highp float speed = speed2+.1;\n" +
//                    "    offset += sin(offset)*.96;\n" +
//                    "    offset *= 2.0;\n" +
//                    "    highp vec3 col = vec3(0);\n" +
//                    "    highp vec3 stp = ray/max(abs(ray.x), abs(ray.y));\n" +
//                    "    int count = 5;\n" +
//                    "    highp vec3 pos = 2.0*stp+.5;\n" +
//                    "    for (int i=0; i < count; i++)\n" +
//                    "    {\n" +
//                    "        highp float z = Noise(ivec2(pos.xy)).x;\n" +
//                    "        z = fract(z-offset);\n" +
//                    "        highp float d = 50.0*z-pos.z;\n" +
//                    "        highp float w = pow(max(0.0, 1.0-12.0*length(fract(pos.xy)-.5)), 2.0);\n" +
//                    "        highp vec3 c = vec3(0.0);\n" +
//                    "        c = max(vec3(0), vec3(1.0-abs(d+speed2*.5)/speed, 1.0-abs(d)/speed, 1.0-abs(d-speed2*.5)/speed));\n" +
//                    "\n" +
//                    "        col += 1.5*(1.0-z)*c*w;\n" +
//                    "        pos += stp;\n" +
//                    "    }\n" +
//                    "\n" +
//                    "    col = ToGamma(col);\n" +
//                    "    highp vec3 c = texture2D(inputImageTexture, textureCoordinate).rgb;\n" +
//                    "\n" +
//                    "    gl_FragColor.rgb = c + col;\n" +
//                    "    gl_FragColor.a = 1.0;\n" +
//                    "}\",\n" +
//                    "    \"timeRelated\": true,\n" +
//                    "    \"sizeRelated\": true,\n" +
//                    "    \"noise\": \"tvnoise.png\"\n" +
//                    "}";
//            VNISpecialEffectsFilter vniSpecialEffectsFilter = null;
//            if (specialEffectsFilterHashMap.containsKey(keys)) {
//                vniSpecialEffectsFilter = specialEffectsFilterHashMap.get(keys);
//            } else {
//                vniSpecialEffectsFilter = new VNISpecialEffectsFilter(json);
//                specialEffectsFilterHashMap.put(keys, vniSpecialEffectsFilter);
//                vniSpecialEffectsFilter.init();
//            }
//            vniSpecialEffectsFilter.setMiTimegVlaue((float) (currentTime - 1));
//            this.addFilter(vniSpecialEffectsFilter);
//
//        }
//    }
}

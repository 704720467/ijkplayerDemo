package com.zp.libvideoedit.modle.effectModel;

/**
 * Created by gwd on 2018/5/21.
 */

public enum EffectType {
    EffectType_Video("EFFECTVIDEO", 0),
    EffectType_Filter("EFFECTFILTER", 1),
    EffectType_Pic("EFFECTPIC", 2),
    EffectType_Sticker("EFFECTSTICKER", 5),//贴纸
    EffectType_Special_Effect("EFFECTSPECIALEFFECT", 6);//特效


    String name;
    int value;

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    EffectType(String name, int value) {
        this.name = name;
        this.value = value;

    }

    public static EffectType getEffect(int value) {
        if (value == EffectType_Video.getValue()) {
            return EffectType_Video;
        } else if (value == EffectType_Pic.getValue()) {
            return EffectType_Pic;
        } else if (value == EffectType_Sticker.getValue()) {
            return EffectType_Sticker;
        } else if (value == EffectType_Special_Effect.getValue()) {
            return EffectType_Special_Effect;
        } else {
            return EffectType_Filter;
        }
    }
}

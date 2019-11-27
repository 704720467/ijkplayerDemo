package com.zp.libvideoedit.modle;

/**
 * Created by gwd on 2018/6/11.
 */

public enum ChunkScreenActionType {

    ChunkScreenActionType_None("none", 0), ChunkScreenActionType_Zoom_In("flip", 1), ChunkScreenActionType_Zoom_Out("vflip", 2), ChunkScreenActionType_Translate_Up("leftrote", 3), ChunkScreenActionType_Translate_Down("rightrote", 4), ChunkScreenActionType_Translate_Left("lefgr", 5), ChunkScreenActionType_Translate_Right("rightr", 6);
    String name;
    int value;

    ChunkScreenActionType(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public static ChunkScreenActionType getScreenType(int value) {
        if (value == ChunkScreenActionType_None.getValue()) {
            return ChunkScreenActionType_None;
        } else if (value == ChunkScreenActionType_Zoom_In.getValue()) {
            return ChunkScreenActionType_Zoom_In;
        } else if (value == ChunkScreenActionType_Zoom_Out.getValue()) {
            return ChunkScreenActionType_Zoom_Out;
        } else if (value == ChunkScreenActionType_Translate_Up.getValue()) {
            return ChunkScreenActionType_Translate_Up;
        } else if (value == ChunkScreenActionType_Translate_Down.getValue()) {
            return ChunkScreenActionType_Translate_Down;
        } else if (value == ChunkScreenActionType_Translate_Left.getValue()) {
            return ChunkScreenActionType_Translate_Left;

        } else if (value == ChunkScreenActionType_Translate_Right.getValue()) {
            return ChunkScreenActionType_Translate_Right;

        }
        return null;
    }


    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }


}

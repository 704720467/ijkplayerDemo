package com.zp.libvideoedit.modle;

/**
 * Created by gwd on 2018/6/13.
 */

public enum ChunkType {
    ChunkType_Default("default", 0), ChunkType_Black("black", 1), ChunkType_White("white", 2);
    private String name;
    private int value;

    ChunkType(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    public static ChunkType chunkType(int value) {
        if (value == ChunkType_Default.getValue()) {
            return ChunkType_Default;
        } else if (value == ChunkType_Black.getValue()) {
            return ChunkType_Black;
        } else if (value == ChunkType_White.getValue()) {
            return ChunkType_White;
        }
        return null;
    }

}

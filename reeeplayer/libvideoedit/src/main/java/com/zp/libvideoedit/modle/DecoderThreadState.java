package com.zp.libvideoedit.modle;

/**
 * Created by guoxian on 2018/5/8.
 */

public enum DecoderThreadState {
    unconfig("unconfig",10),readly("readly",11),runing("runing",12),idle("idle",13)/*,pause*/,stoping("stoping",14),stoped("stoped",15);

    String name;
    int value;

    DecoderThreadState(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }
}

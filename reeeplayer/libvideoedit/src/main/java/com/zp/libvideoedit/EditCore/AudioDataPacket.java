package com.zp.libvideoedit.EditCore;

import android.util.Log;

import static com.zp.libvideoedit.Constants.TAG_A;
import static com.zp.libvideoedit.Constants.VERBOSE_A;

/**
 * 音频解码出的数据
 * Created by zp on 2019/6/14.
 */

public class AudioDataPacket {
    private short[] data;
    private long pts;
    private boolean decodeEnd = false;

    public short[] getData() {
        return data;
    }

    public void setData(short[] data) {
        this.data = data;
    }

    public long getPts() {
        return pts;
    }

    public void setPts(long pts) {
        this.pts = pts;
    }

    public boolean isDecodeEnd() {
        return decodeEnd;
    }

    public void setDecodeEnd(boolean decodeEnd) {
        this.decodeEnd = decodeEnd;
    }

    public AudioDataPacket() {
    }

    public AudioDataPacket(short[] data, long pts, int isDecodeEnd) {
        this.data = data;
        this.pts = pts;
        if (VERBOSE_A)
            Log.d(TAG_A, "创建AudioDataPacket，isDecodeEnd=" + isDecodeEnd + "；pts=" + pts + "；data.length=" + data.length);
        decodeEnd = isDecodeEnd == 1;
    }
}

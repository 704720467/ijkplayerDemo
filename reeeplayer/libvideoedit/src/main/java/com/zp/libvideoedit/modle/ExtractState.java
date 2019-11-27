package com.zp.libvideoedit.modle;

/**
 * Created by guoxian on 2018/5/10.
 */

public enum ExtractState {
    /**
     * 正常，有值
     */
    hasNext, /**
     * segment begin,此时需要重新配置解码器
     */
    segBegin, /**
     * 空的segment
     */
    empty,

    /**
     * 当视频没有音频轨道时,track of Audio_Main 的segment为空时，返回pading,需要解码线程填充值为0的buffer
     */
    pading,
    /**
     * segment 结束
     */


    segEos; /**
     * seek 时，需要解码器清空缓冲，不在输出帧。
     */
    //flushEos;
    /**
     * track 结束
     */
//    trackEos;

}

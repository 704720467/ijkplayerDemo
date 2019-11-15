package com.zp.libvideoedit.Time;

/**
 * Created by gwd on 2018/3/8.
 */

public class CMTimeMapping {
    private CMTimeRange sourceTimeRange;
    private CMTimeRange targetTimeRange;

    public CMTimeMapping(CMTimeRange source, CMTimeRange target) {
        this.sourceTimeRange = source;
        this.targetTimeRange = target;
    }

    public CMTimeRange getSourceTimeRange() {
        return sourceTimeRange;
    }

    public CMTimeRange getTargetTimeRange() {
        return targetTimeRange;
    }

    public String print(){
        String string = "Src start:"+CMTime.getSecond(sourceTimeRange.getStartTime())+" :SrcEnd:"+CMTime.getSecond(sourceTimeRange.getDuration())+": dstStart:"+CMTime.getSecond(this.targetTimeRange.getStartTime())+" :dstEnd"+CMTime.getSecond(this.targetTimeRange.getDuration());
        return string;
    }
    public double diff(){
        return targetTimeRange.getDuration().getSecond()-sourceTimeRange.getDuration().getSecond();
    }


    @Override
    public String toString() {
        return "CMTimeMapping{" + "source:" + String.format("%,d",sourceTimeRange.getStartTime().getMs()) +"-" + String.format("%,d",sourceTimeRange.getStartTime().getMs()+sourceTimeRange.getDuration().getMs())+", target:" + String.format("%,d",targetTimeRange.getStartTime().getMs())+"-"+ String.format("%,d",targetTimeRange.getStartTime().getMs()+targetTimeRange.getDuration().getMs()) + '}';
    }
}
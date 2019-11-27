package com.zp.libvideoedit.modle;

/**
 * Created by guoxian on 2018/5/10.
 */
public class HasNextResult {
    private ExtractState state;
    private long pts;
    private Segment segment;

    public HasNextResult(ExtractState state, long pts, Segment segment) {
        this.state = state;
        this.pts = pts;
        this.segment = segment;
    }

    public long getPts() {
        return pts;
    }

    public Segment getSegment() {
        return segment;
    }

    public ExtractState getState() {
        return state;
    }

    @Override
    public String toString() {
        return "HasNextResult{" + "result=" + state + ", pts=" + String.format("%,d",pts) + ", segment=" + segment + '}';
    }

}

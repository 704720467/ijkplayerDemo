package com.zp.libvideoedit.modle;

public class ExtractResult {
    private int size;
    private int flag;
    private Segment segment;
    private long ptsInFile;

    public Segment getSegment() {
        return segment;
    }


    @Override
    public String toString() {
        return "ExtractResult{"+"ptsInFile=" + ptsInFile+ ", size=" + size + ", flag=" + flag + ", segment=" + segment  + '}';
    }

    public ExtractResult(Segment segment, int size, int flag, long ptsInFile) {
        super();
        this.size = size;
        this.flag = flag;
        this.segment = segment;
        this.ptsInFile = ptsInFile;

    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }


    public long getPtsInFile() {
        return ptsInFile;
    }

    public void setPtsInFile(long ptsInFile) {
        this.ptsInFile = ptsInFile;
    }




}

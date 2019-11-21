package cn.reee.reeeplayer.view.seekBar;

/**
 * seek bar 数据布局
 * Create by zp on 2019-11-20
 */
public class ReeeSeekBarData {
    private float mStartTime;
    private float mEndTime;

    public ReeeSeekBarData(float mStartTime, float mEndTime) {
        this.mStartTime = mStartTime;
        this.mEndTime = mEndTime;
    }

    public float getmStartTime() {
        return mStartTime;
    }

    public void setmStartTime(float mStartTime) {
        this.mStartTime = mStartTime;
    }

    public float getmEndTime() {
        return mEndTime;
    }

    public void setmEndTime(float mEndTime) {
        this.mEndTime = mEndTime;
    }
}

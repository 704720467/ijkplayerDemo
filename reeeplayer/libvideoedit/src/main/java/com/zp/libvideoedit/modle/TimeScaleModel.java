package com.zp.libvideoedit.modle;

import androidx.annotation.NonNull;

import com.zp.libvideoedit.Time.CMTime;

import java.util.ArrayList;

/**
 * Created by gwd on 2018/5/15.
 */

public class TimeScaleModel implements Comparable {
    private CMTime timePosition;
    private float speedScale;

    public TimeScaleModel(CMTime timePosition, float speedScale) {
        this.timePosition = timePosition;
        this.speedScale = speedScale;
    }

    @Override
    public String toString() {
        return "TimeScaleModel{" +
                "timePosition=" + timePosition.getSecond() +
                ", speedScale=" + speedScale +
                '}';
    }

    public CMTime getTimePosition() {
        return timePosition;
    }

    public void setTimePosition(CMTime timePosition) {
        this.timePosition = timePosition;
    }


    public void setSpeedScale(float speedScale) {
        this.speedScale = speedScale;
    }

    public CMTime timeAfterSpeed() {
        float timeValue = (float) CMTime.getSecond(this.timePosition);
        timeValue = timeValue / (1.0f * this.getSpeedScale());
        CMTime speedTime = new CMTime((double) timeValue, 1000);
        return speedTime;
    }

    public float getSpeedScale() {
        this.speedScale = Math.max(0.25f, this.speedScale);
        this.speedScale = Math.min(8f, this.speedScale);
        return this.speedScale;
    }

//    public static TimeScaleModel beanToModel(ScriptJsonBean.SpeedPointsBean pointsBean) {
//        if (pointsBean.getTimePosition() == null) return null;
//        CMTime positionTime = new CMTime(pointsBean.getTimePosition().get(0), pointsBean.getTimePosition().get(1));
//        float timeScale = pointsBean.getSpeedScale();
//        return new TimeScaleModel(positionTime, timeScale);
//    }
//
//    public ArrayList<TimeScaleModel> scaleModelFromBean(ArrayList<ScriptJsonBean.SpeedPointsBean> pointsBeans) {
//        ArrayList<TimeScaleModel> scaleModels = new ArrayList<TimeScaleModel>();
//        for (ScriptJsonBean.SpeedPointsBean pointsBean : pointsBeans) {
////            TimeScaleModel model = new TimeScaleModel(pointsBean.getTimePosition(),pointsBean.getSpeedScale());
////            scaleModels.add(model);
//        }
//        return scaleModels;
//    }

    /**
     * 开始的秒数
     *
     * @return
     */
    public double position() {
        return timePosition.getSecond();
    }

    @Override
    public int compareTo(@NonNull Object o) {
        TimeScaleModel other = (TimeScaleModel) o;
        return Double.compare(this.position(), other.position());
    }

//    public TimeScaleModelVo timeScaleModelVo() {
//        TimeScaleModelVo timeScaleModelVo = new TimeScaleModelVo();
//        timeScaleModelVo.setSpeedScale(getSpeedScale());
//        timeScaleModelVo.setTimePosition(getTimePosition().timeVo());
//        return timeScaleModelVo;
//    }
}

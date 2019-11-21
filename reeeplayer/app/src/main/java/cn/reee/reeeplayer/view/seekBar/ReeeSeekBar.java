package cn.reee.reeeplayer.view.seekBar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

/**
 * Seek Bar
 * Create by zp on 2019-11-20
 */
public class ReeeSeekBar extends View {
    private int mDuration;//总时长
    private ArrayList<ReeeSeekBarData> haveDataTime;
    private float progressHeight = 6;//单位像素
    private float currentProgressHeight = progressHeight + 1;//单位像素
    private float mThumbBarRadius = 13;//单位像素
    private float marginLeft = mThumbBarRadius;
    private float marginRight = mThumbBarRadius;
    private float mStartX;

    private Paint mTimeDataPaint;//有数据 画笔
    private Paint mNotTimeDataPaint;//无数据 画笔
    private Paint mCurrentProgressPaint;//进度画笔
    private Paint mCirclePaint;

    private Canvas canvas;
    private float currentProgressX;//当前seek到的位置 单位像素 包括了margin大小

    private float startCircleRadius = progressHeight / 2;//seekbar 左边右边圆角半径

    public ReeeSeekBar(Context context) {
        super(context);
        initData();
    }

    public ReeeSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initData();
    }

    public ReeeSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initData();
    }


    public void initData() {


        mCirclePaint = new Paint();
        mCirclePaint.setColor(Color.WHITE);
        mCirclePaint.setStyle(Paint.Style.FILL);
//        mCirclePaint.setAntiAlias(true);

        mTimeDataPaint = new Paint();
        mTimeDataPaint.setColor(Color.parseColor("#7fffffff"));
        mTimeDataPaint.setStrokeWidth(progressHeight);
        mTimeDataPaint.setStyle(Paint.Style.FILL);

        mNotTimeDataPaint = new Paint();
        mNotTimeDataPaint.setColor(Color.parseColor("#7f383838"));
        mNotTimeDataPaint.setStrokeWidth(progressHeight);
        mNotTimeDataPaint.setStyle(Paint.Style.FILL);

        mCurrentProgressPaint = new Paint();
        mCurrentProgressPaint.setColor(Color.parseColor("#EE5514"));
        mCurrentProgressPaint.setStrokeWidth(currentProgressHeight);
        mCurrentProgressPaint.setStyle(Paint.Style.FILL);

        haveDataTime = new ArrayList<>();
        haveDataTime.add(new ReeeSeekBarData(0, 100));
        haveDataTime.add(new ReeeSeekBarData(200, 300));
        mDuration = 300;
        mStartX = marginLeft;
        currentProgressX = mStartX;

    }

    /**
     * 更新数据
     */
    public void updataView() {
        initData();
        invalidate();
    }

    private int getCanDrawWidth() {
        return (int) (getWidth() - marginLeft - marginRight);
    }

    private int getMaxWidth() {
        return (int) (mStartX + getCanDrawWidth());
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        this.canvas = canvas;
        float startY = (getHeight() - progressHeight) / 2;

        canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
        for (int i = 0; i < haveDataTime.size(); i++) {
            ReeeSeekBarData reeeSeekBarData = haveDataTime.get(i);
            float startX = getCanDrawWidth() * (reeeSeekBarData.getmStartTime() / mDuration) + mStartX;
            float endX = getCanDrawWidth() * (reeeSeekBarData.getmEndTime() / mDuration) + mStartX;
            startX = startX < mStartX ? mStartX : startX;
            endX = endX > getMaxWidth() ? getMaxWidth() : endX;

            if (i == 0) {//绘制左边 半圆
                Path mPath = new Path();
                mPath.addArc(startX, startY, startX + startCircleRadius * 2, startY + startCircleRadius * 2, 90, 180);
                canvas.drawPath(mPath, mTimeDataPaint);
                startX = startX + startCircleRadius;
            }
            if (i == haveDataTime.size() - 1) {//绘制右边半圆
                Path mPath = new Path();
                mPath.addArc(endX - startCircleRadius * 2, startY, endX, startY + startCircleRadius * 2, 270, 180);
                canvas.drawPath(mPath, mTimeDataPaint);
                endX = endX - startCircleRadius;
            }
            drawEmptyData(i, startY);
            canvas.drawRect(new RectF(startX, startY, endX, startY + progressHeight), mTimeDataPaint);
        }
        drawCuurenProgress(canvas, startY);

        //绘制thumbBar
        float nowProgress = Math.max(currentProgressX - mStartX, mStartX);
        nowProgress = Math.min(nowProgress, getMaxWidth());
        canvas.drawCircle(nowProgress, getHeight() / 2, mThumbBarRadius, mCirclePaint);
    }

    /**
     * 绘制当前进度
     *
     * @param canvas
     * @param startY
     */
    private void drawCuurenProgress(Canvas canvas, float startY) {
        //1.绘制 半圆进度
        float currentProgress = Math.max(currentProgressX - mStartX, 0);
        currentProgress = Math.min(currentProgress, startCircleRadius);
        if (currentProgress > 0) {
            float rangle = currentProgress / startCircleRadius * 180;
            Path mPath = new Path();
            mPath.addArc(mStartX, startY, mStartX + startCircleRadius * 2, startY + startCircleRadius * 2, 180 - rangle / 2, rangle);
            canvas.drawPath(mPath, mCurrentProgressPaint);
            Log.e("ReeeSeekBar", "rangle=" + rangle + "\t currentProgress=" + currentProgress + "\t currentProgressX=" + currentProgressX);
        }
        //2.绘制直角进度
        float lastPriogress = currentProgressX - mStartX - startCircleRadius;
        if (lastPriogress > 0)
            canvas.drawRect(new RectF(mStartX + startCircleRadius, (getHeight() - currentProgressHeight) / 2, currentProgressX - startCircleRadius, startY + currentProgressHeight), mCurrentProgressPaint);

    }

    private void drawEmptyData(int position, float startY) {
        if (position > 0) {
            boolean haveEmptyData = haveDataTime.get(position).getmStartTime() - haveDataTime.get(position - 1).getmEndTime() > 0;
            if (!haveEmptyData) return;
            float startX = getCanDrawWidth() * (haveDataTime.get(position - 1).getmEndTime() / mDuration) + mStartX;
            float endX = getCanDrawWidth() * (haveDataTime.get(position).getmStartTime() / mDuration) + mStartX;
            startX = startX < mStartX ? mStartX : startX;
            endX = endX > getMaxWidth() ? getMaxWidth() : endX;
            canvas.drawRect(new RectF(startX, startY, endX, startY + progressHeight), mNotTimeDataPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentProgressX = Math.max(event.getX(), mStartX);
                currentProgressX = Math.min(event.getX(), getMaxWidth());
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                currentProgressX = Math.max(event.getX(), mStartX);
                currentProgressX = Math.min(event.getX(), getMaxWidth());
                invalidate();
                break;
        }
        return true;
    }
}

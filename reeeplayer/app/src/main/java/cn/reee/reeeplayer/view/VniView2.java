package cn.reee.reeeplayer.view;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

import com.zp.libvideoedit.utils.GlobalTools;


/**
 * vni loading
 * Created by zp on 2018/2/2.
 */

public class VniView2 extends View {

    private Paint paint;

    private int paintColor;

    private int strokeWidth;//背景线宽

    private ValueAnimator valueAnimator;
    private float[] mCurrentPosition = new float[2];

    private int w;//大小
    private float l;//三角形边长
    private Path mPath;

    private int step = 0;

    float left;
    float top;
    float right;
    float bottom;


    public VniView2(Context context) {
        super(context);
        init(context);
    }

    public VniView2(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public VniView2(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        paint = new Paint();
        paint.setAntiAlias(true);
        paintColor = Color.WHITE;
        strokeWidth = GlobalTools.dip2px(context, 4.1f);
        paint.setStrokeWidth(strokeWidth);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);

        paint.setColor(paintColor);
        paint.setPathEffect(new CornerPathEffect(10));

        w = GlobalTools.dip2px(context, 50);
        l = w / 2.3f;
    }

    float lastVaule;

    float progress = 0;

    private void doAnim() {
        step = 0;
        progress = 0;
        Path path = new Path();
        float h = (float) Math.sqrt(3f / 4 * l * l);
        float p = l / 10;
        left = w / 2f - h / 2 + p;
        top = w / 2f - l / 2f;
        right = left + h;
        bottom = top + l;

        path.moveTo(left, top);
        path.lineTo(right, top + l / 2);
        path.lineTo(left, bottom);
        path.close();

        mPath = new Path();
        final PathMeasure mPathMeasure = new PathMeasure(path, false);
        mPathMeasure.getPosTan(0, mCurrentPosition, null);
        mPath.moveTo(mCurrentPosition[0], mCurrentPosition[1]);

        final float p1 = mPathMeasure.getLength() / 3f;
        final float p2 = mPathMeasure.getLength() / 3f * 2f;
        lastVaule = 0;
        valueAnimator = ValueAnimator.ofFloat(0, mPathMeasure.getLength());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (Float) animation.getAnimatedValue();
                progress = value / mPathMeasure.getLength();

                if (value > p1 && lastVaule < p1) {
                    float v = mPathMeasure.getLength() / 3f;
                    mPathMeasure.getPosTan(v, mCurrentPosition, null);
                    mPath.lineTo(mCurrentPosition[0], mCurrentPosition[1]);
                }
                if (value > p2 && lastVaule < p2) {
                    mPathMeasure.getPosTan(mPathMeasure.getLength() / 3f * 2f, mCurrentPosition, null);
                    mPath.lineTo(mCurrentPosition[0], mCurrentPosition[1]);
                }

                mPathMeasure.getPosTan(value, mCurrentPosition, null);
                mPath.lineTo(mCurrentPosition[0], mCurrentPosition[1]);
                postInvalidate();
                lastVaule = value;
            }
        });
        valueAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                doAnim1();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        valueAnimator.setDuration(800);
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.start();
    }

    private void doAnim1() {
        step = 1;
        Path path = new Path();
        path.moveTo(left, top);
        path.quadTo(right / 2, top / 2, right + strokeWidth / 2, top - strokeWidth);

        final Path mPath2 = new Path();
        final PathMeasure mPathMeasure = new PathMeasure(path, false);
        mPathMeasure.getPosTan(0, mCurrentPosition, null);
        mPath2.moveTo(mCurrentPosition[0], mCurrentPosition[1]);

        valueAnimator = ValueAnimator.ofFloat(0, mPathMeasure.getLength());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (Float) animation.getAnimatedValue();
                mPathMeasure.getPosTan(value, mCurrentPosition, null);
                mPath2.lineTo(mCurrentPosition[0], mCurrentPosition[1]);
                postInvalidate();
            }
        });
        valueAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                doAnim2();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        valueAnimator.setDuration(150);
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.start();
    }

    private void doAnim2() {
        step = 2;
        Path path = new Path();
        float r = (float) Math.sqrt(Math.pow(right + strokeWidth / 2 - w / 2f, 2) + Math.pow(w / 2f - top + strokeWidth, 2));
        path.addCircle(w / 2, w / 2, r, Path.Direction.CW);

        final PathMeasure mPathMeasure = new PathMeasure(path, false);
        valueAnimator = ValueAnimator.ofFloat(0, mPathMeasure.getLength());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (Float) animation.getAnimatedValue();
                float v = value + mPathMeasure.getLength() / 8 * 7;
                if (v >= mPathMeasure.getLength()) {
                    v = value;
                    v = v - mPathMeasure.getLength() / 8;
                }
                mPathMeasure.getPosTan(v, mCurrentPosition, null);
                postInvalidate();
            }
        });
        valueAnimator.setDuration(800);
        valueAnimator.setRepeatCount(-1);
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.start();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mPath == null) {
            return;
        }


        canvas.save();
        canvas.rotate(-30 * (1 - progress), w / 2, w / 2);
        canvas.scale(progress, progress);
        paint.setStrokeWidth(strokeWidth * (0.4f + progress * 0.6f));
        paint.setColor(getColorWithAlpha(0.4f + progress * 0.6f, paintColor));

        canvas.drawPath(mPath, paint);

        if (step > 0) {
            canvas.drawPoint(mCurrentPosition[0], mCurrentPosition[1], paint);
        }
        canvas.restore();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(w, w);
    }

    public void show() {
        clear();
        doAnim();
    }

    public void clear() {
        if (valueAnimator != null)
            valueAnimator.cancel();
    }

    /**
     * 对rgb色彩加入透明度
     *
     * @param alpha     透明度，取值范围 0.0f -- 1.0f.
     * @param baseColor
     * @return a color with alpha made from base color
     */
    public static int getColorWithAlpha(float alpha, int baseColor) {
        int a = Math.min(255, Math.max(0, (int) (alpha * 255))) << 24;
        int rgb = 0x00ffffff & baseColor;
        return a + rgb;
    }

}

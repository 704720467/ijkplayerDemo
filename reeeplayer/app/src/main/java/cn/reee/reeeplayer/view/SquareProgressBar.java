package cn.reee.reeeplayer.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

/**
 * 方形进度条
 * Create by zp on 2019-12-01
 */
public class SquareProgressBar extends View {
    private int progress = 50;//进度

    private Paint paint = null;
    private Path path = null;
    private int margin;


    public SquareProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }


    private void init(Context context) {
        paint = new Paint();
        margin = convertDpToPx(2, context);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.parseColor("#ee5514"));
        paint.setStrokeWidth(margin);
        paint.setAntiAlias(true);
        path = new Path();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float scope = getMeasuredWidth() * 2 + getMeasuredHeight() * 2;
        DrawLocation mLocation = getLocation(scope);
        if (mLocation.place == Place.TOP) {
            path.moveTo(margin, 0);
            path.lineTo(mLocation.location, 0);
            canvas.drawPath(path, paint);
        }
        if (mLocation.place == Place.RIGHT) {
            path.moveTo(margin, 0);
            path.lineTo(getMeasuredWidth() - margin, 0);
            path.moveTo(getMeasuredWidth() - margin, 0);
            paint.setColor(Color.parseColor("#00ff00"));
            paint.setStrokeWidth(margin / 2);
            path.lineTo(getMeasuredWidth() - margin, mLocation.location);
            canvas.drawPath(path, paint);
        }
        if (mLocation.place == Place.BOTTOM) {
            path.moveTo(margin, 0);
            path.lineTo(getMeasuredWidth() - margin, 0);
            path.moveTo(getMeasuredWidth() - margin, 0);
            path.lineTo(getMeasuredWidth() - margin, getMeasuredHeight());
            path.moveTo(getMeasuredWidth() - margin, getMeasuredHeight());
            path.lineTo(mLocation.location, getMeasuredHeight());
            canvas.drawPath(path, paint);
        }
        if (mLocation.place == Place.LEFT) {
            path.moveTo(margin, 0);
            path.lineTo(getMeasuredWidth() - margin, 0);
            path.moveTo(getMeasuredWidth() - margin, 0);
            path.lineTo(getMeasuredWidth() - margin, getMeasuredHeight());
            path.moveTo(getMeasuredWidth() - margin, getMeasuredHeight());
            path.lineTo(margin, getMeasuredHeight());
            path.moveTo(margin, getMeasuredHeight());
            path.lineTo(margin, mLocation.location);
            Log.i("lcf", " mLocation.location = " + mLocation.location);
            canvas.drawPath(path, paint);
        }
    }


    public void setProgress(int progress) {
        this.progress = progress;
        postInvalidate();
    }


    private DrawLocation getLocation(float scope) {
        float length = scope * progress / 100;
        DrawLocation mLocation = new DrawLocation();
        if (length > getMeasuredWidth()) {
            float second = length - getMeasuredWidth();
            if (second > getMeasuredHeight()) {
                float third = second - getMeasuredHeight();
                if (third > getMeasuredWidth()) {
                    float four = third - getMeasuredWidth();
                    mLocation.place = Place.LEFT;
                    mLocation.location = getMeasuredHeight() - four;
                } else {
                    mLocation.place = Place.BOTTOM;
                    mLocation.location = getMeasuredWidth() - third;
                }
            } else {
                mLocation.place = Place.RIGHT;
                mLocation.location = second;
            }
        } else {
            mLocation.place = Place.TOP;
            mLocation.location = length;
        }
        return mLocation;
    }


    private class DrawLocation {
        public Place place;
        public float location;
    }


    public enum Place {
        LEFT, RIGHT, TOP, BOTTOM
    }

    public int convertDpToPx(float dp, Context context) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics());
    }
}

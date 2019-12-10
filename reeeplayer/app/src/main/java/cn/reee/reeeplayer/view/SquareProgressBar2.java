package cn.reee.reeeplayer.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.shapes.RectShape;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

/**
 * 方形进度条
 * Create by zp on 2019-12-01
 */
public class SquareProgressBar2 extends View {
    private float allLength;//进度条的总长度
    private int maxProgress = 100;//总的进度条长度为100（可改变）
    private int curProgress = 25;//当前进度为30（可改变）
    private Paint curPaint;//当前进度条的画笔
    private int width;//整个view的宽度，（包括paddingleft和paddingright）
    private int height;//整个view的高度，（包括paddingtop和paddingbottom）

    private float curProgressWidth;//当前进度条画笔的宽度
    private Path curPath;//当前进度条的路径，（总的进度条的路径作为onDraw的局部变量）
    private float proWidth;//整个进度条构成矩形的宽度
    private float proHeight;//整个进度条构成矩形的高度

    private float dotCX;//小圆点的X坐标（相对view）
    private float dotCY;//小圆点的Y坐标（相对view）

    public SquareProgressBar2(Context context) {
        super(context);
        initView();
    }

    public SquareProgressBar2(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    private void initView() {

        curPaint = new Paint();//当前进度条的画笔设置
        curProgressWidth = dp2Px(2);//dp转px
        curPaint.setAntiAlias(true);//设置画笔抗锯齿
        curPaint.setStyle(Paint.Style.STROKE);//设置画笔（忘了）
        curPaint.setStrokeWidth(curProgressWidth);//设置画笔宽度
        curPaint.setColor(Color.parseColor("#ee5514"));//设置画笔颜色
    }

    private float dp2Px(float dp) {
        return dp * getContext().getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        width = measureWidth(widthMeasureSpec);//得到view的宽度
        height = measureHeight(heightMeasureSpec);//得到view的高度
        setMeasuredDimension(width, height);//将自己重新测量的宽高度应用到视图上（只设置size而不设置mode，mode是在布局中就确定了的）
    }

    private int measureWidth(int widthMeasureSpec) {
        int result;
        int mode = MeasureSpec.getMode(widthMeasureSpec);//得到measurespec的模式
        int size = MeasureSpec.getSize(widthMeasureSpec);//得到measurespec的大小
        int padding = getPaddingLeft() + getPaddingRight();//得到padding在宽度上的大小
        if (mode == MeasureSpec.EXACTLY)//这种模式对应于match_parent和具体的数值dp
        {
            result = size;
        } else {
            result = getSuggestedMinimumWidth();//得到屏幕能给的最大的view的最小宽度，原话：Returns the suggested minimum width that the view should use. This returns the maximum of the view's minimum width and the background's minimum width
            result += padding;//考虑padding后最大的view最小宽度
            if (mode == MeasureSpec.AT_MOST)//这种模式对应于wrap_parent
            {
                result = Math.max(result, size);
            }
        }
        return result;
    }

    private int measureHeight(int heightMeasureSpec) {
        int result;
        int mode = MeasureSpec.getMode(heightMeasureSpec);
        int size = MeasureSpec.getSize(heightMeasureSpec);
        int padding = getPaddingBottom() + getPaddingTop();
        if (mode == MeasureSpec.EXACTLY) {
            result = size;
        } else {
            result = getSuggestedMinimumHeight();
            result += padding;
            if (mode == MeasureSpec.AT_MOST) {
                result = Math.max(result, size);
            }
        }
        return result;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int tWidth = width - getPaddingRight() - getPaddingLeft();//得到整个view出去padding后的宽度
        int tHeight = height - getPaddingTop() - getPaddingBottom();//得到整个view除去padding后的高度
        int point1X = tWidth + getPaddingLeft() - tWidth;
        int point1Y = getPaddingTop() + tHeight;
        int point2X = tWidth + getPaddingLeft() - tWidth;
        int point2Y = tHeight + getPaddingTop() - tHeight;
        int point3X = getPaddingLeft() + tWidth;
        int point3Y = tHeight + getPaddingTop() - tHeight;
        int point4X = getPaddingLeft() + tWidth;
        int point4Y = getPaddingTop() + tHeight;
        proWidth = point3X - point1X;
        proHeight = point1Y - point3Y;
        allLength = 2 * (proWidth + proHeight);
        curPath = new Path();//当前进度条的路径
        curPath.moveTo(point1X, point1Y);
        float curPersent = (float) curProgress / maxProgress;//当前进度占总进度的百分比
        if (curPersent > 0) {
            if (curPersent <= proWidth / allLength) {//处在第一段上面的小圆点的原点坐标和当前进度条的路径
                dotCX = point1X + allLength * curProgress / maxProgress;
                dotCY = point1Y;
                curPath.lineTo(dotCX, dotCY);
            } else if (curPersent <= (proHeight + proWidth) / allLength) {
                dotCY = point2Y;
                dotCX = point2X + allLength * curProgress / maxProgress - proHeight;
                curPath.lineTo(point2X, point2Y);
                curPath.lineTo(dotCX, dotCY);
            } else if (curPersent <= (2 * proWidth + proHeight) / allLength) {
                dotCX = point3X;
                dotCY = point3Y + allLength * curProgress / maxProgress - proHeight - proWidth;
                curPath.lineTo(point2X, point2Y);
                curPath.lineTo(point3X, point3Y);
                curPath.lineTo(dotCX, dotCY);
            } else if (curPersent <= 1) {
                dotCY = point4Y;
                dotCX = point4X - (allLength * curProgress / maxProgress - proHeight * 2 - proWidth);
                curPath.lineTo(point2X, point2Y);
                curPath.lineTo(point3X, point3Y);
                curPath.lineTo(point4X, point4Y);
                curPath.lineTo(dotCX, dotCY);
            } else if (curPersent > 1) {
                dotCX = point1X;
                dotCY = point1Y;
                curPath.lineTo(point2X, point2Y);
                curPath.lineTo(point3X, point3Y);
                curPath.lineTo(point4X, point4Y);
                curPath.close();
            }
        } else {
            dotCX = point1X;
            dotCY = point1Y;
            curPath.lineTo(point1X, point1Y);
        }
        canvas.drawPath(curPath, curPaint);
    }

    public void setCurProgress(int curProgress) {
        this.curProgress = curProgress;
        invalidate();//更新view界面
    }

}

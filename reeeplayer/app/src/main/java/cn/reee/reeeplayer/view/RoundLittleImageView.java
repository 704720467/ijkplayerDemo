package cn.reee.reeeplayer.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.ImageView;

import cn.reee.reeeplayer.R;


/**
 * 四角都是圆的imageVIew
 *
 * @author zp
 */

public class RoundLittleImageView extends ImageView {
    private final RectF roundRect = new RectF();// 圆角矩形

    private float rect_adius = 6;// 角度

    private final Paint maskPaint = new Paint();

    private final Paint zonePaint = new Paint();

    public RoundLittleImageView(Context context) {
        this(context, null);
    }

    public RoundLittleImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundLittleImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray mTypedArray = context.obtainStyledAttributes(attrs, R.styleable.RoundLittleImageView);
        rect_adius = mTypedArray.getDimension(R.styleable.RoundLittleImageView_image_view_radius, 10);
        mTypedArray.recycle();
        init();
    }

    private void init() {
        maskPaint.setAntiAlias(true);
        maskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        zonePaint.setAntiAlias(true);
        zonePaint.setColor(Color.WHITE);
//        float density = getResources().getDisplayMetrics().density;
//        rect_adius = rect_adius * density;

    }

    public void setRectAdius(float adius) {
        rect_adius = adius;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        // int heightSize = widthSpecSize * 9 / 16;
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(widthSpecSize, heightSize);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right,

                            int bottom) {

        super.onLayout(changed, left, top, right, bottom);

        int w = getWidth();

        int h = getHeight();
        // roundRect.set(0, 0, w, h + rect_adius);// 仅上部
        // roundRect.set(0, rect_adius, w, h ); 仅下边有圆角
        roundRect.set(0, 0, w, h); // 全圆角
        // roundRect.set(0, 0, w + rect_adius, h + rect_adius); //仅 左上角是圆角

    }

    @Override
    public void draw(Canvas canvas) {
        // System.out.println("draw");
        canvas.saveLayer(roundRect, zonePaint, Canvas.ALL_SAVE_FLAG);// 单独分配了一个画布用于绘制图层
        // 它定义了一个画布区域（可设置透明度），此方法之后的所有绘制都在此区域中绘制，直到调用canvas.restore()方法
        canvas.drawRoundRect(roundRect, rect_adius, rect_adius, zonePaint);
        canvas.saveLayer(roundRect, maskPaint, Canvas.ALL_SAVE_FLAG);
        super.draw(canvas);
        canvas.restore();
    }
}

package cn.reee.reeeplayer.view.CutScrollLayout;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cn.reee.reeeplayer.R;

/**
 * 裁剪布局
 * Create by zp on 2019-11-30
 */
public class CutView extends FrameLayout implements View.OnTouchListener {
    private View mRootLayout;
    private ImageView mLeftBarView;
    private ImageView mRightBarView;
    private ChunkViewLayout currentSelectView;
    private View leftMask;
    private View mSelectLeftMaskView;//最上层裁剪布局 左边的蒙版，来调整裁剪把手的位置


    private int touchType = 0;//1 左边 2右边

    public CutView(@NonNull Context context) {
        this(context, null);
    }

    public CutView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CutView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
        addListener();
    }

    private void initView() {
        mRootLayout = LayoutInflater.from(getContext()).inflate(R.layout.cut_view_layout, null);
        mLeftBarView = mRootLayout.findViewById(R.id.left_bar_view);
        mRightBarView = mRootLayout.findViewById(R.id.right_bar_view);
        addView(mRootLayout);
    }

    public View getCurrentSelectView() {
        return currentSelectView;
    }

    public void initCutView(ChunkViewLayout currentSelectView, View leftMask, View mSelectLeftMaskView) {
        this.currentSelectView = currentSelectView;
        this.leftMask = leftMask;
        this.mSelectLeftMaskView = mSelectLeftMaskView;
    }

    private void addListener() {
        if (mLeftBarView != null)
            mLeftBarView.setOnTouchListener(this);
        if (mRightBarView != null)
            mRightBarView.setOnTouchListener(this);
    }

    private float downX;

    @Override
    public boolean onTouch(View v, MotionEvent motionEvent) {
        if (v == mLeftBarView) touchType = 1;
        if (v == mRightBarView) touchType = 2;

        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = motionEvent.getRawX();
                break;
            case MotionEvent.ACTION_MOVE:
                requestDisallowInterceptTouchEvent(true);//禁止父类拦截事件 ,DOWN 时间设置不顶用，应为DOWN回清楚状态
                int moveLength = Math.round(downX - motionEvent.getRawX());
                if (touchType == 1) {
                    cutLeft(moveLength);
                }
                if (touchType == 2) {
                    cutRight(moveLength);
                }
                downX = motionEvent.getRawX();
                break;
            case MotionEvent.ACTION_UP:
                Log.e("======>", "ACTION_UP！");
                break;
            case MotionEvent.ACTION_CANCEL:
                Log.e("======>", "ACTION_CANCEL！");
                break;
        }
        return true;
    }

    /**
     * 裁剪左边
     *
     * @param moveLength
     */
    private void cutLeft(int moveLength) {
        //1.设置 选中控件的大小
        boolean canMove = currentSelectView.cutLeft(moveLength);
        if (!canMove) return;
        currentSelectView.getLayoutParams().width = currentSelectView.getWidth() + moveLength;
        //设置最底层最左边的空白宽度
        leftMask.getLayoutParams().width = leftMask.getWidth() - moveLength;
        //设置顶层裁剪布局 左边留白宽度
        mSelectLeftMaskView.getLayoutParams().width = mSelectLeftMaskView.getWidth() - moveLength;
        //设置裁剪控件 宽度 保证能与 选中控件 一样宽
        getLayoutParams().width = currentSelectView.getLayoutParams().width + mRightBarView.getWidth() * 2;
        currentSelectView.requestLayout();
    }

    /**
     * 裁剪右边
     *
     * @param moveLength
     */
    private void cutRight(int moveLength) {
        boolean canMove = currentSelectView.cutRight(moveLength);
        if (!canMove) return;
        //设置裁剪控件 宽度 保证能与 选中控件 一样宽
        currentSelectView.getLayoutParams().width = currentSelectView.getWidth() - moveLength;
        getLayoutParams().width = currentSelectView.getLayoutParams().width + mRightBarView.getWidth() * 2;
        currentSelectView.requestLayout();
    }
}

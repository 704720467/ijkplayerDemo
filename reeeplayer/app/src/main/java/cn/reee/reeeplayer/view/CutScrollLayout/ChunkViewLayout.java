package cn.reee.reeeplayer.view.CutScrollLayout;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.view.View;

import cn.reee.reeeplayer.R;

/**
 * 每一个视频对应的 图片预览布局
 * Create by zp on 2019-12-05
 */
public class ChunkViewLayout extends FrameLayout {
    private View mRootLayout;
    private long chunkDuration;//chunk时长
    private int MIN_LENGTH = -1;//布局最小长度
    private int MAX_LENGTH = -1;//布局最大长度

    public ChunkViewLayout(Context context) {
        this(context, null);
    }

    public ChunkViewLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChunkViewLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mRootLayout = LayoutInflater.from(getContext()).inflate(R.layout.chunk_view_layout, null);
//        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
//        lp.gravity = Gravity.RIGHT;
        addView(mRootLayout);

        setBackgroundColor(Color.parseColor("#000000"));
    }

    /**
     * 裁剪左边
     *
     * @param length 移动的距离
     * @return true 可以移动 false 不可以移动
     */
    public boolean cutLeft(float length) {
        initLengthData();
        mRootLayout.getLayoutParams().width = mRootLayout.getWidth();
        float cutLeft = Math.min(mRootLayout.getTranslationX() + length, 0);
        //计算出裁剪后的可用宽度
        float afterCutRootLength = length + mRootLayout.getWidth() + mRootLayout.getTranslationX();
        boolean canCut = afterCutRootLength <= MAX_LENGTH && afterCutRootLength >= MIN_LENGTH;
        if (cutLeft == 0) {
            mRootLayout.setTranslationX(cutLeft);
            return false;
        }
        if (canCut) {
            mRootLayout.setTranslationX(cutLeft);
        }
        return canCut;
    }

    /**
     * 裁剪左边
     *
     * @param length
     */
    public boolean cutRight(int length) {
        initLengthData();
        int rootLength = mRootLayout.getWidth() - length;
        boolean canCut = rootLength <= MAX_LENGTH && rootLength >= (MIN_LENGTH + Math.abs(mRootLayout.getTranslationX()));
        if (!canCut) return false;
        mRootLayout.getLayoutParams().width = rootLength;
        return true;
    }

    /**
     * 初始化长度的最小值和最大值
     */
    private void initLengthData() {
        if (MAX_LENGTH == -1)
            MAX_LENGTH = mRootLayout.getWidth();
        if (MIN_LENGTH == -1)
            MIN_LENGTH = 150;
    }
}

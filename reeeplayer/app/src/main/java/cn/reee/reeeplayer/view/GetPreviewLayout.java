package cn.reee.reeeplayer.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import cn.reee.reeeplayer.R;
import cn.reee.reeeplayer.view.seekBar.CutSeekBar;

/**
 * Get 预览界面
 * Create by zp on 2019-11-25
 */
public class GetPreviewLayout extends FrameLayout implements View.OnClickListener {
    private View mRootLayout;
    private ImageView mIcDownBack;//返回
    private TextView mTvToEdit;//跳转编辑界面
    private TextView mTvDown;//下载当前选中视频
    private LinearLayout mGetCounts;//Get 布局展示
    private LinearLayout mDeleteLayout;//删除片段
    private FrameLayout mContentVideoLayout;//视频播放父布局
    private RelativeLayout mBeforeLayout;//上一段
    private RelativeLayout mNextLayout;//下一段
    private CutSeekBar mCutSeekBar;//裁剪时长界面

    public GetPreviewLayout(Context context) {
        this(context, null);
    }

    public GetPreviewLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GetPreviewLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mRootLayout = LayoutInflater.from(getContext()).inflate(R.layout., null);
        addView(mRootLayout);
        initView();
        addOnClick();
    }

    private void initView() {
        mIcDownBack = mRootLayout.findViewById(R.id.ic_down_back);
        mTvToEdit = mRootLayout.findViewById(R.id.tv_to_edit);
        mTvDown = mRootLayout.findViewById(R.id.tv_down);
        mGetCounts = mRootLayout.findViewById(R.id.get_counts);
        mDeleteLayout = mRootLayout.findViewById(R.id.delete_layout);
        mContentVideoLayout = mRootLayout.findViewById(R.id.content_video_layout);
        mBeforeLayout = mRootLayout.findViewById(R.id.before_layout);
        mNextLayout = mRootLayout.findViewById(R.id.next_layout);
        mCutSeekBar = mRootLayout.findViewById(R.id.cut_seek_bar);
    }


    private void addOnClick() {
        if (mIcDownBack != null)
            mIcDownBack.setOnClickListener(this);
        if (mTvToEdit != null)
            mTvToEdit.setOnClickListener(this);
        if (mTvDown != null)
            mTvDown.setOnClickListener(this);
        if (mDeleteLayout != null)
            mDeleteLayout.setOnClickListener(this);
        if (mBeforeLayout != null)
            mBeforeLayout.setOnClickListener(this);
        if (mNextLayout != null)
            mNextLayout.setOnClickListener(this);

    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ic_down_back:

                break;
            case R.id.tv_to_edit:

                break;
            case R.id.tv_down:

                break;
            case R.id.delete_layout:

                break;
            case R.id.before_layout:

                break;
            case R.id.next_layout:

                break;
        }
    }
}

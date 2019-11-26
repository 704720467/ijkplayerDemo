package cn.reee.reeeplayer.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import cn.reee.reeeplayer.R;
import cn.reee.reeeplayer.adapter.StadiumAngleAdapter;
import cn.reee.reeeplayer.adapter.StadiumDataAdapter;
import cn.reee.reeeplayer.adapter.StadiumHalfAdapter;
import cn.reee.reeeplayer.base.BaseListAdapter;
import cn.reee.reeeplayer.modle.StadiumAngleAdapterModle;
import cn.reee.reeeplayer.modle.StadiumDataAdapterModle;
import cn.reee.reeeplayer.modle.StadiumHalfAdapterModle;

/**
 * 切换 日期、角度、相邻半场 布局
 * Create by zp on 2019-11-22
 */
public class ChangeDateAndStadium extends FrameLayout {
    private View mRootLayout;
    private int mVideoScaleType = 0;//0默认状态 内容全部展示 1填满整个屏幕
    private ImageView mIvToFullScreen;
    private TextView mTvToFullScreen;
    private RecyclerView mRvDate;//日期列表
    private RecyclerView mRvAngle;//角度列表
    private RecyclerView mRvHalf;//半场列表

    private StadiumDataAdapter stadiumDataAdapter;
    private StadiumAngleAdapter stadiumAngleAdapter;
    private StadiumHalfAdapter stadiumHalfAdapter;


    public ChangeDateAndStadium(Context context) {
        this(context, null);
    }

    public ChangeDateAndStadium(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChangeDateAndStadium(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mRootLayout = LayoutInflater.from(getContext()).inflate(R.layout.change_date_and_stadium_layout, null);
        addView(mRootLayout);
        initView();
        initRvDate();
        initRvAngle();
        initRvHalf();
    }

    private void initView() {
        mIvToFullScreen = mRootLayout.findViewById(R.id.im_to_full_screen);
        mTvToFullScreen = mRootLayout.findViewById(R.id.tv_to_full_screen);
        mRvDate = mRootLayout.findViewById(R.id.rv_date);
        mRvAngle = mRootLayout.findViewById(R.id.rv_angle);
        mRvHalf = mRootLayout.findViewById(R.id.rv_half);
    }

    private void initRvDate() {
        ArrayList<StadiumDataAdapterModle> stadiumDataAdapterModles = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            stadiumDataAdapterModles.add(new StadiumDataAdapterModle());
        }
        stadiumDataAdapter = new StadiumDataAdapter(getContext());
        mRvDate.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
        mRvDate.setAdapter(stadiumDataAdapter);
        stadiumDataAdapter.setNewDatas(stadiumDataAdapterModles);
        stadiumDataAdapter.setOnItemClickListener(new BaseListAdapter.OnItemClickListener() {
            @Override
            public void onClick(int position, View view, Object itemData) {
                stadiumDataAdapter.selectPosition(position);
            }
        });
    }

    private void initRvAngle() {
        ArrayList<StadiumAngleAdapterModle> stadiumAngleAdapterModles = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            stadiumAngleAdapterModles.add(new StadiumAngleAdapterModle());
        }
        stadiumAngleAdapter = new StadiumAngleAdapter(getContext());
        mRvAngle.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
        mRvAngle.setAdapter(stadiumAngleAdapter);
        stadiumAngleAdapter.setNewDatas(stadiumAngleAdapterModles);
        stadiumAngleAdapter.setOnItemClickListener(new BaseListAdapter.OnItemClickListener() {
            @Override
            public void onClick(int position, View view, Object itemData) {
                stadiumAngleAdapter.selectPosition(position);
            }
        });
    }

    private void initRvHalf() {

        ArrayList<StadiumHalfAdapterModle> stadiumHalfAdapters = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            stadiumHalfAdapters.add(new StadiumHalfAdapterModle());
        }
        stadiumHalfAdapter = new StadiumHalfAdapter(getContext());
        mRvHalf.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
        mRvHalf.setAdapter(stadiumHalfAdapter);
        stadiumHalfAdapter.setNewDatas(stadiumHalfAdapters);
        stadiumHalfAdapter.setOnItemClickListener(new BaseListAdapter.OnItemClickListener() {
            @Override
            public void onClick(int position, View view, Object itemData) {
                stadiumHalfAdapter.selectPosition(position);
            }
        });
    }
}

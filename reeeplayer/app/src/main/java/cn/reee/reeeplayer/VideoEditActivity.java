package cn.reee.reeeplayer;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.zp.libvideoedit.WidthHeightRatioGlSurfaceView;

import java.util.ArrayList;
import java.util.Arrays;

import cn.reee.reeeplayer.adapter.TextListAdapter;
import cn.reee.reeeplayer.base.VideoEditBaseActivity;
import cn.reee.reeeplayer.util.FilterModule;

/**
 * 视频编辑
 */
public class VideoEditActivity extends VideoEditBaseActivity {

    private String videoPath;
    private TextView mTvPath;
    private TextView mTvCost;
    private Button mBtPlay;
    private RecyclerView rvView;
    private TextListAdapter textListAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_videio_edit);

        videoPath = getIntent().getExtras().getString("path");
        initView();
        initSurfaceView(R.id.video_surface_view);
        setVideoList(Arrays.asList(videoPath));
        loadVideo();
    }

    private void initView() {
        initRv();
        mTvPath = findViewById(R.id.path);
        mBtPlay = findViewById(R.id.bt_play);
        mTvCost = findViewById(R.id.tv_cost);
        mTvPath.setText(videoPath);
        mBtPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (player.isPlaying())
                    player.pause();
                else
                    player.play();
            }
        });
    }

    private void initRv() {
        rvView = findViewById(R.id.cost_list);
        textListAdapter = new TextListAdapter(this);
        rvView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, true));
        rvView.setAdapter(textListAdapter);
    }

    @Override
    public void onDrawCostTime(final long startTime, final long endTime) {
        rvView.post(new Runnable() {
            @Override
            public void run() {
                textListAdapter.addDatas("渲染耗时：" + (endTime - startTime));
            }
        });
    }
}

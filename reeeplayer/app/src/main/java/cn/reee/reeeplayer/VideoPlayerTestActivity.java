package cn.reee.reeeplayer;

import android.os.Bundle;
import android.view.View;

import cn.reee.reeeplayer.base.BaseActivity;
import cn.reee.reeeplayer.player.ReeeIjkplayer;
import cn.reee.reeeplayer.widget.media.IjkVideoView;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class VideoPlayerTestActivity extends BaseActivity {

    private ReeeIjkplayer mVideoView;
    private String mVideoPath;
    private String mVideoPath2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player_test);
        initData();
        initView();
        initVideo();
    }

    private void initData() {
        //老播放器能播放的
//        mVideoPath = "http://qiniunstadium.reee.cn/m3u8/20191025/CG000113/CA000378/20191025154016/be725996eb8e4f8abd237b1452360298.m3u8";
//        mVideoPath = "http://qiniunstadium.reee.cn/m3u8/20191028/CG000111/CA000374/20191028163657/602bf0b44a454ceaadf14c48d2eef716.m3u8";
//        mVideoPath = "http://192.168.1.155:8888/1e7b452d3db6499c949a11600b5b2a9f.m3u8";
        mVideoPath = "http://192.168.1.155:8888/ac0361f872af437585b9d406332a5320.m3u8";
        //不能播放的 老播放器不能播放的
        mVideoPath2 = "http://qiniunstadium.reee.cn/m3u8/20191025/CG000112/CA000376/20191025154016/d730481c1a0a439d904e6e09f0a3085b.m3u8";
    }

    private void initView() {
        mVideoView = findViewById(R.id.reeeIjkplayer_layout);
        findViewById(R.id.bg_play).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mVideoView != null)
                    mVideoView.start();
            }
        });
    }

    private void initVideo() {
        IjkMediaPlayer.loadLibrariesOnce(null);
        IjkMediaPlayer.native_profileBegin("libijkplayer.so");
        mVideoView.setVideoPath(mVideoPath);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mVideoView != null)
            mVideoView.pause();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        IjkMediaPlayer.native_profileEnd();
    }
}

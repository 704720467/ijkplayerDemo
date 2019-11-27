package cn.reee.reeeplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.MediaController;
import android.widget.VideoView;

import com.zp.libvideoedit.utils.ToastUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class VideoViewPlayTsActivity extends AppCompatActivity {

    private VideoView mVideoViewFrist;
    private VideoView mVideoViewSecond;
    int playTimes = 0;
    List<String> videoPathList = Arrays.asList(
            "/sdcard/Reee/ts/bh/191119150016476_2.00_1920X1080_30_00000006.ts",
            "/sdcard/Reee/ts/bh/191119150018473_2.00_1920X1080_30_00000007.ts",
            "/sdcard/Reee/ts/bh/191119150020484_2.01_1920X1080_30_00000008.ts",
            "/sdcard/Reee/ts/bh/191119150022486_2.00_1920X1080_30_00000009.ts"
    );

    Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_view_play_ts);
        mVideoViewFrist = findViewById(R.id.video_view);
        mVideoViewSecond = findViewById(R.id.video_view_second);
        initVideoView();
        findViewById(R.id.bt_play).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playTimes = 0;
                mVideoViewFrist.start();
                iniTimer();
            }
        });


    }

    private void iniTimer() {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (mVideoViewFrist.isPlaying() && mVideoViewFrist.getCurrentPosition() >= mVideoViewFrist.getDuration() - 600) {
//                    mVideoViewSecond.setVisibility(View.INVISIBLE);
                    if (!mVideoViewSecond.isPlaying())
                        mVideoViewSecond.start();
                }
            }
        }, 60, 10);
    }

    private void initVideoView() {
        /**媒体控制面版常用方法：MediaController:
         hide();     隐藏MediaController;
         show();     显示MediaController
         show(int timeout);设置MediaController显示的时间，以毫秒计算，如果设置为0则一直到调用hide()时隐藏；
         */
//        mVideoViewFrist.setMediaController(new MediaController(this));//设置控制器
        /*File file = new File(Environment.getExternalStorageDirectory(),"movie.3gp");
        videoView.setVideoPath(file.getPath()); // 指定视频文件的路径*/
        mVideoViewFrist.setVideoPath(videoPathList.get(0));//播放网络视频
        mVideoViewFrist.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
//                mVideoViewFrist.setVisibility(View.INVISIBLE);
//                mVideoViewSecond.setVisibility(View.VISIBLE);
//                mVideoViewSecond.requestFocus();
//                mVideoViewSecond.start();
//                playTimes++;
//                if (playTimes < videoPathList.size()) {
//                    mVideoViewFrist.setVideoPath(videoPathList.get(playTimes));
//                    mVideoViewFrist.requestFocus();
//                    mVideoViewFrist.start();
//                } else {
//                    ToastUtil.showToast(VideoViewPlayTsActivity.this, "播放完毕");
//                }
                mVideoViewSecond.setVisibility(View.VISIBLE);
            }
        });
        mVideoViewFrist.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                Log.e("======>", "mVideoViewFrist准备完毕");
            }
        });
        mVideoViewSecond.setVideoPath(videoPathList.get(1));//播放网络视频
        mVideoViewSecond.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                ToastUtil.showToast(VideoViewPlayTsActivity.this, "播放完毕");
//                mVideoViewFrist.requestFocus();
//                mVideoViewFrist.start();
                mVideoViewFrist.setVisibility(View.VISIBLE);
                mVideoViewSecond.setVisibility(View.INVISIBLE);
            }
        });
        mVideoViewSecond.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                Log.e("======>", "mVideoViewSecond准备完毕");

            }
        });
    }
}

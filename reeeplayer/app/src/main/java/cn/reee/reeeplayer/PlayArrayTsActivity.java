package cn.reee.reeeplayer;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import com.zp.libvideoedit.Constants;
import com.zp.libvideoedit.EditCore.VideoPlayer;
import com.zp.libvideoedit.Time.CMTime;
import com.zp.libvideoedit.Time.CMTimeRange;
import com.zp.libvideoedit.VideoEditUtils;
import com.zp.libvideoedit.VideoEffect;
import com.zp.libvideoedit.WidthHeightRatioGlSurfaceView;
import com.zp.libvideoedit.exceptions.InvalidVideoSourceException;
import com.zp.libvideoedit.modle.AudioFile;
import com.zp.libvideoedit.modle.BuildType;
import com.zp.libvideoedit.modle.MediaComposition;
import com.zp.libvideoedit.modle.Transition.Origentation;
import com.zp.libvideoedit.modle.VideoBean;
import com.zp.libvideoedit.modle.VideoFile;
import com.zp.libvideoedit.modle.VideoPlayerCallBack;
import com.zp.libvideoedit.utils.ToastUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import cn.reee.reeeplayer.base.BaseActivity;
import cn.reee.reeeplayer.util.ScreenUtil;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static com.zp.libvideoedit.Constants.TAG_V;
import static com.zp.libvideoedit.Constants.VERBOSE_SEEK;
import static com.zp.libvideoedit.Constants.VERBOSE_V;

public class PlayArrayTsActivity extends BaseActivity {
    WidthHeightRatioGlSurfaceView playerview;
    private String productId;
    public static int videoProportion = 0;
    public VideoEffect effect;
    public VideoPlayer player;
    public Handler handler;
    Timer timer;
    private MediaComposition composition;
    private List<VideoBean> videoBeanList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_array_ts);
        VideoEditUtils.init(this);
        playerview = findViewById(R.id.playerview);
        handler = new Handler();
        initPlayer();
        findViewById(R.id.bt_init).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadVideo();
            }
        });
        findViewById(R.id.bt_play).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (player.isPlaying()) player.pause();
                else
                    player.play();
            }
        });

    }

    private void loadVideo() {
        List<String> videoPathList = Arrays.asList(
//                "/sdcard/Reee/tsCache/test6.mp4"
//                , "/sdcard/Reee/tsCache/test7.mp4"
                "/sdcard/Reee/ts/bh/191119150016476_2.00_1920X1080_30_00000006.ts"
                , "/sdcard/Reee/ts/bh/191119150018473_2.00_1920X1080_30_00000007.ts"
//                , "/sdcard/Reee/ts/bh/191119150020484_2.01_1920X1080_30_00000008.ts"
//                , "/sdcard/Reee/ts/bh/191119150022486_2.00_1920X1080_30_00000009.ts"
        );
        videoBeanList.clear();
        Observable.from(videoPathList)
                .map(new Func1<String, VideoBean>() {
                    @Override
                    public VideoBean call(String path) {
                        try {
                            VideoFile videoFile = VideoFile.getVideoFileInfo(path, PlayArrayTsActivity.this);
                            AudioFile audioFile = AudioFile.getAudioFileInfo(path, PlayArrayTsActivity.this);
                            VideoBean videoBean = new VideoBean();
                            videoBean.setPath(path);
                            videoBean.setVideoFile(videoFile);
                            videoBean.setAudioFile(audioFile);
                            return videoBean;
                        } catch (InvalidVideoSourceException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<VideoBean>() {
                    @Override
                    public void onCompleted() {

                        for (int i = 0; i < videoBeanList.size(); i++) {
                            VideoBean videoBean = videoBeanList.get(i);
                            try {
                                float jingdu = 1000;
                                float weidu = 1000;
                                effect.addChunk(videoBean.getPath(), videoBean.getVideoFile(),
                                        videoBean.getAudioFile(), jingdu, weidu);
                            } catch (InvalidVideoSourceException e) {
                                e.printStackTrace();
                            }
                        }
                        build();
                    }

                    @Override
                    public void onError(Throwable e) {
                        ToastUtil.showToast(PlayArrayTsActivity.this, getString(R.string.video_decode_error));
                        finish();
                    }

                    @Override
                    public void onNext(VideoBean videoBean) {
                        if (videoBean == null) {
                            ToastUtil.showToast(PlayArrayTsActivity.this, getString(R.string.video_decode_error));
                            finish();
                            return;
                        }
                        videoBeanList.add(videoBean);
                    }
                });
    }

    private void build() {
        composition = effect.getProjectMediaComposition(false);
        player.setMediaComposition(composition, effect,
                new VideoPlayer.VideoCompositionCallBack2() {
                    @Override
                    public void setCompositionComplete() {
//                        isBuilding = false;
//
//                        setCurrentDuration(nowDuration, true);
//
//                        //只有添加装饰的build needPlay 为 true
//                        if (needPlay) {
//                            handler.postDelayed(new Runnable() {
//                                @Override
//                                public void run() {
//                                    startPlay();
//                                }
//                            }, 50);
//                            return;
//                        }
//
//                        if (!needUpdatePlayer) {
//                            return;
//                        }
//
//                        handler.postDelayed(new Runnable() {
//                            @Override
//                            public void run() {
//                                seekBeforePlaying();
//                            }
//                        }, 200);
//
//
//                        //某些手机无法更新当前画面，主动再次seek下
//                        handler.postDelayed(new Runnable() {
//                            @Override
//                            public void run() {
//                                if (isFinishing()) {
//                                    return;
//                                }
//                                double seekto = currentDuration;
//                                if (currentDuration < effect.getProjectDuration()) {
//                                    seekto += 0.04;
//                                } else {
//                                    seekto -= 0.04;
//                                }
//                                currentDuration = seekto;
//                                if (!player.isPlaying()) {
//                                    player.seekTo(new CMTime(seekto));
//                                    player.seekTo(new CMTime(currentDuration));
//                                }
//                            }
//                        }, 600);
//
//                        LogUtil.e("duration", effect.getProjectDuration() + " ...........");
////                        if (!isUser) {
////                            if (tiaosuModule.updateAllPointForChunkChanged()) {
////                                chunkBuild(true, true);
////                            }
////                        }
//
//                        if (updateTypeset) {
//                            typesetManager.updateTypeset();
//                        }
                    }
                }, BuildType.BuildType_Default);
    }

    private void setSurfaceViewSize() {
        RelativeLayout.LayoutParams layoutParams =
                (RelativeLayout.LayoutParams) playerview.getLayoutParams();
        //此处横竖屏比例变化排版view的操作：
        //竖屏时，view宽度等于屏幕宽度，view高度等于屏幕宽度 / (9 / 16)然后进行缩放处理
        //横屏时，view高度等于屏幕宽度，view宽度等于屏幕宽度 / (9 / 16)然后进行缩放处理
        if (videoProportion == 0) {//0竖屏//1横屏
//            int height = ScreenUtil.getScreenHeightSize(this);
            int height = 360;
            layoutParams.height = height;
            layoutParams.width = (int) (height * (16f / 9));
            playerview.setWh_ratio(16f / 9);
        } else {
            int width = ScreenUtil.getScreenWidthSize(this);
            layoutParams.height = (int) (width * (9f / 16));
            layoutParams.width = width;
            playerview.setWh_ratio(16f / 9);
        }
        playerview.setLayoutParams(layoutParams);
    }

    private void initPlayer() {
        setSurfaceViewSize();
        try {
            effect = new VideoEffect(this, productId, true,
                    new VideoEffect.VideoEffectSaveEventCallback() {
                        @Override
                        public void videoEffectSaveEvent() {
//                            addDraft();
                        }
                    });
        } catch (Exception e) {
            Log.e("", " ==================创建了！VideoEffect");
            ToastUtil.showToast(this, getString(R.string.no_find_videopath));
            return;
        }

        player = new VideoPlayer(this, playerview, new VideoPlayerCallBack() {
            @Override
            public void onPlayerReady() {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (VERBOSE_SEEK)
                            Log.i(TAG_V, "EditorActivity.seekTo:0" + "initSurfaceView_onPlayerReady");
                        player.seekTo(CMTime.zeroTime());
                    }
                }, 80);
            }

            @Override
            public void onPuased() {
                if (timer != null) {
                    timer.cancel();
                }

            }


            @Override
            public void onPlaying(double percent, final long currentPalyTimeInOrange) {
                if (VERBOSE_V) Log.d("onPlaying", "percent:" + percent);
            }

            @Override
            public void onPlayFinished() {
                if (VERBOSE_V) Log.d("onPlaying", "percent:onPlayFinished");

            }
        });
    }

    /**
     * 获取当前时间，播放时调用
     */
    private void getCurrentDurationFromPlayer() {
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                double currentTime = CMTime.getSecond(player.currentTime());
//                                if (isPlayingWithTimeRange) {
//                                    if (currentTime >= playingTimeRange.getEnd().getSecond()) {
////                                        pausePlay();
//                                        timer.cancel();
//
//                                        handler.postDelayed(new Runnable() {
//                                            @Override
//                                            public void run() {
//                                                startPlay(playingTimeRange);
//                                            }
//                                        }, 10);
//
//                                        return;
//
//                                    }
//                                }
//                                if (VERBOSE_V) LogUtil.d("onPlaying", "currentTime:" + currentTime);
//                                setCurrentDuration(currentTime, false);
//                                goToDuration(currentDuration, 0);
//                                //录音
//                                if (studioMode == 8 && menusManager.btnRecord.getStatus() == 2) {
//                                    audioRecordModule.updateVolumeWave();
//                                }
//
//                                //特效
//                                if (studioMode == 17 && isAudioRecording) {
//                                    editorSpecialModule.updateViewWidth();
//                                }
                            }
                        });
                    }
                });

            }
        }, 0, 20);
    }
}

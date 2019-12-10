package cn.reee.reeeplayer.base;

import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.annotation.IdRes;


import com.zp.libvideoedit.EditCore.VideoPlayer;
import com.zp.libvideoedit.Time.CMTime;
import com.zp.libvideoedit.VideoEffect;
import com.zp.libvideoedit.WidthHeightRatioGlSurfaceView;
import com.zp.libvideoedit.exceptions.InvalidVideoSourceException;
import com.zp.libvideoedit.modle.AudioFile;
import com.zp.libvideoedit.modle.BuildType;
import com.zp.libvideoedit.modle.MediaComposition;
import com.zp.libvideoedit.modle.TimeScaleModel;
import com.zp.libvideoedit.modle.VideoBean;
import com.zp.libvideoedit.modle.VideoFile;
import com.zp.libvideoedit.modle.VideoPlayerCallBack;
import com.zp.libvideoedit.utils.ToastUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.UUID;

import cn.reee.reeeplayer.R;
import cn.reee.reeeplayer.util.ProgressDialogUtil;
import cn.reee.reeeplayer.util.ScreenUtil;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static com.zp.libvideoedit.EditConstants.TAG_V;
import static com.zp.libvideoedit.EditConstants.VERBOSE;
import static com.zp.libvideoedit.EditConstants.VERBOSE_SEEK;

/**
 * 视频相关 基础类
 * Create by zp on 2019-12-07
 */
public class VideoEditBaseActivity extends BaseActivity implements VideoPlayerCallBack {
    private WidthHeightRatioGlSurfaceView mVideoSurfaceView;

    public static int videoProportion = 0;
    public VideoEffect effect;
    public VideoPlayer player;
    private String productId;
    public Handler handler;
    public Timer timer;
    public ArrayList<String> videoList;
    private List<VideoBean> videoBeanList = new ArrayList<>();
    private boolean isLoading = false;//是否在加载中
    private boolean isLoadingSucess = false;//是否加载成功
    private MediaComposition composition;
    public ProgressDialogUtil progressDialogUtil;
    private int canMoveLength;


    /**
     * 初始化播放surface view
     *
     * @param surfaceViewId
     */
    public void initSurfaceView(@IdRes int surfaceViewId) {
        mVideoSurfaceView = findViewById(surfaceViewId);
        handler = new Handler();
        videoList = new ArrayList<>();
        progressDialogUtil = new ProgressDialogUtil(this);
        progressDialogUtil.showWaiteDialog();
        initPlayer();
        setSurfaceViewSize();
    }

    /**
     * 添加播放 文件
     *
     * @param videoList
     */
    public void setVideoList(List<String> videoList) {
        if (this.videoList == null) this.videoList = new ArrayList<>();
        this.videoList.clear();
        this.videoList.addAll(videoList);
    }

    /**
     * 调整 画面大小
     */
    private void setSurfaceViewSize() {
        RelativeLayout.LayoutParams layoutParams =
                (RelativeLayout.LayoutParams) mVideoSurfaceView.getLayoutParams();
        //此处横竖屏比例变化排版view的操作：
        //竖屏时，view宽度等于屏幕宽度，view高度等于屏幕宽度 / (9 / 16)然后进行缩放处理
        //横屏时，view高度等于屏幕宽度，view宽度等于屏幕宽度 / (9 / 16)然后进行缩放处理
        if (videoProportion == 0) {//0竖屏//1横屏
            layoutParams.width = ScreenUtil.getScreenWidth(this);
            layoutParams.height = Math.round(layoutParams.width / 9f * 16);
//            mVideoSurfaceView.setWh_ratio(9f / 16);
        } else {
//            int width = ScreenUtil.getScreenWidthSize(this);
//            layoutParams.height = (int) (width * (16f / 9));
//            layoutParams.width = width;
            mVideoSurfaceView.setWh_ratio(16f / 9);
        }
        mVideoSurfaceView.setLayoutParams(layoutParams);
    }


    public void initPlayer() {
        if (player != null) {
            player.pause();
            player.release();
        }
        try {
            productId = UUID.randomUUID().toString().replace("-", "");
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
        player = new VideoPlayer(this, mVideoSurfaceView, this);
    }

    /**
     * 加载视频并且播放
     */
    public void loadVideo() {
        if (player.isPlaying())
            player.pause();
        isLoading = true;
        isLoadingSucess = false;
        Observable.from(videoList)
                .map(new Func1<String, VideoBean>() {
                    @Override
                    public VideoBean call(String path) {
                        try {
                            VideoFile videoFile = VideoFile.getVideoFileInfo(path, VideoEditBaseActivity.this);
                            AudioFile audioFile = AudioFile.getAudioFileInfo(path, VideoEditBaseActivity.this);
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
//                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<VideoBean>() {
                    @Override
                    public void onCompleted() {
                        int chunkSize = effect.getmProject().getChunks().size();
                        for (int i = 0; i < chunkSize; i++) {
                            effect.deleteChunk(0);
                        }

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
                        buildPlayer();
                    }

                    @Override
                    public void onError(Throwable e) {
                        ToastUtil.showToast(VideoEditBaseActivity.this, VideoEditBaseActivity.this.getResources().getString(R.string.video_decode_error));
                        isLoading = false;
//                        finish();
                    }

                    @Override
                    public void onNext(VideoBean videoBean) {
                        if (videoBean == null) {
                            ToastUtil.showToast(VideoEditBaseActivity.this, VideoEditBaseActivity.this.getResources().getString(R.string.video_decode_error));
//                            finish();
                            return;
                        }
                        videoBeanList.add(videoBean);
                    }
                });
    }

    protected void buildPlayer() {
        composition = effect.getProjectMediaComposition(false);
        effect.addLutFilterAll("摩登时代", 1.0f);
        player.setMediaComposition(composition, effect,
                new VideoPlayer.VideoCompositionCallBack2() {
                    @Override
                    public void setCompositionComplete() {
                        onCompositionComplete();
//                        handler.postDelayed(new Runnable() {
//                            @Override
//                            public void run() {
//                                player.seekTo(new CMTime(0), false);
//                            }
//                        }, 300);
                    }

                    @Override
                    public void drawCostTime(long startTime, long endTime) {
                        onDrawCostTime(startTime, endTime);
                    }
                }, BuildType.BuildType_Default);
    }

    public void onCompositionComplete() {
        isLoading = false;
        isLoadingSucess = true;
        //加载chunk缩略图
//        mProgressView.initChunkData(effect.getChunks());
        progressDialogUtil.cancelWaiteDialog();
    }

    public void onDrawCostTime(long startTime, long endTime) {
    }

    public void setSpeed(float speed) {
        if (effect.getmProject().getSpeedPoints().isEmpty()) {
            effect.insertSpeedPoint(new TimeScaleModel(new CMTime(0), speed), 0);
        } else {
            effect.updateSpeedPointAtIndex(0, speed);
        }
        buildPlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        release();
    }
//======================player callback start============================

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
    public void onPlaying(double percent, long currentPalyTimeInOrange) {
        if (VERBOSE) Log.d("onPlaying", "percent:" + percent);
//        int totalLength = (int) Math.round(mProgressView.getContentViewWidth() * percent);
//        mProgressView.setChangeCurrentProgress(totalLength);
        changePlayerUI(true);
    }

    @Override
    public void onPuased() {
        if (timer != null) {
            timer.cancel();
        }
        changePlayerUI(false);
    }

    @Override
    public void onPlayFinished() {
        changePlayerUI(false);
        if (VERBOSE) Log.d("onPlaying", "percent:onPlayFinished");
        player.seekTo(new CMTime(0));
//        mProgressView.setChangeCurrentProgress(0);
    }

    public void changePlayerUI(boolean isPlaying) {
    }

    public void setViewVisibility(final View view, final int visibility) {
        if (view == null) return;
        view.post(new Runnable() {
            @Override
            public void run() {
                view.setVisibility(visibility);
            }
        });
    }

    public void changePlayState() {
        if (player == null) return;
        if (player.isPlaying())
            player.pause();
        else
            player.play();
    }
//======================player callback end============================


    public void release() {
        if (player != null)
            player.stop();
        if (effect != null)
            effect.relaseExportManeger();
        if (player != null)
            player.release();
    }
}

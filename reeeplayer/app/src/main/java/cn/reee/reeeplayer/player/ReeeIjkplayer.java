package cn.reee.reeeplayer.player;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TableLayout;
import android.widget.TextView;

import cn.reee.reeeplayer.R;
import cn.reee.reeeplayer.util.OrientationUtils;
import cn.reee.reeeplayer.util.ScreenUtil;
import cn.reee.reeeplayer.util.TimeUtil;
import cn.reee.reeeplayer.view.AdapterView.TextureVideoViewOutlineProvider;
import cn.reee.reeeplayer.widget.media.IjkVideoView;
import tv.danmaku.ijk.media.player.IMediaPlayer;

import android.view.View.OnClickListener;

import androidx.appcompat.widget.TintContextWrapper;

import java.util.Timer;
import java.util.TimerTask;

/**
 * 直接基于Ijkplayer 的播放器
 * Create by zp on 2019-11-01
 */
public class ReeeIjkplayer extends FrameLayout implements OnClickListener
        , IMediaPlayer.OnPreparedListener, IMediaPlayer.OnInfoListener {
    private String TAG = "ReeeIjkplayer";

    private View mRootLayout;
    private LinearLayout mBottomContralView;//底部 控制条父布局
    private ImageView mBottomStart;//底部 开始按钮
    private TextView mTvCurrentTime;//底部 当前播放时间
    private TextView mTvTotalTime;//底部 视频总时长
    private SeekBar mBottomSeekBar;//底部 进度拖动条
    private ProgressBar mBottomProgressBar;//底部 当底部控制条隐藏式展示
    private ImageView mFullscreen;//底部 全屏按钮
    private View mLoadingView;//视频加载动画布局
    private ImageView mMiddleStart;//中间 开始按钮
    private ImageView mIvRepeat;//重播按钮
    private ImageView mVideoThumb;//视频封面图
    private TableLayout mHudView;//播放器加载视频基础信息

    protected Timer mUpdateProcessTimer;   //进度定时器
    protected ProgressTimerTask mProgressTimerTask;   //定时器任务
    private IjkVideoView mVideoView;//视频播放控件
    private String mVideoPath;//播放地址

    private boolean mMoveSeekBar = false;//是否在拖动SeekBar 防止拖动的时候 进度条来回的跳
    private OrientationUtils orientationUtils;
    private boolean isFullScreen = false;//是否是全屏

    private ViewGroup mNormalParentView;//非全屏播放时父布局 只有切换了全屏后才存在
    private ViewGroup.LayoutParams mNormalLayoutParams;//非全屏播放时 布局的基本参数
    private ViewGroup mFullParentView;//全屏播放父布局 只有切换了全屏后才存在

    private boolean isRoundAnglePlayer = false;//播放器是否时圆角  true 播放器时圆角，false播放器不是圆角


    public ReeeIjkplayer(Context context) {
        this(context, null);
    }

    public ReeeIjkplayer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ReeeIjkplayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        mRootLayout = LayoutInflater.from(getContext()).inflate(getLayout(), null);
        initContralView();
        addOnClickToView();
        initVideoView();
        initSeekBar();
        addView(mRootLayout);
        orientationUtils = new OrientationUtils((Activity) getContext());
    }

    public int getLayout() {
        return R.layout.reee_video_player_normal_layout;
    }

    private void initContralView() {
        mBottomContralView = mRootLayout.findViewById(R.id.layout_bottom);
        mBottomStart = mRootLayout.findViewById(R.id.layout_bottom_start);
        mTvCurrentTime = mRootLayout.findViewById(R.id.tv_current_time);
        mTvTotalTime = mRootLayout.findViewById(R.id.total);
        mBottomSeekBar = mRootLayout.findViewById(R.id.video_seek_bar);
        mBottomProgressBar = mRootLayout.findViewById(R.id.bottom_progressbar);
        mFullscreen = mRootLayout.findViewById(R.id.fullscreen);
        mLoadingView = mRootLayout.findViewById(R.id.loading);
        mMiddleStart = mRootLayout.findViewById(R.id.start);
        mIvRepeat = mRootLayout.findViewById(R.id.iv_repeat);
        mVideoView = mRootLayout.findViewById(R.id.video_view);
        mVideoThumb = mRootLayout.findViewById(R.id.thumb);
        mHudView = mRootLayout.findViewById(R.id.hud_view);
    }

    private void initVideoView() {
        if (mVideoView == null) return;
        //TODO 正式环境不能展示
        mVideoView.setHudView(mHudView);//加载播放数据信息
        mVideoView.setOnPreparedListener(this);
        mVideoView.setOnInfoListener(this);
        if (isRoundAnglePlayer)
            changeTextureViewShowType();
    }

    private void initSeekBar() {
        if (mBottomSeekBar == null) return;
        mBottomSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mMoveSeekBar = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mMoveSeekBar = false;
                if (mVideoView == null) return;
                int seekPosition = (seekBar.getProgress() > mVideoView.getDuration()) ? mVideoView.getDuration() : seekBar.getProgress();
                mVideoView.seekTo(seekPosition);
            }
        });
    }

    private void addOnClickToView() {
        if (mVideoThumb != null)
            mVideoThumb.setOnClickListener(this);
        if (mVideoView != null)
            mVideoView.setOnClickListener(this);
        if (mBottomStart != null)
            mBottomStart.setOnClickListener(this);
        if (mFullscreen != null)
            mFullscreen.setOnClickListener(this);
        if (mMiddleStart != null)
            mMiddleStart.setOnClickListener(this);
        if (mIvRepeat != null)
            mIvRepeat.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.thumb://封面图
                break;
            case R.id.video_view://播放器
                if (mBottomContralView != null)
                    mBottomContralView.setVisibility(mBottomContralView.getVisibility() == VISIBLE ? GONE : VISIBLE);
                break;
            case R.id.layout_bottom_start://开始按钮
                toChangeVideoPlayerState();
                break;
            case R.id.fullscreen://全屏按钮
                if (isFullScreen) toNormalScreen();
                else toFullScreen();
                break;
            case R.id.start://中间开始按钮
                break;
            case R.id.iv_repeat://重复播放按钮
                break;
        }
    }

    private void toChangeVideoPlayerState() {
        if (mVideoView == null) return;
        if (mVideoView.isPlaying()) pause();
        else start();
    }

    /**
     * 更新播放时间
     *
     * @param duration
     * @param currentPosition
     */
    private void updataProgressAndTime(int duration, int currentPosition) {
        if (mVideoView == null) return;
        if (mTvTotalTime != null)
            mTvTotalTime.setText(TimeUtil.getTimeString(duration / 1000));
        if (mTvCurrentTime != null)
            mTvCurrentTime.setText(TimeUtil.getTimeString(currentPosition / 1000));

        if (mBottomSeekBar != null && !mMoveSeekBar) {
            mBottomSeekBar.setMax(duration);
            mBottomSeekBar.setProgress(currentPosition);
        }
        if (mBottomProgressBar != null && !mMoveSeekBar) {
            mBottomProgressBar.setMax(duration);
            mBottomProgressBar.setProgress(currentPosition);
        }
    }

    /**
     * Get activity from context object
     *
     * @param context something
     * @return object of Activity or null if it is not Activity
     */
    public static Activity scanForActivity(Context context) {
        if (context == null) return null;

        if (context instanceof Activity) {
            return (Activity) context;
        } else if (context instanceof TintContextWrapper) {
            return scanForActivity(((TintContextWrapper) context).getBaseContext());
        } else if (context instanceof ContextWrapper) {
            return scanForActivity(((ContextWrapper) context).getBaseContext());
        }

        return null;
    }

    protected void toFullScreen() {
        orientationUtils.resolveByClick();
        mNormalLayoutParams = getLayoutParams();
        mNormalParentView = (ViewGroup) getParent();
        //1.将当前的播放View从原先布局移除
        mNormalParentView.removeView(this);
        //2.修改播放布局的宽高比
        mVideoView.getLayoutParams().width = LayoutParams.MATCH_PARENT;
        mVideoView.getLayoutParams().height = LayoutParams.MATCH_PARENT;
        if (isRoundAnglePlayer)
            mVideoView.setClipToOutline(false);

        //3.将播放视频布局 添加到新的布局里面
        if (mFullParentView == null) {
            mFullParentView = new FrameLayout(getContext());
            mFullParentView.setBackgroundColor(Color.BLACK);
        }
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        lp.gravity = Gravity.CENTER;
        mFullParentView.addView(this, lp);
        //4.将全屏时 播放布局添加到根布局中
        ViewGroup vp = (scanForActivity(getContext())).findViewById(Window.ID_ANDROID_CONTENT);
        LayoutParams lpParent = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        vp.addView(mFullParentView, lpParent);
        isFullScreen = true;
    }

    protected void toNormalScreen() {
        orientationUtils.resolveByClick();
        //1.将当前的播放View从原先布局移除
        mFullParentView.removeView(this);
        //2.移除全屏布局
        ViewGroup vp = (scanForActivity(getContext())).findViewById(Window.ID_ANDROID_CONTENT);
        vp.removeView(mFullParentView);
        //3.开启圆角模式
        if (isRoundAnglePlayer)
            mVideoView.setClipToOutline(true);
        //4.将播放视频布局 添加到正常播放的（非全屏） 布局里面
        setLayoutParams(mNormalLayoutParams);//将播放器本身的宽高重新还原
        mNormalParentView.addView(this);
        mNormalParentView.requestLayout();
        isFullScreen = false;
    }

    protected void changeTextureViewShowType() {
        if (mVideoView == null) return;
        ViewGroup.LayoutParams layoutParams = mVideoView.getLayoutParams();
        if (layoutParams.width <= 0 || layoutParams.height <= 0) {
            layoutParams.width = (int) ((ScreenUtil.getScreenWidthSize(getContext()) - ((ScreenUtil.getInstance(getContext()).getHorizontalScale()) * 80)));
            layoutParams.height = layoutParams.width * 9 / 16;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (mVideoView != null) {
                mVideoView.setOutlineProvider(new TextureVideoViewOutlineProvider(
                        ScreenUtil.dp2px(getContext(), 10), layoutParams.height, layoutParams.width));
                mVideoView.setClipToOutline(true);
            }
        }
    }

    //===========================IjkVideoView start=================================
    @Override
    public void onPrepared(IMediaPlayer mp) {
        if (mp == null) return;
        updataProgressAndTime((int) mp.getDuration(), (int) mp.getCurrentPosition());
    }

    @Override
    public boolean onInfo(IMediaPlayer mp, int what, int extra) {
        switch (what) {
            case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
//                Log.d(TAG, "MEDIA_INFO_BUFFERING_START:");
                if (mLoadingView != null)
                    mLoadingView.setVisibility(VISIBLE);
                break;
            case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
//                Log.d(TAG, "MEDIA_INFO_BUFFERING_END:");
                if (mLoadingView != null)
                    mLoadingView.setVisibility(GONE);
                break;
        }
        return false;
    }

    //===========================IjkVideoView end=================================
    private class ProgressTimerTask extends TimerTask {
        @Override
        public void run() {
            if (mVideoView == null) return;
            if (mVideoView.isPlaying()) {
                new Handler(Looper.getMainLooper()).post(
                        new Runnable() {
                            @Override
                            public void run() {
                                updataProgressAndTime(mVideoView.getDuration(), mVideoView.getCurrentPosition());
                            }
                        }
                );
            }
        }
    }

    protected void startProgressTimer() {
        cancelProgressTimer();
        mUpdateProcessTimer = new Timer();
        mProgressTimerTask = new ProgressTimerTask();
        mUpdateProcessTimer.schedule(mProgressTimerTask, 0, 300);
    }

    protected void cancelProgressTimer() {
        if (mUpdateProcessTimer != null) {
            mUpdateProcessTimer.cancel();
            mUpdateProcessTimer = null;
        }
        if (mProgressTimerTask != null) {
            mProgressTimerTask.cancel();
            mProgressTimerTask = null;
        }
    }

    public void setVideoPath(String videoPath) {
        if (TextUtils.isEmpty(videoPath)) return;
        this.mVideoPath = videoPath;
        if (mVideoView != null)
            mVideoView.setVideoPath(mVideoPath);
    }

    /**
     * 是否可以时圆角播放器
     */
    public void setRoundAnglePlayer(boolean isRoundAnglePlayer) {
        this.isRoundAnglePlayer = isRoundAnglePlayer;
    }

    public void start() {
        if (mVideoView == null) return;
        mVideoView.start();
        startProgressTimer();
        if (mBottomStart != null)
            mBottomStart.setImageResource(R.drawable.ic_pause_circle);
    }

    public void pause() {
        if (mVideoView == null) return;
        mVideoView.pause();
        if (mBottomStart != null)
            mBottomStart.setImageResource(R.drawable.ic_play_circle);
    }

    public void release() {
        cancelProgressTimer();
        if (mVideoView == null) return;
        mVideoView.stopPlayback();
        mVideoView.release(true);
        mVideoView.stopBackgroundPlay();
    }
}

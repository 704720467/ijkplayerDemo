package cn.reee.reeeplayer.view.dialog;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

/**
 * 延迟加载 管理类
 */

public class DelayLoadingDialogManager implements Runnable {

    private LoadingDialog2 loadingDialog;

    private long loadDelayTime = 400l;
    private Handler handler;
    private Context context;
    private boolean cancelable = true;

    public DelayLoadingDialogManager(Context context) {
        this(context, -1);
    }

    public DelayLoadingDialogManager(Context context, long loadDelayTime) {
        this(context, loadDelayTime, null);
    }

    public DelayLoadingDialogManager(Context context, Handler handler) {
        this(context, -1, handler);
    }

    public DelayLoadingDialogManager(Context context, long loadDelayTime, Handler handler) {
        this.context = context;
        if (loadDelayTime != -1) {
            this.loadDelayTime = loadDelayTime;
        }
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper());
        }
        this.handler = handler;
    }

    @Override
    public void run() {
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            if (loadingDialog == null) {
                loadingDialog = new LoadingDialog2(activity);
                loadingDialog.setCancelable(cancelable);
            }

            if (!activity.isFinishing()) {
                loadingDialog.show();
            }
        }
    }

    public void setLoadDelayTime(long loadDelayTime) {
        this.loadDelayTime = loadDelayTime;
    }

    public void setCancelable(boolean flag) {
        cancelable = flag;
    }

    /**
     * 默认为有延时的loading
     */
    public void showLoading() {
        showLoading(false);
    }

    public boolean isLoadingShowing() {
        return loadingDialog != null && loadingDialog.isShowing();
    }

    /**
     * 设置是否立即显示loading
     *
     * @param isImmediately true 表示立即显示 false 表示延时显示
     */
    public void showLoading(boolean isImmediately) {
        long loadDelayTime = isImmediately ? 0 : this.loadDelayTime;
        handler.removeCallbacks(this);
        handler.postDelayed(this, loadDelayTime);
    }

    /**
     * 隐藏加载进度条
     */
    public void hideLoading() {
        handler.removeCallbacks(this);
        if (loadingDialog != null) {
            loadingDialog.dismiss();
        }
    }

    public Context getContext() {
        return context;
    }

    public void onDestroy() {
        handler.removeCallbacks(this);
        context = null;
    }
}

package cn.reee.reeeplayer.view.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import cn.reee.reeeplayer.R;
import cn.reee.reeeplayer.view.VniView2;


/**
 * Created by zp on 2018/2/6.
 */

public class LoadingDialog2 {

    private Dialog mDialog;
    private VniView2 vniView;

    public LoadingDialog2(Activity context) {
        init(context);
    }

    private void init(Activity context) {
        mDialog = new Dialog(context, R.style.transparentFrameWindowStyle);
        View view = context.getLayoutInflater().inflate(R.layout.dialog_loading2, null);
        vniView = view.findViewById(R.id.loadingView);
        mDialog.setContentView(view, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        Window window = mDialog.getWindow();
        WindowManager.LayoutParams wl = window.getAttributes();
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        wl.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        wl.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        mDialog.onWindowAttributesChanged(wl);
        mDialog.setCanceledOnTouchOutside(true);
        mDialog.setCancelable(true);
        mDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                vniView.clear();
            }
        });
    }

    public void setCancelable(boolean flag) {
        mDialog.setCanceledOnTouchOutside(flag);
        mDialog.setCancelable(flag);
    }


    public void show() {
        try {
            if (mDialog != null && !mDialog.isShowing()) {
                mDialog.show();
                vniView.show();
            }
        } catch (Exception e) {
            Log.e("LoadingDialog2", e.getMessage(), e);
        }

    }

    public boolean isShowing() {
        return mDialog != null && mDialog.isShowing();
    }

    public void dismiss() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }
}

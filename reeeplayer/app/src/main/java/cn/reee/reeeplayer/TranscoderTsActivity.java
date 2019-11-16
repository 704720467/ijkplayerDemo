package cn.reee.reeeplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;
import com.zp.libvideoedit.Transcoder.Transcoder;
import com.zp.libvideoedit.utils.FileUtils;
import com.zp.libvideoedit.utils.MediaUtils;
import com.zp.libvideoedit.utils.ToastUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.reee.reeeplayer.view.dialog.DelayLoadingDialogManager;

import static com.zp.libvideoedit.Constants.VERBOSE;

public class TranscoderTsActivity extends AppCompatActivity {
    private static final String TAG = "TranscoderTsActivity";
    String tempStr = "_temp_";
    public DelayLoadingDialogManager delayLoadingDialogManager;
    private static ExecutorService threadPool = Executors.newFixedThreadPool(3);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transcoder_ts);

        delayLoadingDialogManager = new DelayLoadingDialogManager(this);
        delayLoadingDialogManager.setCancelable(false);
        MediaUtils.getInstance(this);

        AndPermission.with(this)
                .runtime()
                .permission(Permission.READ_EXTERNAL_STORAGE, Permission.WRITE_EXTERNAL_STORAGE)
                .start();
        findViewById(R.id.bt_trancode).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                threadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        delayLoadingDialogManager.showLoading();
                        //                String videoPath = "sdcard/test.ts";
                        String videoPath = "sdcard/长春.ts";
                        //                        String videoPath = "sdcard/test1.mp4";
                        String outPutFilePath = "sdcard/test.mp4";
                        transCode(videoPath, outPutFilePath, 1);
                    }
                });

            }
        });
    }

    /**
     * 视频转码
     */
    private void transCode(String videoPath, String outPutFilePath, final int index) {

        final Transcoder transCoder = new Transcoder(this);
        transCoder.setForceAllKeyFrame(true);
        transCoder.setInPutFilePath(videoPath);
        //临时文件名
        String newOutPutFilePath = outPutFilePath.replace(".mp4", tempStr + ".mp4");
        transCoder.setOutPutFilePath(newOutPutFilePath);


        transCoder.setCallback(new Transcoder.Callback() {
            @Override
            public void onThumbGenerated(Transcoder transCoder, Bitmap thumb, int index, long pts) {
                Log.i(TAG, "onThumbGenerated:" + index + "\t" + pts + "\t" + thumb.getWidth() + "x" + thumb.getHeight());
//                String filename = thumbPath + "/" + index + ".png";
//                BufferedOutputStream bos = null;
//                try {
//                    bos = new BufferedOutputStream(new FileOutputStream(filename));
//                    thumb.compress(Bitmap.CompressFormat.PNG, 90, bos);
//                    thumb.recycle();
//                    bos.close();
//                } catch (Exception e2) {
//                    e2.printStackTrace();
//                }

            }

            @Override
            public void onProgress(Transcoder transCoder, float percent) {
                if (VERBOSE) Log.i(TAG, "percent:" + percent);
//                if (index == maxIndex) {
//                    dialogProgressView.setProgress(percent);
//                }
            }

            @Override
            public void OnSuccessed(Transcoder transCoder, final String outPutFilePath) {
                TranscoderTsActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String filename = outPutFilePath.substring(outPutFilePath.lastIndexOf("/") + 1);
                        FileUtils.reName(outPutFilePath, filename.replace(tempStr, ""));
//                        transCodeSuccessed();
                    }
                });

            }

            @Override
            public void onError(Transcoder transCoder, final String errmsg) {
                Log.i(TAG, "onError:" + errmsg);
                TranscoderTsActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        delayLoadingDialogManager.hideLoading();
                        ToastUtil.showToast(TranscoderTsActivity.this, errmsg);
                    }
                });
            }
        });
        delayLoadingDialogManager.showLoading();
        transCoder.transCode();
    }
}

package cn.reee.reeeplayer;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;
import com.zp.libvideoedit.Transcoder.TranscodeManager;
import com.zp.libvideoedit.Transcoder.TranscodeManagerCallback;
import com.zp.libvideoedit.Transcoder.Transcoder;
import com.zp.libvideoedit.utils.FileUtils;
import com.zp.libvideoedit.utils.MediaUtils;
import com.zp.libvideoedit.utils.ToastUtil;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.reee.reeeplayer.view.dialog.DelayLoadingDialogManager;

import static com.zp.libvideoedit.EditConstants.VERBOSE;

public class TranscoderTsActivity extends AppCompatActivity {
    private static final String TAG = "TranscoderTsActivity";
    String tempStr = "_temp_";
    public DelayLoadingDialogManager delayLoadingDialogManager;
    private static ExecutorService threadPool = Executors.newFixedThreadPool(3);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transcoder_ts);

//        delayLoadingDialogManager = new DelayLoadingDialogManager(this);
//        delayLoadingDialogManager.setCancelable(false);
        MediaUtils.getInstance(this);

        AndPermission.with(this)
                .runtime()
                .permission(Permission.READ_EXTERNAL_STORAGE, Permission.WRITE_EXTERNAL_STORAGE)
                .start();
        findViewById(R.id.bt_trancode).setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View view) {
//                threadPool.execute(new Runnable() {
//                    @Override
//                    public void run() {
////                        delayLoadingDialogManager.showLoading();
//                        //                String videoPath = "sdcard/test.ts";
//                        String videoPath = "sdcard/TestTs/长春.ts";
//                        //                        String videoPath = "sdcard/test1.mp4";
//                        String outPutFilePath = "sdcard/TestTs/test.mp4";
////                        transCode(videoPath, outPutFilePath, 1);
//                        transCode3();//多个文件合成一个全关键帧 文件
////                        transCode2(videoPath, outPutFilePath, 1);
//                    }
//                });
                transString();

            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void transString() {
//        byte[] bytes = "hello world".getBytes();
//        byte[] bytes1 = {0x67, 0x64, 0, 0x1f, 0xac, 0x2c, 0x6a, 0x81, 0x40, 0x16, 0xe9, 0xb8, 0x8, 0x8, 0x8, 0x10};
//        byte[] bytes =  {103,   100, 0, 31,    172,   44,  106,  129,   64,   22,  233,  184, 8, 8, 8, 16};
//        String encoded = Base64.getEncoder().encodeToString(bytes); //Base64 Encoded
//        byte[] decoded = Base64.getDecoder().decode(encoded);   //Base64 Decoded
//        Log.e("=======>", "===>转码后：" + new String(decoded));
    }

    /**
     * 视频转码 单独合成一个
     */
    private void transCode(String videoPath, String outPutFilePath, final int index) {

        //临时文件名
        String newOutPutFilePath = outPutFilePath.replace(".mp4", tempStr + ".mp4");
        TranscodeManager transCoder = new TranscodeManager(this, videoPath, newOutPutFilePath);
        transCoder.setForceAllKeyFrame(true);

        transCoder.setCallback(new TranscodeManagerCallback() {
            @Override
            public void onThumbGenerated(Bitmap thumb, int index, long pts) {
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
            public void onProgress(float percent) {
                if (VERBOSE) Log.i(TAG, "percent:" + percent);
//                if (index == maxIndex) {
//                    dialogProgressView.setProgress(percent);
//                }
            }

            @Override
            public void OnSuccessed(final String outPutFilePath) {
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
            public void onError(final String errmsg) {
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
//        delayLoadingDialogManager.showLoading();
        transCoder.transCode();
    }

    /**
     * 视频转码 多个文件合成一个
     */
    private void transCode3() {
        ArrayList<String> tsFiles = new ArrayList<>();
        tsFiles.add("sdcard/TestTs/长春1.ts");
        tsFiles.add("sdcard/TestTs/长春2.ts");
        String outPutFilePath = "sdcard/TestTs/长春test.mp4";
        //临时文件名
        String newOutPutFilePath = outPutFilePath.replace(".mp4", tempStr + ".mp4");
        TranscodeManager transCoder = new TranscodeManager(this, tsFiles, newOutPutFilePath);
        transCoder.setForceAllKeyFrame(true);

        transCoder.setCallback(new TranscodeManagerCallback() {
            @Override
            public void onThumbGenerated(Bitmap thumb, int index, long pts) {
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
            public void onProgress(float percent) {
                if (VERBOSE) Log.i(TAG, "percent:" + percent);
//                if (index == maxIndex) {
//                    dialogProgressView.setProgress(percent);
//                }
            }

            @Override
            public void OnSuccessed(final String outPutFilePath) {
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
            public void onError(final String errmsg) {
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
//        delayLoadingDialogManager.showLoading();
        transCoder.transCodes();
    }

    /**
     * 视频转码 旧的转码 单个文件合成单个
     */
    private void transCode2(String videoPath, String outPutFilePath, final int index) {

        //临时文件名
        String newOutPutFilePath = outPutFilePath.replace(".mp4", tempStr + ".mp4");
        Transcoder transCoder = new Transcoder(this);
        transCoder.setInPutFilePath(videoPath);
        transCoder.setOutPutFilePath(newOutPutFilePath);
        transCoder.setForceAllKeyFrame(true);
//
//        transCoder.setCallback(new Transcoder.Callback() {
//            @Override
//            public void onThumbGenerated(Bitmap thumb, int index, long pts) {
//                Log.i(TAG, "onThumbGenerated:" + index + "\t" + pts + "\t" + thumb.getWidth() + "x" + thumb.getHeight());
////                String filename = thumbPath + "/" + index + ".png";
////                BufferedOutputStream bos = null;
////                try {
////                    bos = new BufferedOutputStream(new FileOutputStream(filename));
////                    thumb.compress(Bitmap.CompressFormat.PNG, 90, bos);
////                    thumb.recycle();
////                    bos.close();
////                } catch (Exception e2) {
////                    e2.printStackTrace();
////                }
//
//            }
//
//            @Override
//            public void onProgress(float percent) {
//                if (VERBOSE) Log.i(TAG, "percent:" + percent);
////                if (index == maxIndex) {
////                    dialogProgressView.setProgress(percent);
////                }
//            }
//
//            @Override
//            public void OnSuccessed(final String outPutFilePath) {
//                TranscoderTsActivity.this.runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
////                        String filename = outPutFilePath.substring(outPutFilePath.lastIndexOf("/") + 1);
////                        FileUtils.reName(outPutFilePath, filename.replace(tempStr, ""));
////                        transCodeSuccessed();
//                    }
//                });
//
//            }
//
//            @Override
//            public void onError(final String errmsg) {
//                Log.i(TAG, "onError:" + errmsg);
//                TranscoderTsActivity.this.runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        delayLoadingDialogManager.hideLoading();
//                        ToastUtil.showToast(TranscoderTsActivity.this, errmsg);
//                    }
//                });
//            }
//        });
//        delayLoadingDialogManager.showLoading();
//        transCoder.transCode();
    }
}

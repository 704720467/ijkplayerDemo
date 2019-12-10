package cn.reee.reeeplayer.util;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class AssetsCopyTOSDcard {

    Context context;

    public AssetsCopyTOSDcard(Context context) {
        super();
        this.context = context;
    }


    public void assetToSD(String assetpath, String filePath) {

        InputStream inputStream;
        try {
            inputStream = context.getResources().getAssets().open(assetpath);
            File file = new File(filePath.substring(0, filePath.lastIndexOf("/")));
            if (!file.exists()) {
                file.mkdirs();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(filePath);
            byte[] buffer = new byte[1024];
            int count = 0;
            while ((count = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, count);
            }
            fileOutputStream.flush();
            fileOutputStream.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //分级建立文件夹
    public void getDirectory(String path) {
        //对SDpath进行处理，分层级建立文件夹
        String[] s = path.split("/");
        String str = Environment.getExternalStorageDirectory().toString();
        for (int i = 0; i < s.length; i++) {
            str = str + "/" + s[i];
            File file = new File(str);
            if (!file.exists()) {
                file.mkdir();
            }
        }

    }
}

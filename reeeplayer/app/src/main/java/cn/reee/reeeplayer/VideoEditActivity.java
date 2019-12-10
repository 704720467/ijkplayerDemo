package cn.reee.reeeplayer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.TextView;

import com.zp.libvideoedit.utils.FileUtils;

import java.util.ArrayList;

import cn.reee.reeeplayer.adapter.ZpFileAdapter;
import cn.reee.reeeplayer.base.BaseListAdapter;
import cn.reee.reeeplayer.modle.ZpFileInfo;
import cn.reee.reeeplayer.util.FileToolUtils;

/**
 * 视频编辑
 */
public class VideoEditActivity extends AppCompatActivity {

    private String videoPath;
    private TextView mTvPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_videio_edit);
        videoPath = getIntent().getExtras().getString("path");
        mTvPath = findViewById(R.id.path);
        mTvPath.setText(videoPath);
    }

}

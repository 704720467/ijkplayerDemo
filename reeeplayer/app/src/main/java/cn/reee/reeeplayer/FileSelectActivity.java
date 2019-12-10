package cn.reee.reeeplayer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import cn.reee.reeeplayer.adapter.ZpFileAdapter;
import cn.reee.reeeplayer.base.BaseActivity;
import cn.reee.reeeplayer.base.BaseListAdapter;
import cn.reee.reeeplayer.modle.ZpFileInfo;
import cn.reee.reeeplayer.util.FileToolUtils;

/**
 * 文件筛选
 */
public class FileSelectActivity extends BaseActivity {
    private RecyclerView mRvVideoFiles;
    private ZpFileAdapter mVideoFileAdapter;
    private final int LoadDataBack = 1;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case LoadDataBack:
                    ArrayList<ZpFileInfo> fileInfoArrayList = (ArrayList<ZpFileInfo>) msg.obj;
                    mVideoFileAdapter.setNewDatas(fileInfoArrayList);
                    return;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_select);
        mRvVideoFiles = findViewById(R.id.rv_video_files);
        initRecyclerView();
        loadVideoList();
    }


    private void initRecyclerView() {
        mRvVideoFiles.setLayoutManager(new GridLayoutManager(this, 4));
        mVideoFileAdapter = new ZpFileAdapter(this);
        mRvVideoFiles.setAdapter(mVideoFileAdapter);
        mVideoFileAdapter.setOnItemClickListener(new BaseListAdapter.OnItemClickListener() {
            @Override
            public void onClick(int position, View view, Object itemData) {
                Intent intent = new Intent(FileSelectActivity.this, VideoEditActivity.class);
                intent.putExtra("path", mVideoFileAdapter.getDataByPosition(position).getFilePath());
                startActivity(intent);
            }
        });
    }


    private void loadVideoList() {
        ArrayList<ZpFileInfo> fileInfoArrayList = FileToolUtils.getAllVideo(this);
        Message msg = new Message();
        msg.obj = fileInfoArrayList;
        msg.what = LoadDataBack;
        mHandler.sendMessage(msg);
    }
}

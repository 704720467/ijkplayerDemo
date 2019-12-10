package cn.reee.reeeplayer.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.view.View;

import java.util.ArrayList;
import java.util.List;

import cn.reee.reeeplayer.R;
import cn.reee.reeeplayer.base.BaseListAdapter;
import cn.reee.reeeplayer.modle.ZpFileInfo;
import cn.reee.reeeplayer.modle.listener.FileSelectListener;
import cn.reee.reeeplayer.util.ImageUtil;
import cn.reee.reeeplayer.util.ScreenUtil;
import cn.reee.reeeplayer.util.TimeUtil;

/**
 * Create by zp on 2019-12-10
 */
public class ZpFileAdapter extends BaseListAdapter<ZpFileAdapter.VideoFileAdapterHolder, ZpFileInfo> {
    private ArrayList<Integer> selectPositions;
    private FileSelectListener mFileSelectListener;

    public ZpFileAdapter(Context context) {
        super(context);
    }

    @NonNull
    @Override
    public VideoFileAdapterHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_file, parent, false);
        VideoFileAdapterHolder viewHolder = new VideoFileAdapterHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull VideoFileAdapterHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        holder.bind(position);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoFileAdapterHolder holder, int position, @NonNull List<Object> payloads) {
        super.onBindViewHolder(holder, position, payloads);
    }

    /**
     * 设置选中
     *
     * @param position
     */
    public void selectPosition(int position) {
        if (selectPositions == null) selectPositions = new ArrayList<>();
        if (selectPositions.contains(position)) {
            removeSelect(position);
        } else {
            addSelect(position);
        }
    }

    /**
     * 添加 未添加的
     *
     * @param position
     */
    public void addSelect(int position) {
        if (selectPositions.contains(position)) return;
        selectPositions.add(position);
        if (mFileSelectListener != null)
            mFileSelectListener.selectFile(getDataByPosition(position));
        notifyItemChanged(position, "changeState");

    }

    /**
     * 移除 已经添加的
     *
     * @param position
     */
    public void removeSelect(int position) {
        if (!selectPositions.contains(position)) return;
        selectPositions.remove((Integer) position);
        if (mFileSelectListener != null)
            mFileSelectListener.removeFile(getDataByPosition(position));
        notifyItemChanged(position, "changeState");
        refreshSelect();
    }

    /**
     * 刷新已经选择
     */
    public void refreshSelect() {
        if (selectPositions == null && selectPositions.isEmpty()) return;
        for (Integer position : selectPositions) {
            notifyItemChanged(position, "changeState");
        }
    }

    public void setmFileSelectListener(FileSelectListener mFileSelectListener) {
        this.mFileSelectListener = mFileSelectListener;
    }

    class VideoFileAdapterHolder extends RecyclerView.ViewHolder {
        private RelativeLayout mRootLayout;
        private ImageView mFileThumb;//文件缩略图
        private TextView mTvDuration;
        private TextView mTvNum;
        private int currentPosition;

        public VideoFileAdapterHolder(@NonNull View itemView) {
            super(itemView);
            mRootLayout = itemView.findViewById(R.id.root_layout);
            mFileThumb = itemView.findViewById(R.id.iv_thumb);
            mTvDuration = itemView.findViewById(R.id.tv_duration);
            mTvNum = itemView.findViewById(R.id.tv_num);
            int screenWidth = ScreenUtil.getScreenWidth(context);
            mRootLayout.getLayoutParams().width = screenWidth / 4;
            mRootLayout.getLayoutParams().height = screenWidth / 4;
            mTvNum.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    selectPosition(currentPosition);
                }
            });
        }

        public void bind(int position) {
            this.currentPosition = position;
            ZpFileInfo klFileInfo = getDataByPosition(position);
            ImageUtil.loadmAdapterContextImage(klFileInfo.getFilePath(), mFileThumb, true, false);
            if (klFileInfo.getFileType() == ZpFileInfo.FILE_TYPE_PICTURE) {
                mTvDuration.setText("");
            } else {
                mTvDuration.setText(TimeUtil.formattedTime(klFileInfo.getDuration() / 1000));
            }
            boolean selected = selectPositions != null && selectPositions.contains(position);
            mTvNum.setBackgroundResource(selected ? R.drawable.bg_round_layout_f62b43 : R.drawable.bg_round_layout_cc000000);
            mTvNum.setText(selected ? String.valueOf(selectPositions.indexOf(position) + 1) : "");
        }
    }
}

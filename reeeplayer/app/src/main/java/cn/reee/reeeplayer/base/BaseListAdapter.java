package cn.reee.reeeplayer.base;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * 列表适配器基类
 * Create by zp on 2019-07-18
 */
public abstract class BaseListAdapter<T extends RecyclerView.ViewHolder, M> extends RecyclerView.Adapter<T> {
    protected List<M> mDatas;
    protected LayoutInflater mInflater;
    protected Context context;
    private OnItemClickListener listener;
    private OnItemLongClickListener longClickListener;

    public BaseListAdapter(Context context) {
        this.context = context;
        mDatas = new ArrayList<>();
        mInflater = LayoutInflater.from(context);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
    }

    @Override
    public int getItemCount() {
        int count = (mDatas == null || mDatas.isEmpty()) ? 0 : mDatas.size();
        return count;
    }

    @Override
    public void onBindViewHolder(@NonNull T holder, final int position) {
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null)
                    listener.onClick(position, view, getDataByPosition(position));
            }
        });
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (longClickListener != null)
                    longClickListener.onLongClick(position, view, getDataByPosition(position));
                return true;
            }
        });
    }

    public void setNewDatas(List<M> mDatas) {
        if (mDatas == null) return;
        checkDatas();
        this.mDatas.clear();
        this.mDatas.addAll(mDatas);
        notifyDataSetChanged();
    }

    public void addDatas(List<M> mDatas) {
        if (mDatas == null) return;
        checkDatas();
        this.mDatas.addAll(mDatas);
        notifyDataSetChanged();
    }

    /**
     * 添加数据
     *
     * @param mData
     */
    public void addDatas(M mData) {
        if (mData == null) return;
        checkDatas();
        this.mDatas.add(mData);
        notifyDataSetChanged();
    }

    /**
     * 指定位置
     *
     * @param mData
     * @param postion
     */
    public void addDatas(M mData, int postion) {
        if (mData == null) return;
        checkDatas();
        if (postion != 0 && postion >= mDatas.size()) return;
        this.mDatas.add(postion, mData);
        notifyDataSetChanged();
    }

    public List<M> getDatas() {
        if (mDatas == null) mDatas = new ArrayList<>();
        return mDatas;
    }


    public M getDataByPosition(int position) {
        if (mDatas.size() <= position || mDatas.isEmpty()) return null;
        return mDatas.get(position);
    }

    private void checkDatas() {
        if (mDatas == null)
            mDatas = new ArrayList<>();
    }

    public Context getContext() {
        return context;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public interface OnItemClickListener {
        void onClick(int position, View view, Object itemData);
    }

    public interface OnItemLongClickListener {
        void onLongClick(int position, View view, Object itemData);
    }
}

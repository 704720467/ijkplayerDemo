package cn.reee.reeeplayer.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cn.reee.reeeplayer.R;
import cn.reee.reeeplayer.base.BaseListAdapter;
import cn.reee.reeeplayer.modle.StadiumDataAdapterModle;

/**
 * Create by zp on 2019-11-22
 */
public class StadiumDataAdapter extends BaseListAdapter<StadiumDataAdapter.StadiumDataAdapterHolder, StadiumDataAdapterModle> {
    private int selectPosition = 0;
    private int oldSelectPosition = 0;

    public StadiumDataAdapter(Context context) {
        super(context);
    }

    @NonNull
    @Override
    public StadiumDataAdapterHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_stadium_date, parent, false);
        StadiumDataAdapterHolder viewHolder = new StadiumDataAdapterHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull StadiumDataAdapterHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        holder.bind(position);
    }

    @Override
    public void onBindViewHolder(@NonNull StadiumDataAdapterHolder holder, int position, @NonNull List<Object> payloads) {
        super.onBindViewHolder(holder, position, payloads);
        if (payloads == null || payloads.isEmpty()) return;
        holder.bind(position);
    }

    public void selectPosition(int position) {
        this.selectPosition = position;
        notifyItemChanged(position, "changeSelectPosition");
        notifyItemChanged(oldSelectPosition, "changeSelectPosition");
        this.oldSelectPosition = position;
    }

    class StadiumDataAdapterHolder extends RecyclerView.ViewHolder {
        private View mLeftMarginView;
        private View mRightMarginView;
        private TextView mTvDate;
        private TextView mTvWeek;
        private LinearLayout mContentLayout;


        public StadiumDataAdapterHolder(@NonNull View itemView) {
            super(itemView);
            mLeftMarginView = itemView.findViewById(R.id.left_margin_view);
            mRightMarginView = itemView.findViewById(R.id.right_margin_view);
            mTvDate = itemView.findViewById(R.id.tv_date);
            mTvWeek = itemView.findViewById(R.id.tv_week);
            mContentLayout = itemView.findViewById(R.id.content_layout);
        }

        public void bind(int position) {
            mLeftMarginView.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
            mRightMarginView.setVisibility(position == getDatas().size() - 1 ? View.VISIBLE : View.GONE);
            mTvDate.setTextColor(Color.parseColor(selectPosition == position ? "#ee5514" : "#ffffff"));
            mTvWeek.setTextColor(Color.parseColor(selectPosition == position ? "#ee5514" : "#ffffff"));
            mTvDate.setText(getDataByPosition(position).getDate());
            mTvWeek.setText(getDataByPosition(position).getWeek());
            mContentLayout.setBackgroundResource(selectPosition == position ? R.drawable.bg_stadium_date_select : R.drawable.bg_stadium_date);
        }
    }
}

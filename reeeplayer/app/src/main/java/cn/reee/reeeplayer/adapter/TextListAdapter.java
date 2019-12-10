package cn.reee.reeeplayer.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import cn.reee.reeeplayer.R;
import cn.reee.reeeplayer.base.BaseListAdapter;

/**
 * Create by zp on 2019-12-10
 */
public class TextListAdapter extends BaseListAdapter<TextListAdapter.TextListAdapterHolder, String> {

    public TextListAdapter(Context context) {
        super(context);
    }

    @NonNull
    @Override
    public TextListAdapterHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_text, parent, false);
        TextListAdapterHolder viewHolder = new TextListAdapterHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull TextListAdapterHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        holder.tvCost.setText(getDataByPosition(position));
    }

    class TextListAdapterHolder extends RecyclerView.ViewHolder {
        private TextView tvCost;

        public TextListAdapterHolder(@NonNull View itemView) {
            super(itemView);
            tvCost = itemView.findViewById(R.id.tv_cost);
        }
    }
}

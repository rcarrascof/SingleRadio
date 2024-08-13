package com.app.matrixFM.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.matrixFM.R;
import com.app.matrixFM.models.Social;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;

public class AdapterSocial extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    Context context;
    private ArrayList<Social> items;
    private OnItemClickListener mOnItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(View view, Social obj, int position);
    }

    public void setOnItemClickListener(final OnItemClickListener mItemClickListener) {
        this.mOnItemClickListener = mItemClickListener;
    }

    public AdapterSocial(Context context, ArrayList<Social> items) {
        this.items = items;
        this.context = context;
    }

    public static class OriginalViewHolder extends RecyclerView.ViewHolder {

        TextView txtName;
        ImageView imgIcon;
        LinearLayout lytParent;

        public OriginalViewHolder(View view) {
            super(view);
            lytParent = view.findViewById(R.id.lyt_parent);
            txtName = view.findViewById(R.id.txt_name);
            imgIcon = view.findViewById(R.id.img_icon);
        }

    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder vh;
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_social, parent, false);
        vh = new OriginalViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof OriginalViewHolder) {
            final Social social = items.get(position);
            final OriginalViewHolder vItem = (OriginalViewHolder) holder;

            vItem.txtName.setText(social.social_name);

            Glide.with(context)
                    .load(social.social_icon.replace(" ", "%20"))
                    .placeholder(R.drawable.ic_thumbnail)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(vItem.imgIcon);

            vItem.lytParent.setOnClickListener(view -> {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClick(view, social, position);
                }
            });

        }
    }

    public void setItems(ArrayList<Social> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public void resetListData() {
        this.items.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

}
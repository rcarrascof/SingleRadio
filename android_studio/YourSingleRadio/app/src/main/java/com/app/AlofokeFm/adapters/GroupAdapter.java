package com.app.AlofokeFm.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.app.AlofokeFm.R;
import com.app.AlofokeFm.models.Group;

import java.util.List;

/**
 * Created by Reinold Carrasco
 * on 2022-03-16
 */
public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.ViewHolder> {

    private List<Group> mData;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    // data is passed into the constructor
    public GroupAdapter(Context context, List<Group> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
    }

    // inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.grouprow, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Group group = mData.get(position);
        holder.groupName.setText(group.getGroupName());
        holder.groupLastMessage.setText(group.getLastMessage().getText());
        holder.groupLastMessageTimestamp.setText(" - " + group.getLastMessage().getTimestamp());
        holder.groupIcnText.setText(MessageAdapter.getUserNameInitials(group.getGroupName()));//TODO FTIAKSTO

    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }

    // convenience method for getting data at click position
    public Group getItem(int id) {
        return mData.get(id);
    }

    // allows clicks events to be caught
    public void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView groupName;
        TextView groupLastMessage;
        TextView groupLastMessageTimestamp;
        View groupIcn;
        TextView groupIcnText;

        ViewHolder(View itemView) {
            super(itemView);
            groupName = itemView.findViewById(R.id.name);
            groupLastMessage = itemView.findViewById(R.id.lastMessage);
            groupLastMessageTimestamp = itemView.findViewById(R.id.lastMessageTimestamp);
            groupIcnText = itemView.findViewById(R.id.groupIcnTxt);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }
}
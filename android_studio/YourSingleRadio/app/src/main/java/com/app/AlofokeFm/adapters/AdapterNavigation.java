package com.app.AlofokeFm.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.app.AlofokeFm.R;
import com.app.AlofokeFm.database.dao.SocialEntity;
import com.app.AlofokeFm.database.prefs.AdsPref;
import com.app.AlofokeFm.database.prefs.SharedPref;
import com.app.AlofokeFm.utils.Constant;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.card.MaterialCardView;
import com.solodroid.ads.sdk.format.NativeAdViewHolder;

import java.util.List;

public class AdapterNavigation extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final int VIEW_ITEM = 0;
    private final int VIEW_AD = 1;
    private List<SocialEntity> items;
    Context context;
    private OnItemClickListener mOnItemClickListener;
    private int clickedItemPosition = 1;
    SharedPref sharedPref;
    AdsPref adsPref;
    public static boolean isFirstItemClicked = false;

    public interface OnItemClickListener {
        void onItemClick(View view, SocialEntity obj, int position);
    }

    public void setOnItemClickListener(final OnItemClickListener mItemClickListener) {
        this.mOnItemClickListener = mItemClickListener;
    }

    public AdapterNavigation(Context context, List<SocialEntity> items) {
        this.items = items;
        this.context = context;
        this.sharedPref = new SharedPref(context);
        this.adsPref = new AdsPref(context);
    }

    public static class OriginalViewHolder extends RecyclerView.ViewHolder {

        public TextView menuSection;
        public TextView menuName;
        public ImageView menuIcon;
        public LinearLayout lytItem;
        //public RelativeLayout lytParent;
        public MaterialCardView cardView;

        public OriginalViewHolder(View v) {
            super(v);
            menuSection = v.findViewById(R.id.menu_section);
            menuName = v.findViewById(R.id.menu_name);
            menuIcon = v.findViewById(R.id.menu_icon);
            lytItem = v.findViewById(R.id.lyt_item);
            //lytParent = v.findViewById(R.id.lyt_parent);
            cardView = v.findViewById(R.id.lyt_parent);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder vh;
        if (viewType == VIEW_AD) {
            View view;
            if (Constant.NATIVE_AD_STYLE_DRAWER_MENU.equals("small")) {
                view = LayoutInflater.from(parent.getContext()).inflate(com.solodroid.ads.sdk.R.layout.view_native_ad_radio, parent, false);
            } else if (Constant.NATIVE_AD_STYLE_DRAWER_MENU.equals("medium")) {
                view = LayoutInflater.from(parent.getContext()).inflate(com.solodroid.ads.sdk.R.layout.view_native_ad_news, parent, false);
            } else if (Constant.NATIVE_AD_STYLE_DRAWER_MENU.equals("large")) {
                view = LayoutInflater.from(parent.getContext()).inflate(com.solodroid.ads.sdk.R.layout.view_native_ad_medium, parent, false);
            } else {
                view = LayoutInflater.from(parent.getContext()).inflate(com.solodroid.ads.sdk.R.layout.view_native_ad_medium, parent, false);
            }
            vh = new NativeAdViewHolder(view);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_drawer, parent, false);
            vh = new OriginalViewHolder(v);
        }
        return vh;
    }

    @SuppressLint({"RecyclerView", "NotifyDataSetChanged"})
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {

        if (holder instanceof OriginalViewHolder) {

            final SocialEntity obj = items.get(position);
            final OriginalViewHolder vItem = (OriginalViewHolder) holder;

            if (position == 1) {
                vItem.menuSection.setText(context.getString(R.string.drawer_section_menu));
                vItem.menuSection.setVisibility(View.VISIBLE);
            } else if (position == 3) {
                vItem.menuSection.setText(context.getString(R.string.drawer_section_socials));
                vItem.menuSection.setVisibility(View.VISIBLE);
            } else {
                vItem.menuSection.setText("");
                vItem.menuSection.setVisibility(View.GONE);
            }

            vItem.menuName.setText(obj.social_name);

            if (obj.social_icon.equals("home")) {
                vItem.menuIcon.setImageResource(R.drawable.ic_drawer_home);
            } else if (obj.social_icon.equals("settings")) {
                vItem.menuIcon.setImageResource(R.drawable.ic_drawer_settings);
            } else {
                Glide.with(context)
                        .load(obj.social_icon.replace(" ", "%20"))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.ic_thumbnail)
                        .centerCrop()
                        .into(vItem.menuIcon);
            }

            vItem.cardView.setOnClickListener(view -> {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClick(view, obj, position);
                    clickedItemPosition = position;
                    notifyDataSetChanged();
                }
            });

            if (clickedItemPosition == position) {
                vItem.lytItem.setBackgroundResource(R.drawable.bg_item_selected);
                vItem.menuName.setTextColor(ContextCompat.getColor(context, R.color.color_light_primary));
                vItem.menuIcon.setColorFilter(ContextCompat.getColor(context, R.color.color_light_primary), PorterDuff.Mode.SRC_IN);
            } else {
                vItem.lytItem.setBackgroundResource(R.drawable.bg_item_unselected);
                vItem.menuName.setTextColor(ContextCompat.getColor(context, R.color.color_light_text));
                vItem.menuIcon.setColorFilter(ContextCompat.getColor(context, R.color.color_light_text), PorterDuff.Mode.SRC_IN);
            }

        } else if (holder instanceof NativeAdViewHolder) {

            final NativeAdViewHolder vItem = (NativeAdViewHolder) holder;

            if (Constant.NATIVE_AD_DRAWER_MENU) {

                if (adsPref.getAdStatus().equals("1")) {
                    vItem.loadNativeAd(context,
                            "1",
                            1,
                            adsPref.getAdType(),
                            adsPref.getBackupAds(),
                            adsPref.getAdMobNativeId(),
                            adsPref.getAdManagerNativeId(),
                            adsPref.getFanNativeUnitId(),
                            adsPref.getAppLovinNativeAdManualUnitId(),
                            "0",
                            "0",
                            false,
                            false,
                            Constant.NATIVE_AD_STYLE_DRAWER_MENU,
                            R.color.color_native_ad_background,
                            R.color.color_native_ad_background
                    );
                }

                int margin = context.getResources().getDimensionPixelOffset(R.dimen.padding_medium);
                vItem.setNativeAdMargin(0, 0, 0, margin);

            }

        }

    }

    @SuppressLint("NotifyDataSetChanged")
    public void setListData(List<SocialEntity> items) {
        this.items = items;
        items.add(0, new SocialEntity());
        items.add(1, new SocialEntity(context.getString(R.string.drawer_menu_home), "home", ""));
        items.add(2, new SocialEntity(context.getString(R.string.drawer_menu_settings), "settings", ""));
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void resetListData() {
        this.items.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        SocialEntity obj = items.get(position);
        if (obj != null) {
            if (obj.social_name == null || obj.social_name.equals("")) {
                return VIEW_AD;
            }
            return VIEW_ITEM;
        } else {
            return VIEW_ITEM;
        }
    }

}
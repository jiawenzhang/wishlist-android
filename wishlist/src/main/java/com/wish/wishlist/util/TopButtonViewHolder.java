package com.wish.wishlist.util;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.wish.wishlist.R;

/**
 * Created by jiawen on 15-11-08.
 */

public class TopButtonViewHolder extends RecyclerView.ViewHolder {
    public TextView txtView;
    public ImageView imgProfile;
    public FrameLayout rootLayout;

    public TopButtonViewHolder(View v) {
        super(v);
        txtView = (TextView) v.findViewById(R.id.txt_view);
        imgProfile = (ImageView) v.findViewById(R.id.profile_image);
        rootLayout = (FrameLayout) v.findViewById(R.id.recyclerview_top_button_layout);
    }
}

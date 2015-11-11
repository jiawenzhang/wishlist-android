package com.wish.wishlist.util;

/**
 * Created by jiawen on 15-10-05.
 */

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.wish.wishlist.R;
import com.wish.wishlist.model.WishItem;

import java.text.DecimalFormat;
import java.util.List;

public class WishAdapterList extends WishAdapter {
    private static final String TAG = "WishAdapter";

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView txtName;
        public TextView txtPrice;
        public TextView txtStore;
        public TextView txtAddress;
        public ImageView imgComplete;
        public ImageView imgPhoto;
        public LinearLayout rootLayout;

        public ViewHolder(View v) {
            super(v);
            txtName = (TextView) v.findViewById(R.id.txtName);
            txtPrice = (TextView) v.findViewById(R.id.txtPrice);
            txtStore = (TextView) v.findViewById(R.id.txtStore);
            txtAddress = (TextView) v.findViewById(R.id.txtAddress);
            imgComplete = (ImageView) v.findViewById(R.id.checkmark_complete);
            imgPhoto = (ImageView) v.findViewById(R.id.imgPhoto);
            rootLayout = (LinearLayout) v.findViewById(R.id.wish_root_layout);
        }
    }

    public WishAdapterList(List<WishItem> wishList, Activity fromActivity) {
        super(wishList);
        setWishTapListener(fromActivity);
    }

    // Create new views (invoked by the layout manager)
    @Override
    public WishAdapterList.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.wishitem_single, parent, false);
        // set the view's size, margins, padding and layout parameters
        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder vh, int position) {
        // - get element from your data set at this position
        // - replace the contents of the view with that element
        final WishAdapterList.ViewHolder holder = (WishAdapterList.ViewHolder) vh;
        final WishItem wish = mWishList.get(position);

        final String photoWebURL = wish.getPicURL();
        final String photoParseURL = wish.getPicParseURL();
        if (photoWebURL != null) {
            holder.imgPhoto.setVisibility(View.VISIBLE);
            Picasso.with(holder.imgPhoto.getContext()).load(photoWebURL).fit().centerCrop().into(holder.imgPhoto);
        } else if (photoParseURL != null) {
            holder.imgPhoto.setVisibility(View.VISIBLE);
            Picasso.with(holder.imgPhoto.getContext()).load(photoParseURL).fit().centerCrop().into(holder.imgPhoto);
        } else {
            holder.imgPhoto.setVisibility(View.GONE);
        }

        holder.txtName.setText(wish.getName());

        double price = wish.getPrice();
        //we use float.min_value to indicate price is not available
        if (price != Double.MIN_VALUE) {
            DecimalFormat Dec = new DecimalFormat("0.00");
            String priceStr = (Dec.format(price));
            holder.txtPrice.setText(WishItem.priceStringWithCurrency(priceStr));
            holder.txtPrice.setVisibility(View.VISIBLE);
        } else {
            holder.txtPrice.setVisibility(View.GONE);
        }

        String storeName = wish.getStoreName();
        boolean hasStoreName = false;
        if (!storeName.equals("")) {
            hasStoreName = true;
            holder.txtStore.setText(storeName);
            holder.txtStore.setVisibility(View.VISIBLE);
        } else {
            holder.txtStore.setVisibility(View.GONE);
        }

        String Address = wish.getAddress();
        if (!Address.equals("unknown") && !Address.equals("")) {
            if (!hasStoreName) {
                Address = "At " + Address;
            }
            holder.txtAddress.setText(Address);
            holder.txtAddress.setVisibility(View.VISIBLE);
        } else {
            holder.txtAddress.setVisibility(View.GONE);
        }

        int complete = wish.getComplete();
        if (complete == 1) {
            holder.imgComplete.setVisibility(View.VISIBLE);
        } else {
            holder.imgComplete.setVisibility(View.GONE);
        }

        holder.rootLayout.setClickable(true);
        holder.rootLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "wish clicked");
                onWishTapped(wish);
            }
        });
    }
}

package com.wish.wishlist.util;

/**
 * Created by jiawen on 15-10-05.
 */

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.wish.wishlist.R;
import com.wish.wishlist.WishlistApplication;
import com.wish.wishlist.model.WishItem;

import java.text.DecimalFormat;
import java.util.List;

public class WishAdapterGrid extends WishAdapter {

    private int mScreenWidth;
    private static final String TAG = "WishAdapter";

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView txtName;
        public TextView txtPrice;
        public ImageView imgComplete;
        public ImageView imgPhoto;
        public LinearLayout rootLayout;

        public ViewHolder(View v) {
            super(v);
            txtName = (TextView) v.findViewById(R.id.txtName);
            txtPrice = (TextView) v.findViewById(R.id.txtPrice);
            imgComplete = (ImageView) v.findViewById(R.id.checkmark_complete);
            imgPhoto = (ImageView) v.findViewById(R.id.imgPhoto);
            rootLayout = (LinearLayout) v.findViewById(R.id.wish_root_layout);
        }
    }

    public WishAdapterGrid(final List<WishItem> wishList, Activity fromActivity) {
        super(wishList);
        setWishTapListener(fromActivity);
        final Display display = ((WindowManager) WishlistApplication.getAppContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        mScreenWidth = size.x / 2;
        Log.d(TAG, " screen width " + mScreenWidth);
    }

    // Create new views (invoked by the layout manager)
    @Override
    public WishAdapterGrid.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.wishitem_grid, parent, false);
        // set the view's size, margins, padding and layout parameters
        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder vh, int position) {
        // - get element from your data set at this position
        // - replace the contents of the view with that element
        final WishAdapterGrid.ViewHolder holder = (WishAdapterGrid.ViewHolder) vh;
        final WishItem wish = mWishList.get(position);

        final String photoWebURL = wish.getPicURL();
        final String photoParseURL = wish.getPicParseURL();
        if (photoWebURL != null) {
            holder.imgPhoto.setVisibility(View.VISIBLE);
            Picasso.with(holder.imgPhoto.getContext()).load(photoWebURL).resize(mScreenWidth, 0).into(holder.imgPhoto);
        } else if (photoParseURL != null) {
            holder.imgPhoto.setVisibility(View.VISIBLE);
            Picasso.with(holder.imgPhoto.getContext()).load(photoParseURL).resize(mScreenWidth, 0).into(holder.imgPhoto);
        } else {
            holder.imgPhoto.setVisibility(View.GONE);
        }

        holder.txtName.setText(wish.getName());
        final double price = wish.getPrice();
        //we use float.min_value to indicate price is not available
        if (price != Double.MIN_VALUE) {
            DecimalFormat Dec = new DecimalFormat("0.00");
            String priceStr = (Dec.format(price));
            holder.txtPrice.setText(WishItem.priceStringWithCurrency(priceStr));
            holder.txtPrice.setVisibility(View.VISIBLE);
        } else {
            holder.txtPrice.setVisibility(View.GONE);
        }

        final int complete = wish.getComplete();
        if (complete == 1) {
            holder.imgComplete.setVisibility(View.VISIBLE);
        } else {
            holder.imgComplete.setVisibility(View.GONE);
        }

        holder.rootLayout.setClickable(true);
        holder.rootLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "wish tapped");
                onWishTapped(wish);
            }
        });
    }
}

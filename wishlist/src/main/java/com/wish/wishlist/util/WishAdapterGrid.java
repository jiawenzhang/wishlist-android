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

import com.parse.ParseFile;
import com.parse.ParseObject;
import com.squareup.picasso.Picasso;
import com.wish.wishlist.R;
import com.wish.wishlist.WishlistApplication;
import com.wish.wishlist.db.ItemDBManager;
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

    public WishAdapterGrid(List<ParseObject> wishList, Activity fromActivity) {
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
        WishAdapterGrid.ViewHolder holder = (WishAdapterGrid.ViewHolder) vh;
        final ParseObject wish = mWishList.get(position);

        String photoURL = wish.getString(ItemDBManager.KEY_PHOTO_URL);
        ParseFile photoFile = wish.getParseFile(WishItem.PARSE_KEY_IMAGE);
        if (photoURL != null) {
            holder.imgPhoto.setVisibility(View.VISIBLE);
            Picasso.with(holder.imgPhoto.getContext()).load(photoURL).resize(mScreenWidth, 0).into(holder.imgPhoto);
            Log.e(TAG, "web url " + photoURL);
        } else if (photoFile != null) {
            holder.imgPhoto.setVisibility(View.VISIBLE);
            Picasso.with(holder.imgPhoto.getContext()).load(photoFile.getUrl()).resize(mScreenWidth, 0).into(holder.imgPhoto);
            Log.e(TAG, "parse url " + photoFile.getUrl());
        } else {
            holder.imgPhoto.setVisibility(View.GONE);
        }

        holder.txtName.setText(wish.getString(ItemDBManager.KEY_NAME));

        double price = wish.getDouble(ItemDBManager.KEY_PRICE);
        //we use float.min_value to indicate price is not available
        if (price != Double.MIN_VALUE) {
            DecimalFormat Dec = new DecimalFormat("0.00");
            String priceStr = (Dec.format(price));
            holder.txtPrice.setText(WishItem.priceStringWithCurrency(priceStr));
            holder.txtPrice.setVisibility(View.VISIBLE);
        } else {
            holder.txtPrice.setVisibility(View.GONE);
        }

        int complete = wish.getInt(ItemDBManager.KEY_COMPLETE);
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
                onWishTapped(WishItem.fromParseObject(wish, -1));
            }
        });
    }
}

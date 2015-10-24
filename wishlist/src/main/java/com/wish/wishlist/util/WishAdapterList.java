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

import com.parse.ParseFile;
import com.parse.ParseObject;
import com.squareup.picasso.Picasso;
import com.wish.wishlist.R;
import com.wish.wishlist.db.ItemDBManager;
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

    public WishAdapterList(List<ParseObject> wishList, Activity fromActivity) {
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
        WishAdapterList.ViewHolder holder = (WishAdapterList.ViewHolder) vh;
        final ParseObject wish = mWishList.get(position);

        String photoURL = wish.getString(ItemDBManager.KEY_PHOTO_URL);
        ParseFile photoFile = wish.getParseFile(WishItem.PARSE_KEY_IMAGE);
        if (photoURL != null) {
            holder.imgPhoto.setVisibility(View.VISIBLE);
            Picasso.with(holder.imgPhoto.getContext()).load(photoURL).fit().centerCrop().into(holder.imgPhoto);
        } else if (photoFile != null) {
            holder.imgPhoto.setVisibility(View.VISIBLE);
            Picasso.with(holder.imgPhoto.getContext()).load(photoFile.getUrl()).fit().centerCrop().into(holder.imgPhoto);
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

        String storeName = wish.getString(ItemDBManager.KEY_STORENAME);
        boolean hasStoreName = false;
        if (!storeName.equals("")) {
            hasStoreName = true;
            holder.txtStore.setText(storeName);
            holder.txtStore.setVisibility(View.VISIBLE);
        } else {
            holder.txtStore.setVisibility(View.GONE);
        }

        String Address = wish.getString(ItemDBManager.KEY_ADDRESS);
        if (!Address.equals("unknown") && !Address.equals("")) {
            if (!hasStoreName) {
                Address = "At " + Address;
            }
            holder.txtAddress.setText(Address);
            holder.txtAddress.setVisibility(View.VISIBLE);
        } else {
            holder.txtAddress.setVisibility(View.GONE);
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
                Log.d(TAG, "wish clicked");
                onWishTapped(WishItem.fromParseObject(wish, -1));
            }
        });
    }
}

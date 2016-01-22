package com.wish.wishlist.wish;

/**
 * Created by jiawen on 15-10-05.
 */

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.bignerdranch.android.multiselector.MultiSelector;
import com.squareup.picasso.Picasso;
import com.wish.wishlist.R;
import com.wish.wishlist.WishlistApplication;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.image.PhotoFileCreater;
import com.wish.wishlist.util.RoundedCornersTransformation;

import java.io.File;
import java.text.DecimalFormat;
import java.util.List;

public class WishAdapterGrid extends WishAdapter {

    private int mScreenWidth;
    private static final String TAG = "WishAdapter";

    public class ViewHolder extends ItemSwappingHolder {
        public CardView cardView;
        public TextView txtName;
        public TextView txtDescription;
        public TextView txtPrice;
        public ImageView imgComplete;
        public ImageView imgPrivate;
        public ImageView imgPhoto;

        public ViewHolder(View v, MultiSelector multiSelector) {
            super(v, multiSelector);
            cardView = (CardView) v.findViewById(R.id.wish_grid_card);
            txtName = (TextView) v.findViewById(R.id.txtName);
            txtDescription = (TextView) v.findViewById(R.id.txtDescription);
            txtPrice = (TextView) v.findViewById(R.id.txtPrice);
            imgComplete = (ImageView) v.findViewById(R.id.imgComplete);
            imgPrivate = (ImageView) v.findViewById(R.id.imgPrivate);
            imgPhoto = (ImageView) v.findViewById(R.id.imgPhoto);
        }
    }

    public WishAdapterGrid(final List<WishItem> wishList, Activity fromActivity, MultiSelector ms) {
        super(wishList, fromActivity, ms);

        final Display display = ((WindowManager) WishlistApplication.getAppContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        mScreenWidth = size.x / 2;
        Log.d(TAG, " screen width " + mScreenWidth);

    }

    // Create new views (invoked by the layout manager)
    @Override
    public WishAdapterGrid.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.wishitem_grid, parent, false);
        // set the view's size, margins, padding and layout parameters
        ViewHolder vh = new ViewHolder(v, mMultiSelector);
        final Drawable d = ContextCompat.getDrawable(WishlistApplication.getAppContext(), R.drawable.card_foreground_selector);
        vh.setSelectionModeBackgroundDrawable(d);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder vh, int position) {
        // - get element from your data set at this position
        // - replace the contents of the view with that element
        final WishAdapterGrid.ViewHolder holder = (WishAdapterGrid.ViewHolder) vh;
        final WishItem wish = mWishList.get(position);

        if (Build.VERSION.SDK_INT < 21) {
            // to make rounded corner work
            holder.cardView.setPreventCornerOverlap(false);
        }

        final String photo_path = wish.getFullsizePicPath();
        if (photo_path != null) {
            // we are loading my wish, and it has a fullsize photo on disk
            String thumb_path = PhotoFileCreater.getInstance().thumbFilePath(photo_path);
            holder.imgPhoto.setVisibility(View.VISIBLE);
            Picasso.with(holder.imgPhoto.getContext()).load(new File(thumb_path)).resize(mScreenWidth, 0).transform(mTransform).into(holder.imgPhoto);
        } else {
            // we are loading friend wish

            final String photoWebURL = wish.getPicURL();
            final String photoParseURL = wish.getPicParseURL();
            if (photoWebURL != null) {
                holder.imgPhoto.setVisibility(View.VISIBLE);
                Picasso.with(holder.imgPhoto.getContext()).load(photoWebURL).resize(mScreenWidth, 0).transform(mTransform).into(holder.imgPhoto);
            } else if (photoParseURL != null) {
                holder.imgPhoto.setVisibility(View.VISIBLE);
                Picasso.with(holder.imgPhoto.getContext()).load(photoParseURL).resize(mScreenWidth, 0).transform(mTransform).into(holder.imgPhoto);
            } else {
                holder.imgPhoto.setVisibility(View.GONE);
            }
        }

        holder.txtName.setText(wish.getName());

        if (wish.getDesc().isEmpty()) {
            holder.txtDescription.setVisibility(View.GONE);
        } else {
            holder.txtDescription.setText(wish.getDesc());
            holder.txtDescription.setVisibility(View.VISIBLE);
        }

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

        final int access = wish.getAccess();
        if (access == wish.PRIVATE) {
            holder.imgPrivate.setVisibility(View.VISIBLE);
        } else {
            holder.imgPrivate.setVisibility(View.GONE);
        }
    }

    protected RoundedCornersTransformation.CornerType cornerType() {
        return RoundedCornersTransformation.CornerType.TOP;
    }
}

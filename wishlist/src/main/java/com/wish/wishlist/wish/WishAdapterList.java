package com.wish.wishlist.wish;

/**
 * Created by jiawen on 15-10-05.
 */

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.List;

public class WishAdapterList extends WishAdapter {
    private static final String TAG = "WishAdapter";

    public class ViewHolder extends ItemSwappingHolder {
        public TextView txtName;
        public TextView txtPrice;
        public TextView txtStore;
        public TextView txtDescription;
        public ImageView imgComplete;
        public ImageView imgPrivate;
        public ImageView imgPhoto;
        public CardView cardView;

        public ViewHolder(View itemView, MultiSelector multiSelector) {
            super(itemView, multiSelector);
            txtName = (TextView) itemView.findViewById(R.id.txtName);
            txtPrice = (TextView) itemView.findViewById(R.id.txtPrice);
            txtStore = (TextView) itemView.findViewById(R.id.txtStore);
            txtDescription = (TextView) itemView.findViewById(R.id.txtDescription);
            imgComplete = (ImageView) itemView.findViewById(R.id.imgComplete);
            imgPrivate = (ImageView) itemView.findViewById(R.id.imgPrivate);
            imgPhoto = (ImageView) itemView.findViewById(R.id.imgPhoto);
            cardView = (CardView) itemView.findViewById(R.id.wish_list_card);
        }
    }

    public WishAdapterList(List<WishItem> wishList, Activity fromActivity, MultiSelector ms) {
        super(wishList, fromActivity, ms);
    }

    // Create new views (invoked by the layout manager)
    @Override
    public WishAdapterList.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.wishitem_list, parent, false);
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
        final WishAdapterList.ViewHolder holder = (WishAdapterList.ViewHolder) vh;

        if (Build.VERSION.SDK_INT < 21) {
            // to make rounded corner work
            holder.cardView.setPreventCornerOverlap(false);
        }

        final WishItem wish = mWishList.get(position);

        final String photo_path = wish.getFullsizePicPath();
        if (photo_path != null) {
            // we are loading my wish
            String thumb_path = PhotoFileCreater.getInstance().thumbFilePath(photo_path);
            Picasso.with(holder.imgPhoto.getContext()).load(new File(thumb_path)).fit().centerCrop().transform(mTransform).into(holder.imgPhoto);
            holder.imgPhoto.setVisibility(View.VISIBLE);
        } else {
            // we are loading friend wish
            final WebImgMeta webImgMeta = wish.getWebImgMeta();
            final WebImgMeta parseImgMeta = wish.getParseImgMeta();
            if (webImgMeta != null) {
                holder.imgPhoto.setVisibility(View.VISIBLE);
                Picasso.with(holder.imgPhoto.getContext()).load(webImgMeta.mUrl).fit().centerCrop().transform(mTransform).into(holder.imgPhoto);
            } else if (parseImgMeta != null) {
                holder.imgPhoto.setVisibility(View.VISIBLE);
                Picasso.with(holder.imgPhoto.getContext()).load(parseImgMeta.mUrl).fit().centerCrop().transform(mTransform).into(holder.imgPhoto);
            } else {
                holder.imgPhoto.setVisibility(View.GONE);
            }
        }

        holder.txtName.setText(wish.getName());

        String priceStr = wish.getPriceAsString();
        if (priceStr != null) {
            holder.txtPrice.setText(WishItem.priceStringWithCurrency(priceStr));
            holder.txtPrice.setVisibility(View.VISIBLE);
        } else {
            holder.txtPrice.setVisibility(View.GONE);
        }

        String storeName = wish.getStoreName();
        if (!storeName.isEmpty()) {
            holder.txtStore.setText(storeName);
            holder.txtStore.setVisibility(View.VISIBLE);
        } else {
            holder.txtStore.setVisibility(View.GONE);
        }

        if (!wish.getDesc().isEmpty()) {
            holder.txtDescription.setText(wish.getDesc());
            holder.txtDescription.setVisibility(View.VISIBLE);
        } else {
            holder.txtDescription.setVisibility(View.GONE);
        }

        int complete = wish.getComplete();
        if (complete == 1) {
            holder.imgComplete.setVisibility(View.VISIBLE);
        } else {
            holder.imgComplete.setVisibility(View.GONE);
        }

        if (WishlistApplication.getAppContext().getResources().getBoolean(R.bool.enable_friend)) {
            int access = wish.getAccess();
            if (access == wish.PRIVATE) {
                holder.imgPrivate.setVisibility(View.VISIBLE);
            } else {
                holder.imgPrivate.setVisibility(View.GONE);
            }
        } else {
            holder.imgPrivate.setVisibility(View.GONE);
        }
    }

    protected RoundedCornersTransformation.CornerType cornerType() {
        return RoundedCornersTransformation.CornerType.RIGHT;
    }
}

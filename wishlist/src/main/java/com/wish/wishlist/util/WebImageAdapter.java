package com.wish.wishlist.util;

import android.app.Activity;
import android.os.Build;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.view.View.OnClickListener;

import com.squareup.picasso.Picasso;
import com.wish.wishlist.R;
import com.wish.wishlist.WishlistApplication;
import com.wish.wishlist.activity.WebImage;

import java.util.ArrayList;

public class WebImageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    /******************* WebImageTapListener *********************/
    private WebImageTapListener mWebImageTapListener = null;
    public interface WebImageTapListener {
        void onWebImageTap(int position);
    }
    protected void onWebImageTap(int position) {
        if (mWebImageTapListener != null) {
            mWebImageTapListener.onWebImageTap(position);
        }
    }
    public void setWebImageTapListener(final WebImageTapListener listener) {
        mWebImageTapListener = listener;
    }
    /*************************************************************/

    static final String TAG = "WebImageAdapter";
    private ArrayList<WebImage> mImageList;

    public class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        DynamicHeightImageView imageView;
        TextView textView;

        public ViewHolder(View v) {
            super(v);
            cardView = (CardView) v.findViewById(R.id.web_image_card);
            imageView = (DynamicHeightImageView) v.findViewById(R.id.web_image);
            textView = (TextView) v.findViewById(R.id.web_image_size);
        }
    }

    public WebImageAdapter(ArrayList<WebImage> imageList, Activity fromActivity) {
        mImageList = imageList;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        // - get element from your data set at this position
        // - replace the contents of the view with that element
        ViewHolder holder_ = (ViewHolder) holder;

        if (Build.VERSION.SDK_INT < 21) {
            // to make rounded corner work
            holder_.cardView.setPreventCornerOverlap(false);
        }

        final WebImage webImage = mImageList.get(position);
        if (webImage.mId.equals("button")) {
            holder_.imageView.setVisibility(View.GONE);
            holder_.textView.setText("Load more");
            final float scale = WishlistApplication.getAppContext().getResources().getDisplayMetrics().density;
            int button_height = (int) (40 /*dp*/ * scale + 0.5f);
            holder_.textView.setHeight(button_height);
        } else {
            if (webImage.mUrl != null) {
                holder_.imageView.setVisibility(View.VISIBLE);
                final float ratio = (float) webImage.mHeight / (float) webImage.mWidth;
                holder_.imageView.setHeightRatio(ratio);
                Picasso.with(holder_.imageView.getContext()).load(webImage.mUrl).fit().into(holder_.imageView);

                holder_.textView.setText(webImage.mWidth + " x " + webImage.mHeight);
            }
        }

        holder_.cardView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onWebImageTap(position);
            }
        });
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.web_image_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public int getItemCount() {
        return mImageList.size();
    }
}
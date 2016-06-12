package com.wish.wishlist.util;

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
import com.squareup.picasso.Transformation;
import com.wish.wishlist.BuildConfig;
import com.wish.wishlist.R;
import com.wish.wishlist.WishlistApplication;
import com.wish.wishlist.activity.WebImage;

import java.util.ArrayList;

public class WebImageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    protected Transformation mTransform;

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

    public WebImageAdapter(ArrayList<WebImage> imageList) {
        mImageList = imageList;
        if (Build.VERSION.SDK_INT < 21) {
            // Image in CardView pre-LOLLIPOP (API 21) does not have rounded corner, so we need to transform the image ourselves to have
            // round corner
            int radius = (int) WishlistApplication.getAppContext().getResources().getDimension(R.dimen.radius); // radius is in px
            mTransform = new RoundedCornersTransformation(radius, 0,RoundedCornersTransformation.CornerType.TOP);
        } else {
            mTransform = new NoTransformation();
        }
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

                //Fixme: ideally, we should get the exact card view width
                int proximateCardWidth = dimension.screenWidth() / 2;
                Picasso.with(holder_.imageView.getContext()).load(webImage.mUrl).resize(proximateCardWidth, 0).transform(mTransform).into(holder_.imageView);

                if (BuildConfig.DEBUG) {
                    holder_.textView.setText(webImage.mWidth + " x " + webImage.mHeight);
                } else {
                    holder_.textView.setVisibility(View.GONE);
                }
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
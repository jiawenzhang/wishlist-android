package com.wish.wishlist;

import android.content.Context;
import android.graphics.Point;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;

import com.etsy.android.grid.util.DynamicHeightImageView;
import com.etsy.android.grid.util.DynamicHeightTextView;
import com.squareup.picasso.Picasso;
import com.wish.wishlist.activity.WebImage;

import java.util.ArrayList;

/***
 * ADAPTER
 */

public class WebImageAdapter extends ArrayAdapter<WebImage> {
    static final String TAG = "WebImageAdapter";

    static class ViewHolder {
        DynamicHeightImageView imageView;
        DynamicHeightTextView textView;
    }

    private final LayoutInflater mLayoutInflater;

    public WebImageAdapter(final Context context, final int textViewResourceId, ArrayList<WebImage> webImages) {
        super(context, textViewResourceId, webImages);
        mLayoutInflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        ViewHolder vh;
        WebImage img = getItem(position);
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.web_image_item, parent, false);
            vh = new ViewHolder();
            vh.imageView = (DynamicHeightImageView) convertView.findViewById(R.id.web_image);
            vh.textView = (DynamicHeightTextView) convertView.findViewById(R.id.web_image_size);
            convertView.setTag(vh);
        } else {
            vh = (ViewHolder) convertView.getTag();
        }

        Display display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        int width = size.x / 2;

        final float ratio = (float) img.mHeight / (float) img.mWidth;
        int height = (int) (width * ratio);

        if (img.mId.equals("button")) {
            vh.imageView.setVisibility(View.GONE);
            vh.textView.setText("Load more");
            final float scale = getContext().getResources().getDisplayMetrics().density;
            int button_height = (int) (40 /*dp*/ * scale + 0.5f);
            vh.textView.setHeight(button_height);
        } else {
            vh.imageView.setVisibility(View.VISIBLE);
            Picasso.with(getContext()).load(img.mUrl).resize(width, height).into(vh.imageView);
            vh.imageView.setHeightRatio(ratio);
            vh.textView.setText(img.mWidth + " x " + img.mHeight);
        }
        return convertView;
    }
}
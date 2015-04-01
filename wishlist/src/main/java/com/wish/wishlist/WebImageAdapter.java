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

    static class ViewHolder {
        DynamicHeightImageView imageView;
        DynamicHeightTextView textView;
    }

    private final LayoutInflater mLayoutInflater;

    public WebImageAdapter(final Context context, final int textViewResourceId, ArrayList<WebImage> imageUrls) {
        super(context, textViewResourceId, imageUrls);
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

        final float ratio = (float) img.mHeight / (float) img.mWidth;
        Display display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        int width = size.x / 2;
        int height = (int) (width * ratio);

        Picasso.with(getContext()).load(img.mUrl).resize(width, height).into(vh.imageView);
        vh.imageView.setHeightRatio(ratio);
        vh.textView.setText(img.mWidth + " x " +img.mHeight);
        return convertView;
    }
}
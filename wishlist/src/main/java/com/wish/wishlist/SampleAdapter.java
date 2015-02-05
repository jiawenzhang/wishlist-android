package com.wish.wishlist;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.etsy.android.grid.util.DynamicHeightTextView;
import com.etsy.android.grid.util.DynamicHeightImageView;

/***
 * ADAPTER
 */

public class SampleAdapter extends ArrayAdapter<String> {

    private static final String TAG = "SampleAdapter";
    private static int a = 0;

    static class ViewHolder {
        DynamicHeightImageView imageView;
        DynamicHeightTextView txtLineOne;
    }

    private final LayoutInflater mLayoutInflater;

    public SampleAdapter(final Context context, final int textViewResourceId) {
        super(context, textViewResourceId);
        mLayoutInflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {

        ViewHolder vh;
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.staggeredview_item, parent, false);
            vh = new ViewHolder();
            vh.imageView = (DynamicHeightImageView) convertView.findViewById(R.id.imageGridView);
            vh.txtLineOne = (DynamicHeightTextView) convertView.findViewById(R.id.txt_line1);

            convertView.setTag(vh);
        } else {
            vh = (ViewHolder) convertView.getTag();
        }

        Drawable d = getContext().getResources().getDrawable(R.drawable.cake);
        if (a == 0) {
            vh.imageView.setImageResource(R.drawable.cake);
            a = 1;
            d = getContext().getResources().getDrawable(R.drawable.cake);
        }
        else if (a == 1) {
            vh.imageView.setImageResource(R.drawable.d3);
            a = 2;
            d = getContext().getResources().getDrawable(R.drawable.d3);
        }
        else if (a == 2) {
            vh.imageView.setImageResource(R.drawable.mini_cooper);
            a = 0;
            d = getContext().getResources().getDrawable(R.drawable.mini_cooper);
        }
        float h = (float) d.getIntrinsicHeight();
        float w = (float) d.getIntrinsicWidth();
        Log.d(TAG, " h/w" + h/w);
        vh.imageView.setHeightRatio(h/w);
        return convertView;
    }
}
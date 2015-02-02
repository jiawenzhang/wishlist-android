package com.wish.wishlist;


import java.util.ArrayList;
import java.util.Random;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

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
        Button btnGo;
    }

    private final LayoutInflater mLayoutInflater;
    private final Random mRandom;
    private final ArrayList<Integer> mBackgroundColors;

    private static final SparseArray<Double> sPositionHeightRatios = new SparseArray<Double>();

    public SampleAdapter(final Context context, final int textViewResourceId) {
        super(context, textViewResourceId);
        mLayoutInflater = LayoutInflater.from(context);
        mRandom = new Random();
        mBackgroundColors = new ArrayList<Integer>();
        //mBackgroundColors.add(R.color.orange);
        mBackgroundColors.add(R.color.black);
        mBackgroundColors.add(R.color.green);
        //mBackgroundColors.add(R.color.blue);
        mBackgroundColors.add(R.color.red);
        mBackgroundColors.add(R.color.yellow);
        mBackgroundColors.add(R.color.grey);
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {

        ViewHolder vh;
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.list_item_sample, parent, false);
            vh = new ViewHolder();
            vh.imageView = (DynamicHeightImageView) convertView.findViewById(R.id.imageGridView);
            vh.txtLineOne = (DynamicHeightTextView) convertView.findViewById(R.id.txt_line1);
            vh.btnGo = (Button) convertView.findViewById(R.id.btn_go);

            convertView.setTag(vh);
        } else {
            vh = (ViewHolder) convertView.getTag();
        }

        double positionHeight = getPositionRatio(position);
        int backgroundIndex = position >= mBackgroundColors.size() ?
                position % mBackgroundColors.size() : position;

        convertView.setBackgroundResource(mBackgroundColors.get(backgroundIndex));

        Log.d(TAG, "getView position:" + position + " h:" + positionHeight);

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

        //vh.txtLineOne.setHeightRatio(positionHeight);
        //vh.txtLineOne.setText(getItem(position) + position);

//        vh.btnGo.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(final View v) {
//                Toast.makeText(getContext(), "Button Clicked Position " +
//                        position, Toast.LENGTH_SHORT).show();
//            }
//        });

        return convertView;
    }

    private double getPositionRatio(final int position) {
        double ratio = sPositionHeightRatios.get(position, 0.0);
        // if not yet done generate and stash the columns height
        // in our real world scenario this will be determined by
        // some match based on the known height and width of the image
        // and maybe a helpful way to get the column height!
        if (ratio == 0) {
            ratio = getRandomHeightRatio();
            sPositionHeightRatios.append(position, ratio);
            Log.d(TAG, "getPositionRatio:" + position + " ratio:" + ratio);
        }
        return ratio;
    }

    private double getRandomHeightRatio() {
        return (mRandom.nextDouble() / 2.0) + 1.0; // height will be 1.0 - 1.5 the width
    }
}
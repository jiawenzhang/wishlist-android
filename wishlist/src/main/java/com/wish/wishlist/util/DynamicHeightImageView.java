package com.wish.wishlist.util;

/**
 * Created by jiawen on 2016-01-22.
 */

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.wish.wishlist.R;

/**
 * An {@link android.widget.ImageView} layout that maintains a consistent width to height aspect ratio.
 */
public class DynamicHeightImageView extends ImageView {

    private float mHeightRatio;

    public DynamicHeightImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        //TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.DynamicHeightImageView, 0, 0);
        //try {
        //    mHeightRatio = ta.getFloat(R.styleable.DynamicHeightImageView_height_ratio, 0.0f);
        //} finally {
        //    ta.recycle();
        //}
    }

    public DynamicHeightImageView(Context context) {
        super(context);
    }

    public void setHeightRatio(float ratio) {
        if (ratio != mHeightRatio) {
            mHeightRatio = ratio;
            requestLayout();
        }
    }

    public double getHeightRatio() {
        return mHeightRatio;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mHeightRatio > 0.0f) {
            // set the image views size
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = (int) (width * mHeightRatio);
            setMeasuredDimension(width, height);
        }
        else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }
}

package com.wish.wishlist.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;

import com.etsy.android.grid.StaggeredGridView;
import com.wish.wishlist.R;
import com.wish.wishlist.WebImageAdapter;
import com.wish.wishlist.activity.WebImage;

import java.util.ArrayList;

public class WebImageFragmentDialog extends DialogFragment implements
        AbsListView.OnScrollListener, AbsListView.OnItemClickListener {

    private StaggeredGridView mGridView;
    private WebImageAdapter mAdapter;
    private static ArrayList<WebImage> mList;

    public static WebImageFragmentDialog newInstance(ArrayList<WebImage> list) {
        mList = list;
        return new WebImageFragmentDialog();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        int style = DialogFragment.STYLE_NO_TITLE, theme;
        theme = android.R.style.Theme_Holo_Light_Dialog;
        setStyle(style, theme);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.web_image_view, container, false);
        mGridView = (StaggeredGridView) v.findViewById(R.id.staggered_web_image_view);

        if (savedInstanceState == null) {
            final LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        }

        if (mAdapter == null) {
            if (mList == null) {
                //Log.d("AAA", "list null");
            } else {
                for (WebImage img : mList) {
                    //Log.d("AAA", img.mUrl + " " + img.mId + " " + img.mWidth + " " + img.mHeight);
                }
            }
            mAdapter = new WebImageAdapter(getActivity(), 0, mList);
        }
        mGridView.setAdapter(mAdapter);
        mGridView.setOnScrollListener(this);
        mGridView.setOnItemClickListener(this);

        return v;
    }

    public static interface OnWebImageSelectedListener {
        public abstract void onWebImageSelected(int position);
    }

    private OnWebImageSelectedListener mListener;

    // make sure the Activity implemented it
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            this.mListener = (OnWebImageSelectedListener)activity;
        }
        catch (final ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnWebImageSelectedListener");
        }
    }

    @Override
    public void onScrollStateChanged(final AbsListView view, final int scrollState) {
        //Log.d(TAG, "onScrollStateChanged:" + scrollState);
    }

    @Override
    public void onScroll(final AbsListView view, final int firstVisibleItem, final int visibleItemCount, final int totalItemCount) {
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        this.mListener.onWebImageSelected(position);
        dismiss();
    }
}

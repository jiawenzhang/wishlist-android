package com.wish.wishlist.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;

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
    private OnWebImageSelectedListener mWebImageSelectedListener;
    private OnLoadMoreFromWebViewListener mLoadMoreFromWebView;
    private OnLoadMoreSelectedListener mLoadMoreSelectedListener;
    private OnWebImageCancelledListener mWebImageCancelledListener;
    private static boolean mAllowLoadMore = true;
    final private static String TAG = "WebImageFragmentDialog";

    public static WebImageFragmentDialog newInstance(ArrayList<WebImage> list, boolean allowLoadMore) {
        mList = list;
        mAllowLoadMore = allowLoadMore;
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
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.d(TAG, "onCreateDialog");
        if (mList.size() > 1) {
            // We have multiple images, we show them in a grid view that is loaded in onCreateView
            return super.onCreateDialog(savedInstanceState);
        }

        if (savedInstanceState == null) {
            final LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        }

        // We only have one image, show the image in an AlertDialog with a "Load more" button
        final View v = getActivity().getLayoutInflater().inflate(R.layout.single_web_image_view, null);

        final View imageFrame = v.findViewById(R.id.single_web_image_frame);
        imageFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mWebImageSelectedListener.onWebImageSelected(0);
                dismiss();
            }
        });

        final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setPositiveButton("Load more",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                mLoadMoreSelectedListener.onLoadMoreFromStaticHtml();
                            }
                        }
                )
                .create();

        dialog.setView(v, 0, 0, 0, 0);

        final Bitmap bitmap = mList.get(0).mBitmap;
        final ImageView imageView = (ImageView) v.findViewById(R.id.single_web_image);

        // We have to get the width of the imageFrame after the dialog is shown, otherwise,
        // the UI has not been rendered and the width is always zero.
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                int width = imageFrame.getWidth();
                if (bitmap != null) {
                    // Resize the image to fit the dialog's width
                    final float ratio = (float) bitmap.getHeight() / (float) bitmap.getWidth();
                    int height = (int) (width * ratio);
                    final Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
                    imageView.setImageBitmap(resizedBitmap);
                }
            }
        });

        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        if (mList.size() <= 1) {
           return super.onCreateView(inflater, container, savedInstanceState);
        }

        View v = inflater.inflate(R.layout.web_image_view, container, false);
        mGridView = (StaggeredGridView) v.findViewById(R.id.staggered_web_image_view);

        if (savedInstanceState == null) {
            final LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        }

        if (mAdapter == null) {
            if (mList == null) {
                Log.d(TAG, "mList is null");
            } else {
                if (mAllowLoadMore) {
                    mList.add(new WebImage(null, 0, 0, "button", null));
                }
                for (WebImage img : mList) {
                    Log.d(TAG, img.mUrl + " " + img.mId + " " + img.mWidth + " " + img.mHeight);
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

    public static interface OnLoadMoreFromWebViewListener {
        public abstract void onLoadMoreFromWebView();
    }

    public static interface OnLoadMoreSelectedListener {
        public abstract void onLoadMoreFromStaticHtml();
    }

    public static interface OnWebImageCancelledListener {
        public abstract void onWebImageCancelled();
    }

    // make sure the Activity implemented it
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            this.mWebImageSelectedListener = (OnWebImageSelectedListener) activity;
            this.mLoadMoreFromWebView = (OnLoadMoreFromWebViewListener) activity;
            this.mLoadMoreSelectedListener = (OnLoadMoreSelectedListener) activity;
            this.mWebImageCancelledListener = (OnWebImageCancelledListener) activity;
        }
        catch (final ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnWebImageSelectedListener");
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        Log.d(TAG, "onCancel");
        mWebImageCancelledListener.onWebImageCancelled();
        dismiss();
    }

    @Override
    public void onScrollStateChanged(final AbsListView view, final int scrollState) {
    }

    @Override
    public void onScroll(final AbsListView view, final int firstVisibleItem, final int visibleItemCount, final int totalItemCount) {
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        if (mAllowLoadMore && position == mList.size() - 1) {
            mList.remove(position);
            this.mLoadMoreFromWebView.onLoadMoreFromWebView();
        } else {
            this.mWebImageSelectedListener.onWebImageSelected(position);
        }
        dismiss();
    }
}

package com.wish.wishlist.fragment;

import android.app.Activity;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.wish.wishlist.R;
import com.wish.wishlist.WishlistApplication;
import com.wish.wishlist.util.Analytics;
import com.wish.wishlist.util.ImageDimensionTask;
import com.wish.wishlist.util.WebImageAdapter;
import com.wish.wishlist.activity.WebImage;
import com.wish.wishlist.util.dimension;
import com.wish.wishlist.widgets.ItemDecoration;

import java.util.ArrayList;

public class WebImageFragmentDialog extends DialogFragment implements
        WebImageAdapter.WebImageTapListener,
        ImageDimensionTask.OnImageDimension {

    protected StaggeredGridLayoutManager mStaggeredGridLayoutManager;
    private WebImageAdapter mAdapter;
    private static ArrayList<WebImage> mList;
    private static boolean mShowOneImage;
    private OnWebImageSelectedListener mWebImageSelectedListener;
    private OnLoadMoreSelectedListener mLoadMoreSelectedListener;
    private OnWebImageCancelledListener mWebImageCancelledListener;
    private RecyclerView mRecyclerView;
    private TextView mTitleView;
    private ImageDimensionTask mImageDimensionTask = null;
    private Handler mainHandler;
    private static String mHost;
    final private static String TAG = "WebImageFragmentDialog";

    public static WebImageFragmentDialog newInstance(ArrayList<WebImage> list, boolean showOneImage, String host) {
        mList = list;
        mShowOneImage = showOneImage;
        mHost = host;
        return new WebImageFragmentDialog();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainHandler = new Handler(getContext().getMainLooper());
        setRetainInstance(true);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (!mShowOneImage && mList.size() > 1) {
            // We have multiple images, we show them in a grid view that is loaded in onCreateView
            return new AppCompatDialog(getActivity(), R.style.AppCompatAlertDialogStyleNoTitle);
        }

        // We only have one image, show the image in an AlertDialog with a "Load more" button
        final View v = getActivity().getLayoutInflater().inflate(R.layout.single_web_image_view, null);

        final View imageFrame = v.findViewById(R.id.single_web_image_frame);
        imageFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Analytics.send(Analytics.WISH, "SelectOneWebImage", mHost);
                mWebImageSelectedListener.onWebImageSelected(mList.get(0).mUrl);
                dismiss();
            }
        });

        final AlertDialog dialog = new AlertDialog.Builder(getActivity(), R.style.AppCompatAlertDialogStyle)
                .setPositiveButton("More images",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                mLoadMoreSelectedListener.onLoadMoreImages();
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
                } else if (mList.get(0).mUrl != null) {
                    Log.e(TAG, mList.get(0).mUrl);
                    Picasso.with(imageView.getContext()).load(mList.get(0).mUrl).resize(width, 0).into(imageView);
                }
            }
        });

        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mShowOneImage) {
           return super.onCreateView(inflater, container, savedInstanceState);
        }

        View v = inflater.inflate(R.layout.web_image_view, container, false);
        mRecyclerView = (RecyclerView) v.findViewById(R.id.web_image_recycler_view);
        mTitleView = (TextView) v.findViewById(R.id.web_image_title);

        // set the min view height so the view won't keep expanding while images are loaded into it.
        // Fixme: should ideally get real the max height of the view
        int viewHeight = (int) ((double) dimension.screenHeight() * 0.9);
        mRecyclerView.setMinimumHeight(viewHeight);
        mStaggeredGridLayoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(mStaggeredGridLayoutManager);
        int itemSpace = (int) WishlistApplication.getAppContext().getResources().getDimension(R.dimen.item_decoration_space); // in px
        if (itemSpace % 2 != 0) {
            // odd number, make it even
            // ItemDecoration will use half mItemSpace for item rect margin
            itemSpace--;
        }

        mRecyclerView.addItemDecoration(new ItemDecoration(itemSpace));

        mImageDimensionTask = new ImageDimensionTask(this);
        mImageDimensionTask.execute(mList);
        setTitle(0, mList.size());

        return v;
    }

    @Override
    public void onImageDimension(final WebImage webImage, final int count, final int total) {
        // onImageDimension is invoked in the background thread of an AsyncTask, post it to main thread
        // for UI layout
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (webImage != null) {
                    if (mAdapter == null) {
                        mAdapter = new WebImageAdapter(webImage);
                        mAdapter.setWebImageTapListener(WebImageFragmentDialog.this);

                        mRecyclerView.setAdapter(mAdapter);
                    } else {
                        mAdapter.addItem(webImage);
                    }
                }
                setTitle(count, total);
            }
        });
    }

    private void setTitle(int count, int total) {
        if (count < total) {
            mTitleView.setText("Loading images... " + count + "/" + total);
        } else {
            if (total <= 1) {
                mTitleView.setText("Got " + total + " image");
            } else if (total > 1) {
                mTitleView.setText("Got " + total + " images");
            }
        }
    }

    public static interface OnWebImageSelectedListener {
        public abstract void onWebImageSelected(String url);
    }

    public static interface OnLoadMoreSelectedListener {
        public abstract void onLoadMoreImages();
    }

    public static interface OnWebImageCancelledListener {
        public abstract void onWebImageCancelled(boolean showOneImage);
    }

    // make sure the Activity implemented it
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            this.mWebImageSelectedListener = (OnWebImageSelectedListener) activity;
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
        if (mImageDimensionTask != null) {
            mImageDimensionTask.cancel(true);
        }
        mWebImageCancelledListener.onWebImageCancelled(mShowOneImage);
        dismiss();
    }

    public void onWebImageTap(String url) {
        this.mWebImageSelectedListener.onWebImageSelected(url);

        int i;
        for (i = 0; i < mList.size(); i++) {
            if (url.equals(mList.get(i).mUrl)) {
                break;
            }
        }
        Analytics.send(Analytics.DEBUG, "SelectMoreWebImage", mHost + " " + i + "/" + mList.size());

        dismiss();
    }
}
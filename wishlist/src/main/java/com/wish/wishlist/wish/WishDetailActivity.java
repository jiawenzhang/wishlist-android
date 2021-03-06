package com.wish.wishlist.wish;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.view.ActionMode;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ksoichiro.android.observablescrollview.ObservableScrollView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;
import com.github.ksoichiro.android.observablescrollview.ScrollUtils;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.wish.wishlist.R;
import com.wish.wishlist.activity.ActivityBase;
import com.wish.wishlist.activity.FullscreenPhotoActivity;
import com.wish.wishlist.activity.MapActivity;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.social.ShareHelper;
import com.wish.wishlist.util.ScreenOrientation;
import com.wish.wishlist.util.dimension;
import com.wish.wishlist.widgets.ClearableEditText;


public abstract class WishDetailActivity extends ActivityBase implements ObservableScrollViewCallbacks {
    private final static String TRANSLUCENT_TOOLBAR = "TRANSLUCENT_TOOLBAR";
    private final static String TAG = "WishDetailActivity";

    protected Boolean mTranslucentToolBar = false;
    protected ActionMode mActionMode;
    protected View mToolbarView;
    protected ObservableScrollView mScrollView;
    private int mParallaxImageHeight;
    protected ImageView mPhotoView;
    protected ImageView mImgComplete;
    protected LinearLayout mCompleteInnerLayout;
    protected LinearLayout mPrivateInnerLayout;
    protected TextView mTextComplete;
    protected TextView mTextPrivate;
    protected ClearableEditText mNameView;
    protected ClearableEditText mDescriptionView;
    protected TextView mDateView;
    protected ClearableEditText mPriceView;
    protected ClearableEditText mStoreView;
    protected ClearableEditText mLocationView;
    protected TextView mLinkView;
    protected LinearLayout mLinkLayout;
    protected ClearableEditText mLinkText;
    protected WishItem mItem;
    protected ProgressDialog mProgressDialog;
    private Target mTarget;

    protected abstract boolean myWish();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wishitem_detail);
        setupActionBar(R.id.item_detail_toolbar);

        getSupportActionBar().setTitle("");

        mToolbarView = findViewById(R.id.item_detail_toolbar);

        mScrollView = (ObservableScrollView) findViewById(R.id.scroll);
        mScrollView.setScrollViewCallbacks(this);
        mParallaxImageHeight = getResources().getDimensionPixelSize(R.dimen.wish_detail_image_height);

        mPhotoView = (ImageView) findViewById(R.id.imgPhotoDetail);
        mCompleteInnerLayout = (LinearLayout) findViewById(R.id.completeInnerLayout);
        mPrivateInnerLayout = (LinearLayout) findViewById(R.id.privateInnerLayout);
        mImgComplete = (ImageView) findViewById(R.id.completeImageView);
        mTextComplete = (TextView) findViewById(R.id.completeTextView);
        mTextPrivate = (TextView) findViewById(R.id.privateTextView);
        mNameView = (ClearableEditText) findViewById(R.id.itemNameDetail);
        mDescriptionView = (ClearableEditText) findViewById(R.id.itemDescription);
        mDateView = (TextView) findViewById(R.id.itemDateDetail);
        mPriceView = (ClearableEditText) findViewById(R.id.itemPrice);
        mStoreView = (ClearableEditText) findViewById(R.id.itemStore);
        mLocationView = (ClearableEditText) findViewById(R.id.itemLocation);
        mLinkView = (TextView) findViewById(R.id.itemLink);
        mLinkLayout = (LinearLayout) findViewById(R.id.linkLayout);
        mLinkText = (ClearableEditText) findViewById(R.id.itemLinkText);
    }

    protected void showPhoto() {}

    protected void showItemInfo() {
        if (mItem == null) {
            return;
        }

        showPhoto();

        if (mItem.getComplete() == 1) {
            mCompleteInnerLayout.setVisibility(View.VISIBLE);
        } else {
            mCompleteInnerLayout.setVisibility(View.GONE);
        }

        if (mItem.getAccess() == WishItem.PRIVATE) {
            mPrivateInnerLayout.setVisibility(View.VISIBLE);
        } else {
            mPrivateInnerLayout.setVisibility(View.GONE);
        }

        mDateView.setText(mItem.getUpdatedTimeStr());

        mNameView.setText(mItem.getName());
        if (mItem.getComplete() == 1) {
            final ImageView completeImage = (ImageView) findViewById(R.id.completeImageView);
            completeImage.setVisibility(View.VISIBLE);
        }

        // format the price
        String priceStr = mItem.getFormatPriceString();
        if (priceStr != null) {
            mPriceView.setText(WishItem.priceStringWithCurrency(priceStr));
            mPriceView.setVisibility(View.VISIBLE);
        } else {
            mPriceView.setVisibility(View.GONE);
        }

        //used as a note
        String description = mItem.getDesc();
        if (description != null) {
            mDescriptionView.setText(description);
            mDescriptionView.setVisibility(View.VISIBLE);
        } else {
            mDescriptionView.setVisibility(View.GONE);
        }

        String storeName = mItem.getStoreName();
        if (storeName != null) {
            mStoreView.setText(storeName);
            mStoreView.setVisibility(View.VISIBLE);
        } else {
            mStoreView.setVisibility(View.GONE);
        }

        String address = mItem.getAddress();
        if (address != null) {
            mLocationView.setText(address);
            mLocationView.setVisibility(View.VISIBLE);
        } else {
            mLocationView.setVisibility(View.GONE);
        }

        String link = mItem.getLink();
        if (link != null) {
            String url = mItem.getLink();
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://" + url;
            }
            String text = "<a href=\"" + url + "\">LINK</a>";
            mLinkView.setText(Html.fromHtml(text));
            mLinkView.setMovementMethod(LinkMovementMethod.getInstance());
            mLinkView.setVisibility(View.VISIBLE);
            mLinkLayout.setVisibility(View.VISIBLE);
        } else {
            mLinkView.setVisibility(View.GONE);
            mLinkLayout.setVisibility(View.GONE);
        }
    }

    protected void inflateMenu(int resId, Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(resId, menu);

        MenuItem item = menu.findItem(R.id.location);
        if (!mItem.hasGeoLocation()) {
            item.setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        long itemId = menuItem.getItemId();
        if (itemId == android.R.id.home) {
            Intent resultIntent = new Intent();
            setResult(RESULT_OK, resultIntent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    protected void shareItem() {
        long[] itemIds = new long[1];
        itemIds[0] = mItem.getId();
        ShareHelper share = new ShareHelper(this, itemIds);
        share.share();
    }

    protected void showOnMap() {
        if (mItem.getLatitude() == null && mItem.getLongitude() == null) {
            Toast toast = Toast.makeText(this, "location unknown", Toast.LENGTH_SHORT);
            toast.show();
        } else {
            Intent mapIntent = new Intent(this, MapActivity.class);
            mapIntent.putExtra(MapActivity.TYPE, MapActivity.MARK_ONE);
            mapIntent.putExtra(MapActivity.ITEM, mItem);
            mapIntent.putExtra(MapActivity.MY_WISH, myWish());
            startActivity(mapIntent);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(TRANSLUCENT_TOOLBAR, mTranslucentToolBar);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mTranslucentToolBar = savedInstanceState.getBoolean(TRANSLUCENT_TOOLBAR);
        if (mTranslucentToolBar) {
            // Need to post here so the value from mScrollView.getScrollY() is correct
            mScrollView.post(new Runnable() {
                public void run() {
                    onScrollChanged(mScrollView.getScrollY(), false, false);
                }
            });
        }
    }

    protected int toolBarHeight() {
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(android.support.v7.appcompat.R.attr.actionBarSize, tv, true);
        return TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
    }

    protected void setPhotoVisible(boolean visible) {
        if (visible) {
            mPhotoView.setVisibility(View.VISIBLE);
            if (mActionMode == null) {
                // only enable translucent toolbar when action mode is off
                setToolbarTranslucent(true);
            } else {
                setToolbarTranslucent(false);
            }
        } else {
            mPhotoView.setVisibility(View.GONE);
            setToolbarTranslucent(false);
        }
    }

    protected void showFullScreenPhoto(String key, String val) {
        final Intent i = new Intent(this, FullscreenPhotoActivity.class);
        i.putExtra(key, val);

        if (Build.VERSION.SDK_INT < 21) {
            startActivity(i);
        } else {
            if (key.equals(FullscreenPhotoActivity.PHOTO_URL)) {
                // we are loading the image from web, it could be slow when opening the FullscreenPhotoActivity due to animation
                // because we need the full bitmap to be ready before showing the animation
                // so let's pre-load the image to cache before starting the activity
                mTarget = new Target() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                        dismissProgressDialog();
                        if (bitmap != null) {
                            Log.d(TAG, "valid bitmap");
                            animateToFullScreenPhoto(i);
                        } else {
                            Log.e(TAG, "null bitmap");
                        }
                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {}

                    @Override
                    public void onBitmapFailed(Drawable errorDrawable) {
                        dismissProgressDialog();
                        Log.e(TAG, "onBitmapFailed");
                    }
                };

                showProgressDialog("Loading...");
                Picasso.with(this).load(val).resize(dimension.screenWidth(), dimension.screenHeight()).centerInside().onlyScaleDown().into(mTarget);
            } else {
                animateToFullScreenPhoto(i);
            }
        }
    }

    private void animateToFullScreenPhoto(Intent i) {
        // hide toolbar so it's fading away won't interfere with photo animation
        mToolbarView.setVisibility(View.GONE);

        // show photo transition animation
        ActivityOptionsCompat options = ActivityOptionsCompat.
                makeSceneTransitionAnimation(this, mPhotoView, getString(R.string.photo));
        startActivity(i, options.toBundle());
    }

    @Override
    public void onActivityReenter(int requestCode, Intent data) {
        super.onActivityReenter(requestCode, data);

        // show toolbar only after the photoView is fully drawn
        // so the toolbar's appearing won't interfere with photo animation
        mPhotoView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                removeOnGlobalLayoutListener(mPhotoView, this);
                mToolbarView.setVisibility(View.VISIBLE);
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static void removeOnGlobalLayoutListener(View v, ViewTreeObserver.OnGlobalLayoutListener listener){
        if (Build.VERSION.SDK_INT < 16) {
            v.getViewTreeObserver().removeGlobalOnLayoutListener(listener);
        } else {
            v.getViewTreeObserver().removeOnGlobalLayoutListener(listener);
        }
    }

    protected void setToolbarTranslucent(boolean enable) {
        mTranslucentToolBar = enable;
        if (enable) {
            mScrollView.setPadding(mScrollView.getPaddingLeft(), 0, mScrollView.getPaddingRight(), mScrollView.getPaddingBottom());
            float alpha = Math.min(1, (float) mScrollView.getScrollY() / mParallaxImageHeight);
            mToolbarView.setBackgroundColor(ScrollUtils.getColorWithAlpha(alpha, ContextCompat.getColor(this, R.color.material_dark)));
        } else {
            mScrollView.setPadding(mScrollView.getPaddingLeft(), toolBarHeight(), mScrollView.getPaddingRight(), mScrollView.getPaddingBottom());
            mToolbarView.setBackgroundColor(ContextCompat.getColor(this, R.color.material_dark));
        }
    }

    protected void showProgressDialog(String text) {
        ScreenOrientation.lock(this);
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(text);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    protected void dismissProgressDialog() {
        ScreenOrientation.unlock(this);
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    public void onScrollChanged(int scrollY, boolean firstScroll, boolean dragging) {
        if (mTranslucentToolBar) {
            float alpha = Math.min(1, (float) scrollY / mParallaxImageHeight);
            mToolbarView.setBackgroundColor(ScrollUtils.getColorWithAlpha(alpha, ContextCompat.getColor(this, R.color.material_dark)));
        }
    }

    @Override
    public void onDownMotionEvent() {}

    @Override
    public void onUpOrCancelMotionEvent(ScrollState scrollState) {}
}

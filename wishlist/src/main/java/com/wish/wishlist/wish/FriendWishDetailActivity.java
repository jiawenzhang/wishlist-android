package com.wish.wishlist.wish;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.wish.wishlist.R;
import com.wish.wishlist.activity.FullscreenPhotoActivity;
import com.wish.wishlist.event.EventBus;
import com.wish.wishlist.event.MyWishChangeEvent;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.util.Analytics;

import java.util.ArrayList;

public class FriendWishDetailActivity extends WishDetailActivity implements
        WishImageDownloader.onWishImageDownloadDoneListener {
    private static final String TAG = "FriendWishDetailAct";
    private WishImageDownloader mWishImageDownloader;

    public final static String ITEM = "ITEM";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Analytics.sendScreen("FriendWishDetail");

        Intent i = getIntent();
        mItem = i.getParcelableExtra(ITEM);

        showItemInfo();
        mLinkText.setVisibility(View.GONE);

        final View imageFrame = findViewById(R.id.imagePhotoDetailFrame);
        imageFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mItem.getImgMetaArray() != null) {
                    showFullScreenPhoto(FullscreenPhotoActivity.PHOTO_URL, mItem.getImgMetaArray().get(0).mUrl);
                }
            }
        });

        // Friend's wish is not edible, so make EditText not edible
        mNameView.setFocusableInTouchMode(false);
        mDescriptionView.setFocusableInTouchMode(false);
        mPriceView.setFocusableInTouchMode(false);
        mStoreView.setFocusableInTouchMode(false);
        mLocationView.setFocusableInTouchMode(false);
    }

    @Override
    protected void showPhoto() {
        if (mItem.getImgMetaArray() != null) {
            setPhotoVisible(true);
            Picasso.with(this).load(mItem.getImgMetaArray().get(0).mUrl).fit().centerCrop().into(mPhotoView);
        } else {
            setPhotoVisible(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        inflateMenu(R.menu.menu_friend_wish_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.save:
                Log.d(TAG, "save as my own wish");
                showProgressDialog("Saving...");
                mWishImageDownloader = new WishImageDownloader();
                mWishImageDownloader.setWishImageDownloadDoneListener(this);
                mWishImageDownloader.download(new ArrayList<WishItem>() {{
                    add(mItem);
                }});

                Analytics.send(Analytics.WISH, "SaveAsMyWish", "FromDetail");
                return true;
            case R.id.share:
                Log.d(TAG, "share");
                return true;
            case R.id.location:
                Log.d(TAG, "map");
                showOnMap();
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    @Override
    protected void showProgressDialog(final String text) {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(text);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                Log.d(TAG, text + " canceled");
                Toast.makeText(FriendWishDetailActivity.this, "Failed to save wish", Toast.LENGTH_LONG).show();
            }
        });
        mProgressDialog.show();
    }

    public void onWishImageDownloadDone(boolean success) {
        Log.d(TAG, "wishes image download all done");
        if (success) {
            Toast.makeText(this, "Saved", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Failed, check network", Toast.LENGTH_LONG).show();
        }
        mProgressDialog.dismiss();
        EventBus.getInstance().post(new MyWishChangeEvent());
    }

    @Override
    protected boolean myWish() { return false; }
}

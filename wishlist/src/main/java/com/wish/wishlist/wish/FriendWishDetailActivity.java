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
import com.wish.wishlist.model.WishItem;

import java.util.ArrayList;

public class FriendWishDetailActivity extends WishDetailActivity implements
        WishImageDownloader.onWishImageDownloadDoneListener {
    private static final String TAG = "FriendWishDetailAct";
    private WishImageDownloader mWishImageDownloader;
    private ProgressDialog mProgressDialog = null;

    public final static String ITEM = "ITEM";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent i = getIntent();
        mItem = i.getParcelableExtra(ITEM);

        showItemInfo();

        final View imageFrame = findViewById(R.id.imagePhotoDetailFrame);
        imageFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mItem.getImgMetaArray() != null) {
                    showFullScreenPhoto(FullscreenPhotoActivity.PHOTO_URL, mItem.getImgMetaArray().get(0).mUrl);
                }
            }
        });
    }

    @Override
    protected void showPhoto() {
        if (mItem.getImgMetaArray() != null) {
            mPhotoView.setVisibility(View.VISIBLE);
            Picasso.with(this).load(mItem.getImgMetaArray().get(0).mUrl).fit().centerCrop().into(mPhotoView);
        } else {
            mPhotoView.setVisibility(View.GONE);
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
                Log.d(TAG, "save");
                showProgressDialog("Saving...");
                mWishImageDownloader = new WishImageDownloader();
                mWishImageDownloader.setWishImageDownloadDoneListener(this);
                mWishImageDownloader.download(new ArrayList<WishItem>() {{
                    add(mItem);
                }});
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

    private void showProgressDialog(final String text) {
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
    }

    @Override
    protected boolean myWish() { return false; }
}

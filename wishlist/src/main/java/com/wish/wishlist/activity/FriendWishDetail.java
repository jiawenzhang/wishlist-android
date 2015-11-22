package com.wish.wishlist.activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.wish.wishlist.R;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.util.WishImageDownloader;

import java.util.ArrayList;

public class FriendWishDetail extends WishDetail implements
        WishImageDownloader.onWishImageDownloadDoneListener {
    private static final String TAG = "FriendWishDetail";
    private WishImageDownloader mWishImageDownloader;
    private ProgressDialog mProgressDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent i = getIntent();
        mItem = i.getParcelableExtra(FriendsWish.ITEM);
        showItemInfo(mItem);

//        final View imageFrame = findViewById(R.id.imagePhotoDetailFrame);
//        imageFrame.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent i = new Intent(FriendWishDetail.this, FullscreenPhoto.class);
//                if (_fullsize_picture_str != null) {
//                    i.putExtra(EditItem.FULLSIZE_PHOTO_PATH, _fullsize_picture_str);
//                    startActivity(i);
//                }
//            }
//        });
    }

    @Override
    protected void showPhoto() {
        if (mItem.getPicURL() != null) {
            // we have the photo somewhere on the internet
            mPhotoView.setVisibility(View.VISIBLE);
            Picasso.with(this).load(mItem.getPicURL()).fit().centerCrop().into(mPhotoView);
        } else if (mItem.getPicParseURL() != null) {
            // we have the photo on Parse
            mPhotoView.setVisibility(View.VISIBLE);
            Picasso.with(this).load(mItem.getPicParseURL()).fit().centerCrop().into(mPhotoView);
        } else {
            mPhotoView.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_friend_wish_detail, menu);
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
                mWishImageDownloader.download(new ArrayList<WishItem>() {{ add(mItem); }} );
                return true;
            case R.id.share:
                Log.d(TAG, "share");
                return true;
            case R.id.map:
                Log.d(TAG, "map");
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
                Toast.makeText(FriendWishDetail.this, "Failed to save wish", Toast.LENGTH_LONG).show();
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
}

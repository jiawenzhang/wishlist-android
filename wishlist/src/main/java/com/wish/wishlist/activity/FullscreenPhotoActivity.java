package com.wish.wishlist.activity;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.wish.wishlist.R;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;

import com.wish.wishlist.view.ZoomPanImageView;

import java.io.FileNotFoundException;
import java.io.IOException;

public class FullscreenPhotoActivity extends Activity {
    private static final String TAG = "FullscreenPhotoAct";
    public static final String PHOTO_PATH = "PHOTO_PATH";
    public static final String PHOTO_URI = "PHOTO_URI";
    public static final String PHOTO_URL = "PHOTO_URL";
    String mPhotoPath;
    String mPhotoUri;
    String mPhotoUrl;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fullscreen_photo);

        Intent intent = getIntent();
        mPhotoPath = intent.getStringExtra(PHOTO_PATH);
        mPhotoUri = intent.getStringExtra(PHOTO_URI);
        mPhotoUrl = intent.getStringExtra(PHOTO_URL);

        if (savedInstanceState != null) {
            //we are restoring on switching screen orientation
            mPhotoPath = savedInstanceState.getString(PHOTO_PATH);
            mPhotoUri = savedInstanceState.getString(PHOTO_URI);
            mPhotoUrl = savedInstanceState.getString(PHOTO_URL);
        }

        final ZoomPanImageView imageItem = (ZoomPanImageView) findViewById(R.id.fullscreen_photo);

        if (mPhotoUri != null) {
            try {
                Uri photoUri = Uri.parse(mPhotoUri);
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoUri);
                imageItem.setImageBitmap(bitmap);
            } catch (FileNotFoundException e) {
                Log.e(TAG, e.toString());
                finish();
            } catch (IOException e) {
                Log.e(TAG, e.toString());
                finish();
            }
        } else if (mPhotoPath != null) {
            final Bitmap bitmap = BitmapFactory.decodeFile(mPhotoPath, null);
            if (bitmap != null) {
                imageItem.setImageBitmap(bitmap);
            } else {
                finish();
            }
        } else if (mPhotoUrl != null) {
            Target target = new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    if (bitmap != null) {
                        Log.d(TAG, "valid bitmap");
                        imageItem.setImageBitmap(bitmap);
                    } else {
                        Log.e(TAG, "null bitmap");
                        finish();
                    }
                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {}

                @Override
                public void onBitmapFailed(Drawable errorDrawable) {
                    finish();
                }
            };

            Picasso.with(this).load(mPhotoUrl).into(target);
        } else {
            finish();
        }
    }

    //this will also save the photo on switching screen orientation
    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString(PHOTO_PATH, mPhotoPath);
        savedInstanceState.putString(PHOTO_URI, mPhotoUri);
        savedInstanceState.putString(PHOTO_URL, mPhotoUrl);
        super.onSaveInstanceState(savedInstanceState);
    }

}

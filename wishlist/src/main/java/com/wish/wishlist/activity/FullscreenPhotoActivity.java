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
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.wish.wishlist.image.ImageManager;
import com.wish.wishlist.util.Analytics;
import com.wish.wishlist.util.dimension;
import com.wish.wishlist.view.ZoomPanImageView;

import java.io.FileNotFoundException;
import java.io.IOException;

public class FullscreenPhotoActivity extends Activity {
    private static final String TAG = "FullscreenPhotoAct";
    private String mPhotoPath;
    private String mPhotoUri;
    private String mPhotoUrl;
    private ZoomPanImageView mImageItem;
    private class GetBitmapTask extends AsyncTask<Void, Void, Bitmap> {//<param, progress, result>
        @Override
        protected Bitmap doInBackground(Void... arg) {
            // First decode with inJustDecodeBounds=true to check dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(mPhotoPath, options);
            final float ratio = (float) options.outHeight / (float) options.outWidth;

            int width = dimension.screenWidth();
            int height = (int) (width * ratio);

            return ImageManager.getInstance().decodeSampledBitmapFromFile(mPhotoPath, width, height, false);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap == null) {
                finish();
            } else {
                showPhoto(bitmap);
            }
        }
    }

    public static final String PHOTO_PATH = "PHOTO_PATH";
    public static final String PHOTO_URI = "PHOTO_URI";
    public static final String PHOTO_URL = "PHOTO_URL";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fullscreen_photo);

        // Postpone the shared element enter transition until startPostponedEnterTransition() is called;
        // This is to make the shared element's (photo) transition smooth
        // Potential problem: if photo is from internet (PHOTO_URL), entering the activity might be slow
        ActivityCompat.postponeEnterTransition(this);

        Analytics.sendScreen("FullscreenPhoto");

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

        mImageItem = (ZoomPanImageView) findViewById(R.id.fullscreen_photo);

        if (mPhotoUri != null) {
            try {
                Uri photoUri = Uri.parse(mPhotoUri);
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoUri);
                showPhoto(bitmap);
            } catch (FileNotFoundException e) {
                Log.e(TAG, e.toString());
                finish();
            } catch (IOException e) {
                Log.e(TAG, e.toString());
                finish();
            }
        } else if (mPhotoPath != null) {
            new GetBitmapTask().execute();
        } else if (mPhotoUrl != null) {
            Target target = new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    if (bitmap != null) {
                        Log.d(TAG, "valid bitmap");
                        showPhoto(bitmap);
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

    void showPhoto(Bitmap bitmap) {
        mImageItem.setImageBitmap(bitmap);
        ActivityCompat.startPostponedEnterTransition(FullscreenPhotoActivity.this);
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

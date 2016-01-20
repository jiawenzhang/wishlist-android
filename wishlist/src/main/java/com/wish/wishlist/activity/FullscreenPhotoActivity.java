package com.wish.wishlist.activity;

import com.wish.wishlist.R;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;

import com.wish.wishlist.view.ZoomPanImageView;
import com.wish.wishlist.wish.EditWishActivity;

import java.io.FileNotFoundException;
import java.io.IOException;

public class FullscreenPhotoActivity extends Activity {
    private static final String TAG = "FullscreenPhotoAct";
    public static final String PHOTO_PATH = "PHOTO_PATH";
    public static final String PHOTO_URI = "PHOTO_URI";
    String mPhotoPath;
    String mPhotoUri;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fullscreen_photo);

        Intent intent = getIntent();
        mPhotoPath = intent.getStringExtra(PHOTO_PATH);
        mPhotoUri = intent.getStringExtra(PHOTO_URI);

        if (savedInstanceState != null) {
            //we are restoring on switching screen orientation
            mPhotoPath = savedInstanceState.getString(PHOTO_PATH);
            mPhotoUri = savedInstanceState.getString(PHOTO_URI);
        }

        ZoomPanImageView imageItem = (ZoomPanImageView) findViewById(R.id.fullscreen_photo);

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
        } else {
            final Bitmap bitmap = BitmapFactory.decodeFile(mPhotoPath, null);
            if (bitmap != null) {
                imageItem.setImageBitmap(bitmap);
            } else {
                finish();
            }
        }
        //Bitmap bitmap = null;
//			
//			//check if pic_str is null, which user added this item without taking a pic.
//			if (picture_str != null){
//				Uri picture_Uri = Uri.parse(picture_str);
//				
//				// check if pic_str is a resId
//				try {
//					// view.getContext().getResources().getDrawable(Integer.parseInt(pic_str));
//					int picResId = Integer.valueOf(picture_str, 16).intValue();
//					bitmap = BitmapFactory.decodeResource(imageItem.getContext()
//							.getResources(), picResId);
//					// it is resource id.
//					imageItem.setImageBitmap(bitmap);
//
//				} catch (NumberFormatException e) {
//					// Not a resId, so it must be a content provider uri
//					picture_Uri = Uri.parse(picture_str);
//					imageItem.setImageURI(picture_Uri);
//
//				}
//			}

//			imageItem.setLayoutParams( new ViewGroup.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT));
//
//			
//					imageView.setImageResource(imageId);
//					imageView.setScaleType(ImageView.ScaleType.FIT_XY);




//			/* There isn't enough memory to open up more than a couple camera photos */
//			/* So pre-scale the target bitmap into which the file is decoded */
//
//			/* Get the size of the ImageView */
//			int targetW = imageItem.getWidth();
//			int targetH = imageItem.getHeight();
//
//			/* Get the size of the image */
//			BitmapFactory.Options bmOptions = new BitmapFactory.Options();
//			bmOptions.inJustDecodeBounds = true;
//			BitmapFactory.decodeFile(picture_str, bmOptions);
//			int photoW = bmOptions.outWidth;
//			int photoH = bmOptions.outHeight;
//			
//			/* Figure out which way needs to be reduced less */
//			int scaleFactor = 1;
//			if ((targetW > 0) || (targetH > 0)) {
//				scaleFactor = Math.min(photoW/targetW, photoH/targetH);	
//			}
//
//			/* Set bitmap options to scale the image decode target */
//			bmOptions.inJustDecodeBounds = false;
//			bmOptions.inSampleSize = scaleFactor;
//			bmOptions.inPurgeable = true;

			/* Decode the JPEG file into a Bitmap */
//			Bitmap bitmap = BitmapFactory.decodeFile(picture_str, bmOptions);
        //to-do save mCurrentPhotoPath to db
			
			/* Associate the Bitmap to the ImageView */
//			imageItem.setImageBitmap(bitmap);
//			mVideoUri = null;
//			mImageView.setVisibility(View.VISIBLE);
//			mVideoView.setVisibility(View.INVISIBLE);
    }

    //this will also save the photo on switching screen orientation
    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString(PHOTO_PATH, mPhotoPath);
        savedInstanceState.putString(PHOTO_URI, mPhotoUri);
        super.onSaveInstanceState(savedInstanceState);
    }

}

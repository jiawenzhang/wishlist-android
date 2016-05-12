package com.wish.wishlist.activity;


/**
 * Created by jiawen on 15-09-13.
 */
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.isseiaoki.simplecropview.CropImageView;
import com.wish.wishlist.R;
import com.wish.wishlist.image.ImageManager;
import com.wish.wishlist.util.Analytics;
import com.wish.wishlist.util.ProfileUtil;

import java.io.FileNotFoundException;
import java.io.IOException;

public class CropperActivity extends ActivityBase {
    CropImageView mCropImageView;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cropper);
        setupActionBar(R.id.cropper_toolbar);

        Analytics.sendScreen("Cropper");

        Intent i = getIntent();
        String uri = i.getStringExtra(ProfileActivity.IMAGE_URI);
        Uri imageUri = Uri.parse(uri);

        // Initialize components of the app
        mCropImageView = (CropImageView) findViewById(R.id.CropImageView);
        mCropImageView.setCropMode(CropImageView.CropMode.CIRCLE);

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            mCropImageView.setImageBitmap(bitmap);
        } catch (FileNotFoundException e) {
            Log.e("CropperActivity", e.toString());
        } catch (IOException e) {
            Log.e("CropperActivity", e.toString());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_cropper, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_CANCELED);
                finish();
                return true;
            case R.id.menu_cropper_crop:
                Analytics.send(Analytics.USER, "ChangeAvatar", null);
                final Bitmap croppedImage = mCropImageView.getCroppedBitmap();
                final Bitmap scaledCroppedImage = ImageManager.getScaleDownBitmap(croppedImage, 256);
                ProfileUtil.saveProfileImageToFile(scaledCroppedImage);
                setResult(RESULT_OK);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

package com.wish.wishlist.activity;


/**
 * Created by jiawen on 15-09-13.
 */
import android.app.ActionBar;
import android.app.Activity;
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
import com.wish.wishlist.util.ImageManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Cropper extends Activity {
    CropImageView mCropImageView;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cropper);

        setUpActionBar();

        Intent i = getIntent();
        String uri = i.getStringExtra(Profile.IMAGE_URI);
        Uri imageUri = Uri.parse(uri);

        // Initialize components of the app
        mCropImageView = (CropImageView) findViewById(R.id.CropImageView);
        mCropImageView.setCropMode(CropImageView.CropMode.CIRCLE);

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            mCropImageView.setImageBitmap(bitmap);
        } catch (FileNotFoundException e) {
            Log.e("Cropper", e.toString());
        } catch (IOException e) {
            Log.e("Cropper", e.toString());
        }
    }

    private void setUpActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
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
                final Bitmap croppedImage = mCropImageView.getCroppedBitmap();
                final Bitmap scaledCroppedImage = ImageManager.getThumb(croppedImage);
                Profile.saveProfileImageToFile(scaledCroppedImage);
                setResult(RESULT_OK);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

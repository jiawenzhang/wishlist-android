package com.wish.wishlist.image;

import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;

public class CameraManager {
    private Intent mIntent;
    private String mPhotoPath = null;

    public CameraManager() {
        mIntent =  new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File f = PhotoFileCreater.getInstance().getTempImageFile();
        mPhotoPath = f.getAbsolutePath();

        // we need to supply EXTRA_OUTPUT for the camera to save a high-quality photo
        mIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
    }

    public Intent getCameraIntent() {
        return mIntent;
    }

    public String getPhotoPath() {
        return mPhotoPath;
    }
}

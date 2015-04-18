package com.wish.wishlist.util.camera;

import android.content.Intent;
import android.net.Uri;
import java.io.File;
import java.io.IOException;
import android.provider.MediaStore;

public class CameraManager
{
    private Intent _intent;
    private String _photoPath;

    public CameraManager() {
        _intent =  new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File f;
        try {
            f = PhotoFileCreater.getInstance().setUpPhotoFile(false);
            _photoPath = f.getAbsolutePath();
            _intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Intent getCameraIntent() {
        return _intent;
    }

    public String getPhotoPath() {
        return _photoPath;
    }
}

package com.wish.wishlist.util.camera;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Build;
import android.os.Environment;
import android.util.Log;

public class PhotoFileCreater {
    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";
    private AlbumStorageDirFactory mAlbumStorageDirFactory = null;

    private static PhotoFileCreater instance = null;

    public static PhotoFileCreater getInstance() {
        if (instance == null){
            instance = new PhotoFileCreater();
        }
        return instance;
    }

    private PhotoFileCreater() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            mAlbumStorageDirFactory = new FroyoAlbumDirFactory();
        } else {
            mAlbumStorageDirFactory = new BaseAlbumDirFactory();
        }
    }

    private File createImageFile(boolean thumnail) throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
        File albumF = getAlbumDir(thumnail);
        File imageF = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX, albumF);
        return imageF;
    }

    public File setUpPhotoFile(boolean thumnail) throws IOException {
        hideAlbumFromGallery(); //should not be called here, it shold be less frequent
        File f = createImageFile(thumnail);
        return f;
    }

    private String getAlbumName(boolean thumnail) {
        //return getString(R.string.album_name);
        if (thumnail) {
            return ".WishListThumnail";
        }
        else {
            return ".WishListPhoto";
        }
    }

    private File getAlbumDir(boolean thumnail) {
        File storageDir = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            storageDir = mAlbumStorageDirFactory.getAlbumStorageDir(getAlbumName(thumnail));
            if (storageDir != null) {
                if (! storageDir.mkdirs()) {
                    if (! storageDir.exists()){
                        Log.d("wishlist", "failed to create directory");
                        return null;
                    }
                }
            }

        } else {
            Log.v("wishlist", "External storage is not mounted READ/WRITE.");
        }

        return storageDir;
    }

    private void hideAlbumFromGallery() {
        File thumnailDir = getAlbumDir(true);
        String thumnailPath = thumnailDir.getAbsolutePath() + "/.nomedia";
        //Log.d("wishlist", thumnailPath);
        File f = new File(thumnailPath);
        if (!f.exists()) {
            try {
                f.createNewFile();
            }
            catch (IOException e)
            {
            }
        }

        File photoDir = getAlbumDir(false);
        String photoPath = photoDir.getAbsolutePath() + "/.nomedia";
        Log.d("wishlist", photoPath);
        f = new File(photoPath);
        if (!f.exists()) {
            try {
                f.createNewFile();
            }
            catch (IOException e)
            {
            }
        }
    }
}

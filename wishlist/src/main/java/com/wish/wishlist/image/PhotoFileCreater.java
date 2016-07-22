package com.wish.wishlist.image;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.wish.wishlist.WishlistApplication;

public class PhotoFileCreater {
    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";
    private AlbumStorageDirFactory mAlbumStorageDirFactory = null;

    private static final String TAG = "PhotoFileCreater";

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

    /* old way
    public String thumbFilePath(String fullsizeFilePath) {
        // thumb file has the same file name as fullsize file, but in the .WishlistThumbnail folder
        String fileName = fullsizeFilePath.substring(fullsizeFilePath.lastIndexOf("/") + 1);
        File f = new File(getAlbumDir(true), fileName);
        return f.getAbsolutePath();
    }
    */

    public String thumbFilePath(String fullsizeFilePath) {
        // thumb file has the same file name as fullsize file, but in the /thumb folder
        if (fullsizeFilePath == null) {
            return null;
        }
        String fileName = fullsizeFilePath.substring(fullsizeFilePath.lastIndexOf("/") + 1);
        if (fileName.isEmpty()) {
            return null;
        }

        File dir = new File(WishlistApplication.getAppContext().getFilesDir(), "/thumb");
        File f = new File(dir, fileName);
        return f.getAbsolutePath();
    }

    private File createImageFile(boolean thumb) throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
        File albumF = getAlbumDir(thumb);
        File imageF = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX, albumF);
        return imageF;
    }

    public File getTempImageFile() {
        File albumF = getAlbumDir(false);
        return new File(albumF.getAbsolutePath(), "TEMP_PHOTO.jpg");
    }

    public File setupPhotoFile(boolean thumb) throws IOException {
        hideAlbumFromGallery(); //should not be called here, it should be less frequent
        File f = createImageFile(thumb);
        return f;
    }

    private String getAlbumName(boolean thumb) {
        if (thumb) {
            return ".WishListThumnail";
        } else {
            return ".WishListPhoto";
        }
    }

    public File getAlbumDir(boolean thumb) {
        File storageDir = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            storageDir = mAlbumStorageDirFactory.getAlbumStorageDir(getAlbumName(thumb));
            if (storageDir != null) {
                if (! storageDir.mkdirs()) {
                    if (! storageDir.exists()){
                        Log.d(TAG, "failed to create directory");
                        return null;
                    }
                }
            }

        } else {
            Log.d(TAG, "External storage is not mounted READ/WRITE.");
        }

        return storageDir;
    }

    private void hideAlbumFromGallery() {
        File thumbDir = getAlbumDir(true);
        String thumbPath = thumbDir.getAbsolutePath() + "/.nomedia";
        File f = new File(thumbPath);
        if (!f.exists()) {
            try {
                f.createNewFile();
            }
            catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }

        File photoDir = getAlbumDir(false);
        String photoPath = photoDir.getAbsolutePath() + "/.nomedia";
        Log.d(TAG, photoPath);
        f = new File(photoPath);
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }
    }
}

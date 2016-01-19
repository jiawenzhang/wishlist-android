package com.wish.wishlist.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.res.Resources;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ImageManager
{
    private static ImageManager _instance = null;
    public static final int THUMB_WIDTH = 350; // about half the size of an iphone screen (iphone 5s w 640px, iphone 6 w 750px)
    private static final String TAG = "ImageManager";

    private ImageManager() {}

    public static ImageManager getInstance() {
        if (_instance == null) {
            _instance = new ImageManager();
        }
        return _instance;
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight, boolean fitFullImage) {
        // if fitFullImage is true, the image will be resized so that the whole image will fit into reqWidth * reqHeight
        // in other words, reqWidth*reqHeight is the min rec in which the whole image can be shown
        // if fitFullImage is false, the image can be larger than reqWidth*reqHeight

        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            if (fitFullImage) {
                if (width < height) {
                    inSampleSize = Math.round((float)height / (float)reqHeight);
                } else {
                    inSampleSize = Math.round((float)width / (float)reqWidth);
                }
            }
            else {
                if (width < height) {
                    inSampleSize = Math.round((float)width / (float)reqWidth);
                }
                else {
                    inSampleSize = Math.round((float)height / (float)reqHeight);
                }
            }
        }
        return inSampleSize;
    }

    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight, boolean fitFullImage) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight, fitFullImage);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    public static Bitmap decodeSampledBitmapFromFile(String file, int reqWidth, int reqHeight, boolean fitFullImage) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight, fitFullImage);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(file, options);
    }

    public static String saveBitmapToAlbum(Bitmap bitmap) {
        try {
            //save the image to a file we created in wishlist album
            File f = PhotoFileCreater.getInstance().setupPhotoFile(false);
            String path = f.getAbsolutePath();
            OutputStream stream = new FileOutputStream(path);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream);
            stream.flush();
            stream.close();
            return path;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void saveBitmapToThumb(Uri imageUri, String fullsizePath, Context ctx)
    {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(ctx.getContentResolver(), imageUri);
            saveBitmapToThumb(bitmap, fullsizePath);
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.toString());
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }

    public static void saveBitmapToThumb(String fullsizePath)
    {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        Bitmap bitmap = BitmapFactory.decodeFile(fullsizePath, bmOptions);
        if (bitmap != null) {
            saveBitmapToThumb(bitmap, fullsizePath);
        } else {
            Log.e(TAG, "saveBitmapToThumb bitmap null");
        }
    }

    public static void saveBitmapToThumb(Bitmap bitmap, String fullsizePath)
    {
        try {
            String thumbPath = PhotoFileCreater.getInstance().thumbFilePath(fullsizePath);
            Bitmap thumbBitmap = getThumb(bitmap);
            OutputStream stream = new FileOutputStream(thumbPath);
            thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream);
            stream.flush();
            stream.close();
            Log.d(TAG, "Save thumb as " + thumbPath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String saveByteToAlbum(byte[] data, String fileName, boolean thumb)
    {
        BufferedOutputStream bos;
        try {
            File f = new File(PhotoFileCreater.getInstance().getAlbumDir(thumb), fileName);
            String path = f.getAbsolutePath();
            FileOutputStream fos = new FileOutputStream(path);
            bos = new BufferedOutputStream(fos);
            bos.write(data);
            bos.close();
            return path;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean saveByteToPath(byte[] data, String absPath) {
        Log.d(TAG, "save byte to file " + absPath);
        try {
            FileOutputStream fos = new FileOutputStream(absPath);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            bos.write(data);
            bos.close();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean saveBitmapToPath(Bitmap bitmap, File f) {
        try {
            //save the image to a file
            f.delete();
            OutputStream stream = new FileOutputStream(f.getAbsoluteFile());
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            stream.flush();
            stream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static byte[] readFile(String path)
    {
        File file = new File(path);
        int size = (int) file.length();
        byte[] bytes = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(bytes, 0, bytes.length);
            buf.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    public static Bitmap getThumb(Bitmap bitmap) {
        Float width = new Float(bitmap.getWidth());
        Float height = new Float(bitmap.getHeight());
        Float ratio = height / width;
        bitmap = Bitmap.createScaledBitmap(bitmap, THUMB_WIDTH, (int) (THUMB_WIDTH * ratio), false);
        return bitmap;
    }
}


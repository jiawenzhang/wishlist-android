package com.wish.wishlist.util;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.wish.wishlist.R;
import com.wish.wishlist.image.PhotoFileCreater;

import java.io.File;

/**
 * Created by jiawen on 2016-05-27.
 */
public class ImagePicker {
    private Activity mActivity;
    private File mPhotoFile;

    public static final int TAKE_PICTURE = 100;
    public static final int SELECT_PICTURE = 101;
    public static final int PERMISSIONS_TAKE_PHOTO = 0;

    private static final String TAG="ImagePicker";

    public ImagePicker(Activity activity) {
        mActivity = activity;
    }

    public void start() {
        showChangePhotoDialog();
    }

    protected void showChangePhotoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity, R.style.AppCompatAlertDialogStyle);

        final CharSequence[] items = {"Take a photo", "From gallery"};
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // The 'which' argument contains the index position
                // of the selected item
                if (which == 0) {
                    dispatchTakePictureIntent();
                } else if (which == 1) {
                    dispatchImportPictureIntent();
                }
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void dispatchTakePictureIntent() {
        if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(mActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(mActivity,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSIONS_TAKE_PHOTO);

            return;
        }
        takePhoto();
    }

    public void takePhoto() {
        // prevent screen orientation to re-create activity, in which case we lose the state of ImagePicker
        ScreenOrientation.lock(mActivity);
        Intent intent =  new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // we need to supply EXTRA_OUTPUT for the camera to save a high-quality photo
        mPhotoFile = PhotoFileCreater.getInstance().getTempImageFile();
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mPhotoFile));
        mActivity.startActivityForResult(intent, TAKE_PICTURE);
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Log.e(TAG, "onRequestPermissionsResult");
        Log.e(TAG, "code " + requestCode);
        switch (requestCode) {
            case PERMISSIONS_TAKE_PHOTO: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length == 2 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                    Analytics.send(Analytics.PERMISSION, "TakePhoto", "Grant");
                    takePhoto();
                } else {
                    Log.e(TAG, "Take photo permission denied");
                    Analytics.send(Analytics.PERMISSION, "TakePhoto", "Deny");

                    // Should we show an explanation?
                    if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                        ActivityCompat.shouldShowRequestPermissionRationale(mActivity, Manifest.permission.CAMERA)) {
                        //Show permission explanation dialog...
                        Analytics.send(Analytics.PERMISSION, "TakePhoto", "ExplainDialog");
                        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity, R.style.AppCompatAlertDialogStyle);
                        builder.setMessage("Cannot take photos without the permissions.").setCancelable(
                                false).setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                    }
                                });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    } else {
                        //Never ask again selected, or device policy prohibits the app from having that permission.
                        Analytics.send(Analytics.PERMISSION, "TakePhoto", "NeverAskAgain");
                        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity, R.style.AppCompatAlertDialogStyle);
                        builder.setMessage("To allow taking photos, please go to System Settings -> Apps -> Wishlist -> Permissions and enable Camera and Storage.").setCancelable(
                                false).setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                    }
                                });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                }
            }
        }
    }

    public String getPhotoPath() {
        return mPhotoFile == null? null : mPhotoFile.getAbsolutePath();
    }

    public Uri getPhotoUri() {
        return Uri.fromFile(mPhotoFile);
    }

    private void dispatchImportPictureIntent() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        mActivity.startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE);
    }
}

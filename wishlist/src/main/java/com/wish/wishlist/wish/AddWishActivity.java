package com.wish.wishlist.wish;

import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.path.android.jobqueue.JobManager;
import com.wish.wishlist.R;
import com.wish.wishlist.WishlistApplication;
import com.wish.wishlist.job.GetWishAddressJob;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.util.Analytics;
import com.wish.wishlist.util.PositionManager;
import java.util.Observable;
import java.util.Observer;

public class AddWishActivity extends EditWishActivityBase
        implements Observer {

    private static final String TAG = "AddWishActivity";
    private class GetAddressTask extends AsyncTask<Void, Void, Void> {//<param, progress, result>
        @Override
        protected Void doInBackground(Void... arg) {
            //getCurrentAddStr using geocode, may take a while, need to put this to a separate thread
            mAddStr = mPositionManager.getCurrentAddStr();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (mAddStr.equals("unknown")) {
                Toast.makeText(AddWishActivity.this, "Location unavailable", Toast.LENGTH_LONG).show();
            }
            mLocationEditText.setText(mAddStr);
            mGettingLocation = false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Analytics.sendScreen("AddWish");

        if (loadLocation()) {
            mMapImageButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    //get the location
                    if (!mGettingLocation) {
                        mPositionManager.startLocationUpdates();
                        mGettingLocation = true;
                        mLocationEditText.setText("Loading location...");
                    }
                }
            });

            mPositionManager = new PositionManager();
            mPositionManager.addObserver(this);

            // Get the location in background
            final boolean tagLocation = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("autoLocation", true);
            if (tagLocation) {
                mPositionManager.startLocationUpdates();
                mGettingLocation = true;
                mLocationEditText.setText("Loading location...");
            }
        }

        Intent intent = getIntent();
        //get the mTempPhotoPath, if it is not null, activity is launched from camera
        mTempPhotoPath = intent.getStringExtra(TEMP_PHOTO_PATH);
        setTakenPhoto();

        final boolean wishDefaultPrivate = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("wishDefaultPrivate", false);
        if (wishDefaultPrivate) {
            mPrivateCheckBox.setChecked(true);
        } else {
            mPrivateCheckBox.setChecked(false);
        }
    }

    /***
     * Save user input as a wish item
     */
    protected boolean saveWishItem() {
        if (mSelectedPic && mSelectedPicUri != null) {
            showProgressDialog(getString(R.string.saving_image));
            new saveSelectedPhotoTask().execute();
        } else if (mTempPhotoPath != null) {
            showProgressDialog(getString(R.string.saving_image));
            new saveTempPhoto().execute();
        } else {
            WishItem item = createNewWish();
            mItem_id = item.saveToLocal();
            getWishAddressInBackground(item);
            wishSaved();
        }
        return true;
    }

    @Override
    protected void newImageSaved() {
        super.newImageSaved();
        WishItem item = createNewWish();
        item.setWebImgMeta(null, 0, 0);
        mItem_id = item.saveToLocal();
        getWishAddressInBackground(item);
        wishSaved();
    }

    protected WishItem createNewWish() {
        // create a new item
        WishInput input = wishInput();

        return new WishItem(
                -1,
                "",
                input.mAccess,
                input.mStore,
                input.mName,
                input.mDescription,
                System.currentTimeMillis(),
                null,
                null,
                mFullsizePhotoPath,
                input.mPrice,
                mLat,
                mLng,
                input.mAddress,
                0,
                input.mComplete,
                input.mLink,
                false,
                false);
    }

    protected void getWishAddressInBackground(WishItem item) {
        if (item.getLatitude() != Double.MIN_VALUE && item.getLongitude() != Double.MIN_VALUE && (item.getAddress().equals("unknown") || item.getAddress().equals(""))) {
            JobManager jobManager = ((WishlistApplication) getApplication()).getJobManager();
            jobManager.addJobInBackground(new GetWishAddressJob(item.getId()));
        }
    }

    @Override
    protected boolean navigateBack() {
        //all fields are empty
        if (mNameEditText.getText().toString().length() == 0 &&
                mDescriptionEditText.getText().toString().length() == 0 &&
                mPriceEditText.getText().toString().length() == 0 &&
                mLocationEditText.getText().toString().length() == 0 &&
                mStoreEditText.getText().toString().length() == 0){

            removeTempPhoto();
            setResult(RESULT_CANCELED, null);
            finish();
            return false;
        }

        AlertDialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        builder.setMessage("Discard the wish?").setCancelable(
                false).setPositiveButton("Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        removeTempPhoto();
                        setResult(RESULT_CANCELED, null);
                        AddWishActivity.this.finish();
                    }
                }).setNegativeButton("No",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        dialog = builder.create();
        dialog.show();

        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void update(Observable observable, Object data) {
        // This method is notified after data changes.
        // get the location
        Location location = mPositionManager.getCurrentLocation();
        if (location == null){
            mAddStr = "unknown";
            //need better value to indicate it's not valid lat and lng
            mLat = Double.MIN_VALUE;
            mLng = Double.MIN_VALUE;
            mLocationEditText.setText(mAddStr);
            mGettingLocation = false;
        } else {
            //get current latitude and longitude
            mLat = location.getLatitude();
            mLng = location.getLongitude();
            Log.d(TAG, "execute GetAddressTAsk");
            new GetAddressTask().execute();
        }
    }

    protected boolean loadLocation() {
        return true;
    }
}

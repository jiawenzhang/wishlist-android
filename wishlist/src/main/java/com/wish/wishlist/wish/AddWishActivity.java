package com.wish.wishlist.wish;

import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.path.android.jobqueue.JobManager;
import com.wish.wishlist.R;
import com.wish.wishlist.WishlistApplication;
import com.wish.wishlist.db.TagItemDBManager;
import com.wish.wishlist.event.EventBus;
import com.wish.wishlist.event.MyWishChangeEvent;
import com.wish.wishlist.job.GetWishAddressJob;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.sync.SyncAgent;
import com.wish.wishlist.util.Analytics;
import com.wish.wishlist.util.PositionManager;

import java.io.File;
import java.util.Observable;
import java.util.Observer;

public class AddWishActivity extends MyWishDetailActivity
        implements Observer {

    private static final String TAG = "AddWishActivity";
    protected String mAddStr = "unknown";
    protected PositionManager mPositionManager;
    protected boolean mGettingLocation = false;
    protected double mLat = Double.MIN_VALUE;
    protected double mLng = Double.MIN_VALUE;

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
            mLocationView.setText(mAddStr);
            mGettingLocation = false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Analytics.sendScreen("AddWish");

        if (loadLocation()) {
            //mMapImageButton.setOnClickListener(new OnClickListener() {
            //    @Override
            //    public void onClick(View view) {
            //        //get the location
            //        if (!mGettingLocation) {
            //            mPositionManager.startLocationUpdates();
            //            mGettingLocation = true;
            //            mLocationEditText.setText("Loading location...");
            //        }
            //    }
            //});

            mPositionManager = new PositionManager();
            mPositionManager.addObserver(this);

            // Get the location in background
            final boolean tagLocation = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("autoLocation", true);
            if (tagLocation) {
                mPositionManager.startLocationUpdates();
                mGettingLocation = true;
                mLocationView.setText("Loading location...");
            }
        }

        Intent intent = getIntent();

        //get the mTempPhotoPath, if it is not null, activity is launched from camera
        mTempPhotoPath = intent.getStringExtra(TEMP_PHOTO_PATH);
        if (mTempPhotoPath != null) {
            setTakenPhoto();
            mTxtInstruction.setText(getResources().getString(R.string.tap_here_to_change_photo));
        } else {
            setPhotoVisible(false);
            mTxtInstruction.setText(getResources().getString(R.string.add_photo));
        }

        mInstructionLayout.setVisibility(View.VISIBLE);
        mDescriptionView.setVisibility(View.VISIBLE);
        mLocationView.setVisibility(View.VISIBLE);

        mLinkLayout.setVisibility(View.VISIBLE);
        mLinkView.setVisibility(View.GONE);
        mLinkText.setVisibility(View.VISIBLE);

        mCompleteInnerLayout.setVisibility(View.GONE);
        mCompleteCheckBox.setVisibility(View.VISIBLE);

        mTagLayout.setVisibility(View.VISIBLE);
        mTagView.setVisibility(View.VISIBLE);
        mImageFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showChangePhotoDialog();
            }
        });


        //final boolean wishDefaultPrivate = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("wishDefaultPrivate", false);
        //if (wishDefaultPrivate) {
        //    mPrivateCheckBox.setChecked(true);
        //} else {
        //    mPrivateCheckBox.setChecked(false);
        //}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_add_wish, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.menu_done:
                save();
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    /***
     * Save user input as a wish item
     */
    @Override
    protected void saveWishItem() {
        if (mSelectedPic && mSelectedPicUri != null) {
            showProgressDialog(getString(R.string.saving_image));
            new saveSelectedPhotoTask().execute();
        } else if (mTempPhotoPath != null) {
            showProgressDialog(getString(R.string.saving_image));
            new saveTempPhoto().execute();
        } else {
            mItem = createNewWish();
            mItem.saveToLocal();
            getWishAddressInBackground(mItem);
            wishSaved();
        }
    }

    @Override
    protected void newImageSaved() {
        dismissProgressDialog();
        mItem = createNewWish();
        mItem.setWebImgMeta(null, 0, 0);
        mItem.saveToLocal();
        getWishAddressInBackground(mItem);
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

    @Override
    protected void wishSaved() {
        TagItemDBManager.instance().Update_item_tags(mItem.getId(), mTags);
        EventBus.getInstance().post(new MyWishChangeEvent());
        SyncAgent.getInstance().sync();
        Analytics.send(Analytics.WISH, "Save", "New");
        finish();
    }

    protected void getWishAddressInBackground(WishItem item) {
        if (item.getLatitude() != Double.MIN_VALUE && item.getLongitude() != Double.MIN_VALUE && (item.getAddress().equals("unknown") || item.getAddress().equals(""))) {
            JobManager jobManager = ((WishlistApplication) getApplication()).getJobManager();
            jobManager.addJobInBackground(new GetWishAddressJob(item.getId()));
        }
    }

    protected void removeTempPhoto() {
        if (mTempPhotoPath != null) {
            File f = new File(mTempPhotoPath);
            f.delete();
        }
    }

    /***
     * called when the "return" button is clicked
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            return navigateBack();
        }
        return false;
    }

    protected boolean navigateBack() {
        //all fields are empty
        if (mNameView.getText().toString().length() == 0 &&
                mDescriptionView.getText().toString().length() == 0 &&
                mPriceView.getText().toString().length() == 0 &&
                mLocationView.getText().toString().length() == 0 &&
                mStoreView.getText().toString().length() == 0){

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
            mLocationView.setText(mAddStr);
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

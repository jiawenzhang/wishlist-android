package com.wish.wishlist.wish;

import java.io.File;
import java.util.Observer;
import java.util.Observable;

import com.bumptech.glide.Glide;
import com.wish.wishlist.db.TagItemDBManager;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.model.WishItemManager;
import com.wish.wishlist.image.ImageManager;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.os.AsyncTask;

public class EditWishActivity extends EditWishActivityBase
        implements Observer {

    private static final String TAG = "EditWishActivity";
    public static final String ITEM_ID = "ITEM_ID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMapImageButton.setVisibility(View.GONE);
        mCompleteCheckBox.setVisibility(View.VISIBLE);

        Intent intent = getIntent();
        mItem_id = intent.getLongExtra(ITEM_ID, -1);

        WishItem item = WishItemManager.getInstance().getItemById(mItem_id);
        mComplete = item.getComplete();
        if (mComplete == 1) {
            mCompleteCheckBox.setChecked(true);
        } else {
            mCompleteCheckBox.setChecked(false);
        }

        if (item.getAccess() == WishItem.PRIVATE) {
            mPrivateCheckBox.setChecked(true);
        } else {
            mPrivateCheckBox.setChecked(false);
        }

        mNameEditText.setText(item.getName());
        mDescriptionEditText.setText(item.getDesc());
        String priceStr = item.getPriceAsString();
        if (priceStr != null) {
            mPriceEditText.setText(priceStr);
        }
        mLocationEditText.setText(item.getAddress());
        mLinkEditText.setText(item.getLink());
        mStoreEditText.setText(item.getStoreName());
        mFullsizePhotoPath = item.getFullsizePicPath();
        if (mFullsizePhotoPath != null) {
            // Picasso bug: fit().centerCrop() does not work together when image is large
            // https://github.com/square/picasso/issues/249
            Glide.with(this).load(new File(mFullsizePhotoPath)).fitCenter().into(mImageItem);
            mImageItem.setVisibility(View.VISIBLE);
        }
        mTags = TagItemDBManager.instance().tags_of_item(mItem_id);
    }

    protected boolean saveWishItem(final WishInput input) {
        WishItem item = WishItemManager.getInstance().getItemById(mItem_id);
        if (mSelectedPic && mSelectedPicUri != null) {
            mFullsizePhotoPath = copyPhotoToAlbum(mSelectedPicUri);
            ImageManager.saveBitmapToThumb(mSelectedPicUri, mFullsizePhotoPath, this);
            removeItemImage();
            item.setWebImgMeta(null, 0, 0);
        } else if (mTempPhotoPath != null) {
            if (saveTempPhoto()) {
                removeItemImage();
                item.setWebImgMeta(null, 0, 0);
            } else {
                return false;
            }
        }

        item.setAccess(input.mAccess);
        item.setStoreName(input.mStore);
        item.setName(input.mName);
        item.setDesc(input.mDescription);
        item.setUpdatedTime(System.currentTimeMillis());
        item.setFullsizePicPath(mFullsizePhotoPath);
        item.setPrice(input.mPrice);
        item.setAddress(mAddStr);
        item.setComplete(input.mComplete);
        item.setLink(input.mLink);
        item.setSyncedToServer(false);
        item.saveToLocal();

        return true;
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
        //get the location
        Location location = mPositionManager.getCurrentLocation();
        if (location == null){
            mAddStr = "unknown";
            //need better value to indicate it's not valid lat and lng
            mLat = Double.MIN_VALUE;
            mLng = Double.MIN_VALUE;
            mLocationEditText.setText(mAddStr);
            mGettingLocation = false;
        }
        else {
            //get current latitude and longitude
            mLat = location.getLatitude();
            mLng = location.getLongitude();
            new GetAddressTask().execute("");
        }
    }

    private class GetAddressTask extends AsyncTask<String, Void, String> {//<param, progress, result>
        @Override
        protected String doInBackground(String... arg) {
            //getCuttentAddStr using geocode, may take a while, need to put this to a separate thread
            mAddStr = mPositionManager.getCuttentAddStr();
            return mAddStr;
        }

        @Override
        protected void onPostExecute(String add) {
            if (mAddStr.equals("unknown")) {
                Toast.makeText(EditWishActivity.this, "location not available", Toast.LENGTH_LONG).show();
            }
            mLocationEditText.setText(mAddStr);
            mGettingLocation = false;
        }
    }

    @Override
    protected boolean navigateBack() {
        removeTempPhoto();
        setResult(RESULT_CANCELED, null);
        finish();
        return false;
    }
}

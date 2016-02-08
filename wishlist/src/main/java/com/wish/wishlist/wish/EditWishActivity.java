package com.wish.wishlist.wish;

import java.io.File;

import com.squareup.picasso.Picasso;
import com.wish.wishlist.R;
import com.wish.wishlist.db.TagItemDBManager;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.model.WishItemManager;
import com.wish.wishlist.util.Analytics;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class EditWishActivity extends EditWishActivityBase {
    private static final String TAG = "EditWishActivity";
    public static final String ITEM_ID = "ITEM_ID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Analytics.sendScreen("EditWish");

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

        if (getResources().getBoolean(R.bool.enable_account)) {
            if (item.getAccess() == WishItem.PRIVATE) {
                mPrivateCheckBox.setChecked(true);
            } else {
                mPrivateCheckBox.setChecked(false);
            }
        } else {
            mPrivateCheckBox.setVisibility(View.GONE);
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
            Picasso.with(mImageItem.getContext()).load(new File(mFullsizePhotoPath)).fit().centerCrop().into(mImageItem);
            mImageItem.setVisibility(View.VISIBLE);
        }
        mTags = TagItemDBManager.instance().tags_of_item(mItem_id);
    }

    protected boolean saveWishItem() {
        if (mSelectedPic && mSelectedPicUri != null) {
            showProgressDialog(getString(R.string.saving_image));
            new saveSelectedPhotoTask().execute();
        } else if (mTempPhotoPath != null) {
            showProgressDialog(getString(R.string.saving_image));
            new saveTempPhoto().execute();
        } else {
            WishItem item = populateItem();
            item.saveToLocal();
            wishSaved();
        }
        return true;
    }

    protected WishItem populateItem() {
        WishItem item = WishItemManager.getInstance().getItemById(mItem_id);
        WishInput input = wishInput();
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

        return item;
    }

    protected void newImageSaved() {
        super.newImageSaved();
        removeItemImage();
        WishItem item = populateItem();
        item.setWebImgMeta(null, 0, 0);
        item.saveToLocal();
        wishSaved();
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
    protected boolean navigateBack() {
        removeTempPhoto();
        setResult(RESULT_CANCELED, null);
        finish();
        return false;
    }
}

package com.wish.wishlist.wish;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;

import com.tokenautocomplete.TokenCompleteTextView;
import com.wish.wishlist.R;
import com.wish.wishlist.db.TagItemDBManager;
import com.wish.wishlist.util.Analytics;

public class ExistingWishDetailActivity extends MyWishDetailActivity implements TokenCompleteTextView.TokenListener {
    private static final String TAG = "ExistingWishDetail";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Analytics.sendScreen("ExistingWishDetail");

        showItemInfo();
        mTags = TagItemDBManager.instance().tags_of_item(mItem.getId());
        addTags();

        mFullsizePhotoPath = mItem.getFullsizePicPath();
        mLinkText.setText(mItem.getLink());
        mComplete = mItem.getComplete();
        if (mComplete == 1) {
            mCompleteCheckBox.setChecked(true);
        } else {
            mCompleteCheckBox.setChecked(false);
        }

        mNameView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    enterEditMode();
                    Log.d(TAG, "has focus");
                } else {
                    Log.d(TAG, "lost focus");
                }
            }
        });

        mDescriptionView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    enterEditMode();
                    Log.d(TAG, "has focus");
                } else {
                    Log.d(TAG, "lost focus");
                }
            }
        });

        mPriceView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    enterEditMode();
                    Log.d(TAG, "has focus");
                } else {
                    Log.d(TAG, "lost focus");
                }
            }
        });

        mLocationView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    enterEditMode();
                    Log.d(TAG, "has focus");
                } else {
                    Log.d(TAG, "lost focus");
                }
            }
        });

        mStoreView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    enterEditMode();
                    Log.d(TAG, "has focus");
                } else {
                    Log.d(TAG, "lost focus");
                }
            }
        });

        mLinkLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLinkText.setVisibility(View.VISIBLE);
                mLinkText.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(mLinkView, InputMethodManager.SHOW_FORCED);
                enterEditMode();
            }
        });

        LinearLayout completeLayout = (LinearLayout) findViewById(R.id.itemCompleteLayout);
        completeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enterEditMode();
            }
        });
    }

    @Override
    protected void newImageSaved() {
        dismissProgressDialog();
        removeItemImage();
        mItem = populateItem();
        mItem.setWebImgMeta(null, 0, 0);
        mItem.saveToLocal();
        wishSaved();
    }
}

package com.wish.wishlist.wish;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;

import com.tokenautocomplete.TokenCompleteTextView;
import com.wish.wishlist.R;
import com.wish.wishlist.activity.FullscreenPhotoActivity;
import com.wish.wishlist.db.TagItemDBManager;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.model.WishItemManager;
import com.wish.wishlist.util.Analytics;

public class ExistingWishDetailActivity extends MyWishDetailActivity implements TokenCompleteTextView.TokenListener {
    private static final String TAG = "ExistingWishDetail";
    public final static String ITEM_ID = "ITEM_ID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Analytics.sendScreen("ExistingWishDetail");

        Intent i = getIntent();
        long item_id = i.getLongExtra(ITEM_ID, -1);
        mItem = WishItemManager.getInstance().getItemById(item_id);

        showItemInfo();

        mTags = TagItemDBManager.instance().tags_of_item(mItem.getId());
        addTags();

        mFullsizePhotoPath = mItem.getFullsizePicPath();
        mLinkText.setText(mItem.getLink());
        int complete = mItem.getComplete();
        if (complete == 1) {
            mCompleteCheckBox.setChecked(true);
        } else {
            mCompleteCheckBox.setChecked(false);
        }

        int access = mItem.getAccess();
        if (access == WishItem.PRIVATE) {
            mPrivateCheckBox.setChecked(true);
        } else {
            mPrivateCheckBox.setChecked(false);
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

        final LinearLayout completeLayout = (LinearLayout) findViewById(R.id.itemCompleteLayout);
        completeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enterEditMode();
                mCompleteCheckBox.getParent().requestChildFocus(mCompleteCheckBox, mCompleteCheckBox);
            }
        });

        mTextComplete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enterEditMode();
                mCompleteCheckBox.getParent().requestChildFocus(mCompleteCheckBox, mCompleteCheckBox);
            }
        });

        mItemPrivateLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enterEditMode();
                mPrivateCheckBox.getParent().requestChildFocus(mPrivateCheckBox, mPrivateCheckBox);
            }
        });

        mTextPrivate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enterEditMode();
                mPrivateCheckBox.getParent().requestChildFocus(mPrivateCheckBox, mPrivateCheckBox);
            }
        });
    }

    protected void enterEditMode() {
        if (mActionMode != null) {
            Log.d(TAG, "ActionMode is already on");
            return;
        }

        Analytics.send(Analytics.WISH, "EnterEdit", null);

        mInstructionLayout.setVisibility(View.VISIBLE);
        mDescriptionView.setVisibility(View.VISIBLE);
        mPriceView.setVisibility(View.VISIBLE);

        // price is shown with currency and number format, remove the currency and format for editing mode
        if (mItem != null) {
            String priceStr = mItem.getPriceString();
            if (priceStr != null) {
                mPriceView.setText(priceStr);
            }
        }

        mStoreView.setVisibility(View.VISIBLE);
        mLocationView.setVisibility(View.VISIBLE);

        mLinkLayout.setVisibility(View.VISIBLE);
        mLinkText.setVisibility(View.VISIBLE);

        if (mItem != null) {
            String link = mItem.getLink();
            if (link != null && !link.isEmpty()) {
                mLinkText.setText(link);
            }
        }

        mCompleteInnerLayout.setVisibility(View.GONE);
        mCompleteCheckBox.setVisibility(View.VISIBLE);
        mCompleteCheckBox.setChecked(mItem != null && mItem.getComplete() == 1);

        mPrivateInnerLayout.setVisibility(View.GONE);
        mPrivateCheckBox.setVisibility(View.VISIBLE);
        mPrivateCheckBox.setChecked(mItem != null && mItem.getAccess() == WishItem.PRIVATE);

        mTagLayout.setVisibility(mTags.size() == 0 ? View.VISIBLE : View.GONE);
        mImageFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startImagePicker();
            }
        });

        if (mTempPhotoPath == null && mSelectedPicUri == null && mItem.getFullsizePicPath() == null) {
            mTxtInstruction.setText(getResources().getString(R.string.add_photo));
        } else {
            mTxtInstruction.setText(getResources().getString(R.string.tap_here_to_change_photo));
        }

        mScrollView.setPadding(mScrollView.getPaddingLeft(), toolBarHeight(), mScrollView.getPaddingRight(), mScrollView.getPaddingBottom());

        mActionMode = startSupportActionMode(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.menu_my_wish_detail_action, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_done:
                        if (save()) {
                            mEditDone = true;
                        }
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                mActionMode = null;
                showItemInfo();

                if (!mEditDone) {
                    // user canceled editing, clear the photos that were taken/selected
                    clearPhotoState();
                }
                mEditDone = false;

                clearFocus();

                mInstructionLayout.setVisibility(View.GONE);
                mLinkText.setVisibility(View.GONE);
                mTagLayout.setVisibility(View.GONE);
                mCompleteCheckBox.setVisibility(View.GONE);
                mCompleteInnerLayout.setVisibility(mItem != null && mItem.getComplete() == 1 ? View.VISIBLE : View.GONE);
                mPrivateCheckBox.setVisibility(View.GONE);
                mPrivateInnerLayout.setVisibility(mItem != null && mItem.getAccess() == WishItem.PRIVATE ? View.VISIBLE : View.GONE);

                mImageFrame.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String fullsize_picture_str = mItem.getFullsizePicPath();
                        if (fullsize_picture_str != null) {
                            showFullScreenPhoto(FullscreenPhotoActivity.PHOTO_PATH, fullsize_picture_str);
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void newImageSaved() {
        dismissProgressDialog();
        removeItemImage();

        WishItem newItem = populateItem();
        if (newItem == null) {
            mItem.setFullsizePicPath(mFullsizePhotoPath);
        } else {
            mItem = newItem;
        }
        mItem.setImgMetaArray(null);
        mItem.saveToLocal();
        wishSaved();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (mTempPhotoPath == null && mSelectedPicUri == null && mItem.getFullsizePicPath() == null) {
            mTxtInstruction.setText(getResources().getString(R.string.add_photo));
        } else {
            mTxtInstruction.setText(getResources().getString(R.string.tap_here_to_change_photo));
        }
    }
}

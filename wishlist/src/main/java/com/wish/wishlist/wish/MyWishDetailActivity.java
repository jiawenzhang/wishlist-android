package com.wish.wishlist.wish;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.util.Patterns;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.StringSignature;
import com.squareup.picasso.Picasso;
import com.tokenautocomplete.TokenCompleteTextView;
import com.wish.wishlist.R;
import com.wish.wishlist.activity.FullscreenPhotoActivity;
import com.wish.wishlist.db.TagItemDBManager;
import com.wish.wishlist.event.EventBus;
import com.wish.wishlist.event.MyWishChangeEvent;
import com.wish.wishlist.image.CameraManager;
import com.wish.wishlist.image.ImageManager;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.model.WishItemManager;
import com.wish.wishlist.sync.SyncAgent;
import com.wish.wishlist.tag.AddTagActivity;
import com.wish.wishlist.tag.AddTagFromEditActivity;
import com.wish.wishlist.util.Analytics;
import com.wish.wishlist.util.StringUtil;
import com.wish.wishlist.util.dimension;
import com.wish.wishlist.widgets.ClearableEditText;

import java.io.File;
import java.util.ArrayList;

import me.kaede.tagview.Tag;
import me.kaede.tagview.TagView;

/***
 * MyWishDetailActivity displays the detailed info. of an item.
 * It also handles the left/right swipe gesture form user, which correspond to
 * navigating to the previous and next item, respectively.
 *
 * the order of the items during swiping is the order of the items displayed in
 * the MyWishActivity activity
 */

public class MyWishDetailActivity extends WishDetailActivity implements TokenCompleteTextView.TokenListener {
    private static final String TAG = "MyWishDetailActivity";
    protected LinearLayout mInstructionLayout;
    protected ClearableEditText mLinkText;
    protected TagView mTagView;
    protected View mImageFrame;
    protected TextView mTxtInstruction;
    protected LinearLayout mTagLayout;
    protected CheckBox mCompleteCheckBox;
    protected ActionMode mActionMode;
    protected ArrayList<String> mTags = new ArrayList<>();
    protected String mFullsizePhotoPath = null;
    protected String mTempPhotoPath = null;
    protected Uri mSelectedPicUri = null;
    protected Boolean mSelectedPic = false;
    protected Boolean mEditDone = false;
    protected static final int TAKE_PICTURE = 1;
    protected static final int SELECT_PICTURE = 2;
    protected static final int ADD_TAG = 3;

    protected static final String TEMP_PHOTO_PATH = "TEMP_PHOTO_PATH";
    protected static final String SELECTED_PIC_URL = "SELECTED_PIC_URL";
    private ProgressDialog mProgressDialog;
    protected class saveTempPhoto extends AsyncTask<Void, Void, Void> {//<param, progress, result>
        @Override
        protected Void doInBackground(Void... arg) {
            final Bitmap bitmap = ImageManager.decodeSampledBitmapFromFile(mTempPhotoPath, 1024);
            mFullsizePhotoPath = ImageManager.saveBitmapToAlbum(bitmap);
            ImageManager.saveBitmapToThumb(bitmap, mFullsizePhotoPath);
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            newImageSaved();
        }
    }

    protected class saveSelectedPhotoTask extends AsyncTask<Void, Void, Void> {//<param, progress, result>
        @Override
        protected Void doInBackground(Void... arg) {
            final Bitmap bitmap = ImageManager.decodeSampledBitmapFromUri(mSelectedPicUri, 1024);
            mFullsizePhotoPath = ImageManager.saveBitmapToAlbum(bitmap);
            ImageManager.saveBitmapToThumb(bitmap, mFullsizePhotoPath);
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            newImageSaved();
        }
    }

    protected class WishInput {
        WishInput(
                String name,
                String description,
                String store,
                String address,
                double price,
                int complete,
                int access,
                String link) {
            mName = name;
            mDescription = description;
            mPrice = price;
            mComplete = complete;
            mAccess = access;
            mStore = store;
            mAddress = address;
            mLink = link;
        }

        String mName;
        String mDescription;
        String mStore;
        String mAddress;
        double mPrice;
        int mComplete;
        int mAccess;
        String mLink;
    }

    // workaround to avoid Picasso bug: fit().centerCrop() does not work together when image is large
    // https://github.com/square/picasso/issues/249
    // Picasso.with(mPhotoView.getContext()).load(new File(fullsize_picture_str)).fit().centerCrop().into(mPhotoView);
    private class GetBitmapTask extends AsyncTask<String, Void, Bitmap> {//<param, progress, result>
        @Override
        protected Bitmap doInBackground(String... arg) {
            Bitmap bitmap = null;
            String fullsize_picture_str = mItem.getFullsizePicPath();
            if (fullsize_picture_str != null) {
                File f = new File(fullsize_picture_str);
                if (f.exists()) {
                    // First decode with inJustDecodeBounds=true to check dimensions
                    final BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(fullsize_picture_str, options);
                    final float ratio = (float) options.outHeight / (float) options.outWidth;

                    int width = dimension.screenWidth();
                    int height = (int) (width * ratio);

                    bitmap = ImageManager.getInstance().decodeSampledBitmapFromFile(fullsize_picture_str, width, height, false);
                }
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap == null) {
                mPhotoView.setVisibility(View.GONE);
            } else {
                mPhotoView.setVisibility(View.VISIBLE);
                mPhotoView.setImageBitmap(bitmap);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Analytics.sendScreen("MyWishDetail");

        mTxtInstruction = (TextView) findViewById(R.id.txtInstruction);
        mInstructionLayout = (LinearLayout) findViewById(R.id.instructionLayout);
        mTagLayout = (LinearLayout) findViewById(R.id.tagLayout);
        mLinkText = (ClearableEditText) findViewById(R.id.itemLinkText);
        mLinkText.setVisibility(View.GONE);
        mCompleteCheckBox = (CheckBox) findViewById(R.id.completeCheckBox);

        mTagView = (TagView) findViewById(R.id.tag_view);
        mImageFrame = findViewById(R.id.imagePhotoDetailFrame);

        if (savedInstanceState != null) {
            // on screen orientation, reload the item from db as it could have been changed
            if (mItem != null) {
                mItem = WishItemManager.getInstance().getItemById(mItem.getId());
            }
        }

        mImageFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFullScreenPhoto();
            }
        });

        mImageFrame.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return false;
            }
        });

        mTagView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "tag click");
                startAddTagIntent();
            }
        });

        mTagLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAddTagIntent();
            }
        });
    }

    protected void showFullScreenPhoto() {
        final String fullsize_picture_str = mItem.getFullsizePicPath();
        if (fullsize_picture_str != null) {
            Intent i = new Intent(MyWishDetailActivity.this, FullscreenPhotoActivity.class);
            i.putExtra(FullscreenPhotoActivity.PHOTO_PATH, fullsize_picture_str);
            startActivity(i);
        }
    }

    private void dispatchTakePictureIntent() {
        CameraManager c = new CameraManager();
        mTempPhotoPath = c.getPhotoPath();
        startActivityForResult(c.getCameraIntent(), TAKE_PICTURE);
    }

    private void dispatchImportPictureIntent() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE);
    }

    protected Boolean setTakenPhoto() {
        if (mTempPhotoPath == null) {
            return false;
        }
        Log.d(TAG, "setTakePhoto " + mTempPhotoPath);
        File tempPhotoFile = new File(mTempPhotoPath);
        mPhotoView.setVisibility(View.VISIBLE);

        // Picasso bug: fit() does not work when image is large
        // https://github.com/square/picasso/issues/249
        // Picasso.with(this).invalidate(tempPhotoFile);
        // Picasso.with(this).load(tempPhotoFile).fit.into(mImageItem);

        // Because we save the taken photo to the same file, use StringSignature here to avoid loading image from cache
        Glide.with(this).load(tempPhotoFile).fitCenter().signature(new StringSignature(String.valueOf(tempPhotoFile.lastModified()))).into(mPhotoView);
        mSelectedPicUri = null;
        mSelectedPic = false;
        return true;
    }

    protected Boolean setSelectedPic() {
        if (mSelectedPicUri == null) {
            return false;
        }
        Log.d(TAG, "setSelectedPic " + mSelectedPicUri.toString());
        mImageFrame.setVisibility(View.VISIBLE);
        mPhotoView.setVisibility(View.VISIBLE);
        // Picasso bug: fit() does not work when image is large
        // https://github.com/square/picasso/issues/249
        Glide.with(this).load(mSelectedPicUri).fitCenter().into(mPhotoView);
        mTempPhotoPath = null;
        mSelectedPic = true;
        return true;
    }

    private void startAddTagIntent() {
        Intent i = new Intent(MyWishDetailActivity.this, AddTagFromEditActivity.class);
        long[] ids = new long[1];
        if (mItem != null) {
            ids[0] = mItem.getId();
            mTags = TagItemDBManager.instance().tags_of_item(mItem.getId());
        }
        i.putExtra(AddTagActivity.ITEM_ID_ARRAY, (ids));
        i.putExtra(AddTagFromEditActivity.TAGS, mTags);
        startActivityForResult(i, ADD_TAG);
    }

    protected void showChangePhotoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MyWishDetailActivity.this, R.style.AppCompatAlertDialogStyle);

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

    protected void clearFocus() {
        // Check if any view has focus, clear its focus and close keyboard
        View view = getCurrentFocus();
        if (view != null) {
            view.clearFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    protected WishInput wishInput() {
        String name = mNameView.getText().toString().trim();
        String description = mDescriptionView.getText().toString().trim();
        String store = mStoreView.getText().toString().trim();
        String link = mLinkText.getText().toString().trim();

        String address = mLocationView.getText().toString().trim();
        if (address.equals("Loading location...")) {
            address = "unknown";
        }

        String priceString = mPriceView.getText().toString().trim();
        double price = priceString.isEmpty() ? Double.MIN_VALUE : Double.valueOf(mPriceView.getText().toString().trim());
        int complete = mCompleteCheckBox.isChecked() ? 1 : 0;
        //int access = mPrivateCheckBox.isChecked() ? WishItem.PRIVATE : WishItem.PUBLIC;
        int access = WishItem.PUBLIC;

        return new WishInput(
                name,
                description,
                store,
                address,
                price,
                complete,
                access,
                link);
    }

    protected void showProgressDialog(final String text) {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(text);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    protected void dismissProgressDialog() {
        mProgressDialog.dismiss();
    }

    protected WishItem populateItem() {
        WishItem item = WishItemManager.getInstance().getItemById(mItem.getId());
        WishInput input = wishInput();
        item.setAccess(input.mAccess);
        item.setStoreName(input.mStore);
        item.setName(input.mName);
        item.setDesc(input.mDescription);
        item.setUpdatedTime(System.currentTimeMillis());
        item.setFullsizePicPath(mFullsizePhotoPath);
        item.setPrice(input.mPrice);
        item.setAddress(input.mAddress);
        item.setComplete(input.mComplete);
        item.setLink(input.mLink);
        item.setSyncedToServer(false);

        return item;
    }

    protected boolean validateInput() {
        if (mNameView.getText().toString().trim().length() == 0) {
            showErrorToast("Please give a name to your wish");
            return false;
        }

        String link = mLinkText.getText().toString().trim();
        if (!link.isEmpty() && !Patterns.WEB_URL.matcher(link).matches()) {
            showErrorToast("Link invalid");
            return false;
        }

        String priceString = mPriceView.getText().toString().trim();
        if (priceString.isEmpty()) {
            return true;
        }
        try {
            double price = Double.valueOf(priceString);
        } catch (NumberFormatException e) {
            // need some error message here
            // price format incorrect
            Log.e(TAG, e.toString());
            showErrorToast("Price invalid");
            return false;
        }
        return true;
    }

    private void showErrorToast(String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
        toast.setGravity(Gravity.TOP | Gravity.CENTER, 0, screenHeight/4);
        toast.show();
    }

    protected void saveWishItem() {
        if (mSelectedPic && mSelectedPicUri != null) {
            showProgressDialog(getString(R.string.saving_image));
            new saveSelectedPhotoTask().execute();
        } else if (mTempPhotoPath != null) {
            showProgressDialog(getString(R.string.saving_image));
            new saveTempPhoto().execute();
        } else {
            mItem = populateItem();
            mItem.saveToLocal();
            wishSaved();
        }
    }

    protected void newImageSaved() {

    }

    protected void removeItemImage() {
        if (mItem != null) {
            mItem.removeImage();
        }
    }

    protected void wishSaved() {
        EventBus.getInstance().post(new MyWishChangeEvent());

        SyncAgent.getInstance().sync();

        Analytics.send(Analytics.WISH, "Save", "Existing");

        clearPhotoState();
        mActionMode.finish();
    }

    protected void clearPhotoState() {
        mSelectedPic = false;
        mSelectedPicUri = null;
        mTempPhotoPath = null;
    }

    protected boolean save() {
        if (!validateInput()) {
            Log.e(TAG, "error in input");
            return false;
        }

        saveWishItem();
        return true;
    }

    @Override
    protected void showItemInfo() {
        super.showItemInfo();
        final ImageView privateImage = (ImageView) findViewById(R.id.imgPrivate);

        if (getResources().getBoolean(R.bool.enable_account)) {
            if (mItem.getAccess() == mItem.PRIVATE) {
                privateImage.setVisibility(View.VISIBLE);
            } else {
                privateImage.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void showPhoto() {
        if (mItem == null) {
            return;
        }
        if (mItem.getFullsizePicPath() != null) {
            Picasso.with(mPhotoView.getContext()).load(new File(mItem.getFullsizePicPath())).fit().centerCrop().into(mPhotoView);
            mPhotoView.setVisibility(View.VISIBLE);
        } else {
            mPhotoView.setVisibility(View.GONE);
        }
    }

    void addTags() {
        //ArrayList<String> tags = TagItemDBManager.instance().tags_of_item(mItem.getId());
        mTagView.removeAllTags();
        for (String tagTxt : mTags) {
            Tag tag = new Tag(tagTxt);
            tag.layoutColor = ContextCompat.getColor(this, R.color.wishlist_yellow_color);
            mTagView.addTag(tag);
        }
    }

    private void deleteItem() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        builder.setMessage("Delete the wish?").setCancelable(
                false).setPositiveButton("Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Analytics.send(Analytics.WISH, "Delete", null);
                        WishItemManager.getInstance().deleteItemById(mItem.getId());
                        EventBus.getInstance().post(new MyWishChangeEvent());

                        Intent intent = new Intent();
                        intent.putExtra("id", mItem.getId());
                        setResult(Activity.RESULT_OK, intent);
                        MyWishDetailActivity.this.finish();
                    }
                }).setNegativeButton("No",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void editItem() {
        mNameView.requestFocus();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        inflateMenu(R.menu.menu_my_wish_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.menu_item_detail_edit:
                editItem();
                return true;
            case R.id.menu_item_detail_share:
                shareItem();
                return true;
            case R.id.location:
                showOnMap();
                return true;
            case R.id.menu_item_detail_delete:
                deleteItem();
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case TAKE_PICTURE: {
                if (resultCode == RESULT_OK) {
                    Analytics.send(Analytics.WISH, "TakenPicture", "FromEditItemCameraButton");

                    setTakenPhoto();
                    mTxtInstruction.setText(getResources().getString(R.string.tap_here_to_change_photo));
                } else {
                    Log.d(TAG, "cancel taking photo");
                    mTempPhotoPath = null;
                }
                break;
            }
            case SELECT_PICTURE: {
                if (resultCode == RESULT_OK) {
                    mSelectedPicUri = data.getData();
                    Analytics.send(Analytics.WISH, "SelectedPicture", null);
                    setSelectedPic();
                    mTxtInstruction.setText(getResources().getString(R.string.tap_here_to_change_photo));
                }
                break;
            }
            case ADD_TAG: {
                clearFocus();
                if (resultCode == RESULT_OK) {
                    ArrayList<String> tags = data.getStringArrayListExtra(AddTagFromEditActivity.TAGS);
                    if (!StringUtil.sameArrays(tags, mTags)) {
                        Log.d(TAG, "Save tags");
                        mTags = tags;
                        addTags();
                        mTagLayout.setVisibility(mTags.size() == 0 && mActionMode != null ? View.VISIBLE : View.GONE);

                        //save the tags of this item
                        if (mItem != null) {
                            TagItemDBManager.instance().Update_item_tags(mItem.getId(), mTags);
                            mItem.setUpdatedTime(System.currentTimeMillis());
                            mItem.setSyncedToServer(false);
                            mItem.saveToLocal();

                            EventBus.getInstance().post(new MyWishChangeEvent());
                            SyncAgent.getInstance().sync();
                        }
                    } else {
                        Log.d(TAG, "Tags have not been changed");
                    }
                }
            }
        } //end of switch
    }

    @Override
    public void onTokenAdded(Object token) {}

    @Override
    public void onTokenRemoved(Object token) {}

    @Override
    protected boolean myWish() { return true; }

    //this will make the photo taken before to show up if user cancels taking a second photo
    //this will also save the thumbnail on switching screen orientation
    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        Log.d(TAG, "onSaveInstanceState");
        if (mTempPhotoPath != null) {
            savedInstanceState.putString(TEMP_PHOTO_PATH, mTempPhotoPath);
        }
        if (mSelectedPicUri != null) {
            savedInstanceState.putString(SELECTED_PIC_URL, mSelectedPicUri.toString());
        }

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // restore the current selected item in the list
        if (savedInstanceState != null) {
            mTempPhotoPath = savedInstanceState.getString(TEMP_PHOTO_PATH);
            if (savedInstanceState.getString(SELECTED_PIC_URL) != null) {
                mSelectedPicUri = Uri.parse(savedInstanceState.getString(SELECTED_PIC_URL));
            }
            if (mTempPhotoPath != null) {
                setTakenPhoto();
            } else if (mSelectedPicUri != null) {
                setSelectedPic();
            }
        }
    }
}

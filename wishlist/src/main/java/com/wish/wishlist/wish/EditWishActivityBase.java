package com.wish.wishlist.wish;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.Patterns;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.StringSignature;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.wish.wishlist.R;
import com.wish.wishlist.WishlistApplication;
import com.wish.wishlist.activity.ActivityBase;
import com.wish.wishlist.activity.FullscreenPhotoActivity;
import com.wish.wishlist.db.TagItemDBManager;
import com.wish.wishlist.event.EventBus;
import com.wish.wishlist.event.MyWishChangeEvent;
import com.wish.wishlist.image.CameraManager;
import com.wish.wishlist.image.ImageManager;
import com.wish.wishlist.image.PhotoFileCreater;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.model.WishItemManager;
import com.wish.wishlist.sync.SyncAgent;
import com.wish.wishlist.tag.AddTagActivity;
import com.wish.wishlist.tag.AddTagFromEditActivity;
import com.wish.wishlist.util.PositionManager;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public abstract class EditWishActivityBase extends ActivityBase {
    protected EditText mNameEditText;
    protected EditText mDescriptionEditText;
    protected EditText mPriceEditText;
    protected EditText mStoreEditText;
    protected EditText mLocationEditText;
    protected EditText mLinkEditText;
    protected CheckBox mCompleteCheckBox;
    protected ImageView mCompleteImageView;
    protected CheckBox mPrivateCheckBox;
    protected ImageView mPrivateImageView;

    protected ImageButton mMapImageButton;
    protected ImageButton mCameraImageButton;
    protected ImageView mImageItem;
    protected double mLat = Double.MIN_VALUE;
    protected double mLng = Double.MIN_VALUE;
    protected String mAddStr = "unknown";
    protected Uri mSelectedPicUri = null;
    protected String mFullsizePhotoPath = null;
    protected String mTempPhotoPath = null;
    protected PositionManager mPositionManager;
    protected long mItem_id = -1;
    protected int mComplete = -1;
    protected boolean mGettingLocation = false;
    protected ArrayList<String> mTags = new ArrayList<>();

    protected static final int TAKE_PICTURE = 1;
    protected static final int SELECT_PICTURE = 2;
    protected static final int ADD_TAG = 3;
    protected Boolean mSelectedPic = false;

    protected static final String TEMP_PHOTO_PATH = "TEMP_PHOTO_PATH";
    protected static final String SELECTED_PIC_URL = "SELECTED_PIC_URL";

    private static final String TAG = "EditWishActivityBase";

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_item);
        setupActionBar(R.id.edit_item_toolbar);

        mMapImageButton = (ImageButton) findViewById(R.id.imageButton_map);
        mNameEditText = (EditText) findViewById(R.id.itemname);
        mDescriptionEditText = (EditText) findViewById(R.id.note);
        mPriceEditText = (EditText) findViewById(R.id.price);
        mStoreEditText = (EditText) findViewById(R.id.store);
        mLocationEditText = (EditText) findViewById(R.id.location);
        mLinkEditText = (EditText) findViewById(R.id.link);

        mCompleteImageView = (ImageView) findViewById(R.id.completeImageView);
        mCompleteCheckBox = (CheckBox) findViewById(R.id.completeCheckBox);
        mCompleteCheckBox.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            mCompleteImageView.setVisibility(View.VISIBLE);
                        } else {
                            mCompleteImageView.setVisibility(View.GONE);
                        }
                    }
                }
        );

        mPrivateImageView = (ImageView) findViewById(R.id.privateImageView);
        mPrivateCheckBox = (CheckBox) findViewById(R.id.privateCheckBox);
        mPrivateCheckBox.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            mPrivateImageView.setVisibility(View.VISIBLE);
                        } else {
                            mPrivateImageView.setVisibility(View.GONE);
                        }
                    }
                }
        );

        ImageButton tagImageButton = (ImageButton) findViewById(R.id.imageButton_tag);
        tagImageButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(EditWishActivityBase.this, AddTagFromEditActivity.class);
                long[] ids = new long[1];
                ids[0] = mItem_id;
                i.putExtra(AddTagActivity.ITEM_ID_ARRAY, (ids));
                i.putExtra(AddTagFromEditActivity.TAGS, mTags);
                startActivityForResult(i, ADD_TAG);
            }
        });

        final View imageFrame = findViewById(R.id.image_photo_frame);
        imageFrame.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(EditWishActivityBase.this, FullscreenPhotoActivity.class);
                if (mTempPhotoPath != null) {
                    i.putExtra(FullscreenPhotoActivity.PHOTO_PATH, mTempPhotoPath);
                } else if (mSelectedPicUri != null) {
                    i.putExtra(FullscreenPhotoActivity.PHOTO_URI, mSelectedPicUri.toString());
                } else if (mFullsizePhotoPath != null) {
                    i.putExtra(FullscreenPhotoActivity.PHOTO_PATH, mFullsizePhotoPath);
                }
                startActivity(i);
            }
        });

        Intent intent = getIntent();
        //get the mTempPhotoPath, if it is not null, EdiItemInfo is launched from camera
        mTempPhotoPath = intent.getStringExtra(TEMP_PHOTO_PATH);
        setTakenPhoto();
        if (intent.getStringExtra(SELECTED_PIC_URL) != null) {
            mSelectedPicUri = Uri.parse(intent.getStringExtra(SELECTED_PIC_URL));
            setSelectedPic();
        }

        mMapImageButton.setVisibility(View.GONE);
        mCompleteCheckBox.setVisibility(View.VISIBLE);

        mImageItem = (ImageView) findViewById(R.id.image_photo);
        mTags = TagItemDBManager.instance().tags_of_item(mItem_id);

        mCameraImageButton = (ImageButton) findViewById(R.id.imageButton_camera);
        mCameraImageButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(EditWishActivityBase.this, R.style.AppCompatAlertDialogStyle);
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
        });

        //set the keyListener for the Item Name EditText
        //this is needed to show cancel button on the right
        mNameEditText.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                        mNameEditText.setSelected(false);
                }
                return false;
            }
        });

        //set the keyListener for the Item Description EditText
        mDescriptionEditText.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                        mDescriptionEditText.setSelected(false);
                }
                return false;
            }
        });

        //set the keyListener for the Item Price EditText
        mPriceEditText.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                        mPriceEditText.setSelected(false);
                }
                return false;
            }
        });

        //set the keyListener for the Item Location EditText
        mLocationEditText.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                        mLocationEditText.setSelected(false);
                }
                return false;
            }
        });

        // We are restoring an instance, for example after screen orientation
        if (savedInstanceState != null) {
            // restore the current selected item in the list
            mTempPhotoPath = savedInstanceState.getString(TEMP_PHOTO_PATH);
            if (intent.getStringExtra(SELECTED_PIC_URL) != null) {
                mSelectedPicUri = Uri.parse(intent.getStringExtra(SELECTED_PIC_URL));
                // Picasso bug: fit() does not work when image is large
                // https://github.com/square/picasso/issues/249
                Glide.with(this).load(mSelectedPicUri).fitCenter().into(mImageItem);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_actionbar_edititeminfo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            navigateBack();
            return true;
        }
        else if (id == R.id.menu_done) {
            //this replaced the saveImageButton used in GingerBread
            // app icon save in action bar clicked;
            save();
            return true;
        }
        else {
            return super.onOptionsItemSelected(item);
        }
    }

    protected void removeItemImage() {
        WishItem item = WishItemManager.getInstance().getItemById(mItem_id);
        item.removeImage();
    }

    private void showErrorToast(String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
        toast.setGravity(Gravity.TOP | Gravity.CENTER, 0, screenHeight/4);
        toast.show();
    }

    protected boolean validateInput() {
        if (mNameEditText.getText().toString().trim().length() == 0) {
            showErrorToast("Please give a name to your wish");
            return false;
        }

        String link = mLinkEditText.getText().toString().trim();
        if (!link.isEmpty() && !Patterns.WEB_URL.matcher(link).matches()) {
            showErrorToast("Link invalid");
            return false;
        }

        String priceString = mPriceEditText.getText().toString().trim();
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

    protected WishInput wishInput() {
        String name = mNameEditText.getText().toString().trim();
        String description = mDescriptionEditText.getText().toString().trim();
        String store = mStoreEditText.getText().toString().trim();
        String link = mLinkEditText.getText().toString().trim();

        String address = mLocationEditText.getText().toString().trim();
        if (address.equals("Loading location...")) {
            address = "unknown";
        }

        String priceString = mPriceEditText.getText().toString().trim();
        double price = priceString.isEmpty() ? Double.MIN_VALUE : Double.valueOf(mPriceEditText.getText().toString().trim());
        int complete = mCompleteCheckBox.isChecked() ? 1 : 0;
        int access = mPrivateCheckBox.isChecked() ? WishItem.PRIVATE : WishItem.PUBLIC;

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

    protected boolean saveTempPhoto() {
        File f;
        try {
            f = PhotoFileCreater.getInstance().setupPhotoFile(false);
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            return false;
        }
        File tempPhotoFile = new File(mTempPhotoPath);
        if (tempPhotoFile.renameTo(f)) {
            mFullsizePhotoPath = f.getAbsolutePath();
            ImageManager.saveBitmapToThumb(mFullsizePhotoPath);
        }
        return true;
    }

    protected abstract boolean saveWishItem(final WishInput input);

    /***
     * Save user input as a wish item
     */
    protected void save() {
        if (!validateInput()) {
            Log.e(TAG, "error in input");
            return;
        }

        saveWishItem(wishInput());

        //save the tags of this item
        TagItemDBManager.instance().Update_item_tags(mItem_id, mTags);
        EventBus.getInstance().post(new MyWishChangeEvent());

        SyncAgent.getInstance().sync();

        Tracker t = ((WishlistApplication) getApplication()).getTracker(WishlistApplication.TrackerName.APP_TRACKER);
        t.send(new HitBuilders.EventBuilder()
                .setCategory("Wish")
                .setAction("Save")
                .build());

        //close this activity
        Intent resultIntent = new Intent();
        resultIntent.putExtra("itemID", mItem_id);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case TAKE_PICTURE: {
                if (resultCode == RESULT_OK) {
                    Tracker t = ((WishlistApplication) getApplication()).getTracker(WishlistApplication.TrackerName.APP_TRACKER);
                    t.send(new HitBuilders.EventBuilder()
                            .setCategory("Wish")
                            .setAction("TakenPicture")
                            .setLabel("FromEditItemCameraButton")
                            .build());

                    setTakenPhoto();
                } else {
                    Log.d(TAG, "cancel taking photo");
                    mTempPhotoPath = null;
                }
                break;
            }
            case SELECT_PICTURE: {
                if (resultCode == RESULT_OK) {
                    mSelectedPicUri = data.getData();
                    Tracker t = ((WishlistApplication) getApplication()).getTracker(WishlistApplication.TrackerName.APP_TRACKER);
                    t.send(new HitBuilders.EventBuilder()
                            .setCategory("Wish")
                            .setAction("SelectedPicture")
                            .build());
                    setSelectedPic();
                }
                break;
            }
            case ADD_TAG: {
                if (resultCode == RESULT_OK) {
                    mTags = data.getStringArrayListExtra(AddTagFromEditActivity.TAGS);
                }
            }
        }//switch
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

    protected String copyPhotoToAlbum(Uri uri) {
        try {
            //save the photo to a file we created in wishlist album
            final InputStream in = getContentResolver().openInputStream(uri);
            File f = PhotoFileCreater.getInstance().setupPhotoFile(false);
            String path = f.getAbsolutePath();
            OutputStream stream = new BufferedOutputStream(new FileOutputStream(f));
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int len;
            while ((len = in.read(buffer)) != -1) {
                stream.write(buffer, 0, len);
            }
            in.close();
            if (stream != null) {
                stream.close();
            }
            return path;
        }
        catch (FileNotFoundException e) {
            Log.e(TAG, e.toString());
        }
        catch (IOException e) {
            Log.e(TAG, e.toString());
        }
        return null;
    }

    protected void removeTempPhoto() {
        if (mTempPhotoPath != null) {
            File f = new File(mTempPhotoPath);
            f.delete();
        }
    }

    protected abstract boolean navigateBack();

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

    protected Boolean setTakenPhoto() {
        if (mTempPhotoPath == null) {
            return false;
        }
        Log.d(TAG, "setTakePhoto " + mTempPhotoPath);
        File tempPhotoFile = new File(mTempPhotoPath);
        mImageItem.setVisibility(View.VISIBLE);

        // Picasso bug: fit() does not work when image is large
        // https://github.com/square/picasso/issues/249
        // Picasso.with(this).invalidate(tempPhotoFile);
        // Picasso.with(this).load(tempPhotoFile).fit.into(mImageItem);

        // Because we save the taken photo to the same file, use StringSignature here to avoid loading image from cache
        Glide.with(this).load(tempPhotoFile).fitCenter().signature(new StringSignature(String.valueOf(tempPhotoFile.lastModified()))).into(mImageItem);
        mSelectedPicUri = null;
        mSelectedPic = false;
        return true;
    }

    protected Boolean setSelectedPic() {
        if (mSelectedPicUri == null) {
            return false;
        }
        Log.e(TAG, "setSelectedPic " + mSelectedPicUri.toString());
        mImageItem.setVisibility(View.VISIBLE);
        // Picasso bug: fit() does not work when image is large
        // https://github.com/square/picasso/issues/249
        Glide.with(this).load(mSelectedPicUri).fitCenter().into(mImageItem);
        mFullsizePhotoPath = null;
        mSelectedPic = true;
        return true;
    }

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

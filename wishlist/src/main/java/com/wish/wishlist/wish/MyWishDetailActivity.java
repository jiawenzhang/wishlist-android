package com.wish.wishlist.wish;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.tokenautocomplete.TokenCompleteTextView;
import com.wish.wishlist.R;
import com.wish.wishlist.activity.FullscreenPhotoActivity;
import com.wish.wishlist.db.TagItemDBManager;
import com.wish.wishlist.event.EventBus;
import com.wish.wishlist.event.MyWishChangeEvent;
import com.wish.wishlist.image.ImageManager;
import com.wish.wishlist.model.WishItemManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
    private static final int EDIT_ITEM = 0;
    private static final String TAG = "MyWishDetailActivity";
    private Point mScreenSize = new Point();
    private TagView mTagView;

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

                    int width = mScreenSize.x;
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

        double lat = mItem.getLatitude();
        double lng = mItem.getLongitude();
        String address = mItem.getAddress();

        if (lat != Double.MIN_VALUE && lng != Double.MIN_VALUE && (address.equals("unknown") || address.equals(""))) {
            //we have a location by gps, but don't have an address
            Geocoder gc = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> addresses = gc.getFromLocation(lat, lng, 1);
                StringBuilder sb = new StringBuilder();
                if (addresses.size() > 0) {
                    Address add = addresses.get(0);
                    for (int k = 0; k < add.getMaxAddressLineIndex()+1; k++)
                        sb.append(add.getAddressLine(k)).append("\n");
                }
                address = sb.toString();
            } catch (IOException e) {
                address = "unknown";
            }
            mItem.setAddress(address);
            mItem.setUpdatedTime(System.currentTimeMillis());
            mItem.save();
        }

        Display display = ((WindowManager) mPhotoView.getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        display.getSize(mScreenSize);
        showItemInfo();

        mTagView = (TagView) this.findViewById(R.id.tag_view);

        if (savedInstanceState == null) {
            // if screen is oriented, savedInstanceStat != null,
            // and don't add tags again on screen orientation
            addTags();
        }

        final View imageFrame = findViewById(R.id.imagePhotoDetailFrame);
        imageFrame.setOnClickListener(new View.OnClickListener() {
            final String fullsize_picture_str = mItem.getFullsizePicPath();
            @Override
            public void onClick(View view) {
                if (fullsize_picture_str != null) {
                    Intent i = new Intent(MyWishDetailActivity.this, FullscreenPhotoActivity.class);
                    i.putExtra(FullscreenPhotoActivity.PHOTO_PATH, fullsize_picture_str);
                    startActivity(i);
                }
            }
        });
    }

    @Override
    protected void showItemInfo() {
        super.showItemInfo();
        final ImageView privateImage = (ImageView) findViewById(R.id.imgPrivate);
        if (mItem.getAccess() == mItem.PRIVATE) {
            privateImage.setVisibility(View.VISIBLE);
        } else {
            privateImage.setVisibility(View.GONE);
        }
    }

    @Override
    protected void showPhoto() {
        String fullsize_picture_str = mItem.getFullsizePicPath();
        new GetBitmapTask().execute(fullsize_picture_str);
    }

    void addTags() {
        ArrayList<String> tags = TagItemDBManager.instance().tags_of_item(mItem.getId());
        mTagView.removeAllTags();
        for (String tagTxt : tags) {
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
        Intent i = new Intent(MyWishDetailActivity.this, EditWishActivity.class);
        i.putExtra(EditWishActivity.ITEM_ID, mItem.getId());
        startActivityForResult(i, EDIT_ITEM);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case EDIT_ITEM: {
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        long id = data.getLongExtra("itemID", -1);
                        if (id != -1) {
                            mItem = WishItemManager.getInstance().getItemById(id);
                            showItemInfo();
                            addTags();
                        }
                    }
                }
                break;
            }
        }
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

    @Override
    public void onTokenAdded(Object token) {}

    @Override
    public void onTokenRemoved(Object token) {}

    @Override
    protected boolean myWish() { return true; }
}

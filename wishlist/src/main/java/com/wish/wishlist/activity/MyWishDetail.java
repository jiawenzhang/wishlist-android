package com.wish.wishlist.activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.squareup.picasso.Picasso;
import com.tokenautocomplete.TokenCompleteTextView;
import com.wish.wishlist.R;
import com.wish.wishlist.db.TagItemDBManager;
import com.wish.wishlist.model.WishItemManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/***
 * MyWishDetail displays the detailed info. of an item.
 * It also handles the left/right swipe gesture form user, which correspond to
 * navigating to the previous and next item, respectively.
 *
 * the order of the items during swiping is the order of the items displayed in
 * the MyWish activity
 */

public class MyWishDetail extends WishDetail implements TokenCompleteTextView.TokenListener {
    private static final int EDIT_ITEM = 0;

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

        showItemInfo();

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
                    Intent i = new Intent(MyWishDetail.this, FullscreenPhoto.class);
                    i.putExtra(EditItem.FULLSIZE_PHOTO_PATH, fullsize_picture_str);
                    startActivity(i);
                }
            }
        });
    }

    @Override
    protected void showPhoto() {
        String fullsize_picture_str = mItem.getFullsizePicPath();
        if (fullsize_picture_str != null) {
            Picasso.with(this).load(new File(fullsize_picture_str)).fit().centerCrop().into(mPhotoView);
            mPhotoView.setVisibility(View.VISIBLE);
        } else {
            // user added this item without taking a pic
            mPhotoView.setVisibility(View.GONE);
        }
    }

    void addTags() {
        ArrayList<String> tags = TagItemDBManager.instance().tags_of_item(mItem.getId());
        mTagsView.removeAllObject();
        for (String tag : tags) {
            mTagsView.addObject(tag);
        }
    }

    private void deleteItem() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        builder.setMessage("Discard the wish?").setCancelable(
                false).setPositiveButton("Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        WishItemManager.getInstance().deleteItemById(mItem.getId());
                        Intent intent = new Intent();
                        intent.putExtra("id", mItem.getId());
                        setResult(RESULT_OK, intent);
                        MyWishDetail.this.finish();
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
        Intent i = new Intent(MyWishDetail.this, EditItem.class);
        i.putExtra("item_id", mItem.getId());
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
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_my_wish_detail, menu);
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
            case R.id.menu_item_detail_map:
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

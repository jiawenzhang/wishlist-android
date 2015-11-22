package com.wish.wishlist.activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.tokenautocomplete.TokenCompleteTextView;
import com.wish.wishlist.R;
import com.wish.wishlist.db.ItemDBManager;
import com.wish.wishlist.db.ItemDBManager.ItemsCursor;
import com.wish.wishlist.db.TagItemDBManager;
import com.wish.wishlist.model.WishItem;
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
    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;
    //	private GestureDetector gestureDetector;
    View.OnTouchListener _gestureListener;

    private static final int EDIT_ITEM = 0;
    private ItemDBManager mItemDBManager;

    private long mItemId = -1;
    private int _position;
    private int _prevPosition;
    private int _nextPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Remember the id of the item user clicked
        // in the previous activity (MyWish.java)
        Intent i = getIntent();
        mItemId = i.getLongExtra("item_id", -1);
        _position = i.getIntExtra("position", 0);

        mItem = WishItemManager.getInstance().getItemById(mItemId);
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

        showItemInfo(mItem);

        if (savedInstanceState == null) {
            // if screen is oriented, savedInstanceStat != null,
            // and don't add tags again on screen orientation
            addTags();
        }

//		// set the gesture detection
//		gestureDetector = new GestureDetector(new MyGestureDetector());
//
//		gestureListener = new View.OnTouchListener() {
//			public boolean onTouch(View v, MotionEvent event) {
//				if (gestureDetector.onTouchEvent(event)) {
//					return true;
//				}
//				return false;
//			}
//		};
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
        ArrayList<String> tags = TagItemDBManager.instance().tags_of_item(mItemId);
        mTagsView.removeAllObject();
        for (String tag : tags) {
            mTagsView.addObject(tag);
        }
    }

    /***
     * get the _ID of the item in Item table
     * whose position in the listview is next
     * to the current item
     *
     * @return
     */
    private long[] getNextDBItemID() {

        // Get all of the rows from the database in sorted order as in the
        long[] next_pos_id = new long[2];
        // ItemsCursor c = wishListDB.getItems(ItemsCursor.SortBy.name);
        mItemDBManager = new ItemDBManager();
        ItemsCursor c = mItemDBManager.getItems(ItemDBManager.KEY_NAME, null, new ArrayList<Long>());
        long nextItemID;
        if (_position < c.getCount())
            _nextPosition = _position + 1;

        else
            _nextPosition = _position;

        c.move(_nextPosition);
        nextItemID = c.getLong(c.getColumnIndexOrThrow(ItemDBManager.KEY_ID));

        next_pos_id[0] = _nextPosition;
        next_pos_id[1] = nextItemID;
        return next_pos_id;
    }

    /***
     * get the _ID of the item in Item table
     * whose position in the listview is previous
     * to the current item
     *
     * @return
     */

    private long[] getPrevDBItemID() {

        long[] prev_pos_id = new long[2];

        // open the database for operations of Item table
        mItemDBManager = new ItemDBManager();
        ItemsCursor c = mItemDBManager.getItems(ItemDBManager.KEY_NAME, null, new ArrayList<Long>());
        long prevItemID;
        if (_position > 0)
            _prevPosition = _position - 1;

        else
            _prevPosition = _position;

        c.move(_prevPosition);
        // prevItemID = c.getLong(
        // c.getColumnIndexOrThrow(WishListDataBase.KEY_ITEMID));
        prevItemID = c.getLong(c.getColumnIndexOrThrow(ItemDBManager.KEY_ID));
        // long item_id = Long.parseLong(itemIdTextView.getText().toString());
        prev_pos_id[0] = _prevPosition;
        prev_pos_id[1] = prevItemID;
        return prev_pos_id;
    }

    private void deleteItem() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        builder.setMessage("Discard the wish?").setCancelable(
                false).setPositiveButton("Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        WishItemManager.getInstance().deleteItemById(mItemId);
                        Intent intent = new Intent();
                        intent.putExtra("id", mItemId);
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
        i.putExtra("item_id", mItemId);
        startActivityForResult(i, EDIT_ITEM);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case EDIT_ITEM: {
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        long id = data.getLongExtra("itemID", -1);
                        if (id != -1) {
                            WishItem item = WishItemManager.getInstance().getItemById(id);
                            showItemInfo(item);
                            addTags();
                        }
                    }
                }
                break;
            }
        }
    }

    class MyGestureDetector extends SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                               float velocityY) {
            try {
                if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
                    return false;
                // right to left swipe
                if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
                        && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    // Toast.makeText(WishDetail.this, "swipe to right",
                    // Toast.LENGTH_SHORT).show();

                    //get the item id of the next item and
                    //start a new activity to display the
                    //next item's detailed info.
                    long[] next_p_i = new long[2];
                    next_p_i = getNextDBItemID();
                    Intent i = new Intent(MyWishDetail.this,
                            MyWishDetail.class);

                    i.putExtra("position", (int) next_p_i[0]);
                    i.putExtra("item_id", next_p_i[1]);

                    startActivity(i);
                    // Set the transition -> method available from Android 2.0
                    // and beyond
                    overridePendingTransition(R.anim.slide_left_in,
                            R.anim.slide_right_out);

                    // WishDetail.this.overridePendingTransition(0,0);

                } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
                        && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    // Toast.makeText(WishDetail.this, "swipe to left",
                    // Toast.LENGTH_SHORT).show();


                    //get the item id of the previous item and
                    //start a new activity to display the
                    //previous item's detailed info.
                    long[] prev_p_i = new long[2];
                    prev_p_i = getPrevDBItemID();
                    Intent i = new Intent(MyWishDetail.this,
                            MyWishDetail.class);
                    i.putExtra("position", (int) prev_p_i[0]);
                    i.putExtra("item_id", prev_p_i[1]);

                    startActivity(i);
                    overridePendingTransition(R.anim.slide_right_in,
                            R.anim.slide_left_out);
                }
            } catch (Exception e) {
                // nothing
            }
            return false;
        }
    }

//	@Override
//	public boolean onTouchEvent(MotionEvent event) {
//		if (gestureDetector.onTouchEvent(event))
//			return true;
//		else
//			return false;
//	}

    /***
     * called when the "return" button is clicked
     * it closes the WishDetail activity and starts
     * the MyWish activity
     */
//	@Override
//	public boolean onKeyDown(int keyCode, KeyEvent event) {
//		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
//			// do something on back.
//			startActivity(new Intent(WishDetail.this, MyWish.class));
//			WishDetail.this.finish();
//
//			return true;
//		}
//
//		return super.onKeyDown(keyCode, event);
//	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_my_wish_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        long itemId = menuItem.getItemId();
        if (itemId == R.id.menu_item_detail_edit) {
            editItem();
            return true;
        } else if (itemId == R.id.menu_item_detail_share) {
            shareItem();
            return true;
        } else if (itemId == R.id.menu_item_detail_map) {
            mItemDBManager = new ItemDBManager();
            WishItem wishItem = WishItemManager.getInstance().getItemById(mItemId);

            if (wishItem.getLatitude() == Double.MIN_VALUE && wishItem.getLongitude() == Double.MIN_VALUE) {
                Toast toast = Toast.makeText(this, "location unknown", Toast.LENGTH_SHORT);
                toast.show();
            } else {
                Intent mapIntent = new Intent(this, Map.class);
                mapIntent.putExtra("type", "markOne");
                mapIntent.putExtra("id", mItemId);
                startActivity(mapIntent);
            }
            return true;
        } else if (itemId == R.id.menu_item_detail_delete) {
            deleteItem();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onTokenAdded(Object token) {}

    @Override
    public void onTokenRemoved(Object token) {}
}

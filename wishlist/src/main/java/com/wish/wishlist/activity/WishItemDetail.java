package com.wish.wishlist.activity;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.tokenautocomplete.TokenCompleteTextView;
import com.wish.wishlist.R;
import com.wish.wishlist.db.ItemDBManager;
import com.wish.wishlist.db.ItemDBManager.ItemsCursor;
import com.wish.wishlist.db.TagItemDBManager;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.model.WishItemManager;
import com.wish.wishlist.util.DateTimeFormatter;
import com.wish.wishlist.util.DialogOnShowListener;
import com.wish.wishlist.util.social.ShareHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/***
 * WishItemDetail is responsible for displaying the detailed info. of an item. 
 * It also handles the left/right swipe gesture form user, which correspond to 
 * navigating to the previous and next item, respectively.
 *
 * the order of the items during swiping is the order of the items displayed in 
 * the WishList activity
 */
public class WishItemDetail extends Activity implements TokenCompleteTextView.TokenListener {
    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;
    //	private GestureDetector gestureDetector;
    View.OnTouchListener _gestureListener;

    private static final int EDIT_ITEM = 0;
    private ItemDBManager _itemDBManager;

    private ImageView _photoView;
    private TextView _nameView;
    private TextView _descrpView;
    private TextView _dateView;
    private TextView _priceView;
    private TextView _storeView;
    private TextView _locationView;
    private TextView _linkView;
    private ImageButton _backImageButton;
    private ImageButton _deleteImageButton;
    private ImageButton _editImageButton;
    private ImageButton _shareImageButton;
    private TagsCompletionView _tagsView;

    private long _itemId = -1;
    private int _position;
    private int _prevPosition;
    private int _nextPosition;
    private String _fullsize_picture_str = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wishitem_detail);

        setUpActionBar();

        // Remember the id of the item user clicked
        // in the previous activity (WishList.java)
        Intent i = getIntent();
        _itemId = i.getLongExtra("item_id", -1);
        _position = i.getIntExtra("position", 0);

        WishItem item = WishItemManager.getInstance(this).retrieveItembyId(_itemId);
        double lat = item.getLatitude();
        double lng = item.getLongitude();
        String address = item.getAddress();

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
            item.setAddress(address);
            item.save();
        }

        _nameView = (TextView) findViewById(R.id.itemNameDetail);
        _descrpView = (TextView) findViewById(R.id.itemDesriptDetail);
        _dateView = (TextView) findViewById(R.id.itemDateDetail);
        _priceView = (TextView) findViewById(R.id.itemPriceDetail);
        _storeView = (TextView) findViewById(R.id.itemStoreDetail);
        _locationView = (TextView) findViewById(R.id.itemLocationDetail);
        _linkView = (TextView) findViewById(R.id.itemLink);
        _photoView = (ImageView) findViewById(R.id.imgPhotoDetail);

        // tagsView will gain focus automatically when the activity starts, and it will trigger the keyboard to
        // show up if we don't have the following line.
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        _tagsView = (TagsCompletionView)findViewById(R.id.ItemTagsView);
        View.OnTouchListener otl = new View.OnTouchListener() {
            public boolean onTouch (View v, MotionEvent event) {
                return true;
                // the listener has consumed the event
                // this is to prevent touch event on the tagsView such as
                // keyboard pops up, text select, copy/paste etc.
            }
        };
        _tagsView.setOnTouchListener(otl);
        _tagsView.setCursorVisible(false);

        showItemInfo(item);

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
            @Override
            public void onClick(View view) {
                Intent i = new Intent(WishItemDetail.this, FullscreenPhoto.class);
                if (_fullsize_picture_str != null) {
                    i.putExtra(EditItem.FULLSIZE_PHOTO_PATH, _fullsize_picture_str);
                    startActivity(i);
                }
            }
        });
    }

    private void showItemInfo(WishItem item) {
        _fullsize_picture_str = item.getFullsizePicPath();
        if (_fullsize_picture_str != null) {
            Picasso.with(this).load(new File(_fullsize_picture_str)).fit().centerCrop().into(_photoView);
            _photoView.setVisibility(View.VISIBLE);
        }
        else {
            //pic_str is null, so user added this item without taking a pic.
            //simply don't show any picture in this case.
            _photoView.setVisibility(View.GONE);
        }

        String dateTimeStr = item.getDate();
        String dateTimeStrNew = DateTimeFormatter.getInstance().getDateTimeString(dateTimeStr);

        _nameView.setText(item.getName());
        if (item.getComplete() == 1) {
            final ImageView completeImage = (ImageView) findViewById(R.id.item_checkmark_complete);
            completeImage.setVisibility(View.VISIBLE);
        }
        _dateView.setText(dateTimeStrNew);

        // format the price
        String priceStr = item.getPriceAsString();
        if (priceStr != null) {
            _priceView.setText(WishItem.priceStringWithCurrency(priceStr, this));
            _priceView.setVisibility(View.VISIBLE);
        }
        else {
            _priceView.setVisibility(View.GONE);
        }

        //used as a note
        String descrptStr = item.getDesc();
        if (!descrptStr.equals("")) {
            _descrpView.setText(descrptStr);
            _descrpView.setVisibility(View.VISIBLE);
        }
        else {
            _descrpView.setVisibility(View.GONE);
        }

        String storeName = item.getStoreName();
        if (!storeName.equals("")) {
            _storeView.setText(storeName);
            _storeView.setVisibility(View.VISIBLE);
        }
        else {
            _storeView.setVisibility(View.GONE);
        }

        String address = item.getAddress();
        if (!address.equals("unknown") && !address.equals("")) {
            _locationView.setText(address);
            _locationView.setVisibility(View.VISIBLE);
        }
        else {
            _locationView.setVisibility(View.GONE);
        }

        String link = item.getLink();
        if (link != null && !link.isEmpty()) {
            String url = item.getLink();
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://" + url;
            }
            String text = "<a href=\"" + url + "\">Link</a>";
            _linkView.setText(Html.fromHtml(text));
            _linkView.setMovementMethod(LinkMovementMethod.getInstance());
            _linkView.setVisibility(View.VISIBLE);
        } else {
            _linkView.setVisibility(View.GONE);
        }
    }

    void addTags() {
        ArrayList<String> tags = TagItemDBManager.instance(this).tags_of_item(_itemId);
        _tagsView.removeAllObject();
        for (String tag : tags) {
            _tagsView.addObject(tag);
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
        // open the database for operations of Item table
        _itemDBManager = new ItemDBManager(this);
        _itemDBManager.open();
        ItemsCursor c = _itemDBManager.getItems(ItemsCursor.SortBy.item_name.toString(), null, new ArrayList<Long>());
        _itemDBManager.close();
        long nextItemID;
        if (_position < c.getCount())
            _nextPosition = _position + 1;

        else
            _nextPosition = _position;

        c.move(_nextPosition);
        // nextItemID = c.getLong(
        // c.getColumnIndexOrThrow(WishListDataBase.KEY_ITEMID));
        nextItemID = c.getLong(c.getColumnIndexOrThrow(ItemDBManager.KEY_ID));

        // long item_id = Long.parseLong(itemIdTextView.getText().toString());
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

        // ItemsCursor c = wishListDB.getItems(ItemsCursor.SortBy.name);
        // open the database for operations of Item table
        _itemDBManager = new ItemDBManager(this);
        _itemDBManager.open();
        ItemsCursor c = _itemDBManager.getItems(ItemsCursor.SortBy.item_name.toString(), null, new ArrayList<Long>());
        _itemDBManager.close();
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

    private void deleteItem(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Discard the wish?").setCancelable(
                false).setPositiveButton("Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        WishItemManager.getInstance(WishItemDetail.this).deleteItembyId(_itemId);
                        WishItemDetail.this.finish();
                        //return super.onKeyDown(keyCode, event);
                    }
                }).setNegativeButton("No",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogOnShowListener(this));
        dialog.show();
    }

    private void editItem(){
        Intent i = new Intent(WishItemDetail.this, EditItem.class);
        i.putExtra("item_id", _itemId);
        startActivityForResult(i, EDIT_ITEM);
    }

    private void shareItem(){
        ShareHelper share = new ShareHelper(this, _itemId);
        share.share();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case EDIT_ITEM: {
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        long id = data.getLongExtra("itemID", -1);
                        if (id != -1) {
                            WishItem item = WishItemManager.getInstance(this).retrieveItembyId(id);
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
                    // Toast.makeText(WishItemDetail.this, "swipe to right",
                    // Toast.LENGTH_SHORT).show();

                    //get the item id of the next item and
                    //start a new activity to display the
                    //next item's detailed info.
                    long[] next_p_i = new long[2];
                    next_p_i = getNextDBItemID();
                    Intent i = new Intent(WishItemDetail.this,
                            WishItemDetail.class);

                    i.putExtra("position", (int) next_p_i[0]);
                    i.putExtra("item_id", next_p_i[1]);

                    startActivity(i);
                    // Set the transition -> method available from Android 2.0
                    // and beyond
                    overridePendingTransition(R.anim.slide_left_in,
                            R.anim.slide_right_out);

                    // WishItemDetail.this.overridePendingTransition(0,0);

                } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
                        && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    // Toast.makeText(WishItemDetail.this, "swipe to left",
                    // Toast.LENGTH_SHORT).show();


                    //get the item id of the previous item and
                    //start a new activity to display the
                    //previous item's detailed info.
                    long[] prev_p_i = new long[2];
                    prev_p_i = getPrevDBItemID();
                    Intent i = new Intent(WishItemDetail.this,
                            WishItemDetail.class);
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
     * it closes the WishItemDetail activity and starts
     * the WishList activity
     */
//	@Override
//	public boolean onKeyDown(int keyCode, KeyEvent event) {
//		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
//			// do something on back.
//			startActivity(new Intent(WishItemDetail.this, WishList.class));
//			WishItemDetail.this.finish();
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
        inflater.inflate(R.menu.menu_item_detail, menu);
        return true;
    }
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        long itemId = item.getItemId();
        if (itemId ==  android.R.id.home) {
            Intent resultIntent = new Intent();
            setResult(RESULT_OK, resultIntent);
            finish();
            return true;
        }
        else if (itemId == R.id.menu_item_detail_edit) {
            editItem();
            return true;
        }
        else if (itemId == R.id.menu_item_detail_share) {
            shareItem();
            return true;
        }
        else if (itemId == R.id.menu_item_detail_map) {
            // open the database for operations of Item table
            _itemDBManager = new ItemDBManager(this);
            _itemDBManager.open();
            double[] dLocation  = _itemDBManager.getItemLocation(_itemId);
            _itemDBManager.close();

            if (dLocation[0] == Double.MIN_VALUE && dLocation[1] == Double.MIN_VALUE) {
                Toast toast = Toast.makeText(this, "location unknown", Toast.LENGTH_SHORT);
                toast.show();
            } else {
                Intent mapIntent = new Intent(this, Map.class);
                mapIntent.putExtra("type", "markOne");
                mapIntent.putExtra("latitude", dLocation[0]);
                mapIntent.putExtra("longitude", dLocation[1]);
                startActivity(mapIntent);
            }
            return true;
        }
        else if (itemId == R.id.menu_item_detail_delete) {
            deleteItem();
            return true;
        }
        return false;
    }

    @SuppressLint("NewApi")
    private void setUpActionBar() {
        // Make sure we're running on Honeycomb or higher to use ActionBar APIs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            findViewById(R.id.detailView_header).setVisibility(View.GONE);
            ActionBar actionBar = getActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        else {
            // we use the header instead of action bar for GingerBread and lower
            findViewById(R.id.detailView_header).findViewById(R.id.imageButton_back_logo).setVisibility(View.VISIBLE);
            findViewById(R.id.detailView_header).findViewById(R.id.imageButton_delete).setVisibility(View.VISIBLE);
            findViewById(R.id.detailView_header).findViewById(R.id.imageButton_edit).setVisibility(View.VISIBLE);
            findViewById(R.id.detailView_header).findViewById(R.id.imageButton_share).setVisibility(View.VISIBLE);

            _backImageButton = (ImageButton) findViewById(R.id.imageButton_back_logo);
            _backImageButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish();
                    //start the WishList activity and move the focus to the newly added item
                    //				Intent home = new Intent(WishItemDetail.this, DashBoard.class);
                    //				startActivity(home);
                    //onSearchRequested();
                }
            });

            _deleteImageButton = (ImageButton) findViewById(R.id.imageButton_delete);
            _deleteImageButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    deleteItem();
                }
            });

            _editImageButton = (ImageButton) findViewById(R.id.imageButton_edit);
            _editImageButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    editItem();
                }
            });

            _shareImageButton = (ImageButton) findViewById(R.id.imageButton_share);
            _shareImageButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    shareItem();
                }
            });
        }
    }

    @Override
    public void onTokenAdded(Object token) {}

    @Override
    public void onTokenRemoved(Object token) {}
}

package com.wish.wishlist.activity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.content.Intent;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.wish.wishlist.R;
import com.wish.wishlist.db.TagItemDBManager;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.model.WishItemManager;
import com.wish.wishlist.WishlistApplication;
import com.wish.wishlist.util.Options;
import com.wish.wishlist.util.camera.CameraManager;
import com.wish.wishlist.util.sync.SyncAgent;

/***
 * WishList.java is responsible for displaying wish items in either list or grid
 * view and providing access to functions of manipulating items such as adding,
 * deleting and editing items, sorting items, searching items, viewing item
 * detailed info. and etc.
 *
 * Item display is via binding the list view or grid view to the Item table in
 * the database using WishListItemCursorAdapter
 *
 * switching between list and grid view is realized using viewflipper
 *
 * sorting items is via "SELECT ... ORDER BY" query to the database
 *
 */
public class WishList extends WishBaseActivity implements
        SyncAgent.OnSyncWishChangedListener,
        SyncAgent.OnDownloadWishDoneListener {

    //private java.util.Map _where = new HashMap<>();
    public static final String TAG = "WishList";

    private static final int EDIT_ITEM = 0;
    private static final int ADD_ITEM = 1;
    private static final int FIND_TAG = 2;
    private static final int ADD_TAG = 3;
    private static final int ITEM_DETAILS = 4;
    private static final int TAKE_PICTURE = 5;

    private Options.Tag _tag = new Options.Tag(null);

    private String _fullsizePhotoPath = null;
    private String _newfullsizePhotoPath = null;

    private Button _addNew;
    private ArrayList<Long> _itemIds = new ArrayList<>();
    private ModalMultiSelectorCallback mActionModeCallback = new ModalMultiSelectorCallback(mMultiSelector) {
        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            super.onCreateActionMode(actionMode, menu);
            getMenuInflater().inflate(R.menu.menu_main_action, menu);
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            super.onDestroyActionMode(mode);
            mWishAdapter.clearSelectedItemIds();

            // notifyDataSetChanged will fix a bug in recyclerview-multiselect lib, where the selected item's state does
            // not get cleared when the action mode is finished.
            mWishAdapter.notifyDataSetChanged();
            mMultiSelector.clearSelections();
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            ArrayList<Long> itemIds = selectedItemIds();
            Log.d(TAG, "selected item ids: " + itemIds.toString());
            actionMode.finish();

            switch (menuItem.getItemId()) {
                case R.id.menu_tag:
                    Log.d(TAG, "tag");
                    Intent intent = new Intent(WishList.this, AddTag.class);
                    long[] ids = new long[itemIds.size()];
                    for (int i = 0; i < itemIds.size() ; i++) {
                        ids[i] = itemIds.get(i);
                    }
                    intent.putExtra(AddTag.ITEM_ID_ARRAY, (ids));
                    startActivityForResult(intent, ADD_TAG);
                    return true;
                case R.id.menu_share:
                    Log.d(TAG, "share");
                    //ShareHelper share = new ShareHelper(this, _selectedItem_id);
                    //share.share();
                    return true;
                case R.id.menu_delete:
                    Log.d(TAG, "delete");
                    deleteItems(itemIds);
                    return true;
                case R.id.menu_complete:
                    Log.d(TAG, "complete");
                    markItemComplete(itemIds, 1);
                    return true;
                case R.id.menu_incomplete:
                    Log.d(TAG, "incomplete");
                    markItemComplete(itemIds, 0);
                    return true;
                default:
                    return false;
            }
        }
    };

    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        _tag.read();
        if (_tag.val() != null) {
            _itemIds = TagItemDBManager.instance().ItemIds_by_tag(_tag.val());
        }

        _addNew = (Button) findViewById(R.id.addNewWishButton);
        _addNew.setOnClickListener(new android.view.View.OnClickListener() {
            public void onClick(android.view.View v) {
                Intent editItem = new Intent(WishList.this, EditItem.class);
                startActivityForResult(editItem, ADD_ITEM);
            }
        });

        if (savedInstanceState != null) {
            Log.d(WishList.TAG, "savedInstanceState != null");
            // restore the current selected item in the list
            _newfullsizePhotoPath = savedInstanceState.getString("newfullsizePhotoPath");
            _fullsizePhotoPath = savedInstanceState.getString("fullsizePhotoPath");

            Log.d(WishList.TAG, "_newfullsizePhotoPath " + _newfullsizePhotoPath);
            Log.d(WishList.TAG, "_fullsizePhotoPath " + _fullsizePhotoPath);
        } else{
            Log.d(WishList.TAG, "savedInstanceState == null");
        }

        SyncAgent.getInstance().register(this);

        // only enable swipe down refresh when the first item in recycler view is visible
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                int topRowVerticalPosition =
                        (recyclerView == null || recyclerView.getChildCount() == 0) ? 0 : recyclerView.getChildAt(0).getTop();
                _swipeRefreshLayout.setEnabled(topRowVerticalPosition >= 0);
            }
        });

        // the refresh listener. this would be called when the layout is pulled down
        _swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                _swipeRefreshLayout.setRefreshing(true);
                Log.e(TAG, "refresh");
                SyncAgent.getInstance().sync();
                // TODO : request data here
                // our swipeRefreshLayout needs to be notified when the data is returned in order for it to stop the animation
                //handler.post(refreshing);
            }
        });

        handleIntent(getIntent());
    }

    @Override
    protected void setContentView() {
        setContentView(R.layout.main);
        setupActionBar(R.id.main_toolbar);
    }

    @Override
    protected void initializeWishView() {
        mWishlist = WishItemManager.getInstance().getItems(_sort.toString(), _where, _itemIds);
        updateWishView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_my_wish, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        _menuSearch = menu.findItem(R.id.menu_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(_menuSearch);
        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
        return super.onCreateOptionsMenu(menu);
    }

    /***
     * display the items in either list or grid view sorted by "sortBy"
     *
     * @param searchName
     *            : the item name to match, null for all items
     */
    @Override
    protected void reloadItems(String searchName, java.util.Map where) {
        if (searchName == null) {
            // Get all of the rows from the Item table
            // Keep track of the TextViews added in list lstTable
            mWishlist = WishItemManager.getInstance().getItems(_sort.toString(), where, _itemIds);
        } else {
            mWishlist = WishItemManager.getInstance().searchItems(searchName, _sort.toString());
        }
        updateWishView();
        updateDrawerList();
        updateActionBarTitle();
    }

    private void markItemComplete(final List<Long> item_ids, int complete) {
        HashSet<Long/*item_id*/> changed = new HashSet<>();
        for (long item_id : item_ids) {
            WishItem wish_item = WishItemManager.getInstance().getItemById(item_id);
            String label = "Complete";
            if (complete == 0) {
                label = "InProgress";
            }
            if (wish_item.getComplete() != complete) {
                changed.add(item_id);
                wish_item.setComplete(complete);
                Tracker t = ((WishlistApplication) getApplication()).getTracker(WishlistApplication.TrackerName.APP_TRACKER);
                t.send(new HitBuilders.EventBuilder()
                        .setCategory("Wish")
                        .setAction("ChangeStatus")
                        .setLabel(label)
                        .build());
            }
            wish_item.setUpdatedTime(System.currentTimeMillis());
            wish_item.setSyncedToServer(false);
            wish_item.save();

        }
        for (int i = 0; i < mWishlist.size(); i++) {
            WishItem item = mWishlist.get(i);
            if (changed.contains(item.getId())) {
                item.setComplete(complete);
                if (mWishAdapter != null) {
                    mWishAdapter.notifyItemChanged(i, item);
                }
            }
        }
    }

    private void deleteItems(final List<Long> item_ids) {
        final String message;
        if (item_ids.size() == 1) {
            message = "Delete the wish?";
        } else {
            message = "Delete " + item_ids.size() + " wishes?";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        builder.setMessage(message);
        builder.setCancelable(false);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                for (long item_id : item_ids) {
                    WishItemManager.getInstance().deleteItemById(item_id);
                }
                mWishAdapter.removeByItemIds(item_ids);
                //Fixme: need to show make a wish view if there is no wish left
            }
        });
        builder.setNegativeButton("No",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        AlertDialog dialog;
        dialog = builder.create();
        dialog.show();
    }

    private void dispatchTakePictureIntent() {
        CameraManager c = new CameraManager();
        _newfullsizePhotoPath = c.getPhotoPath();
        startActivityForResult(c.getCameraIntent(), TAKE_PICTURE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        super.onOptionsItemSelected(item);

        long itemId = item.getItemId();
        if (itemId == R.id.menu_search) {
            //do nothing here, the search view is already configured in onCreateOptionsMenu()
            return true;
        } else if (itemId == R.id.menu_add) {
            // let user generate a wish item
            dispatchTakePictureIntent();
            return true;
        } else if(itemId == R.id.menu_sort) {
            showDialog(DIALOG_SORT);
        } else if (itemId == R.id.menu_status) {
            showDialog(DIALOG_FILTER);
        } else if (itemId == R.id.menu_tags) {
            Intent i = new Intent(WishList.this, FindTag.class);
            startActivityForResult(i, FIND_TAG);
        }
        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString("newfullsizePhotoPath", _newfullsizePhotoPath);
        savedInstanceState.putString("fullsizePhotoPath", _fullsizePhotoPath);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }

        _newfullsizePhotoPath = savedInstanceState.getString("newfullsizePhotoPath");
        _fullsizePhotoPath = savedInstanceState.getString("fullsizePhotoPath");
        super.onRestoreInstanceState(savedInstanceState);
    }

    private void updateItemIdsForTag() {
        // If the current wishlist is filtered by tag "T", and there is an item "A" in this list
        // we then enter the AddTag view for item "A" and delete the tag "T" from A. When we come back to
        // this list, we need to update _itemIds to exclude "A" so "A" will not show up in this list.
        if (_tag.val() != null) {
            _itemIds = TagItemDBManager.instance().ItemIds_by_tag(_tag.val());
            if (_itemIds.isEmpty()) {
                _tag.setVal(null);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case EDIT_ITEM: {
                if (resultCode == Activity.RESULT_OK) {
                    updateItemIdsForTag();
                }
                break;
            }
            case ITEM_DETAILS: {
                if (resultCode == Activity.RESULT_OK) {
                    updateItemIdsForTag();
                }
                break;
            }
            case ADD_ITEM: {
                if (resultCode == Activity.RESULT_OK) {
                    // Create an intent to show the item detail.
                    // Pass the item_id along so the next activity can use it to
                    // retrieve the info. about the item from database
                    long id = -1;
                    if (data != null) {
                        id = data.getLongExtra("itemID", -1);
                    }

                    if (id != -1) {
                        Intent i = new Intent(WishList.this, MyWishDetail.class);
                        i.putExtra("item_id", id);
                        startActivityForResult(i, ITEM_DETAILS);
                    }
                } else {}
                break;
            }
            case FIND_TAG: {
                if (resultCode == Activity.RESULT_OK) {
                    _tag.setVal(data.getStringExtra("tag"));
                    _tag.save();

                    if (_tag.val() != null) {
                        _itemIds = TagItemDBManager.instance().ItemIds_by_tag(_tag.val());
                    }
                }
                break;
            }
            case ADD_TAG: {
                if (resultCode == Activity.RESULT_OK) {
                    updateItemIdsForTag();
                }
                break;
            }
            case TAKE_PICTURE: {
                if (resultCode == RESULT_OK) {
                    Log.d(WishList.TAG, "TAKE_PICTURE: RESULT_OK");
                    Log.d("TAKE PICTURE", "_new " + _newfullsizePhotoPath);
                    _fullsizePhotoPath = String.valueOf(_newfullsizePhotoPath);
                    _newfullsizePhotoPath = null;
                    Intent i = new Intent(this, EditItem.class);
                    i.putExtra(EditItem.FULLSIZE_PHOTO_PATH, _fullsizePhotoPath);
                    if (_fullsizePhotoPath != null) {
                        Log.v("photo path", _fullsizePhotoPath);
                    } else {
                        Log.v("photo path", "null");
                    }
                    Tracker t = ((WishlistApplication) getApplication()).getTracker(WishlistApplication.TrackerName.APP_TRACKER);
                    t.send(new HitBuilders.EventBuilder()
                            .setCategory("Wish")
                            .setAction("TakenPicture")
                            .setLabel("FromActionBarCameraButton")
                            .build());
                    startActivityForResult(i, EDIT_ITEM);
                } else {
                    Log.d(WishList.TAG, "TAKE_PICTURE: not RESULT_OK");
                }
                break;
            }
        }
    }

    @Override
    protected Boolean goBack()
    {
        if (_nameQuery != null) {
            // We tap back on search results view, show all wishes
            _nameQuery = null;
            _tag.setVal(null);
            _itemIds.clear();
            _status.setVal(Options.Status.ALL);
            _where.clear();

            MenuItem tagItem =  _menu.findItem(R.id.menu_tags);
            tagItem.setVisible(true);

            MenuItem statusItem = _menu.findItem(R.id.menu_status);
            statusItem.setVisible(true);

            reloadItems(null, _where);
            return true;
        }
        if (_tag.val() != null || _status.val() != Options.Status.ALL) {
            //the wishes are currently filtered by tag or status, tapping back button now should clean up the filter and show all wishes
            _tag.setVal(null);
            _itemIds.clear();

            _status.setVal(Options.Status.ALL);
            // need to remove the status single item choice dialog so it will be re-created and its initial choice will refreshed
            // next time it is opened.
            removeDialog(DIALOG_FILTER);

            _where.clear();

            _tag.save();
            _status.save();

            reloadItems(null, _where);
        } else {
            //we are already showing all the wishes, tapping back button should close the list view
            finish();
        }
        return true;
    }

    /***
     * called when the "return" button is clicked
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            return goBack();
        }
        return false;
    }

    public void onSyncWishChanged() {
        Log.d(TAG, "onSyncWishChanged");
        reloadItems(_nameQuery, _where);
    }

    public void onDownloadWishDone() {
        Log.d(TAG, "onDownloadWishDone");
        _swipeRefreshLayout.setRefreshing(false);
    }

    public void onWishTapped(WishItem item) {
        Log.d(TAG, "onWishTapped");
        Intent i = new Intent(WishList.this, MyWishDetail.class);
        i.putExtra("item_id", item.getId());
        i.putExtra("position", 0);
        startActivityForResult(i, ITEM_DETAILS);
    }

    public void onWishLongTapped() {
        Log.d(TAG, "onWishLongTap");
        mActionMode = startSupportActionMode(mActionModeCallback);
    }
}

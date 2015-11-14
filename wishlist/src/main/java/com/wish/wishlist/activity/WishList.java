package com.wish.wishlist.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.support.v7.widget.SearchView;
import android.widget.TextView;
import android.widget.ViewFlipper;
import android.support.design.widget.NavigationView;

import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback;
import com.bignerdranch.android.multiselector.MultiSelector;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.parse.ParseUser;
import com.squareup.otto.Subscribe;
import com.wish.wishlist.R;
import com.wish.wishlist.db.TagItemDBManager;
import com.wish.wishlist.event.ProfileChangeEvent;
import com.wish.wishlist.model.ItemNameComparator;
import com.wish.wishlist.model.ItemPriceComparator;
import com.wish.wishlist.model.ItemTimeComparator;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.model.WishItemManager;
import com.wish.wishlist.WishlistApplication;
import com.wish.wishlist.event.EventBus;
import com.wish.wishlist.util.Options;
import com.wish.wishlist.util.WishAdapter;
import com.wish.wishlist.util.WishAdapterGrid;
import com.wish.wishlist.util.WishAdapterList;
import com.wish.wishlist.util.camera.CameraManager;
import com.wish.wishlist.util.sync.SyncAgent;
import com.wish.wishlist.widgets.ItemDecoration;

import org.apache.commons.lang3.ArrayUtils;

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
public class WishList extends ActivityBase implements
        SyncAgent.OnSyncWishChangedListener,
        SyncAgent.OnDownloadWishDoneListener,
        WishAdapter.onWishTapListener,
        WishAdapter.onWishLongTapListener {
    static final private int DIALOG_MAIN = 0;
    static final private int DIALOG_FILTER = 1;
    static final private int DIALOG_SORT = 2;
    static final private int POST_ITEM = 3;
    private static final String SELECTED_INDEX_KEY = "SELECTED_INDEX_KEY";


    private java.util.Map _where = new HashMap<>();
    private String _nameQuery = null;
    public static final String LOG_TAG = "WishList";

    private static final int EDIT_ITEM = 0;
    private static final int ADD_ITEM = 1;
    private static final int FIND_TAG = 2;
    private static final int ADD_TAG = 3;
    private static final int ITEM_DETAILS = 4;
    private static final int TAKE_PICTURE = 5;

    private static final String TAG = "wishlist";
    private static final String MULTI_SELECT_STATE = "multi_select_state";
    private static final String SELECTED_ITEM_IDS = "selected_item_ids";

    private Options.View _view = new Options.View(Options.View.LIST);
    private Options.Status _status = new Options.Status(Options.Status.ALL);
    private Options.Tag _tag = new Options.Tag(null);
    private Options.Sort _sort = new Options.Sort(Options.Sort.NAME);

    private String _fullsizePhotoPath = null;
    private String _newfullsizePhotoPath = null;

    private ViewFlipper _viewFlipper;
    private SwipeRefreshLayout _swipeRefreshLayout;
    private Button _addNew;
    private MenuItem _menuSearch;
    private Menu _menu;
    private ArrayList<Long> _itemIds = new ArrayList<>();
    private MultiSelector mMultiSelector = new MultiSelector();
    // Set up toolbar action mode. This mode is activated when an item is long tapped and user can then select
    // multiple items for an action
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
            if (_view.val() == Options.View.LIST) {
                mWishAdapterList.clearSelectedItemIds();

                // notifyDataSetChanged will fix a bug in recyclerview-multiselect lib, where the selected item's state does
                // not get cleared when the action mode is finished.
                mWishAdapterList.notifyDataSetChanged();
            } else {
                mWishAdapterGrid.clearSelectedItemIds();
                mWishAdapterGrid.notifyDataSetChanged();
            }
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

    protected RecyclerView mRecyclerView;
    protected LinearLayoutManager mLinearLayoutManager;
    protected StaggeredGridLayoutManager mStaggeredGridLayoutManager;
    private WishAdapterList mWishAdapterList;
    private WishAdapterGrid mWishAdapterGrid;
    private List<WishItem> mWishlist;
    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    private View mNavigationViewHeader;
    private ActionBarDrawerToggle mDrawerToggle;
    private RelativeLayout mHeaderLayout;

    private static final int WISH_VIEW = 0;
    private static final int MAKE_A_WISH_VIEW = 1;
    private static final int NO_MATCHING_WISH_VIEW = 2;

    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Listen for profile change events
        EventBus.getInstance().register(this);

        setContentView(R.layout.main);
        setupActionBar(R.id.main_toolbar);
        updateActionBarTitle();
        setupNavigationDrawer();

        _view.read();
        Tracker t = ((WishlistApplication) getApplication()).getTracker(WishlistApplication.TrackerName.APP_TRACKER);
        if (_view.val() == Options.View.LIST) {
            t.setScreenName("ListView");
        } else {
            t.setScreenName("GridView");
        }
        t.send(new HitBuilders.AppViewBuilder().build());

        _status.read();
        if (_status.val() == Options.Status.ALL) {
            _where.clear();
        } else if(_status.val() == Options.Status.COMPLETED) {
            _where.put("complete", "1");
        } else if(_status.val() == Options.Status.IN_PROGRESS) {
            _where.put("complete", "0");
        }

        _sort.read();
        _tag.read();
        if (_tag.val() != null) {
            _itemIds = TagItemDBManager.instance().ItemIds_by_tag(_tag.val());
        }

        // Get the intent, verify the action and get the query
        _viewFlipper = (ViewFlipper) findViewById(R.id.myFlipper);

        _addNew = (Button) findViewById(R.id.addNewWishButton);
        _addNew.setOnClickListener(new android.view.View.OnClickListener() {
            public void onClick(android.view.View v) {
                Intent editItem = new Intent(WishList.this, EditItem.class);
                startActivityForResult(editItem, ADD_ITEM);
            }
        });

        if (savedInstanceState != null) {
            Log.d(WishList.LOG_TAG, "savedInstanceState != null");
            // restore the current selected item in the list
            _newfullsizePhotoPath = savedInstanceState.getString("newfullsizePhotoPath");
            _fullsizePhotoPath = savedInstanceState.getString("fullsizePhotoPath");

            Log.d(WishList.LOG_TAG, "_newfullsizePhotoPath " + _newfullsizePhotoPath);
            Log.d(WishList.LOG_TAG, "_fullsizePhotoPath " + _fullsizePhotoPath);
        } else{
            Log.d(WishList.LOG_TAG, "savedInstanceState == null");
        }

        SyncAgent.getInstance().register(this);

        _swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
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
        // sets the colors used in the refresh animation
        //_swipeRefreshLayout.setColorSchemeResources(R.color.blue_bright, R.color.green_light,
        //R.color.orange_light, R.color.red_light);

        mRecyclerView = (RecyclerView) findViewById(R.id.wish_recycler_view);

        // only enable swipe down refresh when the first item in recycler view is visible
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                int topRowVerticalPosition =
                        (recyclerView == null || recyclerView.getChildCount() == 0) ? 0 : recyclerView.getChildAt(0).getTop();
                _swipeRefreshLayout.setEnabled(topRowVerticalPosition >= 0);
            }
        });

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        //mRecyclerView.setHasFixedSize(true);

        mLinearLayoutManager = new LinearLayoutManager(this);
        mStaggeredGridLayoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);

        // Fixme use dp and covert to px
        mRecyclerView.addItemDecoration(new ItemDecoration(10 /*px*/));

        // restore multi-select state when activity is re-created due to, for example, screen orientation
        if (mMultiSelector != null) {
            // restore selected item state
            if (savedInstanceState != null) {
                mMultiSelector.restoreSelectionStates(savedInstanceState.getBundle(MULTI_SELECT_STATE));
            }
            if (mMultiSelector.isSelectable()) {
                if (mActionModeCallback != null) {
                    mActionModeCallback.setClearOnPrepare(false);
                    startSupportActionMode(mActionModeCallback);
                }
            }
        }

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        // check if the activity is started from search
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            MenuItemCompat.collapseActionView(_menuSearch);
            // activity is started from search, get the search query and
            // displayed the searched items
            _nameQuery = intent.getStringExtra(SearchManager.QUERY);
            MenuItem tagItem =  _menu.findItem(R.id.menu_tags);
            MenuItemCompat.collapseActionView(tagItem);

            MenuItem statusItem = _menu.findItem(R.id.menu_status);
            MenuItemCompat.collapseActionView(statusItem);
        } else {
            // activity is not started from search
            // display all the items saved in the Item table
            // sorted by item name
            initializeView();
        }
    }

    /***
     * initial display of items in both list and grid view, called when the
     * activity is created
     */
    private void initializeView() {
        mWishlist = WishItemManager.getInstance().getItems(_sort.toString(), _where, _itemIds);
        if (mWishlist.isEmpty() && (!_where.isEmpty() || !_itemIds.isEmpty() || _nameQuery != null)) {
            // no matching wishes text
            _viewFlipper.setDisplayedChild(NO_MATCHING_WISH_VIEW);
            return;
        }
        if (mWishlist.isEmpty()) {
            // make a new wish button
            _viewFlipper.setDisplayedChild(MAKE_A_WISH_VIEW);
            return;
        }

        if (_view.val() == Options.View.LIST) {
            updateListView();
        } else {
            updateStaggeredView();
        }
        _viewFlipper.setDisplayedChild(WISH_VIEW);
    }

    /***
     * display the items in either list or grid view sorted by "sortBy"
     *
     * @param searchName
     *            : the item name to match, null for all items
     */
    private void populateItems(String searchName, java.util.Map where) {
        if (searchName == null) {
            // Get all of the rows from the Item table
            // Keep track of the TextViews added in list lstTable
            mWishlist = WishItemManager.getInstance().getItems(_sort.toString(), where, _itemIds);
        } else {
            mWishlist = WishItemManager.getInstance().searchItems(searchName, _sort.toString());
        }
        itemListChanged();
    }

    private void itemListChanged() {
        updateView();
        updateDrawerList();
        updateActionBarTitle();
    }

    /***
     * update either list view or grid view according view option
     */
    private void updateView() {
        //_wishItemCursor.requery();
        if (mWishlist.isEmpty() && (!_where.isEmpty() || !_itemIds.isEmpty() || _nameQuery != null)) {
            // no matching wishes text
            _viewFlipper.setDisplayedChild(NO_MATCHING_WISH_VIEW);
            return;
        }
        if (mWishlist.isEmpty()) {
            // make a new wish button
            _viewFlipper.setDisplayedChild(MAKE_A_WISH_VIEW);
            return;
        }
        if (_view.val() == Options.View.LIST) {
            updateListView();
        } else if (_view.val() == Options.View.GRID) {
            updateStaggeredView();
        }
        if (_viewFlipper.getDisplayedChild() != WISH_VIEW) {
            _viewFlipper.setDisplayedChild(WISH_VIEW);
        }
    }

    private void updateStaggeredView() {
        mRecyclerView.setLayoutManager(mStaggeredGridLayoutManager);
        if (mWishAdapterGrid == null) {
            mWishAdapterGrid = new WishAdapterGrid(mWishlist, this, mMultiSelector);
            mRecyclerView.setAdapter(mWishAdapterGrid);
        } else {
            mWishAdapterGrid.setWishList(mWishlist);
        }
    }

    private void switchToStaggeredView() {
        mRecyclerView.setAdapter(null);
        mRecyclerView.setLayoutManager(mStaggeredGridLayoutManager);
        if (mWishAdapterGrid == null) {
            mWishAdapterGrid = new WishAdapterGrid(mWishlist, this, mMultiSelector);
        } else {
            mWishAdapterGrid.setWishList(mWishlist);
        }
        mRecyclerView.setAdapter(mWishAdapterGrid);
    }

    private void updateListView() {
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        if (mWishAdapterList == null) {
            mWishAdapterList = new WishAdapterList(mWishlist, this, mMultiSelector);
            mRecyclerView.setAdapter(mWishAdapterList);
        } else {
            mWishAdapterList.setWishList(mWishlist);
        }
    }

    private void switchToListView() {
        mRecyclerView.setAdapter(null);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        if (mWishAdapterList == null) {
            mWishAdapterList = new WishAdapterList(mWishlist, this, mMultiSelector);
        } else {
            mWishAdapterList.setWishList(mWishlist);
        }
        mRecyclerView.setAdapter(mWishAdapterList);
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
                if (mWishAdapterList != null) {
                    mWishAdapterList.notifyItemChanged(i, item);
                }
                if (mWishAdapterGrid != null) {
                    mWishAdapterGrid.notifyItemChanged(i, item);
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
                if (_view.val() == Options.View.LIST) {
                    mWishAdapterList.removeByItemIds(item_ids);
                } else if (_view.val() == Options.View.GRID) {
                    mWishAdapterGrid.removeByItemIds(item_ids);
                }

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
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        _menu = menu;

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        _menuSearch = menu.findItem(R.id.menu_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(_menuSearch);
        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default

        // Style the searchView with yellow accent color
        //int searchPlateId = searchView.getContext().getResources().getIdentifier("android:id/search_plate", null, null);
        // Getting the 'search_plate' LinearLayout.
        //android.view.View searchPlate = searchView.findViewById(searchPlateId);
        // Setting background of 'search_plate'.
        //searchPlate.setBackgroundResource(R.drawable.textfield_searchview_yellow);

        //int closeButtonId = searchView.getContext().getResources().getIdentifier("android:id/search_close_btn", null, null);
        //ImageView closeButton= (ImageView) searchView.findViewById(closeButtonId);
        //closeButton.setBackgroundResource(R.drawable.selectable_background_wishlist);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        return true;
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
    protected Dialog onCreateDialog(int id) {
        AlertDialog dialog;
        switch (id) {
            case DIALOG_MAIN:
                dialog = null;
                break;
            case DIALOG_SORT:
                final String BY_NAME = "By name";
                final String BY_TIME = "By time";
                final String BY_PRICE = "By price";
                final CharSequence[] sortOption = {BY_NAME, BY_TIME, BY_PRICE};

                AlertDialog.Builder sortBuilder = new AlertDialog.Builder(WishList.this, R.style.AppCompatAlertDialogStyle);
                sortBuilder.setTitle("Sort wishes");

                int j = 0;// 0 is by name
                if (_sort.val() == Options.Sort.UPDATED_TIME) {
                    j = 1;
                } else if (_sort.val() == Options.Sort.PRICE) {
                    j = 2;
                } sortBuilder.setSingleChoiceItems(sortOption, j, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        if (sortOption[item].equals(BY_NAME)) {
                            _sort.setVal(Options.Sort.NAME);
                            Collections.sort(mWishlist, new ItemNameComparator());
                        } else if (sortOption[item].equals(BY_TIME)) {
                            _sort.setVal(Options.Sort.UPDATED_TIME);
                            Collections.sort(mWishlist, new ItemTimeComparator());
                        } else {
                            _sort.setVal(Options.Sort.PRICE);
                            Collections.sort(mWishlist, new ItemPriceComparator());
                        }
                        _sort.save();

                        dialog.dismiss();
                        itemListChanged();
                    }
                });

                dialog = sortBuilder.create();
                break;

            case DIALOG_FILTER:
                final String BY_ALL = "All";
                final String BY_COMPLETED = "Completed";
                final String BY_INPROGRESS = "In progress";
                final CharSequence[] options = {BY_ALL, BY_COMPLETED, BY_INPROGRESS};

                AlertDialog.Builder optionBuilder = new AlertDialog.Builder(WishList.this, R.style.AppCompatAlertDialogStyle);
                optionBuilder.setTitle("Wish status");

                optionBuilder.setSingleChoiceItems(options, _status.val(), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {

                        if (options[item].equals(BY_ALL)) {
                            _where.clear();
                            _status.setVal(Options.Status.ALL);
                        } else if (options[item].equals(BY_COMPLETED)) {
                            _where.put("complete", "1");
                            _status.setVal(Options.Status.COMPLETED);
                        } else {
                            _where.put("complete", "0");
                            _status.setVal(Options.Status.IN_PROGRESS);
                        }
                        _status.save();

                        dialog.dismiss();
                        populateItems(null, _where);
                    }
                });

                dialog = optionBuilder.create();
                break;
            default:
                dialog = null;
        }
        return dialog;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        // save the multi-select state, so we can restore them
        savedInstanceState.putBundle(MULTI_SELECT_STATE, mMultiSelector.saveSelectionStates());
        long[] itemIds = new long[selectedItemIds().size()];
        for (int i=0; i< selectedItemIds().size(); i++) {
            itemIds[i] = selectedItemIds().get(i);
        }
        savedInstanceState.putLongArray(SELECTED_ITEM_IDS, itemIds);
        // save the position of the currently selected item in the list
        if (_view.val() == Options.View.LIST) {
            //savedInstanceState.putInt(SELECTED_INDEX_KEY, _listView.getSelectedItemPosition());
        }
        else {
            //savedInstanceState.putInt(SELECTED_INDEX_KEY, _gridView.getSelectedItemPosition());
        }
        savedInstanceState.putString("newfullsizePhotoPath", _newfullsizePhotoPath);
        savedInstanceState.putString("fullsizePhotoPath", _fullsizePhotoPath);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState == null) {
            return;
        }

        // restore selected items
        long[] itemIds = savedInstanceState.getLongArray(SELECTED_ITEM_IDS);
        Long[] itemIdsLong = ArrayUtils.toObject(itemIds);
        setSelectedItemIds(java.util.Arrays.asList(itemIdsLong));

        // restore the current selected item in the list
        int pos = -1;
        if (savedInstanceState.containsKey(SELECTED_INDEX_KEY)) {
            pos = savedInstanceState.getInt(SELECTED_INDEX_KEY, -1);
        }
        _newfullsizePhotoPath = savedInstanceState.getString("newfullsizePhotoPath");
        _fullsizePhotoPath = savedInstanceState.getString("fullsizePhotoPath");

        //_listView.setSelection(pos);
        //_gridView.setSelection(pos);

        updateView();
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
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getInstance().unregister(this);
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
                }
                else {

                }
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
                    Log.d(WishList.LOG_TAG, "TAKE_PICTURE: RESULT_OK");
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
                    Log.d(WishList.LOG_TAG, "TAKE_PICTURE: not RESULT_OK");
                }
                break;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // When we navigate to another activity and navigate back to the wishlist activity, the wishes could have been changed,
        // so we need to reload the list.

        // Exmaples:
        // 1. tap a wish to open wishitemdetail view -> edit the wish and save it, or delete the wish -> tap back button
        // 2. add a new wish -> done -> show wishitemdetail -> back
        // 3. filter by tag -> findtag view -> tap a tag
        // ...

        // If we search a wish by name, onResume will also be called.


        // If we are still in this activity but are changing the list by interacting with a dialog like sort, status, we need to
        // explicitly reload the list, as in these cases, onResume won't be called.

        populateItems(_nameQuery, _where);
        updateDrawerList();
        updateActionBarTitle();
    }

    private Boolean goBack()
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

            populateItems(null, _where);
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

            populateItems(null, _where);
        }
        else {
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

    private void updateActionBarTitle() {
        if (_nameQuery != null) {
            // we are showing search results
            getSupportActionBar().setTitle("Search: " + _nameQuery);
            getSupportActionBar().setSubtitle(null);
            return;
        }

        if (_tag.val() == null) {
            getSupportActionBar().setTitle(R.string.app_name);
        } else {
            getSupportActionBar().setTitle(_tag.val());
        }

        if (_status.val() == Options.Status.COMPLETED) {
            getSupportActionBar().setSubtitle("Completed");
        } else if (_status.val() == Options.Status.IN_PROGRESS) {
            getSupportActionBar().setSubtitle("In progress");
        } else {
            getSupportActionBar().setSubtitle(null);
        }
    }

    private void updateDrawerList() {
        Menu menuNav = mNavigationView.getMenu();
        MenuItem item  = menuNav.findItem(R.id.all_wishes);
        if (!_itemIds.isEmpty() || _status.val() != Options.Status.ALL || _nameQuery != null) {
            item.setVisible(true);
        } else {
            item.setVisible(false);
        }
    }

    @Subscribe
    public void profileChanged(ProfileChangeEvent event) {
        Log.d(TAG, "profileChanged " + event.type.toString());
        if (event.type == ProfileChangeEvent.ProfileChangeType.email) {
            setupUserEmail();
        } else if (event.type == ProfileChangeEvent.ProfileChangeType.name) {
            setupUserName();
        } else if (event.type == ProfileChangeEvent.ProfileChangeType.image) {
            setupProfileImage();
        }
    }

    private void setupUserEmail() {
        TextView emailTextView = (TextView) mHeaderLayout.findViewById(R.id.email);
        emailTextView.setText(ParseUser.getCurrentUser().getEmail());
    }

    private void setupUserName() {
        TextView nameTextView = (TextView) mHeaderLayout.findViewById(R.id.username);
        nameTextView.setText(ParseUser.getCurrentUser().getString("name"));
    }

    private void setupProfileImage() {
        // set profile image in the header
        final File profileImageFile = new File(getFilesDir(), Profile.profileImageName());
        final Bitmap bitmap = BitmapFactory.decodeFile(profileImageFile.getAbsolutePath());
        final ImageView profileImageView = (ImageView) mNavigationViewHeader.findViewById(R.id.profile_image);
        if (bitmap != null) {
            profileImageView.setImageBitmap(bitmap);
        }
    }

    private void setupNavigationDrawer() {
        // Setup NavigationView
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mNavigationView = (NavigationView) findViewById(R.id.navigation_view);
        mNavigationViewHeader = mNavigationView.inflateHeaderView(R.layout.navigation_drawer_header);
        mHeaderLayout = (RelativeLayout) mNavigationViewHeader.findViewById(R.id.drawer_header_layout);
        final ParseUser currentUser = ParseUser.getCurrentUser();
        mHeaderLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentUser != null) {
                    startActivity(new Intent(getApplication(), Profile.class));
                } else {
                    startActivity(new Intent(getApplication(), UserLoginActivity.class));
                }
            }
        });

        if (currentUser != null) {
            setupUserName();
            setupUserEmail();
        }
        setupProfileImage();

        updateDrawerList();
        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            // This method will trigger on item Click of navigation menu
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                //Closing drawer on item click
                mDrawerLayout.closeDrawers();

                //Check to see which item was being clicked and perform appropriate action
                switch (menuItem.getItemId()){
                    case R.id.all_wishes:
                        goBack();
                        return true;
                    case R.id.Add:
                        Intent editItem = new Intent(WishList.this, EditItem.class);
                        startActivityForResult(editItem, ADD_ITEM);
                        return true;
                    case R.id.list_view: {
                        if (_view.val() == Options.View.LIST) {
                            return true;
                        }
                        switchToListView();
                        _view.setVal(Options.View.LIST);
                        _view.save();

                        Tracker t = ((WishlistApplication) getApplication()).getTracker(WishlistApplication.TrackerName.APP_TRACKER);
                        t.setScreenName("ListView");
                        t.send(new HitBuilders.AppViewBuilder().build());
                        return true;
                    }
                    case R.id.grid_view:
                        if (_view.val() == Options.View.GRID) {
                            return true;
                        }
                        switchToStaggeredView();
                        _view.setVal(Options.View.GRID);
                        _view.save();

                        Tracker t = ((WishlistApplication) getApplication()).getTracker(WishlistApplication.TrackerName.APP_TRACKER);
                        t.setScreenName("GridView");
                        t.send(new HitBuilders.AppViewBuilder().build());
                        return true;
                    case R.id.map_view:
                        Intent mapIntent = new Intent(WishList.this, Map.class);
                        mapIntent.putExtra("type", "markAll");
                        startActivity(mapIntent);
                        return true;
                    case R.id.settings:
                        Intent prefIntent = new Intent(getApplicationContext(), WishListPreference.class);
                        startActivity(prefIntent);
                        return true;
                    case R.id.friends:
                        final Intent friendsIntent = new Intent(getApplicationContext(), Friends.class);
                        startActivity(friendsIntent);
                        return true;
                    case R.id.notifications:
                        final Intent friendRequestIntent = new Intent(getApplicationContext(), FriendRequest.class);
                        startActivity(friendRequestIntent);
                        return true;
                    default:
                        return true;
                }
            }
        });

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, R.string.drawer_open, R.string.drawer_close) {

            @Override
            public void onDrawerClosed(View drawerView) {
            /** Called when a drawer has settled in a completely closed state. */
                super.onDrawerClosed(drawerView);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerOpened(View drawerView) {
            /** Called when a drawer has settled in a completely open state. */
                mDrawerLayout.requestFocus();
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        // Calling sync state is needed or the hamburger icon wont show up
        mDrawerToggle.syncState();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

//    @Override
//    public boolean onItemLongClick(AdapterView<?> parent, android.view.View view, int position, long id)
//    {
//        _selectedItem_id = id;
//        if (_view.val() == Options.View.LIST) {
//            _listView.showContextMenu();
//        } else {
//            _staggeredView.showContextMenu();
//        }
//        return true;
//    }

    private ArrayList<Long> selectedItemIds() {
        if (_view.val() == Options.View.LIST) {
            return mWishAdapterList.selectedItemIds();
        } else {
            return mWishAdapterGrid.selectedItemIds();
        }
    }

    private void setSelectedItemIds(List<Long> itemIds) {
        if (_view.val() == Options.View.LIST) {
            mWishAdapterList.setSelectedItemIds(itemIds);
        } else {
            mWishAdapterGrid.setSelectedItemIds(itemIds);
        }
    }

    public void onSyncWishChanged() {
        Log.d(TAG, "onSyncWishChanged");
        populateItems(_nameQuery, _where);
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
        startSupportActionMode(mActionModeCallback);
    }
}

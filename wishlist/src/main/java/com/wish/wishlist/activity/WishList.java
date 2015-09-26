package com.wish.wishlist.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import android.support.design.widget.NavigationView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.parse.ParseUser;
import com.wish.wishlist.R;
import com.wish.wishlist.db.ItemDBManager;
import com.wish.wishlist.db.ItemDBManager.ItemsCursor;
import com.wish.wishlist.db.TagItemDBManager;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.model.WishItemManager;
import com.wish.wishlist.WishlistApplication;
import com.wish.wishlist.util.Options;
import com.wish.wishlist.util.WishItemStaggeredCursorAdapter;
import com.wish.wishlist.util.WishListItemCursorAdapter;
import com.wish.wishlist.util.camera.CameraManager;
import com.wish.wishlist.util.social.ShareHelper;
import com.wish.wishlist.util.DialogOnShowListener;
import com.wish.wishlist.util.sync.SyncAgent;

import com.etsy.android.grid.StaggeredGridView;

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
@SuppressLint("NewApi")
public class WishList extends ActionBarActivity implements
        AbsListView.OnScrollListener,
        AbsListView.OnItemClickListener,
        AdapterView.OnItemLongClickListener,
        SyncAgent.OnSyncWishChangedListener,
        SyncAgent.OnDownloadWishDoneListener {
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

    private Options.View _view = new Options.View(Options.View.LIST);
    private Options.Status _status = new Options.Status(Options.Status.ALL);
    private Options.Tag _tag = new Options.Tag(null);
    private Options.Sort _sort = new Options.Sort(Options.Sort.NAME);

    private String _fullsizePhotoPath = null;
    private String _newfullsizePhotoPath = null;

    private ViewFlipper _viewFlipper;
    private ListView _listView;
    private GridView _gridView;
    private StaggeredGridView _staggeredView;
    private SwipeRefreshLayout _swipeRefreshLayout;
    private Button _addNew;
    private MenuItem _menuSearch;

    private Menu _menu;

    private ItemsCursor _wishItemCursor;
    private WishListItemCursorAdapter _wishListItemAdapterCursor;
    private WishItemStaggeredCursorAdapter _wishItemStaggeredAdapterCursor;

    private ItemDBManager _itemDBManager;
    private ArrayList<Long> _itemIds = new ArrayList<>();

    private long _selectedItem_id;

    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    private ActionBarDrawerToggle mDrawerToggle;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        setContentView(R.layout.main);

        setUpActionBar();
        setupNavigationDrawer();

        // get the resources by their IDs
        _viewFlipper = (ViewFlipper) findViewById(R.id.myFlipper);
        _staggeredView = (StaggeredGridView) findViewById(R.id.staggered_view);
        _staggeredView.setOnScrollListener(this);
        _staggeredView.setOnItemClickListener(this);
        _staggeredView.setOnItemLongClickListener(this);

        _listView = (ListView) findViewById(R.id.myListView);
        _listView.setOnScrollListener(this);
        _listView.setOnItemClickListener(this);
        _listView.setOnItemLongClickListener(this);

        _gridView = (GridView) findViewById(R.id.myGridView);
        _gridView.setNumColumns(3);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            _gridView.setNumColumns(4);
        }
        _gridView.setOnItemClickListener(this);

        _addNew = (Button) findViewById(R.id.addNewWishButton);
        _addNew.setOnClickListener(new android.view.View.OnClickListener() {
            public void onClick(android.view.View v) {
                Intent editItem = new Intent(WishList.this, EditItem.class);
                startActivityForResult(editItem, ADD_ITEM);
            }
        });

        // register context menu for both listview and gridview
        registerForContextMenu(_listView);
        registerForContextMenu(_gridView);
        registerForContextMenu(_staggeredView);

        _itemDBManager = new ItemDBManager();

        handleIntent(getIntent());

        // set the spinner for switching between list and grid views
//		ArrayAdapter<CharSequence> adapter = ArrayAdapter
//				.createFromResource(this, R.array.views_array,
//						android.R.layout.simple_spinner_item);

//		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//		myViewSpinner.setAdapter(adapter);

//		// set the default spinner option
//		if (_view == "list") {
//			myViewSpinner.setSelection(0);
//		} else {
//			myViewSpinner.setSelection(1);
//		}
//
//		myViewSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
//			@Override
//			public void onItemSelected(AdapterView<?> parent, View view,
//					int pos, long id) {
//
//				// list view is selected
//				if (pos == 0) {
//					// Recall populate here is inefficient
//					_view = "list";
//					populateItems(_nameQuery, SORT_BY);
//					_viewFlipper.setDisplayedChild(0);
//
//				}
//				// grid view is selected
//				else if (pos == 1) {
//					_view = "grid";
//					populateItems(_nameQuery, SORT_BY);
//					_viewFlipper.setDisplayedChild(1);
//
//				}
//				// Toast.makeText(parent.getContext(), "The view is " +
//				// parent.getItemAtPosition(pos).toString(),
//				// Toast.LENGTH_LONG).show();
//			}
//
//			@Override
//			public void onNothingSelected(AdapterView parent) {
//				// Do nothing.
//			}
//		});
        if (savedInstanceState != null) {
            Log.d(WishList.LOG_TAG, "savedInstanceState != null");
            // restore the current selected item in the list
            _newfullsizePhotoPath = savedInstanceState.getString("newfullsizePhotoPath");
            _fullsizePhotoPath = savedInstanceState.getString("fullsizePhotoPath");

            Log.d(WishList.LOG_TAG, "_newfullsizePhotoPath " + _newfullsizePhotoPath);
            Log.d(WishList.LOG_TAG, "_fullsizePhotoPath " + _fullsizePhotoPath);
        }
        else{
            Log.d(WishList.LOG_TAG, "savedInstanceState == null");
        }

        SyncAgent.getInstance().register(this);

        _swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        // the refresh listner. this would be called when the layout is pulled down
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
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        // check if the activity is started from search
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            _menuSearch.collapseActionView();
            // activity is started from search, get the search query and
            // displayed the searched items
            _nameQuery = intent.getStringExtra(SearchManager.QUERY);
            MenuItem tagItem =  _menu.findItem(R.id.menu_tags);
            tagItem.setVisible(false);

            MenuItem statusItem = _menu.findItem(R.id.menu_status);
            statusItem.setVisible(false);

            // This is a HACK to fix the bug:
            // If we have scrolled to the middle of the staggered view, and then sort,
            // the items will be positioned incorrectly. Calling resetToTop() will somehow refresh the
            // the view and layout the items correctly.
            _staggeredView.resetToTop();
        } else {
            // activity is not started from search
            // display all the items saved in the Item table
            // sorted by item name
            initializeView();
        }
    }

    @Override
    public boolean onSearchRequested() {
        return super.onSearchRequested();
    }

    /***
     * initial display of items in both list and grid view, called when the
     * activity is created
     */
    private void initializeView() {
        _wishItemCursor = _itemDBManager.getItems(_sort.toString(), _where, _itemIds);
        if (_itemDBManager.getItemsCount() == 0) {
            // make a new wish button
            _viewFlipper.setDisplayedChild(3);
            return;
        }
        if (_wishItemCursor.getCount() == 0 && (!_where.isEmpty() || !_itemIds.isEmpty() || _nameQuery != null)) {
            // no matching wishes text
            _viewFlipper.setDisplayedChild(4);
            return;
        }

        if (_view.val() == Options.View.LIST) {
            updateListView();
            _viewFlipper.setDisplayedChild(1);
        }
        else {
            //updateGridView();
            //_viewFlipper.setDisplayedChild(2);
            updateStaggeredView();
            _viewFlipper.setDisplayedChild(0);
        }
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
            // _wishItemCursor = wishListDB.getItems(sortBy);
            _wishItemCursor = _itemDBManager.getItems(_sort.toString(), where, _itemIds);
        } else {
            _wishItemCursor = _itemDBManager.searchItems(searchName, _sort.toString());
        }

        updateView();
        updateDrawerList();
        updateActionBarTitle();
    }

    /***
     * update either list view or grid view according view option
     */
    private void updateView() {
        if (_itemDBManager.getItemsCount() == 0) {
            // make a new wish button
            _viewFlipper.setDisplayedChild(3);
            return;
        }

        _wishItemCursor.requery();
        if (_wishItemCursor.getCount() == 0 && (!_where.isEmpty() || !_itemIds.isEmpty() || _nameQuery != null)) {
            // no matching wishes text
            _viewFlipper.setDisplayedChild(4);
            return;
        }
        if (_view.val() == Options.View.LIST) {
            updateListView();
            _viewFlipper.setDisplayedChild(1);
        } else if (_view.val() == Options.View.GRID) {
            //updateGridView();
            //_viewFlipper.setDisplayedChild(1);
            updateStaggeredView();
            _viewFlipper.setDisplayedChild(0);
        }
    }

    private void updateStaggeredView() {
        String[] from = new String[]{
                ItemDBManager.KEY_FULLSIZE_PHOTO_PATH,
                ItemDBManager.KEY_NAME,
                ItemDBManager.KEY_DESCRIPTION,
                ItemDBManager.KEY_PRICE,
                ItemDBManager.KEY_COMPLETE};

        int[] to = new int[]{
                R.id.staggered_image,
                R.id.staggered_name,
                R.id.staggered_description,
                R.id.staggered_price,
                R.id.staggered_checkmark_complete};

        _wishItemStaggeredAdapterCursor = new WishItemStaggeredCursorAdapter(this,
                R.layout.staggeredview_item, _wishItemCursor, from, to);

        _staggeredView.setAdapter(_wishItemStaggeredAdapterCursor);
        _wishItemStaggeredAdapterCursor.notifyDataSetChanged();

    }

    private void updateGridView() {
        int resID = R.layout.wishitem_photo;
        String[] from = new String[]{ItemDBManager.KEY_PHOTO_URL};

        int[] to = new int[]{R.id.imgPhotoGrid};
        _wishListItemAdapterCursor = new WishListItemCursorAdapter(this,
                resID, _wishItemCursor, from, to);

        _gridView.setAdapter(_wishListItemAdapterCursor);
        _wishListItemAdapterCursor.notifyDataSetChanged();
    }

    private void updateListView() {
        int resID = R.layout.wishitem_single;
        String[] from = new String[] {
                ItemDBManager.KEY_FULLSIZE_PHOTO_PATH,
                ItemDBManager.KEY_NAME,
                ItemDBManager.KEY_PRICE,
                ItemDBManager.KEY_STORENAME,
                ItemDBManager.KEY_ADDRESS,
                ItemDBManager.KEY_COMPLETE};

        int[] to = new int[] {
                R.id.imgPhoto,
                R.id.txtName,
                R.id.txtPrice,
                R.id.txtStore,
                R.id.txtAddress,
                R.id.checkmark_complete};

        _wishListItemAdapterCursor = new WishListItemCursorAdapter(this,
                resID, _wishItemCursor, from, to);

        // save index and top position
        int index = _listView.getFirstVisiblePosition();
        android.view.View v = _listView.getChildAt(0);
        int top = (v == null) ? 0 : v.getTop();

        _listView.setAdapter(_wishListItemAdapterCursor);
        _wishListItemAdapterCursor.notifyDataSetChanged();

        // restore
        _listView.setSelectionFromTop(index, top);
    }

    private void deleteItem(long item_id){
        _selectedItem_id = item_id;
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        builder.setMessage("Delete the wish?");
        builder.setCancelable(false);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                WishItemManager.getInstance().deleteItemById(_selectedItem_id);
                updateView();
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
        //dialog.setOnShowListener(new DialogOnShowListener(this));
        dialog.show();
    }

    private void dispatchTakePictureIntent() {
        CameraManager c = new CameraManager();
        _newfullsizePhotoPath = c.getPhotoPath();
        startActivityForResult(c.getCameraIntent(), TAKE_PICTURE);
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        _menu = menu;

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//            //search view is part the action bar in honeycomeb and up
//            //Get the SearchView and set the searchable configuration
//            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
//            _menuSearch = menu.findItem(R.id.menu_search);
//            SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
//            // Assumes current activity is the searchable activity
//            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
//            searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
//
//            // Style the searchView with yellow accent color
//            int searchPlateId = searchView.getContext().getResources().getIdentifier("android:id/search_plate", null, null);
//            // Getting the 'search_plate' LinearLayout.
//            android.view.View searchPlate = searchView.findViewById(searchPlateId);
//            // Setting background of 'search_plate'.
//            searchPlate.setBackgroundResource(R.drawable.textfield_searchview_yellow);
//
//            int closeButtonId = searchView.getContext().getResources().getIdentifier("android:id/search_close_btn", null, null);
//            ImageView closeButton= (ImageView) searchView.findViewById(closeButtonId);
//            closeButton.setBackgroundResource(R.drawable.selectable_background_wishlist);
//        }
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, android.view.View v,
                                    ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_item_context, menu);

        WishItem wish_item = WishItemManager.getInstance().getItemById(_selectedItem_id);
        int complete = wish_item.getComplete();
        MenuItem mi = menu.findItem(R.id.COMPLETE);
        if (complete == 1) {
            mi.setTitle("Mark as incomplete");
        } else {
            mi.setTitle("Mark as complete");
        }
        return;
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
            //Intent editItem = new Intent(this, EditItem.class);
            //startActivityForResult(editItem, ADD_ITEM);
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
    public boolean onContextItemSelected(MenuItem item) {
        super.onContextItemSelected(item);

        long itemId = item.getItemId();
        if (itemId == R.id.REMOVE) {
            deleteItem(_selectedItem_id);
            return true;
        }
        else if (itemId == R.id.EDIT) {
            Intent editItem = new Intent(this, EditItem.class);
            editItem.putExtra("item_id", _selectedItem_id);
            startActivityForResult(editItem, EDIT_ITEM);
            return true;
        }
        //	else if (itemId == R.id.POST): {
        //		Intent snsIntent = new Intent(this, WishItemPostToSNS.class);
        //		snsIntent.putExtra("wishItem", "test");
        //		startActivityForResult(snsIntent, POST_ITEM);
        //		return true;
        //	}
        else if (itemId == R.id.MARK) {
            WishItem wishItem = WishItemManager.getInstance().getItemById(_selectedItem_id);
            if (wishItem.getLatitude() == Double.MIN_VALUE && wishItem.getLongitude() == Double.MIN_VALUE) {
                Toast toast = Toast.makeText(this, "location unknown", Toast.LENGTH_SHORT);
                toast.show();
            } else {
                Intent mapIntent = new Intent(this, Map.class);
                mapIntent.putExtra("type", "markOne");
                mapIntent.putExtra("id", _selectedItem_id);
                startActivity(mapIntent);
            }
            return true;
        } else if (itemId == R.id.SHARE) {
            //Display display = getWindowManager().getDefaultDisplay();
            //int width = display.getWidth();  // deprecated
            //int height = display.getHeight();  // deprecated
            ShareHelper share = new ShareHelper(this, _selectedItem_id);
            share.share();
            //Intent sendIntent = new Intent();
            //sendIntent.setAction(Intent.ACTION_SEND);
            //sendIntent.putExtra(Intent.EXTRA_TEXT, message);
            //sendIntent.putExtra(Intent.EXTRA_STREAM, wish_item.getFullsizePicUri());
            //sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Subject");
            //sendIntent.setType("text/plain");
            //sendIntent.setType("*/*");
            //List<ResolveInfo> resInfo = getPackageManager().queryIntentActivities(sendIntent, 0);
            //startActivity(sendIntent);
            //Intent chooserIntent = Intent.createChooser(sendIntent, "Share using");
            //List<ResolveInfo> resInfo = getPackageManager().queryIntentActivities(chooserIntent, 0);
            //for (ResolveInfo info : resInfo) {
            //Log.d(LOG_TAG, "packageName " + info.activityInfo.packageName.toLowerCase());
            //Log.d(LOG_TAG, "name        " + info.activityInfo.name.toLowerCase());
            //}
            //startActivity(Intent.createChooser(sendIntent, "Share using"));
            return true;
        } else if (itemId == R.id.COMPLETE) {
            WishItem wish_item = WishItemManager.getInstance().getItemById(_selectedItem_id);
            if (wish_item.getComplete() == 1) {
                wish_item.setComplete(0);
                Tracker t = ((WishlistApplication) getApplication()).getTracker(WishlistApplication.TrackerName.APP_TRACKER);
                t.send(new HitBuilders.EventBuilder()
                        .setCategory("Wish")
                        .setAction("ChangeStatus")
                        .setLabel("InProgress")
                        .build());
            } else {
                wish_item.setComplete(1);
                Tracker t = ((WishlistApplication) getApplication()).getTracker(WishlistApplication.TrackerName.APP_TRACKER);
                t.send(new HitBuilders.EventBuilder()
                        .setCategory("Wish")
                        .setAction("ChangeStatus")
                        .setLabel("Complete")
                        .build());
            }
            wish_item.setUpdatedTime(System.currentTimeMillis());
            wish_item.setSyncedToServer(false);
            wish_item.save();
            updateView();
        } else if (itemId == R.id.TAG) {
            Intent i = new Intent(WishList.this, AddTag.class);
            i.putExtra(AddTag.ITEM_ID, _selectedItem_id);
            startActivityForResult(i, ADD_TAG);
        }
        return false; }

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
                        } else if (sortOption[item].equals(BY_TIME)) {
                            _sort.setVal(Options.Sort.UPDATED_TIME);
                        } else {
                            _sort.setVal(Options.Sort.PRICE);
                        }
                        _sort.save();

                        dialog.dismiss();
                        populateItems(_nameQuery, _where);

                        // This is a HACK to fix the bug:
                        // If we have scrolled to the middle of the staggered view, and then sort,
                        // the items will be positioned incorrectly. Calling resetToTop() will somehow refresh the
                        // the view and layout the items correctly.
                        _staggeredView.resetToTop();
                    }
                });

                dialog = sortBuilder.create();
                //dialog.setOnShowListener(new DialogOnShowListener(this));
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

                        // This is a HACK to fix the bug:
                        // If we have scrolled to the middle of the staggered view, and then filter wish by completed,
                        // the items will be positioned incorrectly. Calling resetToTop() will somehow refresh the
                        // the view and layout the items correctly.
                        _staggeredView.resetToTop();
                    }
                });

                dialog = optionBuilder.create();
                //dialog.setOnShowListener(new DialogOnShowListener(this));
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

        // save the position of the currently selected item in the list
        if (_view.val() == Options.View.LIST) {
            savedInstanceState.putInt(SELECTED_INDEX_KEY, _listView.getSelectedItemPosition());
        }
        else {
            savedInstanceState.putInt(SELECTED_INDEX_KEY, _gridView.getSelectedItemPosition());
        }
        savedInstanceState.putString("newfullsizePhotoPath", _newfullsizePhotoPath);
        savedInstanceState.putString("fullsizePhotoPath", _fullsizePhotoPath);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // restore the current selected item in the list
        int pos = -1;
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(SELECTED_INDEX_KEY)) {
                pos = savedInstanceState.getInt(SELECTED_INDEX_KEY, -1);
            }
            _newfullsizePhotoPath = savedInstanceState.getString("newfullsizePhotoPath");
            _fullsizePhotoPath = savedInstanceState.getString("fullsizePhotoPath");
        }

        _listView.setSelection(pos);
        _gridView.setSelection(pos);

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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//		IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
//		if (scanResult != null) {
//			Context context = getApplicationContext();
//			CharSequence text = scanResult.getContents();
//			int duration = Toast.LENGTH_SHORT;
//			Toast toast = Toast.makeText(context, text, duration);
//			toast.show();
//		}

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
                        Intent i = new Intent(WishList.this, WishItemDetail.class);
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

                        // This is a HACK to fix the bug:
                        // If we have scrolled to the middle of the staggered view, and then filter wish by tag,
                        // the items will be positioned incorrectly. Calling resetToTop() will somehow refresh the
                        // the view and layout the items correctly.
                        _staggeredView.resetToTop();
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

    private void setUpActionBar() {
        // Make sure we're running on Honeycomb or higher to use ActionBar APIs
        // Set a toolbar to replace the action bar.
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        updateActionBarTitle();
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

    private void setupNavigationDrawer() {
        // Setup NavigationView
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mNavigationView = (NavigationView) findViewById(R.id.navigation_view);
        RelativeLayout headerLayout = (RelativeLayout) mNavigationView.findViewById(R.id.drawer_header_layout);
        final ParseUser currentUser = ParseUser.getCurrentUser();
        headerLayout.setOnClickListener(new View.OnClickListener() {
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
            TextView nameTextView = (TextView) headerLayout.findViewById(R.id.username);
            nameTextView.setText(currentUser.getString("name"));

            TextView emailTextView = (TextView) headerLayout.findViewById(R.id.email);
            emailTextView.setText(currentUser.getEmail());
        }

        // show profile image in the header
        File profileImageFile = new File(getFilesDir(), Profile.profileImageName());
        Bitmap bitmap = BitmapFactory.decodeFile(profileImageFile.getAbsolutePath());
        ImageView profileImageView = (ImageView) mNavigationView.findViewById(R.id.profile_image);
        if (bitmap != null) {
            profileImageView.setImageBitmap(bitmap);
        } else {
            profileImageView.setImageResource(R.drawable.splash_logo);
        }

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
                        _view.setVal(Options.View.LIST);
                        populateItems(_nameQuery, _where);
                        _view.save();

                        Tracker t = ((WishlistApplication) getApplication()).getTracker(WishlistApplication.TrackerName.APP_TRACKER);
                        t.setScreenName("ListView");
                        t.send(new HitBuilders.AppViewBuilder().build());
                        return true;
                    }
                    case R.id.grid_view:
                        _view.setVal(Options.View.GRID);
                        populateItems(_nameQuery, _where);
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
                    default:
                        return true;
                }
            }
        });

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {

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

    @Override
    public void onScrollStateChanged(final AbsListView view, final int scrollState) {
        //       Log.d("wishlist", "onScrollStateChanged:" + scrollState);
    }

    @Override
    public void onScroll(final AbsListView view, final int firstVisibleItem, final int visibleItemCount, final int totalItemCount) {
        if (_swipeRefreshLayout == null) {
            return;
        }

        boolean enable = false;
        if (_view.val() == Options.View.LIST) {
            if (_listView != null && _listView.getChildCount() > 0) {
                // check if the first item of the list is visible
                boolean firstItemVisible = _listView.getFirstVisiblePosition() == 0;
                // check if the top of the first item is visible
                boolean topOfFirstItemVisible = _listView.getChildAt(0).getTop() >= 0;
                // enabling or disabling the refresh layout
                enable = firstItemVisible && topOfFirstItemVisible;
            }
        } else {
            if (_staggeredView != null && _staggeredView.getChildCount() > 0) {
                // check if the first item of the list is visible
                boolean firstItemVisible = _staggeredView.getFirstVisiblePosition() == 0;
                // check if the top of the first item is visible
                boolean topOfFirstItemVisible = _staggeredView.getChildAt(0).getTop() >= 0;
                // enabling or disabling the refresh layout
                enable = firstItemVisible && topOfFirstItemVisible;
            }
        }
        _swipeRefreshLayout.setEnabled(enable);
        //    Log.d("wishlist", "onScroll firstVisibleItem:" + firstVisibleItem +
        //            " visibleItemCount:" + visibleItemCount +
        //            " totalItemCount:" + totalItemCount);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, android.view.View view, int position, long id) {
        // Create an intent to show the item detail.
        // Pass the item_id along so the next activity can use it to
        // retrieve the info. about the item from database
        Intent i = new Intent(WishList.this, WishItemDetail.class);
        i.putExtra("item_id", id);
        i.putExtra("position", position);
        startActivityForResult(i, ITEM_DETAILS);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, android.view.View view, int position, long id)
    {
        _selectedItem_id = id;
        if (_view.val() == Options.View.LIST) {
            _listView.showContextMenu();
        } else {
            _staggeredView.showContextMenu();
        }
        return true;
    }

    public void onSyncWishChanged() {
        Log.d(TAG, "onSyncWishChanged");
        populateItems(_nameQuery, _where);
    }

    public void onDownloadWishDone() {
        Log.d(TAG, "onDownloadWishDone");
        _swipeRefreshLayout.setRefreshing(false);
    }
}

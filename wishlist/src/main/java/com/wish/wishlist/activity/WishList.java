package com.wish.wishlist.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.View.OnClickListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.wish.wishlist.R;
import com.wish.wishlist.db.ItemDBManager;
import com.wish.wishlist.db.ItemDBManager.ItemsCursor;
import com.wish.wishlist.db.TagItemDBManager;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.model.WishItemManager;
import com.wish.wishlist.util.DrawerItemCustomAdapter;
import com.wish.wishlist.util.WishListItemCursorAdapter;
import com.wish.wishlist.util.camera.CameraManager;
import com.wish.wishlist.util.social.ShareHelper;
import com.wish.wishlist.util.DialogOnShowListener;
import com.wish.wishlist.view.ObjectDrawerItem;

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
public class WishList extends Activity {
	static final private int DIALOG_MAIN = 0;
	static final private int DIALOG_FILTER = 1;
	static final private int DIALOG_SORT = 2;
	static final private int POST_ITEM = 3;

	private static final String SELECTED_INDEX_KEY = "SELECTED_INDEX_KEY";
	private static final String SORT_BY_KEY = "SORT_BY_KEY";
	private static final String PREF_VIEW_OPTION = "viewOption";
	private static final String PREF_FILTER_OPTION = "filterOption";
    private static final String PREF_TAG_OPTION = "tagOption";
	private static final String PREF_SORT_OPTION = "sortOption";

	private ItemsCursor.SortBy SORT_BY = ItemsCursor.SortBy.item_name;
	private Map<String,String> _where = new HashMap<String, String>();
	private String _nameQuery = null;
	public static final String LOG_TAG = "WishList";

	private static final int EDIT_ITEM = 0;
	private static final int ADD_ITEM = 1;
    private static final int FIND_TAG = 2;
    private static final int ADD_TAG = 3;
    private static final int ITEM_DETAILS = 4;
    private static final int TAKE_PICTURE = 5;

	private String _viewOption = "list";
	private String _statusOption = "all";
    private String _tagOption = null;
	private String _sortOption = ItemsCursor.SortBy.item_name.toString();

    private String _fullsizePhotoPath = null;
    private String _newfullsizePhotoPath = null;

	private ViewFlipper _viewFlipper;
	private ListView _listView;
	private GridView _gridView;
	private Button _addNew;
	private ImageButton _backImageButton;
	private ImageButton _viewImageButton;
	private ImageButton _searchImageButton;
    private MenuItem _menuSearch;

    private Menu _menu;

	private ItemsCursor _wishItemCursor;
	private WishListItemCursorAdapter _wishListItemAdapterCursor;

	private ItemDBManager _itemDBManager;
    private ArrayList<Long> _itemIds = new ArrayList<Long>();
	
	private long _selectedItem_id;

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        SharedPreferences pref = this.getPreferences(MODE_PRIVATE);
		_viewOption = pref.getString(PREF_VIEW_OPTION, "list");
        _statusOption = pref.getString(PREF_FILTER_OPTION, "all");

		if (_statusOption.equals("all")) {
			_where.clear();
		}
		else if(_statusOption.equals("completed")) {
			_where.put("complete", "1");
		}
		else if(_statusOption.equals("in_progress")) {
			_where.put("complete", "0");
		}

		_sortOption = pref.getString(PREF_SORT_OPTION, ItemsCursor.SortBy.item_name.toString());

        _tagOption = pref.getString(PREF_TAG_OPTION, null);
        if (_tagOption != null) {
            _itemIds = TagItemDBManager.instance(this).ItemIds_by_tag(_tagOption);
        }

		// Get the intent, verify the action and get the query
		Intent intent = getIntent();
		setContentView(R.layout.main);

		setUpActionBar();
        setupNavigationDrawer();


		// get the resources by their IDs
		_viewFlipper = (ViewFlipper) findViewById(R.id.myFlipper);
		_listView = (ListView) findViewById(R.id.myListView);
		_gridView = (GridView) findViewById(R.id.myGridView);
		_addNew = (Button) findViewById(R.id.addNewWishButton);
		// mySearchText = (EditText) findViewById(R.id.mySearchText);

		// Listener for _listView.
		// When clicked, it starts a new activity to display the clicked item's
		// detailed info.
		_listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> a, View v, int position,
					long id) {
				// find which item in the list view has been clicked
				// and get its _id in database
				long item_id = getDBItemID(v);
				if (item_id == -1) {
					return;
				}

				// Create an intent to show the item detail.
				// Pass the item_id along so the next activity can use it to
				// retrieve the info. about the item from database
				Intent i = new Intent(WishList.this, WishItemDetail.class);
				i.putExtra("item_id", item_id);
				i.putExtra("position", position);
				startActivityForResult(i, ITEM_DETAILS);
			}
		});

		// Listener for _gridView
		// When clicked, it starts a new activity to display the clicked item's
		// detailed info.
		_gridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> a, View v, int position, long id) {
				// find which item has been clicked and get its _id in database
				long item_id = getDBItemID(v);
				// Create an intent to show the item detail.
				// Pass the item_id along so the next activity can use it to
				// retrieve the info. about the item from database
				Intent i = new Intent(WishList.this, WishItemDetail.class);
				i.putExtra("item_id", item_id);
				i.putExtra("position", position);
				startActivity(i);
			}
		});

		_addNew.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent editItem = new Intent(WishList.this, EditItem.class);
				startActivityForResult(editItem, ADD_ITEM);
			}
		});

		// register context menu for both listview and gridview
		registerForContextMenu(_listView);
		registerForContextMenu(_gridView);

		// open the database for operations of Item table
		_itemDBManager = new ItemDBManager(this);
		_itemDBManager.open();

        handleIntent(getIntent());

		// set the spinner for switching between list and grid views
//		ArrayAdapter<CharSequence> adapter = ArrayAdapter
//				.createFromResource(this, R.array.views_array,
//						android.R.layout.simple_spinner_item);

//		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//		myViewSpinner.setAdapter(adapter);

//		// set the default spinner option
//		if (_viewOption == "list") {
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
//					_viewOption = "list";
//					populateItems(_nameQuery, SORT_BY);
//					_viewFlipper.setDisplayedChild(0);
//
//				}
//				// grid view is selected
//				else if (pos == 1) {
//					_viewOption = "grid";
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

	/**
	 * Get the ID of the item in Item table whose position in the list/grid view
	 * is position.
	 *
	 * @param position
	 *            : position of item in the list
	 * @param viewMode
	 *            : the current view mode
	 * @return the Item ID
	 */
	public long getDBItemID(View v) {
		TextView itemIdTextView = null;
		// Note that txtItemID is not visible in the UI but can be retrieved
		if (_viewOption.equals("list")) {
			itemIdTextView = (TextView) v.findViewById(R.id.txtItemID);
		}
		else {
			itemIdTextView = (TextView) v.findViewById(R.id.txtItemID_Grid);
		}
		long item_id = Long.parseLong(itemIdTextView.getText().toString());
		return item_id;
	}

	/***
	 * initial display of items in both list and grid view, called when the
	 * activity is created
	 *
	 * @param sortBy
	 */
	private void initializeView() {
		_wishItemCursor = _itemDBManager.getItems(_sortOption, _where, _itemIds);
		if (_itemDBManager.getItemsCount() == 0) {
			_viewFlipper.setDisplayedChild(2);
			return;
		}

		if (_viewOption.equals("list")) {
			updateListView();
			_viewFlipper.setDisplayedChild(0);
		}

		else {
			updateGridView();
			_viewFlipper.setDisplayedChild(1);
		}
	}

	/***
	 * display the items in either list or grid view sorted by "sortBy"
	 *
	 * @param sortBy
	 * @param searchName
	 *            : the item name to match, null for all items
	 */
	private void populateItems(String searchName, Map<String,String> where) {
		if (searchName == null) {
			// Get all of the rows from the Item table
			// Keep track of the TextViews added in list lstTable
			// _wishItemCursor = wishListDB.getItems(sortBy);
			_wishItemCursor = _itemDBManager.getItems(_sortOption, where, _itemIds);
		} else {
			_wishItemCursor = _itemDBManager.searchItems(searchName, _sortOption);
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
			_viewFlipper.setDisplayedChild(2);
			return;
		}


		if (_viewOption.equals("list")) {
			// Update the list view
			updateListView();
			_viewFlipper.setDisplayedChild(0);

		}

		else if (_viewOption.equals("grid")) {
			// Update the grid view
			updateGridView();
			_viewFlipper.setDisplayedChild(1);

		}
	}

	private void updateGridView() {
		if (_wishItemCursor != null) {
            _wishItemCursor.requery();
            int resID = R.layout.wishitem_photo;

            String[] from = new String[]{ItemDBManager.KEY_ID,
                    ItemDBManager.KEY_PHOTO_URL};

            int[] to = new int[]{R.id.txtItemID_Grid, R.id.imgPhotoGrid};
            _wishListItemAdapterCursor = new WishListItemCursorAdapter(this,
                    resID, _wishItemCursor, from, to);

            _gridView.setAdapter(_wishListItemAdapterCursor);
            _wishListItemAdapterCursor.notifyDataSetChanged();
        }
	}

	private void updateListView() {

		if (_wishItemCursor != null) {
			_wishItemCursor.requery();
			int resID = R.layout.wishitem_single;

			String[] from = new String[] {
					ItemDBManager.KEY_ID,
					ItemDBManager.KEY_PHOTO_URL,
					ItemDBManager.KEY_NAME,
					ItemDBManager.KEY_PRICE,
					ItemDBManager.KEY_STORENAME,
					ItemDBManager.KEY_ADDRESS,
					ItemDBManager.KEY_COMPLETE};

			int[] to = new int[] {
					R.id.txtItemID,
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
			View v = _listView.getChildAt(0);
			int top = (v == null) ? 0 : v.getTop();

			_listView.setAdapter(_wishListItemAdapterCursor);
			_wishListItemAdapterCursor.notifyDataSetChanged();

			// restore
			_listView.setSelectionFromTop(index, top);
		}

		else {
			// give message about empty cursor
		}
	}

	private void deleteItem(long item_id){
		_selectedItem_id = item_id;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Delete the wish?");
		builder.setCancelable(false);
		builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						_itemDBManager.deleteItem(_selectedItem_id);
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
        dialog.setOnShowListener(new DialogOnShowListener(this));
        dialog.show();
	}

    private void dispatchTakePictureIntent() {
        CameraManager c = new CameraManager();
        _newfullsizePhotoPath = c.getPhotoPath();
        Log.v("dispatch", "_new " + _newfullsizePhotoPath);
        startActivityForResult(c.getCameraIntent(), TAKE_PICTURE);
    }

	@SuppressLint("NewApi")
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_main, menu);
        _menu = menu;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			//search view is part the action bar in honeycomeb and up
			//Get the SearchView and set the searchable configuration
			SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            _menuSearch = menu.findItem(R.id.menu_search);
			SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
			// Assumes current activity is the searchable activity
			searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
			searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default

            // Style the searchView with yellow accent color
            int searchPlateId = searchView.getContext().getResources().getIdentifier("android:id/search_plate", null, null);
            // Getting the 'search_plate' LinearLayout.
            View searchPlate = searchView.findViewById(searchPlateId);
            // Setting background of 'search_plate'.
            searchPlate.setBackgroundResource(R.drawable.textfield_searchview_yellow);

            int closeButtonId = searchView.getContext().getResources().getIdentifier("android:id/search_close_btn", null, null);
            ImageView closeButton= (ImageView) searchView.findViewById(closeButtonId);
            closeButton.setBackgroundResource(R.drawable.selectable_background_wishlist);
        }
		return true;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_item_context, menu);

		AdapterView.AdapterContextMenuInfo menu_info;
		menu_info = (AdapterView.AdapterContextMenuInfo) menuInfo;

		//get the position of the item in the list
		//this is the position of the items among all items including the invisible ones
		int pos = menu_info.position;
		View selected_view = null;
		_selectedItem_id = -1;
		if(_viewOption.equals("list")){
			//get the position of the items among the visible items
			pos = pos - _listView.getFirstVisiblePosition();
			selected_view = _listView.getChildAt(pos);
			if (selected_view != null) {
				_selectedItem_id = getDBItemID(selected_view);
			}
			else {
				return;
			}
		}
		else if(_viewOption.equals("grid")){
			pos = pos - _gridView.getFirstVisiblePosition();
			selected_view = _gridView.getChildAt(pos);
			if (selected_view != null) {
				_selectedItem_id = getDBItemID(selected_view);
			}
			else {
				return;
			}
		}
		else if(selected_view == null){
			return;
		}

		WishItem wish_item = WishItemManager.getInstance(this).retrieveItembyId(_selectedItem_id);
		int complete = wish_item.getComplete();
		MenuItem mi = (MenuItem) menu.findItem(R.id.COMPLETE);
		if (complete == 1) {
			mi.setTitle("Mark as incomplete");
		}
		else {
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
		}
		else if (itemId == R.id.menu_add) {
			// let user generate a wish item
            dispatchTakePictureIntent();
			//Intent editItem = new Intent(this, EditItem.class);
			//startActivityForResult(editItem, ADD_ITEM);
			return true;
		}
		//else if (itemId == R.id.menu_post) {
		//	Intent snsIntent = new Intent(this, WishItemPostToSNS.class);
		//	startActivityForResult(snsIntent, POST_ITEM);
		//	return true;
		//}

//		else if (itemId == R.id.menu_scan) {
//			IntentIntegrator.initiateScan(this);
//			return true;
//		}

		else if(itemId == R.id.menu_sort) {
				showDialog(DIALOG_SORT);
		}

		else if (itemId == R.id.menu_status) {
				showDialog(DIALOG_FILTER);
		}

        else if (itemId == R.id.menu_tags) {
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
//			String address = _itemDBManager.getItemAddress(_selectedItem_id);
//			if (address.equals("unknown")||address.equals("")){
//				Toast toast = Toast.makeText(this, "location unknown", Toast.LENGTH_SHORT);
//				toast.show();
//			}
//			else{

				// get the latitude and longitude of the clicked item
				double[] dLocation = new double[2];
				dLocation = _itemDBManager.getItemLocation(_selectedItem_id);

				if (dLocation[0] == Double.MIN_VALUE && dLocation[1] == Double.MIN_VALUE) {
					Toast toast = Toast.makeText(this, "location unknown", Toast.LENGTH_SHORT);
					toast.show();
				}
				else {
					Intent mapIntent = new Intent(this, WishListMap.class);
					mapIntent.putExtra("type", "markOne");
					mapIntent.putExtra("latitude", dLocation[0]);
					mapIntent.putExtra("longitude", dLocation[1]);
					startActivity(mapIntent);
				}
//			}
			return true;
		}

		else if (itemId == R.id.SHARE) {
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
		}

		else if (itemId == R.id.COMPLETE) {
			WishItem wish_item = WishItemManager.getInstance(this).retrieveItembyId(_selectedItem_id);
			if (wish_item.getComplete() == 1) {
				wish_item.setComplete(0);
			}
			else {
				wish_item.setComplete(1);
			}
			wish_item.save();
			if (_viewOption.equals("list")) {
				//we only need to update the view if it is list view, as the check mark will be updated
				//in grid view, we don't have the checkmark, so no need to update
				updateView();
			}
            else if(_viewOption.equals("grid") && !_statusOption.equals("all")) {
                //there is a problem of updating the grid view while maintaining its scroll position
                //so we only update it if it is showing only completed or in progress wishes
                updateView();
            }
		}
        else if (itemId == R.id.TAG) {
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

			AlertDialog.Builder sortBuilder = new AlertDialog.Builder(WishList.this);
			sortBuilder.setTitle("Sort wishes");

			int j = 0;// 0 is by name
			if (_sortOption.equals(ItemsCursor.SortBy.date_time.toString())) {
				j = 1;
			}
			else if (_sortOption.equals(ItemsCursor.SortBy.price.toString())) {
				j = 2;
			}
			sortBuilder.setSingleChoiceItems(sortOption, j, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
						if (sortOption[item].equals(BY_NAME)) {
							_sortOption = ItemsCursor.SortBy.item_name.toString();
						}
						else if (sortOption[item].equals(BY_TIME)) {
							_sortOption = ItemsCursor.SortBy.date_time.toString();
						}
						else {
							_sortOption = ItemsCursor.SortBy.price.toString();
						}

						SharedPreferences pref = WishList.this.getPreferences(MODE_PRIVATE);
						SharedPreferences.Editor editor = pref.edit();
						editor.putString(PREF_SORT_OPTION, _sortOption);
						editor.commit();

						dialog.dismiss();

						populateItems(_nameQuery, _where);
					}
			});

			dialog = sortBuilder.create();
            dialog.setOnShowListener(new DialogOnShowListener(this));
			break;

		case DIALOG_FILTER:
			final String BY_ALL = "All";
			final String BY_COMPLETED = "Completed";
			final String BY_INPROGRESS = "In progress";
			final CharSequence[] options = {BY_ALL, BY_COMPLETED, BY_INPROGRESS};

			AlertDialog.Builder optionBuilder = new AlertDialog.Builder(WishList.this);
			optionBuilder.setTitle("Wish status");

			int i = 0;
			if (_statusOption.equals("completed")) {
				i = 1;
			}
			else if (_statusOption.equals("in_progress")) {
				i = 2;
			}
			optionBuilder.setSingleChoiceItems(options, i, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {

						if (options[item].equals(BY_ALL)) {
							_where.clear();
							_statusOption = "all";
						}

						else if (options[item].equals(BY_COMPLETED)) {
							_where.put("complete", "1");
							_statusOption = "completed";
						}
						else {
							_where.put("complete", "0");
							_statusOption = "in_progress";
						}

						SharedPreferences pref = WishList.this.getPreferences(MODE_PRIVATE);
						SharedPreferences.Editor editor = pref.edit();
						editor.putString(PREF_FILTER_OPTION, _statusOption);
						editor.commit();

						dialog.dismiss();
						populateItems(null, _where);
					}
			});

			dialog = optionBuilder.create();
            dialog.setOnShowListener(new DialogOnShowListener(this));
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
		if (_viewOption.equals("list")) {
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
        if (_tagOption != null) {
            _itemIds = TagItemDBManager.instance(this).ItemIds_by_tag(_tagOption);
            if (_itemIds.isEmpty()) {
                _tagOption = null;
            }
        }
    }

	@Override
	protected void onDestroy() {
		super.onDestroy();
		_itemDBManager.close();
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
                    _tagOption = data.getStringExtra("tag");

                    SharedPreferences pref = WishList.this.getPreferences(MODE_PRIVATE);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString(PREF_TAG_OPTION, _tagOption);
                    editor.commit();

                    if (_tagOption != null) {
                        _itemIds = TagItemDBManager.instance(this).ItemIds_by_tag(_tagOption);
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
                    Log.v("TAKE PICTURE", "_new " + _newfullsizePhotoPath);
                    _fullsizePhotoPath = String.valueOf(_newfullsizePhotoPath);
                    _newfullsizePhotoPath = null;
                    Intent i = new Intent(this, EditItem.class);
                    i.putExtra("fullsizePhotoPath", _fullsizePhotoPath);
                    if (_fullsizePhotoPath != null) {
                        Log.v("photo path", _fullsizePhotoPath);
                    }
                    else {
                        Log.v("photo path", "null");
                    }
                    startActivityForResult(i, EDIT_ITEM);
                }
                else {
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
            _tagOption = null;
            _itemIds.clear();
            _statusOption = "all";
            _where.clear();

            MenuItem tagItem =  _menu.findItem(R.id.menu_tags);
            tagItem.setVisible(true);

            MenuItem statusItem = _menu.findItem(R.id.menu_status);
            statusItem.setVisible(true);

            populateItems(null, _where);
            return true;
        }
        if (_tagOption != null || !_statusOption.equals("all")) {
            //the wishes are currently filtered by tag or status, tapping back button now should clean up the filter and show all wishes
            _tagOption = null;
            _itemIds.clear();

            _statusOption = "all";
            // need to remove the status single item choice dialog so it will be re-created and its initial choice will refreshed
            // next time it is opened.
            removeDialog(DIALOG_FILTER);

            _where.clear();

            SharedPreferences pref = WishList.this.getPreferences(MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.putString(PREF_FILTER_OPTION, _statusOption);
            editor.putString(PREF_TAG_OPTION, _tagOption);
            editor.commit();

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
            getActionBar().setTitle("Search: " + _nameQuery);
            getActionBar().setSubtitle(null);
            return;
        }

        if (_tagOption == null) {
            getActionBar().setTitle(R.string.app_name);
        }
        else {
            getActionBar().setTitle(_tagOption);
        }

        if (_statusOption.equals("completed")) {
            getActionBar().setSubtitle("Completed");
        }
        else if (_statusOption.equals("in_progress")) {
            getActionBar().setSubtitle("In progress");
        }
        else {
            getActionBar().setSubtitle(null);
        }
    }

	private void setUpActionBar() {
		// Make sure we're running on Honeycomb or higher to use ActionBar APIs
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			findViewById(R.id.listView_header).setVisibility(View.GONE);
            getActionBar().setDisplayHomeAsUpEnabled(true);
            updateActionBarTitle();
		}
		else {
			// we use the header instead of action bar for GingerBread and lower
			findViewById(R.id.listView_header).findViewById(R.id.imageButton_back_logo).setVisibility(View.VISIBLE);
			findViewById(R.id.listView_header).findViewById(R.id.imageButton_viewType).setVisibility(View.VISIBLE);
			findViewById(R.id.listView_header).findViewById(R.id.imageButton_search).setVisibility(View.VISIBLE);

			_backImageButton = (ImageButton) findViewById(R.id.imageButton_back_logo);
			_backImageButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					finish();
				}
			});

			_searchImageButton = (ImageButton) findViewById(R.id.imageButton_search);
			_searchImageButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					onSearchRequested();
				}

			});

			_viewImageButton = (ImageButton) findViewById(R.id.imageButton_viewType);
			_viewImageButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
				}
		});
		}
	}

    private void updateDrawerList() {
        ArrayList<ObjectDrawerItem> drawerItems = new ArrayList<ObjectDrawerItem>();
        if (!_itemIds.isEmpty() || !_statusOption.equals("all") || _nameQuery != null) {
            drawerItems.add(new ObjectDrawerItem(R.drawable.ic_action_goleft, "All wishes"));
        }
        drawerItems.add(new ObjectDrawerItem(R.drawable.ic_action_add, "Add"));
        drawerItems.add(new ObjectDrawerItem(R.drawable.ic_action_list, "List view"));
        drawerItems.add(new ObjectDrawerItem(R.drawable.ic_action_tiles_small, "Grid view"));
        drawerItems.add(new ObjectDrawerItem(R.drawable.ic_action_map, "Map view"));
        drawerItems.add(new ObjectDrawerItem(R.drawable.ic_action_gear, "Settings"));

        ObjectDrawerItem[] drawerItemArray = new ObjectDrawerItem[drawerItems.size()];
        drawerItems.toArray(drawerItemArray);

        DrawerItemCustomAdapter adapter = new DrawerItemCustomAdapter(this, R.layout.listview_item_row, drawerItemArray);
        mDrawerList.setAdapter(adapter);
    }

    private void setupNavigationDrawer() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        updateDrawerList();

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        // Setting item click listener for the listview mDrawerList
        mDrawerList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                TextView textViewName = (TextView) view.findViewById(R.id.textViewName);
                String item = textViewName.getText().toString();

                // Don't show the selected option in pressed state when the drawer is shown the next time
                mDrawerList.clearChoices();
                if (item.equals("All wishes")) {
                    goBack();
                }
                else if (item.equals("Add")) {
                    Intent editItem = new Intent(WishList.this, EditItem.class);
                    startActivityForResult(editItem, ADD_ITEM);
                }
                else if (item.equals("List view")) {
                    _viewOption = "list";
                    populateItems(_nameQuery, _where);
                    SharedPreferences pref = getPreferences(MODE_PRIVATE);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString(PREF_VIEW_OPTION, _viewOption);
                    editor.commit();
                }
                else if (item.equals("Grid view")) {
                    _viewOption = "grid";
                    populateItems(_nameQuery, _where);
                    SharedPreferences pref = getPreferences(MODE_PRIVATE);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString(PREF_VIEW_OPTION, _viewOption);
                    editor.commit();
                }
                else if (item.equals("Map view")) {
                    Intent mapIntent = new Intent(WishList.this, WishListMap.class);
                    mapIntent.putExtra("type", "markAll");
                    startActivity(mapIntent);
                }
                else if (item.equals("Settings")) {
                    Intent prefIntent = new Intent(getApplicationContext(), WishListPreference.class);
                    startActivity(prefIntent);
                }
                // Closing the drawer
                mDrawerLayout.closeDrawer(mDrawerList);
            }
        });
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

    }

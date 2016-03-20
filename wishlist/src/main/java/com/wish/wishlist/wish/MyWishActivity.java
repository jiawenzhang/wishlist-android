package com.wish.wishlist.wish;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
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
import android.widget.Toast;

import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback;

import me.kaede.tagview.OnTagDeleteListener;
import me.kaede.tagview.Tag;

import com.github.stkent.amplify.prompt.CustomLayoutPromptView;
import com.github.stkent.amplify.tracking.Amplify;
import com.github.stkent.amplify.tracking.PromptViewEvent;
import com.github.stkent.amplify.tracking.interfaces.IEventListener;
import com.parse.ParseUser;
import com.squareup.otto.Subscribe;
import com.wish.wishlist.R;
import com.wish.wishlist.activity.MapActivity;
import com.wish.wishlist.db.TagItemDBManager;
import com.wish.wishlist.event.EventBus;
import com.wish.wishlist.event.MyWishChangeEvent;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.model.WishItemManager;
import com.wish.wishlist.social.ShareHelper;
import com.wish.wishlist.tag.AddTagActivity;
import com.wish.wishlist.tag.FindTagActivity;
import com.wish.wishlist.util.Analytics;
import com.wish.wishlist.util.NetworkHelper;
import com.wish.wishlist.util.Options;
import com.wish.wishlist.image.CameraManager;
import com.wish.wishlist.sync.SyncAgent;

import org.apache.commons.lang3.ArrayUtils;

/***
 * MyWishActivity.java is responsible for displaying wish items in either list or grid
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
public class MyWishActivity extends WishBaseActivity implements
        SyncAgent.OnSyncWishChangedListener,
        SyncAgent.OnDownloadWishDoneListener {

    public static final String TAG = "MyWishActivity";

    private static final int EDIT_ITEM = 0;
    private static final int ADD_ITEM = 1;
    private static final int FIND_TAG = 2;
    private static final int ADD_TAG = 3;
    private static final int ITEM_DETAILS = 4;
    private static final int TAKE_PICTURE = 5;

    private Options.Tag mTag = new Options.Tag(null);

    private String mTempPhotoPath = null;

    private String mNameQuery = null;
    private Button mAddNewButton;
    private ArrayList<Long> mItemIds = new ArrayList<>();
    private MenuItem mMenuSearch;

    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // listen for MyWishChangeEvent
        EventBus.getInstance().register(this);

        mFilterView.setOnTagDeleteListener(new OnTagDeleteListener() {
            @Override
            public void onTagDeleted(Tag tag, int position) {
                Log.d(TAG, "onTagDeleted " + tag.text);

                if (mFilters.get(filterType.name) != null && mFilters.get(filterType.name) == tag) {
                    mNameQuery = null;
                    updateFilterView(filterType.name);
                }
                if (mFilters.get(filterType.tag) != null && mFilters.get(filterType.tag) == tag) {
                    mTag.setVal(null);
                    mTag.save();
                    mItemIds.clear();
                    updateFilterView(filterType.tag);
                }
                if (mFilters.get(filterType.status) != null && mFilters.get(filterType.status) == tag) {
                    mStatus.setVal(mStatus.ALL);
                    mStatus.save();
                    // need to remove the status single item choice dialog so it will be re-created and its initial choice will refreshed
                    // next time it is opened.
                    removeDialog(DIALOG_FILTER);
                    mWhere.clear();
                    updateFilterView(filterType.status);
                }
                reloadItems();
            }
        });

        mTag.read();
        if (mTag.val() != null) {
            mItemIds = TagItemDBManager.instance().ItemIds_by_tag(mTag.val());
            updateFilterView(filterType.tag);
        }

        mAddNewButton = (Button) findViewById(R.id.addNewWishButton);
        mAddNewButton.setOnClickListener(new android.view.View.OnClickListener() {
            public void onClick(android.view.View v) {
                Intent addItem = new Intent(MyWishActivity.this, AddWishActivity.class);
                startActivityForResult(addItem, ADD_ITEM);
            }
        });

        if (savedInstanceState != null) {
            Log.d(MyWishActivity.TAG, "savedInstanceState != null");
            // restore the current selected item in the list
            mTempPhotoPath = savedInstanceState.getString(AddWishActivity.TEMP_PHOTO_PATH);

            Log.d(MyWishActivity.TAG, "mTempPhotoPath " + mTempPhotoPath);
        } else{
            Log.d(MyWishActivity.TAG, "savedInstanceState == null");
        }

        SyncAgent.getInstance().register(this);

        if (refreshEnabled()) {
            // only enable swipe down refresh when the first item in recycler view is visible
            mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    mSwipeRefreshLayout.setEnabled(topRowVerticalPosition() >= 0 && mActionMode == null);
                }
            });

            // the refresh listener. this would be called when the layout is pulled down
            mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    if (!NetworkHelper.getInstance().isNetworkAvailable()) {
                        Toast.makeText(MyWishActivity.this, "Check network", Toast.LENGTH_LONG).show();
                        mSwipeRefreshLayout.setRefreshing(false);
                        return;
                    }
                    mSwipeRefreshLayout.setRefreshing(true);
                    Log.d(TAG, "refresh");
                    SyncAgent.getInstance().sync();
                    // our swipeRefreshLayout needs to be notified when the data is returned in order for it to stop the animation
                }
            });
        } else {
            mSwipeRefreshLayout.setEnabled(false);
        }

        handleIntent(getIntent());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (SyncAgent.getInstance().syncing()) {
            mSwipeRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    mSwipeRefreshLayout.setRefreshing(true);
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getInstance().unregister(this);
    }

    @Subscribe
    public void myWishChangeEvent(MyWishChangeEvent event) {
        Log.d(TAG, "reloadItems");
        reloadItems();
    }

    @Override
    protected Options.Status createStatus() {
        return new Options.MyWishStatus(Options.Status.ALL);
    }

    @Override
    protected Options.Sort createSort() {
        return new Options.MyWishSort(Options.Sort.NAME);
    }

    @Override
    protected void prepareDrawerList() {
        super.prepareDrawerList();
        mNavigationView.getMenu().findItem(R.id.my_wish).setVisible(false);
    }

    private int topRowVerticalPosition() {
        if (mRecyclerView == null || mRecyclerView.getChildCount() == 0) {
            return 0;
        }
        return mRecyclerView.getChildAt(0).getTop() - (mItemSpace / 2);
    }

    protected ModalMultiSelectorCallback createActionModeCallback() {
        return new ModalMultiSelectorCallback(mMultiSelector) {
            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                super.onCreateActionMode(actionMode, menu);
                getMenuInflater().inflate(R.menu.menu_my_wish_action, menu);
                if (!getResources().getBoolean(R.bool.enable_account)) {
                    MenuItem item1 = menu.findItem(R.id.menu_private);
                    item1.setVisible(false);

                    MenuItem item2 = menu.findItem(R.id.menu_public);
                    item2.setVisible(false);
                }

                if (refreshEnabled()) {
                    mSwipeRefreshLayout.setEnabled(false);
                }
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                super.onDestroyActionMode(mode);
                mSelectedItemKeys.clear();

                // notifyDataSetChanged will fix a bug in recyclerview-multiselect lib, where the selected item's state does
                // not get cleared when the action mode is finished.
                mWishAdapter.notifyDataSetChanged();
                mMultiSelector.clearSelections();
                mActionMode = null;
                if (topRowVerticalPosition() >= 0 && refreshEnabled()) {
                    mSwipeRefreshLayout.setEnabled(true);
                }
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                ArrayList<String> itemKeys = selectedItemKeys();
                Log.d(TAG, "selected item ids: " + itemKeys.toString());
                actionMode.finish();

                long[] ids = new long[itemKeys.size()];
                for (int i = 0; i < itemKeys.size() ; i++) {
                    ids[i] = Long.valueOf(itemKeys.get(i));
                }
                Long[] longObjects = ArrayUtils.toObject(ids);
                List<Long> idList = java.util.Arrays.asList(longObjects);

                switch (menuItem.getItemId()) {
                    case R.id.menu_tag:
                        Log.d(TAG, "tag");
                        Intent intent = new Intent(MyWishActivity.this, AddTagActivity.class);
                        intent.putExtra(AddTagActivity.ITEM_ID_ARRAY, (ids));
                        startActivityForResult(intent, ADD_TAG);
                        return true;
                    case R.id.menu_share:
                        Log.d(TAG, "share");
                        ShareHelper share = new ShareHelper(MyWishActivity.this, ids);
                        share.share();
                        return true;
                    case R.id.menu_delete:
                        Log.d(TAG, "delete");
                        deleteItems(idList);
                        return true;
                    case R.id.menu_complete:
                        Log.d(TAG, "complete");
                        markItemComplete(idList, 1);
                        return true;
                    case R.id.menu_incomplete:
                        Log.d(TAG, "incomplete");
                        markItemComplete(idList, 0);
                        return true;
                    case R.id.menu_private:
                        Log.d(TAG, "private");
                        setItemAccess(idList, WishItem.PRIVATE);
                        return true;
                    case R.id.menu_public:
                        Log.d(TAG, "public");
                        setItemAccess(idList, WishItem.PUBLIC);
                        return true;
                    default:
                        return false;
                }
            }
        };
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    protected void handleIntent(Intent intent) {
        // check if the activity is started from search
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            Analytics.send(Analytics.WISH, "Search", null);
            MenuItemCompat.collapseActionView(mMenuSearch);
            // activity is started from search, get the search query and
            // displayed the searched items
            mNameQuery = intent.getStringExtra(SearchManager.QUERY);
            updateFilterView(filterType.name);
            MenuItem tagItem =  mMenu.findItem(R.id.menu_tags);
            MenuItemCompat.collapseActionView(tagItem);

            MenuItem statusItem = mMenu.findItem(R.id.menu_status);
            MenuItemCompat.collapseActionView(statusItem);
        }
        reloadItems();
    }

    @Override
    protected void updateWishView() {
        if (mWishlist.isEmpty() && (!mWhere.isEmpty() || !mItemIds.isEmpty() || mNameQuery != null)) {
            // no matching wishes text
            mViewFlipper.setDisplayedChild(NO_MATCHING_WISH_VIEW);
            return;
        }
        if (mWishlist.isEmpty()) {
            // make a new wish button
            mViewFlipper.setDisplayedChild(MAKE_A_WISH_VIEW);
            return;
        }
        super.updateWishView();
    }

    @Override
    protected void setContentView() {
        setContentView(R.layout.my_wish);
        setupActionBar(R.id.my_wish_toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_my_wish, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        mMenuSearch = menu.findItem(R.id.menu_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(mMenuSearch);
        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
        return super.onCreateOptionsMenu(menu);
    }

    /***
     * display the items in either list or grid view sorted by "sortBy"
     */
    @Override
    protected void reloadItems() {
        // Get all of the rows from the Item table
        // Keep track of the TextViews added in list lstTable
        mWishlist = WishItemManager.getInstance().getItems(mNameQuery, mSort.toString(), mWhere, mItemIds);
        updateWishView();
        updateDrawerList();
    }

    private void setItemAccess(final List<Long> item_ids, int access) {
        HashMap<Long/*item_id*/, WishItem> changed = new HashMap<>();
        for (long item_id : item_ids) {
            WishItem wish_item = WishItemManager.getInstance().getItemById(item_id);
            if (wish_item.getAccess() != access) {
                wish_item.setAccess(access);
                wish_item.setUpdatedTime(System.currentTimeMillis());
                wish_item.setSyncedToServer(false);
                wish_item.saveToLocal();

                changed.put(item_id, wish_item);
            }
        }
        if (changed.size() > 0) {
            SyncAgent.getInstance().sync();
        }

        for (int i = 0; i < mWishlist.size(); i++) {
            Long item_id = mWishlist.get(i).getId();
            WishItem changedItem = changed.get(item_id);
            if (changedItem != null) {
                mWishlist.set(i, changedItem);
                if (mWishAdapter != null) {
                    mWishAdapter.notifyItemChanged(i, changedItem);
                }
            }
        }
    }

    private void markItemComplete(final List<Long> item_ids, int complete) {
        HashMap<Long/*item_id*/, WishItem> changed = new HashMap<>();
        for (long item_id : item_ids) {
            WishItem wish_item = WishItemManager.getInstance().getItemById(item_id);
            String label = "Complete";
            if (complete == 0) {
                label = "InProgress";
            }
            if (wish_item.getComplete() != complete) {
                wish_item.setComplete(complete);
                wish_item.setUpdatedTime(System.currentTimeMillis());
                wish_item.setSyncedToServer(false);
                wish_item.saveToLocal();

                changed.put(item_id, wish_item);

                Analytics.send(Analytics.WISH, "ChangeStatus", label);
            }
        }

        if (changed.size() > 0) {
            SyncAgent.getInstance().sync();
        }

        for (int i = 0; i < mWishlist.size(); i++) {
            Long item_id = mWishlist.get(i).getId();
            WishItem changedItem = changed.get(item_id);
            if (changedItem != null) {
                mWishlist.set(i, changedItem);
                if (mWishAdapter != null) {
                    mWishAdapter.notifyItemChanged(i, changedItem);
                }
            }
        }
        if (mStatus.val() != mStatus.ALL) {
            // if we are filtering by status, the filtered results are changed and
            // we need to reload the items
            reloadItems();
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
                Analytics.send(Analytics.WISH, "Delete", null);
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
        mTempPhotoPath = c.getPhotoPath();
        startActivityForResult(c.getCameraIntent(), TAKE_PICTURE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (super.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            //do nothing here, the search view is already configured in onCreateOptionsMenu()
            case R.id.menu_search:
                return true;
            case R.id.menu_add:
                // let user generate a wish item
                dispatchTakePictureIntent();
                return true;
            case R.id.menu_sort:
                showDialog(DIALOG_SORT);
                return true;
            case R.id.menu_status:
                showDialog(DIALOG_FILTER);
                return true;
            case R.id.menu_tags:
                Intent i = new Intent(MyWishActivity.this, FindTagActivity.class);
                startActivityForResult(i, FIND_TAG);
                return true;
            default:
                return false;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString(AddWishActivity.TEMP_PHOTO_PATH, mTempPhotoPath);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }

        mTempPhotoPath = savedInstanceState.getString(AddWishActivity.TEMP_PHOTO_PATH);
        super.onRestoreInstanceState(savedInstanceState);
    }

    private void updateItemIdsForTag() {
        // If the current wishlist is filtered by tag "T", and there is an item "A" in this list
        // we then enter the AddTagActivity view for item "A" and delete the tag "T" from A. When we come back to
        // this list, we need to update mItemIds to exclude "A" so "A" will not show up in this list.
        if (mTag.val() != null) {
            mItemIds = TagItemDBManager.instance().ItemIds_by_tag(mTag.val());
            if (mItemIds.isEmpty()) {
                mTag.setVal(null);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case EDIT_ITEM: {
                if (resultCode == Activity.RESULT_OK) {
                    updateItemIdsForTag();
                    updateFilterView(filterType.tag);
                }
                break;
            }
            case ITEM_DETAILS: {
                if (resultCode == Activity.RESULT_OK) {
                    updateItemIdsForTag();
                    updateFilterView(filterType.tag);
                }
                break;
            }
            case ADD_ITEM: {
                if (resultCode == Activity.RESULT_OK) {
                    CustomLayoutPromptView promptView = (CustomLayoutPromptView) findViewById(R.id.feedback_prompt_view);
                    Amplify.get(this).promptIfReady(this, promptView, new IEventListener<PromptViewEvent>() {
                        @Override
                        public void notifyEventTriggered(@NonNull final PromptViewEvent event) {
                            if (event == PromptViewEvent.USER_GAVE_FEEDBACK) {
                                Analytics.send(Analytics.APP, "Feedback", "Give");
                            } else if (event == PromptViewEvent.USER_DECLINED_FEEDBACK) {
                                Analytics.send(Analytics.APP, "Feedback", "Decline");
                            }
                        }
                    });
                }
                break;
            }
            case FIND_TAG: {
                if (resultCode == Activity.RESULT_OK) {
                    mTag.setVal(data.getStringExtra("tag"));
                    mTag.save();

                    if (mTag.val() != null) {
                        mItemIds = TagItemDBManager.instance().ItemIds_by_tag(mTag.val());
                        reloadItems();
                        updateFilterView(filterType.tag);
                    }
                }
                break;
            }
            case ADD_TAG: {
                if (resultCode == Activity.RESULT_OK) {
                    updateItemIdsForTag();
                    updateFilterView(filterType.tag);

                    // item's updated time is changed, need to reload to get the update
                    reloadItems();
                }
                break;
            }
            case TAKE_PICTURE: {
                if (resultCode == RESULT_OK) {
                    Log.d(TAG, "TAKE_PICTURE: RESULT_OK");
                    Log.d("TAKE PICTURE ", mTempPhotoPath);
                    Intent i = new Intent(this, AddWishActivity.class);
                    i.putExtra(AddWishActivity.TEMP_PHOTO_PATH, mTempPhotoPath);

                    Analytics.send(Analytics.WISH, "TakenPicture", "FromActionBarCameraButton");
                    startActivityForResult(i, EDIT_ITEM);
                } else {
                    Log.d(TAG, "TAKE_PICTURE: not RESULT_OK");
                }
                break;
            }
        }
    }

    @Override
    protected boolean goBack()
    {
        if (mNameQuery != null || mTag.val() != null || mStatus.val() != Options.Status.ALL) {
            mNameQuery = null;
            updateFilterView(filterType.name);

            //the wishes are currently filtered by tag or status, tapping back button now should clean up the filter and show all wishes
            mTag.setVal(null);
            mTag.save();
            mItemIds.clear();
            updateFilterView(filterType.tag);

            mStatus.setVal(Options.Status.ALL);
            mStatus.save();
            // need to remove the status single item choice dialog so it will be re-created and its initial choice will refreshed
            // next time it is opened.
            removeDialog(DIALOG_FILTER);
            mWhere.clear();
            updateFilterView(filterType.status);

            reloadItems();
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

    @Override
    protected void updateDrawerList() {
        MenuItem item = mNavigationView.getMenu().findItem(R.id.all_wishes);
        if (!mItemIds.isEmpty() || (mStatus != null && mStatus.val() != Options.Status.ALL) || mNameQuery != null) {
            item.setVisible(true);
        } else {
            item.setVisible(false);
        }
    }

    public void onSyncWishChanged() {
        Log.d(TAG, "onSyncWishChanged");
        reloadItems();
    }

    public void onDownloadWishDone() {
        Log.d(TAG, "onDownloadWishDone");
        mSwipeRefreshLayout.setRefreshing(false);
    }

    public void onWishTapped(WishItem item) {
        Log.d(TAG, "onWishTapped");
        Intent i = new Intent(MyWishActivity.this, ExistingWishDetailActivity.class);
        i.putExtra(WishDetailActivity.ITEM, item);
        startActivityForResult(i, ITEM_DETAILS);
    }

    protected boolean onTapAdd() {
        Intent addItem = new Intent(MyWishActivity.this, AddWishActivity.class);
        startActivityForResult(addItem, ADD_ITEM);
        return true;
    }

    @Override
    protected boolean mapView() {
        Intent mapIntent = new Intent(this, MapActivity.class);
        mapIntent.putExtra(MapActivity.TYPE, MapActivity.MARK_ALL);
        mapIntent.putExtra(MapActivity.MY_WISH, true);
        startActivity(mapIntent);
        return true;
    }

    private void updateFilterView(final filterType type) {
        Tag oldTag = mFilters.get(type);
        if (oldTag != null) {
            removeTag(oldTag);
            mFilters.remove(type);
        }

        Tag tag = null;
        if (type == filterType.name) {
            if (mNameQuery != null) {
                tag = new Tag(mNameQuery);
            }
        } else if (type == filterType.tag) {
            if (mTag.val() != null) {
                tag = new Tag(mTag.val());
            }
        } else if (type == filterType.status) {
            if (mStatus.val() != mStatus.ALL) {
                String txt = "";
                if (mStatus.val() == mStatus.COMPLETED) {
                    txt = "completed";
                } else if (mStatus.val() == mStatus.IN_PROGRESS) {
                    txt = "in progress";
                }
                tag = new Tag(txt);
            }
        }

        if (tag != null) {
            tag.isDeletable = true;
            tag.layoutColor = ContextCompat.getColor(this, R.color.wishlist_yellow_color);
            mFilterView.addTag(tag);
            mFilters.put(type, tag);
        }
        updateFilterViewMargin();
    }

    private Boolean refreshEnabled() {
        if (getResources().getBoolean(R.bool.enable_account)) {
            return ParseUser.getCurrentUser() != null;
        } else {
            return false;
        }
    }
}

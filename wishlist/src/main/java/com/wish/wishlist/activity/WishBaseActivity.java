package com.wish.wishlist.activity;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ViewFlipper;

import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback;
import com.bignerdranch.android.multiselector.MultiSelector;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.wish.wishlist.R;
import com.wish.wishlist.WishlistApplication;
import com.wish.wishlist.model.ItemNameComparator;
import com.wish.wishlist.model.ItemPriceComparator;
import com.wish.wishlist.model.ItemTimeComparator;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.util.Options;
import com.wish.wishlist.util.WishAdapter;
import com.wish.wishlist.util.WishAdapterGrid;
import com.wish.wishlist.util.WishAdapterList;
import com.wish.wishlist.widgets.ItemDecoration;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/***
 * WishBaseActivity is responsible for displaying wish items in either list or grid
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
public abstract class WishBaseActivity extends DrawerActivity implements
        WishAdapter.onWishTapListener,
        WishAdapter.onWishLongTapListener {
    public static final String TAG = "WishBaseActivity";

    static final protected int DIALOG_MAIN = 0;
    static final protected int DIALOG_FILTER = 1;
    static final protected int DIALOG_SORT = 2;
    private static final int ITEM_DETAILS = 4;

    protected java.util.Map mWhere = new HashMap<>();

    protected static final String MULTI_SELECT_STATE = "multi_select_state";
    protected static final String SELECTED_ITEM_IDS = "selected_item_ids";

    protected Options.View mView = new Options.View(Options.View.LIST);
    protected Options.Status mStatus = new Options.Status(Options.Status.ALL);
    protected Options.Sort mSort = new Options.Sort(Options.Sort.NAME);

    protected ViewFlipper mViewFlipper;
    protected SwipeRefreshLayout mSwipeRefreshLayout;
    protected Menu mMenu;
    protected MultiSelector mMultiSelector = new MultiSelector();
    protected ActionMode mActionMode;
    protected ModalMultiSelectorCallback mActionModeCallback;

    protected List<WishItem> mWishlist = new ArrayList<>();
    protected RecyclerView mRecyclerView;
    protected LinearLayoutManager mLinearLayoutManager;
    protected StaggeredGridLayoutManager mStaggeredGridLayoutManager;
    protected WishAdapter mWishAdapter;

    protected static final int WISH_VIEW = 0;
    protected static final int MAKE_A_WISH_VIEW = 1;
    protected static final int NO_MATCHING_WISH_VIEW = 2;

    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        updateActionBarTitle();

        mView.read();
        Tracker t = ((WishlistApplication) getApplication()).getTracker(WishlistApplication.TrackerName.APP_TRACKER);
        if (mView.val() == Options.View.LIST) {
            t.setScreenName("ListView");
        } else {
            t.setScreenName("GridView");
        }
        t.send(new HitBuilders.AppViewBuilder().build());

        mStatus.read();
        if (mStatus.val() == Options.Status.ALL) {
            mWhere.clear();
        } else if(mStatus.val() == Options.Status.COMPLETED) {
            mWhere.put("complete", "1");
        } else if(mStatus.val() == Options.Status.IN_PROGRESS) {
            mWhere.put("complete", "0");
        }

        mSort.read();

        // Get the intent, verify the action and get the query
        mViewFlipper = (ViewFlipper) findViewById(R.id.myFlipper);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        // sets the colors used in the refresh animation
        //mSwipeRefreshLayout.setColorSchemeResources(R.color.blue_bright, R.color.green_light,
        //R.color.orange_light, R.color.red_light);

        mRecyclerView = (RecyclerView) findViewById(R.id.wish_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        //mRecyclerView.setHasFixedSize(true);

        mLinearLayoutManager = new LinearLayoutManager(this);
        mStaggeredGridLayoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);

        // Fixme use dp and covert to px
        mRecyclerView.addItemDecoration(new ItemDecoration(10 /*px*/));

        // Set up toolbar action mode. This mode is activated when an item is long tapped and user can then select
        // multiple items for an action
        mActionModeCallback = createActionModeCallback();

        // restore multi-select state when activity is re-created due to, for example, screen orientation
        if (mMultiSelector != null) {
            // restore selected item state
            if (savedInstanceState != null) {
                mMultiSelector.restoreSelectionStates(savedInstanceState.getBundle(MULTI_SELECT_STATE));
            }
            if (mMultiSelector.isSelectable()) {
                if (mActionModeCallback != null) {
                    mActionModeCallback.setClearOnPrepare(false);
                    mActionMode = startSupportActionMode(mActionModeCallback);
                }
            }
        }
    }

    protected abstract ModalMultiSelectorCallback createActionModeCallback();

    protected abstract void setContentView();

    /***
     * display the items in either list or grid view sorted by "sortBy"
     *
     * @param searchName
     *            : the item name to match, null for all items
     */
    protected abstract void reloadItems(String searchName, java.util.Map where);

    /***
     * update either list view or grid view according view option
     */
    protected void updateWishView() {
        if (mWishlist.isEmpty()) {
            // make a new wish button
            mViewFlipper.setDisplayedChild(MAKE_A_WISH_VIEW);
            return;
        }

        if (mWishAdapter != null) {
            mWishAdapter.setWishList(mWishlist);
        } else {
            if (mView.val() == Options.View.LIST) {
                mWishAdapter = new WishAdapterList(mWishlist, this, mMultiSelector);
                mRecyclerView.setLayoutManager(mLinearLayoutManager);
            } else {
                mWishAdapter = new WishAdapterGrid(mWishlist, this, mMultiSelector);
                mRecyclerView.setLayoutManager(mStaggeredGridLayoutManager);
            }
            mRecyclerView.swapAdapter(mWishAdapter, true);
        }
        if (mViewFlipper.getDisplayedChild() != WISH_VIEW) {
            mViewFlipper.setDisplayedChild(WISH_VIEW);
        }
    }

    protected boolean switchView(int viewOption) {
        if (mView.val() == viewOption) {
            return true;
        }
        mRecyclerView.invalidate();
        mRecyclerView.setAdapter(null);
        String screenView;
        if (viewOption == Options.View.LIST) {
            mRecyclerView.setLayoutManager(mLinearLayoutManager);
            mWishAdapter = new WishAdapterList(mWishlist, this, mMultiSelector);
            screenView = "ListView";
        } else {
            mRecyclerView.setLayoutManager(mStaggeredGridLayoutManager);
            mWishAdapter = new WishAdapterGrid(mWishlist, this, mMultiSelector);
            screenView = "GridView";
        }
        mRecyclerView.swapAdapter(mWishAdapter, true);
        mView.setVal(viewOption);
        mView.save();

        Tracker t = ((WishlistApplication) getApplication()).getTracker(WishlistApplication.TrackerName.APP_TRACKER);
        t.setScreenName(screenView);
        t.send(new HitBuilders.AppViewBuilder().build());
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        mMenu = menu;

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
        } else if(itemId == R.id.menu_sort) {
            showDialog(DIALOG_SORT);
        } else if (itemId == R.id.menu_status) {
            showDialog(DIALOG_FILTER);
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

                AlertDialog.Builder sortBuilder = new AlertDialog.Builder(WishBaseActivity.this, R.style.AppCompatAlertDialogStyle);
                sortBuilder.setTitle("Sort wishes");

                int j = 0;// 0 is by name
                if (mSort.val() == Options.Sort.UPDATED_TIME) {
                    j = 1;
                } else if (mSort.val() == Options.Sort.PRICE) {
                    j = 2;
                } sortBuilder.setSingleChoiceItems(sortOption, j, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        if (sortOption[item].equals(BY_NAME)) {
                            mSort.setVal(Options.Sort.NAME);
                            Collections.sort(mWishlist, new ItemNameComparator());
                        } else if (sortOption[item].equals(BY_TIME)) {
                            mSort.setVal(Options.Sort.UPDATED_TIME);
                            Collections.sort(mWishlist, new ItemTimeComparator());
                        } else {
                            mSort.setVal(Options.Sort.PRICE);
                            Collections.sort(mWishlist, new ItemPriceComparator());
                        }
                        mSort.save();
                        mWishAdapter.notifyDataSetChanged();

                        dialog.dismiss();
                    }
                });

                dialog = sortBuilder.create();
                break;

            case DIALOG_FILTER:
                final String BY_ALL = "All";
                final String BY_COMPLETED = "Completed";
                final String BY_IN_PROGRESS = "In progress";
                final CharSequence[] options = {BY_ALL, BY_COMPLETED, BY_IN_PROGRESS};

                AlertDialog.Builder optionBuilder = new AlertDialog.Builder(WishBaseActivity.this, R.style.AppCompatAlertDialogStyle);
                optionBuilder.setTitle("Wish status");

                optionBuilder.setSingleChoiceItems(options, mStatus.val(), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        if (options[item].equals(BY_ALL)) {
                            mWhere.clear();
                            mStatus.setVal(Options.Status.ALL);
                        } else if (options[item].equals(BY_COMPLETED)) {
                            mWhere.put("complete", "1");
                            mStatus.setVal(Options.Status.COMPLETED);
                        } else {
                            mWhere.put("complete", "0");
                            mStatus.setVal(Options.Status.IN_PROGRESS);
                        }
                        mStatus.save();

                        dialog.dismiss();
                        reloadItems(null, mWhere);
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
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        // save the multi-select state, so we can restore them
        savedInstanceState.putBundle(MULTI_SELECT_STATE, mMultiSelector.saveSelectionStates());
        long[] itemIds = new long[selectedItemIds().size()];
        for (int i=0; i< selectedItemIds().size(); i++) {
            itemIds[i] = selectedItemIds().get(i);
        }
        savedInstanceState.putLongArray(SELECTED_ITEM_IDS, itemIds);
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
    }

    protected abstract boolean goBack();

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

    protected void updateActionBarTitle() {
        if (mStatus.val() == Options.Status.COMPLETED) {
            getSupportActionBar().setSubtitle("Completed");
        } else if (mStatus.val() == Options.Status.IN_PROGRESS) {
            getSupportActionBar().setSubtitle("In progress");
        } else {
            getSupportActionBar().setSubtitle(null);
        }
    }

    protected abstract void updateDrawerList();
    protected boolean onTapAdd() { return true; }

    protected ArrayList<Long> selectedItemIds() {
        return mWishAdapter.selectedItemIds();
    }

    protected void setSelectedItemIds(List<Long> itemIds) {
        mWishAdapter.setSelectedItemIds(itemIds);
    }

    public void onWishTapped(WishItem item) {
        Log.d(TAG, "onWishTapped");
        Intent i = new Intent(WishBaseActivity.this, MyWishDetail.class);
        i.putExtra("item_id", item.getId());
        i.putExtra("position", 0);
        startActivityForResult(i, ITEM_DETAILS);
    }

    public void onWishLongTapped() {
        Log.d(TAG, "onWishLongTap");
        mActionMode = startSupportActionMode(mActionModeCallback);
    }

    protected void drawerOpened() {
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }
}

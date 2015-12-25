package com.wish.wishlist.wish;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback;
import com.wish.wishlist.R;
import com.wish.wishlist.friend.FriendsActivity;
import com.wish.wishlist.activity.MapActivity;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.util.NetworkHelper;
import com.wish.wishlist.util.Options;

import java.util.ArrayList;
import java.util.List;

import me.kaede.tagview.OnTagDeleteListener;
import me.kaede.tagview.Tag;

public class FriendsWishActivity extends WishBaseActivity implements
        WishLoader.onGotWishesListener,
        WishAdapter.onWishTapListener,
        AppBarLayout.OnOffsetChangedListener,
        WishImageDownloader.onWishImageDownloadDoneListener {

    final static String TAG = "FriendsWishActivity";
    final static String ITEM = "Item";
    private AppBarLayout mAppBarLayout;
    private WishImageDownloader mWishImageDownloader;
    private ProgressDialog mProgressDialog = null;

    private String mFriendId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFilterView.setOnTagDeleteListener(new OnTagDeleteListener() {
            @Override
            public void onTagDeleted(Tag tag, int position) {
                Log.d(TAG, "onTagDeleted " + tag.text);
                if (mFilters.get(filterType.status) != null && mFilters.get(filterType.status) == tag) {
                    mStatus.setVal(mStatus.ALL);
                    mStatus.save();
                    // need to remove the status single item choice dialog so it will be re-created and its initial choice will refreshed
                    // next time it is opened.
                    removeDialog(DIALOG_FILTER);
                    updateFilterViewForStatus();
                    mWhere.clear();
                    reloadItems();
                }
            }
        });

        mAppBarLayout = (AppBarLayout) findViewById(R.id.appbar);
        mSwipeRefreshLayout.setEnabled(false);
        // the refresh listener. this would be called when the layout is pulled down
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.d(TAG, "refresh");
                if (!NetworkHelper.getInstance().isNetworkAvailable()) {
                    Toast.makeText(FriendsWishActivity.this, "Check network", Toast.LENGTH_LONG).show();
                    mSwipeRefreshLayout.setRefreshing(false);
                    return;
                }
                mSwipeRefreshLayout.setRefreshing(true);
                WishLoader.getInstance().fetchWishes(mFriendId);
                // our swipeRefreshLayout needs to be notified when the data is returned in order for it to stop the animation
                // this is triggered in onGotWishes in this activity
            }
        });

        if (mView.val() == Options.View.LIST) {
            mRecyclerView.setLayoutManager(mLinearLayoutManager);
        } else {
            mRecyclerView.setLayoutManager(mStaggeredGridLayoutManager);
        }
        if (mViewFlipper.getDisplayedChild() != WISH_VIEW) {
            mViewFlipper.setDisplayedChild(WISH_VIEW);
        }

        Intent i = getIntent();
        mFriendId = i.getStringExtra(FriendsActivity.FRIEND_ID);
        WishLoader.getInstance().setGotWishesListener(this);
        WishLoader.getInstance().fetchWishes(mFriendId);
    }

    @Override
    protected Options.Status createStatus() {
        return new Options.FriendWishStatus(Options.Status.ALL);
    }

    @Override
    protected Options.Sort createSort() {
        return new Options.FriendWishSort(Options.Sort.NAME);
    }

    @Override
    protected void prepareDrawerList() {
        mNavigationView.getMenu().findItem(R.id.Add).setVisible(false);
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
        //The Refresh must be only active when the offset is zero :
        mSwipeRefreshLayout.setEnabled(i == 0 && mActionMode == null);
    }

    @Override
    public void onResume() {
        super.onResume();
        mAppBarLayout.addOnOffsetChangedListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mAppBarLayout.removeOnOffsetChangedListener(this);
    }


    @Override
    protected ModalMultiSelectorCallback createActionModeCallback() {
        return new ModalMultiSelectorCallback(mMultiSelector) {
            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                super.onCreateActionMode(actionMode, menu);
                getMenuInflater().inflate(R.menu.menu_friends_wish_action, menu);
                mSwipeRefreshLayout.setEnabled(false);
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
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                ArrayList<String> itemKeys = selectedItemKeys();
                Log.d(TAG, "selected item keys: " + itemKeys.toString());
                actionMode.finish();

                switch (menuItem.getItemId()) {
                    case R.id.menu_share:
                        Log.d(TAG, "share");
                        //ShareHelper share = new ShareHelper(this, _selectedItem_id);
                        //share.share();
                        return true;
                    case R.id.menu_save:
                        Log.d(TAG, "save");
                        saveWishes(itemKeys);
                        return true;
                    default:
                        return false;
                }
            }
        };
    }

    private void showProgressDialog(final String text) {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(text);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                Log.d(TAG, text + " canceled");
                Toast.makeText(FriendsWishActivity.this, "Failed to save wish", Toast.LENGTH_LONG).show();
            }
        });
        mProgressDialog.show();
    }

    private void saveWishes(List<String> wishKeyList) {
        showProgressDialog("Saving...");
        Log.d(TAG, "saving " + wishKeyList.size() + " wishes to my own wishlist");
        List<WishItem> items = new ArrayList<>();
        for (final WishItem item : mWishlist) {
            if (wishKeyList.contains(item.getKey())) {
                items.add(item);
            }
        }
        mWishImageDownloader = new WishImageDownloader();
        mWishImageDownloader.setWishImageDownloadDoneListener(this);
        mWishImageDownloader.download(items);
    }

    public void onWishImageDownloadDone(boolean success) {
        Log.d(TAG, "wishes image download all done");
        if (success) {
            Toast.makeText(this, "Saved", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Failed, check network", Toast.LENGTH_LONG).show();
        }
        mProgressDialog.dismiss();
    }

    @Override
    protected void setContentView() {
        setContentView(R.layout.activity_friends_wish);
        setupActionBar(R.id.friends_wish_toolbar);
    }

    public void onGotWishes(String friendId, List<WishItem> wishList) {
        Log.d(TAG, "got " + wishList.size() + " wishes from friendId " + friendId);
        mSwipeRefreshLayout.setRefreshing(false);
        mWishlist = wishList;

        if (mWhere != null && mWhere.get("complete") != null) {
            int complete = Integer.parseInt((String) mWhere.get("complete"));
            ArrayList<WishItem> filtered_wishList = new ArrayList<>();
            for (WishItem item : mWishlist) {
                if (item.getComplete() == complete) {
                    filtered_wishList.add(item);
                }
            }
            mWishlist = filtered_wishList;
        }
        sortWishes(mSort.val());

        // onGotWishes can be call twice, one from cached data and another from network, if we use setAdapter here,
        // the items in the grid layout will be displaced the second time setAdapter is called.
        // Using swapAdapter and passing false as the removeAndRecycleExistingViews flag will avoid this
        updateWishView();
        updateDrawerList();
    }

    @Override
    protected void updateWishView() {
        if (mWishlist.isEmpty()) {
            // no matching wishes text
            mViewFlipper.setDisplayedChild(NO_MATCHING_WISH_VIEW);
            TextView txtView = (TextView) mViewFlipper.findViewById(R.id.noWishText);
            txtView.setText("No wish found");
            return;
        }
        super.updateWishView();
    }

    @Override
    protected void updateDrawerList() {
        MenuItem item = mNavigationView.getMenu().findItem(R.id.all_wishes);
        if (mStatus.val() != Options.Status.ALL) {
            item.setVisible(true);
        } else {
            item.setVisible(false);
        }
    }

    @Override
    protected boolean goBack()
    {
        if (mStatus.val() != Options.Status.ALL) {
            //the wishes are currently filtered status, tapping back button now should clean up the filter and show all wishes
            mStatus.setVal(Options.Status.ALL);
            // need to remove the status single item choice dialog so it will be re-created and its initial choice will refreshed
            // next time it is opened.
            removeDialog(DIALOG_FILTER);
            mStatus.save();
            mWhere.clear();
            updateFilterViewForStatus();
            reloadItems();
        } else {
            //we are already showing all the wishes, tapping back button should close the list view
            finish();
        }
        return true;
    }

    @Override
    protected void reloadItems() {
        WishLoader.getInstance().fetchWishes(mFriendId);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_friends_wish, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void onWishTapped(WishItem item) {
        Log.d(TAG, "onWishTapped");
        Intent i = new Intent(this, FriendWishDetailActivity.class);
        i.putExtra(ITEM, item);
        startActivity(i);
    }

    @Override
    protected boolean mapView() {
        Intent mapIntent = new Intent(this, MapActivity.class);
        mapIntent.putExtra(MapActivity.TYPE, MapActivity.MARK_ALL);
        mapIntent.putExtra(MapActivity.FRIEND_ID, mFriendId);
        mapIntent.putExtra(MapActivity.MY_WISH, false);
        startActivity(mapIntent);
        return true;
    }
}

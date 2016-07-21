package com.wish.wishlist.friend;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v7.widget.SearchView;
import android.widget.Toast;

import com.parse.ParseUser;
import com.wish.wishlist.R;
import com.wish.wishlist.util.Analytics;
import com.wish.wishlist.util.NetworkHelper;

import java.util.List;

public class FindFriendsActivity extends FriendsBaseActivity implements
        FriendManager.onFoundUserListener,
        FriendManager.onRequestFriendListener,
        AddFriendAdapter.AddFriendListener {

    final static String TAG = "FindFriendsActivity";
    private MenuItem _menuSearch;
    String mSearchQuery;
    private AddFriendAdapter mAddFriendAdapter;
    private int mFriendsLoaded = 0;
    private boolean mLoading = false;
    private boolean mAllLoaded = false;
    private int VISIBLE_THRESHOLD = 0; // the number of remaining elements before starting to load next batch


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Analytics.sendScreen("FindFriends");
        disableDrawer();
        mSwipeRefreshLayout.setEnabled(false);

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (mAllLoaded) {
                    return;
                }

                int totalItemCount = mLayoutManager.getItemCount();
                int visibleItemCount = mLayoutManager.getChildCount();
                int firstVisibleItemPosition = mLayoutManager.findFirstVisibleItemPosition();

                if (!mLoading && (totalItemCount - visibleItemCount)
                        <= (firstVisibleItemPosition + VISIBLE_THRESHOLD)) {
                    // End has been reached

                    mLoading = true;
                    if (!NetworkHelper.getInstance().isNetworkAvailable()) {
                        Toast.makeText(FindFriendsActivity.this, "Check network", Toast.LENGTH_LONG).show();
                        mLoading = false;
                        return;
                    }
                    FriendManager.getInstance().fetchUsers(mSearchQuery, mFriendsLoaded);
                }
            }
        });

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        // check if the activity is started from search
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            // activity is started from search, get the search query and
            // displayed the searched items
            SearchView searchView = (SearchView) _menuSearch.getActionView();
            searchView.clearFocus();
            mSearchQuery = intent.getStringExtra(SearchManager.QUERY);
            FriendManager.getInstance().setFoundUserListener(this);

            mFriendsLoaded = 0;
            mAllLoaded = false;
            mAddFriendAdapter = null;

            mLoading = true;
            showProgressDialog("Loading...");
            FriendManager.getInstance().fetchUsers(mSearchQuery, 0);
        } else {
            // activity is not started from search
        }
    }

    @Override
    protected boolean swipeEnabled() {
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_find_friends, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        _menuSearch = menu.findItem(R.id.find_friends_menu_search);
        MenuItemCompat.expandActionView(_menuSearch);

        // when the "UP" button in the toolbar with a searchView is tapped, the search view is collapsed,
        // but the activity stays open. If we want to close the activity, we need to call finish() onMenuItemActionCollapse
        MenuItemCompat.setOnActionExpandListener(_menuSearch, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                finish();
                return true;
            }
        });

        SearchView searchView = (SearchView) MenuItemCompat.getActionView(_menuSearch);
        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
        return true;
    }

    @Override
    public void onFoundUser(final List<ParseUser> users, final boolean success) {
        Log.d(TAG, "onFoundUsers");
        mProgressDialog.dismiss();

        if (!success) {
            Toast.makeText(this, "Check network", Toast.LENGTH_LONG).show();
            mLoading = false;
            return;
        }

        Log.d(TAG, "Found " + users.size() + " users");
        if (users.size() == 0) {
            if (mFriendsLoaded == 0) {
                Toast.makeText(this, "No user found", Toast.LENGTH_LONG).show();
            }
            mAllLoaded = true;
        }

        mFriendsLoaded += users.size();
        Log.d(TAG, "Friends loaded " + mFriendsLoaded);
        if (mAddFriendAdapter == null) {
            mAddFriendAdapter = new AddFriendAdapter(getUserMetaList(users));
            mAddFriendAdapter.setAddFriendListener(this);
            mRecyclerView.swapAdapter(mAddFriendAdapter, true);
        } else {
            mAddFriendAdapter.add(getUserMetaList(users));
        }

        mLoading = false;
    }

    @Override
    public void onAddFriend(final String friendId) {
        Log.d(TAG, "onAddFriend " + friendId);

        Analytics.send(Analytics.FRIEND, "Add", null);
        showProgressDialog("Sending friend request");

        FriendManager.getInstance().setRequestFriendListener(this);
        FriendManager.getInstance().requestFriend(friendId);
    }

    @Override
    public void onRequestFriendResult(final String friendId, final boolean success) {
        handleResult(friendId, success, "Friend request sent", mAddFriendAdapter);
    }
}

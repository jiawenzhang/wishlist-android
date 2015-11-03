package com.wish.wishlist.activity;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v7.widget.SearchView;
import android.widget.Toast;

import com.parse.ParseUser;
import com.wish.wishlist.R;
import com.wish.wishlist.friend.FriendManager;
import com.wish.wishlist.util.AddFriendAdapter;

import java.util.List;

public class FindFriends extends FriendsBase implements
        FriendManager.onFoundUserListener,
        FriendManager.onRequestFriendListener,
        AddFriendAdapter.addFriendListener {

    final static String TAG = "FindFriends";
    private MenuItem _menuSearch;
    private AddFriendAdapter mAddFriendAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        // check if the activity is started from search
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            //MenuItemCompat.collapseActionView(_menuSearch);
            // activity is started from search, get the search query and
            // displayed the searched items
            final String username = intent.getStringExtra(SearchManager.QUERY);
            if (username.equals(ParseUser.getCurrentUser().getUsername())) {
                Log.e(TAG, "Cannot add self as a friend");
                return;
            }
            showProgressDialog("Loading...");
            FriendManager.getInstance().setFoundUserListener(this);
            FriendManager.getInstance().findUser(username);
        } else {
            // activity is not started from search
        }
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
        mProgressDialog.dismiss();
        if (success) {
            Log.d(TAG, "Found " + users.size() + " users");
            if (users.size() == 0) {
                Toast.makeText(this, "No user found", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Check network", Toast.LENGTH_LONG).show();
        }
        mAddFriendAdapter = new AddFriendAdapter(getUserMetaList(users));
        mAddFriendAdapter.setAddFriendListener(this);
        mRecyclerView.swapAdapter(mAddFriendAdapter, true);
    }

    @Override
    public void onAddFriend(String friendId) {
        Log.d(TAG, "onAddFriend " + friendId);
        showProgressDialog("Sending friend request");

        FriendManager.getInstance().setRequestFriendListener(this);
        FriendManager.getInstance().requestFriend(friendId);
    }

    @Override
    public void onRequestFriendResult(final String friendId, final boolean success) {
        handleResult(friendId, success, "Friend request sent", mAddFriendAdapter);
    }
}

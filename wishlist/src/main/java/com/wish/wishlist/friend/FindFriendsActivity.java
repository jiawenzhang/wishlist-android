package com.wish.wishlist.friend;

import android.app.FragmentManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.app.DialogFragment;
import android.support.v4.view.MenuItemCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v7.widget.SearchView;
import android.widget.Toast;

import com.parse.ParseUser;
import com.wish.wishlist.R;
import com.wish.wishlist.fragment.InviteFriendFragmentDialog;

import java.util.ArrayList;
import java.util.List;

public class FindFriendsActivity extends FriendsBaseActivity implements
        FriendManager.onFoundUserListener,
        FriendManager.onRequestFriendListener,
        AddFriendAdapter.AddFriendListener,
        AddFriendAdapter.InviteFriendTapListener {

    final static String TAG = "FindFriendsActivity";
    private MenuItem _menuSearch;
    private AddFriendAdapter mAddFriendAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        disableDrawer();

        handleIntent(getIntent());
    }

    protected void loadView() {
        // show the top invite friend button
        mAddFriendAdapter = new AddFriendAdapter(new ArrayList<UserAdapter.UserMeta>());
        mAddFriendAdapter.setAddFriendListener(this);
        mAddFriendAdapter.setInviteFriendTapListener(this);
        mRecyclerView.swapAdapter(mAddFriendAdapter, true);
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
            final String searchQuery = intent.getStringExtra(SearchManager.QUERY);
            if (searchQuery.equals(ParseUser.getCurrentUser().getUsername())) {
                Log.e(TAG, "Cannot add self as a friend");
                return;
            }
            showProgressDialog("Loading...");
            FriendManager.getInstance().setFoundUserListener(this);
            FriendManager.getInstance().findUser(searchQuery);
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
        mAddFriendAdapter.setInviteFriendTapListener(this);
        mRecyclerView.swapAdapter(mAddFriendAdapter, true);
    }

    @Override
    public void onAddFriend(final String friendId) {
        Log.d(TAG, "onAddFriend " + friendId);
        showProgressDialog("Sending friend request");

        FriendManager.getInstance().setRequestFriendListener(this);
        FriendManager.getInstance().requestFriend(friendId);
    }

    @Override
    public void onRequestFriendResult(final String friendId, final boolean success) {
        handleResult(friendId, success, "Friend request sent", mAddFriendAdapter);
    }

    @Override
    public void onInviteFriendTap() {
        Log.d(TAG, "onInviteFriendTap");
        final FragmentManager manager = getFragmentManager();

        DialogFragment dialog = new InviteFriendFragmentDialog();
        dialog.show(manager, "dialog");
    }
}

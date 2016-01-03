package com.wish.wishlist.friend;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.parse.ParseUser;
import com.squareup.otto.Subscribe;
import com.wish.wishlist.R;
import com.wish.wishlist.event.EventBus;
import com.wish.wishlist.event.FriendListChangeEvent;
import com.wish.wishlist.util.NetworkHelper;
import com.wish.wishlist.wish.FriendsWishActivity;

import java.util.ArrayList;
import java.util.List;

public class FriendsActivity extends FriendsBaseActivity implements
        FriendAdapter.FriendTapListener,
        FriendAdapter.RemoveFriendListener,
        FriendAdapter.FriendRequestTapListener,
        FriendManager.onGotAllFriendsListener,
        FriendManager.onRemoveFriendResultListener {

    public static final String FRIEND_ID = "FRIEND_ID";
    public static final String FRIEND_NAME = "FRIEND_NAME";
    public static final String FRIEND_IMAGE_URL = "FRIEND_IMAGE_URL";
    final static String TAG = "FriendsActivity";
    private FriendAdapter mFriendAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // listen for FriendListChangeEvent
        EventBus.getInstance().register(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getInstance().unregister(this);
    }

    @Subscribe
    public void friendListChangeEvent(FriendListChangeEvent event) {
        FriendManager.getInstance().fetchFriendsFromCache();
    }

    @Override
    protected void prepareDrawerList() {
        mNavigationView.getMenu().findItem(R.id.Add).setTitle("Add friend");
        mNavigationView.getMenu().findItem(R.id.all_wishes).setVisible(false);
        mNavigationView.getMenu().findItem(R.id.list_view).setVisible(false);
        mNavigationView.getMenu().findItem(R.id.grid_view).setVisible(false);
        mNavigationView.getMenu().findItem(R.id.map_view).setVisible(false);
    }

    protected boolean onTapAdd() {
        final Intent findFriendIntent = new Intent(this, FindFriendsActivity.class);
        startActivity(findFriendIntent);
        return true;
    }

    protected void loadView() {
        // the friend request button at the top is an element in the adapter, we need to
        // setup the adapter and the recyclerview here to show it
        mFriendAdapter = new FriendAdapter(getUserMetaList(new ArrayList<ParseUser>()));
        mFriendAdapter.setFriendRequestTapListener(this);
        mRecyclerView.swapAdapter(mFriendAdapter, true);

        FriendManager.getInstance().setAllFriendsListener(this);
        FriendManager.getInstance().fetchFriends();

        if (!NetworkHelper.getInstance().isNetworkAvailable()) {
            Toast.makeText(this, "Check network, friends may be out of date", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void refreshFromNetwork() {
        FriendManager.getInstance().fetchFriendsFromNetwork();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_friends, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        int id = item.getItemId();
        if (id == R.id.menu_add_friends) {
            final Intent findFriendIntent = new Intent(this, FindFriendsActivity.class);
            startActivity(findFriendIntent);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onGotAllFriends(final List<ParseUser> friends) {
        Log.d(TAG, "onGotAllFriend " + friends.size());
        mFriendAdapter = new FriendAdapter(getUserMetaList(friends));
        mFriendAdapter.setFriendTapListener(this);
        mFriendAdapter.setRemoveFriendListener(this);
        mFriendAdapter.setFriendRequestTapListener(this);
        mRecyclerView.swapAdapter(mFriendAdapter, true);
        mSwipeRefreshLayout.setRefreshing(false);
    }

    public void onFriendTap(final UserAdapter.UserMeta friendMeta) {
        Log.d(TAG, "friend with objectId: " + friendMeta.objectId + " tapped");
        // show the friend's wishes
        final Intent friendsWishIntent = new Intent(this, FriendsWishActivity.class);
        friendsWishIntent.putExtra(FRIEND_ID, friendMeta.objectId);
        friendsWishIntent.putExtra(FRIEND_NAME, friendMeta.name);
        friendsWishIntent.putExtra(FRIEND_IMAGE_URL, friendMeta.imageUrl);
        startActivity(friendsWishIntent);
    }

    public void onRemoveFriend(final String friendId) {
        showProgressDialog("Removing friend");

        FriendManager.getInstance().setRemoveFriendResultListener(this);
        FriendManager.getInstance().removeFriend(friendId);
    }

    public void onRemoveFriendResult(final String friendId, final boolean success) {
        handleResult(friendId, success, "Removed", mFriendAdapter);
    }

    public void onFriendRequestTap() {
        final Intent friendRequestIntent = new Intent(getApplicationContext(), FriendRequestActivity.class);
        startActivity(friendRequestIntent);
    }
}

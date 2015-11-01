package com.wish.wishlist.activity;

import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.parse.ParseUser;
import com.wish.wishlist.R;
import com.wish.wishlist.friend.FriendManager;
import com.wish.wishlist.util.FriendAdapter;

import java.util.List;

public class Friends extends FriendsBase implements
        FriendAdapter.FriendTapListener,
        FriendAdapter.RemoveFriendListener,
        FriendManager.onGotAllFriendsListener,
        FriendManager.onRemoveFriendResultListener {

    public static final String FRIEND_ID = "FRIEND_ID";
    final static String TAG = "Friends";
    private FriendAdapter mFriendAdapter;

    protected void loadView() {
        FriendManager.getInstance().setAllFriendsListener(this);
        FriendManager.getInstance().fetchFriends();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_friends, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_add_friends) {
            final Intent findFriendIntent = new Intent(this, FindFriends.class);
            startActivity(findFriendIntent);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onGotAllFriends(List<ParseUser> friends) {
        Log.d(TAG, "onGotAllFriend " + friends.size());

        mFriendAdapter = new FriendAdapter(getUserMetaList(friends));
        mFriendAdapter.setFriendTapListener(this);
        mFriendAdapter.setmRemoveFriendListener(this);
        mRecyclerView.swapAdapter(mFriendAdapter, false);
    }

    public void onFriendTap(final String friendId) {
        Log.d(TAG, "friend with objectId: " + friendId + " tapped");
        // show the friend's wishes
        final Intent friendsWishIntent = new Intent(this, FriendsWish.class);
        friendsWishIntent.putExtra(FRIEND_ID, friendId);
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
}

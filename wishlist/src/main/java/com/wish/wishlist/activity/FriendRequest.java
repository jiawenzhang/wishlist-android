package com.wish.wishlist.activity;

import android.os.Bundle;
import android.util.Log;

import com.parse.ParseFile;
import com.parse.ParseUser;
import com.wish.wishlist.friend.FriendManager;
import com.wish.wishlist.util.AddFriendAdapter;
import com.wish.wishlist.util.FriendRequestAdapter;

import java.util.ArrayList;
import java.util.List;

public class FriendRequest extends FriendsBase implements
        FriendManager.onFriendRequestListener,
        FriendRequestAdapter.acceptFriendListener,
        FriendRequestAdapter.rejectFriendListener {

    final static String TAG = "FriendRequest";
    private FriendRequestAdapter mFriendRequestAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void loadView() {
        FriendManager.getInstance().setFriendRequestListener(this);
        FriendManager.getInstance().fetchFriendRequest();
    }

    public void onGotFriendRequest(List<ParseUser> friends) {
        Log.d(TAG, "onGotFriendRequest");
        mFriendRequestAdapter = new FriendRequestAdapter(getUserMetaList(friends));
        mFriendRequestAdapter.setAcceptFriendListener(this);
        mFriendRequestAdapter.setRejectFriendListener(this);
        mRecyclerView.swapAdapter(mFriendRequestAdapter, false);
    }

    @Override
    public void onAcceptFriend(final String friendId) {
        Log.d(TAG, "onAddFriend " + friendId);
        FriendManager.getInstance().acceptFriend(friendId);
    }

    @Override
    public void onRejectFriend(final String friendId) {
        Log.d(TAG, "onRejectFriend " + friendId);
        FriendManager.getInstance().rejectFriend(friendId);
    }
}

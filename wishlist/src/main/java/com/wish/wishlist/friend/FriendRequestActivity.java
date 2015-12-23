package com.wish.wishlist.friend;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;


public class FriendRequestActivity extends FriendsBaseActivity implements
        FriendManager.onFriendRequestListener,
        FriendManager.onAcceptFriendListener,
        FriendManager.onRejectFriendListener,
        FriendRequestAdapter.acceptFriendListener,
        FriendRequestAdapter.rejectFriendListener {

    final static String TAG = "FriendRequestActivity";
    private FriendRequestAdapter mFriendRequestAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        disableDrawer();
    }

    @Override
    protected void loadView() {
        FriendManager.getInstance().setFriendRequestListener(this);
        FriendManager.getInstance().fetchFriendRequest();

        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Check network, friend request may be out of date", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void refreshFromNetwork() {
        FriendManager.getInstance().fetchFriendRequestFromNetwork();
    }

    public void onGotFriendRequest() {
        Log.d(TAG, "onGotFriendRequest");
        mFriendRequestAdapter = new FriendRequestAdapter();
        mFriendRequestAdapter.setAcceptFriendListener(this);
        mFriendRequestAdapter.setRejectFriendListener(this);
        mRecyclerView.swapAdapter(mFriendRequestAdapter, true);

        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onAcceptFriend(final String friendId) {
        Log.d(TAG, "onAddFriend " + friendId);
        showProgressDialog("Accepting friend");

        FriendManager.getInstance().setAcceptFriendListener(this);
        FriendManager.getInstance().acceptFriend(friendId);
    }

    @Override
    public void onAcceptFriendResult(final String friendId, final boolean success) {
        handleResult(friendId, success, "Accepted", mFriendRequestAdapter);
    }

    @Override
    public void onRejectFriend(final String friendId) {
        Log.d(TAG, "onRejectFriend " + friendId);
        showProgressDialog("Rejecting friend");

        FriendManager.getInstance().setRejectFriendListener(this);
        FriendManager.getInstance().rejectFriend(friendId);
    }

    @Override
    public void onRejectFriendResult(final String friendId, final boolean success) {
        handleResult(friendId, success, "Rejected", mFriendRequestAdapter);
    }
}

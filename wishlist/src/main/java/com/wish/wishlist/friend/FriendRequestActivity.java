package com.wish.wishlist.friend;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.wish.wishlist.R;
import com.wish.wishlist.util.Analytics;
import com.wish.wishlist.util.NetworkHelper;
import com.wish.wishlist.util.VisibleActivityTracker;


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

        Analytics.sendScreen("FriendRequest");
        disableDrawer();
        loadView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        VisibleActivityTracker.getInstance().activityResumed(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        VisibleActivityTracker.getInstance().activityPaused();
    }

    protected void loadView() {
        FriendManager.getInstance().setFriendRequestListener(this);
        FriendManager.getInstance().fetchFriendRequest();

        if (!NetworkHelper.getInstance().isNetworkAvailable()) {
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

        if (FriendRequestCache.getInstance().friendRequestList().size() == 0) {
            showNoRequestView();
        } else {
            mTxtEmpty.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onAcceptFriend(final String friendId) {
        Log.d(TAG, "onAddFriend " + friendId);
        Analytics.send(Analytics.FRIEND, "Accept", null);
        showProgressDialog("Accepting friend");

        FriendManager.getInstance().setAcceptFriendListener(this);
        FriendManager.getInstance().acceptFriend(friendId);
    }

    @Override
    public void onAcceptFriendResult(final String friendId, final boolean success) {
        handleResult(friendId, success, "Accepted", mFriendRequestAdapter);
        if (mFriendRequestAdapter.getItemCount() == 0) {
            showNoRequestView();
        }
    }

    @Override
    public void onRejectFriend(final String friendId) {
        Log.d(TAG, "onRejectFriend " + friendId);
        Analytics.send(Analytics.FRIEND, "Reject", null);
        showProgressDialog("Rejecting friend");

        FriendManager.getInstance().setRejectFriendListener(this);
        FriendManager.getInstance().rejectFriend(friendId);
    }

    @Override
    public void onRejectFriendResult(final String friendId, final boolean success) {
        handleResult(friendId, success, "Rejected", mFriendRequestAdapter);
        if (mFriendRequestAdapter.getItemCount() == 0) {
            showNoRequestView();
        }
    }

    private void showNoRequestView() {
        Log.d(TAG, "No friend request");
        mTxtEmpty.setText(R.string.no_friend_request);
        mTxtEmpty.setVisibility(View.VISIBLE);
        mRecyclerView.setVisibility(View.GONE);
    }
}

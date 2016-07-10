package com.wish.wishlist.friend;

import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.parse.ParseUser;
import com.squareup.otto.Subscribe;
import com.wish.wishlist.R;
import com.wish.wishlist.event.EventBus;
import com.wish.wishlist.event.FriendListChangeEvent;
import com.wish.wishlist.fragment.FriendOptionDialogFragment;
import com.wish.wishlist.fragment.InviteFriendFragmentDialog;
import com.wish.wishlist.util.NetworkHelper;
import com.wish.wishlist.util.Options;
import com.wish.wishlist.util.VisibleActivityTracker;
import com.wish.wishlist.wish.FriendsWishActivity;

import java.util.ArrayList;
import java.util.List;

import static com.wish.wishlist.R.style.AppCompatAlertDialogStyle;

public class FriendsActivity extends FriendsBaseActivity implements
        FriendAdapter.FriendTapListener,
        FriendAdapter.FriendMoreListener,
        FriendAdapter.FriendRequestTapListener,
        FriendManager.onGotAllFriendsListener,
        FriendManager.onRemoveFriendResultListener,
        FriendOptionDialogFragment.RemoveFriendListener {

    public static final String FRIEND_ID = "FRIEND_ID";
    public static final String FRIEND_NAME = "FRIEND_NAME";
    public static final String FRIEND_USERNAME = "FRIEND_USERNAME";
    public static final String FRIEND_IMAGE_URL = "FRIEND_IMAGE_URL";
    final static String TAG = "FriendsActivity";
    private FriendAdapter mFriendAdapter;
    private String mSelectedFriendId;
    private Button mAddFriendButton;
    private Button mInviteFriendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAddFriendButton = (Button) findViewById(R.id.addFriendButton);
        mAddFriendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addFriend();
            }
        });

        mInviteFriendButton = (Button) findViewById(R.id.inviteFriendButton);
        mInviteFriendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inviteFriend();
            }
        });

        // listen for FriendListChangeEvent
        EventBus.getInstance().register(this);
        clearRingIcon();

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

    @Override
    protected void onNewIntent(Intent intent) {
        clearRingIcon();
    }

    private void clearRingIcon() {
        hideRingIcon();
        Options.ShowNewFriendNotification showNotification = new Options.ShowNewFriendNotification(0);
        showNotification.save();
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
        mNavigationView.getMenu().findItem(R.id.Add).setVisible(false);
        mNavigationView.getMenu().findItem(R.id.all_wishes).setVisible(false);
        mNavigationView.getMenu().findItem(R.id.list_view).setVisible(false);
        mNavigationView.getMenu().findItem(R.id.grid_view).setVisible(false);
        mNavigationView.getMenu().findItem(R.id.map_view).setVisible(false);
        mNavigationView.getMenu().findItem(R.id.friends).setVisible(false);
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
            Log.d(TAG, "onAddFriendTap");
            addFriend();
            return true;
        } else if (id == R.id.menu_invite_friends) {
            Log.d(TAG, "onInviteFriendTap");
            inviteFriend();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onGotAllFriends(final List<ParseUser> friends) {
        Log.d(TAG, "onGotAllFriend " + friends.size());
        List<UserAdapter.UserMeta> userMetaList = getUserMetaList(friends);
        mFriendAdapter = new FriendAdapter(userMetaList);
        mFriendAdapter.setFriendTapListener(this);
        mFriendAdapter.setFriendMoreListener(this);
        mFriendAdapter.setFriendRequestTapListener(this);
        mRecyclerView.swapAdapter(mFriendAdapter, true);
        mSwipeRefreshLayout.setRefreshing(false);

        if (userMetaList.size() == 0) {
            Log.d(TAG, "No friends");
            mTxtEmpty.setText(R.string.no_friends);
            mTxtEmpty.setVisibility(View.VISIBLE);
            mAddFriendButton.setVisibility(View.VISIBLE);
            mInviteFriendButton.setVisibility(View.VISIBLE);
            ViewGroup.LayoutParams params = mRecyclerView.getLayoutParams();
            params.height = (int) getResources().getDimension(R.dimen.recyclerview_top_button_height) + 1;
        } else {
            mTxtEmpty.setVisibility(View.GONE);
            mAddFriendButton.setVisibility(View.GONE);
            mInviteFriendButton.setVisibility(View.GONE);
            ViewGroup.LayoutParams params = mRecyclerView.getLayoutParams();
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        }
    }

    public void onFriendTap(final UserAdapter.UserMeta friendMeta) {
        Log.d(TAG, "friend with objectId: " + friendMeta.objectId + " tapped");
        // show the friend's wishes
        final Intent friendsWishIntent = new Intent(this, FriendsWishActivity.class);
        friendsWishIntent.putExtra(FRIEND_ID, friendMeta.objectId);
        friendsWishIntent.putExtra(FRIEND_NAME, friendMeta.name);
        friendsWishIntent.putExtra(FRIEND_USERNAME, friendMeta.username);
        friendsWishIntent.putExtra(FRIEND_IMAGE_URL, friendMeta.imageUrl);
        startActivity(friendsWishIntent);
    }

    public void onFriendMore(final String friendId) {

        mSelectedFriendId = friendId;

        FragmentManager manager = getFragmentManager();
        FriendOptionDialogFragment dialog = new FriendOptionDialogFragment();
        dialog.setRemoveFriendListener(this);
        dialog.show(manager, "dialog");
    }

    public void onRemoveFriend() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, AppCompatAlertDialogStyle);
        String message = "Sure to remove this friend?";
        builder.setMessage(message);
        builder.setPositiveButton("REMOVE", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                showProgressDialog("Removing friend");

                FriendManager.getInstance().setRemoveFriendResultListener(FriendsActivity.this);
                FriendManager.getInstance().removeFriend(mSelectedFriendId);
            }
        });
        builder.setNegativeButton("CANCEL",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        AlertDialog dialog;
        dialog = builder.create();
        dialog.show();
    }

    public void onRemoveFriendResult(final String friendId, final boolean success) {
        handleResult(friendId, success, "Removed", mFriendAdapter);
    }

    public void onFriendRequestTap() {
        final Intent friendRequestIntent = new Intent(getApplicationContext(), FriendRequestActivity.class);
        startActivity(friendRequestIntent);
    }

    private void inviteFriend() {
        FragmentManager manager = getFragmentManager();
        DialogFragment dialog = new InviteFriendFragmentDialog();
        dialog.show(manager, "dialog");
    }

    private void addFriend() {
        final Intent findFriendIntent = new Intent(this, FindFriendsActivity.class);
        startActivity(findFriendIntent);
    }

}

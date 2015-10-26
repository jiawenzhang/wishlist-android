package com.wish.wishlist.activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
        if (friends.isEmpty()) {
            Log.d(TAG, "no friend request");
            return;
        }

        ArrayList<AddFriendAdapter.UserMeta> userMetaList = new ArrayList<>();
        for (final ParseUser user : friends) {
            final ParseFile parseImage = user.getParseFile("profileImage");
            Bitmap bitmap = null;
            if (parseImage != null) {
                try {
                    bitmap = BitmapFactory.decodeByteArray(parseImage.getData(), 0, parseImage.getData().length);
                } catch (com.parse.ParseException e) {
                    Log.e(TAG, e.toString());
                }
            }
            AddFriendAdapter.UserMeta userMeta = new AddFriendAdapter.UserMeta(user.getObjectId(), user.getString("name"), user.getUsername(), bitmap);
            userMetaList.add(userMeta);
        }
        mFriendRequestAdapter = new FriendRequestAdapter(userMetaList);
        mFriendRequestAdapter.setAcceptFriendListener(this);
        mFriendRequestAdapter.setRejectFriendListener(this);
        mRecyclerView.setAdapter(mFriendRequestAdapter);
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

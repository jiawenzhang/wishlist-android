package com.wish.wishlist.util;

/**
 * Created by jiawen on 15-10-05.
 */

import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import java.util.ArrayList;

public class FriendRequestAdapter extends UserAdapter {

    public interface acceptFriendListener {
        void onAcceptFriend(String friendId);
    }

    protected void onAcceptFriend(String friendId) {
        if (mAcceptFriendListener != null) {
            mAcceptFriendListener.onAcceptFriend(friendId);
        }
    }

    public FriendRequestAdapter(ArrayList<UserMeta> userData) {
        super(userData);
    }

    private acceptFriendListener mAcceptFriendListener = null;
    private static final String TAG = "FriendRequestAdapter";

    public void setAcceptFriendListener(acceptFriendListener listener)
    {
        mAcceptFriendListener = listener;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your data set at this position
        // - replace the contents of the view with that element
        super.onBindViewHolder(holder, position);

        final UserMeta userMeta = mUserMetaList.get(position);
        holder.buttonAddFriend.setText("Accept friend");
        holder.buttonAddFriend.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "accept friend button clicked");
                onAcceptFriend(userMeta.objectId);
            }
        });
    }
}

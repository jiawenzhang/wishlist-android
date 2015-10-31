package com.wish.wishlist.util;

/**
 * Created by jiawen on 15-10-05.
 */

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.wish.wishlist.R;

import java.util.List;

public class FriendRequestAdapter extends UserAdapter {

    /******************** acceptFriendListener*************************/
    public interface acceptFriendListener {
        void onAcceptFriend(final String friendId);
    }
    private void onAcceptFriend(final String friendId) {
        if (mAcceptFriendListener != null) {
            mAcceptFriendListener.onAcceptFriend(friendId);
        }
    }
    private acceptFriendListener mAcceptFriendListener = null;
    public void setAcceptFriendListener(acceptFriendListener listener)
    {
        mAcceptFriendListener = listener;
    }

    /******************** rejectFriendListener*************************/
    public interface rejectFriendListener {
        void onRejectFriend(final String friendId);
    }
    private void onRejectFriend(final String friendId) {
        if (mRejectFriendListener != null) {
            mRejectFriendListener.onRejectFriend(friendId);
        }
    }
    private rejectFriendListener mRejectFriendListener = null;
    public void setRejectFriendListener(rejectFriendListener listener)
    {
        mRejectFriendListener = listener;
    }


    private static final String TAG = "FriendRequestAdapter";

    public FriendRequestAdapter(List<UserMeta> userData) {
        super(userData);
    }

    @Override
    public UserAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                     int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.user, parent, false);
        // set the view's size, margins, padding and layout parameters
        setUserProfileLayoutWidth(150, v); // make sure two buttons are fully shown
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        // - get element from your data set at this position
        // - replace the contents of the view with that element
        super.onBindViewHolder(holder, position);

        final UserMeta userMeta = mUserMetaList.get(position);

        holder.button1.setText("Accept");
        holder.button1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Accept friend button clicked");
                onAcceptFriend(userMeta.objectId);
            }
        });

        holder.button2.setText("Reject");
        holder.button2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Reject friend button clicked");
                onRejectFriend(userMeta.objectId);
            }
        });
    }
}

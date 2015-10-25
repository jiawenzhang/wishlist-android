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
                Log.d(TAG, "accept friend button clicked");
                onAcceptFriend(userMeta.objectId);
                remove(position);
            }
        });

        holder.button2.setText("Reject");
        holder.button2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "reject friend button clicked");
                //onAcceptFriend(userMeta.objectId);
                remove(position);
            }
        });
    }
}

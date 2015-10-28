package com.wish.wishlist.util;

/**
 * Created by jiawen on 15-10-05.
 */

import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import com.wish.wishlist.friend.FriendManager;

import java.util.List;

public class FriendAdapter extends UserAdapter {

    public interface FriendTapListener {
        void onFriendTap(String friendId);
    }

    protected void onFriendTap(String friendId) {
        if (mFriendTapListener != null) {
            mFriendTapListener.onFriendTap(friendId);
        }
    }

    public FriendAdapter(List<UserMeta> userData) {
        super(userData);
    }

    private FriendTapListener mFriendTapListener = null;
    private static final String TAG = "FriendAdapter";

    public void setFriendTapListener(FriendTapListener listener)
    {
        mFriendTapListener = listener;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        super.onBindViewHolder(holder, position);

        final UserMeta userMeta = mUserMetaList.get(position);
        holder.button1.setVisibility(View.GONE);
        holder.button2.setText("Remove");
        holder.button2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "remove friend button clicked");
                FriendManager.getInstance().removeFriend(userMeta.objectId);
                remove(position);
            }
        });

        holder.userLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "friend clicked");
                onFriendTap(userMeta.objectId);
            }
        });
    }
}

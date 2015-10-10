package com.wish.wishlist.util;

/**
 * Created by jiawen on 15-10-05.
 */

import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import java.util.ArrayList;

public class FriendAdapter extends UserAdapter {

    public interface FriendTapListener {
        void onFriendTap(String friendId);
    }

    protected void onFriendTap(String friendId) {
        if (mFriendTapListener != null) {
            mFriendTapListener.onFriendTap(friendId);
        }
    }

    public FriendAdapter(ArrayList<UserMeta> userData) {
        super(userData);
    }

    private FriendTapListener mFriendTapListener = null;
    private static final String TAG = "FriendAdapter";

    public void setFriendTapListener(FriendTapListener listener)
    {
        mFriendTapListener = listener;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);

        final UserMeta userMeta = mUserMetaList.get(position);
        holder.button.setVisibility(View.GONE);
        holder.userLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "friend clicked");
                onFriendTap(userMeta.objectId);
            }
        });
    }
}

package com.wish.wishlist.util;

/**
 * Created by jiawen on 15-10-05.
 */

import android.view.View;

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

        //final UserMeta userMeta = mUserMetaList.get(position);
        holder.button.setVisibility(View.GONE);
    }
}

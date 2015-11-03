package com.wish.wishlist.util;

/**
 * Created by jiawen on 15-10-05.
 */

import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;


import java.util.List;

public class FriendAdapter extends UserAdapter {

    /******************* FriendTapListener *********************/
    private FriendTapListener mFriendTapListener = null;
    public interface FriendTapListener {
        void onFriendTap(final String friendId);
    }
    protected void onFriendTap(final String friendId) {
        if (mFriendTapListener != null) {
            mFriendTapListener.onFriendTap(friendId);
        }
    }
    public void setFriendTapListener(final FriendTapListener listener)
    {
        mFriendTapListener = listener;
    }

    /******************* RemoveFriendListener *********************/
    private RemoveFriendListener mRemoveFriendListener = null;
    public interface RemoveFriendListener {
        void onRemoveFriend(final String friendId);
    }
    protected void onRemoveFriend(final String friendId) {
        if (mRemoveFriendListener != null) {
            mRemoveFriendListener.onRemoveFriend(friendId);
        }
    }
    public void setRemoveFriendListener(RemoveFriendListener listener)
    {
        mRemoveFriendListener = listener;
    }
    /*************************************************************/


    public FriendAdapter(List<UserMeta> userData) {
        super(userData);
    }

    private static final String TAG = "FriendAdapter";


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
                onRemoveFriend(userMeta.objectId);
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

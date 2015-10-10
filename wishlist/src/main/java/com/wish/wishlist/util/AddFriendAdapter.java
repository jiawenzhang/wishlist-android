package com.wish.wishlist.util;

/**
 * Created by jiawen on 15-10-05.
 */
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import java.util.ArrayList;

public class AddFriendAdapter extends UserAdapter {

    public interface addFriendListener {
        void onAddFriend(String friendId);
    }

    protected void onAddFriend(String friendId) {
        if (mAddFriendListener != null) {
            mAddFriendListener.onAddFriend(friendId);
        }
    }

    public AddFriendAdapter(ArrayList<UserMeta> userData) {
        super(userData);
    }

    private addFriendListener mAddFriendListener = null;
    private static final String TAG = "AddFriendAdapter";

    public void setAddFriendListener(addFriendListener listener)
    {
        mAddFriendListener = listener;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your data set at this position
        // - replace the contents of the view with that element
        super.onBindViewHolder(holder, position);
        final UserMeta userMeta = mUserMetaList.get(position);
        holder.buttonAddFriend.setText("Add friend");
        holder.buttonAddFriend.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "add friend button clicked");
                onAddFriend(userMeta.objectId);
            }
        });
    }
}

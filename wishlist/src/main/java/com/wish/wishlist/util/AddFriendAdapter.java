package com.wish.wishlist.util;

/**
 * Created by jiawen on 15-10-05.
 */
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import java.util.ArrayList;

public class AddFriendAdapter extends UserAdapter {
    private addFriendListener mAddFriendListener = null;
    private static final String TAG = "AddFriendAdapter";

    public interface addFriendListener {
        void onAddFriend(String friendId);
    }

    protected void onAddFriend(String friendId) {
        if (mAddFriendListener != null) {
            mAddFriendListener.onAddFriend(friendId);
        }
    }

    public void setAddFriendListener(addFriendListener listener)
    {
        mAddFriendListener = listener;
    }

    public AddFriendAdapter(ArrayList<UserMeta> userData) {
        super(userData);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your data set at this position
        // - replace the contents of the view with that element
        super.onBindViewHolder(holder, position);
        final UserMeta userMeta = mUserMetaList.get(position);
        holder.button.setText("Add friend");
        holder.button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "add friend button clicked");
                onAddFriend(userMeta.objectId);
            }
        });
    }
}

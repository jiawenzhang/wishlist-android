package com.wish.wishlist.util;

/**
 * Created by jiawen on 15-10-05.
 */
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.wish.wishlist.R;

import java.util.List;

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

    public AddFriendAdapter(List<UserMeta> userData) {
        super(userData);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.user, parent, false);
        // set the view's size, margins, padding and layout parameters
        Button button1 = (Button) v.findViewById(R.id.button1);
        button1.setVisibility(View.GONE);
        setUserProfileLayoutWidth(200, v); // make sure one button is fully shown
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        // - get element from your data set at this position
        // - replace the contents of the view with that element
        super.onBindViewHolder(holder, position);

        ViewHolder holder_ = (ViewHolder) holder;
        final UserMeta userMeta = mUserMetaList.get(position);
        holder_.button2.setText("Add friend");
        holder_.button2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "add friend button clicked");
                onAddFriend(userMeta.objectId);
                //holder.button2.setText("Request sent");
                //holder.button2.setEnabled(false);
            }
        });
    }
}

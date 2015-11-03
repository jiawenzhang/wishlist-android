package com.wish.wishlist.util;

/**
 * Created by jiawen on 15-10-05.
 */

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.squareup.picasso.Picasso;
import com.wish.wishlist.R;
import com.wish.wishlist.friend.FriendRequestCache;
import com.wish.wishlist.friend.FriendRequestMeta;

import java.util.ListIterator;

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

    public FriendRequestAdapter() {
        super();
    }

    public void add(final int position, final FriendRequestMeta item) {
        FriendRequestCache.getInstance().friendRequestList().add(position, item);
        notifyItemInserted(position);
    }

    @Override
    protected void remove(final int position) {
        FriendRequestCache.getInstance().friendRequestList().remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public void remove(final String userId) {
        ListIterator<FriendRequestMeta> it = FriendRequestCache.getInstance().friendRequestList().listIterator();
        int count = 0;
        while (it.hasNext()) {
            if (it.next().objectId.equals(userId)) {
                int position = it.previousIndex();
                Log.d(TAG, "position " + position);
                it.remove();
                notifyItemRemoved(position);
                if (++count == 2) {
                    break;
                }
            }
        }
    }

    // Return the size of your data set (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return FriendRequestCache.getInstance().friendRequestList().size();
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
        // super.onBindViewHolder(holder, position);
        final FriendRequestMeta meta = FriendRequestCache.getInstance().friendRequestList().get(position);
        if (meta.imageUrl != null) {
            Picasso.with(holder.imgProfile.getContext()).load(meta.imageUrl).fit().into(holder.imgProfile);
        } else {
            holder.imgProfile.setImageResource(R.drawable.default_profile_image);
        }
        holder.txtName.setText(meta.name);
        holder.txtUsername.setText(meta.username);

        if (meta.fromMe) {
            Log.d(TAG, "request from me");
            holder.button1.setVisibility(View.GONE);
            holder.button2.setText("Pending");
            holder.button2.setEnabled(false);
        } else {
            Log.d(TAG, "request to me");
            holder.button1.setText("Accept");
            holder.button1.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Accept friend button clicked");
                    onAcceptFriend(meta.objectId);
                }
            });

            holder.button2.setText("Reject");
            holder.button2.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Reject friend button clicked");
                    onRejectFriend(meta.objectId);
                }
            });
        }
    }
}

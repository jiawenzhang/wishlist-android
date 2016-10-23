package com.wish.wishlist.friend;

/**
 * Created by jiawen on 15-10-05.
 */

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.squareup.picasso.Picasso;
import com.wish.wishlist.R;
import com.wish.wishlist.util.ProfileUtil;

import java.util.ListIterator;

class FriendRequestAdapter extends UserAdapter {

    /******************** acceptFriendListener*************************/
    interface acceptFriendListener {
        void onAcceptFriend(final String friendId);
    }
    private void onAcceptFriend(final String friendId) {
        if (mAcceptFriendListener != null) {
            mAcceptFriendListener.onAcceptFriend(friendId);
        }
    }
    private acceptFriendListener mAcceptFriendListener = null;
    void setAcceptFriendListener(acceptFriendListener listener) {
        mAcceptFriendListener = listener;
    }

    /******************** rejectFriendListener*************************/
    interface rejectFriendListener {
        void onRejectFriend(final String friendId);
    }
    private void onRejectFriend(final String friendId) {
        if (mRejectFriendListener != null) {
            mRejectFriendListener.onRejectFriend(friendId);
        }
    }
    private rejectFriendListener mRejectFriendListener = null;
    void setRejectFriendListener(rejectFriendListener listener) {
        mRejectFriendListener = listener;
    }


    private static final String TAG = "FriendRequestAdapter";

    FriendRequestAdapter() {
        super();
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
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        // - get element from your data set at this position
        // - replace the contents of the view with that element
        // super.onBindViewHolder(holder, position);

        ViewHolder holder_ = (ViewHolder) holder;
        final FriendRequestMeta meta = FriendRequestCache.getInstance().friendRequestList().get(position);
        if (meta.imageUrl != null) {
            Picasso.with(holder_.imgProfile.getContext()).load(meta.imageUrl).fit().into(holder_.imgProfile);
        } else {
            int size = (int) holder_.imgProfile.getResources().getDimension(R.dimen.profile_image_size);
            holder_.imgProfile.setImageBitmap(ProfileUtil.generateProfileBitmap(meta.name, meta.username, size));
        }
        holder_.txtName.setText(meta.name);
        holder_.txtEmail.setText(meta.email);

        if (meta.fromMe) {
            Log.d(TAG, "request from me");
            holder_.button1.setVisibility(View.GONE);
            // pending
            holder_.button2.setImageResource(R.drawable.ic_action_friend_added_grey);
            holder_.button2.setEnabled(false);
        } else {
            Log.d(TAG, "request to me");
            // accept
            holder_.button1.setImageResource(R.drawable.ic_action_accept_grey);
            holder_.button1.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Accept friend button clicked");
                    onAcceptFriend(meta.objectId);
                }
            });
            holder_.button1.setVisibility(View.VISIBLE);
            holder_.button1.setEnabled(true);

            // reject
            holder_.button2.setImageResource(R.drawable.ic_action_cancel_grey);
            holder_.button2.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Reject friend button clicked");
                    onRejectFriend(meta.objectId);
                }
            });
            holder_.button2.setEnabled(true);
        }
    }
}

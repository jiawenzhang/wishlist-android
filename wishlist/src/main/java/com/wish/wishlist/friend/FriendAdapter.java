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

import com.wish.wishlist.R;
import com.wish.wishlist.util.TopButtonViewHolder;

import java.util.List;

public class FriendAdapter extends UserAdapter {

    /******************* FriendTapListener *********************/
    private FriendTapListener mFriendTapListener = null;
    public interface FriendTapListener {
        void onFriendTap(final UserMeta friendMeta);
    }
    protected void onFriendTap(final UserMeta friendMeta) {
        if (mFriendTapListener != null) {
            mFriendTapListener.onFriendTap(friendMeta);
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

    /******************* FriendRequestTapListener *********************/
    private FriendRequestTapListener mFriendRequestTapListener = null;
    public interface FriendRequestTapListener {
        void onFriendRequestTap();
    }
    protected void onFriendRequestTap() {
        if (mFriendRequestTapListener != null) {
            mFriendRequestTapListener.onFriendRequestTap();
        }
    }
    public void setFriendRequestTapListener(final FriendRequestTapListener listener)
    {
        mFriendRequestTapListener = listener;
    }
    /*************************************************************/


    public FriendAdapter(List<UserMeta> userData) {
        super(userData);
    }

    private static final String TAG = "FriendAdapter";

    // ViewType
    private static final int TOP_BUTTON = 0;
    private static final int FRIEND = 1;

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View topButtonView = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_top_button, parent, false);
        final View friendView = LayoutInflater.from(parent.getContext()).inflate(R.layout.user, parent, false);
        switch (viewType) {
            case TOP_BUTTON: return new TopButtonViewHolder(topButtonView);
            case FRIEND: return new ViewHolder(friendView);
            default: return new ViewHolder(friendView);
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {

        if (position == 0) {
            // top friend request button
            ((TopButtonViewHolder) holder).rootLayout.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "New friend tapped");
                    onFriendRequestTap();
                }
            });
            return;
        }

        ViewHolder holder_ = (ViewHolder) holder;

        // adjust position due to top friend request button
        final int adjusted_position = position - 1;
        final UserMeta userMeta = mUserMetaList.get(adjusted_position);
        holder_.button1.setVisibility(View.GONE);
        holder_.button2.setText("Remove");
        holder_.button2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "remove friend button clicked");
                onRemoveFriend(userMeta.objectId);
            }
        });

        holder_.userLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "friend clicked");
                onFriendTap(userMeta);
            }
        });

        super.onBindViewHolder(holder, adjusted_position);
    }

    @Override
    public void remove(final String userId) {
        for (int position = 0; position < mUserMetaList.size(); position++) {
            if (mUserMetaList.get(position).objectId.equals(userId)) {
                mUserMetaList.remove(position);
                notifyItemRemoved(position + 1);
                break;
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) { // top friend request button
            return TOP_BUTTON;
        } else { // friend
            return FRIEND;
        }
    }

    @Override
    public int getItemCount() {
        // + 1 is the for the top friend request button
        return mUserMetaList.size() + 1;
    }
}

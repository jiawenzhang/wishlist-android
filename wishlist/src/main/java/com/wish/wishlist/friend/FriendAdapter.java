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
import android.widget.ImageView;

import com.squareup.otto.Subscribe;
import com.wish.wishlist.R;
import com.wish.wishlist.event.EventBus;
import com.wish.wishlist.util.Options;
import com.wish.wishlist.util.TopButtonViewHolder;

import java.util.List;

class FriendAdapter extends UserAdapter {

    /******************* FriendTapListener *********************/
    private FriendTapListener mFriendTapListener = null;
    interface FriendTapListener {
        void onFriendTap(final UserMeta friendMeta);
    }
    private void onFriendTap(final UserMeta friendMeta) {
        if (mFriendTapListener != null) {
            mFriendTapListener.onFriendTap(friendMeta);
        }
    }
    void setFriendTapListener(final FriendTapListener listener) {
        mFriendTapListener = listener;
    }

    /******************* FriendMoreListener *********************/
    private FriendMoreListener mFriendMoreListener = null;
    interface FriendMoreListener {
        void onFriendMore(final String friendId);
    }
    private void onFriendMore(final String friendId) {
        if (mFriendMoreListener != null) {
            mFriendMoreListener.onFriendMore(friendId);
        }
    }
    void setFriendMoreListener(FriendMoreListener listener) {
        mFriendMoreListener = listener;
    }

    /******************* FriendRequestTapListener *********************/
    private FriendRequestTapListener mFriendRequestTapListener = null;
    interface FriendRequestTapListener {
        void onFriendRequestTap();
    }
    private void onFriendRequestTap() {
        if (mFriendRequestTapListener != null) {
            mFriendRequestTapListener.onFriendRequestTap();
        }
    }
    void setFriendRequestTapListener(final FriendRequestTapListener listener) {
        mFriendRequestTapListener = listener;
    }
    /*************************************************************/


    FriendAdapter(List<UserMeta> userData) {
        super(userData);
        EventBus.getInstance().register(this); //listen to ShowNewFriendRequestNotification
    }

    private static final String TAG = "FriendAdapter";

    // ViewType
    private static final int TOP_BUTTON = 0;
    private static final int FRIEND = 1;

    private ImageView mImgRedDot;

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
            // top button
            Options.ShowNewFriendRequestNotification showNotification = new Options.ShowNewFriendRequestNotification();
            showNotification.read();
            mImgRedDot = ((TopButtonViewHolder) holder).imgRedDot;
            if (showNotification.val() == 1) {
                mImgRedDot.setVisibility(View.VISIBLE);
            }

            ((TopButtonViewHolder) holder).rootLayout.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "TopButton tapped");
                    Options.ShowNewFriendRequestNotification showNotification = new Options.ShowNewFriendRequestNotification(0);
                    showNotification.save();
                    mImgRedDot.setVisibility(View.GONE);
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
        // remove
        holder_.button2.setImageResource(R.drawable.ic_action_three_dots_grey);
        holder_.button2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "more button clicked");
                onFriendMore(userMeta.objectId);
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

    @Subscribe
    public void newFriendRequest(com.wish.wishlist.event.ShowNewFriendRequestNotification event) {
        Log.d(TAG, "newFriendRequest");
        if (mImgRedDot != null) {
            mImgRedDot.setVisibility(View.VISIBLE);
        }
    }
}

package com.wish.wishlist.friend;

/**
 * Created by jiawen on 15-10-05.
 */
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.wish.wishlist.R;
import com.wish.wishlist.util.TopButtonViewHolder;

import java.util.List;

public class AddFriendAdapter extends UserAdapter {
    private AddFriendListener mAddFriendListener = null;
    private static final String TAG = "AddFriendAdapter";

    // ViewType
    private static final int TOP_BUTTON = 0;
    private static final int FRIEND = 1;

    /******************* InviteFriendTapListener *********************/
    private InviteFriendTapListener mInviteFriendTapListener = null;
    public interface InviteFriendTapListener {
        void onInviteFriendTap();
    }
    protected void onInviteFriendTap() {
        if (mInviteFriendTapListener != null) {
            mInviteFriendTapListener.onInviteFriendTap();
        }
    }
    public void setInviteFriendTapListener(final InviteFriendTapListener listener)
    {
        mInviteFriendTapListener = listener;
    }


    /******************* AddFriendListener *********************/
    public interface AddFriendListener {
        void onAddFriend(String friendId);
    }

    protected void onAddFriend(String friendId) {
        if (mAddFriendListener != null) {
            mAddFriendListener.onAddFriend(friendId);
        }
    }

    public void setAddFriendListener(AddFriendListener listener)
    {
        mAddFriendListener = listener;
    }
    /*************************************************************/

    public AddFriendAdapter(List<UserMeta> userData) {
        super(userData);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View topButtonView = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_top_button, parent, false);
        final View friendView = LayoutInflater.from(parent.getContext()).inflate(R.layout.user, parent, false);
        switch (viewType) {
            case TOP_BUTTON: {
                final TextView txtView = (TextView) topButtonView.findViewById(R.id.txt_view);
                txtView.setText("Invite friends");
                return new TopButtonViewHolder(topButtonView);
            }
            case FRIEND: {
                // set the view's size, margins, padding and layout parameters
                ImageButton button1 = (ImageButton) friendView.findViewById(R.id.button1);
                button1.setVisibility(View.GONE);
                return new ViewHolder(friendView);
            }
            default: return new ViewHolder(friendView);
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        // - get element from your data set at this position
        // - replace the contents of the view with that element
        if (position == 0) {
            // top invite friend button
            ((TopButtonViewHolder) holder).rootLayout.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onInviteFriendTap();
                }
            });
            return;
        }

        // adjust position due to top friend request button
        final int adjusted_position = position - 1;
        ViewHolder holder_ = (ViewHolder) holder;
        final UserMeta userMeta = mUserMetaList.get(adjusted_position);
        // add friend
        holder_.button2.setImageResource(R.drawable.ic_action_add_friend_grey);
        holder_.button2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onAddFriend(userMeta.objectId);
                //holder.button2.setText("Request sent");
                //holder.button2.setEnabled(false);
            }
        });

        super.onBindViewHolder(holder, adjusted_position);
    }

    public static void tintButton(@NonNull ImageButton button) {
        ColorStateList colours = button.getResources()
                .getColorStateList(R.color.button_color);
        Drawable d = DrawableCompat.wrap(button.getDrawable());
        DrawableCompat.setTintList(d, colours);
        button.setImageDrawable(d);
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
    public int getItemViewType(final int position) {
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

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

import com.wish.wishlist.R;

import java.util.List;

class AddFriendAdapter extends UserAdapter {
    private AddFriendListener mAddFriendListener = null;
    private static final String TAG = "AddFriendAdapter";

    /******************* AddFriendListener *********************/
    interface AddFriendListener {
        void onAddFriend(String friendId);
    }

    private void onAddFriend(String friendId) {
        if (mAddFriendListener != null) {
            mAddFriendListener.onAddFriend(friendId);
        }
    }

    void setAddFriendListener(AddFriendListener listener)
    {
        mAddFriendListener = listener;
    }
    /*************************************************************/

    AddFriendAdapter(List<UserMeta> userData) {
        super(userData);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View friendView = LayoutInflater.from(parent.getContext()).inflate(R.layout.user, parent, false);
        // set the view's size, margins, padding and layout parameters
        ImageButton button1 = (ImageButton) friendView.findViewById(R.id.button1);
        button1.setVisibility(View.GONE);
        return new ViewHolder(friendView);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        // - get element from your data set at this position
        // - replace the contents of the view with that element

        ViewHolder holder_ = (ViewHolder) holder;
        final UserMeta userMeta = mUserMetaList.get(position);
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

        super.onBindViewHolder(holder, position);
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
                notifyItemRemoved(position);
                break;
            }
        }
    }

    public void add(List<UserMeta> userMetaList) {
        int startPosition = mUserMetaList.size();
        mUserMetaList.addAll(userMetaList);
        notifyItemRangeInserted(startPosition, userMetaList.size());
    }

    @Override
    public int getItemCount() {
        return mUserMetaList.size();
    }
}

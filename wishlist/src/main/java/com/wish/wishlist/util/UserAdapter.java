package com.wish.wishlist.util;

/**
 * Created by jiawen on 15-10-05.
 */
import java.util.ArrayList;

import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.wish.wishlist.R;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    public static class UserMeta {
        String objectId;
        String name;
        String username;
        Bitmap image;

        public UserMeta(String objectId, String name, String username, Bitmap image) {
            this.objectId = objectId;
            this.name = name;
            this.username = username;
            this.image = image;
        }
    }

    public interface addFriendListener {
        void onAddFriend(String friendId);
    }

    protected void onAddFriend(String friendId) {
        if (mAddFriendListener != null) {
            mAddFriendListener.onAddFriend(friendId);
        }
    }

    public interface acceptFriendListener {
        void onAcceptFriend(String friendId);
    }

    protected void onAcceptFriend(String friendId) {
        if (mAcceptFriendListener != null) {
            mAcceptFriendListener.onAcceptFriend(friendId);
        }
    }

    private addFriendListener mAddFriendListener = null;
    private acceptFriendListener mAcceptFriendListener = null;
    private ArrayList<UserMeta> mUserMetaList;
    private static final String TAG = "UserAdapter";

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView txtName;
        public TextView txtUsername;
        public ImageView imgProfile;
        public Button buttonAddFriend;

        public ViewHolder(View v) {
            super(v);
            txtName= (TextView) v.findViewById(R.id.name);
            txtUsername = (TextView) v.findViewById(R.id.username);
            imgProfile = (ImageView) v.findViewById(R.id.profile_image);
            buttonAddFriend = (Button) v.findViewById(R.id.add_friend);
        }
    }

    public UserAdapter(ArrayList<UserMeta> userData) {
        mUserMetaList = userData;
    }

    public void setAddFriendListener(addFriendListener listener)
    {
        mAddFriendListener = listener;
    }

    public void setAcceptFriendListener(acceptFriendListener listener)
    {
        mAcceptFriendListener = listener;
    }

    public void add(int position, UserMeta item) {
        mUserMetaList.add(position, item);
        notifyItemInserted(position);
    }

    public void remove(String item) {
        int position = mUserMetaList.indexOf(item);
        mUserMetaList.remove(position);
        notifyItemRemoved(position);
    }

    // Create new views (invoked by the layout manager)
    @Override
    public UserAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.user, parent, false);
        // set the view's size, margins, padding and layout parameters
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your data set at this position
        // - replace the contents of the view with that element
        final UserMeta userMeta = mUserMetaList.get(position);
        if (userMeta.image != null) {
            holder.imgProfile.setImageBitmap(mUserMetaList.get(position).image);
        }
        holder.txtName.setText(userMeta.name);
        holder.txtUsername.setText(userMeta.username);
        holder.buttonAddFriend.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "add friend button clicked");
                onAddFriend(userMeta.objectId);
                onAcceptFriend(userMeta.objectId);
            }
        });
    }

    // Return the size of your data set (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mUserMetaList.size();
    }
}

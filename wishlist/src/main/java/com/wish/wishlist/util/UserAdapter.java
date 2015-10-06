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
import android.widget.ImageView;
import android.widget.TextView;

import com.wish.wishlist.R;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    public static class UserMeta {
        String name;
        String username;
        Bitmap image;

        public UserMeta(String name, String username, Bitmap image) {
            this.name = name;
            this.username = username;
            this.image = image;
        }
    }

    private ArrayList<UserMeta> mUserMetaList;
    private static final String TAG = "UserAdapter";

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView txtName;
        public TextView txtUsername;
        public ImageView imgProfile;

        public ViewHolder(View v) {
            super(v);
            txtName= (TextView) v.findViewById(R.id.name);
            txtUsername = (TextView) v.findViewById(R.id.username);
            imgProfile = (ImageView) v.findViewById(R.id.profile_image);
        }
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

    public UserAdapter(ArrayList<UserMeta> userData) {
        mUserMetaList = userData;
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
        holder.txtName.setText(userMeta.name);
        holder.txtUsername.setText(userMeta.username);
        holder.txtUsername.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "user name clicked");
            }
        });

        if (userMeta.image != null) {
            holder.imgProfile.setImageBitmap(mUserMetaList.get(position).image);
        }
    }

    // Return the size of your data set (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mUserMetaList.size();
    }
}

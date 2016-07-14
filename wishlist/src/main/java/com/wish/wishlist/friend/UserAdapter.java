package com.wish.wishlist.friend;

/**
 * Created by jiawen on 15-10-05.
 */

import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.wish.wishlist.R;
import com.wish.wishlist.util.ProfileUtil;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static class UserMeta {
        public String objectId;
        public String name;
        public String email;
        public String username;
        public String imageUrl;

        public UserMeta(final String objectId, final String name, final String email, final String username, final String imageUrl) {
            this.objectId = objectId;
            this.name = name;
            this.email = email;
            this.username = username;
            this.imageUrl = imageUrl;
        }
    }

    public class UserMetaNameComparator implements Comparator<UserMeta> {
        @Override
        public int compare(UserMeta o1, UserMeta o2) {
            if (o1.name == null || o2.name == null) {
                return 0;
            }
            return o1.name.compareTo(o2.name);
        }
    }

    protected List<UserMeta> mUserMetaList;
    private static final String TAG = "UserAdapter";


    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView txtName;
        public TextView txtEmail;
        public ImageView imgProfile;
        public ImageButton button1;
        public ImageButton button2;
        public LinearLayout userLayout;

        public ViewHolder(View v) {
            super(v);
            txtName= (TextView) v.findViewById(R.id.name);
            txtEmail = (TextView) v.findViewById(R.id.email);
            imgProfile = (ImageView) v.findViewById(R.id.profile_image);
            button1 = (ImageButton) v.findViewById(R.id.button1);
            button2 = (ImageButton) v.findViewById(R.id.button2);
            userLayout = (LinearLayout) v.findViewById(R.id.user_root_layout);
        }
    }

    public UserAdapter() {}
    public UserAdapter(List<UserMeta> userData) {
        mUserMetaList = userData;
        Collections.sort(mUserMetaList, new UserMetaNameComparator());
    }

    public void remove(final String userId) {
        for (int position = 0; position < mUserMetaList.size(); position++) {
            if (mUserMetaList.get(position).objectId.equals(userId)) {
                mUserMetaList.remove(position);
                notifyItemRemoved(position);
                break;
            }
        }
    }

    // Create new views (invoked by the layout manager)
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // set the view's size, margins, padding and layout parameters
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.user, parent, false);
        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        // - get element from your data set at this position
        // - replace the contents of the view with that element
        ViewHolder holder_ = (ViewHolder) holder;
        final UserMeta userMeta = mUserMetaList.get(position);
        if (userMeta.imageUrl != null) {
            Picasso.with(holder_.imgProfile.getContext()).load(userMeta.imageUrl).fit().into(holder_.imgProfile);
        } else {
            int size = (int) holder_.imgProfile.getResources().getDimension(R.dimen.profile_image_size);
            Bitmap profileBitmap = ProfileUtil.generateProfileBitmap(userMeta.name, userMeta.username, size);
            if (profileBitmap != null) {
                holder_.imgProfile.setImageBitmap(profileBitmap);
            } else {
                holder_.imgProfile.setImageResource(R.drawable.default_profile_image);
            }
        }
        holder_.txtName.setText(userMeta.name);
        holder_.txtEmail.setText(userMeta.email);
    }

    // Return the size of your data set (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mUserMetaList.size();
    }
}

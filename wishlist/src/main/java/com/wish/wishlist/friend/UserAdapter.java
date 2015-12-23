package com.wish.wishlist.friend;

/**
 * Created by jiawen on 15-10-05.
 */

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.wish.wishlist.R;
import com.wish.wishlist.WishlistApplication;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static class UserMeta {
        public String objectId;
        public String name;
        public String username;
        public String imageUrl;

        public UserMeta() {}
        public UserMeta(final String objectId, final String name, final String username, final String imageUrl) {
            this.objectId = objectId;
            this.name = name;
            this.username = username;
            this.imageUrl = imageUrl;
        }
    }

    public class UserMetaNameComparator implements Comparator<UserMeta> {
        @Override
        public int compare(UserMeta o1, UserMeta o2) {
            return o1.name.compareTo(o2.name);
        }
    }

    protected List<UserMeta> mUserMetaList;
    private static final String TAG = "UserAdapter";


    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView txtName;
        public TextView txtUsername;
        public ImageView imgProfile;
        public Button button1;
        public Button button2;
        public LinearLayout userLayout;

        public ViewHolder(View v) {
            super(v);
            txtName= (TextView) v.findViewById(R.id.name);
            txtUsername = (TextView) v.findViewById(R.id.username);
            imgProfile = (ImageView) v.findViewById(R.id.profile_image);
            button1 = (Button) v.findViewById(R.id.button1);
            button2 = (Button) v.findViewById(R.id.button2);
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

    protected void setUserProfileLayoutWidth(final int width_dp, final View v) {
        RelativeLayout l = (RelativeLayout) v.findViewById(R.id.user_profile_layout);
        final float scale = WishlistApplication.getAppContext().getResources().getDisplayMetrics().density;
        final int width_px = (int) (width_dp * scale + 0.5f);
        l.getLayoutParams().width = width_px;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        // - get element from your data set at this position
        // - replace the contents of the view with that element
        ViewHolder holder_ = (ViewHolder) holder;
        final UserMeta userMeta = mUserMetaList.get(position);
        if (userMeta.imageUrl != null) {
            Picasso.with(holder_.imgProfile.getContext()).load(mUserMetaList.get(position).imageUrl).fit().into(holder_.imgProfile);
        } else {
            holder_.imgProfile.setImageResource(R.drawable.default_profile_image);
        }
        holder_.txtName.setText(userMeta.name);
        holder_.txtUsername.setText(userMeta.username);
    }

    // Return the size of your data set (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mUserMetaList.size();
    }
}

package com.wish.wishlist.fragment;

import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import com.wish.wishlist.R;
import com.wish.wishlist.friend.IconTextAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jiawen on 2016-01-11.
 */
public class FriendOptionDialogFragment extends OptionDialogFragment implements
        AdapterView.OnItemClickListener {

    static final String TAG = "FriendListDialog";

    /******************* RemoveFriendListener *********************/
    private RemoveFriendListener mRemoveFriendListener = null;
    public interface RemoveFriendListener {
        void onRemoveFriend();
    }
    protected void onRemoveFriend() {
        if (mRemoveFriendListener != null) {
            mRemoveFriendListener.onRemoveFriend();
        }
    }
    public void setRemoveFriendListener(RemoveFriendListener listener) {
        mRemoveFriendListener = listener;
    }

    @Override
    protected IconTextAdapter getAdapter() {
        List<IconTextAdapter.Entry> list = new ArrayList<>();
        //list.add(new IconTextAdapter.Entry("View profile", R.drawable.ic_action_profile_grey));
        list.add(new IconTextAdapter.Entry("Unfriend", R.drawable.ic_action_remove_friend_grey));
        return new IconTextAdapter(getActivity(), list);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d(TAG, "unfriend");
        onRemoveFriend();

        dismiss();
    }
}

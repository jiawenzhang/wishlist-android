package com.wish.wishlist.fragment;

import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import com.wish.wishlist.R;
import com.wish.wishlist.friend.FindFriendsActivity;
import com.wish.wishlist.friend.IconTextAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jiawen on 2016-01-11.
 */
public class AddFriendOptionDialogFragment extends OptionDialogFragment implements
        AdapterView.OnItemClickListener {

    static final String TAG = "AddFriendListDialog";

    @Override

    protected IconTextAdapter getAdapter() {
        List<IconTextAdapter.Entry> list = new ArrayList<>();
        list.add(new IconTextAdapter.Entry("Find friend", R.drawable.ic_action_search_grey));
        list.add(new IconTextAdapter.Entry("Invite friend", R.drawable.ic_action_add_friend_grey));
        return new IconTextAdapter(getActivity(), list);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position == 0) {
            final Intent findFriendIntent = new Intent(getActivity(), FindFriendsActivity.class);
            startActivity(findFriendIntent);
        } else {
            Log.d(TAG, "onInviteFriendTap");
            FragmentManager manager = getFragmentManager();

            DialogFragment dialog = new InviteFriendFragmentDialog();
            dialog.show(manager, "dialog");
        }

        dismiss();
    }
}

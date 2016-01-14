package com.wish.wishlist.fragment;

import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;

import com.wish.wishlist.R;
import com.wish.wishlist.friend.FindFriendsActivity;
import com.wish.wishlist.friend.IconTextAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jiawen on 2016-01-11.
 */
public class ListDialogFragment extends DialogFragment implements
        AdapterView.OnItemClickListener {

    static final String TAG = "ListDialogFragment";
    ListView mListView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.list_dialog_fragment, null, false);
        mListView = (ListView) view.findViewById(R.id.list);
        mListView.setDivider(null);

        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        List<IconTextAdapter.Entry> list = new ArrayList<>();
        list.add(new IconTextAdapter.Entry("Find friend", R.drawable.ic_action_search_grey));
        list.add(new IconTextAdapter.Entry("Invite friend", R.drawable.ic_action_add_friend_grey));
        IconTextAdapter adapter  = new IconTextAdapter(getActivity(), list);
        mListView.setAdapter(adapter);
        mListView.setOnItemClickListener(this);
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

package com.wish.wishlist.fragment;

import android.os.Bundle;
import android.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;

import com.wish.wishlist.R;
import com.wish.wishlist.friend.IconTextAdapter;

/**
 * Created by jiawen on 2016-01-11.
 */
public abstract class OptionDialogFragment extends DialogFragment implements
        AdapterView.OnItemClickListener {

    static final String TAG = "OptionDialogFragment";
    ListView mListView;

    abstract IconTextAdapter getAdapter();

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
        mListView.setAdapter(getAdapter());
        mListView.setOnItemClickListener(this);
    }
}

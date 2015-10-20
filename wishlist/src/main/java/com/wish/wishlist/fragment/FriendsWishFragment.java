package com.wish.wishlist.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wish.wishlist.R;

/**
 * A placeholder fragment containing a simple view.
 */
public class FriendsWishFragment extends Fragment {

    public FriendsWishFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_friends_wish, container, false);
    }
}

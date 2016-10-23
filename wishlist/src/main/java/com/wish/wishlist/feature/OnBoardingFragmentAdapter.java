package com.wish.wishlist.feature;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.wish.wishlist.R;
import com.wish.wishlist.WishlistApplication;
import com.wish.wishlist.util.Util;

class OnBoardingFragmentAdapter extends FragmentPagerAdapter {
    OnBoardingFragmentAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        return OnBoardingFragment.newInstance(position);
    }

    @Override
    public int getCount() {
        if (Util.deviceAccountEnabled()) {
            return 4;
        }
        return 3;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return "";
    }

}
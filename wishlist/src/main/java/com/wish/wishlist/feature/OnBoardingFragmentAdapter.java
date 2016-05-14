package com.wish.wishlist.feature;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.wish.wishlist.R;
import com.wish.wishlist.WishlistApplication;

class OnBoardingFragmentAdapter extends FragmentPagerAdapter {
    public OnBoardingFragmentAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        return OnBoardingFragment.newInstance(position);
    }

    @Override
    public int getCount() {
        if (WishlistApplication.getAppContext().getResources().getBoolean(R.bool.enable_account)) {
            return 4;
        }
        return 3;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return "";
    }

}
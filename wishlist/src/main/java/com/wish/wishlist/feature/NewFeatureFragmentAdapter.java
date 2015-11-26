package com.wish.wishlist.feature;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

class NewFeatureFragmentAdapter extends FragmentPagerAdapter {
    private int mCount = 2;

    public NewFeatureFragmentAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        return NewFeatureFragment.newInstance(position);
    }

    @Override
    public int getCount() {
        return mCount;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return "";
    }
}
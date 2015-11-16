package com.wish.wishlist.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

import com.parse.ParseObject;
import com.wish.wishlist.R;
import com.wish.wishlist.friend.WishLoader;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.util.Options;
import com.wish.wishlist.util.WishAdapter;

import java.util.ArrayList;
import java.util.List;

public class FriendsWish extends WishBaseActivity implements
        WishLoader.onGotWishesListener,
        WishAdapter.onWishTapListener {

    final static String TAG = "FriendsWish";
    final static String ITEM = "Item";

    private String mFriendId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        _swipeRefreshLayout.setEnabled(false);

        if (_view.val() == Options.View.LIST) {
            mRecyclerView.setLayoutManager(mLinearLayoutManager);
        } else {
            mRecyclerView.setLayoutManager(mStaggeredGridLayoutManager);
        }
        if (_viewFlipper.getDisplayedChild() != WISH_VIEW) {
            _viewFlipper.setDisplayedChild(WISH_VIEW);
        }

        handleIntent(getIntent());
    }

    @Override
    protected void setContentView() {
        setContentView(R.layout.activity_friends_wish);
        setupActionBar(R.id.friends_wish_toolbar);
    }

    @Override
    protected void initializeWishView() {
        Intent i = getIntent();
        mFriendId = i.getStringExtra(Friends.FRIEND_ID);
        WishLoader.getInstance().setGotWishesListener(this);
        WishLoader.getInstance().fetchWishes(mFriendId);
    }

    public void onGotWishes(String friendId, List<ParseObject> wishList) {
        Log.d(TAG, "got " + wishList.size() + " wishes from friendId " + friendId);
        mWishlist = fromParseObjects(wishList);

        // onGotWishes can be call twice, one from cached data and another from network, if we use setAdapter here,
        // the items in the grid layout will be displaced the second time setAdapter is called.
        // Using swapAdapter and passing false as the removeAndRecycleExistingViews flag will avoid this

        updateWishView();
    }

    @Override
    protected void reloadItems(String searchName, java.util.Map where) {
        if (searchName == null) {
            // Get all of the rows from the Item table
            // Keep track of the TextViews added in list lstTable
            if (where.get("complete") != null) {
                int complete = Integer.parseInt((String) where.get("complete"));
                ArrayList<WishItem> filtered_wishList = new ArrayList<>();
                for (WishItem item : mWishlist) {
                    if (item.getComplete() == complete) {
                        filtered_wishList.add(item);
                    }
                }
                //Fixme: sort wish
                mWishlist = filtered_wishList;
            }
        } else {
            //Fixme: search wish
        }

        updateWishView();
        updateDrawerList();
        updateActionBarTitle();
    }

    private List<WishItem> fromParseObjects(final List<ParseObject> parseWishList) {
        List<WishItem> wishList = new ArrayList<>();
        for (final ParseObject object : parseWishList) {
            wishList.add(WishItem.fromParseObject(object, -1));
        }
        return wishList;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_friends_wish, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void onWishTapped(WishItem item) {
        Log.d(TAG, "onWishTapped");
        Intent i = new Intent(this, FriendWishDetail.class);
        i.putExtra(ITEM, item);
        startActivity(i);
    }
}

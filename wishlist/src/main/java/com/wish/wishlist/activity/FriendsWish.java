package com.wish.wishlist.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.parse.ParseObject;
import com.wish.wishlist.R;
import com.wish.wishlist.friend.WishLoader;
import com.wish.wishlist.util.WishAdapterGrid;
import com.wish.wishlist.util.WishAdapterList;

import java.util.List;

public class FriendsWish extends ActivityBase implements
    WishLoader.onGotWishesListener {

    final static String TAG = "FriendsWish";

    protected RecyclerView mRecyclerView;
    protected RecyclerView.LayoutManager mLinearLayoutManager;
    protected StaggeredGridLayoutManager mStaggeredGridLayoutManager;
    private WishAdapterList mWishAdapterList;
    private WishAdapterGrid mWishAdapterGrid;
    private List<ParseObject> mWishlist;
    private String mFriendId;
    private boolean mListView = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends_wish);
        setupActionBar(R.id.friends_wish_toolbar);

        mRecyclerView = (RecyclerView) findViewById(R.id.wish_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        //mRecyclerView.setHasFixedSize(true);

        mLinearLayoutManager = new LinearLayoutManager(this);
        mStaggeredGridLayoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);

        // use a linear layout manager by default
        mRecyclerView.setLayoutManager(mLinearLayoutManager);

        Intent i = getIntent();
        mFriendId = i.getStringExtra(Friends.FRIEND_ID);
        WishLoader.getInstance().setGotWishesListener(this);
        WishLoader.getInstance().fetchWishes(mFriendId);
    }

    public void onGotWishes(String friendId, List<ParseObject> wishList) {
        Log.d(TAG, "got " + wishList.size() + " wishes from friendId " + friendId);
        mWishlist = wishList;
        // onGotWishes can be call twice, one from cached data and another from network, if we use setAdapter here,
        // the items in the grid layout will be displaced the second time setAdapter is called.
        // Using swapAdapter and passing false as the removeAndRecycleExistingViews flag will avoid this
        if (mListView) {
            mWishAdapterList = new WishAdapterList(mWishlist);
            mRecyclerView.swapAdapter(mWishAdapterList, false);
        } else {
            mWishAdapterGrid = new WishAdapterGrid(mWishlist);
            mRecyclerView.swapAdapter(mWishAdapterGrid, false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_friends_wish, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_list) {
            mRecyclerView.setLayoutManager(mLinearLayoutManager);
            if (mWishAdapterList == null) {
                mWishAdapterList = new WishAdapterList(mWishlist);
            }
            mRecyclerView.swapAdapter(mWishAdapterList, false);
            mListView = true;
            return true;
        } else if (id == R.id.menu_grid) {
            mRecyclerView.setLayoutManager(mStaggeredGridLayoutManager);
            if (mWishAdapterGrid == null) {
                mWishAdapterGrid = new WishAdapterGrid(mWishlist);
            }
            mRecyclerView.swapAdapter(mWishAdapterGrid, false);
            mListView = false;
            return true;
        } else if (id == android.R.id.home) {
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
}

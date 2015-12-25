package com.wish.wishlist.friend;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Toast;

import com.parse.ParseFile;
import com.parse.ParseUser;
import com.wish.wishlist.R;
import com.wish.wishlist.activity.DrawerActivity;
import com.wish.wishlist.util.NetworkHelper;

import java.util.ArrayList;
import java.util.List;

public class FriendsBaseActivity extends DrawerActivity {

    final static String TAG = "FriendsBaseActivity";

    protected RecyclerView mRecyclerView;
    protected RecyclerView.LayoutManager mLayoutManager;
    protected ProgressDialog mProgressDialog = null;
    protected SwipeRefreshLayout mSwipeRefreshLayout;

    final static int ITEM_DECORATION_SPACE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRecyclerView = (RecyclerView) findViewById(R.id.user_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        //mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        // only enable swipe down refresh when the first item in recycler view is visible
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                mSwipeRefreshLayout.setEnabled(topRowVerticalPosition() >= 0);
            }
        });

        // the refresh listener. this would be called when the layout is pulled down
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (!NetworkHelper.getInstance().isNetworkAvailable()) {
                    Toast.makeText(FriendsBaseActivity.this, "Check network", Toast.LENGTH_LONG).show();
                    mSwipeRefreshLayout.setRefreshing(false);
                    return;
                }
                mSwipeRefreshLayout.setRefreshing(true);
                Log.d(TAG, "refresh");
                refreshFromNetwork();
                // our swipeRefreshLayout needs to be notified when the data is returned in order for it to stop the animation
            }
        });

        loadView();
    }

    protected void refreshFromNetwork() {}

    private int topRowVerticalPosition() {
        if (mRecyclerView == null || mRecyclerView.getChildCount() == 0) {
            return 0;
        }
        return mRecyclerView.getChildAt(0).getTop() - ITEM_DECORATION_SPACE;
    }

    protected void setContentView() {
        setContentView(R.layout.activity_friends);
        setupActionBar(R.id.friends_toolbar);
    }

    protected void loadView() {}

    protected List<UserAdapter.UserMeta> getUserMetaList(final List<ParseUser> users) {
        ArrayList<UserAdapter.UserMeta> userMetaList = new ArrayList<>();
        for (final ParseUser user : users) {
            final ParseFile parseImage = user.getParseFile("profileImage");
            String imgUrl = null;
            if (parseImage != null) {
                imgUrl = parseImage.getUrl();
            }
            UserAdapter.UserMeta userMeta = new UserAdapter.UserMeta(
                    user.getObjectId(),
                    user.getString("name"),
                    user.getUsername(),
                    imgUrl);
            userMetaList.add(userMeta);
        }
        return userMetaList;
    }

    protected void showProgressDialog(final String text) {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(text);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                Log.d(TAG, text + " canceled");
                Toast.makeText(FriendsBaseActivity.this, "Check network", Toast.LENGTH_LONG).show();
            }
        });
        mProgressDialog.show();
    }

    protected void handleResult(final String friendId,
                                final boolean success,
                                final String successText,
                                UserAdapter adapter) {
        if (success) {
            adapter.remove(friendId);
            Toast.makeText(this, successText, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Check network", Toast.LENGTH_LONG).show();
        }
        mProgressDialog.dismiss();
    }
}

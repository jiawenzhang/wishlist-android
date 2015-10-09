package com.wish.wishlist.activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.parse.ParseFile;
import com.parse.ParseUser;
import com.wish.wishlist.R;
import com.wish.wishlist.friend.FriendManager;
import com.wish.wishlist.util.UserAdapter;

import java.util.ArrayList;
import java.util.List;

public class FriendRequest extends ActivityBase implements
    FriendManager.onFriendRequestListener,
    UserAdapter.acceptFriendListener {

    final static String TAG = "FriendRequest";

    private RecyclerView mRecyclerView;
    private UserAdapter mUserAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_friends);
        setupActionBar(R.id.find_friends_toolbar);

        mRecyclerView = (RecyclerView) findViewById(R.id.user_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        FriendManager m = new FriendManager();
        m.setFriendRequestListener(this);
        m.fetchFriendRequest();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_find_friends, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        else {
            return super.onOptionsItemSelected(item);
        }
    }

    public void onGotFriendRequest(List<ParseUser> friends) {
        Log.d(TAG, "onGotFriendRequest");
        if (friends.isEmpty()) {
            Log.d(TAG, "no friend request");
            return;
        }

        ArrayList<UserAdapter.UserMeta> userMetaList = new ArrayList<>();
        for (final ParseUser user : friends) {
            final ParseFile parseImage = user.getParseFile("profileImage");
            Bitmap bitmap = null;
            if (parseImage != null) {
                try {
                    bitmap = BitmapFactory.decodeByteArray(parseImage.getData(), 0, parseImage.getData().length);
                } catch (com.parse.ParseException e) {
                    Log.e(TAG, e.toString());
                }
            }
            UserAdapter.UserMeta userMeta = new UserAdapter.UserMeta(user.getObjectId(), user.getString("name"), user.getUsername(), bitmap);
            userMetaList.add(userMeta);
        }
        mUserAdapter = new UserAdapter(userMetaList);
        mUserAdapter.setAcceptFriendListener(this);
        mRecyclerView.setAdapter(mUserAdapter);
    }

    @Override
    public void onAcceptFriend(String friendId) {
        Log.d(TAG, "onAddFriend " + friendId);
        FriendManager m = new FriendManager();
        m.acceptFriend(friendId);
    }
}

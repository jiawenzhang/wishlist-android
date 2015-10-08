package com.wish.wishlist.activity;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v7.widget.SearchView;

import com.parse.ParseFile;
import com.parse.ParseUser;
import com.wish.wishlist.R;
import com.wish.wishlist.friend.FriendManager;
import com.wish.wishlist.util.UserAdapter;

import java.util.ArrayList;
import java.util.List;

public class FindFriends extends ActivityBase implements
        FriendManager.onFoundUserListener,
        UserAdapter.addFriendListener {

    final static String TAG = "FindFriends";
    private MenuItem _menuSearch;

    private RecyclerView mRecyclerView;
    private UserAdapter mUserAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_friends);
        setupActionBar(R.id.find_friends_toolbar);

        handleIntent(getIntent());

        mRecyclerView = (RecyclerView) findViewById(R.id.user_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        FriendManager m = new FriendManager();
        m.setListener(this);
        m.fetchFriends();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        // check if the activity is started from search
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            //MenuItemCompat.collapseActionView(_menuSearch);
            // activity is started from search, get the search query and
            // displayed the searched items
            String username = intent.getStringExtra(SearchManager.QUERY);
            FriendManager m = new FriendManager();
            m.setListener(this);
            m.findUser(username);
        } else {
            // activity is not started from search
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_find_friends, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        _menuSearch = menu.findItem(R.id.find_friends_menu_search);
        MenuItemCompat.expandActionView(_menuSearch);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(_menuSearch);
        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
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

    @Override
    public void onFoundUser(ParseUser user) {
        Log.d(TAG, "onFoundUser");
        if (user == null) {
            Log.d(TAG, "no user");
            return;
        }

        ArrayList<UserAdapter.UserMeta> userMetaList = new ArrayList<>();
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
        mUserAdapter = new UserAdapter(userMetaList);
        mUserAdapter.setAddFriendListener(this);
        mRecyclerView.setAdapter(mUserAdapter);
    }

    @Override
    public void onAddFriend(String friendId) {
        Log.d(TAG, "onAddFriend " + friendId);
        FriendManager m = new FriendManager();
        m.requestFriend(friendId);
    }

    @Override
    public void onGotAllFriends(List<ParseUser> friends) {
        Log.d(TAG, "onGotAllFriend");
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
        mUserAdapter.setAddFriendListener(this);
        mRecyclerView.setAdapter(mUserAdapter);
    }
}

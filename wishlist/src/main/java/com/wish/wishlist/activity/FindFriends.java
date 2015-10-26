package com.wish.wishlist.activity;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v7.widget.SearchView;

import com.parse.ParseFile;
import com.parse.ParseUser;
import com.wish.wishlist.R;
import com.wish.wishlist.friend.FriendManager;
import com.wish.wishlist.util.AddFriendAdapter;

import java.util.ArrayList;

public class FindFriends extends FriendsBase implements
        FriendManager.onFoundUserListener,
        AddFriendAdapter.addFriendListener {

    final static String TAG = "FindFriends";
    private MenuItem _menuSearch;
    private AddFriendAdapter mAddFriendAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handleIntent(getIntent());
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
            FriendManager.getInstance().setFoundUserListener(this);
            FriendManager.getInstance().findUser(username);
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
    public void onFoundUser(ParseUser user) {
        Log.d(TAG, "onFoundUser");
        if (user == null) {
            Log.d(TAG, "no user");
            return;
        }

        ArrayList<AddFriendAdapter.UserMeta> userMetaList = new ArrayList<>();
        final ParseFile parseImage = user.getParseFile("profileImage");
        Bitmap bitmap = null;
        if (parseImage != null) {
            try {
                bitmap = BitmapFactory.decodeByteArray(parseImage.getData(), 0, parseImage.getData().length);
            } catch (com.parse.ParseException e) {
                Log.e(TAG, e.toString());
            }
        }
        AddFriendAdapter.UserMeta userMeta = new AddFriendAdapter.UserMeta(user.getObjectId(), user.getString("name"), user.getUsername(), bitmap);
        userMetaList.add(userMeta);
        mAddFriendAdapter = new AddFriendAdapter(userMetaList);
        mAddFriendAdapter.setAddFriendListener(this);
        mRecyclerView.setAdapter(mAddFriendAdapter);
    }

    @Override
    public void onAddFriend(String friendId) {
        Log.d(TAG, "onAddFriend " + friendId);
        FriendManager.getInstance().requestFriend(friendId);
    }

}

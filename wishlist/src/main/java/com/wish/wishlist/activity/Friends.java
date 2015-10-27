package com.wish.wishlist.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.parse.ParseFile;
import com.parse.ParseUser;
import com.wish.wishlist.R;
import com.wish.wishlist.friend.FriendManager;
import com.wish.wishlist.util.FriendAdapter;
import com.wish.wishlist.util.UserAdapter;

import java.util.ArrayList;
import java.util.List;

public class Friends extends FriendsBase implements
        FriendAdapter.FriendTapListener,
        FriendManager.onGotAllFriendsListener {

    public static final String FRIEND_ID = "FRIEND_ID";
    final static String TAG = "Friends";

    protected void loadView() {
        FriendManager.getInstance().setAllFriendsListener(this);
        FriendManager.getInstance().fetchFriends();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_friends, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_add_friends) {
            final Intent findFriendIntent = new Intent(this, FindFriends.class);
            startActivity(findFriendIntent);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onGotAllFriends(List<ParseUser> friends) {
        Log.d(TAG, "onGotAllFriend " + friends.size());
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
        FriendAdapter friendAdapter= new FriendAdapter(userMetaList);
        friendAdapter.setFriendTapListener(this);
        mRecyclerView.swapAdapter(friendAdapter, false);
    }

    public void onFriendTap(String friendId) {
        Log.d(TAG, "friend with objectId: " + friendId + " tapped");
        // show the friend's wishes
        final Intent friendsWishIntent = new Intent(this, FriendsWish.class);
        friendsWishIntent.putExtra(FRIEND_ID, friendId);
        startActivity(friendsWishIntent);
    }
}

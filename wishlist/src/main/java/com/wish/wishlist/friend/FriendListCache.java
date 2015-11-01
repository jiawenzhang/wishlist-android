package com.wish.wishlist.friend;

import android.util.Log;

import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jiawen on 15-11-01.
 */
public class FriendListCache {
    static final String TAG = "FiendListCache";
    List<ParseUser> mFriends = null;

    private static FriendListCache ourInstance = new FriendListCache();

    public static FriendListCache getInstance() {
        return ourInstance;
    }

    private FriendListCache() {}

    public void addFriend(final ParseUser friend) {
        if (mFriends == null) {
            mFriends = new ArrayList<>();
        }
        mFriends.add(friend);
    }

    public void removeFriend(final String friendId) {
        for (int i = 0; i < mFriends.size(); i++) {
            if (mFriends.get(i).getObjectId().equals(friendId)) {
                mFriends.remove(i);
                break;
            }
        }
    }

    public void invalidate() {
        mFriends = null;
    }

    public boolean valid() {
        return (mFriends != null);
    }

    public List<ParseUser> friends() {
        return mFriends;
    }

    public void setFriends(final List<ParseUser> friends) {
        mFriends = friends;
    }

    public void clear() {
        mFriends.clear();
    }
}

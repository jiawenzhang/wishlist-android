package com.wish.wishlist.friend;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * Created by jiawen on 15-11-01.
 */
public class FriendRequestCache extends FriendListCache {
    static final String TAG = "FiendRequestCache";
    private List<FriendRequestMeta> mFriendRequestList = null;

    private static FriendRequestCache ourInstance = new FriendRequestCache();

    public static FriendRequestCache getInstance() {
        return ourInstance;
    }

    private FriendRequestCache() {}

    public void addFriendRequest(final FriendRequestMeta request) {
        if (mFriendRequestList == null) {
            mFriendRequestList = new ArrayList<>();
            mFriendRequestList.add(request);
            Log.d(TAG, "FriendRequestActivity added");
            return;
        }

        // Fixme: iterate the whole list is not efficient
        for (final FriendRequestMeta meta : mFriendRequestList) {
            if (meta.objectId.equals(request.objectId) && meta.fromMe == request.fromMe) {
                Log.d(TAG, "FriendRequestActivity already exists, ignore");
                return;
            }
        }
        mFriendRequestList.add(request);
        Log.d(TAG, "FriendRequestActivity added");
    }

    public void removeFriendRequest(final String friendId) {
        ListIterator<FriendRequestMeta> it = mFriendRequestList.listIterator();
        int count = 0;
        while (it.hasNext()) {
            if (it.next().objectId.equals(friendId)) {
                it.remove();
                if (++count == 2) {
                    break;
                }
            }
        }
    }

    public void invalidate() {
        mFriendRequestList = null;
    }

    public boolean valid() {
        return (mFriendRequestList != null);
    }

    public List<FriendRequestMeta> friendRequestList() {
        return mFriendRequestList;
    }

    public void setFriendRequestList(final List<FriendRequestMeta> friendRequestList) {
        mFriendRequestList = friendRequestList;
    }

    public void clear() {
        mFriendRequestList.clear();
    }
}

package com.wish.wishlist.model;

import com.wish.wishlist.friend.FriendRequestMeta;

import java.util.Comparator;

/**
 * Created by jiawen on 15-11-11.
 */
public class FriendRequestTimeComparator implements Comparator<FriendRequestMeta> {
    @Override
    // Descending, larger updatedTime first
    public int compare(FriendRequestMeta o1, FriendRequestMeta o2) {
        return Long.valueOf(o2.updatedTime).compareTo(Long.valueOf(o1.updatedTime));
    }
}

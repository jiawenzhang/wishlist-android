package com.wish.wishlist.friend;

/**
 * Created by jiawen on 15-11-01.
 */
public class FriendRequestMeta extends UserAdapter.UserMeta {
    public Boolean fromMe;

    public FriendRequestMeta() {}

    public FriendRequestMeta(final String objectId,
                             final String name,
                             final String username,
                             final String imageUrl,
                             boolean fromMe) {
        super(objectId, name, username, imageUrl);
        this.fromMe = fromMe;
    }
}

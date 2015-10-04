package com.wish.wishlist.event;

/**
 * Created by jiawen on 15-10-04.
 */
public class ProfileChangeEvent {
    public enum ProfileChangeType {
        image,
        name,
        email,
        all
    }

    public ProfileChangeEvent(ProfileChangeType t) {
        type = t;
    }

    public ProfileChangeType type;
}

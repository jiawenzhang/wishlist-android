package com.wish.wishlist.friend;

import android.app.Activity;
import android.util.Log;

import com.parse.FindCallback;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.wish.wishlist.db.ItemDBManager;
import com.wish.wishlist.model.WishItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jiawen on 15-10-19.
 */
public class WishLoader {
    /******************* GotWishesListener  *************************/
    onGotWishesListener mGotWishesListener;
    public interface onGotWishesListener {
        void onGotWishes(final String friendId, List<WishItem> wishList);
    }

    protected void onGotWishes(final String friendId, List<WishItem> wishList) {
        if (mGotWishesListener != null) {
            mGotWishesListener.onGotWishes(friendId, wishList);
        }
    }

    public void setGotWishesListener(Activity a) {
        mGotWishesListener = (onGotWishesListener) a;
    }

    private static WishLoader ourInstance = new WishLoader();

    private static String TAG = "WishLoader";
    private String mFriendId;
    private List<WishItem> mWishlist;

    public static WishLoader getInstance() {
        return ourInstance;
    }

    private WishLoader() {
    }

    public void fetchWishes(final String friendId) {
        mFriendId = friendId;
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Item");
        query.whereEqualTo(WishItem.PARSE_KEY_OWNDER_ID, friendId);
        query.whereEqualTo(ItemDBManager.KEY_DELETED, false);
        query.setCachePolicy(ParseQuery.CachePolicy.CACHE_THEN_NETWORK);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> wishList, com.parse.ParseException e) {
                if (e == null) {
                    mWishlist = fromParseObjects(wishList);
                    onGotWishes(friendId, mWishlist);
                } else {
                    Log.e(TAG, e.toString());
                }
            }
        });
    }

    public List<WishItem> getWishes(final String friendId) {
        assert (friendId.equals(mFriendId));
        return mWishlist;
    }

    private List<WishItem> fromParseObjects(final List<ParseObject> parseWishList) {
        List<WishItem> wishList = new ArrayList<>();
        for (final ParseObject object : parseWishList) {
            wishList.add(WishItem.fromParseObject(object, -1));
        }
        return wishList;
    }
}


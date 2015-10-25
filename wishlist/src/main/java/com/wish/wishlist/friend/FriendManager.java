package com.wish.wishlist.friend;

import android.app.Activity;
import android.util.Log;

import com.parse.FindCallback;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by jiawen on 15-10-05.
 */
public class FriendManager {
    final static String TAG = "FriendManager";
    final static String FRIEND_REQUEST = "FriendRequest";

    final static int REQUESTED = 0;
    final static int ACCEPTED = 1;
    final static int REJECTED = 2;

    /******************* FoundUserListener  *************************/
    onFoundUserListener mFoundUserListener;
    public interface onFoundUserListener {
        void onFoundUser(ParseUser user);
    }

    protected void onFoundUser(ParseUser user) {
        if (mFoundUserListener != null) {
            mFoundUserListener.onFoundUser(user);
        }
    }

    public void setFoundUserListener(Activity a) {
        mFoundUserListener = (onFoundUserListener) a;
    }

    /****************** FriendRequestListener ************************/
    onFriendRequestListener mFriendRequestListener;
    public interface onFriendRequestListener {
        void onGotFriendRequest(List<ParseUser> friends);
    }

    protected void onGotFriendRequest(List<ParseUser> friends) {
        if (mFriendRequestListener != null) {
            mFriendRequestListener.onGotFriendRequest(friends);
        }
    }

    public void setFriendRequestListener(Activity a) {
        mFriendRequestListener = (onFriendRequestListener) a;
    }

    /******************* AllFriendsListener **************************/
    onGotAllFriendsListener mGotAllFriendsListener;
    public interface onGotAllFriendsListener {
        void onGotAllFriends(List<ParseUser> friends);
    }

    protected void onGotAllFriends(List<ParseUser> friends) {
        if (mGotAllFriendsListener!= null) {
            mGotAllFriendsListener.onGotAllFriends(friends);
        }
    }

    public void setAllFriendsListener(Activity a) {
        mGotAllFriendsListener = (onGotAllFriendsListener) a;
    }


    public void requestFriend(final String friendId)
    {
        setFriendRequestStatus(ParseUser.getCurrentUser().getObjectId(), friendId, REQUESTED);
    }

    public void acceptFriend(final String friendId)
    {
        setFriendRequestStatus(friendId, ParseUser.getCurrentUser().getObjectId(), ACCEPTED);
    }

    public void rejectFriend(final String friendId)
    {
        setFriendRequestStatus(friendId, ParseUser.getCurrentUser().getObjectId(), REJECTED);
    }

    private void setFriendRequestStatus(final String from, final String to, final int status)
    {
        Log.d(TAG, "set friend request to " + status);
        ParseObject friendRequest =  new ParseObject(FRIEND_REQUEST);
        friendRequest.put("from", from);
        friendRequest.put("to", to);
        friendRequest.put("status", status);

        // on Parse Cloud code beforeSave trigger for FriendRequest, we validate various conditions before save
        // if it does, we ignore the save
        friendRequest.saveEventually();
    }

    public void removeFriend(final String friendId)
    {
    }

    public void fetchFriendRequest()
    {
        ParseQuery<ParseObject> query = ParseQuery.getQuery(FRIEND_REQUEST);
        query.whereEqualTo("to", ParseUser.getCurrentUser().getObjectId());
        query.whereEqualTo("status", REQUESTED);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> friendRequestList, com.parse.ParseException e) {
                if (e == null) {
                    if (friendRequestList.isEmpty()) {
                        Log.d(TAG, "no friend request to me");
                        onGotFriendRequest(new ArrayList<ParseUser>());
                        return;
                    }

                    HashSet<String> friendIds = new HashSet<>();
                    for (final ParseObject friendRequest : friendRequestList) {
                        friendIds.add(friendRequest.getString("from"));
                    }

                    ParseQuery<ParseUser> query = ParseUser.getQuery();
                    query.whereContainedIn("objectId", friendIds);
                    query.findInBackground(new FindCallback<ParseUser>() {
                        public void done(List<ParseUser> users, com.parse.ParseException e) {
                            if (e == null) {
                                onGotFriendRequest(users);
                            } else {
                                Log.e(TAG, e.toString());
                            }
                        }
                    });
                } else {
                    Log.e(TAG, e.toString());
                }
            }
        });
    }

    public void pendingFriends()
    {
        ParseQuery<ParseObject> query = ParseQuery.getQuery(FRIEND_REQUEST);
        query.whereEqualTo("from", ParseUser.getCurrentUser().getObjectId());
        query.whereEqualTo("status", REQUESTED);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> friendRequestList, com.parse.ParseException e) {
                if (e == null) {
                    if (friendRequestList.isEmpty()) {
                        Log.d(TAG, "no pending friends");
                        return;
                    }

                    for (final ParseObject friendRequest : friendRequestList) {
                        Log.d(TAG, "pending friend " + friendRequest.getString("to"));
                    }
                } else {
                    Log.e(TAG, e.toString());
                }
            }
        });
    }

    public void findUser(final String username)
    {
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo("username", username); // this is email user name, not display name
        query.findInBackground(new FindCallback<ParseUser>() {
            public void done(List<ParseUser> users, com.parse.ParseException e) {
                if (e == null) {
                    if (users.isEmpty()) {
                        Log.d(TAG, "no user found for username: " + username);
                        onFoundUser(null);
                        return;
                    }

                    for (final ParseUser user : users) {
                        onFoundUser(user);
                        Log.d(TAG, "find user: username " + user.getUsername() + " email: " + user.getEmail() + " name: " + user.getString("name"));
                    }
                } else {
                    Log.e(TAG, e.toString());
                    onFoundUser(null);
                }
            }
        });
    }

    public void fetchFriends()
    {
        ParseQuery<ParseObject> queryToMe = ParseQuery.getQuery(FRIEND_REQUEST);
        queryToMe.whereEqualTo("to", ParseUser.getCurrentUser().getObjectId());
        queryToMe.whereEqualTo("status", ACCEPTED);

        ParseQuery<ParseObject> queryFromMe = ParseQuery.getQuery(FRIEND_REQUEST);
        queryFromMe.whereEqualTo("from", ParseUser.getCurrentUser().getObjectId());
        queryFromMe.whereEqualTo("status", ACCEPTED);

        List<ParseQuery<ParseObject>> queries = new ArrayList<>();
        queries.add(queryToMe);
        queries.add(queryFromMe);

        ParseQuery<ParseObject> mainQuery = ParseQuery.or(queries);
        mainQuery.setCachePolicy(ParseQuery.CachePolicy.CACHE_THEN_NETWORK);
        mainQuery.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> results, com.parse.ParseException e) {
                if (e == null) {
                    HashSet<String> friendIds = new HashSet<>();
                    final String selfId = ParseUser.getCurrentUser().getObjectId();
                    for (final ParseObject friendRequest : results) {
                        if (!friendRequest.getString("from").equals(selfId)) {
                            friendIds.add(friendRequest.getString("from"));
                        }
                        if (!friendRequest.getString("to").equals(selfId)) {
                            friendIds.add(friendRequest.getString("to"));
                        }
                    }

                    if (friendIds.isEmpty()) {
                        Log.d(TAG, "no friends");
                        return;
                    }

                    for (final String friendId : friendIds) {
                        Log.d(TAG, "friend id: " + friendId);
                    }

                    ParseQuery<ParseUser> query = ParseUser.getQuery();
                    query.setCachePolicy(ParseQuery.CachePolicy.CACHE_THEN_NETWORK);
                    query.whereContainedIn("objectId", friendIds);
                    query.findInBackground(new FindCallback<ParseUser>() {
                        public void done(List<ParseUser> users, com.parse.ParseException e) {
                            if (e == null) {
                                onGotAllFriends(users);
                            } else {
                                Log.e(TAG, e.toString());
                            }
                        }
                    });
                } else {
                    Log.e(TAG, e.toString());
                }
            }
        });
    }
}


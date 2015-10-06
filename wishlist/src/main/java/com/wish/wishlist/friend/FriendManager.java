package com.wish.wishlist.friend;

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
    final static int ACCEPTED = 0;
    final static int REJECTED = 0;

    public void requestFriend(final String friendId)
    {
        ParseObject friendRequest =  new ParseObject(FRIEND_REQUEST);
        friendRequest.put("from", ParseUser.getCurrentUser().getObjectId());
        friendRequest.put("to", friendId);
        friendRequest.put("status", REQUESTED);
        friendRequest.saveEventually();
    }

    public void acceptFriend(final String friendId)
    {
        setFriendRequestStatus(friendId, ACCEPTED);
    }

    public void rejectFriend(final String friendId)
    {
        setFriendRequestStatus(friendId, REJECTED);
    }

    private void setFriendRequestStatus(final String friendId, final int status)
    {
        ParseQuery<ParseObject> query = ParseQuery.getQuery(FRIEND_REQUEST);
        query.whereEqualTo("from", friendId);
        query.whereEqualTo("to", ParseUser.getCurrentUser().getObjectId());
        query.whereEqualTo("status", REQUESTED);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> friendRequestList, com.parse.ParseException e) {
                if (e == null) {
                    if (friendRequestList.size() != 1) {
                        Log.e(TAG, "find " + friendRequestList.size() + " friendRequest from " + friendId + " to me");
                        return;
                    }
                    ParseObject friendRequest = friendRequestList.get(0);
                    friendRequest.put("status", status);
                    friendRequest.saveEventually();
                    Log.d(TAG, "set friend request from " + friendId + " to " + status);
                } else {
                    Log.e(TAG, e.toString());
                }
            }
        });
    }

    public void removeFriend(final String friendId)
    {
    }

    public void friendRequest()
    {
        ParseQuery<ParseObject> query = ParseQuery.getQuery(FRIEND_REQUEST);
        query.whereEqualTo("to", ParseUser.getCurrentUser().getObjectId());
        query.whereEqualTo("status", REQUESTED);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> friendRequestList, com.parse.ParseException e) {
                if (e == null) {
                    if (friendRequestList.isEmpty()) {
                        Log.d(TAG, "no friend request to me");
                        return;
                    }

                    for (final ParseObject friendRequest : friendRequestList) {
                        Log.d(TAG, "friend request from " + friendRequest.getString("from"));
                    }
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
                        return;
                    }

                    for (final ParseUser user : users) {
                        Log.d(TAG, "find user: username " + user.getUsername() + " email: " + user.getEmail() + " name: " + user.getString("name"));
                    }
                } else {
                    Log.e(TAG, e.toString());
                }
            }
        });
    }

    public void friends()
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
                } else {
                    Log.e(TAG, e.toString());
                }
            }
        });
    }
}


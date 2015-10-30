package com.wish.wishlist.friend;

import android.app.Activity;
import android.util.Log;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

/**
 * Created by jiawen on 15-10-05.
 */
public class FriendManager {
    private static FriendManager instance = null;
    public static FriendManager getInstance() {
        if (instance == null) {
            instance = new FriendManager();
        }
        return instance;
    }

    private FriendManager() {}

    final static String TAG = "FriendManager";
    final static String FRIEND_REQUEST = "FriendRequest";

    final static int REQUESTED = 0;
    final static int ACCEPTED = 1;
    final static int REJECTED = 2;

    // one friendId can have one or two FriendRequest objects linked to it - one I send to my friend, or/and one my friend sent to me
    private Hashtable<String /* friendId */, Set<String> /* FriendRequest object_id */> mFriendRequestTable = new Hashtable<>();

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

    /******************* RequestFriendListener **************************/
    onRequestFriendListener mRequestFriendListener;
    public interface onRequestFriendListener {
        void onRequestFriendResult(final String friendId, final boolean success);
    }

    protected void onRequestFriendResult(final String friendId, final boolean success) {
        if (mRequestFriendListener!= null) {
            mRequestFriendListener.onRequestFriendResult(friendId, success);
        }
    }

    public void setRequestFriendListener(Activity a) {
        mRequestFriendListener = (onRequestFriendListener) a;
    }

    /******************* AcceptFriendListener **************************/

    /******************* RejectFriendListener **************************/

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
        friendRequest.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Log.d(TAG, "save FriendRequest success");
                    onSetFriendRequestStatusResult(from, to, status, true);
                } else {
                    Log.e(TAG, "save FriendRequest failed " + e.toString());
                    onSetFriendRequestStatusResult(from, to, status, false);
                }
            }
        });
    }

    private void onSetFriendRequestStatusResult(final String from, final String to, final int status, final boolean success)
    {
        switch (status) {
            case REQUESTED: {
                onRequestFriendResult(to, success);
            }
            case ACCEPTED: {
            }
            case REJECTED: {
            }
        }
    }

    public void removeFriend(final String friendId)
    {
        final Set<String> friendRequestIds = mFriendRequestTable.get(friendId);
        if (friendRequestIds != null) {
            for (final String id : friendRequestIds) {
                Log.d(TAG, "removing friend - friendId: " + friendId + " requestId: " + id);
                ParseObject.createWithoutData(FRIEND_REQUEST, id).deleteEventually();
            }
        }
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
        // this is email user name, not display name. email username is unique while display name is not
        query.whereEqualTo("username", username);
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

    private void gotFriendRequestResults(final List<ParseObject> results, final ParseQuery.CachePolicy cachePolicy) {
        mFriendRequestTable.clear();
        HashSet<String> friendIds = new HashSet<>();
        final String selfId = ParseUser.getCurrentUser().getObjectId();
        for (final ParseObject friendRequest : results) {
            if (!friendRequest.getString("from").equals(selfId)) {
                final String friendId = friendRequest.getString("from");
                friendIds.add(friendId);
                if (mFriendRequestTable.containsKey(friendId)) {
                    mFriendRequestTable.get(friendId).add(friendRequest.getObjectId());
                } else {
                    mFriendRequestTable.put(friendId, new HashSet<String>() {{
                        add(friendRequest.getObjectId());
                    }});
                }
            }
            if (!friendRequest.getString("to").equals(selfId)) {
                final String friendId = friendRequest.getString("to");
                friendIds.add(friendId);
                if (mFriendRequestTable.containsKey(friendId)) {
                    mFriendRequestTable.get(friendId).add(friendRequest.getObjectId());
                } else {
                    mFriendRequestTable.put(friendId, new HashSet<String>() {{
                        add(friendRequest.getObjectId());
                    }} );
                }
            }
        }

        if (friendIds.isEmpty()) {
            Log.d(TAG, "no friends");
            onGotAllFriends(new ArrayList<ParseUser>());
            if (cachePolicy == ParseQuery.CachePolicy.CACHE_ONLY) {
                fetchFriends(ParseQuery.CachePolicy.NETWORK_ONLY);
            }
            return;
        }

        for (final String friendId : friendIds) {
            Log.d(TAG, "friend id: " + friendId);
        }

        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.setCachePolicy(cachePolicy);
        query.whereContainedIn("objectId", friendIds);
        query.findInBackground(new FindCallback<ParseUser>() {
            public void done(List<ParseUser> users, com.parse.ParseException e) {
                if (e == null) {
                    onGotAllFriends(users);
                    if (cachePolicy == ParseQuery.CachePolicy.CACHE_ONLY) {
                        fetchFriends(ParseQuery.CachePolicy.NETWORK_ONLY);
                    }
                } else {
                    Log.e(TAG, e.toString());
                }
            }
        });
    }

    private void fetchFriends(final ParseQuery.CachePolicy cachePolicy) {
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
        mainQuery.setCachePolicy(cachePolicy);
        mainQuery.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> results, com.parse.ParseException e) {
                if (e == null) {
                    gotFriendRequestResults(results, cachePolicy);
                } else {
                    Log.e(TAG, e.toString());
                    if (e.getCode() == com.parse.ParseException.CACHE_MISS) {
                        // we don't have cache yet, pass an empty list
                        gotFriendRequestResults(new ArrayList<ParseObject>(), cachePolicy);
                    }
                }
            }
        });
    }

    public void fetchFriends() {
        // we fetch friends from cache first, then from network
        fetchFriends(ParseQuery.CachePolicy.CACHE_ONLY);
    }
}


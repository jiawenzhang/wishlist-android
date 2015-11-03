package com.wish.wishlist.friend;

import android.app.Activity;
import android.util.Log;

import com.parse.FindCallback;
import com.parse.FunctionCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

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

    /******************* Parse Cloud Functions *************************/
    final static String REMOVE_FRIEND = "removeFriend";
    final static String SET_FRIEND_REQUEST_STATUS = "setFriendRequestStatus";

    /******************* FriendRequest status *************************/
    final static int REQUESTED = 0;
    final static int ACCEPTED = 1;
    final static int REJECTED = 2;


    /******************* FoundUserListener  *************************/
    onFoundUserListener mFoundUserListener;
    public interface onFoundUserListener {
        void onFoundUser(final List<ParseUser> users, final boolean success);
    }

    protected void onFoundUser(final List<ParseUser> users, final boolean success) {
        if (mFoundUserListener != null) {
            mFoundUserListener.onFoundUser(users, success);
        }
    }

    public void setFoundUserListener(final Activity a) {
        mFoundUserListener = (onFoundUserListener) a;
    }



    /****************** FriendRequestListener ************************/
    onFriendRequestListener mFriendRequestListener;
    public interface onFriendRequestListener {
        void onGotFriendRequest();
    }

    protected void onGotFriendRequest() {
        if (mFriendRequestListener != null) {
            mFriendRequestListener.onGotFriendRequest();
        }
    }

    public void setFriendRequestListener(final Activity a) {
        mFriendRequestListener = (onFriendRequestListener) a;
    }



    /******************* AllFriendsListener **************************/
    onGotAllFriendsListener mGotAllFriendsListener;
    public interface onGotAllFriendsListener {
        void onGotAllFriends(final List<ParseUser> friends);
    }

    protected void onGotAllFriends(final List<ParseUser> friends) {
        if (mGotAllFriendsListener!= null) {
            mGotAllFriendsListener.onGotAllFriends(friends);
        }
    }

    public void setAllFriendsListener(final Activity a) {
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

    public void setRequestFriendListener(final Activity a) {
        mRequestFriendListener = (onRequestFriendListener) a;
    }



    /******************* AcceptFriendListener **************************/
    onAcceptFriendListener mAcceptFriendListener;
    public interface onAcceptFriendListener {
        void onAcceptFriendResult(final String friendId, final boolean success);
    }

    protected void onAcceptFriendResult(final String friendId, final boolean success) {
        if (mAcceptFriendListener!= null) {
            mAcceptFriendListener.onAcceptFriendResult(friendId, success);
        }
    }

    public void setAcceptFriendListener(final Activity a) {
        mAcceptFriendListener = (onAcceptFriendListener) a;
    }



    /******************* RejectFriendListener **************************/
    onRejectFriendListener mRejectFriendListener;
    public interface onRejectFriendListener {
        void onRejectFriendResult(final String friendId, final boolean success);
    }

    protected void onRejectFriendResult(final String friendId, final boolean success) {
        if (mRejectFriendListener!= null) {
            mRejectFriendListener.onRejectFriendResult(friendId, success);
        }
    }

    public void setRejectFriendListener(final Activity a) {
        mRejectFriendListener = (onRejectFriendListener) a;
    }


    /******************* RemoveFriendResultListener **************************/
    onRemoveFriendResultListener mRemoveFriendResultListener;
    public interface onRemoveFriendResultListener {
        void onRemoveFriendResult(final String friendId, final boolean success);
    }

    protected void onRemoveFriendResult(final String friendId, final boolean success) {
        if (mRemoveFriendResultListener != null) {
            mRemoveFriendResultListener.onRemoveFriendResult(friendId, success);
        }
    }

    public void setRemoveFriendResultListener(final Activity a) {
        mRemoveFriendResultListener = (onRemoveFriendResultListener) a;
    }


    public void setFriendRequestStatus(final String from, final String to, final int status) {
        Map<String, Object> params = new HashMap<>();
        params.put("from", from);
        params.put("to", to);
        params.put("status", status);

        ParseCloud.callFunctionInBackground(SET_FRIEND_REQUEST_STATUS, params, new FunctionCallback<Map<String, Object>>() {
            public void done(Map<String, Object> mapObject, ParseException e) {
                if (e == null) {
                    Log.d(TAG, "Save FriendRequest success");
                    onSetFriendRequestStatusResult(from, to, status, true);
                } else {
                    Log.e(TAG, "Save FriendRequest failed " + e.toString());
                    onSetFriendRequestStatusResult(from, to, status, false);
                }
            }
        });
    }

    public void requestFriend(final String friendId)
    {
        Log.d(TAG, "Request friend");
        setFriendRequestStatus(ParseUser.getCurrentUser().getObjectId(), friendId, REQUESTED);
    }

    public void acceptFriend(final String friendId)
    {
        Log.d(TAG, "Accept friend");
        setFriendRequestStatus(friendId, ParseUser.getCurrentUser().getObjectId(), ACCEPTED);
    }

    public void rejectFriend(final String friendId)
    {
        Log.d(TAG, "Reject friend");
        setFriendRequestStatus(friendId, ParseUser.getCurrentUser().getObjectId(), REJECTED);
    }

    private void onSetFriendRequestStatusResult(final String from, final String to, final int status, final boolean success)
    {
        switch (status) {
            case REQUESTED:
                if (success) {
                    //Add the friend request to cache
                    ParseQuery<ParseUser> query = ParseUser.getQuery();
                    query.whereEqualTo("objectId", to);
                    query.setLimit(1);
                    query.findInBackground(new FindCallback<ParseUser>() {
                        public void done(List<ParseUser> users, com.parse.ParseException e) {
                            if (e == null) {
                                Log.d(TAG, "Found parse user");
                                final ParseUser friend = users.get(0);
                                FriendRequestMeta meta = new FriendRequestMeta();
                                meta.objectId = friend.getObjectId();
                                meta.name = friend.getString("name");
                                meta.username = friend.getUsername();
                                String imgUrl = null;
                                final ParseFile parseImage = friend.getParseFile("profileImage");
                                if (parseImage != null) {
                                    imgUrl = parseImage.getUrl();
                                }
                                meta.imageUrl = imgUrl;
                                meta.fromMe = true;
                                FriendRequestCache.getInstance().addFriendRequest(meta);
                            } else {
                                Log.e(TAG, e.toString());
                            }
                        }
                    });
                }
                onRequestFriendResult(to, success);
                break;

            case ACCEPTED:
                // Add the friend to cache
                ParseQuery<ParseUser> query = ParseUser.getQuery();
                query.whereEqualTo("objectId", from);
                query.setLimit(1);
                query.findInBackground(new FindCallback<ParseUser>() {
                    public void done(List<ParseUser> users, com.parse.ParseException e) {
                        if (e == null) {
                            Log.d(TAG, "Found parse user");
                            FriendListCache.getInstance().addFriend(users.get(0));
                        } else {
                            Log.e(TAG, e.toString());
                        }
                    }
                });
                // FriendRequest will be removed from cache in FriendRequestAdapter remove(friendId)
                onAcceptFriendResult(from, success);
                break;

            case REJECTED:
                // FriendRequest will be removed from cache in FriendRequestAdapter remove(friendId)
                onRejectFriendResult(from, success);
                break;
        }
    }

    public void removeFriend(final String friendId)
    {
        Map<String, Object> params = new HashMap<>();
        params.put("friendId", friendId);
        ParseCloud.callFunctionInBackground(REMOVE_FRIEND, params, new FunctionCallback<Map<String, Object>>() {
            public void done(Map<String, Object> mapObject, ParseException e) {
                if (e == null) {
                    Log.d(TAG, "Remove friend success");

                    // Remove the friend in cache
                    FriendListCache.getInstance().removeFriend(friendId);
                    onRemoveFriendResult(friendId, true);
                } else {
                    Log.e(TAG, "Remove friend failed " + e.toString());
                    onRemoveFriendResult(friendId, false);
                }
            }
        });
    }

    public void fetchFriendRequest()
    {
        // fetch form cache first, then from network
        if (FriendRequestCache.getInstance().valid()) {
            Log.d(TAG, "Got cached FriendRequest");
            onGotFriendRequest();
        }

        ParseQuery<ParseObject> queryFromMe = ParseQuery.getQuery(FRIEND_REQUEST);
        queryFromMe.whereEqualTo("from", ParseUser.getCurrentUser().getObjectId());
        queryFromMe.whereEqualTo("status", REQUESTED);

        ParseQuery<ParseObject> queryToMe = ParseQuery.getQuery(FRIEND_REQUEST);
        queryToMe.whereEqualTo("to", ParseUser.getCurrentUser().getObjectId());
        queryToMe.whereEqualTo("status", REQUESTED);

        List<ParseQuery<ParseObject>> queries = new ArrayList<>();
        queries.add(queryToMe);
        queries.add(queryFromMe);

        ParseQuery<ParseObject> mainQuery = ParseQuery.or(queries);

        mainQuery.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> friendRequestList, com.parse.ParseException e) {
                if (e == null) {
                    if (friendRequestList.isEmpty()) {
                        Log.d(TAG, "No friend request");
                        FriendRequestCache.getInstance().setFriendRequestList(new ArrayList<FriendRequestMeta>());
                        onGotFriendRequest();
                        return;
                    }
                    Log.d(TAG, "Got " + friendRequestList.size() + " FriendRequest");

                    // a friendId can be linked to two FriendRequest, one to the friend and one from the friend.
                    final HashMap<String /*friendId*/, HashSet<Boolean>> friendRequestMeta = new HashMap<>();
                    String friendId;
                    for (final ParseObject friendRequest : friendRequestList) {
                        final boolean fromMe;
                        if (friendRequest.getString("from").equals(ParseUser.getCurrentUser().getObjectId())) {
                            // request from me
                            friendId = friendRequest.getString("to");
                            fromMe = true;
                        } else {
                            // request to me
                            friendId = friendRequest.getString("from");
                            fromMe = false;
                        }
                        if (friendRequestMeta.get(friendId) == null) {
                            friendRequestMeta.put(friendId, new HashSet<Boolean>() {{ add(fromMe); }} );
                        } else {
                            friendRequestMeta.get(friendId).add(fromMe);
                        }
                    }

                    ParseQuery<ParseUser> query = ParseUser.getQuery();
                    query.whereContainedIn("objectId", friendRequestMeta.keySet());
                    query.findInBackground(new FindCallback<ParseUser>() {
                        public void done(List<ParseUser> users, com.parse.ParseException e) {
                            if (e == null) {
                                List<FriendRequestMeta> metaList = new ArrayList<>();
                                for (final ParseUser user : users) {
                                    for (boolean fromMe: friendRequestMeta.get(user.getObjectId())) {
                                        FriendRequestMeta meta = new FriendRequestMeta();
                                        meta.objectId = user.getObjectId();
                                        meta.name = user.getString("name");
                                        meta.username = user.getUsername();
                                        String imgUrl = null;
                                        final ParseFile parseImage = user.getParseFile("profileImage");
                                        if (parseImage != null) {
                                            imgUrl = parseImage.getUrl();
                                        }
                                        meta.imageUrl = imgUrl;
                                        meta.fromMe = fromMe;
                                        metaList.add(meta);
                                    }
                                }
                                FriendRequestCache.getInstance().setFriendRequestList(metaList);
                                onGotFriendRequest();
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

    public void findUser(final String username)
    {
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        // this is email user name, not display name. email username is unique while display name is not
        query.whereEqualTo("username", username);
        query.findInBackground(new FindCallback<ParseUser>() {
            public void done(List<ParseUser> users, com.parse.ParseException e) {
                if (e == null) {
                    onFoundUser(users, true);
                } else {
                    Log.e(TAG, e.toString());
                    onFoundUser(new ArrayList<ParseUser>(), false);
                }
            }
        });
    }

    private void gotFriendRequestResults(final List<ParseObject> results, final ParseQuery.CachePolicy cachePolicy) {
        HashSet<String> friendIds = new HashSet<>();
        final String selfId = ParseUser.getCurrentUser().getObjectId();
        for (final ParseObject friendRequest : results) {
            if (!friendRequest.getString("from").equals(selfId)) {
                final String friendId = friendRequest.getString("from");
                friendIds.add(friendId);
            }
            if (!friendRequest.getString("to").equals(selfId)) {
                final String friendId = friendRequest.getString("to");
                friendIds.add(friendId);
            }
        }

        if (friendIds.isEmpty()) {
            Log.d(TAG, "no friends");
            FriendListCache.getInstance().setFriends(new ArrayList<ParseUser>());
            onGotAllFriends(FriendListCache.getInstance().friends());
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
                    // Populate the in-memory FriendListCache with results from Parse cloud
                    FriendListCache.getInstance().setFriends(users);
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
        // we fetch friends from in-memory cache first, then from network
        // if we have network, onGotAllFriends will be called twice
        // first returns the cached result, second returns the result from network
        if (FriendListCache.getInstance().valid()) {
            Log.d(TAG, "Got friends from cache");
            onGotAllFriends(FriendListCache.getInstance().friends());
        }

        fetchFriends(ParseQuery.CachePolicy.NETWORK_ONLY);
    }
}


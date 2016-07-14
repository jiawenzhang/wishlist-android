/**
 * Created by jiawen on 2016-04-23.
 */

package com.wish.wishlist.test;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.InstrumentationTestCase;
import android.test.RenamingDelegatingContext;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import com.parse.LogInCallback;
import com.parse.LogOutCallback;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.wish.wishlist.R;
import com.wish.wishlist.friend.FriendManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class FriendRequestTest extends InstrumentationTestCase implements
        FriendManager.onRequestFriendListener,
        FriendManager.onFoundUserListener {

    // pre-request, 5 users are already sign up
    // Test 5 users send friend request to one user

    private static final String TAG = "FriendTest";
    Context mMockContext;
    static public CountDownLatch mSignal = null;
    static public int mUserCount = 5;
    private ArrayList<ParseUser> mUsers = new ArrayList<>();
    private static final String friendUserEmail = "li.kevin@mail.com";
    private ParseUser mCurrentUser;

    @Before
    public void setUp() {
        mMockContext = new RenamingDelegatingContext(InstrumentationRegistry.getInstrumentation().getTargetContext(), "test_");

        for (int i = 0; i < mUserCount; i++) {
            ParseUser user = new ParseUser();
            String email = "user_" + i + "@test.com";
            user.setUsername(email);
            user.setPassword("123456");
            user.put("name", "name_" + i);
            user.setEmail(email);
            mUsers.add(user);
        }
    }

    @Test
    public void runTest() throws InterruptedException {
        for (ParseUser user : mUsers) {
            login(user);
            sendFriendRequest();
            logout();
        }
    }

    private void login(ParseUser user) throws InterruptedException {
        mSignal = new CountDownLatch(1);

        ParseUser.logInInBackground(user.getUsername(), "123456", new LogInCallback() {
            @Override
            public void done(ParseUser user, ParseException e) {
                if (user != null) {
                    Log.d(TAG, "log in success");
                    if (user.getBoolean("emailVerified")) {
                        //loginSuccess();
                    } else {
                        final String message = "Please verify your email before sign in";
                        //loginVerifyEmail(message);
                        //loginSuccess();
                    }
                    mCurrentUser = user;
                } else {
                    Log.d(TAG, "log in failed");
                    if (e != null) {
                        Log.d(TAG, mMockContext.getString(R.string.com_parse_ui_login_warning_parse_login_failed) + e.toString());
                        if (e.getCode() == ParseException.OBJECT_NOT_FOUND) {
                            Log.d(TAG, mMockContext.getString(R.string.com_parse_ui_parse_login_invalid_credentials_toast));
                        } else {
                            Log.d(TAG, mMockContext.getString(R.string.com_parse_ui_parse_login_failed_unknown_toast));
                        }
                    }
                    assertTrue(false);
                }
                mSignal.countDown();
            }
        });

        waitSignal(mSignal);
    }

    private void sendFriendRequest() {
        mSignal = new CountDownLatch(1);
        FriendManager.getInstance().setFoundUserListener(this);
        FriendManager.getInstance().fetchUsers(friendUserEmail, 0);
    }


    @Override
    public void onFoundUser(final List<ParseUser> users, final boolean success) {
        Log.d(TAG, "onFoundUsers");

        if (!success) {
            assertTrue(false);
            return;
        }

        Log.d(TAG, "Found " + users.size() + " users");
        if (users.size() == 0) {
            assertTrue(false);
            return;
        }

        FriendManager.getInstance().setRequestFriendListener(this);

        int REQUESTED = 0;
        FriendManager.getInstance().setFriendRequestStatus(mCurrentUser.getObjectId(), users.get(0).getObjectId(), REQUESTED);
    }

    @Override
    public void onRequestFriendResult(final String friendId, final boolean success) {
        Log.d(TAG, "onRequestFriendResult " + friendId + " success? " + success);
        assertTrue(success);
    }

    private void waitSignal(CountDownLatch signal) {
        try {
            signal.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void logout() {
        mSignal = new CountDownLatch(1);

        ParseUser.logOutInBackground(new LogOutCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    // logout successful
                    Log.d(TAG, "success to logout");
                } else {
                    Log.d(TAG, "Fail to logout");
                    assertTrue(false);
                }
                mSignal.countDown();
            }
        });

        waitSignal(mSignal);
    }
}

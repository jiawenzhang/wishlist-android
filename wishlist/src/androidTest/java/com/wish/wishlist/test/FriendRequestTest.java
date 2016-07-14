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

import com.parse.ParseUser;
import com.wish.wishlist.friend.FriendManager;
import com.wish.wishlist.test.util.UserManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

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
    private static final String friendUserEmail = "li.kevin@mail.com";

    @Before
    public void setUp() {
        mMockContext = new RenamingDelegatingContext(InstrumentationRegistry.getInstrumentation().getTargetContext(), "test_");
        UserManager.setupUserList();
    }

    @Test
    public void runTest() throws InterruptedException {
        for (ParseUser user : UserManager.userList) {
            login(user);
            sendFriendRequest();
            logout();
        }
    }

    private void login(ParseUser user) throws InterruptedException {
        mSignal = new CountDownLatch(1);
        UserManager.login(user, mSignal, mMockContext);
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
        FriendManager.getInstance().setFriendRequestStatus(UserManager.currentUser.getObjectId(), users.get(0).getObjectId(), REQUESTED);
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
        UserManager.logout(mSignal);
        waitSignal(mSignal);
    }
}

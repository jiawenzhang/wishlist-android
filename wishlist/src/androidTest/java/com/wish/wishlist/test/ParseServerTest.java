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

import com.parse.FunctionCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SignUpCallback;
import com.wish.wishlist.R;
import com.wish.wishlist.db.TagItemDBManager;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.sync.SyncAgent;
import com.wish.wishlist.test.util.UserManager;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ParseServerTest extends InstrumentationTestCase implements SyncAgent.OnSyncDoneListener {

    private static final String TAG = "ParseServerTest";
    Context mMockContext;
    private static CountDownLatch mSignal = null;
    private static CountDownLatch mSyncDoneSignal = null;
    private static int mWishCount = 20;

    @Before
    public void setUp() {
        mMockContext = new RenamingDelegatingContext(InstrumentationRegistry.getInstrumentation().getTargetContext(), "test_");
        UserManager.setupUserList();
    }

    @Test
    public void runTest() throws InterruptedException {
        signup();

        SyncAgent.getInstance().registerSyncDoneListener(this);

        for (ParseUser user : UserManager.userList) {
            login(user);
            createWishes();

            waitSignal(mSyncDoneSignal);

            logout();
        }

        deleteUsers();
    }

    @Override
    public void onSyncDone(boolean success) {
        Log.e(TAG, "onSyncDone, success: " + success);
        assertTrue(success);
        mSyncDoneSignal.countDown();
    }

    private void signup() {
        mSignal = new CountDownLatch(UserManager.userCount);

        // Create users and sign up
        for (final ParseUser user : UserManager.userList) {
            user.signUpInBackground(new SignUpCallback() {
                @Override
                public void done(ParseException e) {
                    if (e == null) {
                        if (user.getBoolean("emailVerified")) {
                            Log.d(TAG, "sign up success");
                            //signupSuccess();
                        } else {
                            String message = "Please verify your email";
                            Log.d(TAG, message);
                            //verifyEmail(message);
                        }
                    } else {
                        Log.d(TAG, mMockContext.getString(R.string.com_parse_ui_login_warning_parse_signup_failed) + e.toString());
                        switch (e.getCode()) {
                            case ParseException.INVALID_EMAIL_ADDRESS:
                                Log.d(TAG, mMockContext.getString(R.string.com_parse_ui_invalid_email_toast));
                                break;
                            case ParseException.USERNAME_TAKEN:
                                Log.d(TAG, user.getUsername() + " " + mMockContext.getString(R.string.com_parse_ui_username_taken_toast));
                                break;
                            case ParseException.EMAIL_TAKEN:
                                Log.d(TAG, user.getEmail() + " " + mMockContext.getString(R.string.com_parse_ui_email_taken_toast));
                                break;
                            default:
                                Log.d(TAG, mMockContext.getString(R.string.com_parse_ui_signup_failed_unknown_toast));
                        }
                        assertTrue(false);
                    }
                    ParseServerTest.mSignal.countDown();
                }
            });
        }

        waitSignal(mSignal);
    }

    private void waitSignal(CountDownLatch signal) {
        try {
            signal.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void login(ParseUser user) throws InterruptedException {
        mSignal = new CountDownLatch(1);
        UserManager.login(user, mSignal, mMockContext);
        waitSignal(mSignal);
    }

    private void logout() {
        mSignal = new CountDownLatch(1);

        UserManager.logout(mSignal);
        waitSignal(mSignal);
    }

    private void createWishes() {
        mSyncDoneSignal = new CountDownLatch(1);
        for (int i = 0; i < mWishCount; i++) {
            WishItem item = Tester.generateWish();

            // don't test downloading image yet
            item.setDownloadImg(false);
            item.saveToLocal();
            TagItemDBManager.instance().Update_item_tags(item.getId(), Tester.randomTags());
        }
        SyncAgent.getInstance().sync();
    }

    private void deleteUsers() {
        // delete all the users created
        mSignal = new CountDownLatch(UserManager.userCount);

        for (ParseUser user : UserManager.userList) {
            HashMap<String, String> params = new HashMap<>();
            params.put("username", user.getUsername());
            ParseCloud.callFunctionInBackground("deleteUserByUsername", params, new FunctionCallback<Object>() {
                @Override
                public void done(Object result, ParseException e) {
                    if (e == null) {
                        Log.d(TAG, "delete user success");
                    } else {
                        Log.e(TAG, "delete user error " + e.toString());
                        assertTrue(false);
                    }
                    ParseServerTest.mSignal.countDown();
                }
            });
        }

        waitSignal(mSignal);
    }
}

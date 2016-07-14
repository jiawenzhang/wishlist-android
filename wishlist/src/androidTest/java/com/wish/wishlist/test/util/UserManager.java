package com.wish.wishlist.test.util;

import android.content.Context;
import android.test.InstrumentationTestCase;
import android.util.Log;

import com.parse.LogInCallback;
import com.parse.LogOutCallback;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.wish.wishlist.R;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

/**
 * Created by jiawen on 2016-07-13.
 */
public class UserManager extends InstrumentationTestCase {
    static final String TAG = "UserManager";
    static public ParseUser currentUser;

    static public int userCount = 5;
    static public ArrayList<ParseUser> userList = new ArrayList<>();

    public static void setupUserList() {
        userList.clear();
        for (int i = 0; i < userCount; i++) {
            ParseUser user = new ParseUser();
            String email = "user_" + i + "@test.com";
            user.setUsername(email);
            user.setPassword("123456");
            user.put("name", "name_" + i);
            user.setEmail(email);
            userList.add(user);
        }
    }

    public static void login(ParseUser user, final CountDownLatch signal, final Context context) {
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
                    currentUser = user;
                } else {
                    Log.d(TAG, "log in failed");
                    if (e != null) {
                        Log.d(TAG, context.getString(R.string.com_parse_ui_login_warning_parse_login_failed) + e.toString());
                        if (e.getCode() == ParseException.OBJECT_NOT_FOUND) {
                            Log.d(TAG, context.getString(R.string.com_parse_ui_parse_login_invalid_credentials_toast));
                        } else {
                            Log.d(TAG, context.getString(R.string.com_parse_ui_parse_login_failed_unknown_toast));
                        }
                    }
                    assertTrue(false);
                }
                signal.countDown();
            }
        });
    }

    public static void logout(final CountDownLatch signal) {
        ParseUser.logOutInBackground(new LogOutCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    // logout successful
                    Log.d(TAG, "success to logout");
                } else {
                    Log.e(TAG, "Fail to logout");
                    assertTrue(false);
                }
                signal.countDown();
            }
        });
    }
}


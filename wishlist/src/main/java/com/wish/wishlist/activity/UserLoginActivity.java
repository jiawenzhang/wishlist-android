package com.wish.wishlist.activity;

import com.parse.ParseInstallation;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.parse.ParseUser;
import com.parse.ui.ParseLoginBuilder;
import com.wish.wishlist.R;

import java.util.Arrays;

/**
 * Shows the user profile. This simple activity can function regardless of whether the user
 * is currently logged in.
 */
public class UserLoginActivity extends Activity {
    private static final int LOGIN_REQUEST = 0;
    private boolean fromSplash;
    final static String TAG = "UserLoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        fromSplash = intent.getBooleanExtra("fromSplash", false);

        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser != null) {
            onLogin();
            return;
        }

        ParseLoginBuilder builder = new ParseLoginBuilder(
                UserLoginActivity.this);
        Intent parseLoginIntent = builder.setParseLoginEnabled(true)
                .setAppLogo(R.drawable.splash_logo)
                .setParseLoginButtonText("Login")
                .setParseSignupButtonText("Register")
                .setParseLoginHelpText("Forgot password?")
                .setParseLoginInvalidCredentialsToastText("You email and/or password is not correct")
                .setParseLoginEmailAsUsername(true)
                .setParseSignupSubmitButtonText("Submit registration")
                .setFacebookLoginEnabled(true)
                .setFacebookLoginButtonText("Facebook")
                .setFacebookLoginPermissions(Arrays.asList("user_status", "read_stream"))
                .setTwitterLoginEnabled(true)
                .setTwitterLoginButtontext("Twitter")
                .build();
        startActivityForResult(parseLoginIntent, LOGIN_REQUEST);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case LOGIN_REQUEST: {
                onLogin();
            }
        }
    }

    private void onLogin() {
        Log.d(TAG, "login success");
        ParseUser currentUser = ParseUser.getCurrentUser();
        Log.d(TAG, "You are logged in as " + currentUser.getEmail() + " " + currentUser.getString("name"));
        ParseInstallation i = ParseInstallation.getCurrentInstallation();
        i.put("user", ParseUser.getCurrentUser());
        i.saveInBackground();
        Log.d(TAG, "installation saved");
        Log.d(TAG, "installation id: " + i.getInstallationId());

        if (fromSplash) {
            startActivity(new Intent(this, WishList.class));
        }
        finish();
    }
}

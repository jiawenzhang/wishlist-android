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
    private boolean mFromSplash;
    final static String FROM_SPLASH = "FROM_SPLASH";
    final static String TAG = "UserLoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        mFromSplash = intent.getBooleanExtra(FROM_SPLASH, false);

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
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case LOGIN_REQUEST: {
                    onLogin();
                }
            }
        } else if (resultCode == RESULT_CANCELED){
            Log.e(TAG, "Login canceled");
            if (mFromSplash) {
                // Show Android home screen
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
            finish();
        }
    }

    private void onLogin() {
        Log.d(TAG, "login success");
        ParseUser currentUser = ParseUser.getCurrentUser();
        Log.d(TAG, "You are logged in as " + currentUser.getEmail() + " " + currentUser.getString("name"));
        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
        installation.put("user", ParseUser.getCurrentUser());
        installation.saveInBackground();
        Log.d(TAG, "installation saved");
        Log.d(TAG, "installation id: " + installation.getInstallationId());

        if (mFromSplash) {
            startActivity(new Intent(this, WishList.class));
        }
        finish();
    }
}

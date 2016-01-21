package com.wish.wishlist.login;

import com.parse.ParseException;
import com.parse.ParseInstallation;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.wish.wishlist.activity.ProfileActivity;
import com.wish.wishlist.event.EventBus;
import com.wish.wishlist.event.ProfileChangeEvent;
import com.wish.wishlist.sync.SyncAgent;
import com.wish.wishlist.util.Options;
import com.wish.wishlist.util.ProfileUtil;
import com.wish.wishlist.wish.MyWishActivity;

import java.util.Arrays;

/**
 * Shows the user profile. This simple activity can function regardless of whether the user
 * is currently logged in.
 */
public class UserLoginActivity extends Activity {
    public final static String FROM_SPLASH = "FROM_SPLASH";
    public final static String ALLOW_SKIP = "ALLOW_SKIP";

    private final static String TAG = "UserLoginActivity";
    private static final int LOGIN_REQUEST = 0;
    private boolean mFromSplash = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        mFromSplash = intent.getBooleanExtra(FROM_SPLASH, false);
        boolean allowSkip = intent.getBooleanExtra(ALLOW_SKIP, false);

        ParseLoginBuilder builder = new ParseLoginBuilder(
                UserLoginActivity.this);
        Intent parseLoginIntent = builder.setParseLoginEnabled(true)
                .setParseLoginAllowSkip(allowSkip)
                .setParseLoginButtonText("Login")
                .setParseSignupButtonText("Sign up")
                .setParseLoginHelpText("Forgot password?")
                .setParseLoginInvalidCredentialsToastText("You email and/or password is not correct")
                .setParseLoginEmailAsUsername(true)
                .setParseSignupSubmitButtonText("Sign up")
                .setFacebookLoginEnabled(true)
                .setFacebookLoginPermissions(Arrays.asList("user_status", "read_stream"))
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
        } else if (resultCode == RESULT_CANCELED) {
            Log.e(TAG, "Login canceled");
            if (mFromSplash) {
                // Show Android home screen
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
            finish();
        } else if (resultCode == ParseLoginActivity.RESULT_LOGIN_SKIP) {
            onLoginSkip();
        }
    }

    private void onLoginSkip() {
        Log.d(TAG, "login skip");
        if (mFromSplash) {
            // Don't show login again on next startup
            Options.ShowLoginOnStartup showLoginOption = new Options.ShowLoginOnStartup(0);
            showLoginOption.save();

            startActivity(new Intent(this, MyWishActivity.class));
        } else {
            // from Settings->ProfileActivity
            startActivity(new Intent(getApplication(), ProfileActivity.class));
        }
        finish();
    }

    private void onLogin() {
        Log.d(TAG, "login success");
        ParseUser currentUser = ParseUser.getCurrentUser();
        Log.d(TAG, "You are logged in as " + currentUser.getEmail() + " " + currentUser.getString("name"));

        // Installation is used to identify self devices for push notification
        // so self device can sync wishes from Parse
        final ParseInstallation installation = ParseInstallation.getCurrentInstallation();
        installation.put("user", ParseUser.getCurrentUser());
        installation.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Log.d(TAG, "installation saved");
                    Log.d(TAG, "installation id: " + installation.getInstallationId());
                } else {
                    Log.e(TAG, e.toString());
                }
            }
        });

        ProfileUtil.downloadProfileImage();
        SyncAgent.getInstance().sync();

        if (mFromSplash) {
            startActivity(new Intent(this, MyWishActivity.class));
        } else {
            // event will notify navigation drawer to update username, email and profile image
            EventBus.getInstance().post(new ProfileChangeEvent(ProfileChangeEvent.ProfileChangeType.all));
            startActivity(new Intent(getApplication(), ProfileActivity.class));
        }
        finish();
    }
}

package com.wish.wishlist.activity;

import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.util.Log;

import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.widget.LoginButton;
import com.facebook.widget.LoginButton.OnErrorListener;
import com.facebook.FacebookException;

import com.wish.wishlist.R;

//followed the sample:
//https://developers.facebook.com/docs/howtos/androidsdk/3.0/fetch-user-data/

public class Login extends Activity {
    private static final String URL_PREFIX_FRIENDS = "https://graph.facebook.com/me/friends?access_token=";

    private String TAG="Login";
    private static final List<String> PERMISSIONS = Arrays.asList("publish_actions");

    private UiLifecycleHelper uiHelper;
    private Session.StatusCallback callback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            onSessionStateChange(session, state, exception);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uiHelper = new UiLifecycleHelper(this, callback);
        uiHelper.onCreate(savedInstanceState);

        setContentView(R.layout.login);
        LoginButton authButton = (LoginButton) findViewById(R.id.facebook_login_button);
        authButton.setPublishPermissions(PERMISSIONS);
        authButton.setOnErrorListener(new OnErrorListener() {
            @Override
            public void onError(FacebookException error) {
                Log.i(MyWish.LOG_TAG, "Error " + error.getMessage());
            }
        });

        Button skipButton = (Button) findViewById(R.id.skip_button);
        skipButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Login.this.finish();
            }
        });
    }

    private void onSessionStateChange(Session session, SessionState state, Exception exception) {
        Log.i(TAG, "onSessionStateChange");
        if (state.isOpened()) {
            Log.i(MyWish.LOG_TAG, "Logged in...");
            Login.this.finish();
            //	Request.executeMeRequestAsync(session, new Request.GraphUserCallback() {
            //		@Override
            //		public void onCompleted(GraphUser user,Response response) {
            //			if (user != null) {
            //				Log.i(MyWish.LOG_TAG,"User ID "+ user.getId());
            //				Log.i(MyWish.LOG_TAG,"Email "+ user.asMap().get("email"));
            //			}
            //		}
            //	});
        }
        else if (state.isClosed()) {
            Log.i(MyWish.LOG_TAG, "Logged out...");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(MyWish.LOG_TAG, "onResume");
        // For scenarios where the main activity is launched and user
        // session is not null, the session state change notification
        // may not be triggered. Trigger it if it's open/closed.
        Session session = Session.getActiveSession();
        if (session != null && (session.isOpened() || session.isClosed()) ) {
            onSessionStateChange(session, session.getState(), null);
        }
        uiHelper.onResume();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        uiHelper.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onPause() {
        super.onPause();
        uiHelper.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);
    }
}

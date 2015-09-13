package com.wish.wishlist.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.parse.ParseUser;
import com.wish.wishlist.R;

public class Profile extends Activity {
    final static String TAG = "Profile";
    ParseUser mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile);

        setUpActionBar();

        mUser = ParseUser.getCurrentUser();
        if (mUser != null) {
            ImageView profileImage = (ImageView) findViewById(R.id.profile_image);
            profileImage.setImageResource(R.drawable.splash_logo);

            TextView userName = (TextView) findViewById(R.id.username_text);
            userName.setText(mUser.getString("name"));

            TextView userEmail = (TextView) findViewById(R.id.user_email_text);
            userEmail.setText(mUser.getEmail());
        } else {
            Log.d(TAG, "currentUser null ");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if (mUser != null) {
            getMenuInflater().inflate(R.menu.menu_profile, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_profile_logout:
                mUser.logOut();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setUpActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }
}

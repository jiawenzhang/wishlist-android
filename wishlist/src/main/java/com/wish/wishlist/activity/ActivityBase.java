package com.wish.wishlist.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.wish.wishlist.util.Analytics;
import com.wish.wishlist.wish.AddWishFromLinkActivity;

import java.util.ArrayList;


/**
 * Created by jiawen on 15-09-26.
 */
public class ActivityBase extends AppCompatActivity {
    protected Toolbar mToolbar;
    private static final String TAG = "ActivityBase";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void setupActionBar(int resId) {
        // Set a toolbar to replace the action bar.
        mToolbar = (Toolbar) findViewById(resId);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void showLinkSnack(final ArrayList<String> links) {
        View rootView = findViewById(android.R.id.content);
        if (rootView == null) {
            return;
        }

        Analytics.send(Analytics.WISH, "LinkSnackbar", "Show");
        Snackbar.make(rootView, "You copied a link, add as a wish?\n" + links.get(0), Snackbar.LENGTH_LONG)
                .setAction("ADD", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Analytics.send(Analytics.WISH, "LinkSnackbar", "TapAdd");
                        Intent intent = new Intent(ActivityBase.this, AddWishFromLinkActivity.class);
                        intent.putStringArrayListExtra(AddWishFromLinkActivity.LINKS, links);
                        startActivity(intent);
                    }
                })
                .setDuration(10000)
                .show();
    }
}

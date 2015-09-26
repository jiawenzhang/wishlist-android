package com.wish.wishlist.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

/**
 * Created by jiawen on 15-09-26.
 */
public class ActivityBase extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void setupActionBar(int resId) {
        // Set a toolbar to replace the action bar.
        Toolbar toolbar = (Toolbar) findViewById(resId);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
}

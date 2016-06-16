package com.wish.wishlist.activity;

import android.os.Bundle;
import android.view.MenuItem;
import com.wish.wishlist.R;
import com.wish.wishlist.fragment.PrefsFragment;
import com.wish.wishlist.util.Analytics;

public class PreferenceActivity extends ActivityBase {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wishlist_preference);
        getSupportFragmentManager().beginTransaction().replace(R.id.pref_frame, new PrefsFragment()).commit();
        setupActionBar(R.id.pref_toolbar);

        Analytics.sendScreen("Preference");
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

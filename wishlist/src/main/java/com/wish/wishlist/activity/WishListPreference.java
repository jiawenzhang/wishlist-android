package com.wish.wishlist.activity;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.Toast;

import com.parse.ParseUser;
import com.wish.wishlist.R;
import com.wish.wishlist.util.sync.SyncAgent;
import com.wish.wishlist.view.ReleaseNotesView;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.SharedPreferences;
import android.preference.EditTextPreference;

@SuppressLint("NewApi")
public class WishListPreference extends PreferenceActivity implements
        OnSharedPreferenceChangeListener {
    private ImageButton _backImageButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.wishlist_preference);
        setContentView(R.layout.preference_parent);

        setUpActionBar();

        Preference userProfile = (Preference) findPreference("userProfile");
        userProfile.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                ParseUser currentUser = ParseUser.getCurrentUser();
                if (currentUser != null) {
                    startActivity(new Intent(getApplication(), Profile.class));
                } else {
                    startActivity(new Intent(getApplication(), UserProfileActivity.class));
                }
                return true;
            }
        });

        // Get the custom preference
        EditTextPreference currencyTextPref = (EditTextPreference) findPreference("currency");
        currencyTextPref.setSummary(currencyTextPref.getText());

        Preference newFeature = (Preference) findPreference("newFeature");
        newFeature.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(getApplication(), NewFeatureFragmentActivity.class));
                return true;
            }
        });

        Preference releaseNotes = (Preference) findPreference("releaseNotes");
        releaseNotes.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                //Toast.makeText(getBaseContext(), "The release notes has been clicked", Toast.LENGTH_LONG).show();
                //		SharedPreferences customSharedPreference = getSharedPreferences(
                //			"myCustomSharedPrefs", Activity.MODE_PRIVATE);
                //		SharedPreferences.Editor editor = customSharedPreference;
                //	.edit();
                //editor.putString("myCustomPref",
                //	"The preference has been clicked");
                //editor.commit();

                //for testing
                //SyncAgent.getInstance(WishListPreference.this).sync();

                ReleaseNotesView view = new ReleaseNotesView(WishListPreference.this);
                view.show();
                return true;
            }
        });

        Preference rateApp = (Preference) findPreference("rateApp");
        rateApp.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Uri uri = Uri.parse("market://details?id=" + getPackageName());
                Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                try {
                    startActivity(goToMarket);
                } catch (ActivityNotFoundException e) {
                }
                //Toast.makeText(getBaseContext(), "The rate app has been clicked", Toast.LENGTH_LONG).show();
                return true;
            }
        });

        Preference facebook = (Preference) findPreference("facebook");
        facebook.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Uri link = Uri.parse("https://www.facebook.com/BeansWishlist");
                Intent launchBrowser = new Intent(Intent.ACTION_VIEW, link);
                startActivity(launchBrowser);
                return true;
            }
        });

        Preference privacy = (Preference) findPreference("privacy");
        privacy.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Uri link = Uri.parse("http://beanswishlist.com/privacy.html");
                Intent launchBrowser = new Intent(Intent.ACTION_VIEW, link);
                startActivity(launchBrowser);
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
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

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updatePrefSummary(findPreference(key));
    }

    private void updatePrefSummary(Preference p) {
        if (p instanceof EditTextPreference) {
            EditTextPreference editTextPref = (EditTextPreference) p;
            p.setSummary(editTextPref.getText());
        }
    }

    private void setUpActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }
}

package com.wish.wishlist.activity;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

import com.parse.ParseUser;
import com.wish.wishlist.R;
import com.wish.wishlist.view.ReleaseNotesView;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.SharedPreferences;
import android.preference.EditTextPreference;
import android.widget.LinearLayout;

@SuppressLint("NewApi")
public class WishListPreference extends PreferenceActivity implements
        OnSharedPreferenceChangeListener {
    private ImageButton _backImageButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.wishlist_preference);
        setContentView(R.layout.preference_parent);

        Preference userProfile = (Preference) findPreference("userProfile");
        userProfile.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                ParseUser currentUser = ParseUser.getCurrentUser();
                if (currentUser != null) {
                    startActivity(new Intent(getApplication(), Profile.class));
                } else {
                    startActivity(new Intent(getApplication(), UserLoginActivity.class));
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
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        LinearLayout root = (LinearLayout)findViewById(android.R.id.list).getParent().getParent().getParent();
        Toolbar bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.tool_bar, root, false);
        bar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        root.addView(bar, 0); // insert at top
        bar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
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
}

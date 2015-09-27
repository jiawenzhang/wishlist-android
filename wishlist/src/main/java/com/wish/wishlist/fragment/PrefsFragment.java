package com.wish.wishlist.fragment;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.parse.ParseUser;
import com.wish.wishlist.R;
import com.wish.wishlist.activity.NewFeatureFragmentActivity;
import com.wish.wishlist.activity.Profile;
import com.wish.wishlist.activity.UserLoginActivity;
import com.wish.wishlist.view.ReleaseNotesView;

/**
 * Created by jiawen on 15-09-27.
 */
public class PrefsFragment extends PreferenceFragment
    implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preference);

        final Preference userProfile = findPreference("userProfile");
        userProfile.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                ParseUser currentUser = ParseUser.getCurrentUser();
                if (currentUser != null) {
                    startActivity(new Intent(getActivity(), Profile.class));
                } else {
                    startActivity(new Intent(getActivity(), UserLoginActivity.class));
                }
                return true;
            }
        });

        // Get the custom preference
        final EditTextPreference currencyTextPref = (EditTextPreference) findPreference("currency");
        currencyTextPref.setSummary(currencyTextPref.getText());

        Preference newFeature = findPreference("newFeature");
        newFeature.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(getActivity(), NewFeatureFragmentActivity.class));
                return true;
            }
        });

        final Preference releaseNotes = findPreference("releaseNotes");
        releaseNotes.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
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

                ReleaseNotesView view = new ReleaseNotesView(getActivity());
                view.show();
                return true;
            }
        });

        final Preference rateApp = findPreference("rateApp");
        rateApp.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Uri uri = Uri.parse("market://details?id=" + getActivity().getPackageName());
                Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                try {
                    startActivity(goToMarket);
                } catch (ActivityNotFoundException e) {
                }
                //Toast.makeText(getBaseContext(), "The rate app has been clicked", Toast.LENGTH_LONG).show();
                return true;
            }
        });

        final Preference facebook = findPreference("facebook");
        facebook.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Uri link = Uri.parse("https://www.facebook.com/BeansWishlist");
                Intent launchBrowser = new Intent(Intent.ACTION_VIEW, link);
                startActivity(launchBrowser);
                return true;
            }
        });

        final Preference privacy = findPreference("privacy");
        privacy.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Uri link = Uri.parse("http://beanswishlist.com/privacy.html");
                Intent launchBrowser = new Intent(Intent.ACTION_VIEW, link);
                startActivity(launchBrowser);
                return true;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister the listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
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

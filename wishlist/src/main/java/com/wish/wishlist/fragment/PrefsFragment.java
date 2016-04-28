package com.wish.wishlist.fragment;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.app.DialogFragment;
import android.preference.PreferenceManager;

import com.parse.ParseUser;
import com.wish.wishlist.BuildConfig;
import com.wish.wishlist.R;
import com.wish.wishlist.WishlistApplication;
import com.wish.wishlist.activity.DebugActivity;
import com.wish.wishlist.event.EventBus;
import com.wish.wishlist.event.MyWishChangeEvent;
import com.wish.wishlist.feature.NewFeatureFragmentActivity;
import com.wish.wishlist.activity.ProfileActivity;
import com.wish.wishlist.login.UserLoginActivity;
import com.wish.wishlist.util.Analytics;
import com.wish.wishlist.view.ReleaseNotesView;

/**
 * Created by jiawen on 15-09-27.
 */
public class PrefsFragment extends PreferenceFragment implements
        CurrencyFragmentDialog.onCurrencyChangedListener {

    private static String TAG = "PrefsFragment";

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
                    startActivity(new Intent(getActivity(), ProfileActivity.class));
                } else {
                    startActivity(new Intent(getActivity(), UserLoginActivity.class));
                }
                return true;
            }
        });

        PreferenceCategory generalCategory = (PreferenceCategory) findPreference("general");
        if (!getResources().getBoolean(R.bool.enable_friend)) {
            final Preference p = findPreference("wishDefaultPrivate");
            generalCategory.removePreference(p);
        }

        if (!getResources().getBoolean(R.bool.enable_account)) {
            generalCategory.removePreference(userProfile);
        }

        final Preference currencyPref = findPreference("currency");
        String currency = PreferenceManager.getDefaultSharedPreferences(WishlistApplication.getAppContext()).getString("currency", "");
        currencyPref.setSummary(currency);
        currencyPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                DialogFragment currencyFragment = new CurrencyFragmentDialog();
                currencyFragment.setTargetFragment(PrefsFragment.this, 0);
                currencyFragment.show(getFragmentManager(), "dialog");
                return true;
            }
        });

        // Get the custom preference
        final Preference newFeature = findPreference("newFeature");
        newFeature.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(getActivity(), NewFeatureFragmentActivity.class));
                return true;
            }
        });
        PreferenceCategory c = (PreferenceCategory) findPreference("about");
        c.removePreference(newFeature);

        final Preference releaseNotes = findPreference("releaseNotes");
        releaseNotes.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
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

        final Preference debug = findPreference("debug");
        if (!BuildConfig.DEBUG) {
            PreferenceCategory aboutCategory = (PreferenceCategory) findPreference("about");
            aboutCategory.removePreference(debug);
        } else {
            debug.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    startActivity(new Intent(getActivity(), DebugActivity.class));
                    return true;
                }
            });
        }
    }

    public void onCurrencyChanged(String currency) {
        final Preference currencyPref = findPreference("currency");
        currencyPref.setSummary(currency);

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WishlistApplication.getAppContext()).edit();
        editor.putString("currency", currency);
        editor.commit();

        EventBus.getInstance().post(new MyWishChangeEvent());
        Analytics.send(Analytics.WISH, "Currency", currency);
    }
}

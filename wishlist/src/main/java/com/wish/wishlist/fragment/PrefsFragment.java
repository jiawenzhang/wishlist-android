package com.wish.wishlist.fragment;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.app.AlertDialog;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.Log;

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
import com.wish.wishlist.util.Util;
import com.wish.wishlist.view.ReleaseNotesView;

/**
 * Created by jiawen on 15-09-27.
 */
public class PrefsFragment extends PreferenceFragmentCompat implements
        CurrencyFragmentDialog.onCurrencyChangedListener {

    private static String TAG = "PrefsFragment";
    private final static int PERMISSIONS_REQUEST_LOCATION = 1;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String s) {
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
        final Preference p = findPreference("wishDefaultPrivate");
        generalCategory.removePreference(p);

        if (!Util.deviceAccountEnabled()) {
            generalCategory.removePreference(userProfile);
        }

        final Preference pref = findPreference("autoLocation");
        pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Boolean checked = (Boolean) newValue;
                if (checked) {
                    if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                        requestPermissions( new String[] {
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION},
                                PERMISSIONS_REQUEST_LOCATION);

                        return false;
                    } else {
                        Log.d(TAG, "change to auto location");
                        return true;
                    }
                } else {
                    Log.d(TAG, "change to not auto location");
                    return true;
                }
            }
        });

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
                    Log.e(TAG, e.toString());
                }
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult");
        switch (requestCode) {
            case PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length == 2 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                    Log.d(TAG, "granted");

                    Analytics.send(Analytics.PERMISSION, "Location", "Grant");
                    final CheckBoxPreference pref = (CheckBoxPreference) findPreference("autoLocation");
                    pref.setChecked(true);
                } else {
                    Log.d(TAG, "denied");
                    Analytics.send(Analytics.PERMISSION, "Location", "Deny");
                    if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) ||
                            ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)) {
                        Log.d(TAG, "should");
                    } else {
                        Log.d(TAG, "should not");
                        // User has selected Never Ask Again previously, so Android won't show permission request dialog.
                        // Notify user to enable permission in system settings
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AppCompatAlertDialogStyle);
                        builder.setMessage("To allow auto location tag, please enable location permission in System Settings -> Apps -> Wishlist -> Permissions.").setCancelable(
                                false).setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {}
                                });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                }
            }
        }
    }
}

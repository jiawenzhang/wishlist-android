package com.wish.wishlist.util;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by jiawen on 2016-06-05.
 */
public class Util {

    final private static String TAG = "Util";
    private static boolean deviceAccountEnabled = false;
    /**
     * Get ISO 3166-1 alpha-2 country code for this device
     * @param context Context reference to get the TelephonyManager instance from
     * @return country code or null
     */
    public static String getDeviceCountry(Context context) {
        try {
            final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            final String simCountry = tm.getSimCountryIso();
            if (simCountry != null && simCountry.length() == 2) { // SIM country code is available
                String country = simCountry.toUpperCase(Locale.US);
                Analytics.send(Analytics.DEVICE, "Sim", country);
                return country;
            } else if (tm.getPhoneType() != TelephonyManager.PHONE_TYPE_CDMA) { // device is not 3G (would be unreliable)
                String networkCountry = tm.getNetworkCountryIso();
                if (networkCountry != null && networkCountry.length() == 2) { // network country code is available
                    String country = networkCountry.toUpperCase(Locale.US);
                    Analytics.send(Analytics.DEVICE, "Network", country);
                    return country;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

        String country = context.getResources().getConfiguration().locale.getCountry();
        Analytics.send(Analytics.DEVICE, "Locale", country);
        return country;
    }

    public static void initDeviceAccountEnabled() {
        Options.DeviceCountry deviceCountry = new Options.DeviceCountry(null);
        deviceCountry.read();
        ArrayList<String> enabledCountryList = new ArrayList<>();
        enabledCountryList.add("US");
        enabledCountryList.add("CA");
        if (deviceCountry.val() != null && enabledCountryList.contains(deviceCountry.val())) {
            deviceAccountEnabled = true;
        }
    }

    public static boolean deviceAccountEnabled() {
        return deviceAccountEnabled;
    }
}

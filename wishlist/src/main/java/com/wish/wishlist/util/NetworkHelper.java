package com.wish.wishlist.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.wish.wishlist.WishlistApplication;

/**
 * Created by jiawen on 2015-12-25.
 */
public class NetworkHelper {
    private static NetworkHelper ourInstance = new NetworkHelper();

    public static NetworkHelper getInstance() {
        return ourInstance;
    }

    private NetworkHelper() {
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) WishlistApplication.getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}

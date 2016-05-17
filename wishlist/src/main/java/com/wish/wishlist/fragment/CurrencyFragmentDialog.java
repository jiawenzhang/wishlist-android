package com.wish.wishlist.fragment;

import android.os.Bundle;
import android.preference.PreferenceManager;

import com.wish.wishlist.WishlistApplication;

/**
 * Created by jiawen on 15-09-16.
 */
public class CurrencyFragmentDialog extends EditFragmentDialog {
    private static String TAG = "CurrencyFragmentDialog";

    public interface onCurrencyChangedListener {
        void onCurrencyChanged(String email);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void onCurrencyChanged(String currency) {
        onCurrencyChangedListener listener = (onCurrencyChangedListener) getTargetFragment();
        listener.onCurrencyChanged(currency);
    }

    protected void configEditText() {
        String currency = PreferenceManager.getDefaultSharedPreferences(WishlistApplication.getAppContext()).getString("currency", "");
        mEditText.setHint("Currency symbol");
        if (!currency.isEmpty()) {
            setText(currency);
        }
    }

    public boolean onOK(String text) {
        String currency = mEditText.getText().toString().trim();
        if (currency.isEmpty()) {
            showError("Currency cannot be empty");
            return false;
        }
        onCurrencyChanged(currency);
        return true;
    }
}

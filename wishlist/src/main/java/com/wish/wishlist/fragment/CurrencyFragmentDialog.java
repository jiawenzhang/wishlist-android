package com.wish.wishlist.fragment;

import android.os.Bundle;

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
        mEditText.setHint("Currency symbol");
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

package com.wish.wishlist.fragment;

import android.os.Bundle;

public class PasswordFragmentDialog extends EditFragmentDialog {
    private static String TAG = "PasswordFragmentDialog";

    public interface OnPasswordListener {
        void onPassword(String password);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void onPassword(String password) {
        OnPasswordListener listener = (OnPasswordListener) getTargetFragment();
        listener.onPassword(password);
    }

    protected void configEditText() {
        mEditText.setHint("Password");
    }

    public boolean onOK(String text) {
        String password = mEditText.getText().toString().trim();
        if (password.isEmpty()) {
            showError("Password cannot be empty");
            return false;
        }
        onPassword(password);
        return true;
    }
}

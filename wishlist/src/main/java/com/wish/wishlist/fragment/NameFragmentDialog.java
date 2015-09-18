package com.wish.wishlist.fragment;

import android.app.Activity;
import android.util.Log;

/**
 * Created by jiawen on 15-09-16.
 */
public class NameFragmentDialog extends EditFragmentDialog {

    private static String TAG = "NameFragmentDialog";

    public interface onNameChangedListener {
        void onNameChanged(String email);
    }

    protected void onNameChanged(String email) {
        onNameChangedListener listener = (onNameChangedListener) getActivity();
        listener.onNameChanged(email);
    }

    @Override
    public void onAttach(Activity activity) {
        if (!(activity instanceof onNameChangedListener)) {
            throw new IllegalStateException("Activity must implement onNameChanged");
        }
        super.onAttach(activity);
    }

    protected void configEditText() {
        mEditText.setHint("Change name");
    }

    public boolean onOK(String text) {
        String name = mEditText.getText().toString().trim();
        if (name.isEmpty()) {
            showError("Name cannot be empty");
            return false;
        } else {
            onNameChanged(name);
            return true;
        }
    }
}

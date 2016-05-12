package com.wish.wishlist.fragment;

import android.app.Activity;
import android.content.res.Resources;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.wish.wishlist.util.Analytics;

/**
 * Created by jiawen on 15-09-16.
 */
public class EmailFragmentDialog extends EditFragmentDialog {
    private static String TAG = "EmailFragmentDialog";

    public interface onEmailChangedListener {
        void onEmailChanged(String email);
    }

    protected void onEmailChanged(String email) {
        onEmailChangedListener listener = (onEmailChangedListener) getActivity();
        listener.onEmailChanged(email);
        Analytics.send(Analytics.USER, "ChangeEmail", null);
    }

    @Override
    public void onAttach(Activity activity) {
        if (!(activity instanceof onEmailChangedListener)) {
            throw new IllegalStateException("Activity must implement onEmailChanged");
        }
        super.onAttach(activity);
    }

    protected void configEditText() {
        mEditText.setHint("Change email");
        mEditText.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
    }

    public boolean onOK(String text) {
        String email = mEditText.getText().toString().trim();
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Invalid email format");
            return false;
        } else {
            onEmailChanged(email);
            return true;
        }
    }
}

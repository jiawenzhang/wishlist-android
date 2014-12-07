package com.wish.wishlist.view;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.wish.wishlist.R;

/**
 * Created by jiawen on 14-12-07.
 */

public class CustomEditTextPreference extends EditTextPreference {
    public CustomEditTextPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    public CustomEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomEditTextPreference(Context context) {
        super(context);
    }

    protected void showDialog(Bundle state) {
        super.showDialog(state);
        AlertDialog d = (AlertDialog) getDialog();
        try {
            int titleId = d.getContext().getResources().getIdentifier("android:id/alertTitle", null, null);
            TextView titleView = (TextView) d.findViewById(titleId);
            titleView.setTextColor(getContext().getResources().getColor(R.color.wishlist_yellow_color)); // earlier defined color-int
        } catch (Exception e) {
        }

        try {
            int divierId = d.getContext().getResources().getIdentifier("android:id/titleDivider", null, null);
            View divider = d.findViewById(divierId);
            divider.setBackgroundColor(getContext().getResources().getColor(R.color.wishlist_yellow_color)); // earlier defined color-int
        } catch (Exception e) {
        }

        Button positiveButton = d.getButton(AlertDialog.BUTTON_POSITIVE);
        if (positiveButton != null) {
            positiveButton.setBackgroundResource(R.drawable.selectable_background_wishlist);
        }
        Button negativeButton = d.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (negativeButton != null) {
            negativeButton.setBackgroundResource(R.drawable.selectable_background_wishlist);
        }
    }
}

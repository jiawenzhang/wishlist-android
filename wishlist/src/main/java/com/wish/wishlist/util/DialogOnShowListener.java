package com.wish.wishlist.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.wish.wishlist.R;

/**
 * Created by jiawen on 14-12-07.
 */

public class DialogOnShowListener implements DialogInterface.OnShowListener {
    Activity activity;

    public DialogOnShowListener(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void onShow(DialogInterface dialog) {
        AlertDialog d= ((AlertDialog) dialog);
        try {
            int titleId = d.getContext().getResources().getIdentifier("android:id/alertTitle", null, null);
            TextView titleView = (TextView) d.findViewById(titleId);
            titleView.setTextColor(activity.getResources().getColor(R.color.bodyText_dark_grey));
        } catch (Exception e) {
        }

        try {
            int divierId = d.getContext().getResources().getIdentifier("android:id/titleDivider", null, null);
            View divider = d.findViewById(divierId);
            divider.setBackgroundColor(activity.getResources().getColor(R.color.wishlist_yellow_color));
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

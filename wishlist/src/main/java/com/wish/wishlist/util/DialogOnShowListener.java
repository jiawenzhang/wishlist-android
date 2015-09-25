package com.wish.wishlist.util;

import android.app.Activity;
import android.support.v7.app.AlertDialog;
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
        AlertDialog alertDialog = (AlertDialog) dialog;
        if (alertDialog == null) {
            return;
        }
        try {
            int titleId = alertDialog.getContext().getResources().getIdentifier("android:id/alertTitle", null, null);
            TextView titleView = (TextView) alertDialog.findViewById(titleId);
            titleView.setTextColor(activity.getResources().getColor(R.color.bodyText_dark_grey));
        } catch (Exception e) {
        }

        try {
            int divierId = alertDialog.getContext().getResources().getIdentifier("android:id/titleDivider", null, null);
            View divider = alertDialog.findViewById(divierId);
            divider.setBackgroundColor(activity.getResources().getColor(R.color.wishlist_yellow_color));
        } catch (Exception e) {
        }


        Button positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (positiveButton != null) {
            positiveButton.setBackgroundResource(R.drawable.selectable_background_wishlist);
        }
        Button negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (negativeButton != null) {
            negativeButton.setBackgroundResource(R.drawable.selectable_background_wishlist);
        }
    }
}

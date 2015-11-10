package com.wish.wishlist.fragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.wish.wishlist.R;

/**
 * Created by jiawen on 15-11-08.
 */
public class InviteFriendFragmentDialog extends DialogFragment {

    private static final String TAG = "InviteFriendFragment";
    private final static String msgSubject = "Get Beans Wishlist";
    private final static String msgBody = "Get Beans Wishlist for Android, and let's see each other's wishes\n" +
            "https://play.google.com/store/apps/details?id=com.wish.wishlist";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity(), R.style.AppCompatAlertDialogStyle);
        final int FACEBOOK = 0;
        final int EMAIL = 1;
        final int MESSAGE = 2;
        final int MORE = 3;
        final String[] mListItems = { "Facebook", "Email", "Message", "More"};
        dialogBuilder.setItems(mListItems, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                switch (item) {
                    case FACEBOOK:
                        Log.d(TAG, "Facebook");
                        break;
                    case EMAIL:
                        Log.d(TAG, "Email");
                        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                        emailIntent.setData(Uri.parse("mailto:" + ""));
                        emailIntent.putExtra(Intent.EXTRA_SUBJECT, msgSubject);
                        emailIntent.putExtra(Intent.EXTRA_TEXT, msgBody);
                        try {
                            startActivity(Intent.createChooser(emailIntent, "Send E-Mail..."));
                        } catch (android.content.ActivityNotFoundException e) {
                            Log.e(TAG, e.toString());
                        }
                        break;
                    case MESSAGE:
                        Log.d(TAG, "Message");
                        Intent sendIntent = new Intent(Intent.ACTION_VIEW);
                        sendIntent.setType("vnd.android-dir/mms-sms");
                        sendIntent.putExtra("sms_body", msgBody);
                        startActivity(sendIntent);
                        break;
                    case MORE:
                        final Intent i = new Intent(Intent.ACTION_SEND);
                        i.setType("text/plain");
                        i.putExtra(Intent.EXTRA_SUBJECT, msgSubject);
                        i.putExtra(Intent.EXTRA_TEXT, msgBody);
                        try {
                            startActivity(Intent.createChooser(i, ""));
                        } catch (android.content.ActivityNotFoundException e) {
                            Log.e(TAG, e.toString());
                        }
                        break;
                }
            }
        });

        return dialogBuilder.create();
    }
}

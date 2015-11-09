package com.wish.wishlist.fragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.wish.wishlist.R;

/**
 * Created by jiawen on 15-11-08.
 */
public class InviteFriendFragmentDialog extends DialogFragment {

    private static final String TAG = "InviteFriendFragmentDialog";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity(), R.style.AppCompatAlertDialogStyle);
        final int FACEBOOK = 0;
        final int EMAIL = 1;
        final int MESSAGE = 2;
        final String[] mListItems = { "Facebook", "Email", "Message" };
        dialogBuilder.setItems(mListItems, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                //String selectedText = mListItems[item].toString();
                switch (item) {
                    case FACEBOOK:
                        Log.d(TAG, "Facebook");
                        break;
                    case EMAIL:
                        Log.d(TAG, "Email");
                        break;
                    case MESSAGE:
                        Log.d(TAG, "Message");
                        break;
                }
            }
        });

        return dialogBuilder.create();
    }
}

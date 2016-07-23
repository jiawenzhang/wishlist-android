package com.wish.wishlist.fragment;

import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatDialog;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.wish.wishlist.R;

/**
 * Created by jiawen on 15-09-16.
 */
public class EditFragmentDialog extends DialogFragment {
    protected EditText mEditText;
    private static String TAG = "EditFragmentDialog";

    protected void configEditText() {}

    protected void setText(String text) {
        if (text != null) {
            mEditText.setText(text);
            mEditText.setSelection(text.length());
        }
    }

    protected boolean onOK(String text) { return true; }

    protected void showError(String error) {
        Toast toast = Toast.makeText(getActivity(), error, Toast.LENGTH_SHORT);
        int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
        toast.setGravity(Gravity.TOP | Gravity.CENTER, 0, screenHeight/4);
        toast.show();
    }

    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AppCompatAlertDialogStyle);
        // Get the layout inflater
        final LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        final View v = inflater.inflate(R.layout.simple_edit_dialog, null);
        mEditText = (EditText) v.findViewById(R.id.edit_dialog_text);
        configEditText();

        builder.setView(v)
                // Add action buttons
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        //Do nothing here because we override this button later to change the close behaviour.
                        //However, we still need this because on older versions of Android unless we
                        //pass a handler the button doesn't get instantiated
                        //onOK(mEditText.getText().toString().trim());
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        EditFragmentDialog.this.getDialog().cancel();
                    }
                });
        AppCompatDialog d = builder.create();
        d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return d;
    }

    @Override
    public void onStart() {
        super.onStart();
        //super.onStart() is where dialog.show() is actually called on the underlying dialog, so we have to do it after this point
        final AlertDialog d = (AlertDialog) getDialog();
        if(d != null) {
            Button positiveButton = d.getButton(AppCompatDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v)
                {
                    if (onOK(mEditText.getText().toString().trim())) {
                        d.dismiss();
                    }
                    //else dialog stays open. Make sure you have an obvious way to close the dialog especially if you set cancellable to false.
                }
            });
        }
    }
}

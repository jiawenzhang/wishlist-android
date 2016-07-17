package com.wish.wishlist.fragment;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

import com.wish.wishlist.R;
import com.wish.wishlist.util.Options;

/**
 * Created by jiawen on 15-09-16.
 */
public class PrivateWishExplainFragmentDialog extends DialogFragment {
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AppCompatAlertDialogStyle);
        // Get the layout inflater
        final LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        final View v = inflater.inflate(R.layout.private_wish_explain_dialog, null);

        builder.setView(v)
                // Add action buttons
                .setPositiveButton("GOT IT", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        CheckBox c = (CheckBox) v.findViewById(R.id.no_show_next_time);
                        if (c.isChecked()) {
                            Options.ShowPrivateWishExplainDialog op = new Options.ShowPrivateWishExplainDialog();
                            op.setVal(0);
                            op.save();
                        }
                        dismiss();
                    }
                });
        return builder.create();
    }
}

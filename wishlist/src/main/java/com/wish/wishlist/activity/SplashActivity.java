package com.wish.wishlist.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.support.v7.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.parse.ParseUser;
import com.wish.wishlist.R;
import com.wish.wishlist.db.DBAdapter;
import com.wish.wishlist.WishlistApplication;
import com.wish.wishlist.login.UserLoginActivity;
import com.wish.wishlist.util.MigrationTask;
import com.wish.wishlist.util.Options;
import com.wish.wishlist.wish.MyWishActivity;

import java.io.File;

public class SplashActivity extends Activity implements
        MigrationTask.OnMigrationDone {
    private static final String TAG = "SplashActivity";
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Tracker t = ((WishlistApplication) getApplication()).getTracker(WishlistApplication.TrackerName.APP_TRACKER);
        t.send(new HitBuilders.EventBuilder()
                .setCategory("App")
                .setAction("Start")
                .build());

        DBAdapter.getInstance().createDB();
        File dir = new File(WishlistApplication.getAppContext().getFilesDir(), "/image");
        if (!dir.exists()) {
            dir.mkdir();
        }

        File thumb_dir = new File(WishlistApplication.getAppContext().getFilesDir(), "/thumb");
        if (!thumb_dir.exists()) {
            thumb_dir.mkdir();
        }

        showProgressDialog("Updating...");
        new MigrationTask(this).execute();
    }

    private void showProgressDialog(final String text) {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(text);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    protected void dismissProgressDialog() {
        mProgressDialog.dismiss();
    }

    private void showWhatsNewDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(SplashActivity.this, R.style.AppCompatAlertDialogStyle);
        builder.setTitle("What's new");
        builder.setMessage("Version 1.1.1\n\n" +
                "Completely renovated grid view, showing wishes in multi-column staggered fashion.\n\n" +
                "Improved list view.\n\n" +
                "Loads wish images more efficiently.\n");

        builder.setCancelable(false);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                startActivity(new Intent(getApplication(), MyWishActivity.class));
                SplashActivity.this.finish();
            }
        });
        AlertDialog dialog = builder.create();
        //dialog.setOnShowListener(new DialogOnShowListener(SplashActivity.this));
        dialog.show();
    }

    @Override
    public void onMigrationDone() {
        Log.d(TAG, "onMigrationDone");
        dismissProgressDialog();
        if (getResources().getBoolean(R.bool.enable_account)) {
            Options.ShowLoginOnStartup showLoginOption = new Options.ShowLoginOnStartup();
            showLoginOption.read();
            if (ParseUser.getCurrentUser() == null && showLoginOption.val() == 1) {
                // User is not logged in and he has not skipped login before

                Intent intent = new Intent(getApplication(), UserLoginActivity.class);
                intent.putExtra(UserLoginActivity.FROM_SPLASH, true);
                intent.putExtra(UserLoginActivity.ALLOW_SKIP, true);
                startActivity(intent);
            } else {
                startActivity(new Intent(getApplication(), MyWishActivity.class));
            }
        } else {
            startActivity(new Intent(getApplication(), MyWishActivity.class));
        }

        SplashActivity.this.finish();
    }
}

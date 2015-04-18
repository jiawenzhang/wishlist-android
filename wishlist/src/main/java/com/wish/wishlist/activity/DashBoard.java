package com.wish.wishlist.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.wish.wishlist.R;
import com.wish.wishlist.db.DBAdapter;
import com.wish.wishlist.util.camera.CameraManager;

public class DashBoard extends Activity {
    static final private int TAKE_PICTURE = 1;
    private static final int EDIT_ITEM = 2;
    private String _fullsizePhotoPath = null;
    private String _newfullsizePhotoPath = null;
    private static final String VERSION_KEY = "version_number";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dashboard);

        setUpActionBar();
        // myDBAdapter is effective only when the database is first created
        DBAdapter.getInstance(this).createDB();

        if (savedInstanceState != null) {
            Log.d(WishList.LOG_TAG, "savedInstanceState != null");
            // restore the current selected item in the list
            _newfullsizePhotoPath = savedInstanceState.getString("newfullsizePhotoPath");
            _fullsizePhotoPath = savedInstanceState.getString("fullsizePhotoPath");

            Log.d(WishList.LOG_TAG, "_newfullsizePhotoPath " + _newfullsizePhotoPath);
            Log.d(WishList.LOG_TAG, "_fullsizePhotoPath " + _fullsizePhotoPath);
        }
        else{
            Log.d(WishList.LOG_TAG, "savedInstanceState == null");
        }

        //show the what's new dialog if necessary
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        int currentVersionNumber = 0;
        int savedVersionNumber = sharedPref.getInt(VERSION_KEY, 0);
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            currentVersionNumber = pi.versionCode;
        } catch (Exception e) {}

        if (currentVersionNumber > savedVersionNumber) {
            showWhatsNewDialog();
            Editor editor = sharedPref.edit();
            editor.putInt(VERSION_KEY, currentVersionNumber);
            editor.commit();
        }
    }

    /**
     * Handle the click of a Feature button.
     *
     * @param v View
     * @return void
     */


    public void onClickFeature (View v)
    {
        int id = v.getId ();
        if (id == R.id.home_btn_new_item) {
            startActivityForResult(new Intent(getApplicationContext(), EditItem.class), EDIT_ITEM);
        }
        else if(id ==  R.id.home_btn_wishlist) {
            startActivity (new Intent(getApplicationContext(), WishList.class));
        }
        else if(id == R.id.home_btn_camera) {
            // startActivity (new Intent(getApplicationContext(), IntentIntegrator.class));
//			   Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//			   startActivityForResult(intent, TAKE_PICTURE);
            dispatchTakePictureIntent();
        }
//		  case R.id.home_btn_scan :
////			   startActivity (new Intent(getApplicationContext(), F4Activity.class));
//			   IntentIntegrator.initiateScan(this);
//			   break;
        else if(id == R.id.home_btn_settings) {
            Intent prefIntent = new Intent(getApplicationContext(), WishListPreference.class);
            startActivity(prefIntent);
//			   startActivity (new Intent(getApplicationContext(), F5Activity.class));
        }
//		  case R.id.home_btn_help :
////			   startActivity (new Intent(getApplicationContext(), F6Activity.class));
//			   break;
    }

    /**
     * Handle the click on the home button.
     *
     * @param v View
     * @return void
     */

    public void onClickHome (View v)
    {
        goHome (this);
    }

    /**
     * Go back to the home activity.
     *
     * @param context Context
     * @return void
     */

    public void goHome(Context context)
    {
//		final Intent intent = new Intent(context, HomeActivity.class);
//		intent.setFlags (Intent.FLAG_ACTIVITY_CLEAR_TOP);
//		context.startActivity (intent);
    }

    /**
     * Handle the click on the search button.
     *
     * @param v View
     * @return void
     */

    public void onClickSearch (View v)
    {
        //startActivity (new Intent(getApplicationContext(), SearchActivity.class));
    }

    /**
     * Handle the click on the About button.
     *
     * @param v View
     * @return void
     */

    public void onClickAbout (View v)
    {
        //startActivity (new Intent(getApplicationContext(), AboutActivity.class));
    }

    /**
     * Use the activity label to set the text in the activity's title text view.
     * The argument gives the name of the view.
     *
     * <p> This method is needed because we have a custom title bar rather than the default Android title bar.
     * See the theme definitons in styles.xml.
     *
     * @param textViewId int
     * @return void
     */


    public void setTitleFromActivityLabel (int textViewId)
    {
        TextView tv = (TextView) findViewById (textViewId);
        if (tv != null) tv.setText (getTitle ());
    }

    private void dispatchTakePictureIntent() {
        CameraManager c = new CameraManager();
        _newfullsizePhotoPath = c.getPhotoPath();
        startActivityForResult(c.getCameraIntent(), TAKE_PICTURE);
    }

    private void showWhatsNewDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("What's new");
        builder.setMessage("Version 1.0.10\n\n" +
                "Users can add tags to wishes, view the tags of a wish, and look up wishes by tag.\n\n" +
                "Improved messages on header bar about the status and tags of the wishes shown.");
        builder.setCancelable(false);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        builder.create().show();

        //	LayoutInflater inflater = LayoutInflater.from(this);
        //	View view = inflater.inflate(R.layout.dialog_whatsnew, null);
        //	Builder builder = new AlertDialog.Builder(this);
        //	builder.setView(view).setTitle("What's New")
        //	.setPositiveButton("OK", new DialogInterface.OnClickListener() {
        //		@Override
        //		public void onClick(DialogInterface dialog, int which) {
        //		dialog.dismiss();
        //		}
        //	});
        //	builder.create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case TAKE_PICTURE: {
                if (resultCode == RESULT_OK) {
                    Log.d(WishList.LOG_TAG, "TAKE_PICTURE: RESULT_OK");
                    _fullsizePhotoPath = String.valueOf(_newfullsizePhotoPath);
                    _newfullsizePhotoPath = null;
                    Intent i = new Intent(getApplicationContext(), EditItem.class);
                    i.putExtra("fullsizePhotoPath", _fullsizePhotoPath);
                    startActivityForResult (i, EDIT_ITEM);
                    //	startActivityForResult(new Intent(getApplicationContext(), EditItemInfo.class), EDIT_ITEM);

                }
                else {
                    Log.d(WishList.LOG_TAG, "TAKE_PICTURE: not RESULT_OK");
                }
                break;
            }
            case EDIT_ITEM: {
                if (resultCode == RESULT_OK) {
                    // Create an intent to show the item detail.
                    // Pass the item_id along so the next activity can use it to
                    // retrieve the info. about the item from database
                    long id = -1;
                    if (data != null) {
                        id = data.getLongExtra("itemID", -1);
                    }

                    if (id != -1) {
//						finish();
                        Intent i = new Intent(DashBoard.this, WishItemDetail.class);
                        i.putExtra("item_id", id);
                        startActivity(i);
                    }
                }
                else {

                }
            }
        }//switch
    }

    //this will make the photo taken before to show up if user cancels taking a second photo
    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        Log.d(WishList.LOG_TAG, "");
        savedInstanceState.putString("newfullsizePhotoPath", _newfullsizePhotoPath);
        savedInstanceState.putString("fullsizePhotoPath", _fullsizePhotoPath);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // restore the current selected item in the list
        if (savedInstanceState != null) {
            Log.d(WishList.LOG_TAG, "savedInstanceState != null");
            _newfullsizePhotoPath = savedInstanceState.getString("newfullsizePhotoPath");
            _fullsizePhotoPath = savedInstanceState.getString("fullsizePhotoPath");
        }
        else {
            Log.d(WishList.LOG_TAG, "savedInstanceState == null");
        }
    }

    private void setUpActionBar() {
        // Make sure we're running on Honeycomb or higher to use ActionBar APIs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            findViewById(R.id.dashboard_header).setVisibility(View.GONE);
        }
        else {
            // we use the header instead of action bar for GingerBread and lower
            findViewById(R.id.dashboard_header).findViewById(R.id.imageView_logo).setVisibility(View.VISIBLE);
        }
    }
}

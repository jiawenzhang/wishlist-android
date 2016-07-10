package com.wish.wishlist.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.LogOutCallback;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseUser;
import com.parse.RequestPasswordResetCallback;
import com.parse.SaveCallback;
import com.path.android.jobqueue.JobManager;
import com.squareup.otto.Subscribe;
import com.wish.wishlist.R;
import com.wish.wishlist.WishlistApplication;
import com.wish.wishlist.db.ItemDBManager;
import com.wish.wishlist.event.ProfileChangeEvent;
import com.wish.wishlist.fragment.EmailFragmentDialog;
import com.wish.wishlist.fragment.NameFragmentDialog;
import com.wish.wishlist.job.UploadProfileImageJob;
import com.wish.wishlist.event.EventBus;
import com.wish.wishlist.sync.SyncAgent;
import com.wish.wishlist.util.Analytics;
import com.wish.wishlist.util.ImagePicker;
import com.wish.wishlist.util.NetworkHelper;
import com.wish.wishlist.util.Options;
import com.wish.wishlist.util.ProfileUtil;
import com.wish.wishlist.util.ScreenOrientation;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import static com.wish.wishlist.R.style.AppCompatAlertDialogStyle;

public class ProfileActivity extends ActivityBase implements
        EmailFragmentDialog.onEmailChangedListener,
        NameFragmentDialog.onNameChangedListener {
    final static String TAG = "ProfileActivity";

    public static final String IMAGE_URI = "IMAGE_URI";
    private static final int CHOOSE_IMAGE = 1;
    private static final int PROFILE_IMAGE = 2;

    private TextView mNameTextView;
    private TextView mEmailTextView;
    private TextView mPasswordTextView;
    private ImageView mGeneratedProfileImageView;
    protected SwipeRefreshLayout mSwipeRefreshLayout;

    private ProgressDialog mProgressDialog = null;
    private ImagePicker mImagePicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile);
        setupActionBar(R.id.profile_toolbar);

        Analytics.sendScreen("Profile");

        // make toolbar transparent
        mToolbar.setBackgroundResource(android.R.color.transparent);

        final ParseUser user = ParseUser.getCurrentUser();
        if (user == null) {
            Log.d(TAG, "currentUser null ");
            return;
        }

        // listen for ProfileChange event, triggered by push or swipe down to sync
        EventBus.getInstance().register(this);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        // the refresh listener. this would be called when the layout is pulled down
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Analytics.send(Analytics.SYNC, "RefreshProfile", null);
                if (!NetworkHelper.getInstance().isNetworkAvailable()) {
                    Toast.makeText(ProfileActivity.this, "Check network", Toast.LENGTH_LONG).show();
                    mSwipeRefreshLayout.setRefreshing(false);
                    return;
                }
                Log.d(TAG, "refresh");
                // we immediately stop spinning instead of waiting for sync to finish
                // so that if there is sync error, we won't end up having an infinite spinning
                mSwipeRefreshLayout.setRefreshing(false);
                SyncAgent.getInstance().updateProfileFromParse();
            }
        });

        final ScrollView scrollView = (ScrollView) findViewById(R.id.profile_scrollview);
        scrollView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                int scrollY = scrollView.getScrollY(); //for verticalScrollView
                // only enable swipe refresh when have scrolled to the top
                mSwipeRefreshLayout.setEnabled(scrollY == 0);
            }
        });

        ProfileUtil.downloadProfileImageIfNotExists();

        ImageView profileImage = (ImageView) findViewById(R.id.profile_image);
        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startImagePicker();
            }
        });

        mGeneratedProfileImageView = (ImageView) findViewById(R.id.generated_profile_image);
        mGeneratedProfileImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startImagePicker();
            }
        });

        setProfileImage();

        formatWishCount(ItemDBManager.getItemsCount(), (TextView) findViewById(R.id.wish_count));
        formatWishComplete(ItemDBManager.getCompletedItemsCount(), (TextView) findViewById(R.id.completed_count));
        formatWishValue(ItemDBManager.getTotalValue(), (TextView) findViewById(R.id.wish_value));

        FrameLayout profile_username = (FrameLayout) findViewById(R.id.profile_username);
        FrameLayout profile_name = (FrameLayout) findViewById(R.id.profile_name);
        FrameLayout profile_email = (FrameLayout) findViewById(R.id.profile_email);
        FrameLayout profile_change_password = (FrameLayout) findViewById(R.id.profile_change_password);

        if (ParseFacebookUtils.isLinked(user)) {
            Log.d(TAG, "user linked to facebook");
            // when user logs in via Facebook, username is an array of characters meaningless to display
            profile_username.setVisibility(View.GONE);
            // user logs in via Facebook does not have a password
            profile_change_password.setVisibility(View.GONE);
        } else {
            profile_username.setVisibility(View.VISIBLE);

            // user's email is username, don't allow user to change it
            // if user changes email, emailVerified will become false and user cannot login next time unless they
            // verify the email
            profile_email.setVisibility(View.GONE);
            ((TextView) profile_username.findViewById(R.id.title)).setText("Username");
            ((TextView) profile_username.findViewById(R.id.value)).setText(user.getUsername());
        }

        ((TextView) profile_name.findViewById(R.id.title)).setText("Name");
        mNameTextView = ((TextView) profile_name.findViewById(R.id.value));
        mNameTextView.setText(user.getString("name"));

        profile_name.setOnClickListener(new android.view.View.OnClickListener() {
            public void onClick(android.view.View v) {
                DialogFragment newFragment = new NameFragmentDialog();
                newFragment.show(getSupportFragmentManager(), "dialog");
            }
        });

        ((TextView) profile_email.findViewById(R.id.title)).setText("Email");
        mEmailTextView = ((TextView) profile_email.findViewById(R.id.value));
        mEmailTextView.setText(user.getEmail());

        profile_email.setOnClickListener(new android.view.View.OnClickListener() {
            public void onClick(android.view.View v) {
                DialogFragment newFragment = new EmailFragmentDialog();
                newFragment.show(getSupportFragmentManager(), "dialog");
            }
        });

        ((TextView) profile_change_password.findViewById(R.id.title)).setText("Change password");
        mPasswordTextView = ((TextView) profile_change_password.findViewById(R.id.value));
        mPasswordTextView.setVisibility(View.GONE);

        profile_change_password.setOnClickListener(new android.view.View.OnClickListener() {
            public void onClick(android.view.View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this, AppCompatAlertDialogStyle);
                String message = "An email will be sent to " + ParseUser.getCurrentUser().getUsername() + " for instructions to reset password";
                builder.setMessage(message);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ScreenOrientation.lock(ProfileActivity.this);
                        mProgressDialog = new ProgressDialog(ProfileActivity.this);
                        mProgressDialog.setMessage("Sending email...");
                        mProgressDialog.setCancelable(true);
                        mProgressDialog.setCanceledOnTouchOutside(false);
                        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                            public void onCancel(DialogInterface dialog) {
                                Toast.makeText(ProfileActivity.this, "Canceled, check network", Toast.LENGTH_LONG).show();
                            }
                        });
                        mProgressDialog.show();

                        ParseUser.requestPasswordResetInBackground(user.getUsername(), new RequestPasswordResetCallback() {
                            public void done(ParseException e) {
                                mProgressDialog.dismiss();
                                ScreenOrientation.unlock(ProfileActivity.this);
                                if (e == null) {
                                    Toast.makeText(ProfileActivity.this, "Email sent", Toast.LENGTH_LONG).show();
                                } else {
                                    Log.e(TAG, e.toString());
                                    Toast.makeText(ProfileActivity.this, "Fail to send email, check network", Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                        Analytics.send(Analytics.USER, "ResetPassword", "FromProfile");
                    }
                });
                builder.setNegativeButton("CANCEL",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

                AlertDialog dialog;
                dialog = builder.create();
                dialog.show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getInstance().unregister(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if (ParseUser.getCurrentUser() != null) {
            getMenuInflater().inflate(R.menu.menu_profile, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_profile_logout:
                AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
                builder.setMessage("Are you sure to logout?");
                builder.setCancelable(false);
                builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Analytics.send(Analytics.APP, "Logout", null);

                        mProgressDialog = new ProgressDialog(ProfileActivity.this);
                        mProgressDialog.setMessage("Logging out...");
                        mProgressDialog.setCancelable(false);
                        mProgressDialog.show();
                        ParseUser.logOutInBackground(new LogOutCallback() {
                            @Override
                            public void done(ParseException e) {
                                if (e == null) {
                                    // logout successful
                                    // show login on next app startup
                                    Options.ShowLoginOnStartup showLoginOption = new Options.ShowLoginOnStartup(1);
                                    showLoginOption.save();

                                    // re-launch the app
                                    WishlistApplication.restart();
                                } else {
                                    Toast.makeText(ProfileActivity.this, "Fail to logout, check network?", Toast.LENGTH_LONG).show();
                                    Analytics.send(Analytics.DEBUG, "LogoutFailed", e.getMessage());
                                }
                                mProgressDialog.dismiss();
                            }
                        });
                        Analytics.send(Analytics.USER, "Logout", null);
                    }
                });

                builder.setNegativeButton("CANCEL",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

                AlertDialog dialog;
                dialog = builder.create();
                dialog.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startCropperActivity(Uri picUri) {
        Intent intent = new Intent(this, CropperActivity.class);
        intent.putExtra(IMAGE_URI, picUri.toString());
        startActivityForResult(intent, PROFILE_IMAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (mImagePicker != null) {
            mImagePicker.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PROFILE_IMAGE: {
                if (resultCode == Activity.RESULT_OK) {
                    saveProfileImageToParse();
                }
                break;
            }
            case ImagePicker.TAKE_PICTURE: {
                ScreenOrientation.unlock(this);
                if (resultCode == Activity.RESULT_OK) {
                    startCropperActivity(mImagePicker.getPhotoUri());
                }
                break;
            }
            case ImagePicker.SELECT_PICTURE: {
                if (resultCode == Activity.RESULT_OK) {
                    startCropperActivity(data.getData());
                }
                break;
            }
        }
    }

    private void startImagePicker() {
        if (mImagePicker == null) {
            mImagePicker = new ImagePicker(this);
        }
        mImagePicker.start();
    }

    private void setProfileImage() {
        Bitmap bitmap = ProfileUtil.profileImageBitmap();
        ImageView profileImageView = (ImageView) findViewById(R.id.profile_image);
        if (bitmap != null) {
            profileImageView.setImageBitmap(bitmap);
            profileImageView.setVisibility(View.VISIBLE);
            mGeneratedProfileImageView.setVisibility(View.GONE);
            return;
        }

        Drawable generatedProfileImage = ProfileUtil.generateProfileImage();
        if (generatedProfileImage != null) {
            mGeneratedProfileImageView.setImageDrawable(generatedProfileImage);
            mGeneratedProfileImageView.setVisibility(View.VISIBLE);
            profileImageView.setVisibility(View.GONE);
        }
    }

    private void saveProfileImageToParse () {
        JobManager jobManager = ((WishlistApplication) getApplication()).getJobManager();
        jobManager.addJobInBackground(new UploadProfileImageJob());
        setProfileImage();
    }

    /**
     * Create a chooser intent to select the source to get image from.<br/>
     * The source can be camera's (ACTION_IMAGE_CAPTURE) or gallery's (ACTION_GET_CONTENT).<br/>
     * All possible sources are added to the intent chooser.
     */
    public Intent getPickImageChooserIntent() {

        // Determine Uri of camera image to save.
        Uri outputFileUri = getCaptureImageOutputUri();

        List<Intent> allIntents = new ArrayList<>();
        PackageManager packageManager = getPackageManager();

        // collect all camera intents
        Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
        for (ResolveInfo res : listCam) {
            Intent intent = new Intent(captureIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(res.activityInfo.packageName);
            if (outputFileUri != null) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
            }
            allIntents.add(intent);
        }

        // collect all gallery intents
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        List<ResolveInfo> listGallery = packageManager.queryIntentActivities(galleryIntent, 0);
        for (ResolveInfo res : listGallery) {
            Intent intent = new Intent(galleryIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(res.activityInfo.packageName);
            allIntents.add(intent);
        }

        // the main intent is the last in the list (fucking android) so pickup the useless one
        Intent mainIntent = allIntents.get(allIntents.size() - 1);
        for (Intent intent : allIntents) {
            if (intent.getComponent().getClassName().equals("com.android.documentsui.DocumentsActivity")) {
                mainIntent = intent;
                break;
            }
        }
        allIntents.remove(mainIntent);

        // Create a chooser from the main intent
        Intent chooserIntent = Intent.createChooser(mainIntent, "Change profile photo");

        // Add all other intents
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, allIntents.toArray(new Parcelable[allIntents.size()]));

        return chooserIntent;
    }

    /**
     * Get URI to image received from capture by camera.
     */
    private Uri getCaptureImageOutputUri() {
        Uri outputFileUri = null;
        File getImage = getExternalCacheDir();
        if (getImage != null) {
            outputFileUri = Uri.fromFile(new File(getImage.getPath(), "pickImageResult.jpeg"));
        }
        return outputFileUri;
    }

    /**
     * Get the URI of the selected image from {@link #getPickImageChooserIntent()}.<br/>
     * Will return the correct URI for camera and gallery image.
     *
     * @param data the returned data of the activity result
     */
    public Uri getPickImageResultUri(Intent data) {
        boolean isCamera = true;
        if (data != null) {
            String action = data.getAction();
            isCamera = action != null && action.equals(MediaStore.ACTION_IMAGE_CAPTURE);
        }
        return isCamera ? getCaptureImageOutputUri() : data.getData();
    }

    @Override
    public void onNameChanged(final String name) {
        final ParseUser user = ParseUser.getCurrentUser();
        final String oldName = user.getString("name");
        if (name.equals(oldName)) {
            Log.d(TAG, "same name, do nothing");
            return;
        }

        if (!NetworkHelper.getInstance().isNetworkAvailable()) {
            Toast.makeText(ProfileActivity.this, "Failed, check network", Toast.LENGTH_LONG).show();
            return;
        }

        Log.d(TAG, "name changed to: " + name);
        user.put("name", name);

        showProgressDialog("Updating name...");
        user.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Log.d(TAG, "name saved success");
                    mNameTextView.setText(name);
                    EventBus.getInstance().post(new ProfileChangeEvent(ProfileChangeEvent.ProfileChangeType.name));
                } else {
                    Log.e(TAG, "name save error " + e.toString());
                    user.put("name", oldName);
                    Toast.makeText(ProfileActivity.this, "Failed, check network", Toast.LENGTH_LONG).show();
                    Analytics.send(Analytics.DEBUG, "SaveUserNameFail", e.getMessage());
                }
                dismissProgressDialog();
            }
        });
    }

    @Override
    public void onEmailChanged(final String email) {
        final ParseUser user = ParseUser.getCurrentUser();
        final String oldEmail = user.getEmail();
        if (email.equals(oldEmail)) {
            Log.d(TAG, "same email, do nothing");
            return;
        }

        if (!NetworkHelper.getInstance().isNetworkAvailable()) {
            Toast.makeText(ProfileActivity.this, "Failed, check network", Toast.LENGTH_LONG).show();
            return;
        }

        Log.d(TAG, "email changed to: " + email);
        user.setEmail(email);

        showProgressDialog("Updating email...");
        user.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Log.d(TAG, "email saved success");
                    mEmailTextView.setText(email);
                    EventBus.getInstance().post(new ProfileChangeEvent(ProfileChangeEvent.ProfileChangeType.email));
                } else {
                    Log.e(TAG, "email save error " + e.toString());
                    user.setEmail(oldEmail);
                    if (e.getCode() == ParseException.EMAIL_TAKEN) {
                        Toast.makeText(ProfileActivity.this, "Failed, email already taken", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(ProfileActivity.this, "Failed, check network", Toast.LENGTH_LONG).show();
                    }
                    Analytics.send(Analytics.DEBUG, "SaveUserEmailFail", e.getMessage());
                }
                dismissProgressDialog();
            }
        });
    }

    @Subscribe
    public void profileChanged(ProfileChangeEvent event) {
        Log.d(TAG, "profileChanged " + event.type.toString());
        ParseUser user = ParseUser.getCurrentUser();
        if (event.type == ProfileChangeEvent.ProfileChangeType.email) {
            mEmailTextView.setText(user.getEmail());
        } else if (event.type == ProfileChangeEvent.ProfileChangeType.name) {
            mNameTextView.setText(user.getString("name"));
            if (mGeneratedProfileImageView.getVisibility() == View.VISIBLE) {
                // re-generate default profile image because it is based on name
                setProfileImage();
            }
        } else if (event.type == ProfileChangeEvent.ProfileChangeType.image) {
            setProfileImage();
        } else if (event.type == ProfileChangeEvent.ProfileChangeType.all) {
            mEmailTextView.setText(user.getEmail());
            mNameTextView.setText(user.getString("name"));
            setProfileImage();
        }
    }

    private void showProgressDialog(final String text) {
        ScreenOrientation.lock(this);
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(text);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    private void dismissProgressDialog() {
        mProgressDialog.dismiss();
        ScreenOrientation.unlock(this);
    }

    public static void formatWishCount (int count, TextView view) {
        String txt = count > 1 ? "Wishes" : "Wish";
        view.setText(count + "\n" + txt);
    }

    public static void formatWishComplete(int completedCount, TextView view) {
        view.setText(completedCount + "\nCompleted");
    }

    public static void formatWishValue(double value, TextView view) {
        String currency = PreferenceManager.getDefaultSharedPreferences(WishlistApplication.getAppContext()).getString("currency", "");
        if (currency.isEmpty()) {
            currency = "Value";
        }
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(0);
        nf.setMinimumFractionDigits(0);
        view.setText(nf.format(value) + "\n" + currency);
    }

}

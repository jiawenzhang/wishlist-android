package com.wish.wishlist.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.app.DialogFragment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.parse.ParseFacebookUtils;
import com.parse.ParseUser;
import com.path.android.jobqueue.JobManager;
import com.wish.wishlist.R;
import com.wish.wishlist.WishlistApplication;
import com.wish.wishlist.fragment.EmailFragmentDialog;
import com.wish.wishlist.fragment.NameFragmentDialog;
import com.wish.wishlist.job.UploadProfileImageJob;
import com.wish.wishlist.util.ImageManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Profile extends Activity implements
        EmailFragmentDialog.onEmailChangedListener,
        NameFragmentDialog.onNameChangedListener {
    final static String TAG = "Profile";
    private ParseUser mUser;

    public static final String IMAGE_URI = "IMAGE_URI";
    private static final int CHOOSE_IMAGE = 1;
    private static final int PROFILE_IMAGE = 2;

    private TextView mEmailTextView;
    private TextView mNameTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile);

        setUpActionBar();

        mUser = ParseUser.getCurrentUser();
        if (mUser == null) {
            Log.d(TAG, "currentUser null ");
            return;
        }

        showProfileImage();
        ImageView profileImage = (ImageView) findViewById(R.id.profile_image);
        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(getPickImageChooserIntent(), CHOOSE_IMAGE);
            }
        });

        FrameLayout profile_username = (FrameLayout) findViewById(R.id.profile_username);
        FrameLayout profile_name = (FrameLayout) findViewById(R.id.profile_name);
        FrameLayout profile_email = (FrameLayout) findViewById(R.id.profile_email);

        if (ParseFacebookUtils.isLinked(mUser)) {
            Log.d(TAG, "user linked to facebook");
            // when user logs in via Facebook, username is an array of characters meaningless to display
            profile_username.setVisibility(View.GONE);
        } else {
            profile_username.setVisibility(View.VISIBLE);
            ((TextView) profile_username.findViewById(R.id.title)).setText("Username");
            ((TextView) profile_username.findViewById(R.id.value)).setText(mUser.getUsername());
        }

        ((TextView) profile_name.findViewById(R.id.title)).setText("Name");
        mNameTextView = ((TextView) profile_name.findViewById(R.id.value));
        mNameTextView.setText(mUser.getString("name"));

        profile_name.setOnClickListener(new android.view.View.OnClickListener() {
            public void onClick(android.view.View v) {
                DialogFragment newFragment = new NameFragmentDialog();
                newFragment.show(getFragmentManager(), "dialog");
            }
        });

        ((TextView) profile_email.findViewById(R.id.title)).setText("Email");
        mEmailTextView = ((TextView) profile_email.findViewById(R.id.value));
        mEmailTextView.setText(mUser.getEmail());

        profile_email.setOnClickListener(new android.view.View.OnClickListener() {
            public void onClick(android.view.View v) {
                DialogFragment newFragment = new EmailFragmentDialog();
                newFragment.show(getFragmentManager(), "dialog");
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if (mUser != null) {
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
                mUser.logOut();
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setUpActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case PROFILE_IMAGE: {
                    saveProfileImageToParse();
                    break;
                }
                case CHOOSE_IMAGE: {
                    Uri imageUri = getPickImageResultUri(data);
                    Intent intent = new Intent(getApplication(), Cropper.class);
                    intent.putExtra(IMAGE_URI, imageUri.toString());
                    startActivityForResult(intent, PROFILE_IMAGE);
                    break;
                }
            }
        }
    }

    public static String profileImageName() {
        ParseUser user = ParseUser.getCurrentUser();
        if (user == null) {
            return null;
        }
        return user.getObjectId() + "-profile-image.jpg";
    }

    private void showProfileImage() {
        File profileImageFile = new File(getFilesDir(), profileImageName());
        Bitmap bitmap = BitmapFactory.decodeFile(profileImageFile.getAbsolutePath());
        ImageView profileImageView = (ImageView) findViewById(R.id.profile_image);
        if (bitmap != null) {
            profileImageView.setImageBitmap(bitmap);
        } else {
            profileImageView.setImageResource(R.drawable.splash_logo);
        }
    }

    private void saveProfileImageToParse () {
        JobManager jobManager = ((WishlistApplication) getApplication()).getJobManager();
        jobManager.addJobInBackground(new UploadProfileImageJob());
        showProfileImage();
    }

    public static boolean saveProfileImageToFile(Bitmap bitmap) {
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bs);
        return saveProfileImageToFile(bs.toByteArray());
    }

    public static boolean saveProfileImageToFile(byte[] data) {
        File profileImageFile = new File(WishlistApplication.getAppContext().getFilesDir(), profileImageName());
        return ImageManager.saveByteToPath(data, profileImageFile.getAbsolutePath());
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
        Intent chooserIntent = Intent.createChooser(mainIntent, "Select source");

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
    public void onNameChanged(String name) {
        Log.d(TAG, "name changed to: " + name);
        if (!name.equals(mUser.getEmail())) {
            mUser.put("name", name);
            mUser.saveEventually();
            mNameTextView.setText(name);
        }
    }

    @Override
    public void onEmailChanged(String email) {
        Log.d(TAG, "email changed to: " + email);
        if (!email.equals(mUser.getString("name"))) {
            mUser.setEmail(email);
            mUser.saveEventually();
            mEmailTextView.setText(email);
        }
    }
}

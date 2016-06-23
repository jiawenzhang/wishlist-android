package com.wish.wishlist.wish;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import com.wish.wishlist.R;
import com.wish.wishlist.util.Analytics;
import com.wish.wishlist.util.Link;

import java.util.ArrayList;


/**
 * Created by jiawen on 2016-06-19.
 */
public class AddWishFromActionActivity extends AddWishFromLinkActivity {
    final static String TAG = "AddWishFromAction";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (savedInstanceState == null) {
            if (Intent.ACTION_SEND.equals(action) && type != null) {
                getWindow().setSoftInputMode(WindowManager.
                        LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                if ("*/*".equals(type)) {
                    handleSendAll(intent);
                } else if (type.startsWith("text/")) {
                    handleSendText(intent); // Handle text being sent
                } else if (type.startsWith("image/")) {
                    handleSendImage(intent); // Handle single image being sent
                }
            } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
                if (type.startsWith("image/")) {
                    handleSendMultipleImages(intent); // Handle multiple images being sent
                }
            }
        }
    }

    private void handleSendAll(Intent intent) {
        Log.d(TAG, "handleSendAll");
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            mNameView.setText(sharedText);
        }

        mSelectedPicUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (mSelectedPicUri != null) {
            showSelectedImage();
            mHost = mSelectedPicUri.getHost();
            if (mHost != null) {
                Log.d(TAG, "host " + mHost);
                Analytics.send(Analytics.WISH, "ShareFrom_All", mHost);
            }
        }
    }

    private void handleSendImage(Intent intent) {
        Log.d(TAG, "handleSendImage");
        mSelectedPicUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (mSelectedPicUri != null) {
            showSelectedImage();
            mHost = mSelectedPicUri.getHost();
            if (mHost != null) {
                Analytics.send(Analytics.WISH, "ShareFrom_Image", mHost);
            }
        }
    }

    private void handleSendMultipleImages(Intent intent) {
        Log.d(TAG, "handleSendMultipleImages");
        ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (imageUris != null) {
            // Update UI to reflect multiple images being shared
        }

        Analytics.send(Analytics.WISH, "ShareFrom_MultipleImage", null);
    }

    private void handleSendText(Intent intent) {
        Log.d(TAG, "handleSendText");
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText == null) {
            return;
        }

        Log.d(TAG, "shared text: " + sharedText);
        ArrayList<String> links = Link.extract(sharedText);
        if (links.isEmpty()) {
            mNameView.setText(sharedText);
            return;
        }

        // remove the link from the text;
        String name = sharedText.replace(links.get(0), "");
        mNameView.setText(name);

        handleLinks(links, "ShareFrom_Text");
    }

    @Override
    protected void saveWishItem() {
        if (mWebBitmap != null) {
            saveWishWithWebBitmap();
        } else if (mSelectedPicUri != null) {
            // image from uri
            showProgressDialog(getString(R.string.saving_image));
            new saveSelectedPhotoTask().execute();
        } else {
            saveWishWithNoImage();
        }
    }
}

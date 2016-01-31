package com.wish.wishlist.wish;

import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.wish.wishlist.WishlistApplication;
import com.wish.wishlist.activity.WebImage;
import com.wish.wishlist.fragment.WebImageFragmentDialog;
import com.wish.wishlist.image.ImageManager;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.util.GetWebItemTask;
import com.wish.wishlist.util.WebRequest;
import com.wish.wishlist.util.WebResult;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Observer;
import java.util.regex.Matcher;

public class AddWishFromActionActivity extends AddWishActivity
        implements Observer,
        WebImageFragmentDialog.OnWebImageSelectedListener,
        WebImageFragmentDialog.OnLoadMoreFromWebViewListener,
        WebImageFragmentDialog.OnLoadMoreSelectedListener,
        WebImageFragmentDialog.OnWebImageCancelledListener,
        GetWebItemTask.OnWebResult {

    private String mWebPicUrl = null;
    private Bitmap mWebBitmap = null;
    private String mLink;
    private String mHost = null;
    private ProgressDialog mProgressDialog = null;
    private GetWebItemTask mGetWebItemTask = null;
    private WebView mWebView = null;
    private static WebResult mWebResult = null;
    private static final String TAG = "AddWishActionAct";
    private long mStartTime;
    private class MyJavaScriptInterface {
        private Context ctx;

        MyJavaScriptInterface(Context ctx) {
            this.ctx = ctx;
        }

        @JavascriptInterface
        public void gotHTML(String html) {
            Log.d(TAG, "gotHTML");
            long time = System.currentTimeMillis() - mStartTime;
            Log.d(TAG, "webview show HTML took " + time + " ms");
            ((AddWishFromActionActivity) ctx).loadImagesFromHtml(html);
        }
    }

    public static final String WEB_PIC_URL = "WEB_PIC_URL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCameraImageButton.setVisibility(View.GONE);

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

        if (intent.getStringExtra(WEB_PIC_URL) != null) {
            mWebPicUrl = intent.getStringExtra(WEB_PIC_URL);
            setWebPic(mWebPicUrl);
        }
    }

    //this will make the photo taken before to show up if user cancels taking a second photo
    //this will also save the thumbnail on switching screen orientation
    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        Log.d(TAG, "onSaveInstanceState");
        if (mWebPicUrl != null) {
            savedInstanceState.putString(WEB_PIC_URL, mWebPicUrl);
            Log.d(TAG, "mWebPicUrl " + mWebPicUrl);
        }
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // restore the current selected item in the list
        if (savedInstanceState != null) {
            if (savedInstanceState.getString(WEB_PIC_URL) != null) {
                mWebPicUrl = savedInstanceState.getString(WEB_PIC_URL);
                setWebPic(mWebPicUrl);
            }
        }
    }

    private void handleSendAll(Intent intent) {
        Log.d(TAG, "handleSendAll");
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            mNameEditText.setText(sharedText);
        }
        mSelectedPicUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        setSelectedPic();

        if (mSelectedPicUri != null) {
            mHost = mSelectedPicUri.getHost();
            Log.d(TAG, "host " + mHost);

            if (mHost == null) {
                return;
            }

            Tracker t = ((WishlistApplication) getApplication()).getTracker(WishlistApplication.TrackerName.APP_TRACKER);
            t.send(new HitBuilders.EventBuilder()
                    .setCategory("Wish")
                    .setAction("ShareFrom_All")
                    .setLabel(mHost)
                    .build());
        }
    }

    private void handleSendText(Intent intent) {
        Log.d(TAG, "handleSendText");
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            Log.d(TAG, "shared text: " + sharedText);
            ArrayList<String> links = extractLinks(sharedText);
            if (links.isEmpty()) {
                mNameEditText.setText(sharedText);
                return;
            }

            mHost = null;
            for (String link : links) {
                try {
                    URL url = new URL(link);
                    mLink = link;
                    mHost = url.getHost();

                    String store = mHost.startsWith("www.") ? mHost.substring(4) : mHost;
                    mStoreEditText.setText(store);
                    break;
                } catch (MalformedURLException e) {
                    Log.d(TAG, e.toString());
                }
            }

            if (mLink == null) {
                mNameEditText.setText(sharedText);
                return;
            }

            // remove the link from the text;
            String name = sharedText.replace(mLink, "");
            mNameEditText.setText(name);

            if (mHost != null && mHost.equals("pages.ebay.com")) {
                String redirected_link = getEbayLink(mLink);
                if (redirected_link != null) {
                    mLink = redirected_link;
                }
            }
            Log.d(TAG, "extracted link: " + mLink);

            Tracker t = ((WishlistApplication) getApplication()).getTracker(WishlistApplication.TrackerName.APP_TRACKER);
            if (mHost != null) {
                t.send(new HitBuilders.EventBuilder()
                        .setCategory("Wish")
                        .setAction("ShareFrom_Text")
                        .setLabel(mHost)
                        .build());
            }

            mLinkEditText.setText(mLink);
            mLinkEditText.setEnabled(false);

            WebRequest request = new WebRequest();
            request.url = mLink;
            request.getAllImages = false;
            lockScreenOrientation();

            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage("Loading images");
            mProgressDialog.setCancelable(true);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    Log.d(TAG, "onCancel");

                    Tracker t = ((WishlistApplication) getApplication()).getTracker(WishlistApplication.TrackerName.APP_TRACKER);
                    t.send(new HitBuilders.EventBuilder()
                            .setCategory("Wish")
                            .setAction("CancelLoadingImages")
                            .setLabel(mLink)
                            .build());

                    if (mGetWebItemTask != null) {
                        mGetWebItemTask.cancel(true);
                    }
                    if (mWebView != null) {
                        mWebView.stopLoading();
                        mWebView.destroy();
                        Log.d(TAG, "stopped loading webview");
                        if (!mWebResult._webImages.isEmpty()) {
                            showImageDialog(true);
                        }
                    }
                }
            });

            mProgressDialog.show();
            mGetWebItemTask = new GetWebItemTask(this, this);
            mGetWebItemTask.execute(request);
        }
    }

    private void getGeneratedHtml() {
        mWebView = new WebView(AddWishFromActionActivity.this);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.addJavascriptInterface(new MyJavaScriptInterface(this), "HtmlViewer");
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                        /* This call inject JavaScript into the page which just finished loading. */
                Log.d(TAG, "onPageFinished");
                mWebView.loadUrl("javascript:window.HtmlViewer.gotHTML" +
                        "('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');");

            }
            @Override
            public void onReceivedError(WebView view, int errorCode,
                                        String description, String failingUrl) {
                Log.e(TAG, "onReceivedError " + description);
                super.onReceivedError(view, errorCode, description, failingUrl);
            }
        });

        mWebView.loadUrl(mLink);
    }

    private void handleSendImage(Intent intent) {
        Log.d(TAG, "handleSendImage");
        mSelectedPicUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        setSelectedPic();
        if (mSelectedPicUri != null) {
            mHost= mSelectedPicUri.getHost();
            if (mHost == null) {
                return;
            }

            Log.d(TAG, "host " + mSelectedPicUri.getHost());

            Tracker t = ((WishlistApplication) getApplication()).getTracker(WishlistApplication.TrackerName.APP_TRACKER);
            t.send(new HitBuilders.EventBuilder()
                    .setCategory("Wish")
                    .setAction("ShareFrom_Image")
                    .setLabel(mHost)
                    .build());
        }
    }

    private void handleSendMultipleImages(Intent intent) {
        Log.d(TAG, "handleSendMultipleImages");
        ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (imageUris != null) {
            // Update UI to reflect multiple images being shared
        }

        Tracker t = ((WishlistApplication) getApplication()).getTracker(WishlistApplication.TrackerName.APP_TRACKER);
        t.send(new HitBuilders.EventBuilder()
                .setCategory("Wish")
                .setAction("ShareFrom_MultipleImage")
                .build());
    }

    private String getEbayLink(String link) {
        // ebay app will send us link like:
        // http://pages.ebay.com/link/?nav=item.view&id=201331161611&alt=web

        // If we open this link, the site will be redirected to a new link by javascript
        // and the product information are all stored in the new link
        // http://www.ebay.com/itm/201331161611

        // So what we do is to convert the given link to the redirected link

        // Retrieve the product id from the given link
        String id = null;
        for (NameValuePair nvp : URLEncodedUtils.parse(URI.create(link), "UTF-8")) {
            if ("id".equals(nvp.getName())) {
                id = nvp.getValue();
            }
        }

        if (id == null) {
            return null;
        }

        // Construct the redirected link
        return  "http://www.ebay.com/itm/" + id;
    }

    private ArrayList<String> extractLinks(String text) {
        ArrayList<String> links = new ArrayList<String>();
        Matcher m = Patterns.WEB_URL.matcher(text);
        while (m.find()) {
            String url = m.group();
            links.add(url);
        }
        return links;
    }

    private Boolean setWebPic(String url) {
        Log.d(TAG, "setWebPic " + url);
        final Target target = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                mWebBitmap = ImageManager.getScaleDownBitmap(bitmap, 1024);
                mImageItem.setImageBitmap(mWebBitmap);
                mImageItem.setVisibility(View.VISIBLE);
            }
            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {}

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {}
        };
        mImageItem.setTag(target);

        Picasso.with(this).load(url).into(target);
        mFullsizePhotoPath = null;
        mSelectedPicUri = null;
        mSelectedPic = false;
        return true;
    }

    @Override
    protected boolean saveWishItem() {
        if (mWebBitmap != null) {
            mFullsizePhotoPath = ImageManager.saveBitmapToAlbum(mWebBitmap);
            ImageManager.saveBitmapToThumb(mWebBitmap, mFullsizePhotoPath);
        }

        // create a new item
        WishItem item = createNewWish();
        item.setWebImgMeta(mWebPicUrl, mWebBitmap.getWidth(), mWebBitmap.getHeight());
        mItem_id = item.saveToLocal();
        wishSaved();
        return true;
    }

    public void onWebImageSelected(int position) {
        // After the dialog fragment completes, it calls this callback.
        WebImage webImage = mWebResult._webImages.get(position);
        mWebPicUrl = webImage.mUrl;
        setWebPic(mWebPicUrl);
        unlockScreenOrientation();

        Tracker t = ((WishlistApplication) getApplication()).getTracker(WishlistApplication.TrackerName.APP_TRACKER);
        t.send(new HitBuilders.EventBuilder()
                .setCategory("Wish")
                .setAction("SelectWebImage`")
                .setLabel(mHost)
                .build());
    }

    public void onLoadMoreFromWebView() {
        Log.d(TAG, "onLoadMoreFromWebview");

        Tracker t = ((WishlistApplication) getApplication()).getTracker(WishlistApplication.TrackerName.APP_TRACKER);
        t.send(new HitBuilders.EventBuilder()
                .setCategory("Wish")
                .setAction("LoadMoreFromWebView")
                .setLabel(mLink)
                .build());

        mProgressDialog.show();
        getGeneratedHtml();
    }

    public void onWebImageCancelled() {
        Log.d(TAG, "onWebImageCancelled");
        unlockScreenOrientation();

        Tracker t = ((WishlistApplication) getApplication()).getTracker(WishlistApplication.TrackerName.APP_TRACKER);
        t.send(new HitBuilders.EventBuilder()
                .setCategory("Wish")
                .setAction("CancelWebImage")
                .setLabel(mLink)
                .build());
    }

    public void onLoadMoreFromStaticHtml() {
        Log.d(TAG, "onLoadMoreFromStaticHtml");
        lockScreenOrientation();
        mProgressDialog.show();

        Tracker t = ((WishlistApplication) getApplication()).getTracker(WishlistApplication.TrackerName.APP_TRACKER);
        t.send(new HitBuilders.EventBuilder()
                .setCategory("Wish")
                .setAction("LoadMoreFromStaticHtml")
                .setLabel(mLink)
                .build());

        if (mWebResult._attemptedAllFromJsoup) {
            getGeneratedHtml();
        } else {
            WebRequest request = new WebRequest();
            request.url = mLink;
            request.getAllImages = true;
            mGetWebItemTask = new GetWebItemTask(this, this);
            mGetWebItemTask.execute(request);
        }
    }

    public void loadImagesFromHtml(String html) {
        Log.d(TAG, "loadImagesFromHtml");
        WebRequest request = new WebRequest();
        request.url = mLink;
        request.getAllImages = true;
        request.html = html;
        mGetWebItemTask = new GetWebItemTask(this, this);
        mGetWebItemTask.execute(request);
    }


    private void lockScreenOrientation() {
        int currentOrientation = getResources().getConfiguration().orientation;
        if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    private void unlockScreenOrientation() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    }

    private void loadPartialImage(String src) {
        try {
            Log.d(TAG, "Downloading image: " + src);
            URL imageUrl = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) imageUrl.openConnection();
            connection.setRequestProperty("Range", "bytes=0-168");
            Log.d(TAG, "content length " + connection.getContentLength());
            int response = connection.getResponseCode();
            Log.d(TAG, "response code: " + response);
            Log.d(TAG, "\n");

            InputStream is = connection.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(isr);
            try {
                String read = br.readLine();

                while(read != null){
                    sb.append(read);
                    read = br.readLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            Log.d(TAG, "IOException" + e.toString());
        }
    }

    @Override
    public void onWebResult(WebResult result)
    {
        Log.d(TAG, "onWebResult");
        if (result._webImages.isEmpty() && !result._attemptedDynamicHtml) {
            mStartTime = System.currentTimeMillis();
            getGeneratedHtml();
            return;
        }
        mProgressDialog.dismiss();
        if (result._title != null && !result._title.trim().isEmpty()) {
            mNameEditText.setText(result._title);
        }
        if (result._description != null && !result._description.trim().isEmpty()) {
            mDescriptionEditText.setText(result._description);
        }
        mWebResult = result;
        if (!mWebResult._webImages.isEmpty()) {
            Log.d(TAG, "Got " + result._webImages.size() + " images to choose from");
            showImageDialog(!result._attemptedDynamicHtml);
        } else {
            unlockScreenOrientation();
            Toast.makeText(this, "No image found", Toast.LENGTH_SHORT).show();

            Tracker t = ((WishlistApplication) getApplication()).getTracker(WishlistApplication.TrackerName.APP_TRACKER);
            t.send(new HitBuilders.EventBuilder()
                    .setCategory("Wish")
                    .setAction("NoImageFound")
                    .setLabel(mLink)
                    .build());
        }
    }

    private void showImageDialog(boolean allowLoadMore) {
        DialogFragment fragment = WebImageFragmentDialog.newInstance(mWebResult._webImages, allowLoadMore);
        final FragmentManager fm = getFragmentManager();
        Log.d(TAG, "fragment.show");
        fragment.show(fm, "dialog");
    }

    @Override
    protected boolean loadLocation() {
        return false;
    }
}

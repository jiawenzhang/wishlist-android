package com.wish.wishlist.wish;

import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.wish.wishlist.R;
import com.wish.wishlist.activity.FullscreenPhotoActivity;
import com.wish.wishlist.activity.WebImage;
import com.wish.wishlist.fragment.WebImageFragmentDialog;
import com.wish.wishlist.image.ImageManager;
import com.wish.wishlist.util.Analytics;
import com.wish.wishlist.util.GetWebItemTask;
import com.wish.wishlist.util.ScreenOrientation;
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
            // gotHTML could be called twice for some websites, usually the second time with more contents in html
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

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        mInstructionLayout.setVisibility(View.GONE);
        setPhotoVisible(false);
        mImageFrame.setVisibility(View.GONE);
        mLinkTextView.setVisibility(View.GONE);

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
        if (sharedText != null) {
            Log.d(TAG, "shared text: " + sharedText);
            ArrayList<String> links = extractLinks(sharedText);
            if (links.isEmpty()) {
                mNameView.setText(sharedText);
                return;
            }

            mHost = null;
            for (String link : links) {
                try {
                    URL url = new URL(link);
                    mLink = link;
                    mHost = url.getHost();

                    String store = mHost.startsWith("www.") ? mHost.substring(4) : mHost;
                    mStoreView.setText(store);
                    break;
                } catch (MalformedURLException e) {
                    Log.d(TAG, e.toString());
                }
            }

            if (mLink == null) {
                mNameView.setText(sharedText);
                return;
            }

            // remove the link from the text;
            String name = sharedText.replace(mLink, "");
            mNameView.setText(name);

            if (mHost != null && mHost.equals("pages.ebay.com")) {
                String redirected_link = getEbayLink(mLink);
                if (redirected_link != null) {
                    mLink = redirected_link;
                }
            }
            Log.d(TAG, "extracted link: " + mLink);

            if (mHost != null) {
                Analytics.send(Analytics.WISH, "ShareFrom_Text", mHost);
            }

            mLinkView.setText(mLink);
            mLinkView.setEnabled(false);

            WebRequest request = new WebRequest();
            request.url = mLink;
            request.getAllImages = false;
            ScreenOrientation.lock(this);

            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage("Loading images");
            mProgressDialog.setCancelable(true);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    Log.d(TAG, "onCancel");

                    Analytics.send(Analytics.WISH, "CancelLoadingImages", mLink);

                    if (mGetWebItemTask != null) {
                        mGetWebItemTask.cancel(true);
                    }
                    if (mWebView != null) {
                        mWebView.stopLoading();
                        mWebView.destroy();
                        Log.d(TAG, "stopped loading webview");
                        if (mWebResult != null && !mWebResult._webImages.isEmpty()) {
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
                /* This call inject JavaScript into the page which just finished loading.
                 * onPageFinished could be called twice for some websites, second time with different results*/
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

    private void showSelectedImage() {
        setSelectedPic();
        mImageFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFullScreenPhoto(FullscreenPhotoActivity.PHOTO_URI, mSelectedPicUri.toString());
            }
        });
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

    private Boolean setWebPic(final String url) {
        Log.d(TAG, "setWebPic " + url);
        final Target target = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                mWebBitmap = bitmap;
                mPhotoView.setImageBitmap(mWebBitmap);
                setPhotoVisible(true);
                mImageFrame.setVisibility(View.VISIBLE);
                mImageFrame.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showFullScreenPhoto(FullscreenPhotoActivity.PHOTO_URL, url);
                    }
                });
            }
            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {}

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {}
        };
        mPhotoView.setTag(target);

        Picasso.with(this).load(url).resize(ImageManager.IMG_WIDTH, 0).onlyScaleDown().into(target);
        mFullsizePhotoPath = null;
        mSelectedPicUri = null;
        mSelectedPic = false;
        return true;
    }

    @Override
    protected void saveWishItem() {
        if (mWebBitmap != null) {
            // image from web
            mFullsizePhotoPath = ImageManager.saveBitmapToFile(mWebBitmap);
            ImageManager.saveBitmapToThumb(mWebBitmap, mFullsizePhotoPath);

            mItem = createNewWish();

            ArrayList<ImgMeta> metaArray = new ArrayList<>();
            metaArray.add(new ImgMeta(ImgMeta.WEB, mWebPicUrl, mWebBitmap.getWidth(), mWebBitmap.getHeight()));
            mItem.setImgMetaArray(metaArray);
            mItem.saveToLocal();
            wishSaved();
        } else if (mSelectedPicUri != null) {
            // image from uri
            showProgressDialog(getString(R.string.saving_image));
            new saveSelectedPhotoTask().execute();
        } else {
            // no image
            mItem = createNewWish();
            mItem.setImgMetaArray(null);

            mItem.saveToLocal();
            wishSaved();
        }
    }

    public void onWebImageSelected(int position) {
        // After the dialog fragment completes, it calls this callback.
        WebImage webImage = mWebResult._webImages.get(position);
        mWebPicUrl = webImage.mUrl;
        setWebPic(mWebPicUrl);
        ScreenOrientation.unlock(this);

        Analytics.send(Analytics.WISH, "SelectWebImage", mHost);
    }

    public void onLoadMoreFromWebView() {
        Log.d(TAG, "onLoadMoreFromWebview");

        Analytics.send(Analytics.WISH, "LoadMoreFromWebView", mLink);

        mProgressDialog.show();
        getGeneratedHtml();
    }

    public void onWebImageCancelled() {
        Log.d(TAG, "onWebImageCancelled");
        ScreenOrientation.unlock(this);

        Analytics.send(Analytics.WISH, "CancelWebImage", mLink);
    }

    public void onLoadMoreFromStaticHtml() {
        Log.d(TAG, "onLoadMoreFromStaticHtml");
        ScreenOrientation.lock(this);
        mProgressDialog.show();

        Analytics.send(Analytics.WISH, "LoadMoreFromStaticHtml", mLink);

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
            mNameView.setText(result._title);
        }
        if (result._description != null && !result._description.trim().isEmpty()) {
            mDescriptionView.setText(result._description);
        }
        mWebResult = result;
        if (!mWebResult._webImages.isEmpty()) {
            Log.d(TAG, "Got " + result._webImages.size() + " images to choose from");
            showImageDialog(!result._attemptedDynamicHtml);
        } else {
            ScreenOrientation.unlock(this);
            Toast.makeText(this, "No image found", Toast.LENGTH_SHORT).show();

            Analytics.send(Analytics.WISH, "NoImageFound", mLink);
        }
    }

    private void showImageDialog(boolean allowLoadMore) {
        DialogFragment fragment = WebImageFragmentDialog.newInstance(mWebResult._webImages, allowLoadMore);
        FragmentManager fm = getSupportFragmentManager();
        Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            WebImageFragmentDialog df = (WebImageFragmentDialog) prev;
            df.reload(mWebResult._webImages, allowLoadMore);
            return;
        }
        Log.d(TAG, "fragment.show");
        fragment.show(fm, "dialog");
    }

    @Override
    protected boolean loadLocation() {
        return false;
    }
}

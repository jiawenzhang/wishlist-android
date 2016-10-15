package com.wish.wishlist.wish;

import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.wish.wishlist.activity.FullscreenPhotoActivity;
import com.wish.wishlist.activity.WebImage;
import com.wish.wishlist.fragment.WebImageFragmentDialog;
import com.wish.wishlist.image.ImageManager;
import com.wish.wishlist.util.Analytics;
import com.wish.wishlist.util.ImageDimensionTask;
import com.wish.wishlist.util.ScreenOrientation;
import com.wish.wishlist.util.WebItemTask;
import com.wish.wishlist.util.WebRequest;
import com.wish.wishlist.util.WebResult;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Observer;

public class AddWishFromLinkActivity extends AddWishActivity
        implements Observer,
        WebImageFragmentDialog.OnWebImageSelectedListener,
        WebImageFragmentDialog.OnLoadMoreSelectedListener,
        WebImageFragmentDialog.OnWebImageCancelledListener,
        WebItemTask.OnWebResult,
        ImageDimensionTask.OnImageDimension {

    private String mWebPicUrl = null;
    protected Bitmap mWebBitmap = null;
    private String mLink;
    protected String mHost = null;
    private ProgressDialog mProgressDialog = null;
    private WebItemTask mWebItemTask = null;
    private ImageDimensionTask mImageDimensionTask = null;
    private static WebResult mWebResult = null;
    private static final String TAG = "AddWishFromLink";
    private long mStartTime;

    public static final String WEB_PIC_URL = "WEB_PIC_URL";
    public static final String LINKS = "LINKS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mInstructionLayout.setVisibility(View.GONE);
        setPhotoVisible(false);
        mImageFrame.setVisibility(View.GONE);
        mLinkView.setVisibility(View.GONE);

        Intent intent = getIntent();
        ArrayList<String> links = intent.getStringArrayListExtra(LINKS);
        if (links != null && !links.isEmpty()) {
            if (savedInstanceState == null) {
                Log.d(TAG, "got links " + links.toString());
                handleLinks(links, "ShareFrom_Link");
            }
        }

        if (intent.getStringExtra(WEB_PIC_URL) != null) {
            mWebPicUrl = intent.getStringExtra(WEB_PIC_URL);
            setWebPic(mWebPicUrl);
        }
    }

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
        if (savedInstanceState != null) {
            if (savedInstanceState.getString(WEB_PIC_URL) != null) {
                mWebPicUrl = savedInstanceState.getString(WEB_PIC_URL);
                setWebPic(mWebPicUrl);
            }
        }
    }

    protected void handleLinks(ArrayList<String> links, String action) {
        mHost = null;

        // only cares about the valid first link
        for (String link : links) {
            try {
                URL url = new URL(link);
                mLink = link;
                mHost = url.getHost();

                String store = mHost.startsWith("www.") ? mHost.substring(4) : mHost;
                mStoreView.setText(store);
                break;
            } catch (MalformedURLException e) {
                Log.e(TAG, e.toString());
            }
        }

        if (mHost != null && mHost.equals("pages.ebay.com")) {
            String redirected_link = getEbayLink(mLink);
            if (redirected_link != null) {
                mLink = redirected_link;
            }
        }
        Log.d(TAG, "extracted link: " + mLink);

        if (mHost != null) {
            Analytics.send(Analytics.WISH, action, mHost);
        }

        mLinkText.setText(mLink);
        mLinkText.setEnabled(false);

        WebRequest request = new WebRequest();
        request.url = mLink;
        ScreenOrientation.lock(this);

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("Loading images");
        mProgressDialog.setCancelable(true);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                Log.d(TAG, "onCancel");

                Analytics.send(Analytics.WISH, "CancelLoadingImages", mLink);

                if (mWebItemTask != null) {
                    mWebItemTask.cancel();
                }
                if (mImageDimensionTask != null) {
                    Log.d(TAG, "cancel loading more images");
                    mImageDimensionTask.cancel(true);
                    if (mWebResult != null && !mWebResult.webImages.isEmpty()) {
                        showImageDialog(true);
                    }
                }
            }
        });

        mProgressDialog.show();

        mWebItemTask = new WebItemTask(this, request, this);
        mWebItemTask.run();
    }

    protected void showSelectedImage() {
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

        Picasso.with(this).load(url).resize(ImageManager.IMG_WIDTH, 2048).centerInside().onlyScaleDown().into(target);
        mFullsizePhotoPath = null;
        mSelectedPicUri = null;
        mSelectedPic = false;
        return true;
    }

    @Override
    protected void saveWishItem() {
        if (mWebBitmap != null) {
            saveWishWithWebBitmap();
        } else {
            saveWishWithNoImage();
        }
        Toast.makeText(this, "Wish saved", Toast.LENGTH_LONG).show();
    }

    protected void saveWishWithWebBitmap() {
        // image from web
        mFullsizePhotoPath = ImageManager.saveBitmapToFile(mWebBitmap);
        ImageManager.saveBitmapToThumb(mWebBitmap, mFullsizePhotoPath);

        mItem = createNewWish();

        ArrayList<ImgMeta> metaArray = new ArrayList<>();
        metaArray.add(new ImgMeta(ImgMeta.WEB, mWebPicUrl, mWebBitmap.getWidth(), mWebBitmap.getHeight()));
        mItem.setImgMetaArray(metaArray);
        mItem.saveToLocal();
        wishSaved();
    }

    protected void saveWishWithNoImage() {
        mItem = createNewWish();
        mItem.setImgMetaArray(null);

        mItem.saveToLocal();
        wishSaved();
    }

    public void onWebImageSelected(int position) {
        // After the dialog fragment completes, it calls this callback.
        WebImage webImage = mWebResult.webImages.get(position);
        mWebPicUrl = webImage.mUrl;
        setWebPic(mWebPicUrl);
        ScreenOrientation.unlock(this);

        Analytics.send(Analytics.WISH, "SelectWebImage", mHost);
        Analytics.send(Analytics.DEBUG, "SelectWebImage: " + mHost, position + "/" + mWebResult.webImages.size());
    }

    public void onWebImageCancelled() {
        Log.d(TAG, "onWebImageCancelled");
        ScreenOrientation.unlock(this);

        Analytics.send(Analytics.WISH, "CancelWebImage", mLink);
    }

    public void onLoadMoreImages() {
        Log.d(TAG, "onLoadMoreImages");
        //ScreenOrientation.lock(this);
        mProgressDialog.show();

        Analytics.send(Analytics.WISH, "LoadMoreImages", mLink);
        mImageDimensionTask = new ImageDimensionTask(this);
        mImageDimensionTask.execute(mWebResult.webImages);
    }

    @Override
    public void onWebResult(WebResult result) {
        Log.d(TAG, "onWebResult");
        mProgressDialog.dismiss();
        if (result.title != null && !result.title.trim().isEmpty()) {
            mNameView.setText(result.title);
        }
        if (result.description != null && !result.description.trim().isEmpty()) {
            mDescriptionView.setText(result.description);
        }

        mWebResult = result;
        if (!mWebResult.webImages.isEmpty()) {
            Log.d(TAG, "Got " + result.webImages.size() + " images to choose from");
            showImageDialog(true);
        } else {
            ScreenOrientation.unlock(this);
            Toast.makeText(this, "No image found", Toast.LENGTH_SHORT).show();

            Analytics.send(Analytics.WISH, "NoImageFound", mLink);
        }
    }

    @Override
    public void onImageDimension(ArrayList<WebImage> webImages) {
        Log.d(TAG, "onImageDimension");
        mWebResult.webImages = webImages;
        mProgressDialog.dismiss();
        showImageDialog(false);
    }

    private void showImageDialog(boolean showOneImage) {
        DialogFragment fragment = WebImageFragmentDialog.newInstance(mWebResult.webImages, showOneImage);
        FragmentManager fm = getSupportFragmentManager();
//        Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
//        if (prev != null) {
//            WebImageFragmentDialog df = (WebImageFragmentDialog) prev;
//            df.reload(mWebResult.webImages);
//            return;
//        }
        Log.d(TAG, "fragment.show");
        fragment.show(fm, "dialog");
    }

    @Override
    protected boolean loadLocation() {
        return false;
    }
}

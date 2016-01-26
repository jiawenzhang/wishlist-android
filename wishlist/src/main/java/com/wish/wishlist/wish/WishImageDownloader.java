package com.wish.wishlist.wish;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.util.Log;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.wish.wishlist.WishlistApplication;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.image.ImageManager;

import java.util.HashMap;
import java.util.List;

/**
 * Created by jiawen on 15-11-21.
 */
public class WishImageDownloader {
    /****************** WishImageDownloadDoneListener ************************/
    onWishImageDownloadDoneListener mWishImageDownloadDoneListener;
    public interface onWishImageDownloadDoneListener {
        void onWishImageDownloadDone(boolean success);
    }

    protected void onWishImageDownloadDone(boolean success) {
        if (mWishImageDownloadDoneListener != null) {
            mWishImageDownloadDoneListener.onWishImageDownloadDone(success);
        }
    }

    public void setWishImageDownloadDoneListener(onWishImageDownloadDoneListener l) {
        mWishImageDownloadDoneListener = l;
    }
    /**********************************************************************/


    private HashMap<String, Target> m_targets = new HashMap<>();
    private int mItemCount;
    private static String TAG = "WishImageDownloader";

    public WishImageDownloader() {}

    public void download(List<WishItem> items) {
        mItemCount = items.size();
        for (WishItem item : items) {
            download(item);
        }
    }

    private void download(final WishItem item) {
        final WebImgMeta webImgMeta = item.getWebImgMeta();
        final WebImgMeta parseImgMeta = item.getParseImgMeta();
        if (webImgMeta == null && parseImgMeta == null) {
            // wish does not have a picture
            bitmapLoaded(null, null, item);
            return;
        }

        final String picURL;
        if (webImgMeta != null) {
            // try to download pic from internet first
            picURL = webImgMeta.mUrl;
        } else {
            picURL = parseImgMeta.mUrl;
        }

        Target target = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                if (bitmap != null) {
                    Log.d(TAG, "downloadWishImage::onBitmapLoaded valid bitmap");
                } else {
                    Log.e(TAG, "downloadWishImage::onBitmapLoaded null bitmap");
                }
                bitmapLoaded(bitmap, picURL, item);
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {}

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
                bitmapFailed(picURL);
            }
        };

        m_targets.put(picURL, target);
        Picasso.with(WishlistApplication.getAppContext()).load(picURL).into(target);
    }

    private void bitmapFailed(final String picUrl) {
        m_targets.remove(picUrl);
        --mItemCount;
        Log.d(TAG, "download wish image fail");
        onWishImageDownloadDone(false);
    }

    private void bitmapLoaded(final Bitmap bitmap, final String picUrl, WishItem item) {
        m_targets.remove(picUrl);
        String fullsizePath = null;
        if (bitmap != null) {
            // save the bitmap and a thumbnail as a local file
            fullsizePath = ImageManager.saveBitmapToAlbum(ImageManager.getScaleDownBitmap(bitmap, 1024));
            ImageManager.saveBitmapToThumb(bitmap, fullsizePath);
        }

        item.setObjectId("");

        final boolean wishDefaultPrivate = PreferenceManager.getDefaultSharedPreferences(WishlistApplication.getAppContext()).getBoolean("wishDefaultPrivate", false);
        if (wishDefaultPrivate) {
            item.setAccess(WishItem.PRIVATE);
        } else {
            item.setAccess(WishItem.PUBLIC);
        }
        item.setComplete(0);
        item.setSyncedToServer(false);

        item.setFullsizePicPath(fullsizePath);

        item.setUpdatedTime(System.currentTimeMillis());
        item.saveToLocal();
        Log.d(TAG, "wish " + item.getName() + " saved");

        if (--mItemCount == 0) {
            Log.d(TAG, "download wish images finish");
            onWishImageDownloadDone(true);
        }
    }
}

package com.wish.wishlist.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Patterns;
import android.webkit.MimeTypeMap;

import com.squareup.picasso.Picasso;
import com.wish.wishlist.activity.WebImage;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashSet;

/**
 * Created by jiawen on 15-05-14.
 */


public class GetWebItemTask extends AsyncTask<WebRequest, Integer, WebResult> {
    final static String TAG = "GetWebItemTask";
    Context mContext;
    private OnWebResult mListener;

    public interface OnWebResult {
        void onWebResult(WebResult result);
    }

    public GetWebItemTask(Context context, OnWebResult listener) {
        super();
        this.mContext = context;
        this.mListener = listener;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    WebResult getImages(WebRequest request) {
        WebResult result = new WebResult();
        try {
            //Connection.Response response = Jsoup.connect(urls[0]).followRedirects(true).execute();
            Document doc;
            if (request.html != null && !request.html.isEmpty()) {
                result._attemptedDynamicHtml = true;
                doc = Jsoup.parse(request.html);
            } else {
                long startTime = System.currentTimeMillis();
                doc = Jsoup.connect(request.url)
                        .header("Accept-Encoding", "gzip, deflate")
                        .userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:23.0) Gecko/20100101 Firefox/23.0")
                        .maxBodySize(0)
                        .timeout(600000)
                        .get();

                long time = System.currentTimeMillis() - startTime;
                Log.d(TAG, "Jsoup connect took " + time + " ms");
            }


            // Prefer og:title if the site has it
            Elements og_title_element = doc.head().select("meta[property=og:title]");
            if (!og_title_element.isEmpty()) {
                String og_title = og_title_element.first().attr("content");
                Log.d(TAG, "og:title : " + og_title);
                result._title = og_title;
            }
            if (result._title == null || result._title.trim().isEmpty()) {
                Elements meta_title_element = doc.head().select("meta[name=title]");
                if (!meta_title_element.isEmpty()) {
                    String meta_title = meta_title_element.first().attr("content");
                    Log.d(TAG, "meta:title : " + meta_title);
                    result._title = meta_title;
                }
            }
            if (result._title == null || result._title.trim().isEmpty()) {
                result._title = doc.title();
            }

            Elements og_description_element = doc.head().select("meta[property=og:description]");
            if (!og_description_element.isEmpty()) {
                String og_description = og_description_element.first().attr("content");
                Log.d(TAG, "og:description : " + og_description);
                result._description = og_description;
            }
            if (result._description == null || result._description.trim().isEmpty()) {
                Elements meta_description_element = doc.head().select("meta[name=description]");
                if (!meta_description_element.isEmpty()) {
                    String meta_description = meta_description_element.first().attr("content");
                    Log.d(TAG, "meta:description : " + meta_description);
                    result._description = meta_description;
                }
            }

            // start loading images
            HashSet<String> imageUrls = new HashSet<>();
            Bitmap single_image = null;

            // Prefer twitter image over facebook image, because facebook og:image requires dimension of
            // 1200 x 630 or 600 x 315, which will crop a tall image in an ugly way

            // Some websites use twitter:image, some use twitter:image:src, make sure we cover both.
            Elements twitter_image_element = doc.head().select("meta[property=twitter:image]");
            if (twitter_image_element.isEmpty()) {
                twitter_image_element = doc.head().select("meta[property=twitter:image:src]");
            }
            if (!twitter_image_element.isEmpty()) {
                String twitter_image_src = twitter_image_element.first().attr("content");
                twitter_image_src = getValidImageUrl(twitter_image_src);
                if (!twitter_image_src.isEmpty()) {
                    Bitmap image;
                    Log.d(TAG, "twitter image src " + twitter_image_src);
                    image = scaleDownBitmap(twitter_image_src);

                    if (image != null && image.getWidth() >= 100 && image.getHeight() >= 100) {
                        Log.d(TAG, "twitter:image:src " + twitter_image_src + " " + image.getWidth() + "x" + image.getHeight());
                        result._webImages.add(new WebImage(twitter_image_src, image.getWidth(), image.getHeight(), "", image));
                        Analytics.send(Analytics.DEBUG, "GotImage", "Twitter");
                        if (!request.getAllImages) {
                            return result;
                        }
                        single_image = image;
                        imageUrls.add(twitter_image_src);
                    }
                }
            }

            Elements og_image = doc.head().select("meta[property=og:image]");
            if (!og_image.isEmpty()) {
                String og_image_src = og_image.first().attr("content");
                og_image_src = getValidImageUrl(og_image_src);
                if (!og_image_src.isEmpty() && !imageUrls.contains(og_image_src)) {
                    imageUrls.add(og_image_src);
                    Bitmap image = scaleDownBitmap(og_image_src);
                    // some websites like kijiji will return us a tiny og:image
                    // let's try to get more images if this happens.
                    if (image != null && image.getWidth() >= 100 && image.getHeight() >= 100) {
                        Log.d(TAG, "og:image src: " + og_image_src + " " + image.getWidth() + "x" + image.getHeight());
                        result._webImages.add(new WebImage(og_image_src, image.getWidth(), image.getHeight(), "", image));
                        Analytics.send(Analytics.DEBUG, "GotImage", "OG");
                        if (!request.getAllImages) {
                            return result;
                        }
                        single_image = image;
                    }
                }
            }

            // Didn't find og:image tag, so retrieve all the images in the website, filter them by type and size, and
            // let user choose one.
            Elements img_elements = doc.getElementsByTag("img");
            Log.d(TAG, "Found " + img_elements.size() + " img elements");

            for (Element el : img_elements) {
                //Log.d(TAG, "el tostring " + el.toString());

                String src = getImageUrl(el, "src");
                if (src.isEmpty()) {
                    // website like overstock.com put useful image in data-src attribute
                    src = getImageUrl(el, "data-src");
                    if (src.isEmpty()) {
                        continue;
                    }
                }

                // there could be duplicate image urls from img_elements, make sure we don't process the same url more than once.
                if (imageUrls.contains(src)) {
                    continue;
                }
                imageUrls.add(src);
                // width and height can be in the format of 100px,
                // remove the non digit part of the string.
                //String width = el.attr("width").replaceAll("[^0-9]", "");
                //String height = el.attr("height").replaceAll("[^0-9]", "");
                String style = el.attr("style");

                // filter out hidden images, gif and png images. (gif and png are usually used for icons etc.)
                // image file url may not ends with file extension, for example
                // https://www.teslamotors.com/tesla_theme/assets/img/powerwall/powerwall-laptop.png?20160419
                // MimeTypeMap.getFileExtensionFromUrl(src)) will handle this
                String extension = MimeTypeMap.getFileExtensionFromUrl(src);
                if (src.isEmpty() || style.contains("display:none") ||
                        extension.equals("gif") ||
                        extension.equals("png") ||
                        extension.equals("svg")) {
                    continue;
                }

                final Bitmap image = scaleDownBitmap(src);
                // filter out small images
                if (image == null || image.getWidth() <= 100 || image.getHeight() <= 100) {
                    continue;
                }

                if (single_image == null) {
                    single_image = image;
                }
                Log.d(TAG, "adding " + src + " " + image.getWidth() + " x " + image.getHeight());
                result._webImages.add(new WebImage(src, image.getWidth(), image.getHeight(), el.id(), null));
            }

            // when there is only one image, we need to pass the bitmap, because we set the bitmap on the ImageView
            // instead of loading from url
            if (result._webImages.size() == 1) {
                result._webImages.get(0).mBitmap = single_image;
            }
            result._attemptedAllFromJsoup = true;
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }

        Analytics.send(Analytics.DEBUG, "GotImage", "All " + result._webImages.size());
        return result;
    }

    protected WebResult doInBackground(WebRequest... requests) {
        WebRequest request = requests[0];
        return getImages(request);
    }

    protected void onProgressUpdate(Integer... progress) {
    }

    @Override
    protected void onCancelled() {
        Log.d(TAG, "onCancelled");
        super.onCancelled();
    }

    protected void onPostExecute(WebResult result) {
        mListener.onWebResult(result);
    }

    private String getImageUrl(Element el, String attribute) {
        String src = el.absUrl(attribute);

        if (src.isEmpty()) {
            src = el.attr("src").trim();
            Log.d(TAG, "mal-formatted img src " + src);
            src = getValidImageUrl(src);
        }
        return src;
    }

    private String getValidImageUrl(String src) {
        // mal-formatted Url, for example m.gamestop.com will give us img src like
        // "//www.gamestop.com/common/images/lbox/111111.jpg"

        // urbanoutffters.com's og:img is also mal-formatted
        // try to correct it

        // trim the leading '/' if there is any
        src = src.replaceAll("^/+", "");
        if (!src.startsWith("http://") && !src.startsWith("https://")) {
            src = "http://" + src;
        }

        // validate the url
        if (!Patterns.WEB_URL.matcher(src).matches()) {
            Log.e(TAG, "Invalid url " + src);
            src = "";
        }
        return src;
    }

    private Bitmap scaleDownBitmap(String url) {
        final int maxImageWidth = dimension.screenWidth() / 2;
        final int maxImageHeight = dimension.screenHeight() / 2;

        //final Bitmap image = Picasso.with(mContext).load(src).resize(imageWidth, 0).centerInside().onlyScaleDown().get();
        // onlyScaleDown() has no effect when working together with resize(targetWidth, 0) on Android 5.1, 6.0
        // it works on Android 4.4

        // workaround the issue by using centerInside
        try {
            return Picasso.with(mContext).load(url).resize(maxImageWidth, maxImageHeight).centerInside().onlyScaleDown().get();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            return null;
        }
    }
}


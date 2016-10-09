package com.wish.wishlist.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.wish.wishlist.activity.WebImage;

import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.StringReader;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.wish.wishlist.util.ItemJsonReader.readObjString;

public class WebItemTask implements ImageDimensionTask.OnImageDimension {
    private final String TAG = "WebItemTask";
    private Context mContext;
    private WebView mWebView;
    private WebRequest mWebRequest;
    private Boolean loadingFinished = true;
    private Boolean redirect = false;
    private Boolean gotResult = false;
    private String stage;
    private final static String STATIC_HTML="STATIC_HTML";
    private final static String WEBVIEW_NO_IMAGE="WEBVIEW_NO_IMAGE";
    private final static String WEBVIEW_IMAGE="WEBVIEW_IMAGE";
    private OnWebResult mListener;
    private String[] jsFiles = {"currency_symbol_map.js", "util.js", "scrape.js"};
    private int fileIndex = 0;
    private long startTime;
    private String html;
    private Target mTarget;
    private ImageDimensionTask imageDimensionTask;

    @JavascriptInterface
    public String getHTML() {
        Log.d(TAG, "getHTML");
        return html;
    }

    @JavascriptInterface
    public String getStage() {
        return stage;
    }

    public interface OnWebResult {
        void onWebResult(WebResult result);
    }

    public WebItemTask(Context context, WebRequest request, OnWebResult listener) {
        super();

        this.mContext = context;
        this.mWebRequest = request;
        this.mListener = listener;
    }

    public void run() {
        startTime = System.currentTimeMillis();
        mWebView = new WebView(mContext);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.addJavascriptInterface(this, "Android");
        getFromStaticHTML();
    }

    private void getFromStaticHTML() {
        stage = STATIC_HTML;

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(mWebRequest.url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                Log.e(TAG, e.toString());
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "response not successful");
                }

                if (response.body().contentType().toString().startsWith("text/html")) {
                    //Log.e(TAG, response.body().string());
                    html = response.body().string();
                    Handler mainHandler = new Handler(mContext.getMainLooper());

                    // okhttp response callback is in its own thread, post the runJS to main thread
                    // because webview can only run in the main thread
                    Runnable myRunnable = new Runnable() {
                        @Override
                        public void run() {
                            runJS();
                        }
                    };
                    mainHandler.post(myRunnable);
                } else {
                    Log.e(TAG, "content type not supported");
                }

                response.body().close();
            }
        });
    }

    private void getFromWebView() {
        html = null;
        fileIndex = 0;
        mWebView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (!loadingFinished) {
                    redirect = true;
                }

                loadingFinished = false;
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                loadingFinished = false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                // onPageFinished could be called multiple times for some websites due to url redirection
                if (!redirect) {
                    loadingFinished = true;
                }

                if (loadingFinished && !redirect) {
                    Log.d(TAG, "loading page finished");
                    if (!gotResult) {
                        runJS();
                    }
                } else {
                    redirect = false;
                }
            }

            @SuppressWarnings("deprecation")
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                // Handle the error
                Log.e(TAG, "onReceivedError " + description + " " + failingUrl);
                super.onReceivedError(view, errorCode, description, failingUrl);
            }

            @TargetApi(android.os.Build.VERSION_CODES.M)
            @Override
            public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError error) {
                // Redirect to deprecated method, so you can use it in all SDK versions
                onReceivedError(view, error.getErrorCode(), error.getDescription().toString(), req.getUrl().toString());
            }
        });

        // skip loading images, try to get images from twitter, og meta prop first
        //mWebView.getSettings().setLoadsImagesAutomatically(false);
        mWebView.getSettings().setBlockNetworkImage(true);
        //mWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        //mWebView.getSettings().setSupportMultipleWindows(false);
        mWebView.loadUrl(mWebRequest.url);
    }

    private void getFromWebViewWithImages() {
        fileIndex = 0;
        //mWebView.getSettings().setLoadsImagesAutomatically(true);
        //enable loading images
        mWebView.getSettings().setBlockNetworkImage(false);
        mWebView.reload();
    }

    private void getBitmapFromUrls(WebResult result) {
        // start downloading the bitmap from result.imageUrls array until we get a valid one
        scaleDownBitmap(result.imageUrls.get(0), result);
    }

    public void onImageDimension(WebResult result) {
        if (result.webImages.size() > 0) {
            mListener.onWebResult(result);
        } else {
            stage = WEBVIEW_NO_IMAGE;
            getFromWebView();
        }
    }

    private void parse(String s) {
        WebResult result = readObjString(s);

        switch (stage) {
            case STATIC_HTML:
                if (!getOneBitmap(result)) {
                    nextStage();
                }
                break;
            case WEBVIEW_NO_IMAGE:
                if (!getOneBitmap(result)) {
                    nextStage();
                }
                break;
            case WEBVIEW_IMAGE:
                gotResult = true;
                Log.e(TAG, stage + " loading: Time: " + (System.currentTimeMillis() - startTime));
                printResult(result);
                if (!getOneBitmap(result)) {
                    mListener.onWebResult(result);
                }
                break;
            default:
                Log.e(TAG, "incorrect stage!");
        }
    }

    private boolean getOneBitmap(WebResult result) {
        if (result.imageUrls != null && !result.imageUrls.isEmpty()) {
            // We got an image from og, twitter or itemprop="image"
            getBitmapFromUrls(result);
            return true;
        } else if (result.webImages.size() > 0) {
            // We fail to get an image from og, twitter or itemprop="image", but
            // we have an array of img src, try to get the first image's bitmap
            for (WebImage webImage : result.webImages) {
                result.imageUrls.add(webImage.mUrl);
            }
            getBitmapFromUrls(result);
            return true;
        } else {
            return false;
        }
    }

    private void nextStage() {
        switch (stage) {
            case STATIC_HTML:
                stage = WEBVIEW_NO_IMAGE;
                getFromWebView();
                break;
            case WEBVIEW_NO_IMAGE:
                stage = WEBVIEW_IMAGE;
                getFromWebViewWithImages();
                break;
            case WEBVIEW_IMAGE:
                Log.e(TAG, "last stage, cannot start the next");
        }
    }

    private void parseJSONString(String s) {
        JsonReader reader = new JsonReader(new StringReader(s));
        // Must set lenient to parse single values
        reader.setLenient(true);
        try {
            if (reader.peek() != JsonToken.NULL) {
                if (reader.peek() == JsonToken.STRING) {
                    String msg = reader.nextString();
                    if (msg != null) {
                        parse(msg);
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException: " + e.toString());
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                Log.e(TAG, "IOException: " + e.toString());
            }
        }
    }

    private void runJS() {
        //startTime = System.currentTimeMillis();
        fileIndex = 0;
        evaluateJS();
    }

    private void evaluateJS() {
        try {
            String js = StringUtil.readFromAssets(jsFiles[fileIndex]);
            Log.d(TAG, "evaluating: " + jsFiles[fileIndex]);
            mWebView.evaluateJavascript(js, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String s) {
                    if (fileIndex == jsFiles.length - 1) {
                        // we have evaluated the last javascript file
                        Log.e(TAG, "url " + mWebRequest.url);
                        //Log.e(TAG, "js time: " + (System.currentTimeMillis() - startTime));
                        parseJSONString(s);
                        return;
                    }
                    fileIndex++;
                    evaluateJS();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    private String getImageUrl(Element el, String attribute) {
        String src = el.absUrl(attribute);

        if (src.isEmpty()) {
            src = el.attr("src").trim();
            Log.d(TAG, "mal-formatted img src " + src);
            //src = getValidImageUrl(src);
        }
        return src;
    }

    private void scaleDownBitmap(final String url, final WebResult result) {
        final int maxImageWidth = dimension.screenWidth() / 2;
        final int maxImageHeight = dimension.screenHeight() / 2;

        mTarget = new Target() {
            private void nextBitmap() {
                int index = result.imageUrls.indexOf(url);
                if (index < result.imageUrls.size() - 1) {
                    scaleDownBitmap(result.imageUrls.get(index + 1), result);
                } else {
                    // we have tried all the urls in result.imageUrls but still fail to get a bitmap
                    // Fixme: shall we proceed to next stage?
                    Log.d(TAG, "tried all, no valid bitmap");
                    printResult(result);
                    nextStage();
                }
            }

            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                if (bitmap != null) {
                    Log.d(TAG, "got valid bitmap");
                    int w = bitmap.getWidth();
                    int h = bitmap.getHeight();
                    if (w < 100 || h < 100) {
                        Log.d(TAG, "bitmap width < 100 or height < 100, skip");
                        nextBitmap();
                        return;
                    }
                    if (w / h > 8 || h / w > 8) {
                        Log.d(TAG, "bitmap aspect ratio > 8, skip");
                        nextBitmap();
                        return;
                    }
                    result.webImages.add(0, new WebImage(url, bitmap.getWidth(), bitmap.getHeight(), "", bitmap));
                    Log.e(TAG, stage + " loading: Time: " + (System.currentTimeMillis() - startTime));
                    printResult(result);
                    mListener.onWebResult(result);
                } else {
                    Log.e(TAG, "null bitmap");
                    nextBitmap();
                }
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {}

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
                Log.e(TAG, "onBitmapFailed");
                nextBitmap();
            }
        };

        //final Bitmap image = Picasso.with(mContext).load(src).resize(imageWidth, 0).centerInside().onlyScaleDown().get();
        // onlyScaleDown() has no effect when working together with resize(targetWidth, 0) on Android 5.1, 6.0
        // it works on Android 4.4

        // workaround the issue by using centerInside
        Picasso.with(mContext).load(url).resize(maxImageWidth, maxImageHeight).centerInside().onlyScaleDown().into(mTarget);
    }

    private void printResult(WebResult result) {
        Log.d(TAG, "title: " + result.title);
        Log.d(TAG, "description: " + result.description);
        Log.d(TAG, "price: " + result.price);
        Log.d(TAG, "priceNumber: " + result.priceNumber);
        if (result.currency != null) {
            Log.d(TAG, "currency: ");
            Log.d(TAG, "  code: " + result.currency.code);
            Log.d(TAG, "  symbol: " + result.currency.symbol);
        } else {
            Log.d(TAG, "currency null");
        }

        if (result.imageUrls != null) {
            Log.d(TAG, "imageUrls: ");
            for (String url : result.imageUrls) {
                Log.d(TAG, "  url: " + url);
            }
        }

        if (result.webImages != null) {
            Log.d(TAG, "images: ");
            for (WebImage webImage : result.webImages) {
                Log.d(TAG, "  url: " + webImage.mUrl);
                Log.d(TAG, "  w: " + webImage.mWidth);
                Log.d(TAG, "  h: " + webImage.mHeight);
                Log.d(TAG, "\n");
            }
        }
    }
}


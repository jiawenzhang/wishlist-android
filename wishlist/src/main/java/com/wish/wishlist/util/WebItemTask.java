package com.wish.wishlist.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
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

import com.wish.wishlist.DownloadBitmapTask;
import com.wish.wishlist.activity.WebImage;

import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.wish.wishlist.util.ItemJsonReader.readObjString;

public class WebItemTask implements
        DownloadBitmapTask.WebImagesListener {
    private final String TAG = "WebItemTask";
    private Context context;
    private WebView webView;
    private WebRequest webRequest;
    private Boolean loadingFinished = true;
    private Boolean redirect = false;
    private Boolean gotResult = false;
    private String stage;
    private final static String STATIC_HTML="STATIC_HTML";
    private final static String WEBVIEW_NO_IMAGE="WEBVIEW_NO_IMAGE";
    private final static String WEBVIEW_IMAGE="WEBVIEW_IMAGE";
    private OnWebResult listener;
    private String[] jsFiles = {"currency_symbol_map.js", "util.js", "scrape.js"};
    private int fileIndex = 0;
    private long startTime;
    private String html;
    private DownloadBitmapTask downloadBitmapTask;
    private WebResult result = new WebResult();
    private Call call;
    private Handler mainHandler;

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

        this.context = context;
        this.webRequest = request;
        this.listener = listener;
        this.mainHandler = new Handler(context.getMainLooper());
    }

    public void run() {
        startTime = System.currentTimeMillis();
        webView = new WebView(context);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(this, "Android");
        getFromStaticHTML();
    }

    public void cancel() {
        Log.d(TAG, "cancel");
        if (call != null) {
            call.cancel();
        }

        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }

        if (downloadBitmapTask != null) {
            downloadBitmapTask.cancel();
        }
    }

    private void getFromStaticHTML() {
        stage = STATIC_HTML;

        OkHttpClient client = new OkHttpClient();

        final Request request = new Request.Builder()
                .url(webRequest.url)
                .build();

        call = client.newCall(request);

        call.enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                Log.e(TAG, e.toString());
                postWebResult(result);
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "response not successful");
                    postWebResult(result);
                }

                if (response.body().contentType().toString().startsWith("text/html")) {
                    //Log.e(TAG, response.body().string());
                    html = response.body().string();

                    // okhttp response callback is in its own thread, post the runJS to main thread
                    // because webview can only run in the main thread
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            runJS();
                        }
                    });
                } else {
                    Log.e(TAG, "content type not supported");
                    postWebResult(result);
                }

                response.body().close();
            }
        });
    }

    private void postWebResult(final WebResult result) {
        // okhttp response callback is in its own thread, post to main thread
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                listener.onWebResult(result);
            }
        });
    }

    private void getFromWebView() {
        html = null;
        fileIndex = 0;
        webView.setWebViewClient(new WebViewClient() {

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
                Analytics.send(Analytics.DEBUG, "WebViewLoadError", description + " " + failingUrl);
                super.onReceivedError(view, errorCode, description, failingUrl);
                postWebResult(result);
            }

            @TargetApi(android.os.Build.VERSION_CODES.M)
            @Override
            public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError error) {
                // Redirect to deprecated method, so you can use it in all SDK versions
                onReceivedError(view, error.getErrorCode(), error.getDescription().toString(), req.getUrl().toString());
            }
        });

        // skip loading images, try to get images from twitter, og meta prop first
        //webView.getSettings().setLoadsImagesAutomatically(false);
        webView.getSettings().setBlockNetworkImage(true);
        //webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        //webView.getSettings().setSupportMultipleWindows(false);
        webView.loadUrl(webRequest.url);
    }

    private void getFromWebViewWithImages() {
        fileIndex = 0;
        //webView.getSettings().setLoadsImagesAutomatically(true);
        //enable loading images
        webView.getSettings().setBlockNetworkImage(false);
        webView.reload();
    }

    private void getBitmapFromUrls(ArrayList<String> imageUrls) {
        // start downloading the bitmap from result.imageUrls array until we get a valid one
        downloadBitmapTask = new DownloadBitmapTask(imageUrls, context, this);
        downloadBitmapTask.execute();
    }

    @Override
    public void gotWebImages(WebImage webImage) {
        if (webImage != null) {
            for (int i = 0; i < result.webImages.size(); i++) {
                if (StringUtil.compare(result.webImages.get(i).mUrl, webImage.mUrl)) {
                    result.webImages.remove(i);
                    break;
                }
            }
            result.webImages.add(0, webImage);
            listener.onWebResult(result);
        } else {
            printResult();
            nextStage();
        }
    }

    private void parse(String s) {
        result = readObjString(s);

        switch (stage) {
            case STATIC_HTML:
                if (!getOneBitmap()) {
                    nextStage();
                }
                break;
            case WEBVIEW_NO_IMAGE:
                if (!getOneBitmap()) {
                    nextStage();
                }
                break;
            case WEBVIEW_IMAGE:
                gotResult = true;
                Log.e(TAG, stage + " loading: Time: " + (System.currentTimeMillis() - startTime));
                printResult();
                if (!getOneBitmap()) {
                    listener.onWebResult(result);
                }
                break;
            default:
                Log.e(TAG, "incorrect stage!");
        }
    }

    private boolean getOneBitmap() {
        if (result.imageUrls != null && !result.imageUrls.isEmpty()) {
            // We got an image from og, twitter or itemprop="image"
            getBitmapFromUrls(result.imageUrls);
            return true;
        } else if (result.webImages.size() > 0) {
            // We fail to get an image from og, twitter or itemprop="image", but
            // we have an array of img src, try to get the first image's bitmap
            ArrayList<String> imageUrls = new ArrayList<>();
            for (WebImage webImage : result.webImages) {
                imageUrls.add(webImage.mUrl);
            }
            getBitmapFromUrls(imageUrls);
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
            webView.evaluateJavascript(js, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String s) {
                    if (fileIndex == jsFiles.length - 1) {
                        // we have evaluated the last javascript file
                        Log.e(TAG, "url " + webRequest.url);
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

    private void printResult() {
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


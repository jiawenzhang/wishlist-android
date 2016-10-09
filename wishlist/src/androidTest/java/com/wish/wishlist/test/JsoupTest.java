package com.wish.wishlist.test;

import android.content.Context;
import android.os.Handler;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.InstrumentationTestCase;
import android.test.RenamingDelegatingContext;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.JsonReader;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import com.wish.wishlist.activity.WebImage;
import com.wish.wishlist.util.GetWebItemTask;
import com.wish.wishlist.util.WebRequest;
import com.wish.wishlist.util.WebResult;

import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import static com.wish.wishlist.util.StringUtil.readFromAssets;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class JsoupTest
        extends InstrumentationTestCase
        implements GetWebItemTask.OnWebResult {

    private static final String TAG = "WebViewTest";
    private Context mMockContext;
    private WebView mWebView;
    private Handler mainHandler;
    private ArrayList<WebRequest> tasks = new ArrayList<>();
    private ArrayList<TestCase> testCases = new ArrayList<>();
    private CountDownLatch mSignal = null;

    private void waitSignal(CountDownLatch signal) {
        try {
            signal.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void onWebResult(WebResult webResult) {
        Log.d(TAG, "onWebResult");

//        ArrayList<String> result = new ArrayList<>();
//        if (webResult.title != null) {
//            result.add("title");
//        }
//        if (webResult.description != null) {
//            result.add("description");
//        }
//        if (webResult.price != null) {
//            result.add("price");
//        }
//        if (webResult.priceNumber != null) {
//            result.add("price_number");
//        }
//        if (webResult.currency != null) {
//            result.add("currency");
//        }
//        if (webResult.imageUrls != null && webResult.imageUrls.size() > 0) {
//            result.add("image_urls");
//        }
//        if (webResult.webImages != null && webResult.webImages.size() > 0) {
//            result.add("images");
//        }
//
//        ArrayList expected = testCases.get(0).expected;
//        if (!StringUtil.sameArrays(result, expected)) {
//            Log.e(TAG, "url: " + testCases.get(0).url);
//            Log.e(TAG, "result != expected");
//            Log.e(TAG, "result: " + result.toString());
//            Log.e(TAG, "expected: " + expected);
//            printResult(webResult);
//            assertTrue(false);
//        }

        mSignal.countDown();
        if (!tasks.isEmpty()) {
            tasks.remove(0);
            testCases.remove(0);
        }

        if (!tasks.isEmpty()) {
            scrapeNext();
        }
    }

    class TestCase {
        String store;
        String url;
        ArrayList<String> expected;
    }

    private ArrayList<String> readExpected(JsonReader reader) {
        ArrayList<String> expected = new ArrayList<>();

        try {
            reader.beginArray();
            while (reader.hasNext()) {
               expected.add(reader.nextString());
            }
            reader.endArray();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

        return expected;
    }

    private TestCase readTest(JsonReader reader) {
        TestCase testCase = new TestCase();

        try {
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equals("store")) {
                    testCase.store = reader.nextString();
                } else if (name.equals("url")) {
                    testCase.url = reader.nextString();
                } else if (name.equals("expected")) {
                    testCase.expected = readExpected(reader);
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }

        return testCase;
    }

    private void parseTests(String testsString) {
        JsonReader reader = new JsonReader(new StringReader(testsString));
        reader.setLenient(true);

        try {
            reader.beginArray();
            while (reader.hasNext()) {
                TestCase testCase = readTest(reader);
                testCases.add(testCase);
            }
            reader.endArray();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        } finally {
            try {
                reader.close();
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    private void printResult(WebResult result) {
        Log.d(TAG, "url: " + result.url);
        Log.d(TAG, "title: " + result.title);
        Log.d(TAG, "description: " + result.description);
        Log.d(TAG, "price: " + result.price);
        Log.d(TAG, "priceNumber: " + result.priceNumber);
        if (result.currency != null) {
            Log.d(TAG, "currency: ");
            Log.d(TAG, "code: " + result.currency.code);
            Log.d(TAG, "symbol: " + result.currency.symbol);
        } else {
            Log.d(TAG, "currency null");
        }

        if (result.imageUrls != null) {
            Log.d(TAG, "imageUrls: ");
            for (String url : result.imageUrls) {
                Log.d(TAG, "url: " + url);
            }
        }

        if (result.webImages != null) {
            Log.d(TAG, "images: ");
            for (WebImage webImage : result.webImages) {
                Log.d(TAG, "url: " + webImage.mUrl);
                Log.d(TAG, "w: " + webImage.mWidth);
                Log.d(TAG, "h: " + webImage.mHeight);
                Log.d(TAG, "\n");
            }
        }
    }

    private void readTestCases() {
        // Read in the test cases;
        mSignal = new CountDownLatch(1);
        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                mWebView = new WebView(mMockContext);
                mWebView.getSettings().setJavaScriptEnabled(true);
                try {
                    String js = readFromAssets("mobile_test.js");
                    mWebView.evaluateJavascript(js, new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String s) {
                            // we have evaluated the last javascript file
                            // Log.e(TAG, s); // Returns the value from the function
                            parseTests(s);
                            mSignal.countDown();
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
            }
        };

        mainHandler.post(myRunnable);
        waitSignal(mSignal);
    }

    private void scrapeNext() {
        GetWebItemTask itemTask = new GetWebItemTask(mMockContext, this);
        itemTask.execute(tasks.get(0));
    }

    @org.junit.Test
    public void runTest() throws InterruptedException {
        mMockContext = new RenamingDelegatingContext(InstrumentationRegistry.getInstrumentation().getTargetContext(), "test_");
        mainHandler = new Handler(mMockContext.getMainLooper());

        readTestCases();

        // Scrape the website in each test case
        for (TestCase testCase : testCases) {
            WebRequest request = new WebRequest();
            request.url = testCase.url;
            tasks.add(request);
        }

        mSignal = new CountDownLatch(tasks.size());
        scrapeNext();
        waitSignal(mSignal);
    }
}

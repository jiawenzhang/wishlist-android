package com.wish.wishlist.activity;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Observer;
import java.util.Observable;
import java.util.regex.Matcher;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.wish.wishlist.R;
import com.wish.wishlist.db.TagItemDBManager;
import com.wish.wishlist.fragment.WebImageFragmentDialog;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.model.WishItemManager;
import com.wish.wishlist.WishlistApplication;
import com.wish.wishlist.util.DialogOnShowListener;
import com.wish.wishlist.util.ImageManager;
import com.wish.wishlist.util.PositionManager;
import com.wish.wishlist.util.WebRequest;
import com.wish.wishlist.util.WebResult;
import com.wish.wishlist.util.camera.PhotoFileCreater;
import com.wish.wishlist.util.camera.CameraManager;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import java.net.URL;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import com.wish.wishlist.util.GetWebItemTask;
import com.wish.wishlist.util.sync.SyncAgent;

/*** EditItemInfo.java is responsible for reading in the info. of a newly added item 
 * including its name, description, time, price, location and photo, and saving them
 * as a row in the Item table in the database
 */
@SuppressLint("NewApi")
public class EditItem extends Activity
        implements Observer,
        WebImageFragmentDialog.OnWebImageSelectedListener,
        WebImageFragmentDialog.OnLoadMoreFromWebViewListener,
        WebImageFragmentDialog.OnLoadMoreSelectedListener,
        WebImageFragmentDialog.OnWebImageCancelledListener,
        GetWebItemTask.OnWebResult {

    private EditText _itemNameEditText;
    private EditText _noteEditText;
    private EditText _priceEditText;
    private EditText _storeEditText;
    private EditText _locationEditText;
    private EditText _linkEditText;
    private CheckBox _completeCheckBox;

    private ImageButton _mapImageButton;
    private ImageButton _cameraImageButton;
    private ImageButton _galleryImageButton;
    private ImageView _imageItem;
    private double _lat = Double.MIN_VALUE;
    private double _lng = Double.MIN_VALUE;
    private String _ddStr = "unknown";
    private Uri _selectedPicUri = null;
    private String _webPicUrl = null;
    private String _fullsizePhotoPath = null;
    private String _newfullsizePhotoPath = null;
    PositionManager _pManager;
    private long mItem_id = -1;
    private int _complete = -1;
    private boolean _editNew = true;
    private boolean _isGettingLocation = false;
    private ArrayList<String> _tags = new ArrayList<String>();
    private Bitmap _webBitmap = null;

    private static final int TAKE_PICTURE = 1;
    private static final int SELECT_PICTURE = 2;
    private static final int ADD_TAG = 3;
    private Boolean _selectedPic = false;
    private String mLink = null;
    private String mHost = null;
    ProgressDialog mProgressDialog = null;
    GetWebItemTask mGetWebItemTask = null;
    WebView mWebView = null;

    private static WebResult mWebResult = null;

    static final public String FULLSIZE_PHOTO_PATH = "FULLSIZE_PHOTO_PATH";
    static final public String NEW_FULLSIZE_PHOTO_PATH = "NEW_FULLSIZE_PHOTO_PATH";
    static final public String SELECTED_PIC_URL = "SELECTED_PIC_URL";
    static final public String WEB_PIC_URL = "WEB_PIC_URL";

    static final private String TAG = "EditItem";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_item);
        Log.d(TAG, "onCreate");

        setUpActionBar();

        _mapImageButton = (ImageButton) findViewById(R.id.imageButton_map);
        _mapImageButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                //get the location
                if (!_isGettingLocation) {
                    _pManager.startLocationUpdates();
                    _isGettingLocation = true;
                    _locationEditText.setText("Loading location...");
                }
            }
        });

        _pManager = new PositionManager(EditItem.this);
        _pManager.addObserver(this);

        //find the resources by their ids
        _itemNameEditText = (EditText) findViewById(R.id.itemname);
        _noteEditText = (EditText) findViewById(R.id.note);
        _priceEditText = (EditText) findViewById(R.id.price);
        _storeEditText = (EditText) findViewById(R.id.store);
        _locationEditText = (EditText) findViewById(R.id.location);
        _linkEditText = (EditText) findViewById(R.id.link);
        _completeCheckBox = (CheckBox) findViewById(R.id.completeCheckBox);

        _cameraImageButton = (ImageButton) findViewById(R.id.imageButton_camera);

        ImageButton tagImageButton = (ImageButton) findViewById(R.id.imageButton_tag);
        tagImageButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(EditItem.this, AddTagFromEditItem.class);
                i.putExtra(AddTagFromEditItem.TAGS, _tags);
                startActivityForResult(i, ADD_TAG);
            }
        });

        _galleryImageButton = (ImageButton) findViewById(R.id.imageButton_gallery);

        _imageItem = (ImageView) findViewById(R.id.image_photo);
        _imageItem.setScaleType(ImageView.ScaleType.CENTER_CROP);

        final View imageFrame = findViewById(R.id.image_photo_frame);
        imageFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(EditItem.this, FullscreenPhoto.class);
                if (_fullsizePhotoPath != null) {
                    i.putExtra(FULLSIZE_PHOTO_PATH, _fullsizePhotoPath);
                    i.putExtra(SELECTED_PIC_URL, _selectedPicUri);
                    startActivity(i);
                }
            }
        });

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

        //get the fullsizephotopatch, if it is not null, EdiItemInfo is launched from
        //dashboard camera
        _fullsizePhotoPath = intent.getStringExtra(FULLSIZE_PHOTO_PATH);
        setTakenPhoto();
        if (intent.getStringExtra(SELECTED_PIC_URL) != null) {
            _selectedPicUri = Uri.parse(intent.getStringExtra(SELECTED_PIC_URL));
            setSelectedPic();
        }
        if (intent.getStringExtra(WEB_PIC_URL) != null) {
            _webPicUrl = intent.getStringExtra(WEB_PIC_URL);
            setWebPic(_webPicUrl);
        }

        //get item id from previous intent, if there is an item id, we know this EditItemInfo is launched
        //by editing an existing item, so fill the empty box
        mItem_id = intent.getLongExtra("item_id", -1);

        if (mItem_id != -1) {
            _editNew = false;

            _mapImageButton.setVisibility(View.GONE);
            _completeCheckBox.setVisibility(View.VISIBLE);

            WishItem item = WishItemManager.getInstance(this).getItemById(mItem_id);
            _complete = item.getComplete();
            if (_complete == 1) {
                _completeCheckBox.setChecked(true);
            } else {
                _completeCheckBox.setChecked(false);
            }

            _itemNameEditText.setText(item.getName());
            _noteEditText.setText(item.getDesc());
            String priceStr = item.getPriceAsString();
            if (priceStr != null) {
                _priceEditText.setText(priceStr);
            }
            _locationEditText.setText(item.getAddress());
            _linkEditText.setText(item.getLink());
            _storeEditText.setText(item.getStoreName());
            _fullsizePhotoPath = item.getFullsizePicPath();
            if (_fullsizePhotoPath != null) {
                String thumb_path = PhotoFileCreater.getInstance().thumbFilePath(_fullsizePhotoPath);
                Picasso.with(this).load(new File(thumb_path)).fit().centerCrop().into(_imageItem);
                _imageItem.setVisibility(View.VISIBLE);
            }
            _tags = TagItemDBManager.instance(this).tags_of_item(mItem_id);
        } else { //we are editing a new wish, get the location in background
            boolean action_send = (Intent.ACTION_SEND.equals(action) && type != null);
            boolean tagLocation = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("autoLocation", true) && !action_send;
            if (tagLocation) {
                _pManager.startLocationUpdates();
                _isGettingLocation = true;
                _locationEditText.setText("Loading location...");
            }
        }

        _cameraImageButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(EditItem.this);
                final CharSequence[] items = {"Take a photo", "From gallery"};
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        if (which == 0) {
                            dispatchTakePictureIntent();
                        }
                        else if (which == 1) {
                            dispatchImportPictureIntent();
                        }
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.setOnShowListener(new DialogOnShowListener(EditItem.this));
                dialog.show();
            };
        });

        _galleryImageButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                //open gallery;
                dispatchImportPictureIntent();
            }
        });

        //set the keyListener for the Item Name EditText
        _itemNameEditText.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN)
                    if (keyCode == KeyEvent.KEYCODE_ENTER) {
                        _itemNameEditText.setSelected(false);
                    }
                return false;
            }
        });

        //set the keyListener for the Item Description EditText
        _noteEditText.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN)
                    if (keyCode == KeyEvent.KEYCODE_ENTER) {
                        _noteEditText.setSelected(false);
                    }
                return false;
            }
        });

        //set the keyListener for the Item Price EditText
        _priceEditText.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN)
                    if (keyCode == KeyEvent.KEYCODE_ENTER) {
                        _priceEditText.setSelected(false);
                    }
                return false;
            }
        });

        //set the keyListener for the Item Location EditText
        _locationEditText.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN)
                    if (keyCode == KeyEvent.KEYCODE_ENTER) {
                        _locationEditText.setSelected(false);
                    }
                return false;
            }
        });

        // We are restoring an instance, for example after screen orientation
        if (savedInstanceState != null) {
            // restore the current selected item in the list
            _newfullsizePhotoPath = savedInstanceState.getString(NEW_FULLSIZE_PHOTO_PATH);
            _fullsizePhotoPath = savedInstanceState.getString(FULLSIZE_PHOTO_PATH);
            if (intent.getStringExtra(SELECTED_PIC_URL) != null) {
                _selectedPicUri = Uri.parse(intent.getStringExtra(SELECTED_PIC_URL));
            }
            if (intent.getStringExtra(WEB_PIC_URL) != null) {
                _webPicUrl = intent.getStringExtra(WEB_PIC_URL);
            }
            if (_fullsizePhotoPath != null) {
                String thumb_path = PhotoFileCreater.getInstance().thumbFilePath(_fullsizePhotoPath);
                Picasso.with(this).load(new File(thumb_path)).fit().centerCrop().into(_imageItem);
            } else {
                Picasso.with(this).load(_selectedPicUri).fit().centerCrop().into(_imageItem);
            }
        }
    }

    void handleSendAll(Intent intent) {
        Log.d(TAG, "handleSendAll");
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            _itemNameEditText.setText(sharedText);
        }
        _selectedPicUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        setSelectedPic();

        if (_selectedPicUri != null) {
            mHost = _selectedPicUri.getHost();
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

    void handleSendText(Intent intent) {
        Log.d(TAG, "handleSendText");
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            Log.d(TAG, "shared text: " + sharedText);
            ArrayList<String> links = extractLinks(sharedText);
            if (links.isEmpty()) {
                _itemNameEditText.setText(sharedText);
                return;
            }

            mHost = null;
            for (String link : links) {
                try {
                    URL url = new URL(link);
                    mLink = link;
                    mHost = url.getHost();

                    String store = mHost.startsWith("www.") ? mHost.substring(4) : mHost;
                    _storeEditText.setText(store);
                    break;
                } catch (MalformedURLException e) {
                    Log.d(TAG, e.toString());
                }
            }

            if (mLink == null) {
                _itemNameEditText.setText(sharedText);
                return;
            }

            // remove the link from the text;
            String name = sharedText.replace(mLink, "");
            _itemNameEditText.setText(name);

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

            _linkEditText.setText(mLink);
            _linkEditText.setEnabled(false);

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

    void getGeneratedHtml() {
        mWebView = new WebView(EditItem.this);
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

    class MyJavaScriptInterface {
        private Context ctx;

        MyJavaScriptInterface(Context ctx) {
            this.ctx = ctx;
        }

        @JavascriptInterface
        public void gotHTML(String html) {
            Log.d(TAG, "gotHTML");
            long time = System.currentTimeMillis() - startTime;
            Log.d(TAG, "webview show HTML took " + time + " ms");
            ((EditItem) ctx).loadImagesFromHtml(html);
        }
    }

    void handleSendImage(Intent intent) {
        Log.d(TAG, "handleSendImage");
        _selectedPicUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        setSelectedPic();
        if (_selectedPicUri != null) {
            mHost= _selectedPicUri.getHost();
            if (mHost == null) {
                return;
            }

            Log.d(TAG, "host " + _selectedPicUri.getHost());

            Tracker t = ((WishlistApplication) getApplication()).getTracker(WishlistApplication.TrackerName.APP_TRACKER);
            t.send(new HitBuilders.EventBuilder()
                    .setCategory("Wish")
                    .setAction("ShareFrom_Image")
                    .setLabel(mHost)
                    .build());
        }
    }

    void handleSendMultipleImages(Intent intent) {
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

    public ArrayList<String> extractLinks(String text) {
        ArrayList<String> links = new ArrayList<String>();
        Matcher m = Patterns.WEB_URL.matcher(text);
        while (m.find()) {
            String url = m.group();
            links.add(url);
        }
        return links;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_actionbar_edititeminfo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            navigateBack();
            return true;
        }
        else if (id == R.id.menu_done) {
            //this replaced the saveImageButton used in GingerBread
            // app icon save in action bar clicked;
            saveWishItem();
            return true;
        }
        else {
            return super.onOptionsItemSelected(item);
        }
    }

    /***
     * Save user input as a wish item
     */
    private void saveWishItem() {

        if(_itemNameEditText.getText().toString().trim().length() == 0){
            Toast toast = Toast.makeText(this, "Please give a name to your wish", Toast.LENGTH_SHORT);
            int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
            toast.setGravity(Gravity.TOP | Gravity.CENTER, 0, screenHeight/4);
            toast.show();
            return;
        }

        Tracker t = ((WishlistApplication) getApplication()).getTracker(WishlistApplication.TrackerName.APP_TRACKER);
        t.send(new HitBuilders.EventBuilder()
                .setCategory("Wish")
                .setAction("Save")
                .build());

        //define variables to hold the item info.
        String itemName = "";
        String itemDesc = "";
        String itemStoreName = "";
        double itemPrice;
        int itemPriority = 0;
        int itemComplete = 0;
        String itemLink;
        itemLink = _linkEditText.getText().toString().trim();
        if (!itemLink.isEmpty() && !Patterns.WEB_URL.matcher(itemLink).matches()) {
            Toast toast = Toast.makeText(this, "Link invalid", Toast.LENGTH_SHORT);
            int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
            toast.setGravity(Gravity.TOP | Gravity.CENTER, 0, screenHeight/4);
            toast.show();
            return;
        }

        try {
            // read in the name, description, price and location of the item
            // from the EditText
            itemName = _itemNameEditText.getText().toString().trim();
            itemDesc = _noteEditText.getText().toString().trim();
            itemStoreName = _storeEditText.getText().toString().trim();
            _ddStr = _locationEditText.getText().toString().trim();
            if (_ddStr.equals("Loading location...")) {
                _ddStr = "unknown";
            }

            if (_completeCheckBox.isChecked()) {
                itemComplete = 1;
            }
            else {
                itemComplete = 0;
            }

            itemPrice = Double.valueOf(_priceEditText.getText().toString().trim());
        }

        catch (NumberFormatException e) {
            // need some error message here
            // price format incorrect
            e.toString();
            itemPrice = Double.MIN_VALUE;
        }

        if (_webBitmap != null) {
            _fullsizePhotoPath = ImageManager.saveBitmapToAlbum(_webBitmap);
            ImageManager.saveBitmapToThumb(_webBitmap, _fullsizePhotoPath);
        } else if (_selectedPic && _selectedPicUri != null) {
            _fullsizePhotoPath = copyPhotoToAlbum(_selectedPicUri);
            ImageManager.saveBitmapToThumb(_selectedPicUri, _fullsizePhotoPath, this);
        }

        if (mItem_id == -1) {
            // create a new item
            WishItem item = new WishItem(this, mItem_id, "", itemStoreName, itemName, itemDesc,
                    System.currentTimeMillis(), null, _fullsizePhotoPath, itemPrice, _lat, _lng,
                    _ddStr, itemPriority, itemComplete, itemLink, false);

            mItem_id = item.saveToLocal();
        } else {
            // updating an existing item
            WishItem item = WishItemManager.getInstance(this).getItemById(mItem_id);
            item.setStoreName(itemStoreName);
            item.setName(itemName);
            item.setDesc(itemDesc);
            item.setUpdatedTime(System.currentTimeMillis());
            item.setFullsizePicPath(_fullsizePhotoPath);
            item.setPrice(itemPrice);
            item.setLatitude(_lat);
            item.setLongitude(_lng);
            item.setAddress(_ddStr);
            item.setComplete(itemComplete);
            item.setLink(itemLink);
            item.saveToLocal();
        }

        //save the tags of this item
        TagItemDBManager.instance(EditItem.this).Update_item_tags(mItem_id, _tags);
        SyncAgent.getInstance(this).sync();

        //close this activity
        Intent resultIntent = new Intent();
        resultIntent.putExtra("itemID", mItem_id);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    public void onWebImageSelected(int position) {
        // After the dialog fragment completes, it calls this callback.
        WebImage webImage = mWebResult._webImages.get(position);
        _webPicUrl = webImage.mUrl;
        setWebPic(_webPicUrl);
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

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case TAKE_PICTURE: {
                if (resultCode == RESULT_OK) {
                    _fullsizePhotoPath = String.valueOf(_newfullsizePhotoPath);
                    _newfullsizePhotoPath = null;
                    Tracker t = ((WishlistApplication) getApplication()).getTracker(WishlistApplication.TrackerName.APP_TRACKER);
                    t.send(new HitBuilders.EventBuilder()
                            .setCategory("Wish")
                            .setAction("TakenPicture")
                            .setLabel("FromEditItemCameraButton")
                            .build());
                    setTakenPhoto();
                }
                break;
            }
            case SELECT_PICTURE: {
                if (resultCode == RESULT_OK) {
                    _selectedPicUri = data.getData();
                    Tracker t = ((WishlistApplication) getApplication()).getTracker(WishlistApplication.TrackerName.APP_TRACKER);
                    t.send(new HitBuilders.EventBuilder()
                            .setCategory("Wish")
                            .setAction("SelectedPicture")
                            .build());
                    setSelectedPic();
                }
                break;
            }
            case ADD_TAG: {
                if (resultCode == RESULT_OK) {
                    _tags = data.getStringArrayListExtra(AddTagFromEditItem.TAGS);
                }
            }
        }//switch
    }

    private void dispatchTakePictureIntent() {
        CameraManager c = new CameraManager();
        _newfullsizePhotoPath = c.getPhotoPath();
        startActivityForResult(c.getCameraIntent(), TAKE_PICTURE);
    }

    private void dispatchImportPictureIntent() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,"Select Picture"), SELECT_PICTURE);
    }

    private String copyPhotoToAlbum(Uri uri) {
        try {
            //save the photo to a file we created in wishlist album
            final InputStream in = getContentResolver().openInputStream(uri);
            File f = PhotoFileCreater.getInstance().setUpPhotoFile(false);
            String path = f.getAbsolutePath();
            OutputStream stream = new BufferedOutputStream(new FileOutputStream(f));
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int len;
            while ((len = in.read(buffer)) != -1) {
                stream.write(buffer, 0, len);
            }
            in.close();
            if (stream != null) {
                stream.close();
            }
            return path;
        }
        catch (FileNotFoundException e) {
            Log.e(TAG, e.toString());
        }
        catch (IOException e) {
            Log.e(TAG, e.toString());
        }
        return null;
    }

    private boolean navigateBack(){
        //all fields are empty
        if(_itemNameEditText.getText().toString().length() == 0 &&
                _noteEditText.getText().toString().length() == 0 &&
                _priceEditText.getText().toString().length() == 0 &&
                _locationEditText.getText().toString().length() == 0 &&
                _storeEditText.getText().toString().length() == 0){

            setResult(RESULT_CANCELED, null);
            finish();
            return false;
        }

        //only show warning if user is editing a new item
        if(_editNew){
            AlertDialog dialog;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Discard the wish?").setCancelable(
                    false).setPositiveButton("Yes",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            setResult(RESULT_CANCELED, null);
                            EditItem.this.finish();
                        }
                    }).setNegativeButton("No",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            dialog = builder.create();
            dialog.setOnShowListener(new DialogOnShowListener(this));
            dialog.show();
        }
        else{
            setResult(RESULT_CANCELED, null);
            finish();
        }
        return false;

    }

    /***
     * called when the "return" button is clicked
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            return navigateBack();
        }
        return false;
    }

    private Boolean setTakenPhoto() {
        if (_fullsizePhotoPath == null) {
            return false;
        }
        Log.d(TAG, "setTakePhoto " + _fullsizePhotoPath);
        _imageItem.setVisibility(View.VISIBLE);
        String thumb_path = PhotoFileCreater.getInstance().thumbFilePath(_fullsizePhotoPath);
        Picasso.with(this).load(new File(thumb_path)).fit().centerCrop().into(_imageItem);
        _selectedPicUri = null;
        _webPicUrl = null;
        _selectedPic = false;
        return true;
    }

    private Boolean setSelectedPic() {
        if (_selectedPicUri == null) {
            return false;
        }
        Log.d(TAG, "setSelectedPic " + _selectedPicUri.toString());
        _imageItem.setVisibility(View.VISIBLE);
        Picasso.with(this).load(_selectedPicUri).fit().centerCrop().into(_imageItem);
        _fullsizePhotoPath = null;
        _webPicUrl = null;
        _selectedPic = true;
        return true;
    }

    private Boolean setWebPic(String url) {
        Log.d(TAG, "setWebPic " + url);
        final Target target = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                _imageItem.setImageBitmap(bitmap);
                _imageItem.setVisibility(View.VISIBLE);
                _webBitmap = bitmap;
            }
            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {}

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {}
        };
        _imageItem.setTag(target);

        Picasso.with(this).load(url).into(target);
        _fullsizePhotoPath = null;
        _selectedPicUri = null;
        _selectedPic = false;
        return true;
    }

    //this will make the photo taken before to show up if user cancels taking a second photo
    //this will also save the thumbnail on switching screen orientation
    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        Log.d(TAG, "onSaveInstanceState");
        if (_newfullsizePhotoPath != null) {
            savedInstanceState.putString(NEW_FULLSIZE_PHOTO_PATH, _newfullsizePhotoPath.toString());
        }
        if (_selectedPicUri != null) {
            savedInstanceState.putString(SELECTED_PIC_URL, _selectedPicUri.toString());
        }
        if (_webPicUrl != null) {
            savedInstanceState.putString(WEB_PIC_URL, _webPicUrl);
            Log.d(TAG, "_webPicUrl " + _webPicUrl);
        }

        savedInstanceState.putString(FULLSIZE_PHOTO_PATH, _fullsizePhotoPath);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // restore the current selected item in the list
        if (savedInstanceState != null) {
            _newfullsizePhotoPath = savedInstanceState.getString(NEW_FULLSIZE_PHOTO_PATH);
            _fullsizePhotoPath = savedInstanceState.getString(FULLSIZE_PHOTO_PATH);
            if (savedInstanceState.getString(SELECTED_PIC_URL) != null) {
                _selectedPicUri = Uri.parse(savedInstanceState.getString(SELECTED_PIC_URL));
            }
            if (savedInstanceState.getString(WEB_PIC_URL) != null) {
                _webPicUrl = savedInstanceState.getString(WEB_PIC_URL);
            }
            if (_fullsizePhotoPath != null) {
                setTakenPhoto();
            } else if (_selectedPicUri != null) {
                setSelectedPic();
            } else if (_webPicUrl != null){
                setWebPic(_webPicUrl);
            }
        }
    }

    @Override
    public void update(Observable observable, Object data) {
        // This method is notified after data changes.
        //get the location
        Location location = _pManager.getCurrentLocation();
        if (location == null){
            _ddStr = "unknown";
            //need better value to indicate it's not valid lat and lng
            _lat = Double.MIN_VALUE;
            _lng = Double.MIN_VALUE;
            _locationEditText.setText(_ddStr);
            _isGettingLocation = false;
        }
        else {
            //get current latitude and longitude
            _lat = location.getLatitude();
            _lng = location.getLongitude();
            new GetAddressTask().execute("");
        }
    }

    private class GetAddressTask extends AsyncTask<String, Void, String> {//<param, progress, result>
        @Override
        protected String doInBackground(String... arg) {
            //getCuttentAddStr using geocode, may take a while, need to put this to a separate thread
            _ddStr = _pManager.getCuttentAddStr();
            return _ddStr;
        }

        @Override
        protected void onPostExecute(String add) {
            if (_ddStr.equals("unknown")) {
                Toast.makeText(EditItem.this, "location not available", Toast.LENGTH_LONG);
            }
            _locationEditText.setText(_ddStr);
            _isGettingLocation = false;
        }
    }

    @SuppressLint("NewApi")
    private void setUpActionBar() {
        // Make sure we're running on Honeycomb or higher to use ActionBar APIs
            ActionBar actionBar = getActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);
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

    private long startTime;
    @Override
    public void onWebResult(WebResult result)
    {
        Log.d(TAG, "onWebResult");
        if (result._webImages.isEmpty() && !result._attemptedDynamicHtml) {
            startTime = System.currentTimeMillis();
            getGeneratedHtml();
            return;
        }
        mProgressDialog.dismiss();
        if (result._title != null && !result._title.trim().isEmpty()) {
            _itemNameEditText.setText(result._title);
        }
        if (result._description != null && !result._description.trim().isEmpty()) {
            _noteEditText.setText(result._description);
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
}

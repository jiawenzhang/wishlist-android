package com.wish.wishlist.activity;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Observer;
import java.util.Observable;
import java.util.regex.Matcher;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.wish.wishlist.R;
import com.wish.wishlist.db.LocationDBManager;
import com.wish.wishlist.db.StoreDBManager;
import com.wish.wishlist.db.TagItemDBManager;
import com.wish.wishlist.fragment.StaggeredGridActivityFragment;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.model.WishItemManager;
import com.wish.wishlist.AnalyticsHelper;
import com.wish.wishlist.util.DialogOnShowListener;
import com.wish.wishlist.util.PositionManager;
import com.wish.wishlist.util.camera.PhotoFileCreater;
import com.wish.wishlist.util.camera.CameraManager;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.app.ProgressDialog;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/*** EditItemInfo.java is responsible for reading in the info. of a newly added item 
 * including its name, description, time, price, location and photo, and saving them
 * as a row in the Item table in the database
 */
@SuppressLint("NewApi")
public class EditItem extends Activity implements Observer {

	private EditText _itemNameEditText;
	private EditText _noteEditText;
	private EditText _priceEditText;
	private EditText _storeEditText;
	private EditText _locationEditText;
	private CheckBox _completeCheckBox;

	private ImageButton _backImageButton;
	private ImageButton _saveImageButton;
	private ImageButton _mapImageButton;
	private ImageButton _cameraImageButton;
	private ImageButton _galleryImageButton;
	private ImageView _imageItem;
	private Date mDate;
	private double _lat = Double.MIN_VALUE;
	private double _lng = Double.MIN_VALUE;
	private String _ddStr = "unknown";
    private Uri _selectedPicUri = null;
	private String _fullsizePhotoPath = null;
	private String _newfullsizePhotoPath = null;
	private StoreDBManager _storeDBManager;
	private LocationDBManager _locationDBManager;
	PositionManager _pManager;
	private int mYear = -1;
	private int mMonth = -1;
	private int mDay = -1;
	private int mHour = 0;
	private int mMin = 0;
	private int mSec = 0;
	private long mItem_id = -1;
	private long mLocation_id = -1;
	private long mStore_id = -1;
	private int _complete = -1;
	private boolean _editNew = true;
	private boolean _isGettingLocation = false;
    private ArrayList<String> _tags = new ArrayList<String>();
    private Bitmap _webBitmap = null;
	
	private static final int TAKE_PICTURE = 1;
	private static final int SELECT_PICTURE = 2;
    private static final int ADD_TAG = 3;
    private static final int GET_WEB_IMAGE = 4;
    private Boolean _selectedPic = false;

    private static ArrayList<WebImage> _webImages = new ArrayList<WebImage>();

    static final public String IMG_URLS = "IMG_URLS";
    static final public String FULLSIZE_PHOTO_PATH = "FULLSIZE_PHOTO_PATH";
    static final public String NEW_FULLSIZE_PHOTO_PATH = "NEW_FULLSIZE_PHOTO_PATH";
    static final public String SELECTED_PIC_URL = "SELECTED_PIC_URL";

    private class WebResult {
        public ArrayList<WebImage> _webImages = new ArrayList<WebImage>();
        public String _title;
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.edit_item);

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
		
		// Open the Store table in the database		
		_storeDBManager = new StoreDBManager(this);
		_storeDBManager.open();

		// Open the Location table in the database
		_locationDBManager = new LocationDBManager(this);
		_locationDBManager.open();
		
		_pManager = new PositionManager(EditItem.this);
		_pManager.addObserver(this);

		//find the resources by their ids
		_itemNameEditText = (EditText) findViewById(R.id.itemname);
		_noteEditText = (EditText) findViewById(R.id.note);
		_priceEditText = (EditText) findViewById(R.id.price);
		_storeEditText = (EditText) findViewById(R.id.store);
		_locationEditText = (EditText) findViewById(R.id.location);
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
        int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
        _imageItem.setLayoutParams(new LinearLayout.LayoutParams(screenWidth/3, screenWidth/3));
        _imageItem.setScaleType(ImageView.ScaleType.CENTER_CROP);

        _imageItem.setOnClickListener(new OnClickListener() {
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

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("*/*".equals(type)) {
                handleSendAll(intent);
            } else if ("text/plain".equals(type)) {
                handleSendText(intent); // Handle text being sent
            } else if (type.startsWith("image/")) {
                handleSendImage(intent); // Handle single image being sent
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                handleSendMultipleImages(intent); // Handle multiple images being sent
            }
        } else {
            //Handle other intents, such as being started from the home screen
            //get the fullsizephotopatch, if it is not null, EdiItemInfo is launched from
            //dashboard camera
            _fullsizePhotoPath = intent.getStringExtra(FULLSIZE_PHOTO_PATH);
            setTakenPhoto();
            if (intent.getStringExtra(SELECTED_PIC_URL) != null) {
                _selectedPicUri = Uri.parse(intent.getStringExtra(SELECTED_PIC_URL));
                setSelectedPic();
            }
        }

		//get item id from previous intent, if there is an item id, we know this EditItemInfo is launched
		//by editing an existing item, so fill the empty box
		mItem_id = intent.getLongExtra("item_id", -1);
		
		if (mItem_id != -1) {
			_editNew = false;

			_mapImageButton.setVisibility(View.GONE);
			_completeCheckBox.setVisibility(View.VISIBLE);
			
			WishItem item = WishItemManager.getInstance(this).retrieveItembyId(mItem_id);
			mLocation_id = item.getLocatonId();
			mStore_id = item.getStoreId();
			_complete = item.getComplete();
			if (_complete == 1) {
				_completeCheckBox.setChecked(true);
			}
			else {
				_completeCheckBox.setChecked(false);
			}

			_itemNameEditText.setText(item.getName());
			_noteEditText.setText(item.getDesc());
			String priceStr = item.getPriceAsString();
			if (priceStr != null) {
				_priceEditText.setText(priceStr);
			}
			_locationEditText.setText(item.getAddress());
			_storeEditText.setText(item.getStoreName());
			_fullsizePhotoPath = item.getFullsizePicPath();
            if (_fullsizePhotoPath != null) {
                Picasso.with(this).load(new File(_fullsizePhotoPath)).fit().centerCrop().into(_imageItem);
                 _imageItem.setVisibility(View.VISIBLE);
            }
            _tags = TagItemDBManager.instance(this).tags_of_item(mItem_id);
		}
		
		else { //we are editing a new wish, get the location in background
			boolean tagLocation = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("autoLocation", true);
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

		if (savedInstanceState != null) {
			// restore the current selected item in the list
			_newfullsizePhotoPath = savedInstanceState.getString(NEW_FULLSIZE_PHOTO_PATH);
			_fullsizePhotoPath = savedInstanceState.getString(FULLSIZE_PHOTO_PATH);
            if (intent.getStringExtra(SELECTED_PIC_URL) != null) {
                _selectedPicUri = Uri.parse(intent.getStringExtra(SELECTED_PIC_URL));
            }
            if (_fullsizePhotoPath != null) {
                Picasso.with(this).load(new File(_fullsizePhotoPath)).fit().centerCrop().into(_imageItem);
            } else {
                Picasso.with(this).load(_selectedPicUri).fit().centerCrop().into(_imageItem);
            }
		}
	}

    void handleSendAll(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            _noteEditText.setText(sharedText);
        }
        _selectedPicUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        setSelectedPic();
    }

    void handleSendText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            ArrayList<String> links = extractLinks(sharedText);
            if (links.isEmpty()) {
                _itemNameEditText.setText(sharedText);
                return;
            }
            String url = links.get(0);
            // remove the link from the text;
            String name = sharedText.replace(url, "");
            _itemNameEditText.setText(name);
            new getImageAsync().execute(url);
        }
    }

    void handleSendImage(Intent intent) {
        _selectedPicUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        setSelectedPic();
    }

    void handleSendMultipleImages(Intent intent) {
        ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (imageUris != null) {
            // Update UI to reflect multiple images being shared
        }
    }
    private class getImageAsync extends AsyncTask<String, Integer, WebResult> {
        ProgressDialog asyncDialog = new ProgressDialog(EditItem.this);

        protected WebResult doInBackground(String... urls) {
            WebResult result = new WebResult();
            try {
                Document doc = Jsoup.connect(urls[0]).get();
                result._title = doc.title();
                Elements img = doc.getElementsByTag("img");

                for (Element el : img) {
                    String src = el.absUrl("src");
                    // width and height can be in the format of 100px,
                    // remove the non digit part of the string.
                    //String width = el.attr("width").replaceAll("[^0-9]", "");
                    //String height = el.attr("height").replaceAll("[^0-9]", "");
                    String style = el.attr("style");

                    // filter out hidden images, gif and png images. (gif and png are usually used for icons etc.)
                    if (src.isEmpty() || style.contains("display:none") || src.endsWith(".gif") || src.endsWith(".png")) {
                        continue;
                    }
                    try {
                        final Bitmap image = Picasso.with(EditItem.this).load(src).get();
                        // filter out small images
                        if (image == null || image.getWidth() <= 100 || image.getHeight() <= 100) {
                            continue;
                        }
                        result._webImages.add(new WebImage(src, image.getWidth(), image.getHeight(), el.id()));
                    } catch (IOException e) { }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPreExecute() {
            asyncDialog.setMessage("Loading images");
            asyncDialog.show();
            super.onPreExecute();
        }

        protected void onProgressUpdate(Integer... progress) {
            //setProgressPercent(progress[0]);
        }

        protected void onPostExecute(WebResult result) {
            asyncDialog.dismiss();
            EditItem._webImages = result._webImages;
            if (_itemNameEditText.getText().toString().isEmpty()) {
                _itemNameEditText.setText(result._title);
            }
            Intent fg_intent = new Intent(EditItem.this, StaggeredGridActivityFragment.class);
            fg_intent.putParcelableArrayListExtra(IMG_URLS, _webImages);
            startActivityForResult(fg_intent, GET_WEB_IMAGE);
        }
    }

    public ArrayList<String> extractLinks(String text) {
        ArrayList<String> links = new ArrayList<String>();
        Matcher m = Patterns.WEB_URL.matcher(text);
        while (m.find()) {
            String url = m.group();
            Log.d("AAA", "URL extracted: " + url);
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

        Tracker t = ((AnalyticsHelper) getApplication()).getTracker(AnalyticsHelper.TrackerName.APP_TRACKER);
        t.send(new HitBuilders.EventBuilder()
                .setCategory("Wish")
                .setAction("Save")
                .build());

		//define variables to hold the item info.
		String itemName = "";
		String itemDesc = "";
		String itemStoreName = "";
		double itemPrice = 0;
		int itemPriority = 0;
		int itemComplete = 0;
		
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

		// user did not specify date_time, use dddd"now" as default date_time
		if (mYear == -1) {
			// get the current date_time
			final Calendar c = Calendar.getInstance();
			mYear = c.get(Calendar.YEAR);
			mMonth = c.get(Calendar.MONTH);
			mDay = c.get(Calendar.DAY_OF_MONTH);
			mHour = c.get(Calendar.HOUR_OF_DAY);//24 hour format
			mMin = c.get(Calendar.MINUTE);
			mSec = c.get(Calendar.SECOND);
		}

		// Format the date_time and save it as a string 
		mDate = new Date(mYear - 1900, mMonth, mDay, mHour, mMin, mSec);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String date = sdf.format(mDate);

		if (_editNew) {//we are creating a new item
			// insert the location to the Location table in database
			mLocation_id = _locationDBManager.addLocation(_lat, _lng, _ddStr, -1, "N/A", "N/A", "N/A", "N/A", "N/A");

			// insert the store to the Store table in database, linked to the location
			mStore_id = _storeDBManager.addStore(itemStoreName, mLocation_id);
		}
		else {//we are editing an existing item
			_storeDBManager.updateStore(mStore_id, itemStoreName, mLocation_id);
		}

        if (_webBitmap != null) {
            _fullsizePhotoPath = saveBitmapToAlbum(_webBitmap);
        } else if (_selectedPic && _selectedPicUri != null) {
            _fullsizePhotoPath = copyPhotoToAlbum(_selectedPicUri);
        }
		WishItem item = new WishItem(this, mItem_id, mStore_id, itemStoreName, itemName, itemDesc,
				date, null, _fullsizePhotoPath, itemPrice, _lat, _lng,
				_ddStr, itemPriority, itemComplete);

		mItem_id = item.save();

        //save the tags of this item
        TagItemDBManager.instance(EditItem.this).Update_item_tags(mItem_id, _tags);

		//close this activity
		Intent resultIntent = new Intent();
		resultIntent.putExtra("itemID", mItem_id);
		setResult(RESULT_OK, resultIntent);
		finish();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case TAKE_PICTURE: {
				if (resultCode == RESULT_OK) {
					_fullsizePhotoPath = String.valueOf(_newfullsizePhotoPath);
					_newfullsizePhotoPath = null;
                    Tracker t = ((AnalyticsHelper) getApplication()).getTracker(AnalyticsHelper.TrackerName.APP_TRACKER);
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
                    Tracker t = ((AnalyticsHelper) getApplication()).getTracker(AnalyticsHelper.TrackerName.APP_TRACKER);
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
            case GET_WEB_IMAGE: {
                if (resultCode == RESULT_OK) {
                    int position = data.getIntExtra("position", -1);
                    setWebPic(_webImages.get(position).mUrl);
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
			int len = 0;
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
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

    private String saveBitmapToAlbum(Bitmap bitmap) {
        try {
            //save the image to a file we created in wishlist album
            File f = PhotoFileCreater.getInstance().setUpPhotoFile(false);
            String path = f.getAbsolutePath();
            OutputStream stream = new FileOutputStream(path);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream);
            stream.flush();
            stream.close();
            return path;
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
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
        _imageItem.setVisibility(View.VISIBLE);
        Picasso.with(this).load(new File(_fullsizePhotoPath)).fit().centerCrop().into(_imageItem);
        _selectedPicUri = null;
        _selectedPic = false;
        return true;
	}

    private Boolean setSelectedPic() {
        if (_selectedPicUri == null) {
            return false;
        }
        _imageItem.setVisibility(View.VISIBLE);
        Picasso.with(this).load(_selectedPicUri).fit().centerCrop().into(_imageItem);
        _fullsizePhotoPath = null;
        _selectedPic = true;
        return true;
    }

    private Boolean setWebPic(String url) {
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
        _selectedPic = false;
        return true;
    }

	//this will make the photo taken before to show up if user cancels taking a second photo
	//this will also save the thumbnail on switching screen orientation
	@Override
	protected void onSaveInstanceState(Bundle savedInstanceState) {
        if (_newfullsizePhotoPath !=null) {
            savedInstanceState.putString(NEW_FULLSIZE_PHOTO_PATH, _newfullsizePhotoPath.toString());
        }
        if (_selectedPicUri != null) {
            savedInstanceState.putString(SELECTED_PIC_URL, _selectedPicUri.toString());
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
            if (!setTakenPhoto()) {
                setSelectedPic();
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
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			findViewById(R.id.addItemView_header).setVisibility(View.GONE);
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
		else {
			// we use the header instead of action bar for GingerBread and lower
			findViewById(R.id.addItemView_header).findViewById(R.id.imageButton_back_logo).setVisibility(View.VISIBLE);
			findViewById(R.id.addItemView_header).findViewById(R.id.imageButton_save).setVisibility(View.VISIBLE);

			_backImageButton = (ImageButton) findViewById(R.id.imageButton_back_logo);
			_saveImageButton = (ImageButton) findViewById(R.id.imageButton_save);

			_backImageButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					navigateBack();
				}
			});
			_saveImageButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					saveWishItem();
				}
			});
		}
	}
}

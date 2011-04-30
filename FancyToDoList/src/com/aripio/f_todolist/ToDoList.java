package com.aripio.f_todolist;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

public class ToDoList extends Activity {
	//menu order number
	static final private int ADD_NEW_TODO = Menu.FIRST;
	static final private int REMOVE_TODO = Menu.FIRST + 1;
	static final private int CHK_TODO = Menu.FIRST+2;
	
	private static final String TEXT_ENTRY_KEY = "TEXT_ENTRY_KEY";
	private static final String ADDING_ITEM_KEY = "ADDING_ITEM_KEY";
	private static final String SELECTED_INDEX_KEY = "SELECTED_INDEX_KEY";
	
	
	private boolean addingNew = false;
	private ListView myListView;
	private ArrayList<ToDoItem> todoItems;
	private EditText myEditText;
	private ToDoItemAdapter aa;
	private ToDoDBAdapter toDoDBAdapter;
	private Cursor toDoListCursor;

	private LocationManager mLocationManager;
	private Location mLocation;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		myListView = (ListView) findViewById(R.id.myListView);
		myEditText = (EditText) findViewById(R.id.myEditText);

		todoItems = new ArrayList<ToDoItem>();

		int resID = R.layout.todoitem_rel;
		aa = new ToDoItemAdapter(this, resID, todoItems);
		myListView.setAdapter(aa);

		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setPowerRequirement(Criteria.POWER_LOW);
		//String locationprovider = mLocationManager.getBestProvider(criteria,
				//true);
		String locationprovider = LocationManager.GPS_PROVIDER;
		mLocation = mLocationManager.getLastKnownLocation(locationprovider);

		myEditText.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN)
					if (keyCode == KeyEvent.KEYCODE_ENTER) {
						try {
							Geocoder mGC = new Geocoder(
									getApplicationContext(), Locale.ENGLISH);
							List<Address> addresses;
							if(mLocation != null)
							{
								addresses = mGC.getFromLocation(
										mLocation.getLatitude(), mLocation.getLongitude(), 1);
							}
							else
								addresses = mGC.getFromLocation(
										40.88301, -72.9795, 1);
									
							StringBuilder addr = new StringBuilder();
							Address currentAddr = null;
							if (addresses != null) {
								currentAddr = addresses.get(0);

								for (int i = 0; i < currentAddr
										.getMaxAddressLineIndex(); i++) {
									addr.append(currentAddr.getAddressLine(i));
									if (i != currentAddr
											.getMaxAddressLineIndex() - 1)
										addr.append("\n");
								}
							}
							ToDoItem newItem = new ToDoItem(myEditText
									.getText().toString(), addr.toString());
							toDoDBAdapter.insertTask(newItem);
							updateArray();
							aa.notifyDataSetChanged();
							myEditText.setText("");
							cancelAdd();
						} catch (IOException e) {
							e.printStackTrace();
						}
						return true;
					}
				return false;
			}
		});
		registerForContextMenu(myListView);
		restoreUIState();

		toDoDBAdapter = new ToDoDBAdapter(this);
		// Open or create the database
		toDoDBAdapter.open();
		populateTodoList();
	}
    
    private void populateTodoList() {
    // Get all the todo list items from the database.
    	toDoListCursor = toDoDBAdapter. getAllToDoItemsCursor();
    	startManagingCursor(toDoListCursor);
    // Update the array.
    	updateArray();
    }

	private void updateArray() {
		toDoListCursor.requery();
		todoItems.clear();
		if (toDoListCursor.moveToFirst())
			do {
				String task = toDoListCursor
						.getString(ToDoDBAdapter.TASK_COLUMN);
				long created = toDoListCursor
						.getLong(ToDoDBAdapter.CREATION_DATE_COLUMN);
				
				String addr = toDoListCursor.getString(ToDoDBAdapter.ADDR_COLUMN);
				ToDoItem newItem = new ToDoItem(task, new Date(created),addr);
				todoItems.add(0, newItem);
			} while (toDoListCursor.moveToNext());
		aa.notifyDataSetChanged();
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuItem itemAdd = menu.add(0, ADD_NEW_TODO, Menu.NONE, R.string.add_new);
		MenuItem itemRem = menu.add(0, REMOVE_TODO, Menu.NONE, R.string.remove);
		itemAdd.setShortcut('0', 'a');
		itemRem.setShortcut('1', 'r');
		
		return true;
	}
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.setHeaderTitle("Selected To Do Item");
		menu.add(0, REMOVE_TODO, Menu.NONE, R.string.remove);
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		int idx = myListView.getSelectedItemPosition();
		String removeTitle = getString(addingNew ? R.string.cancel : R.string.remove);
		MenuItem removeItem = menu.findItem(REMOVE_TODO);
		removeItem.setTitle(removeTitle);
		removeItem.setVisible(addingNew || idx > -1);
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		
		int index = myListView.getSelectedItemPosition();
		switch (item.getItemId()) {
		case (REMOVE_TODO): {
			if (addingNew) {
				cancelAdd();
			} else {
				removeItem(index);
			}
			return true;
		}
		case (ADD_NEW_TODO): {
			addNewItem();
			return true;
		}
		}
		return false;
	}
	
	private void addNewItem() {
		addingNew = true;
		myEditText.setVisibility(View.VISIBLE);
		myEditText.requestFocus();
		
	}
	private void removeItem(int index) {
		// Items are added to the listview in reverse order, so invert the index.
		toDoDBAdapter.removeTask(todoItems.size()-index);
		updateArray();
		aa.notifyDataSetChanged();
		
	}
	private void cancelAdd() {
		addingNew = false;
		myEditText.setVisibility(View.GONE);
		
	}
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		super.onContextItemSelected(item);
		switch (item.getItemId()) {
		case (REMOVE_TODO): {
			AdapterView.AdapterContextMenuInfo menuInfo;
			menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
			int index = menuInfo.position;
			removeItem(index);
			return true;
			}
		}
		return false;
	}
	@Override
	protected void onPause() {
		super.onPause();
		// Get the activity preferences object.
		SharedPreferences uiState = getPreferences(0);
		// Get the preferences editor.
		SharedPreferences.Editor editor = uiState.edit();
		// Add the UI state preference values.
		editor.putString(TEXT_ENTRY_KEY, myEditText.getText().toString());
		editor.putBoolean(ADDING_ITEM_KEY, addingNew);
		// Commit the preferences.
		editor.commit();
	}
	
	private void restoreUIState() {
		// Get the activity preferences object.
		SharedPreferences settings = getPreferences(Activity.MODE_PRIVATE);
		// Read the UI state values, specifying default values.
		String text = settings.getString(TEXT_ENTRY_KEY, "");
		Boolean adding = settings.getBoolean(ADDING_ITEM_KEY, false);
		// Restore the UI to the previous state.
		if (adding) {
			addNewItem();
			myEditText.setText(text);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(SELECTED_INDEX_KEY,
				myListView.getSelectedItemPosition());

	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		int pos = -1;
		if (savedInstanceState != null)
			if (savedInstanceState.containsKey(SELECTED_INDEX_KEY))
				pos = savedInstanceState.getInt(SELECTED_INDEX_KEY, -1);
		myListView.setSelection(pos);
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
		// Close the database
		toDoDBAdapter.close();
	}
}
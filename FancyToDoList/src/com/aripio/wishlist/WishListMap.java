package com.aripio.wishlist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Debug;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;

public class WishListMap extends MapActivity {

	private MapView mvMap;
    private WishListDataBase db;
    private MyLocationOverlay mMyLocationOverlay;
    private int latitude, longitude;
    
    private class WishListMapOverlay extends Overlay{      
      
        @Override
        public void draw(Canvas canvas, MapView mapView, boolean shadow) {
            if (!shadow) {
                Point point = new Point();
                Projection p = mapView.getProjection();
                
                final Location myLocation
                = getCurrentLocation((LocationManager) getSystemService(Context.LOCATION_SERVICE));
                GeoPoint myPoint = new GeoPoint((int)(myLocation.getLongitude()*1000000), (int)(myLocation.getLatitude()*1000000));
                
                p.toPixels(myPoint, point);
                super.draw(canvas, mapView, shadow);
                drawAt(canvas, getResources().getDrawable(R.drawable.map_pin), point.x, point.y, shadow);
            }
        }
    }
    
	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
        setContentView(R.layout.map);

        db = WishListDataBase.getDBInstance(this);
//
//        // Get current position
        final Location myLocation
            = getCurrentLocation((LocationManager) getSystemService(Context.LOCATION_SERVICE));

        Spinner spnLocations = (Spinner) findViewById(R.id.spnLocations);
        mvMap = (MapView) findViewById(R.id.mapmain);

//        // get the map controller
        final MapController mc = mvMap.getController();

        mMyLocationOverlay = new MyLocationOverlay(this, mvMap);
        mMyLocationOverlay.enableMyLocation();
        mMyLocationOverlay.enableCompass(); 
        mMyLocationOverlay.runOnFirstFix(
            new Runnable() {
                public void run() {
                    mc.animateTo(mMyLocationOverlay.getMyLocation());
                    mc.setZoom(16);
                }
            });

        Drawable marker = getResources().getDrawable(R.drawable.android_tiny_image);
        marker.setBounds(0, 0, marker.getIntrinsicWidth(), marker.getIntrinsicHeight());
        mvMap.getOverlays().add(new WishListMapOverlay());

        mvMap.setClickable(true);
        mvMap.setEnabled(true);
        mvMap.setSatellite(false);
        mvMap.setTraffic(false);
        mvMap.setStreetView(false);
        
        // start out with a general zoom
        mc.setZoom(16);
        mvMap.invalidate();

        // Create a button click listener for the List Jobs button.
        Button btnList = (Button) findViewById(R.id.btnShowList);
        btnList.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(WishListMap.this.getApplicationContext(), WishList.class);
                startActivity(intent);
            }
        });

        // Load a HashMap with locations and positions
        List<String> lsLocations = new ArrayList<String>();
        final HashMap<String, GeoPoint> hmLocations = new HashMap<String, GeoPoint>();
        hmLocations.put("Current Location", new GeoPoint(latitude, longitude));
        lsLocations.add("Current Location");

        // Add favorite locations from this user's record in workers table
//        worker = db.getWorker();
//        hmLocations.put(worker.getColLoc1Name(), new GeoPoint((int)worker.getColLoc1Lat(), (int)worker.getColLoc1Long()));
//        lsLocations.add(worker.getColLoc1Name());
//        hmLocations.put(worker.getColLoc2Name(), new GeoPoint((int)worker.getColLoc2Lat(), (int)worker.getColLoc2Long()));
//        lsLocations.add(worker.getColLoc2Name());
//        hmLocations.put(worker.getColLoc3Name(), new GeoPoint((int)worker.getColLoc3Lat(), (int)worker.getColLoc3Long()));
//        lsLocations.add(worker.getColLoc3Name());  
        hmLocations.put("BeiJing", new GeoPoint(39904214,116407413));
        lsLocations.add("BeiJing");  
        hmLocations.put("ShangHai", new GeoPoint(31230393,121473704));
        lsLocations.add("ShangHai"); 
        
        ArrayAdapter<String> aspnLocations
            = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, lsLocations);
        aspnLocations.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnLocations.setAdapter(aspnLocations);
        
        // Setup a callback for the spinner
        spnLocations.setOnItemSelectedListener(
            new OnItemSelectedListener() {
                public void onNothingSelected(AdapterView<?> arg0) { }

                public void onItemSelected(AdapterView<?> parent, View v, int position, long id)  {
                    TextView vt = (TextView) v;
                    if ("Current Location".equals(vt.getText())) {
                    	mMyLocationOverlay.enableMyLocation();
                    	try {
                    		mc.animateTo(mMyLocationOverlay.getMyLocation());
                    	}
                    	catch (Exception e) {
                    		Log.i("MicroJobs", "Unable to animate map", e);
                    	}
                    	mvMap.invalidate();
                    } else {
                    	mMyLocationOverlay.disableMyLocation();
                        mc.animateTo(hmLocations.get(vt.getText()));
                    }
                    mvMap.invalidate();
                }
            });
	}

	  /**
     * @return the current location
     */
    private Location getCurrentLocation(LocationManager lm) {
        Location l = lm.getLastKnownLocation("gps");
        if (null != l) { return l; }

        // getLastKnownLocation returns null if loc provider is not enabled
        l = new Location("gps");
        l.setLatitude(42.352299);
        l.setLatitude(-71.063979);
        return l;
    }

    protected GeoPoint setCurrentGeoPoint(){
    	return mMyLocationOverlay.getMyLocation();
    }
    
    /**
     * @see com.google.android.maps.MapActivity#onPause()
     */
    @Override
    public void onPause() {
        super.onPause();
        mMyLocationOverlay.disableMyLocation();
    }

    // stop tracing when application ends
    @Override
    public void onDestroy() {
        super.onDestroy();
        Debug.stopMethodTracing();
    }

    /**
     * @see com.google.android.maps.MapActivity#onResume()
     */
    @Override
    public void onResume() {
        super.onResume();
        mMyLocationOverlay.enableMyLocation();
    }

    /**
     * Setup menus for this page
     *
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean supRetVal = super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, 0, Menu.NONE, getString(R.string.map_menu_zoom_in));
        menu.add(Menu.NONE, 1, Menu.NONE, getString(R.string.map_menu_zoom_out));
        menu.add(Menu.NONE, 2, Menu.NONE, getString(R.string.map_menu_set_satellite));
        menu.add(Menu.NONE, 3, Menu.NONE, getString(R.string.map_menu_set_map));
        menu.add(Menu.NONE, 4, Menu.NONE, getString(R.string.map_menu_set_traffic));
        menu.add(Menu.NONE, 5, Menu.NONE, getString(R.string.map_menu_show_list));
        return supRetVal;
    }

    /**
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                // Zoom in
                zoomIn();
                return true;
            case 1:
                // Zoom out
                zoomOut();
                return true;
            case 2:
                // Toggle satellite views
                mvMap.setSatellite(!mvMap.isSatellite());
                return true;
            case 3:
                // Toggle street views
                mvMap.setStreetView(!mvMap.isStreetView());
                return true;
            case 4:
                // Toggle traffic views
                mvMap.setTraffic(!mvMap.isTraffic());
                return true;
            case 5:
                // Show the job list activity
                startActivity(new Intent(this, WishList.class));
                return true;
        }
        return false;
    }

    /**
     * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP: // zoom in
                zoomIn();
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN: // zoom out
                zoomOut();
                return true;
            case KeyEvent.KEYCODE_BACK: // go back (meaning exit the app)
                finish();
                return true;
            default:
                return false;
        }
    }

   

    /**
     * Zoom in on the map
     */
    private void zoomIn() {
        mvMap.getController().setZoom(mvMap.getZoomLevel() + 1);
    }

    /**
     * Zoom out on the map, but not past level 10
     */
    private void zoomOut() {
        int zoom = mvMap.getZoomLevel() - 1;
        if (zoom < 5) { zoom = 5; }
        mvMap.getController().setZoom(zoom);
    }
    
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
}
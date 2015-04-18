package com.wish.wishlist.activity;

//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.lang.Math;

//import org.apache.http.HttpEntity;
//import org.apache.http.HttpResponse;
//import org.apache.http.client.HttpClient;
//import org.apache.http.client.methods.HttpGet;
//import org.apache.http.impl.client.DefaultHttpClient;
//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;

import android.content.Intent;
//import android.database.Cursor;
//import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
//import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.wish.wishlist.R;
//import com.android.wishlist.R.drawable;
//import com.android.wishlist.R.string;
//import com.wish.wishlist.db.LocationDBManager;
//import com.wish.wishlist.db.StoreDBManager;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;
import com.wish.wishlist.db.ItemDBManager;
import com.wish.wishlist.AnalyticsHelper;

/**
 * Displays a custom map which shows our current location and the location where
 * the photo was taken.
 */
public class WishListMap extends MapActivity {
    private MapView _mapView;
    //	private MyLocationOverlay mMyLocationOverlay;
    private WishListOverlay _wishListOverlay;
    //	private Location myLocation;
    private double _latitude;
    private double _longitude;
    private GeoPoint _currentPoint;
    private Drawable _marker;
    private int _markerXOffset;
    private int _markerYOffset;
    private List<Overlay> _overlays;
    private MapController _controller;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Tracker t = ((AnalyticsHelper) getApplication()).getTracker(AnalyticsHelper.TrackerName.APP_TRACKER);
        t.setScreenName("MapView");
        t.send(new HitBuilders.AppViewBuilder().build());

        FrameLayout frame = new FrameLayout(this);
        _mapView = new MapView(this, getString(R.string.googleMapKey));

        frame.addView(_mapView, new FrameLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        setContentView(frame);
        Intent i = getIntent();
        prepareMarker();

        _overlays = _mapView.getOverlays();
        _controller = _mapView.getController();
        if(i.getStringExtra("type").equals("markOne")){
            // mark one item on map, mapview is invoked from item context menu
            markOneItem();
        }

        else if(i.getStringExtra("type").equals("markAll")){
            markAllItems();//map view is invoked from main menu->map
        }

        GeoPoint p = _wishListOverlay.getSpanCenter();
        if (p == null) {
            return;
        }

        _controller.animateTo(p);//what if there is no item?
        // mark the current location
        // markCurrentLocation();

        // set map zoom
//		mController.setZoom(15);
        int[] spanE6=_wishListOverlay.getSpanE6(1.2f);
        _controller.zoomToSpan(spanE6[0], spanE6[1]);

        // configure the map
        _mapView.setClickable(true);
        _mapView.setEnabled(true);
        _mapView.setSatellite(false);
        _mapView.setTraffic(false);
        _mapView.setStreetView(false);
        addZoomControls(frame);

        // new NetworkThread(myCurrentPoint, _wishListOverlay).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // mMyLocationOverlay.enableMyLocation();
    }

    @Override
    protected void onStop() {
        // mMyLocationOverlay.disableMyLocation();
        super.onStop();
    }

    /**
     * Get the zoom controls and add them to the bottom of the map
     */
    private void addZoomControls(FrameLayout frame) {
        View zoomControls = _mapView.getZoomControls();

        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM + Gravity.CENTER_HORIZONTAL);
        frame.addView(zoomControls, p);
    }

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    /**
     * @return the current location
     */
    private Location getCurrentLocation(LocationManager lm) {
        Location l = lm.getLastKnownLocation("gps");
        if (null != l) {
            return l;
        }

        // getLastKnownLocation returns null if loc provider is not enabled
        l = new Location("gps");

        // yonge/eglinton
        l.setLatitude(43.706739);
        l.setLongitude(-79.398330);
        return l;
    }

    // /**
    // *
    // * @return
    // */
    // private Location getItemLocation(){
    // mapIntent.putExtra("longitude", dLocation[1]);
    // }

    /**
     * mark the current location on the map
     */
//	private void markCurrentLocation() {
//		mMyLocationOverlay = new MyLocationOverlay(this, mMapView);
//		boolean locationEnabled = false;
//		boolean compassEnabled = false;
//
//		locationEnabled = mMyLocationOverlay.enableMyLocation();
//		compassEnabled = mMyLocationOverlay.enableCompass();
//
//		// Get the current location
//		myLocation = getCurrentLocation((LocationManager) getSystemService(Context.LOCATION_SERVICE));
//		myCurrentPoint = new GeoPoint(
//				(int) (myLocation.getLatitude() * 1000000), (int) (myLocation
//						.getLongitude() * 1000000));
//
//		mOverlays.add(mMyLocationOverlay);
//	}

    /**
     * prepare the marker used to mark the item
     */
    private void prepareMarker() {
        _marker = getResources().getDrawable(R.drawable.map_pin);

        // Make sure to give mMarker bounds so it will draw in the overlay
        final int intrinsicWidth = _marker.getIntrinsicWidth();
        final int intrinsicHeight = _marker.getIntrinsicHeight();
        _marker.setBounds(0, 0, intrinsicWidth, intrinsicHeight);

        // mMarkerXOffset = -(intrinsicWidth / 2);
        // mMarkerYOffset = -intrinsicHeight;

    }

    /**
     * mark one item on the map
     */
    private void markOneItem() {
        // Read the item we are displaying from the intent, along with the
        // parameters used to set up the map
        Intent i = getIntent();
        _latitude = i.getDoubleExtra("latitude", Double.MIN_VALUE);
        _longitude = i.getDoubleExtra("longitude", Double.MIN_VALUE);

        _wishListOverlay = new WishListOverlay(_marker);
        GeoPoint itemPoint = new GeoPoint((int) (_latitude * 1000000),
                (int) (_longitude * 1000000));
        _wishListOverlay.addOverlay(new OverlayItem(itemPoint, "A", "B"));
        _overlays.add(_wishListOverlay);
    }

    private void markAllItems() {
        // Read all item location from db
        ItemDBManager mItemDBManager = new ItemDBManager(this);
        mItemDBManager.open();
        ArrayList<double[]> locationList = new ArrayList<double[]>();
        locationList = mItemDBManager.getAllItemLocation();
        mItemDBManager.close();
        if(locationList.isEmpty()) {
            Toast toast = Toast.makeText(this, "No wish available on map", Toast.LENGTH_SHORT);
            toast.show();
            this.finish();
        }

        _wishListOverlay = new WishListOverlay(_marker);
        int i=0;
        while(i<locationList.size()){
            _latitude=locationList.get(i)[0];
            _longitude=locationList.get(i++)[1];
            GeoPoint itemPoint = new GeoPoint((int) (_latitude * 1000000),
                    (int) (_longitude * 1000000));
            _wishListOverlay.addOverlay(new OverlayItem(itemPoint, "A", "B"));
            _overlays.add(_wishListOverlay);
        }
    }

    /**
     * Custom overlay to display the pushpin as a marker for the item
     */
    public class WishListOverlay extends ItemizedOverlay<OverlayItem> {
        private List<OverlayItem> items = new ArrayList<OverlayItem>();
        private Drawable marker = null;

        public WishListOverlay(Drawable marker) {
            super(marker);
            this.marker = marker;
//			GeoPoint itemPoint = new GeoPoint((int) (mLatitude * 1000000),
//					(int) (mLongitude * 1000000));
//
//			addOverlay(new OverlayItem(itemPoint, "A", "B"));
            boundCenterBottom(marker);
//			populate();
        }

        public void addOverlay(OverlayItem overlay) {
            items.add(overlay);
            populate();
        }

        @Override
        protected OverlayItem createItem(int i) {
            return (items.get(i));
        }

        @Override
        protected boolean onTap(int i) {
            return (true);
        }

        @Override
        public int size() {
            return (items.size());
        }

        /**
         * return the center of all overlays as a GeoPoint
         */
        public GeoPoint getSpanCenter() {
            if(items.size()>0){
                int minLat = Integer.MAX_VALUE;
                int maxLat = Integer.MIN_VALUE;
                int minLng = Integer.MAX_VALUE;
                int maxLng = Integer.MIN_VALUE;
                for (OverlayItem item:items){
                    minLat = Math.min(minLat, item.getPoint().getLatitudeE6());
                    maxLat = Math.max(maxLat, item.getPoint().getLatitudeE6());
                    minLng = Math.min(minLng, item.getPoint().getLongitudeE6());
                    maxLng = Math.max(maxLng, item.getPoint().getLongitudeE6());
                }
                int centerLat = (minLat + maxLat)/2;
                int centerLng = (minLng + maxLng)/2;
                return new GeoPoint(centerLat, centerLng);
            }
            else return null;
        }

        public int[] getSpanE6(float scale){
            return new int[]{ (int) (WishListOverlay.this.getLatSpanE6() * scale),
                    (int) (WishListOverlay.this.getLonSpanE6() * scale) };
        }
    }

    public class MarkerOverlay extends Overlay {
        @Override
        public void draw(Canvas canvas, MapView mapView, boolean shadow) {
            if (!shadow) {
                Point point = new Point();
                Projection p = mapView.getProjection();
                p.toPixels(_currentPoint, point);
                super.draw(canvas, mapView, shadow);
                drawAt(canvas, _marker, point.x + _markerXOffset, point.y
                        + _markerYOffset, shadow);
            }
        }
    }

    /**
     * This thread does the actual work of downloading and parsing data.
     *
     */
//	private class NetworkThread extends Thread {
//
//		private final String query_URL = "//maps.googleapis.com/maps/api/place/search/json?location=%f,%f&radius=500&types=food&name=store"
//				+ "&sensor=true&key=AIzaSyDFtnjfv8bJ8uCU-02J1x8XQ-jFWzhKICE";
//		private GeoPoint searchPoint;
//		private WishListOverlay _wishListOverlay;
//
//		NetworkThread(GeoPoint point, WishListOverlay wishListOverlay) {
//			searchPoint = point;
//			_wishListOverlay = wishListOverlay;
//		}
//
//		@Override
//		public void run() {
//
//			String url = query_URL;
//			url = String.format(url,
//					(double) searchPoint.getLatitudeE6() / 1000000,
//					(double) searchPoint.getLongitudeE6() / 1000000);
//			try {
//				URI uri = new URI("https", url, null);
//				HttpGet get = new HttpGet(uri);
//
//				HttpClient client = new DefaultHttpClient();
//				HttpResponse response = client.execute(get);
//				HttpEntity entity = response.getEntity();
//				String str = convertStreamToString(entity.getContent());
//				JSONObject json = new JSONObject(str);
//				parse(json);
//			} catch (Exception e) {
//				Log.e(WishList.LOG_TAG, e.toString());
//			}
//		}
//
//		private void parse(JSONObject json) {
//			try {
//				JSONArray array = json.getJSONArray("results");
//				int count = array.length();
//				for (int i = 0; i < 5; i++) {
//					JSONObject obj = array.getJSONObject(i);
//					JSONObject gobj = obj.getJSONObject("geometry");
//					JSONObject lobj = gobj.getJSONObject("location");
//					double latitude = lobj.getDouble("lat");
//					double longitude = lobj.getDouble("lng");
//					GeoPoint point = new GeoPoint((int) (latitude * 1000000),
//							(int) (longitude * 1000000));
//					_wishListOverlay
//							.addOverlay(new OverlayItem(point, "A", "B"));
//				}
//			} catch (JSONException e) {
//				Log.e(WishList.LOG_TAG, e.toString());
//			}
//		}
//
//		private String convertStreamToString(InputStream is) {
//			BufferedReader reader = new BufferedReader(
//					new InputStreamReader(is), 8 * 1024);
//			StringBuilder sb = new StringBuilder();
//
//			String line = null;
//			try {
//				while ((line = reader.readLine()) != null) {
//					sb.append(line + "\n");
//				}
//			} catch (IOException e) {
//				e.printStackTrace();
//			} finally {
//				try {
//					is.close();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
//			return sb.toString();
//		}

}

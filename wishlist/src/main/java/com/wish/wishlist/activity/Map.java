package com.wish.wishlist.activity;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.wish.wishlist.db.ItemDBManager;
import com.wish.wishlist.WishlistApplication;

import com.wish.wishlist.R;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.model.WishItemManager;

public class Map extends Activity {
    private GoogleMap mGoogleMap;
    private HashMap<Marker, WishItem> mMarkerItemMap = new HashMap<>();
    private boolean mMarkOne;
    private static final int ITEM_DETAILS = 0;
    LatLngBounds mBounds;
    private static final String TAG = "Map";

    private class MarkerCallback implements Callback {
        private Marker marker = null;

        MarkerCallback(Marker marker) {
            this.marker = marker;
        }

        @Override
        public void onError() {
            Log.e(getClass().getSimpleName(), "Error loading thumbnail!");
        }

        @Override
        public void onSuccess() {
            if (marker != null && marker.isInfoWindowShown()) {
                marker.hideInfoWindow();
                marker.showInfoWindow();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == resultCode) {
            Log.d(TAG, "Google Play Services available");
        } else {
            Log.e(TAG, "Google Play Services not available");
            Toast.makeText(this, "Google Play Services v7.3 are required ", Toast.LENGTH_LONG).show();

            int v = 0;
            try {
                v = getPackageManager().getPackageInfo("com.google.android.gms", 0).versionCode;
                Log.d(TAG, "com.google.android.gms " + v);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, e.toString());
            }

            Tracker t = ((WishlistApplication) getApplication()).getTracker(WishlistApplication.TrackerName.APP_TRACKER);
            t.send(new HitBuilders.EventBuilder()
                    .setCategory("Debug")
                    .setAction("com.google.android.gms: " + Integer.toString(v))
                    .build());

            finish();
            return;
        }

        setContentView(R.layout.map);

        Tracker t = ((WishlistApplication) getApplication()).getTracker(WishlistApplication.TrackerName.APP_TRACKER);
        t.setScreenName("MapView");
        t.send(new HitBuilders.AppViewBuilder().build());

        mGoogleMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        Intent i = getIntent();
        if (i.getStringExtra("type").equals("markOne")){
            mMarkOne = true;
            markOneItem();
        } else if (i.getStringExtra("type").equals("markAll")) {
            mMarkOne = false;
            if (markAllItems()) {
                mGoogleMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
                    @Override
                    public void onCameraChange(CameraPosition arg0) {
                        moveToBounds();
                    }
                });
            }
        }
    }

    void setInfoWindowAdapter() {
        mGoogleMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            // Defines the contents of the InfoWindow
            @Override
            public View getInfoContents(Marker marker) {
                Tracker t = ((WishlistApplication) getApplication()).getTracker(WishlistApplication.TrackerName.APP_TRACKER);
                t.send(new HitBuilders.EventBuilder()
                        .setCategory("Map")
                        .setAction("TapPin")
                        .build());

                View v = getLayoutInflater().inflate(R.layout.info_window_layout, null);

                ImageView thumb = (ImageView) v.findViewById(R.id.map_thumb);

                // we need to refresh the InfoWindow when loading image is complete. the callback object's onSuccess is called
                // when Picasso finishes loading the image, and that's when we can refresh the InfoWindow.
                WishItem item = mMarkerItemMap.get(marker);
                if (item.getFullsizePicPath() == null) {
                    thumb.setVisibility(View.GONE);
                } else {
                    MarkerCallback callback = new MarkerCallback(marker);
                    thumb.setTag(callback);
                    Picasso.with(Map.this).load(new File(item.getFullsizePicPath())).resize(200, 200).centerCrop().into(thumb, callback);
                }

                TextView name = (TextView) v.findViewById(R.id.map_name);
                name.setText(item.getName());

                return v;
            }
        });

        if (mMarkOne) {
            // when we mark single wish on map, we disable tapping on pin to show detail view
            return;
        }
        mGoogleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                Intent intent = new Intent(Map.this, WishItemDetail.class);
                WishItem item = mMarkerItemMap.get(marker);
                intent.putExtra("item_id", item.getId());
                startActivityForResult(intent, ITEM_DETAILS);
            }
        });
    }

    boolean markAllItems()
    {        // Read all item location from db
        ItemDBManager mItemDBManager = new ItemDBManager(this);
        mItemDBManager.open();
        ArrayList<Long> ids = mItemDBManager.getItemsWithLocation();

        mItemDBManager.close();
        if (ids.isEmpty()) {
            Log.d(TAG, "no wishes with location");
            Toast toast = Toast.makeText(this, "No wish available on map", Toast.LENGTH_SHORT);
            toast.show();
            this.finish();
            return false;
        }

        mBounds = addMarkers(ids);//map view is invoked from main menu->map
        setInfoWindowAdapter();
        return true;
    }

    void moveToBounds()
    {
        double width = mBounds.northeast.latitude - mBounds.southwest.latitude;
        double height = mBounds.northeast.longitude - mBounds.southwest.longitude;
        double larger = Math.max(Math.abs(width), Math.abs(height));
        // 0.008 is about the size of an residential area in toronto
        if (larger > 0.008) {
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(mBounds, 150));
        } else {
            // the map will be zoomed too much and won't show nicely, so let's hardcode the zoom level
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mBounds.getCenter(), 15));
        }

        // Remove listener to prevent position reset on camera move.
        mGoogleMap.setOnCameraChangeListener(null);
    }

    private LatLngBounds addMarkers(ArrayList<Long> ids) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Long id : ids) {
            final WishItem item = WishItemManager.getInstance(this).retrieveItemById(id);
            final double lat = item.getLatitude();
            final double lng = item.getLongitude();
            final LatLng point = new LatLng(lat, lng);

            Marker marker = mGoogleMap.addMarker(new MarkerOptions().position(point));
            mMarkerItemMap.put(marker, item);
            builder.include(point);
        }

        LatLngBounds bounds = builder.build();
        return bounds;
    }

    /**
     * mark one item on the map
     */
    private void markOneItem() {
        // Read the item we are displaying from the intent, along with the
        // parameters used to set up the map
        Intent i = getIntent();
        final long id = i.getLongExtra("id", -1);
        final WishItem item = WishItemManager.getInstance(this).retrieveItemById(id);
        if (item == null) {
            finish();
            return;
        }
        final double lat = item.getLatitude();
        final double lng = item.getLongitude();

        final LatLng point = new LatLng(lat, lng);
        Marker marker = mGoogleMap.addMarker(new MarkerOptions().position(point));
        mMarkerItemMap.put(marker, item);
        setInfoWindowAdapter();
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(point, 15));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ITEM_DETAILS: {
                Log.d(TAG, "onActivityResult");
                mGoogleMap.clear();
                if (markAllItems()) {
                    moveToBounds();
                }
                break;
            }
        }
    }
}


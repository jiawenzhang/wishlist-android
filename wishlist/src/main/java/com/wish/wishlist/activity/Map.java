package com.wish.wishlist.activity;

import android.app.Activity;
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
import com.wish.wishlist.AnalyticsHelper;

import com.wish.wishlist.R;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.model.WishItemManager;

public class Map extends Activity {
    private GoogleMap mGoogleMap;
    private HashMap<Marker, WishItem> mMarkerItemMap = new HashMap<>();
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
        setContentView(R.layout.map);

        Tracker t = ((AnalyticsHelper) getApplication()).getTracker(AnalyticsHelper.TrackerName.APP_TRACKER);
        t.setScreenName("MapView");
        t.send(new HitBuilders.AppViewBuilder().build());

        mGoogleMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        Intent i = getIntent();
        if (i.getStringExtra("type").equals("markOne")){
            markOneItem();
        } else if (i.getStringExtra("type").equals("markAll")) {
            markAllItems();
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
                View v = getLayoutInflater().inflate(R.layout.info_window_layout, null);

                ImageView thumb = (ImageView) v.findViewById(R.id.map_thumb);
                MarkerCallback callback = new MarkerCallback(marker);
                thumb.setTag(callback);

                // we need to refresh the InfoWindow when loading image is complete. the callback object's onSuccess is called
                // when Picasso finishes loading the image, and that's when we can refresh the InfoWindow.
                WishItem item = mMarkerItemMap.get(marker);
                Picasso.with(Map.this).load(new File(item.getFullsizePicPath())).resize(200, 200).centerCrop().into(thumb, callback);

                TextView name = (TextView) v.findViewById(R.id.map_name);
                name.setText(item.getName());

                return v;
            }
        });

    }

    void markAllItems() {
        final LatLngBounds bounds = getBounds();//map view is invoked from main menu->map

        setInfoWindowAdapter();
        mGoogleMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition arg0) {
                double width = bounds.northeast.latitude - bounds.southwest.latitude;
                double height = bounds.northeast.longitude - bounds.southwest.longitude;
                double larger = Math.max(Math.abs(width), Math.abs(height));
                // 0.008 is about the size of an residential area in toronto
                if (larger > 0.008) {
                    mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));
                } else {
                    // the map will be zoomed too much and won't show nicely, so let's hardcode the zoom level
                    mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(bounds.getCenter(), 15));
                }

                // Remove listener to prevent position reset on camera move.
                mGoogleMap.setOnCameraChangeListener(null);
            }
        });
    }

    private LatLngBounds getBounds() {
        // Read all item location from db
        ItemDBManager mItemDBManager = new ItemDBManager(this);
        mItemDBManager.open();
        ArrayList<Long> ids = mItemDBManager.getItemsWithLocation();

        mItemDBManager.close();
        if (ids.isEmpty()) {
            Toast toast = Toast.makeText(this, "No wish available on map", Toast.LENGTH_SHORT);
            toast.show();
            this.finish();
        }

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Long id : ids) {
            final WishItem item = WishItemManager.getInstance(this).retrieveItembyId(id);
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
        final WishItem item = WishItemManager.getInstance(this).retrieveItembyId(id);
        final double lat = item.getLatitude();
        final double lng = item.getLongitude();

        final LatLng point = new LatLng(lat, lng);
        Marker marker = mGoogleMap.addMarker(new MarkerOptions().position(point));
        mMarkerItemMap.put(marker, item);
        setInfoWindowAdapter();
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(point, 15));
    }
}


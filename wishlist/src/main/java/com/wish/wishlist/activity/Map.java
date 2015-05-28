package com.wish.wishlist.activity;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import java.io.File;
import java.util.ArrayList;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.wish.wishlist.db.ItemDBManager;
import com.wish.wishlist.AnalyticsHelper;

import com.wish.wishlist.R;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.model.WishItemManager;

public class Map extends Activity {
    private GoogleMap mGoogleMap;
    private Target mTarget;
    private static final String TAG = "Map";

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
            final LatLngBounds bounds = getBounds();//map view is invoked from main menu->map
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
    }

    /**
     * mark one item on the map
     */
    private void markOneItem() {
        // Read the item we are displaying from the intent, along with the
        // parameters used to set up the map
        Intent i = getIntent();
        final long id = i.getLongExtra("id", -1);
        WishItem item = WishItemManager.getInstance(this).retrieveItembyId(id);
        final double lat = item.getLatitude();
        final double lng = item.getLongitude();

        final LatLng point = new LatLng(lat, lng);
        mTarget = new Target() {
            @Override
            public void onPrepareLoad(Drawable d) {
                Log.e(TAG, "onPrepareLoad");
            }

            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                Log.d(TAG, "onBitmapLoaded");
                Marker marker = mGoogleMap.addMarker(new MarkerOptions()
                        .position(point)
                                //.title("title")
                                //.snippet("snippet")
                        .icon(BitmapDescriptorFactory
                                .fromBitmap(bitmap)));
                                //.fromResource(R.drawable.map_pin)));
            }
            @Override
            public void onBitmapFailed(Drawable d) {
                Log.e(TAG, "onBitmapFailed");
            }
        };

        Picasso.with(this).load(new File(item.getFullsizePicPath())).resize(100, 100).into(mTarget);

        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(point, 15));
    }

    private LatLngBounds getBounds() {
        // Read all item location from db
        ItemDBManager mItemDBManager = new ItemDBManager(this);
        mItemDBManager.open();
        ArrayList<double[]> locationList = mItemDBManager.getAllItemLocation();
        mItemDBManager.close();
        if (locationList.isEmpty()) {
            Toast toast = Toast.makeText(this, "No wish available on map", Toast.LENGTH_SHORT);
            toast.show();
            this.finish();
        }

        //locationList.add(new double[] {locationList.get(0)[0] + 0.001, locationList.get(0)[1] + 10});

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (double[] location : locationList) {
            final LatLng point = new LatLng(location[0], location[1]);

           mGoogleMap.addMarker(new MarkerOptions()
                   .position(point)
                           //.title("title")
                           //.snippet("snippet")
                   .icon(BitmapDescriptorFactory
                           .fromResource(R.drawable.map_pin)));

            builder.include(point);
        }

        LatLngBounds bounds = builder.build();
        return bounds;
    }
}


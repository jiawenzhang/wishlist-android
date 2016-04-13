package com.wish.wishlist.activity;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

import com.wish.wishlist.R;
import com.wish.wishlist.util.Analytics;
import com.wish.wishlist.wish.ExistingWishDetailActivity;
import com.wish.wishlist.wish.FriendWishDetailActivity;
import com.wish.wishlist.wish.ImgMeta;
import com.wish.wishlist.wish.WishLoader;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.model.WishItemManager;
import com.wish.wishlist.image.PhotoFileCreater;

public class MapActivity extends Activity {
    private GoogleMap mGoogleMap;
    private HashMap<Marker, WishItem> mMarkerItemMap = new HashMap<>();
    private int mMarkType;
    private boolean mMyWish;
    private static final int ITEM_DETAILS = 0;
    LatLngBounds mBounds;
    public final static String ITEM = "Item";
    public final static String TYPE = "Type";
    public final static String MY_WISH = "MyWish";
    public final static String FRIEND_ID = "FriendId";

    public final static int MARK_ONE = 0;
    public final static int MARK_ALL = 1;

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

            Analytics.send(Analytics.DEBUG, "com.google.android.gms: " + Integer.toString(v), null);

            finish();
            return;
        }

        setContentView(R.layout.map);

        Analytics.sendScreen("MapView");

        mGoogleMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        Intent i = getIntent();
        mMarkType = i.getIntExtra(TYPE, 0);
        mMyWish = i.getBooleanExtra(MY_WISH, true);
        if (mMarkType == MARK_ONE){
            markOneItem();
        } else if (mMarkType == MARK_ALL) {
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
                Analytics.send(Analytics.MAP, "TapPin", null);

                View v = getLayoutInflater().inflate(R.layout.info_window_layout, null);
                ImageView thumb = (ImageView) v.findViewById(R.id.map_thumb);

                // we need to refresh the InfoWindow when loading image is complete. the callback object's onSuccess is called
                // when Picasso finishes loading the image, and that's when we can refresh the InfoWindow.
                WishItem item = mMarkerItemMap.get(marker);

                TextView name = (TextView) v.findViewById(R.id.map_name);
                name.setText(item.getName());

                if (item.getFullsizePicPath() != null) {
                    // we are showing my wishes
                    MarkerCallback callback = new MarkerCallback(marker);
                    thumb.setTag(callback);
                    String thumb_path = PhotoFileCreater.getInstance().thumbFilePath(item.getFullsizePicPath());
                    Picasso.with(MapActivity.this).load(new File(thumb_path)).resize(200, 200).centerCrop().into(thumb, callback);
                    return v;
                }

                // we are showing friend's wish
                final ImgMeta imgMeta = item.getImgMeta();
                if (imgMeta == null) {
                    thumb.setVisibility(View.GONE);
                    return v;
                }

                final String picURL;
                picURL = imgMeta.mUrl;

                MarkerCallback callback = new MarkerCallback(marker);
                thumb.setTag(callback);
                Picasso.with(MapActivity.this).load(picURL).resize(200, 200).centerCrop().into(thumb, callback);
                return v;
            }
        });

        if (mMarkType == MARK_ONE) {
            // when we mark single wish on map, we disable tapping on pin to show detail view
            return;
        }
        mGoogleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                Intent intent;
                WishItem item = mMarkerItemMap.get(marker);
                if (mMyWish) {
                    intent = new Intent(MapActivity.this, ExistingWishDetailActivity.class);
                    intent.putExtra(ExistingWishDetailActivity.ITEM_ID, item.getId());
                } else {
                    intent = new Intent(MapActivity.this, FriendWishDetailActivity.class);
                    intent.putExtra(FriendWishDetailActivity.ITEM, item);
                }
                startActivityForResult(intent, ITEM_DETAILS);
            }
        });
    }

    boolean markAllItems()
    {
        // Read all items that have location from db
        final String friendId = getIntent().getStringExtra(FRIEND_ID);
        List<WishItem> items_to_show = new ArrayList<>();
        if (friendId == null) {
            // show my wishes
            ItemDBManager mItemDBManager = new ItemDBManager();
            ArrayList<Long> ids = mItemDBManager.getItemsWithLocation();

            for (final long id : ids) {
                items_to_show.add(WishItemManager.getInstance().getItemById(id));
            }
        } else {
            // show friend's wishes
            List<WishItem> items = WishLoader.getInstance().getWishes(friendId);
            for (WishItem item : items) {
                if (item.hasGeoLocation()) {
                    items_to_show.add(item);
                }
            }
        }

        if (items_to_show.isEmpty()) {
            Log.d(TAG, "no wishes with location");
            Toast toast = Toast.makeText(this, "No wish available on map", Toast.LENGTH_SHORT);
            toast.show();
            this.finish();
            return false;
        }

        mBounds = addMarkers(items_to_show);//map view is invoked from main menu->map
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

    private LatLngBounds addMarkers(List<WishItem> items) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (WishItem item : items) {
            Double lat = item.getLatitude();
            Double lng = item.getLongitude();
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
        final WishItem item = i.getParcelableExtra(ITEM);
        if (item == null) {
            finish();
            return;
        }
        Double lat = item.getLatitude();
        Double lng = item.getLongitude();

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


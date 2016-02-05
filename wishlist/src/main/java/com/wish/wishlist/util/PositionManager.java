package com.wish.wishlist.util;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Observable;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.wish.wishlist.WishlistApplication;

public class PositionManager extends Observable {
	private static final String TAG = "PositionManager";
	private static final String UNKNOWN = "unknown";

	private LocationManager mLocationManager;
	private Location mCurrentBestLocation = null;
	private String mAddressString = UNKNOWN;
	private LocationListener mLocationListenerGPS;
	private LocationListener mLocationListenerNetwork;
	private static final int LISTEN_INTERVAL = 1000 * 60;//1 min
	private boolean mGPSEnabled = false;
	private boolean mNetworkEnabled = false;
	private Handler mTimeoutHandler = new Handler();
	
	public PositionManager() {
		mLocationManager = (LocationManager) WishlistApplication.getAppContext().getSystemService(Context.LOCATION_SERVICE);

		if (mGPSEnabled) {
			Location gpsLastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			if (gpsLastKnownLocation != null) {
				mCurrentBestLocation = gpsLastKnownLocation;
			}
		}

		if (mNetworkEnabled) {
			Location netLastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			if (netLastKnownLocation != null && isBetterLocation(netLastKnownLocation)) {
				mCurrentBestLocation = netLastKnownLocation;
			}
		}

//		criteria = new Criteria();
//      criteria.setAccuracy(Criteria.ACCURACY_FINE);
//      criteria.setAltitudeRequired(false);
//      criteria.setBearingRequired(false);
//      criteria.setCostAllowed(true);
//      criteria.setPowerRequirement(Criteria.POWER_LOW);
	}

	public void startLocationUpdates(){
		Log.d(TAG, "startLocationUpdates");

		//Exceptions will be thrown if provider is not permitted.
        try {
			mGPSEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		} catch (Exception e){
			Log.e(TAG, e.toString());
		}

        try {
			mNetworkEnabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		} catch(Exception e){
			Log.e(TAG, e.toString());
		}

        //don't start listeners if no provider is enabled
        if (!mGPSEnabled && !mNetworkEnabled) {
            return;
        }
        
        // Define a listener that responds to location updates
		mLocationListenerGPS = new LocationListener() {
			public void onLocationChanged(Location location) {
				// Called when a new location is found by the gps location provider.
				gotNewLocation(location);
			}

			public void onStatusChanged(String provider, int status, Bundle extras) {}
			public void onProviderEnabled(String provider) {}
			public void onProviderDisabled(String provider) {}
		};

        mLocationListenerNetwork = new LocationListener() {
			public void onLocationChanged(Location location) {
				// Called when a new location is found by the network location provider.
				gotNewLocation(location);
			}

			public void onStatusChanged(String provider, int status, Bundle extras) {}
			public void onProviderEnabled(String provider) {}
			public void onProviderDisabled(String provider) {}
        };

        if (mGPSEnabled) {
			mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListenerGPS);
		}

        if (mNetworkEnabled) {
			mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListenerNetwork);
		}

		//start the timer for listening to location update
		mTimeoutHandler.postDelayed(new Runnable() {
			public void run() {
				stopLocationUpdates();
				setChanged();
				notifyObservers();
			}
		}, 12 * 1000);//timeout is 12s
	}
	
	public void stopLocationUpdates(){
		// Remove the listener previously added
		mLocationManager.removeUpdates(mLocationListenerGPS);
		mLocationManager.removeUpdates(mLocationListenerNetwork);
	}
	
	public Location getCurrentLocation() {
		return mCurrentBestLocation;
	}
	
	private void gotNewLocation(Location newLocation) {
		if (isBetterLocation(newLocation)) {
			mCurrentBestLocation = newLocation;
		}

		mTimeoutHandler.removeCallbacksAndMessages(null);
		stopLocationUpdates();
		setChanged();
		notifyObservers();
		Log.d(TAG, "notifyObservers called");
	}

	/** Determines whether one Location reading is better than the current Location fix
	  * @param location  The new Location that you want to evaluate
	  */
	private boolean isBetterLocation(Location location) {
		if (mCurrentBestLocation == null && location != null) {
			// A new location is always better than no location
			return true;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - mCurrentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > LISTEN_INTERVAL;
		boolean isSignificantlyOlder = timeDelta < -LISTEN_INTERVAL;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
		// If the new location is more than two minutes older, it must be worse
		} else if (isSignificantlyOlder) {
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - mCurrentBestLocation.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(),
				mCurrentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and accuracy
		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
			return true;
		}
		return false;
	}

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}
	
	public String getCurrentAddStr() { //this needs network to be on
		Geocoder gc = new Geocoder(WishlistApplication.getAppContext(), Locale.getDefault());
		try {
			double latitude = mCurrentBestLocation.getLatitude();
			double longitude = mCurrentBestLocation.getLongitude();
			List<Address> addresses = gc.getFromLocation(latitude, longitude, 1);
			StringBuilder sb = new StringBuilder();
			if (addresses.size() > 0) {
				Address address = addresses.get(0);
				for (int i = 0; i < address.getMaxAddressLineIndex()+1; i++)
					sb.append(address.getAddressLine(i)).append("\n");
				//sb.append(address.getLocality()).append("\n");
				//sb.append(address.getPostalCode()).append("\n");
				//sb.append(address.getCountryName());
			}
			mAddressString = sb.toString().trim();
			return mAddressString;
		} catch (IOException e) {
			mAddressString = UNKNOWN;
			return mAddressString;
		}
	}
}

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

public class PositionManager extends Observable {
	static final String LOG_TAG = "MyWishActivity";
	private Context context;
	private LocationManager _locationManager;
	private Location _currentBestLocation = null;
	private String addressString = "unknown";
	private LocationListener _locationListenerGPS;
	private LocationListener _locationListenerNetwork;
	private static final int LISTEN_INTERVAL = 1000 * 60;//1min
	boolean _gps_enabled=false;
	boolean _network_enabled=false;
	Handler _timeoutHandler = new Handler();
	
	public PositionManager(Context ctx) {
		context = ctx;
		_locationManager = (LocationManager)ctx.getSystemService(Context.LOCATION_SERVICE);
//		criteria = new Criteria();
//        criteria.setAccuracy(Criteria.ACCURACY_FINE);
//        criteria.setAltitudeRequired(false);
//        criteria.setBearingRequired(false);
//        criteria.setCostAllowed(true);
//        criteria.setPowerRequirement(Criteria.POWER_LOW);
	}

	public void startLocationUpdates(){
		Log.d(LOG_TAG, "startLocationUpdates");
		//exceptions will be thrown if provider is not permitted.
        try {
			_gps_enabled=_locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		}
		catch (Exception ex){}

        try {
			_network_enabled=_locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		}
		catch(Exception ex){}

        //don't start listeners if no provider is enabled
        if(!_gps_enabled && !_network_enabled) {
            return;
        }
        
        // Define a listener that responds to location updates
		_locationListenerGPS = new LocationListener() {
			public void onLocationChanged(Location location) {
				// Called when a new location is found by the gps location provider.
				gotNewLocation(location);
			}

			public void onStatusChanged(String provider, int status, Bundle extras) {}
			public void onProviderEnabled(String provider) {}
			public void onProviderDisabled(String provider) {}
		};

        _locationListenerNetwork = new LocationListener() {
			public void onLocationChanged(Location location) {
				// Called when a new location is found by the network location provider.
				gotNewLocation(location);
			}

			public void onStatusChanged(String provider, int status, Bundle extras) {}
			public void onProviderEnabled(String provider) {}
			public void onProviderDisabled(String provider) {}
        };

        if(_gps_enabled)
            _locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, _locationListenerGPS);
        if(_network_enabled)
            _locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, _locationListenerNetwork);

		//start the timer for listenning to location update
		_timeoutHandler.postDelayed(new Runnable() {
			public void run() {
				stopLocationUpdates();
				setChanged();
				notifyObservers();
			}
		}, 12 * 1000);//timout is 12s
	}
	
	public void stopLocationUpdates(){
		// Remove the listener you previously added
		_locationManager.removeUpdates(_locationListenerGPS);
		_locationManager.removeUpdates(_locationListenerNetwork);
	}
	
	public Location getCurrentLocation() {
		if (_currentBestLocation != null) {
			return _currentBestLocation;
		}
		
		// both gps and network location listener has not got a location yet, so use the
		// the lastknown location
        if(_gps_enabled) {
			Location gpsLastKnownLoc=null;
            gpsLastKnownLoc=_locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (gpsLastKnownLoc!= null) {
				gotNewLocation(gpsLastKnownLoc);
            }
        }
        if(_network_enabled) {
			Location netLastKnownloc=null;
            netLastKnownloc=_locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (netLastKnownloc != null) {
				gotNewLocation(netLastKnownloc);
            }
        }
        
        return _currentBestLocation;
  
	}
	
	private void gotNewLocation(Location newlocation) {
		if (isBetterLocation(newlocation, _currentBestLocation)) {
			_currentBestLocation = newlocation;
		}
		_timeoutHandler.removeCallbacksAndMessages(null);
		stopLocationUpdates();
		setChanged();
		notifyObservers();
		Log.d(LOG_TAG, "notifyObservers called");
	}

	/** Determines whether one Location reading is better than the current Location fix
	  * @param location  The new Location that you want to evaluate
	  * @param currentBestLocation  The current Location fix, to which you want to compare the new one
	  */
	protected boolean isBetterLocation(Location location, Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return true;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
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
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(),
				currentBestLocation.getProvider());

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
	
	public String getCuttentAddStr(){ //this needs network to be on
		Geocoder gc = new Geocoder(context, Locale.getDefault());
		try {
			double latitude = _currentBestLocation.getLatitude();
			double longitude = _currentBestLocation.getLongitude();
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
			addressString = sb.toString().trim();
			return addressString;
		} catch (IOException e) {
			addressString = "unknown";
			return addressString;
		}
	}
}

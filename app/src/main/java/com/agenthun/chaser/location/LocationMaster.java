package com.agenthun.chaser.location;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

public class LocationMaster {
    private static final String TAG = "LocationMaster";

    //监测周期
    private static final long CHASE_TIME = 5 * 60 * 1000;
//    private static final long CHASE_TIME = 1000;

    private static LocationMaster sLocationMaster;

    private Context mAppContext;

    private LocationManager mLocationManager;

    private String mLastCoordinate;

    public static final String ACTION_LOCATION = "com.agenthun.chaser.ACTION_LOCATION";

    public String getLastCoordinate() {
        return mLastCoordinate;
    }

    public void setLastCoordinate(String lastCoordinate) {
        mLastCoordinate = lastCoordinate;
    }

    private LocationMaster(Context appContext) {
        mAppContext = appContext;
        mLocationManager = (LocationManager) mAppContext.getSystemService(Context.LOCATION_SERVICE);
    }

    public static LocationMaster get(Context c) {
        if (sLocationMaster == null) {
            sLocationMaster = new LocationMaster(c.getApplicationContext());
        }
        return sLocationMaster;
    }

    private PendingIntent getLocationPendingIntent(boolean shouldCreate) {
        Intent broadcast = new Intent(ACTION_LOCATION);
        int flags = shouldCreate ? 0 : PendingIntent.FLAG_NO_CREATE;
        return PendingIntent.getBroadcast(mAppContext, 0, broadcast, flags);
    }

    public void startLocationUpdates() {
        Log.i(TAG, "Start Location Update...");

        String provider = LocationManager.GPS_PROVIDER;

        // get the last known location and broadcast it if we have one
        Location lastKnown = mLocationManager.getLastKnownLocation(provider);

        if (lastKnown == null) {
            Log.i(TAG, "Not got last known location from GPS");

            lastKnown = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            if (lastKnown == null) {
                Log.i(TAG, "Not got last known location from Network");
            } else {
                Log.i(TAG, "Got last known location from Network");
            }
        } else {
            Log.i(TAG, "Got last known location from GPS");
        }

        if (lastKnown != null) {
            setLastCoordinate(String.format("%1$f,%2$f", lastKnown.getLatitude(), lastKnown.getLongitude()));
            broadcastLocation(lastKnown);
        }

        // start updates from the location manager
        PendingIntent pi = getLocationPendingIntent(true);
        mLocationManager.requestLocationUpdates(provider, CHASE_TIME, 0, pi);
    }

    public void stopLocationUpdates() {
        Log.i(TAG, "Stop Location Update...");

        PendingIntent pi = getLocationPendingIntent(false);
        if (pi != null) {
            mLocationManager.removeUpdates(pi);
            pi.cancel();
        }
    }

    private void broadcastLocation(Location location) {
        Intent broadcast = new Intent(ACTION_LOCATION);
        broadcast.putExtra(LocationManager.KEY_LOCATION_CHANGED, location);
        mAppContext.sendBroadcast(broadcast);
    }
}

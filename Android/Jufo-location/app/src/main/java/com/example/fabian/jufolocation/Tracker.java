package com.example.fabian.jufolocation;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;

public class Tracker extends Service implements LocationListener {

    Context context;
    private double latidude;
    private double longitude;
    LocationManager locationManager;
    Location location = null;

    public Tracker(Context context) {
        this.context = context;
        location = locate();
    }

    private Location locate() {

        Location location = null;

        try {
            locationManager = (LocationManager) this.context.getSystemService(LOCATION_SERVICE);
            assert locationManager != null;
            boolean isGPSActive = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetActive = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (isGPSActive) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                }
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60000, 20, this);
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            else if (isNetActive) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60000, 20, this);
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        if (location != null) {
            latidude = location.getLatitude();
            longitude = location.getLongitude();

        }
        return location;
    }

    public void stopUpdates() {
        if(locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    public double getLongitude() {
        if (location != null) {
            longitude = location.getLongitude();
        }
        return longitude;
    }


    public double getLatidude() {
        if (location != null) {
            latidude = location .getLatitude();
        }
        return latidude;
    }
}

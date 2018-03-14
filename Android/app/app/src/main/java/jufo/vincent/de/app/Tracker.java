package jufo.vincent.de.app;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;

public class Tracker extends Service implements LocationListener {

    Context context;
    private double latidude;
    private double longitude;
    LocationManager locationManager;
    Location location = null;

    @SuppressWarnings("unused")
    public Tracker() {
    }

    public Tracker(Context context) {
        this.context = context;
        location = locate();
    }

    @SuppressLint("MissingPermission")
    private Location locate() {
        Location location = null;
        try {
            locationManager = (LocationManager) this.context.getSystemService(LOCATION_SERVICE);
            assert locationManager != null;
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60000, 20, this);
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        } catch(Exception e) {
            e.printStackTrace();
        }
        if (location != null) {
            latidude = location.getLatitude();
            longitude = location.getLongitude();

        }
        return location;
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

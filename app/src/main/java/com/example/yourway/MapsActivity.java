package com.example.yourway;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;


import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.yourway.databinding.ActivityMapsBinding;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener {

    private GoogleMap mMap;
    private static final String TAG = MapsActivity.class.getSimpleName();

    // A default location (Timisoara, Romania) and default zoom to use when location permission is
    // not granted.
    private final LatLng defaultLocation = new LatLng(45.760696, 21.226788);
    private static final int DEFAULT_ZOOM = 13;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean locationPermissionGranted;

    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient fusedLocationProviderClient;

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location lastKnownLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        com.example.yourway.databinding.ActivityMapsBinding binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Build the map.
        // [START maps_current_place_map_fragment]
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
        // [END maps_current_place_map_fragment]
        // [END_EXCLUDE]
    }
    // [END maps_current_place_on_create]

    /**
     * Manipulates the map when it's available.
     * This callback is triggered when the map is ready to be used.
     */
    // [START maps_current_place_on_map_ready]
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 2);
        }

        mMap.setOnMapClickListener(this);

        // Prompt the user for permission.
        getLocationPermission();
        // [END_EXCLUDE]

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // Get the current location of the device and set the position of the map.
        getDeviceLocation();
    }
    // [END maps_current_place_on_map_ready]

    @Override
    public void onMapClick(LatLng latLng) {
        // Creating a marker at the current position
        MarkerOptions markerOptions = new MarkerOptions();

        markerOptions.position(latLng);
        markerOptions.title(latLng.latitude + " : " + latLng.longitude);

        mMap.clear();
        mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.addMarker(markerOptions);
    }

    /**
     * Prompts the user for permission to use the device location.
     */
    // [START maps_current_place_location_permission]
    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }
    // [END maps_current_place_location_permission]

    /**
     * Handles the result of the request for location permissions.
     */
    // [START maps_current_place_on_request_permissions_result]
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        locationPermissionGranted = false;
        if (requestCode
                == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
        updateLocationUI();
    }
    // [END maps_current_place_on_request_permissions_result]

    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    // [START maps_current_place_update_location_ui]
    @SuppressLint("MissingPermission")
    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (locationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                lastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }
    // [END maps_current_place_update_location_ui]


    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    // [START maps_current_place_get_device_location]
    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (locationPermissionGranted) {
                @SuppressLint("MissingPermission") Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            lastKnownLocation = task.getResult();
                            if (lastKnownLocation != null) {
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(lastKnownLocation.getLatitude(),
                                                lastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                            }
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            mMap.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }
    // [END maps_current_place_get_device_location]


}
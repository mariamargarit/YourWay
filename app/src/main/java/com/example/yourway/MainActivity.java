package com.example.yourway;

import static android.content.ContentValues.TAG;
import static com.example.yourway.BuildConfig.MAPS_API_KEY;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DirectionsStep;
import com.google.maps.model.EncodedPolyline;
import com.google.maps.model.TransitDetails;
import com.google.maps.model.TravelMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener {

    private GoogleMap mMap;
    View mapView;

    private final Integer[] busColors = {Color.RED, Color.GREEN, Color.CYAN, Color.DKGRAY, Color.YELLOW, Color.MAGENTA, Color.GRAY};

    // variables to set the origin and destination coordinates
    private String origin;
    private String destination;

    // creating a variable for search view.
    SearchView searchView;

    @SuppressLint({"MissingPermission", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        TextView textView = findViewById(R.id.simpleTextView);
        //set the color for text view
        textView.setTextColor(Color.BLACK);
        //set 20sp size of text
        textView.setTextSize(20);

        // added scroll
        textView.setMovementMethod(new ScrollingMovementMethod());

        // initializing our search view
        searchView = findViewById(R.id.idSearchView);

        Button btnGetDirections = findViewById(R.id.btnGetDirections);

        // obtain the SupportMapFragment and get notified when the map is ready to be used
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        // adding on query listener for our search view
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @SuppressLint("DefaultLocale")
            @Override
            public boolean onQueryTextSubmit(String query) {
                // getting the location name from search view
                String location = searchView.getQuery().toString();

                // create a list of address where we will store the list of all address
                List<Address> addressList = null;

                // checking if the entered location is null or not
                // creating and initializing a geo coder
                Geocoder geocoder = new Geocoder(MainActivity.this);
                try {
                    // getting location from the location name and adding that location to address list
                    addressList = geocoder.getFromLocationName(location, 1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // getting the location from our list a first position
                assert addressList != null;
                Address address = addressList.get(0);

                // creating a variable for our location where we will add our locations latitude and longitude
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                destination = String.format("%f,%f", address.getLatitude(), address.getLongitude());

                mMap.clear();

                // adding marker to that position
                mMap.addMarker(new MarkerOptions().position(latLng).title(location));

                // animate camera to that position
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    // got last known location, in some rare situations this can be null so we check it
                    if (location != null) {
                        origin = Location.convert(location.getLatitude(), Location.FORMAT_DEGREES) + "," + Location.convert(location.getLongitude(), Location.FORMAT_DEGREES);
                    }
                });

        // get directions functionality
        btnGetDirections.setOnClickListener(v -> {
            int color=0;

            // create a new list to store the transit details
            List<TransitDetails> transitDetailsList = new ArrayList<>();

            // execute Directions API request using the Directions API key
            GeoApiContext context = new GeoApiContext.Builder()
                    .apiKey(MAPS_API_KEY)
                    .build();
            DirectionsApiRequest request = DirectionsApi.newRequest(context);
            request.origin(origin);
            request.destination(destination);

            // find direction using only public transportation
            request.mode(TravelMode.TRANSIT);

            try {
                // set the text for text view
                textView.setText("             Route Instructions:\n");

                // execute the API request and get the response
                DirectionsResult result = request.await();

                // loop through legs and steps to get encoded poly-lines of each step
                if (result.routes != null && result.routes.length > 0) {

                    // get the routes from the response
                    DirectionsRoute route = result.routes[0];

                    if (route.legs !=null) {
                        // iterate through the list of routes and  get the list of legs for the current route
                        for(int i=0; i<route.legs.length; i++) {

                            DirectionsLeg leg = route.legs[i];
                            if (leg.steps != null) {

                                // iterate through the list of legs and get the list of steps for the current leg
                                for (int j=0; j<leg.steps.length;j++){

                                    DirectionsStep step = leg.steps[j];

                                    // check if the current step is a walking step and save needed content
                                    if(step.travelMode.name().equals("WALKING")){
                                        textView.append("   Walk "+ step.distance.humanReadable+ " in " + step.duration.humanReadable+ "\n");

                                    }

                                    // check if the current step is a transit bus step and save details
                                    if (step.transitDetails != null) {
                                        // get the transit details for the current step
                                        TransitDetails busTransitDetails = step.transitDetails;

                                        // add the transit details to the list
                                        transitDetailsList.add(busTransitDetails);

                                        textView.append("   Take line " + busTransitDetails.line + " at " + busTransitDetails.arrivalTime.getHour() +
                                                ":" + busTransitDetails.arrivalTime.getMinute() +" for " + busTransitDetails.numStops + " stops \n");

                                    }

                                    if (step.steps != null && step.steps.length >0) {

                                        // iterate through the list of steps
                                        for (int k=0; k<step.steps.length;k++){

                                            DirectionsStep step1 = step.steps[k];

                                            EncodedPolyline points1 = step1.polyline;
                                            List<LatLng> path = new ArrayList<>();
                                            PolylineOptions opts = new PolylineOptions();
                                            if (points1 != null) {
                                                // decode polyline and add points to list of route coordinates
                                                List<com.google.maps.model.LatLng> coords1 = points1.decodePath();
                                                for (com.google.maps.model.LatLng coord1 : coords1) {
                                                    path.add(new LatLng(coord1.lat, coord1.lng));
                                                }

                                                // draw the polyline
                                                if(step1.travelMode.name().equals("WALKING")){
                                                    opts.addAll(path).color(Color.BLUE).width(5);
                                                    mMap.addPolyline(opts);
                                                }
                                                else{
                                                    if(color>6) color=0;
                                                    opts.addAll(path).color(busColors[color]).width(5);
                                                    color++;
                                                    mMap.addPolyline(opts);
                                                }
                                            }
                                        }
                                    } else {

                                        EncodedPolyline points = step.polyline;
                                        List<LatLng> path = new ArrayList<>();
                                        PolylineOptions opts = new PolylineOptions();
                                        if (points != null) {
                                            // decode polyline and add points to list of route coordinates
                                            List<com.google.maps.model.LatLng> coords = points.decodePath();
                                            for (com.google.maps.model.LatLng coord : coords) {
                                                path.add(new LatLng(coord.lat, coord.lng));
                                            }

                                            // draw the polyline
                                            if(step.travelMode.name().equals("WALKING")){
                                                opts.addAll(path).color(Color.BLUE).width(5);
                                            }
                                            else{
                                                if(color>6) color=0;
                                                opts.addAll(path).color(busColors[color]).width(5);
                                                mMap.addPolyline(opts);
                                                color++;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch(Exception ex) {
                Log.e(TAG, ex.getLocalizedMessage());
            }

            String resultedDetails = transitDetailsList.toString();
            Log.d("Transit Details: ", resultedDetails);

        });


        // at last we calling our map fragment to update
        assert mapFragment != null;
        mapView = mapFragment.getView();
        mapFragment.getMapAsync(this);
    }


    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {

        mMap = googleMap;
        mMap.setMyLocationEnabled(true);

        // add a marker in timisoara and move the camera
        LatLng timisoara = new LatLng(45.760696, 21.226788);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(timisoara, 15));

        if (mapView != null &&
                mapView.findViewById(Integer.parseInt("1")) != null) {

            // get the button view
            View locationButton = ((View) mapView.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));

            // and next place it, on bottom right
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)
                    locationButton.getLayoutParams();

            // position on right bottom
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            layoutParams.setMargins(0, 0, 30, 30);
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 2);
        }

        mMap.setOnMapClickListener(this);

    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onMapClick(@NonNull LatLng latLng) {
        destination = String.format("%f,%f", latLng.latitude, latLng.longitude);

        // creating a marker at the current position
        MarkerOptions markerOptions = new MarkerOptions();

        markerOptions.position(latLng);
        markerOptions.title(latLng.latitude + " : " + latLng.longitude);

        mMap.clear();
        mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.addMarker(markerOptions);
    }

}
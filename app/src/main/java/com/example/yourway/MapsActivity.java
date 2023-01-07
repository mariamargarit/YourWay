package com.example.yourway;

import static android.content.ContentValues.TAG;

import static com.example.yourway.BuildConfig.MAPS_API_KEY;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

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

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener {

    private GoogleMap mMap;
    View mapView;

    // variables to set the origin and destination coordinates
    private String origin;
    private String destination;

    // creating a variable for search view.
    SearchView searchView;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // initializing our search view.
        searchView = findViewById(R.id.idSearchView);

        Button btnGetDirections = findViewById(R.id.btnGetDirections);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        // adding on query listener for our search view.
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @SuppressLint("DefaultLocale")
            @Override
            public boolean onQueryTextSubmit(String query) {
                // on below line we are getting the
                // location name from search view.
                String location = searchView.getQuery().toString();

                // below line is to create a list of address
                // where we will store the list of all address.
                List<Address> addressList = null;

                // checking if the entered location is null or not.
                // on below line we are creating and initializing a geo coder.
                Geocoder geocoder = new Geocoder(MapsActivity.this);
                try {
                    // on below line we are getting location from the
                    // location name and adding that location to address list.
                    addressList = geocoder.getFromLocationName(location, 1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // on below line we are getting the location
                // from our list a first position.
                assert addressList != null;
                Address address = addressList.get(0);

                // on below line we are creating a variable for our location where we will add our locations latitude and longitude.
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                destination = String.format("%f,%f", address.getLatitude(), address.getLongitude());

                mMap.clear();

                // on below line we are adding marker to that position.
                mMap.addMarker(new MarkerOptions().position(latLng).title(location));

                // below line is to animate camera to that position.
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
                    // Got last known location. In some rare situations this can be null.
                    if (location != null) {
                        origin = Location.convert(location.getLatitude(), Location.FORMAT_DEGREES) + "," + Location.convert(location.getLongitude(), Location.FORMAT_DEGREES);
                    }
                });

        //get directions functionality
        btnGetDirections.setOnClickListener(v -> {

            // Create a new list to store the transit details
            List<TransitDetails> transitDetailsList = new ArrayList<>();

            //Define list to get all lat-lng for the route
            List<LatLng> path = new ArrayList<>();

            // Create a new list to store the walking instructions
            List<String> walkingInstructions = new ArrayList<>();

            // Create a new list to store the walking distance
            List<String> walkingDistance = new ArrayList<>();

            // Create a new list to store the walking duration
            List<String> walkingDuration = new ArrayList<>();

            // Create a new list to store the travel mode
            List<String> resultedTravelMode = new ArrayList<>();

            // Create a new list to store the arrival stops
            List<String> arrivalStops = new ArrayList<>();

            // Create a new list to store the arrival time
            List<String> arrivalTimes = new ArrayList<>();

            // Create a new list to store the departure stops
            List<String> departureStops = new ArrayList<>();

            // Create a new list to store the departure time
            List<String> departureTimes = new ArrayList<>();

            // Create a new list to store the number of bus stops
            List<String> numBusStops = new ArrayList<>();

            // Create a new list to store the bus line
            List<String> busLines = new ArrayList<>();

            // Create a new list to store the bus headway
            List<String> busHeadway = new ArrayList<>();

            // Create a new list to store the bus head sign
            List<String> busHeadSign = new ArrayList<>();

            //Execute Directions API request using the Directions API key
            GeoApiContext context = new GeoApiContext.Builder()
                    .apiKey(MAPS_API_KEY)
                    .build();
            DirectionsApiRequest request = DirectionsApi.newRequest(context);
            request.origin(origin);
            request.destination(destination);

            //Find direction using only public transportation
            request.mode(TravelMode.TRANSIT);

            try {
                // Execute the API request and get the response
                DirectionsResult result = request.await();

                //Loop through legs and steps to get encoded poly-lines of each step
                if (result.routes != null && result.routes.length > 0) {

                    // Get the routes from the response
                    DirectionsRoute route = result.routes[0];

                    if (route.legs !=null) {
                        // Iterate through the list of routes and  get the list of legs for the current route
                        for(int i=0; i<route.legs.length; i++) {

                            DirectionsLeg leg = route.legs[i];
                            if (leg.steps != null) {

                                // Iterate through the list of legs and get the list of steps for the current leg
                                for (int j=0; j<leg.steps.length;j++){

                                    DirectionsStep step = leg.steps[j];
                                    if (step.steps != null && step.steps.length >0) {

                                        // Iterate through the list of steps
                                        for (int k=0; k<step.steps.length;k++){

                                            DirectionsStep step1 = step.steps[k];

                                            resultedTravelMode.add(step1.travelMode.name());

                                            com.google.maps.model.LatLng startLocation = step1.startLocation;
                                            com.google.maps.model.LatLng endLocation = step1.endLocation;

                                            // Check if the current step is a walking step and save needed content
                                            if(step1.travelMode.name().equals("walking")){
                                                walkingInstructions.add(step1.htmlInstructions);
                                                walkingDistance.add(step1.distance.humanReadable);
                                                walkingDuration.add(step1.duration.humanReadable);
                                            }

                                            // Check if the current step is a transit bus step and save details
                                            if (step1.transitDetails != null) {
                                                // Get the transit details for the current step
                                                TransitDetails busTransitDetails = step1.transitDetails;

                                                // Add the transit details to the list
                                                transitDetailsList.add(busTransitDetails);

                                                arrivalStops.add(busTransitDetails.arrivalStop.name);
                                                arrivalTimes.add(busTransitDetails.arrivalTime.toString());

                                                departureStops.add(busTransitDetails.departureStop.name);
                                                departureTimes.add(busTransitDetails.departureTime.toString());

                                                busHeadSign.add(busTransitDetails.headsign);
                                                busHeadway.add((String.valueOf(busTransitDetails.headway)));
                                                busLines.add(busTransitDetails.line.name);
                                                numBusStops.add(String.valueOf(busTransitDetails.numStops));
                                            }

                                            EncodedPolyline points1 = step1.polyline;
                                            if (points1 != null) {
                                                //Decode polyline and add points to list of route coordinates
                                                List<com.google.maps.model.LatLng> coords1 = points1.decodePath();
                                                for (com.google.maps.model.LatLng coord1 : coords1) {
                                                    path.add(new LatLng(coord1.lat, coord1.lng));
                                                }
                                            }
                                        }
                                    } else {

                                        resultedTravelMode.add(step.travelMode.name());

                                        com.google.maps.model.LatLng startLocation = step.startLocation;
                                        com.google.maps.model.LatLng endLocation = step.endLocation;

                                        // Check if the current step is a walking step and save needed content
                                        if(step.travelMode.name().equals("walking")){
                                            walkingInstructions.add(step.htmlInstructions);
                                            walkingDistance.add(step.distance.humanReadable);
                                            walkingDuration.add(step.duration.humanReadable);
                                        }

                                        // Check if the current step is a transit bus step and save details
                                        if (step.transitDetails != null) {
                                            // Get the transit details for the current step
                                            TransitDetails busTransitDetails = step.transitDetails;

                                            // Add the transit details to the list
                                            transitDetailsList.add(busTransitDetails);

                                            arrivalStops.add(busTransitDetails.arrivalStop.name);
                                            arrivalTimes.add(busTransitDetails.arrivalTime.toString());

                                            departureStops.add(busTransitDetails.departureStop.name);
                                            departureTimes.add(busTransitDetails.departureTime.toString());

                                            busHeadSign.add(busTransitDetails.headsign);
                                            busHeadway.add((String.valueOf(busTransitDetails.headway)));
                                            busLines.add(busTransitDetails.line.name);
                                            numBusStops.add(String.valueOf(busTransitDetails.numStops));
                                        }

                                        EncodedPolyline points = step.polyline;
                                        if (points != null) {
                                            //Decode polyline and add points to list of route coordinates
                                            List<com.google.maps.model.LatLng> coords = points.decodePath();
                                            for (com.google.maps.model.LatLng coord : coords) {
                                                path.add(new LatLng(coord.lat, coord.lng));
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
            //Draw the polyline
            if (path.size() > 0) {
                PolylineOptions opts = new PolylineOptions().addAll(path).color(Color.BLUE).width(5);
                mMap.addPolyline(opts);
            }

            String resultedDetails = transitDetailsList.toString();
            Log.d("Transit Details", resultedDetails);

            String resultedDistance = walkingDistance.toString();
            Log.d("Distance walking Details", resultedDistance);

            String resultedWalking = walkingInstructions.toString();
            Log.d("Instructions walking Details", resultedWalking);

            String resultedDurationWalking = walkingDuration.toString();
            Log.d("Duration walking Details", resultedDurationWalking);

            String resultedTravelModes = resultedTravelMode.toString();
            Log.d("Duration walking Details", resultedTravelModes);

            String resultedArrivalStops = arrivalStops.toString();
            Log.d("Arrival stops Details", resultedArrivalStops);

            String resultedArrivalTimes = arrivalTimes.toString();
            Log.d("Arrival times Details", resultedArrivalTimes);

            String resultedDepartureStops = departureStops.toString();
            Log.d("Departure stops Details", resultedDepartureStops);

            String resultedDepartureTimes = departureTimes.toString();
            Log.d("Departure times Details", resultedDepartureTimes);

            String resultedNumBusStops = numBusStops.toString();
            Log.d("Num Bus Stops Details", resultedNumBusStops);

            String resultedBusLines = busLines.toString();
            Log.d("Bus lines Details", resultedBusLines);

            String resultedBusHeadAway = busHeadway.toString();
            Log.d("Bus HeadAway Details", resultedBusHeadAway);

            String resultedBusHeadSign = busHeadSign.toString();
            Log.d("Bus Head Sign Details", resultedBusHeadSign);

        });

        // at last we calling our map fragment to update.
        assert mapFragment != null;
        mapView = mapFragment.getView();
        mapFragment.getMapAsync(this);
    }


    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {

        mMap = googleMap;
        mMap.setMyLocationEnabled(true);

        // Add a marker in timisoara and move the camera
        LatLng timisoara = new LatLng(45.760696, 21.226788);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(timisoara, 15));

        if (mapView != null &&
                mapView.findViewById(Integer.parseInt("1")) != null) {

            // Get the button view
            View locationButton = ((View) mapView.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));

            // and next place it, on bottom right (as Google Maps app)
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

        // Creating a marker at the current position
        MarkerOptions markerOptions = new MarkerOptions();

        markerOptions.position(latLng);
        markerOptions.title(latLng.latitude + " : " + latLng.longitude);

        mMap.clear();
        mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.addMarker(markerOptions);
    }

}
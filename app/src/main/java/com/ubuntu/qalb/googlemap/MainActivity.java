package com.ubuntu.qalb.googlemap;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {



    //THSI IS MY FIREBASE BRANCH


    //Request codes
    private final static int PERMISSION = 0xc8;
    private final static int LOCATION = 0x1;

    //Request codes
    private final static int PERMISSION_LOCATION_REQUEST = 0xc8;
    private final static int LOCATION_SETTING_REQUEST_CODE = 0x1;

    //GoogleMap object to show map on fragment
    GoogleMap mGoogleMap;

    //GoogleApiClient to access GooglePlayServices Api's
    GoogleApiClient mGoogleApiClient;

    //Request user's location
    LocationRequest mLocationRequest;

    //only one marker will be visible on user current location
    Marker marker ;

    //circle radius object
    Circle circleRadius;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //check & display the activity layout if google play services is available
        if (isGoogleServiceAvailable()) {
            Toast.makeText(this, "Connected to Google Play Services", Toast.LENGTH_SHORT).show();
            setContentView(R.layout.activity_main);
            initMap();
        }

    }


    //initialize map fragment to display Google map
    private void initMap() {
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mapfragment);
        mapFragment.getMapAsync(this);
    }


    /*--check whether or not google service is available-
    * if available than return true. Else check whether it is installed or not.
    * return false
    * */
    private boolean isGoogleServiceAvailable() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance(); //get google api instance
        int isGoogleServiceAvailable = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if (isGoogleServiceAvailable == ConnectionResult.SUCCESS) {
            return true; //return true if its available
        } else if (googleApiAvailability.isUserResolvableError(isGoogleServiceAvailable)) {
            Dialog dialog = googleApiAvailability.getErrorDialog(this, isGoogleServiceAvailable, 0);
            dialog.show();
        } else {
            Toast.makeText(this, "Cannot connect to services", Toast.LENGTH_SHORT).show();
        }

        return false;
    }


    //callback function when map is ready it will display in mapfragment activity
    //it will load all the details of map first than load map in fragment
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "onMapReady", Toast.LENGTH_SHORT).show();
        mGoogleMap = googleMap;
        //goToLocation(33.562317,73.0805946);

        if (mGoogleMap != null){
            mGoogleMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
                @Override
                public View getInfoWindow(Marker marker) {
                    return null;
                }

                @Override
                public View getInfoContents(Marker marker) {
                    //getPosition for lat n long
                    LatLng ll = marker.getPosition();

                    View v = getLayoutInflater().inflate(R.layout.google_map_user_info_layout, null);
                    TextView username = v.findViewById(R.id.tv_username);
                    TextView lat = v.findViewById(R.id.tv_lat);
                    TextView lon = v.findViewById(R.id.tv_long);

                    username.setText("Qalb Hussain");
                    lat.setText("Lat: "+ll.latitude);
                    lon.setText("Long: "+ll.longitude);

                    return v;
                }
            });
        }



        //build the google api client
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        //connect GoogleApiClient to GooglePlayServices when app start
        mGoogleApiClient.connect();

    }

//    //go to specific location when map is ready
//    private void goToLocation(double lat, double lng) {
//        LatLng latLng = new LatLng(lat, lng);
//        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 10);
//        mGoogleMap.moveCamera(cameraUpdate);
//    }


    //--Google api client connection callbacks
    //--goto user's locations when connection established
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Toast.makeText(this, "onConnected", Toast.LENGTH_SHORT).show();
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(1000);

        //check whether location is enabled on user's device
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);

        PendingResult<LocationSettingsResult> results = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());

        results.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
                final Status status = locationSettingsResult.getStatus();
                final LocationSettingsStates states = locationSettingsResult.getLocationSettingsStates();

                switch (status.getStatusCode()){
                    case LocationSettingsStatusCodes.SUCCESS : //everything is fine, we're good to go. show user current location
                        getUserCurrentLocation();
                        break ;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED: //location is not enabled. Display a dialog to user
                        try {
                            status.startResolutionForResult(MainActivity.this, LOCATION_SETTING_REQUEST_CODE);
                        } catch (IntentSender.SendIntentException e) {
                            e.printStackTrace();
                        }
                        break ;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                        break;
                    default:
                        break;
                }


            }
        });

    }


    //display current location and show on map
    public void getUserCurrentLocation() {
        //show user current location if permissions are granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } else {

            //if the android api version is greater than 21
            //ask user to give permissions
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_LOCATION_REQUEST);
            }
        }
    }


    //--check whether user give access to permissions
    // if not disable funtionality that depends on these permissions
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_LOCATION_REQUEST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "LocationServies.FusedLocationApi called", Toast.LENGTH_SHORT).show();
                        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
                    }

                }
                else{
                    Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show();
                }
            }

        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    //--Google api client on Connection Failed callback
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    //get the user current location and update map when location changes
    //location listener
    @Override
    public void onLocationChanged(Location location) {
        Toast.makeText(this, "onLocationChanged", Toast.LENGTH_SHORT).show();
        //get the location object
        if (location == null) {
            Toast.makeText(this, "Can not access current location", Toast.LENGTH_SHORT).show();
        } else {
            LatLng latlng = new LatLng(location.getLatitude(), location.getLongitude());
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latlng, 15);
            mGoogleMap.animateCamera(cameraUpdate);
            addMarkerToUserLocation(latlng);
        }

    }

    //add marker to user current location
    private void addMarkerToUserLocation(LatLng latlng) {
        //remove any previous added marker on GoogleMap
        if (marker != null){
            removePreviousMarkerWithRadius();
        }

        //add marker to user current location
        MarkerOptions markerOption = new MarkerOptions()
                .title("Current Location")
                .snippet("Qalb Hussain's home")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_local_dining))
                .position(latlng);

        marker = mGoogleMap.addMarker(markerOption);
        circleRadius = addRadiusToCurrentLocation(latlng);
    }

    //--this function will remove any previous marker added to google map
    //--with circular radius
    private void removePreviousMarkerWithRadius() {
        //--remove the marker from google map and set it equals to null
        marker.remove();
        marker = null;
        //--remove the radius from google map and set it equals to null
        circleRadius.remove();
        circleRadius = null;
    }


    //--this will add a radius of few kilometers to user's locations
    //-- the latlng parameter is just for getting user's locations
    private Circle addRadiusToCurrentLocation(LatLng latlng) {
        CircleOptions circleOption = new CircleOptions()
                .fillColor(R.color.colorPrimaryDark)
                .radius(2000)
                .center(latlng);

        return mGoogleMap.addCircle(circleOption);
    }


    //---------------ACTIVITY FUNCTIONS----------------//

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode)
        {
            case LOCATION_SETTING_REQUEST_CODE :
                switch (resultCode)
                {
                    case Activity.RESULT_OK :
                        //All changes were made successfully
                        //get user current location
                        getUserCurrentLocation();
                        break;
                    case Activity.RESULT_CANCELED:
                        //user didnot change setting
                        Toast.makeText(this, "Cannot get you current location. Please enable your device location", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        break;
                }
                break;

            default:
                Toast.makeText(this, "Location setting request code does not matched", Toast.LENGTH_SHORT).show();
                break;
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()){
            Toast.makeText(this, "onStop: GoogleApiClient Disconnect", Toast.LENGTH_SHORT).show();
            mGoogleApiClient.disconnect();
        }
    }
}

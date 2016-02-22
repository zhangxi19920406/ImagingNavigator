package com.example.imagingnavigator.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.imagingnavigator.R;
import com.example.imagingnavigator.function.PlaceAutoCompleteAdapter;
import com.example.imagingnavigator.function.Router;
import com.example.imagingnavigator.function.UpdateIntent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;

public class MapBasedViewActivity extends FragmentActivity implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    private static final String TAG = MapBasedViewActivity.class.getSimpleName();
    private static final int REQUEST_CODE_CAMERA = 1;
    //The mininum distance to update location in 50 meters
    private static final long MIN_DISTANCE_FOR_UPDATE = 50;
    //The minimum time to update location in 1 minutes
    private static final long MIN_TIME_FOR_UPDATE = 1000 * 60 * 1;
    private static final String ROUTE_JSON_DATA = "routeJsonData";
    //flag for GPS status
    boolean isGPSEnabled = false;

    //flag for network status
    boolean isNetworkEnabled = false;
    //flag for current location status
    boolean canGetLocation = false;

    double[] curPosition;
    /**
     * Start activity type for start the CameraBasedViewActivity.
     */
    private static final int CAMERA_BASED_VIEW = 2;

    /**
     * The Google Map object.
     */
    private GoogleMap mMap;

    /**
     *  The LocationManager object.
     */
    private LocationManager locationManager;

    /**
     * The marker in Google Map
     */
    private MarkerOptions markerOpt;
    private CameraPosition cameraPosition;

    private Location location;
    private String bestProvider;

    private MarkerOptions markerOptions;
    private LatLng latLng;
    private LatLng curLatLng;

    private AutoCompleteTextView etLocation;
    private PlaceAutoCompleteAdapter mAdapter;
    protected GoogleApiClient mGoogleApiClient;

    private CardView cardviewbottom;
    private LinearLayout linearLayout;
    private ImageButton walkButton;
    private ImageButton driveButton;
    private ImageButton cameraButton;

    private static final String NAVIGATION_MODE = "mode";
    private static final String NAVIGATION_DESTINATION_LATITUDE = "latitude";
    private static final String NAVIGATION_DESTINATION_LONGITUDE = "longitude";

    private String naviMode;

    private Intent cameraIntent;

    //defalut route
    private static final LatLngBounds BOUNDS_JAMAICA = new LatLngBounds(new LatLng(42.0054446, -87.9678884),
            new LatLng(43.9257104d, -88.0508355d));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_based_view);

        //create SupportMapFragment object, and get Provider
        //initProvider();

        //Getting reference to Navigation button after click button
        linearLayout = (LinearLayout) findViewById(R.id.after_search);

        // Getting reference to EditText to get the user input location
        etLocation = (AutoCompleteTextView) findViewById(R.id.et_location);

        //set GoogleApiClient
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        MapsInitializer.initialize(this);
        mGoogleApiClient.connect();

        //set adapter
        mAdapter = new PlaceAutoCompleteAdapter(this, android.R.layout.simple_list_item_1,
                mGoogleApiClient, BOUNDS_JAMAICA, null);

        //Obtain the Map Fragment
        mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        getCurLocation();
        double dLat = 43.0054446;
        double dLong = -87.9678884;

        if (location != null) {
            //get the latitude
            dLat = location.getLatitude();
            //get the longitude
            dLong = location.getLongitude();
        }
        //drawRoute(new LatLng(dLat, dLong), new LatLng(42.9257104d, -88.0508355d), "driving");


        System.out.println("-------------");
        Log.d(TAG, "----");

        //initialize search button
        searchMap();

        //initialize search bar
        setAutoAdapter();

    }

    @Override
    protected void onPause(){
        super.onPause();

        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putString(this.NAVIGATION_MODE, naviMode)
                .commit();
        if(latLng!=null) {
            PreferenceManager.getDefaultSharedPreferences(this).edit()
                    .putString(this.NAVIGATION_DESTINATION_LATITUDE, Double.toString(latLng.latitude))
                    .commit();

            PreferenceManager.getDefaultSharedPreferences(this).edit()
                    .putString(this.NAVIGATION_DESTINATION_LONGITUDE, Double.toString(latLng.longitude))
                    .commit();

            Log.e(TAG, "=========onPause::naviMode is: " + naviMode + "============");
            Log.e(TAG, "=========onPause::current LatLng is: [" + latLng.latitude + " " + latLng.longitude + "]==========");
        }
    }

    @Override
    protected void onResume(){
        super.onResume();

    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO - fill in here
        Log.e("--------getReturn:", "before");
        if (data!=null){
            switch (requestCode){
                case REQUEST_CODE_CAMERA:

                    curPosition = data.getExtras().getDoubleArray("curLocation");
                    Log.e("--------getReturn:===:", String.valueOf(curPosition[0]));
                    boolean isArrival = data.getExtras().getBoolean("isArrival");
                    if(!isArrival){
                        getReturnStatus();
                    }else{
                        clearMap();
                    }

                    break;

            }
        }
    }

    //when we jump from camera to map, we will get the previous operation info
    private void getReturnStatus(){
        naviMode = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(this.NAVIGATION_MODE, null);

        String latitudeStr = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(this.NAVIGATION_DESTINATION_LATITUDE, null);
        String longitudeStr = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(this.NAVIGATION_DESTINATION_LONGITUDE, null);
        if(!naviMode.isEmpty()) {
            if (latitudeStr != null && longitudeStr != null) {
                double latitude = Double.valueOf(latitudeStr);

                double longitude = Double.valueOf(longitudeStr);

                latLng = new LatLng(latitude, longitude);
                Log.e(TAG, "=========onResume::naviMode is: " + naviMode + "============");
                Log.e(TAG, "=========onResume::current LatLng is: [" + latLng.latitude + " " + latLng.longitude + "]==========");

                walkButton = (ImageButton) findViewById(R.id.walk_mode);
                driveButton = (ImageButton) findViewById(R.id.drive_mode);
                cameraButton = (ImageButton) findViewById(R.id.btn_camera);

                if ("driving".equals(naviMode)) {
                    cameraButton.setVisibility(View.VISIBLE);
                    driveButton.setVisibility(View.INVISIBLE);
                    walkButton.setVisibility(View.VISIBLE);
                } else if ("walking".equals(naviMode)) {
                    cameraButton.setVisibility(View.VISIBLE);
                    driveButton.setVisibility(View.VISIBLE);
                    walkButton.setVisibility(View.INVISIBLE);
                }

                Log.e(TAG, "=========onResume::naviMode1 is: " + naviMode + "============");
                Log.e(TAG, "=========onResume::current LatLng1 is: [" + curLatLng.latitude + " " + curLatLng.longitude + "]==========");
                Log.e(TAG, "=========onResume::destination LatLng1 is: [" + latLng.latitude + " " + latLng.longitude + "]==========");
                if(curPosition.length!=0){
                    curLatLng = new LatLng(curPosition[0],curPosition[1]);
                    drawRoute(curLatLng, latLng, naviMode);
                }

                updateToMapNavigationView();

            }
        }
        }

    //if we arrived the destination, we will initialize the map
    private void clearMap(){
        walkButton = (ImageButton) findViewById(R.id.walk_mode);
        driveButton = (ImageButton) findViewById(R.id.drive_mode);
        cameraButton = (ImageButton) findViewById(R.id.btn_camera);
        cameraButton.setVisibility(View.INVISIBLE);
        driveButton.setVisibility(View.INVISIBLE);
        walkButton.setVisibility(View.INVISIBLE);
        mMap.clear();
    }


    // An AsyncTask class for accessing the GeoCoding Web Service
    private class GeocoderTask extends AsyncTask<String, Void, List<Address>> {
        @Override
        protected List<Address> doInBackground(String... locationName) {
            // Creating an instance of Geocoder class
            Geocoder geocoder = new Geocoder(getBaseContext());
            List<Address> addresses = null;
            try {
                // Getting a maximum of 3 Address that matches the input text
                addresses = geocoder.getFromLocationName(locationName[0], 3);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return addresses;
        }

        @Override
        protected void onPostExecute(List<Address> addresses) {
            if (addresses == null || addresses.size() == 0) {
                Toast.makeText(getBaseContext(), "No Location found", Toast.LENGTH_SHORT).show();
            }
            // Clears all the existing markers on the map
            mMap.clear();
            // Adding Markers on Google Map for each matching address
            for (int i = 0; i < addresses.size(); i++) {
                Address address = (Address) addresses.get(i);
                // Creating an instance of GeoPoint, to display in Google Map
                latLng = new LatLng(address.getLatitude(), address.getLongitude());
                String addressText = String.format("%s, %s",
                        address.getMaxAddressLineIndex() > 0 ? address.getAddressLine(0) : ""
                        , address.getCountryName());
                markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.title(addressText);
                mMap.addMarker(markerOptions);
                // Locate the first location
                if (i == 0)
                    mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
            }
        }
    }

    /**
     * get current location
     */
    public void getCurLocation(){
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //getting GPS status
        isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        //getting network status
        isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (!isGPSEnabled && !isNetworkEnabled) {
            showSettingAlert();
        } else {
            if (isNetworkEnabled) {
                // set the listener, update the location per 3 seconds(3*1000) automatically or moving more than 8 meters
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3 * 1000, 8, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        curLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        updateToCurLocation(location);
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {}

                    @Override
                    public void onProviderEnabled(String provider) {
                        if (ContextCompat.checkSelfPermission(MapBasedViewActivity.this,
                                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(MapBasedViewActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        Log.i("LastKnow:", location.toString());
                    }

                    @Override
                    public void onProviderDisabled(String provider) {
                        updateToCurLocation(null);
                    }
                });
            }else{
                if (isGPSEnabled) {
                    if (location == null) {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                                3 * 1000, 8, new LocationListener() {
                            @Override
                            public void onLocationChanged(Location location1) {
                                location = location1;
                                curLatLng = new LatLng(location1.getLatitude(),
                                        location1.getLongitude());
                                updateToCurLocation(location1);
                            }

                            @Override
                            public void onStatusChanged(String provider, int status, Bundle extras) {}

                            @Override
                            public void onProviderEnabled(String provider) {
                                if (ContextCompat.checkSelfPermission(MapBasedViewActivity.this,
                                        Manifest.permission.ACCESS_FINE_LOCATION)
                                        != PackageManager.PERMISSION_GRANTED
                                        && ContextCompat
                                        .checkSelfPermission(MapBasedViewActivity.this,
                                                Manifest.permission.ACCESS_COARSE_LOCATION)
                                        != PackageManager.PERMISSION_GRANTED) {
                                    return;
                                }
                                location = locationManager
                                        .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                                Log.i("LastKnow:", location.toString());
                            }

                            @Override
                            public void onProviderDisabled(String provider) {
                                updateToCurLocation(null);
                            }
                        });

                    }
                }
            }
        }
    }

    /**
     * Function to show setting alert dialog on pressing setting button will launch settings option
     */
    public void showSettingAlert(){
        AlertDialog.Builder alerDialog = new AlertDialog.Builder(getApplicationContext());
        //setting dialogHelp title
        alerDialog.setTitle("GPS settings");
        //setting dialogHelp message
        alerDialog.setMessage("GPS is not enabled. Do you want to go to settings option?");

        //on pressing settings button
        alerDialog.setPositiveButton("Settings",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        getApplicationContext().startActivity(intent);
                    }
                });
        //on pressing cancel button
        alerDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialogInterface,int i){
                        dialogInterface.cancel();
                    }
                });
        //showing alert message
        alerDialog.show();
    }

    /**
     * initialize the provider
     */
    private void initProvider() {
        //create LocationManager object
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        //check if GPS will work
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "Please open GPS...", Toast.LENGTH_SHORT).show();
            //jump to GPS configuration page
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(intent, 0);
            return;
        }

        //list all providers
        List<String> providers = locationManager.getAllProviders();

        Criteria criteria = new Criteria();
        bestProvider = locationManager.getBestProvider(criteria, false);

        //get the latest location

        location = locationManager.getLastKnownLocation(bestProvider);

       // System.out.println("latitude:" + location.getLatitude() + ", longitude:" + location.getLongitude());
    }

    /**
     * Initialize the search function
     */
    private void searchMap(){
        // Getting reference to btn_find of the layout activity_main
        ImageView btn_find = (ImageView) findViewById(R.id.btn_find);
        final TextView locationInfo = (TextView)findViewById(R.id.location_info);
        cardviewbottom = (CardView) findViewById(R.id.cardviewbottom);
        // Defining button click event listener for the find button
        View.OnClickListener findClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Getting user input location
                String location = etLocation.getText().toString();
                if(location!=null && !location.equals("")){
                    new GeocoderTask().execute(location);
                }
                //hide keyboard after you click search button
                InputMethodManager inputMethodManager =
                        (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(etLocation.getWindowToken(), 0);
                linearLayout.setVisibility(View.VISIBLE);
                cardviewbottom.setVisibility(View.VISIBLE);
                //after click search, we will display the mode buttons
                initNavigationgMode();
                //show destination information
                locationInfo.setText(location);
            }
        };
        // Setting button click event listener for the find button
        btn_find.setOnClickListener(findClickListener);
    }

    /**
     * Adds auto complete adapter to  auto complete text view.
     */
    private void setAutoAdapter(){
        etLocation.setAdapter(mAdapter);
        etLocation.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                final PlaceAutoCompleteAdapter.PlaceAutocomplete item = mAdapter.getItem(position);
                final String placeId = String.valueOf(item.placeId);
                Log.e(TAG, "Autocomplete item selected: " + item.description);

            }
        });
        etLocation.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (latLng != null) {
                    latLng = null;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

    }

    /**
     * update to the current location
     */
    private void updateToCurLocation(Location location){
        mMap.clear();
        markerOpt = new MarkerOptions();
        // Add a marker in Sydney
        double dLat = 43.0054446;
        double dLong = -87.9678884;

        if(location!=null){
            //get the latitude
            dLat = location.getLatitude();
            //get the longitude
            dLong = location.getLongitude();
        }
        //set the marker
        markerOpt.position(new LatLng(dLat, dLong));
        markerOpt.draggable(false);
        markerOpt.visible(true);
        markerOpt.anchor(0.5f, 0.5f);
        markerOpt.title("current location");
        mMap.addMarker(markerOpt);

        //move the camera to the current location
        cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(dLat,dLong))      //set the center of the map to current location
                .zoom(14)      //map zoom
                .bearing(0)    //Sets the orientation of the camera to east
                .tilt(90)      // Sets the tilt of the camera to 30 degrees
                .build();      // Creates a CameraPosition from the builder
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }




    private void drawRoute(LatLng origin, LatLng dest, String mode) {
        Router router = new Router(mMap, new UpdateIntent(){
            @Override
            public void updateIntent(final String result) {
                cameraButton.setVisibility(View.VISIBLE);
                cameraButton.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View view){
                        Intent intent = new Intent();
                        intent.setClass(MapBasedViewActivity.this, CameraBasedViewActivity.class);
                        intent.putExtra(ROUTE_JSON_DATA, result);
                        startActivityForResult(intent,REQUEST_CODE_CAMERA);
                    }
                });
            }
        });
        router.drawRoute(origin, dest, mode);
    }


    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, connectionResult.toString());
    }

    /**
     * Set walking and driving button
     */
    private void initNavigationgMode(){
        walkButton = (ImageButton)findViewById(R.id.walk_mode);
        driveButton = (ImageButton)findViewById(R.id.drive_mode);
        cameraButton = (ImageButton)findViewById(R.id.btn_camera);
        walkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //cameraButton.setVisibility(View.VISIBLE);
                walkButton.setVisibility(View.INVISIBLE);
                driveButton.setVisibility(View.VISIBLE);
                naviMode = "walking";
                drawRoute(curLatLng, latLng, naviMode);
                updateToMapNavigationView();
            }
        });
        driveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //cameraButton.setVisibility(View.VISIBLE);
                driveButton.setVisibility(View.INVISIBLE);
                walkButton.setVisibility(View.VISIBLE);
                naviMode = "driving";
                drawRoute(curLatLng, latLng, naviMode);
                updateToMapNavigationView();
            }
        });
    }

    /**
     * update to the map navigation view
     */
    private void updateToMapNavigationView(){
        mMap.clear();
        MarkerOptions markerOpt1 = new MarkerOptions();
        //set the marker
        markerOpt1.position(curLatLng);
        markerOpt1.draggable(false);
        markerOpt1.visible(true);
        markerOpt1.anchor(0.5f, 0.5f);
        markerOpt1.title("current location");
        mMap.addMarker(markerOpt1);

        //move the camera to the current location
        cameraPosition = new CameraPosition.Builder()
                .target(curLatLng)      //set the center of the map to current location
                .zoom(19)      //map zoom
                .bearing(0)    //Sets the orientation of the camera to east
                .tilt(90)      // Sets the tilt of the camera to 30 degrees
                .build();      // Creates a CameraPosition from the builder
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

}

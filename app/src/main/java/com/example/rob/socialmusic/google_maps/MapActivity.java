package com.example.rob.socialmusic.google_maps;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.rob.socialmusic.MainScreen;
import com.example.rob.socialmusic.R;
import com.example.rob.socialmusic.accounts.PersonalAccount;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.ErrorCallback;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.Metadata;
import com.spotify.sdk.android.player.PlaybackState;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.OnConnectionFailedListener, GoogleMap.OnMarkerClickListener, ConnectionStateCallback, Player.NotificationCallback {

    private static final String TAG = "MapActivity";
    // Client ID refers to Spotify developer code
    private static final String CLIENT_ID = "6f96f597a98646f3859436687fcc59ad";
    private static final String REDIRECT_URI = "socialmusic://callback";

    // Request code that will be used to verify if the result comes from correct activity
    private static final int REQUEST_CODE = 1337;

    // For Spotify player
    private Player mPlayer;
    private Metadata mMetadata;
    private SpotifyAppRemote mSpotifyAppRemote;
    private PlaybackState mCurrentPlaybackState;
    // For Firebase
    private DatabaseReference mDatabase;

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;
    private static final int PLACE_PICKER_REQUEST = 1;
    private static final LatLngBounds LAT_LNG_BOUNDS = new LatLngBounds(
            new LatLng(-40, -168), new LatLng(71, 136));
    public static final LatLng CURRENT_USER_LOCATION = new LatLng(0, 0);
    private long UPDATE_INTERVAL = 10 * 1000;  /* 10 secs */
    private long FASTEST_INTERVAL = 2000; /* 2 sec */


    //widgets
    private AutoCompleteTextView mSearchText;
    private ImageView mGps, mUserProfile;

    //vars
    private Boolean mLocationPermissionsGranted = false;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private PlaceAutocompleteAdapter mPlaceAutocompleteAdapter;
    private GoogleApiClient mGoogleApiClient;
    private PlaceInfo mPlace;
    private Marker mMarker;
    private Marker mUser;
    private Button ListenToSelectedTrackButton;
    private Boolean updating_marker = false;
    private Boolean marker_showing = false;

    private FirebaseAuth mAuth;
    FirebaseAuth.AuthStateListener mAuthListener;
    private LocationRequest mLocationRequest;

    // Spotify Variables

    private static String CurrentUserName = "";
    private static String CurrentTrack = "";
    private static String CurrentTrackID = "";
    private static Object[] userInfo = new Object[6];

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        mSearchText = findViewById(R.id.input_search);
        mGps = findViewById(R.id.ic_gps);
        mUserProfile = findViewById(R.id.user_profile);
        mAuth = FirebaseAuth.getInstance();
        ListenToSelectedTrackButton = findViewById(R.id.ListenToSelectedTrackButton);
        ListenToSelectedTrackButton.setVisibility(View.INVISIBLE);

        connect(true);


        ListenToSelectedTrackButton.setOnClickListener(view -> {
            getTrackId();

            mSpotifyAppRemote.getPlayerApi()
                    .subscribeToPlayerState()
                    .setEventCallback(playerState -> {
                        mSpotifyAppRemote.getPlayerApi().play(CurrentTrackID);
                    });

            // Song is possibly playing on computer, switch to local playback
            mSpotifyAppRemote.getConnectApi()
                    .connectSwitchToLocalDevice()
                    .setResultCallback(empty -> logMessage("Success!"))
                    .setErrorCallback(mErrorCallback);

            ListenToSelectedTrackButton.setVisibility(View.INVISIBLE);
        });

        startLocationUpdates(mMap);
        locationPermissionChecker();
        getLocationPermission();
    } // End onCreate

    public void connect(boolean showAuthView) {
        SpotifyAppRemote.disconnect(mSpotifyAppRemote);

        SpotifyAppRemote.connect(
                getApplication(),
                new ConnectionParams.Builder(CLIENT_ID)
                        .setRedirectUri(REDIRECT_URI)
                        .showAuthView(showAuthView)
                        .build(),
                new Connector.ConnectionListener() {
                    @Override
                    public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                        mSpotifyAppRemote = spotifyAppRemote;
                    }

                    @Override
                    public void onFailure(Throwable error) {
                        logMessage(String.format("Connection failed: %s", error));
                    }
                });
    } // End private void connect




    private void init() {
        Log.d(TAG, "init: initializing");

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();

        mSearchText.setOnItemClickListener(mAutocompleteClickListener);

        mPlaceAutocompleteAdapter = new PlaceAutocompleteAdapter(this, mGoogleApiClient,
                LAT_LNG_BOUNDS, null);

        mSearchText.setAdapter(mPlaceAutocompleteAdapter);

        // User selected address bar
        mSearchText.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || keyEvent.getAction() == KeyEvent.ACTION_DOWN
                    || keyEvent.getAction() == KeyEvent.KEYCODE_ENTER) {

                //execute our method for searching
                geoLocate();
            }
            return false;
        });

        // Get current location and zoom in on location
        mGps.setOnClickListener(view -> {
            getDeviceLocation();
        }); // mGps on click listener

        // More information about current user
        mUserProfile.setOnClickListener(view -> {
            getDeviceLocation();

            // Get Current User
            getUser();

            // Get User permission to go to  user profile
            AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
            builder.setMessage("Proceed to selected users (" + CurrentUserName + ") profile?");
            // Add the buttons
            builder.setPositiveButton("Proceed", (dialog, id) -> {
                Intent i = new Intent(this, PersonalAccount.class);
                startActivity(i);
            });
            builder.setNegativeButton("Cancel", (dialog, id) -> {});
            AlertDialog dialog = builder.create();
            dialog.show();

        }); // End - More information about current user

        hideSoftKeyboard();
    } // Initializing

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {} // End onConnectionFailed

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady: map is ready");
        mMap = googleMap;

        if (mLocationPermissionsGranted) {
            getDeviceLocation();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            } // Requesting permissions
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);

            // Set a listener for marker click.
            mMap.setOnMarkerClickListener(this);

            init();
        } // End mLocationPermissionsGranted
    } // End onMapReady

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(this, data);

                PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                        .getPlaceById(mGoogleApiClient, place.getId());
                placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
            }
        } // End requestCode == PLACE_PICKER_REQUEST
    } // End onActivityResult

    private void geoLocate() {
        Log.d(TAG, "geoLocate: geolocating");

        // Convert user text to string
        String searchString = mSearchText.getText().toString();

        Geocoder geocoder = new Geocoder(MapActivity.this);
        List<Address> list = new ArrayList<>();
        try {
            list = geocoder.getFromLocationName(searchString, 1);
        } catch (IOException e) {
            Log.e(TAG, "geoLocate: IOException: " + e.getMessage());
        }

        if (list.size() > 0) {
            Address address = list.get(0);

            Log.d(TAG, "geoLocate: found a location: " + address.toString());
            //Toast.makeText(this, address.toString(), Toast.LENGTH_SHORT).show();

            // Move camera to new location
            moveCamera(new LatLng(address.getLatitude(), address.getLongitude()), DEFAULT_ZOOM,
                    address.getAddressLine(0));
        } // End list.size
    } // End geoLocate



    // Trigger new location updates at interval
    protected void startLocationUpdates(GoogleMap map) {
        mMap = map;

        // Create the location request to start receiving updates
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

        // Create LocationSettingsRequest object using location request
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        // Check whether location settings are satisfied
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest);

        // Check user permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            getLocationPermission();
            return;
        }

        getFusedLocationProviderClient(this).requestLocationUpdates(mLocationRequest, new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {

                        onLocationChanged(locationResult.getLastLocation());
                        double currentLatitude = locationResult.getLastLocation().getLatitude();
                        double currentLongitude = locationResult.getLastLocation().getLongitude();

                        // Updating "CURRENT_USER_LOCATION" to latest co-ordinates
                        LatLng CURRENT_USER_LOCATION = new LatLng(currentLatitude, currentLongitude);

                        // Check if Google maps is updating
                        if (updating_marker) {
                                // Update user marker
                            // Get Current User
                            getTrack();
                            mUser.setPosition(CURRENT_USER_LOCATION);
                            try{
                                mUser.setSnippet(userInfo[0].toString());
                                mUser.hideInfoWindow();
                                mUser.showInfoWindow();
                            } catch (NullPointerException e){
                                mUser.setSnippet("Unable to get current track");
                            }

                            if(mUser.isInfoWindowShown()) {
                                marker_showing = true;
                                ListenToSelectedTrackButton.setVisibility(View.VISIBLE);
                            } else {
                                marker_showing = false;
                                ListenToSelectedTrackButton.setVisibility(View.INVISIBLE);
                            }
                        } else {
                            getUser();
                            getTrack();


                            // Creating the first marker on the map
                            // This will include Track and Artist
                            mUser = mMap.addMarker(new MarkerOptions()
                                    .position(CURRENT_USER_LOCATION)
                                    .draggable(false)
                                    .title(CurrentUserName)
                                    .snippet("Retrieving track"));

                        }

                        // Google maps refreshing
                        updating_marker = true;
                    } // End onLocationResult
                },
                Looper.myLooper());
    } // End startLocationUpdates



    public String getUser() {
        // Access current User information
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            CurrentUserName = user.getDisplayName();
        } else {
            Toast.makeText(MapActivity.this, "Username required, please set in your personal settings",Toast.LENGTH_SHORT).show();
            Intent i = new Intent(this, PersonalAccount.class);
            startActivity(i);
        }
        return CurrentUserName;
    } // End getUserString


    public String getTrackId() {
        // Access current User information
        // Firebase information
        FirebaseUser user =  mAuth.getCurrentUser();
        String userId = user.getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(userId).child("Current Track ID");
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                CurrentTrackID = dataSnapshot.getValue().toString();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MapActivity.this, "Issue connecting with database", Toast.LENGTH_SHORT).show();
            }
        });
        return CurrentTrackID;
    }

    // Function to check if location settings are enabled
    private void locationPermissionChecker() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        boolean gps_enabled = false;
        boolean network_enabled = false;

        // Check location services is enabled
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) { }

        // Check network is enabled
        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) { }

        // If location services is not enabled, return back to location services activity
        if (!gps_enabled && !network_enabled) {
            Intent i = new Intent(this, com.example.rob.socialmusic.google_maps.LocationServices.class);
            startActivity(i);
        } // Emd if(!gps_enabled && !network_enabled)

    } // End locationPermissionChecker

    private void getTrack() {
        // Make the artwork un-clickable
        // Retrieve information from Spotify
        if (mSpotifyAppRemote != null) {
            mSpotifyAppRemote.getPlayerApi()
                    .subscribeToPlayerState()
                    .setEventCallback(playerState -> {
                        // Check for updated track or artist
                        CurrentTrack = playerState.track.name;
                        userInfo[0] = CurrentTrack;
                    }); // End Try

        } // End if mSpotifyAppRemote != null
    } // End UpdateView

    private void getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: getting the devices current location");
        // Check user permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            getLocationPermission();
            return;
        }

        // Get last known recent location using new Google Play Services SDK (v11+)
        FusedLocationProviderClient locationClient = getFusedLocationProviderClient(this);
        // End onSuccess
        // End onFailure
        locationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    // GPS location can be null if GPS is switched off
                    if (location != null) {
                        onLocationChanged(location);
                        double currentLatitude = location.getLatitude();
                        double currentLongitude = location.getLongitude();

                        LatLng CURRENT_USER_LOCATION = new LatLng(currentLatitude, currentLongitude);

                        // Firebase information
                        FirebaseDatabase database =  FirebaseDatabase.getInstance();
                        FirebaseUser user =  mAuth.getCurrentUser();
                        String userId = user.getUid();
                        DatabaseReference mRef =  database.getReference().child("Users").child(userId);
                        mRef.child("Current Location").setValue(CURRENT_USER_LOCATION);

                        moveCamera(new LatLng(currentLatitude, currentLongitude),
                                DEFAULT_ZOOM,
                                "My Location");
                    } // End if - location != null
                })
                .addOnFailureListener(e -> {
                    Log.d("MapActivity", "Error trying to get last GPS location");
                    e.printStackTrace();
                });

    } // End getDeviceLocation

    private void moveCamera(LatLng latLng, float zoom, PlaceInfo placeInfo){
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude );
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        mMap.clear();

        if(placeInfo != null){
            try{
                String snippet = "Address: " + placeInfo.getAddress() + "\n";

                MarkerOptions options = new MarkerOptions()
                        .position(latLng)
                        .title(placeInfo.getName())
                        .snippet(snippet);
                mMarker = mMap.addMarker(options);

            }catch (NullPointerException e){
                Log.e(TAG, "moveCamera: NullPointerException: " + e.getMessage() );
            } // End Try / Catch
        }else{
            mMap.addMarker(new MarkerOptions().position(latLng));
        } // End if - placeInfo != null

        hideSoftKeyboard();
    } // End moveCamera

    private void moveCamera(LatLng latLng, float zoom, String title){
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude );
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        if(!title.equals("My Location")){
            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .title(title);
            mMap.addMarker(options);
        } // End if - !title.equals("My Location")
        hideSoftKeyboard();
    } // End moveCamera

    private void initMap(){
        Log.d(TAG, "initMap: initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapActivity.this);
    } // End initMap

    private void getLocationPermission(){
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

        // Check if FINE_LOCATION is enabled
        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            // Check if COURSE_LOCATION is enabled
            if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                mLocationPermissionsGranted = true;
                initMap();
            } else {
                ActivityCompat.requestPermissions(this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            } // End if FINE_LOCATION is enabled
        } else {
            ActivityCompat.requestPermissions(this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    } // End getLocationPermission

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: called.");
        mLocationPermissionsGranted = false;

        switch(requestCode){
            case LOCATION_PERMISSION_REQUEST_CODE:{
                if(grantResults.length > 0){
                    for(int i = 0; i < grantResults.length; i++){
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                            mLocationPermissionsGranted = false;
                            Log.d(TAG, "onRequestPermissionsResult: permission failed");
                            return;
                        }
                    }
                    Log.d(TAG, "onRequestPermissionsResult: permission granted");
                    mLocationPermissionsGranted = true;
                    //initialize our map
                    initMap();
                }
            }
        }
    } // End onRequestPermissionsResult

    private void hideSoftKeyboard(){
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    } // End hideSoftKeyboard


    // Google places API autocomplete suggestions
    private AdapterView.OnItemClickListener mAutocompleteClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            hideSoftKeyboard();

            final AutocompletePrediction item = mPlaceAutocompleteAdapter.getItem(i);
            final String placeId = item.getPlaceId();

            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi.getPlaceById(mGoogleApiClient, placeId);
            placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
        } // End onItemClick
    };

    public void onLocationChanged(Location location) {
        // New location has now been determined
        String msg = "Updated Location: " +
                Double.toString(location.getLatitude()) + "," +
                Double.toString(location.getLongitude());
    } // End onLocationChanged

    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(@NonNull PlaceBuffer places) {
            if(!places.getStatus().isSuccess()){
                Log.d(TAG, "onResult: Place query did not complete successfully: " + places.getStatus().toString());
                places.release();
                return;
            }
            final Place place = places.get(0);

            try{
                mPlace = new PlaceInfo();
                mPlace.setLatlng(place.getLatLng());
                mPlace.setRating(place.getRating());
            } catch (NullPointerException e){
                Log.e(TAG, "onResult: NullPointerException: " + e.getMessage() );
            }

            // Move camera to place
            moveCamera(new LatLng(place.getViewport().getCenter().latitude,
                    place.getViewport().getCenter().longitude), DEFAULT_ZOOM, mPlace);
            // Release the place buffer, to avoid memory leaks
            places.release();
        }
    };

    @Override
    protected void onPause() {
        super.onPause();

        // The Spotify.destroyPlayer() also removes any callbacks.
        // The following prevents unnecessary work while the player is on onPause
        if (mPlayer != null) {
            mPlayer.removeNotificationCallback(MapActivity.this);
            mPlayer.removeConnectionStateCallback(MapActivity.this);
        } // End if (mPlayer != null)
   } // End onPause

    @Override
    protected void onStart() {
        super.onStart();
        ConnectionParams connectionParams =
                new ConnectionParams.Builder(CLIENT_ID)
                        .setRedirectUri(REDIRECT_URI)
                        .showAuthView(true)
                        .build();
        SpotifyAppRemote.connect(this, connectionParams,
                new Connector.ConnectionListener() {
                    public void onConnected(SpotifyAppRemote spotifyAppRemote) {  mSpotifyAppRemote = spotifyAppRemote; }
                    public void onFailure(Throwable throwable) { }
                });
    } // End onConnect

    // Prevent user from pressing on back button
    @Override
    public void onBackPressed() {
        // Return to Main Screen when successful
        Intent i = new Intent(MapActivity.this, MainScreen.class);
        startActivity(i);
    } // End onBackPressed

    // Issue playing Spotify Track
    private final ErrorCallback mErrorCallback = t -> Log.d( "Spotify Issues", "An expected error has occurred");
    private void logMessage(String msg) {
        Log.d(TAG, msg);
    } // End logMessage
    @Override
    public void onLoggedIn() {}
    @Override
    public void onLoggedOut() {Log.d("SpotifyLogin", "User logged out");}
    @Override
    public void onLoginFailed(Error var1) { Log.d("SpotifyLogin", "Login failed");}
    @Override
    public void onTemporaryError() {Log.d("SpotifyLogin", "Temporary error occurred");}
    @Override
    public void onConnectionMessage(String message) {Log.d("SpotifyLogin", "Received connection message: " + message);}

    @Override
    public void onPlaybackEvent(PlayerEvent playerEvent) {
        Log.d("SpotifyLogin", "Playback event received: " + playerEvent.name());
        mCurrentPlaybackState = mPlayer.getPlaybackState();
        mMetadata = mPlayer.getMetadata();
    } // End onPlaybackEvent

    @Override
    public void onPlaybackError(Error error) {
        Log.d("SpotifyLogin", "Playback error received: " + error.name());
        switch (error) {
            // Handle error type as necessary
            default:
                break;
        } // End Switch
    } // End onPlaybackError

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }
}
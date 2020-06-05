package com.example.rob.socialmusic.spotify;

// Importing the necessary files for Spotify SDK
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.rob.socialmusic.MainScreen;
import com.example.rob.socialmusic.R;
import com.example.rob.socialmusic.accounts.AccountLogin;
import com.example.rob.socialmusic.accounts.PersonalAccount;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.ErrorCallback;
import com.spotify.protocol.client.PendingResult;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.Capabilities;
import com.spotify.protocol.types.ImageUri;
import com.spotify.protocol.types.PlayerContext;
import com.spotify.protocol.types.PlayerState;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Connectivity;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.Metadata;
import com.spotify.sdk.android.player.PlaybackState;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.SpotifyPlayer;
import com.squareup.picasso.Picasso;


public class SpotifyLogin extends Activity implements SpotifyPlayer.NotificationCallback, ConnectionStateCallback {
    // Client ID refers to Spotify developer code
    private static final String CLIENT_ID = "6f96f597a98646f3859436687fcc59ad";
    private static final String REDIRECT_URI = "socialmusic://callback";
    // Request code that will be used to verify if the result comes from correct activity
    private static final int REQUEST_CODE = 1337;
    private static final String TAG = SpotifyLogin.class.getSimpleName();
    private Context context;
    private Boolean missingArtworkBoolean = true;
    private SpotifyAppRemote mSpotifyAppRemote;
    private BroadcastReceiver mNetworkStateReceiver;
    private PlaybackState mCurrentPlaybackState;
    private Metadata mMetadata;

    // Errors required for Spotify SDK
    private final ErrorCallback mErrorCallback = t -> Log.d( "Boom! Big Issues!", "An expected error has occurred");
    private final Player.OperationCallback mOperationCallback = new Player.OperationCallback() {
        @Override
        public void onSuccess() {
            Log.d("OK!","OK!");
        }
        @Override
        public void onError(Error error) {
            Log.d("ERROR!","ERROR!");
        }
    };

    private static String TRACK_URI = "";
    private TextView TrackName;
    private TextView ArtistName;
    private TextView MiddleSlash;
    private TextView LeftTime;
    private TextView RightTime;
    private TextView retrievingSpotify;
    private TextView FavouriteButtonText;
    private Button PlayPause;
    private Button PreviousButton;
    private Button FavouriteButton;
    private Button NextButton;
    private Button BackButton;
    private ImageView coverArtView;
    private ProgressBar spinner;
    private RelativeLayout Progresslayout;

    // Boolean
    private Boolean isplaying = false;
    private Boolean fieldsRetrieved = false;

    // Handler
    private Handler mHandler;

    // Firebase updater
    private FirebaseAuth mAuth;
    FirebaseAuth.AuthStateListener mAuthListener;
    private DatabaseReference mDatabase;
    private Player mPlayer;
    private Subscription<PlayerState> mPlayerStateSubscription;
    private Subscription<PlayerContext> mPlayerContextSubscription;
    private Subscription<Capabilities> mCapabilitiesSubscription;

    // Spotify Variables
    private static String CurrentTrack = "";
    private static String CurrentArtist = "";
    private static String CurrentTrackID = "";
    private static long CurrentTime = 0;
    private static long TrackDuration = 0;
    private static Object[] userInfo = new Object[6];
    private static int FavouriteTrackCounter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.example.rob.socialmusic.R.layout.activity_spotify_login);

        TrackName = findViewById(com.example.rob.socialmusic.R.id.TrackNameTextView);
        ArtistName = findViewById(com.example.rob.socialmusic.R.id.ArtistNameTextView);
        MiddleSlash = findViewById(com.example.rob.socialmusic.R.id.middleTimeSlash);
        LeftTime = findViewById(R.id.LeftTime);
        RightTime = findViewById(R.id.RightTime);
        retrievingSpotify = findViewById(R.id.retrieving_artwork);
        PlayPause = findViewById(com.example.rob.socialmusic.R.id.PlayPauseButton);
        PreviousButton = findViewById(com.example.rob.socialmusic.R.id.PreviousButton);
        NextButton = findViewById(com.example.rob.socialmusic.R.id.NextButton);
        coverArtView = findViewById(R.id.AlbumArtImageView);
        spinner = findViewById(R.id.progressBar);
        Progresslayout = findViewById(R.id.ProgressLayout);
        FavouriteButton = findViewById(R.id.FavouriteButton);
        FavouriteButtonText = findViewById(R.id.FavouriteButtonText);
        BackButton = findViewById(R.id.BackButton);

        mAuth = FirebaseAuth.getInstance();
        connect(true);
        userAuthentication();
        hideFields();
        updateView();

        BackButton.setOnClickListener(v -> {
            fieldsRetrieved = false;
            finish();
        });

        FavouriteButton.setOnClickListener(v -> {
            // Get User permission to go to  user profile
            AlertDialog.Builder builderMissingArtwork = new AlertDialog.Builder(SpotifyLogin.this);
            builderMissingArtwork.setMessage("Add song to favourites?");
            // Add the buttons
            builderMissingArtwork.setPositiveButton("Add", (dialog, id) -> {
                // Save latest information
                // Firebase information
                FirebaseDatabase database =  FirebaseDatabase.getInstance();
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                String userId = user.getUid();

                if(userInfo[0] != null){
                    QueryFavourites();
                    // Check how many favourite songs the user has, this will be  used to populate their personal account
                    DatabaseReference mRef =  database.getReference().child("Users").child(userId).child("Favourite Songs");
                    mRef.child("Favourite Track " + FavouriteTrackCounter).setValue(userInfo[0]);
                    mRef.child("Favourite Artist " + FavouriteTrackCounter).setValue(userInfo[1]);
                    mRef.child("Favourite Track ID " + FavouriteTrackCounter).setValue(userInfo[3]);

                    // Proceed to personal account
                    Intent i = new Intent(SpotifyLogin.this, PersonalAccount.class);
                    startActivity(i);
                }
            });
            builderMissingArtwork.setNegativeButton("Cancel", (dialog, id) -> {});
            AlertDialog dialog = builderMissingArtwork.create();
            dialog.show();
        });

        coverArtView.setOnClickListener(v -> {
            if(missingArtworkBoolean){
                // Get User permission to go to  user profile
                AlertDialog.Builder builderMissingArtwork = new AlertDialog.Builder(SpotifyLogin.this);
                builderMissingArtwork.setMessage("Open Spotify to Fix connection issue? If Spotify isn't installed, you'll be directed to the market");
                // Add the buttons
                builderMissingArtwork.setPositiveButton("Proceed", (dialog, id) -> {
                    // Go to user profile
                    String appName = "Spotify";
                    String spotifyPackageName = "com.spotify.music";
                    Intent intent = getPackageManager().getLaunchIntentForPackage(spotifyPackageName);
                    if (intent != null) {
                        // The activity was found, now start the activity
                        // Open Spotify
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    } else {
                        // Bring user to the market or let them choose an app?
                        intent = new Intent(Intent.ACTION_VIEW);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.setData(Uri.parse("market://details?id=" + spotifyPackageName));
                        startActivity(intent);
                    } // End if (intent != null)

                });
                builderMissingArtwork.setNegativeButton("Cancel", (dialog, id) -> {});
                AlertDialog dialog = builderMissingArtwork.create();
                dialog.show();
            } // End if(missingArtworkBoolean = true)
        });

        // Check current status of isplaying
        PlayPause.setOnClickListener(v -> {
             if(isplaying){
                mSpotifyAppRemote.getPlayerApi().pause().setResultCallback(result -> logMessage("Pause successful")).setErrorCallback(mErrorCallback);
                updateView();
            } else {
                mSpotifyAppRemote.getPlayerApi().resume().setResultCallback(result -> logMessage("Play successful")).setErrorCallback(mErrorCallback);
                // Song is possibly playing on computer, switch to local playback
                mSpotifyAppRemote.getConnectApi()
                         .connectSwitchToLocalDevice()
                         .setResultCallback(empty -> logMessage("Success!"))
                         .setErrorCallback(mErrorCallback);

                updateView();
            } // End if(isplaying)
        });

        // Previous Song
        PreviousButton.setOnClickListener(v -> {
            mSpotifyAppRemote.getPlayerApi().skipPrevious().setResultCallback(result -> logMessage("Previous successful")).setErrorCallback(mErrorCallback);
            updateView();
        });

        // Next Song
        NextButton.setOnClickListener(v -> {
            mSpotifyAppRemote.getPlayerApi().skipNext().setResultCallback(result -> logMessage("Skip successful")).setErrorCallback(mErrorCallback);
            updateView();
        }); // End onClick

        mAuthListener = firebaseAuth -> {
            // If Firebase fails to retrieve a user, send user to main screen
            if (firebaseAuth.getCurrentUser() != null){
                // Launch to MainScreen when successful
                Intent i = new Intent(SpotifyLogin.this, AccountLogin.class);
                startActivity(i);
            }
        }; // End Firebase authentication listener
    } // End on Create

    // Query number of favourited songs
    public void QueryFavourites(){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String userId = user.getUid();

        // Query in Favourite Songs
        Query query = reference.child("Users").child(userId).child("Favourite Songs").orderByChild("Favourite Track");
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // For each value in "Favourite Track of database + 1
                if (dataSnapshot.exists()) {  FavouriteTrackCounter += 1; }
            } // End onDataChange

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    } // End QueryFavourites

    // Showing and hiding progress bar for user
    public void hideFields() {
        if(!fieldsRetrieved){
            // Show spinner to load next layout
            spinner.setVisibility(View.VISIBLE);
            retrievingSpotify.setVisibility(View.VISIBLE);

            // Hide music playing layout
            PlayPause.setVisibility(View.INVISIBLE);
            PreviousButton.setVisibility(View.INVISIBLE);
            NextButton.setVisibility(View.INVISIBLE);
            coverArtView.setVisibility(View.INVISIBLE);
            TrackName.setVisibility(View.INVISIBLE);
            ArtistName.setVisibility(View.INVISIBLE);
            LeftTime.setVisibility(View.INVISIBLE);
            RightTime.setVisibility(View.INVISIBLE);
            MiddleSlash.setVisibility(View.INVISIBLE);
            FavouriteButton.setVisibility(View.INVISIBLE);
            FavouriteButtonText.setVisibility(View.INVISIBLE);
        } else {
            // Hide spinner to load next layout
            Progresslayout.setVisibility(View.GONE);
            spinner.setVisibility(View.GONE);
            retrievingSpotify.setVisibility(View.GONE);

            // Retrieve music playing layout
            PlayPause.setVisibility(View.VISIBLE);
            PreviousButton.setVisibility(View.VISIBLE);
            NextButton.setVisibility(View.VISIBLE);
            coverArtView.setVisibility(View.VISIBLE);
            TrackName.setVisibility(View.VISIBLE);
            ArtistName.setVisibility(View.VISIBLE);
            LeftTime.setVisibility(View.VISIBLE);
            RightTime.setVisibility(View.VISIBLE);
            MiddleSlash.setVisibility(View.VISIBLE);
            FavouriteButton.setVisibility(View.VISIBLE);
            FavouriteButtonText.setVisibility(View.VISIBLE);
        }
    }

    public void userAuthentication() {
        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI);
        builder.setScopes(new String[]{"user-read-private", "streaming", "user-read-currently-playing", "user-read-playback-state", "user-library-read", "user-library-modify", "user-read-recently-played", "user-top-read"});
        AuthenticationRequest request = builder.build();
        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
    }

    // Auto update update view every second
    private final Runnable m_Runnable = new Runnable(){
        public void run() {
            // Update every second (as per song track)
            SpotifyLogin.this.mHandler.postDelayed(m_Runnable, 1000);
            updateView();

           // If user spotify information is blank, don't update firebase
            if(userInfo[0] == null){
                Log.d("Spotify Information", "Information retrieved from Spotify is blank");
            } else {
                // Firebase information
                FirebaseDatabase database =  FirebaseDatabase.getInstance();
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                String userId = user.getUid();

                DatabaseReference mRef =  database.getReference().child("Users").child(userId);
                mRef.child("Current Track").setValue(userInfo[0]);
                mRef.child("Current Artist").setValue(userInfo[1]);
                mRef.child("Current Time").setValue(userInfo[2]);
                mRef.child("Current Track ID").setValue(userInfo[3]);
            } // End if(userInfo[0] == null)
        } // End run
    };

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
                    } // End onConnected

                    @Override
                    public void onFailure(Throwable error) {
                        logMessage(String.format("Connection failed: %s", error));
                        Toast.makeText(SpotifyLogin.this, "Issue connecting with Spotify", Toast.LENGTH_SHORT).show();
                        Intent i = new Intent(SpotifyLogin.this, MainScreen.class);
                        startActivity(i);
                    } // End onFailure
                });
    } // End private void connect

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume " + "onResume activity");
        // Set up the broadcast receiver for network events. Note that we also unregister
        // this receiver again in onPause().
        mNetworkStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (mPlayer != null) {
                    Connectivity connectivity = getNetworkConnectivity(getBaseContext());
                    mPlayer.setConnectivityStatus(mOperationCallback, connectivity);
                } // End if (mPlayer != null)
            }
        };
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mNetworkStateReceiver, filter);

        this.mHandler = new Handler();
        this.mHandler.postDelayed(m_Runnable,5000);
    } // End onResume

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mNetworkStateReceiver);

        // The Spotify.destroyPlayer() also removes any callbacks.
        // The following prevents unnecessary work while the player is on onPause
        if (mPlayer != null) {
            mPlayer.removeNotificationCallback(SpotifyLogin.this);
            mPlayer.removeConnectionStateCallback(SpotifyLogin.this);
        } // End if (mPlayer != null)
    } // End onPause

    // Update Views
    // Update all Textviews, Images and Buttons to current information
    private void updateView() {
        // Make the artwork un-clickable
        missingArtworkBoolean = false;
        // Retrieve information from Spotify
        if (mSpotifyAppRemote != null) {
            mSpotifyAppRemote.getPlayerApi()
                    .subscribeToPlayerState()
                    .setEventCallback(playerState -> {
                        // Pulling current song track name
                        CurrentTrack = playerState.track.name;
                        userInfo[0] = CurrentTrack;
                        TrackName.setText(userInfo[0].toString());

                        // Pulling current artist
                        CurrentArtist = playerState.track.artist.name;
                        userInfo[1] = CurrentArtist;
                        ArtistName.setText(userInfo[1].toString());

                        // Pulling current song time and formatting appropriately
                        CurrentTime = playerState.playbackPosition;
                        userInfo[2] = CurrentTime;
                        String CurrentTimeString = DateFormat.format("mm:ss", CurrentTime).toString();
                        LeftTime.setText(CurrentTimeString);

                        // Pulling current song duration and formatting appropriately
                        TrackDuration = playerState.track.duration;
                        userInfo[4] = TrackDuration;
                        String TrackDurationString = DateFormat.format("mm:ss", TrackDuration).toString();
                        RightTime.setText(TrackDurationString);

                        // Pulling current track URL
                        CurrentTrackID = playerState.track.uri;
                        userInfo[3] = CurrentTrackID;

                        // Pulling current album cover
                        userInfo[6] = playerState.track.imageUri;
                    }); // End Try

                    PendingResult<PlayerState> playerStatePendingResult = mSpotifyAppRemote.getPlayerApi()
                            .getPlayerState()
                            .setResultCallback(result -> {

                            // Load current album cover
                            ImageUri TrackURL = result.track.imageUri;

                            mSpotifyAppRemote.getImagesApi()
                                    .getImage(result.track.imageUri)
                                    .setResultCallback(coverArtView::setImageBitmap);

                            /*
                                The following code was implemented to prevent the artwork
                                from refreshing constantly.
                            */
                            if (!TRACK_URI.equals(userInfo[6])){
                                // Attempt to place updated artwork, if this fails, replace with missing_artwork logo
                                try {
                                    Picasso.with(SpotifyLogin.this)
                                            .load(String.valueOf(TrackURL))
                                            .into(coverArtView);
                                } catch (NullPointerException e) {
                                    coverArtView.setImageResource(R.drawable.missing_artwork);
                                    missingArtworkBoolean = true;
                                } // End Try
                            }
                    });

                // Check if the device is playing audio
                AudioManager manager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
                if (manager.isMusicActive()) {
                    isplaying = true;
                    // Change media logo to pause
                    PlayPause.setBackgroundResource(R.drawable.pause_logo);
                } else {
                    isplaying = false;
                    // Change media logo to play
                    PlayPause.setBackgroundResource(R.drawable.play_logo);
                } // End checking system audio

                fieldsRetrieved = true;
                hideFields();
            } else {// End if (mSpotifyAppRemote != null)
                TrackName.setText(getString(R.string.spotify_login_issue_connecting));
                TrackName.setVisibility(View.VISIBLE);
                ArtistName.setText(getString(R.string.spotify_login_error_icon));
                ArtistName.setVisibility(View.VISIBLE);
                coverArtView.setImageResource(R.drawable.missing_artwork);
                Log.i(TAG, "mSpotifyAppRemote:" + " Null Pointer Exception");
        } // End if mSpotifyAppRemote != null
    } // End UpdateView

    private Connectivity getNetworkConnectivity(Context context) {
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected()) {
            return Connectivity.fromNetworkType(activeNetwork.getType());
        } else {
            return Connectivity.OFFLINE;
        }
    } // End Connectivity

    @Override
    public void onPlaybackEvent(PlayerEvent playerEvent) {
        Log.d("SpotifyLogin", "Playback event received: " + playerEvent.name());
        mCurrentPlaybackState = mPlayer.getPlaybackState();
        mMetadata = mPlayer.getMetadata();

        Log.i(TAG, "Player state: " + mCurrentPlaybackState);
        Log.i(TAG, "Metadata: " + mMetadata);
        updateView();
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
    public void onBackPressed() {
        super.onBackPressed();
        // If the user leaves the spotify section of the app, hide the fields again
        fieldsRetrieved = false;
    } // End on Back Pressed

    @Override
    public void onLoggedIn() {Log.d("SpotifyLogin", "User logged in"); } // End onLoggedIn
    @Override
    public void onLoggedOut() {Log.d("SpotifyLogin", "User logged out");}
    @Override
    public void onLoginFailed(Error var1) { Log.d("SpotifyLogin", "Login failed");}
    @Override
    public void onTemporaryError() {Log.d("SpotifyLogin", "Temporary error occurred");}
    @Override
    public void onConnectionMessage(String message) {Log.d("SpotifyLogin", "Received connection message: " + message);}

    private void logMessage(String msg) {
        Log.d(TAG, msg);
    } // End logMessage
} // End Spotify Login
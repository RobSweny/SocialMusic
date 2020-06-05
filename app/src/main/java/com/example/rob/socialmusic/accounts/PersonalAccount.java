package com.example.rob.socialmusic.accounts;


import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.rob.socialmusic.MainScreen;
import com.example.rob.socialmusic.R;
import com.google.android.gms.location.LocationRequest;
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
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.Metadata;
import com.spotify.sdk.android.player.PlaybackState;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;

public class PersonalAccount extends AppCompatActivity implements ConnectionStateCallback, Player.NotificationCallback  {

    // For Spotify player
    private Player mPlayer;
    private Metadata mMetadata;
    private SpotifyAppRemote mSpotifyAppRemote;
    private PlaybackState mCurrentPlaybackState;
    private static final String TAG = "MapActivity";
    private static final String CLIENT_ID = "6f96f597a98646f3859436687fcc59ad";
    private static final String REDIRECT_URI = "socialmusic://callback";
    private static final int REQUEST_CODE = 1337;
    private final ErrorCallback mErrorCallback = t -> Log.d( "Spotify Issues", "An expected error has occurred");
    private void logMessage(String msg) {
        Log.d(TAG, msg);
    } // End logMessage


    // Firebase
    FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private LocationRequest mLocationRequest;

    // Initializing Button
    private Button FavouritesPlaySong;
    private Button FavouritesRemoveSong;
    private Button BackButton;
    private TextView FavouritesTrackNameTextView;
    private TextView FavouritesArtistNameTextView;
    private TextView RetrievingTracks;
    private LinearLayout ScrollListing;
    private int informationRetrieved = 0;
    private static int FavouriteTrackCounter;
    private Boolean dataRetrieved = false;

    // Handler
    private Handler mHandler;

    // Object to hold firebase information
    private static Object[] userInfo = new Object[50];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_account);

        // Initializing
        BackButton = findViewById(R.id.BackButton);
        RetrievingTracks = findViewById(R.id.retrieving_tracks);
        ScrollListing = findViewById(R.id.ScrollListing);


        mAuth = FirebaseAuth.getInstance();
        QueryFavourites();
        getFavourites();

        RetrievingTracks.setText("Retrieving Tracks");

        this.mHandler = new Handler();
        this.mHandler.postDelayed(m_Runnable,2000);

        // Back button listener
        BackButton.setOnClickListener(v -> {
            Intent i=new Intent(PersonalAccount.this, MainScreen.class);
            startActivity(i);
        }); // End BackButton
    } // End onCreate

    // Auto update update view every second
    private final Runnable m_Runnable = new Runnable(){
        int i = 0;
        public void run() {
            // Update every 2 seconds to update dynamic buttons
            PersonalAccount.this.mHandler.postDelayed(m_Runnable, 2000);
            if(dataRetrieved){
                if(i < 1){
                    createButtons();
                    i++;
                }
            }
        } // End run
    };

    public void createButtons(){
        for(int i = 0; i < FavouriteTrackCounter; i++){
            if(FavouriteTrackCounter != 0){
                RetrievingTracks.setVisibility(View.GONE);

                FavouritesTrackNameTextView = new TextView(this);
                FavouritesArtistNameTextView = new TextView(this);
                FavouritesPlaySong = new Button(this);
                FavouritesRemoveSong = new Button(this);


                FavouritesTrackNameTextView.setText("Track Name: " + userInfo[i]);
                FavouritesArtistNameTextView.setText("Artist Name: " + userInfo[i + 1]);
                FavouritesTrackNameTextView.setTextColor(Color.WHITE);
                FavouritesArtistNameTextView.setTextColor(Color.WHITE);

                // Add to LinearView
                ScrollListing.addView(FavouritesTrackNameTextView);
                ScrollListing.addView(FavouritesArtistNameTextView);
                ScrollListing.addView(FavouritesPlaySong);
                ScrollListing.addView(FavouritesRemoveSong);

                FavouritesPlaySong.setText("Play Song");
                FavouritesRemoveSong.setText("Remove Song");

                int i_holderPlay = i;
                FavouritesPlaySong.setOnClickListener(v -> mSpotifyAppRemote.getPlayerApi()
                        .subscribeToPlayerState()
                        .setEventCallback(playerState -> {
                            mSpotifyAppRemote.getPlayerApi().play(String.valueOf(userInfo[i_holderPlay + 2]));
                        }));

                        // Song is possibly playing on computer, switch to local playback
                        mSpotifyAppRemote.getConnectApi()
                                .connectSwitchToLocalDevice()
                                .setResultCallback(empty -> logMessage("Success!"))
                                .setErrorCallback(mErrorCallback);

                // Remove song from Favourites
                int i_holderRemove = i;
                FavouritesRemoveSong.setOnClickListener(v -> {
                    removeFavouriteSong(i_holderRemove);
                });
            }
        } // End for
        RetrievingTracks.setText("There are no Favourite Tracks");
        // Reset Favourite Track Counter
        FavouriteTrackCounter = 0;
    }

    public void removeFavouriteSong(int i){
        // Check if user already deleted song from favourites
            // Firebase information
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            String userId = user.getUid();

            Query query = reference.child("Users").child(userId).child("Favourite Songs").orderByChild("Favourite Track");
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot data: dataSnapshot.getChildren()) {
                        data.getRef().removeValue();

                        if(data.equals(userInfo[i].toString())){
                            data.getRef().removeValue();
                        }
                        if(data.equals(userInfo[i+1].toString())){
                            data.getRef().removeValue();
                        }
                        if(data.equals(userInfo[i+2].toString())){
                            data.getRef().removeValue();
                        }

                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(TAG, "onCancelled", databaseError.toException());
                }
            });

            recreate();
    }

    public void QueryFavourites(){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String userId = user.getUid();

        Query query = reference.child("Users").child(userId).child("Favourite Songs").orderByChild("Favourite Track");
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    FavouriteTrackCounter += 1;
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {  }
        });
    } // End QueryFavourites


    public void getFavourites() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String userId = user.getUid();

        Query query = reference.child("Users").child(userId).child("Favourite Songs");
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    int i = 0;
                    for (DataSnapshot data : dataSnapshot.getChildren()) {
                        userInfo[i] = data.getValue().toString();
                        i++;
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {  }
        });
        dataRetrieved = true;
    } // End getFavouriteTrackNames

    // Required for Spotify
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
                    public void onConnected(SpotifyAppRemote spotifyAppRemote) { mSpotifyAppRemote = spotifyAppRemote; }
                    public void onFailure(Throwable throwable) {  }
                });
    } // End onConnect


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

}

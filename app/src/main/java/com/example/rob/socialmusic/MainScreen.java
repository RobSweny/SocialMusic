package com.example.rob.socialmusic;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.ImageButton;

import com.example.rob.socialmusic.accounts.AccountLogin;
import com.example.rob.socialmusic.accounts.PersonalAccount;
import com.example.rob.socialmusic.google_maps.GoogleMapsScreen;
import com.example.rob.socialmusic.spotify.SpotifyLogin;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainScreen extends AppCompatActivity {
    // Firebase authentication
    private static String CurrentUserName = "";

    // Initializing
    private ImageButton MainScreenSpotifyButton;
    private ImageButton MainScreenGoogleMapsButton;
    private ImageButton MainScreenPersonalAccount;
    private Button MainScreenLogOutButton;
    private Button toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        // Initializing Buttons
        MainScreenSpotifyButton = findViewById(R.id.MainScreenSpotifyButton);
        MainScreenGoogleMapsButton= findViewById(R.id.MainScreenGoogleMapsButton);
        MainScreenPersonalAccount= findViewById(R.id.MainScreenPersonalAccount);
        MainScreenLogOutButton= findViewById(R.id.MainScreenLogOutButton);
        toolbar = findViewById(R.id.toolbar);

        // Firebase getting username
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            CurrentUserName = user.getDisplayName();
            toolbar.setText("Welcome " + CurrentUserName);
        } else {
            toolbar.setText("Welcome");
        } // End if user != null

        // Launch to SpotifyLogin on button click
        MainScreenSpotifyButton.setOnClickListener(v -> {
            Intent i = new Intent(v.getContext(), SpotifyLogin.class);
            startActivity(i);
        });

        // Launch to GoogleMaps on button click
        MainScreenGoogleMapsButton.setOnClickListener(v -> {
            Intent i = new Intent(v.getContext(), GoogleMapsScreen.class);
            startActivity(i);
        });

        // Send user back to account login
        MainScreenLogOutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent i = new Intent(v.getContext(), AccountLogin.class);
            startActivity(i);
        });

        MainScreenPersonalAccount.setOnClickListener(v -> {
            Intent i = new Intent(v.getContext(), PersonalAccount.class);
            startActivity(i);
        });
    } //End onCreate

    // Prevent user from pressing on back button
    @Override
    public void onBackPressed() { } // End onBackPressed
}// End MainScreen


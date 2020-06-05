package com.example.rob.socialmusic.google_maps;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.rob.socialmusic.MainScreen;
import com.example.rob.socialmusic.R;

public class LocationServices extends Activity {
    private Boolean Location_settings_checked = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_services);

        Button EnableLocation = findViewById(com.example.rob.socialmusic.R.id.EnableLocationButton);
        TextView BackToMenu = findViewById(com.example.rob.socialmusic.R.id.BackToMenu);


        // Launch to SpotifyLogin on button click
        // Location settings are enabled
        EnableLocation.setOnClickListener(v -> {
            Location_settings_checked = true;
            Intent myIntent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            LocationServices.this.startActivity(myIntent);
        });

        // Launch to SpotifyLogin on button click
        BackToMenu.setOnClickListener(v -> {
            Intent i = new Intent(LocationServices.this, MainScreen.class);
            startActivity(i);
        });
    } // End onCreate


    @Override
    protected void onResume() {
        super.onResume();

        /*
            This section checks if the users location services is on
            As the user is sent to location settings the boolean "Location_settings_checked" is set to true
            once they return back to the app, this section will be checked.
            If the location services have been selected, it will bring them to the maps,
            if not, they will return to the location services activity
        */
        LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;
        gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (Location_settings_checked){
            if (!gps_enabled && !network_enabled){
                Intent i = new Intent(this, LocationServices.class);
                startActivity(i);
            } else {
                Intent i = new Intent(this, MapActivity.class);
                startActivity(i);
            }
        } // End if (Location_settings_checked)
    } // End onResume


    // If the user selects the back button, bring them back to the main menu
    @Override
    public void onBackPressed() {
        // Return to Main Screen when successful
        Intent i = new Intent(this, MainScreen.class);
        startActivity(i);
    } // End onBackPressed



}

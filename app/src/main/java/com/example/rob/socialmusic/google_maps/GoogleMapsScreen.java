
package com.example.rob.socialmusic.google_maps;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.rob.socialmusic.MainScreen;
import com.example.rob.socialmusic.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

public class GoogleMapsScreen extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int ERROR_DIALOG_REQUEST = 9001;
    private ProgressBar GoogleMapsprogressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_maps_screen);

        GoogleMapsprogressBar = findViewById(R.id.GoogleMapsprogressBar);
        showProgressDialog();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                isServicesOK();
            }
        }, 2000);
    } // End onCreate

    public boolean isServicesOK(){
        Log.d(TAG, "isServicesOK: checking google services version");

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(GoogleMapsScreen.this);
        if(available == ConnectionResult.SUCCESS){
            // Everything is working fine, user can make map requests
            Log.d(TAG, "isServicesOK: Google Play Services is working");

            Intent i=new Intent(GoogleMapsScreen.this, MapActivity.class);
            startActivity(i);
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)){
            // An error occurred but we can resolve it
            Log.d(TAG, "isServicesOK: an error occurred but we can fix it");

            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(GoogleMapsScreen.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        } else{
            Toast.makeText(this, "You can't make map requests", Toast.LENGTH_SHORT).show();
        } // End If (available == ConnectionResult.SUCCESS)
        return false;
    } // End isServicesOK

    public void showProgressDialog() {
        GoogleMapsprogressBar.setVisibility(View.VISIBLE);
    } // End showProgressDialog

    public void hideProgressDialog() {
        GoogleMapsprogressBar.setVisibility(View.INVISIBLE);
    } // End hideProgressDialog

    @Override
    public void onStop() {
        super.onStop();
        hideProgressDialog();
    } // End onStop

    @Override
    protected void onResume() {
        super.onResume();
        // Check if services are ok
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                isServicesOK();
            }
        }, 2000);
    } // End onResume

    // Prevent backspace to going back to checking Google maps
    @Override
    public void onBackPressed() {
        // Launch to MainScreen when successful
        Intent i = new Intent(this, MainScreen.class);
        startActivity(i);
    } // End onBackPressed
} // End GoogleMapsScreen



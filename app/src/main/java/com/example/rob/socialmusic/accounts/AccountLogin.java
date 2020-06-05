package com.example.rob.socialmusic.accounts;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.animation.CycleInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.example.rob.socialmusic.MainScreen;
import com.example.rob.socialmusic.R;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class AccountLogin extends AppCompatActivity {

    private FirebaseAuth mAuth;
    SignInButton button;
    GoogleApiClient mGoogleApiClient;
    FirebaseAuth.AuthStateListener mAuthListener;

    // Firebase authentication listener
    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
        mAuth.addAuthStateListener(mAuthListener);
        FirebaseUser currentUser = mAuth.getCurrentUser();
    } // End onStart

    // Request code for function "signIn"
    private final static int RC_SIGN_IN = 2;
    private static final String username = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_login);

        // Initializing FireBase authorization
        mAuth = FirebaseAuth.getInstance();

        // Initializing
        CheckBox ShowPasswordCheckbox = findViewById(R.id.ShowPasswordCheckbox);
        Button sign_in_AccountButton = findViewById(R.id.AccountLoginSignInAccountButton);
        Button create_account_AccountButton = findViewById(R.id.AccountLoginCreateAccountButton);
        SignInButton GoogleSignInButton = findViewById(R.id.GoogleSignInButton);
        EditText PasswordEditText = findViewById(R.id.PasswordEditText);
        EditText EmailEditText = findViewById(R.id.EmailEditText);

        // Shared Preferences for onBoarding
        PreferenceManager.getDefaultSharedPreferences(this);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
               .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        // If Google connection fails
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, connectionResult -> Toast.makeText(AccountLogin.this, "Issue signing into google api",Toast.LENGTH_SHORT).show())
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        // End onAuthStateChanged
        mAuthListener = firebaseAuth -> {
            // If Firebase retrieves a user, send user to main screen
            if (firebaseAuth.getCurrentUser() != null){
                // Launch to MainScreen when successful
                Intent i = new Intent(AccountLogin.this, MainScreen.class);
                startActivity(i);
            } // End if (firebaseAuth.getCurrentUser() != null)
        }; // End Firebase authentication listener

        // When the Google sign in button is pressed, sign the user in
        GoogleSignInButton.setOnClickListener(v -> signIn());

        /*
            On Button click sign in, check if both password and username editTextFields are empty
            then both separately, an animation is used to shake the editTextField when the field is empty.
            If both fields are filled in, check if the user already has an account, if not, prompt to create one.
        */
        sign_in_AccountButton.setOnClickListener(v -> {
            EditText PasswordEditText1 = findViewById(R.id.PasswordEditText);
            EditText EmailEditText1 = findViewById(R.id.EmailEditText);

            String password = PasswordEditText1.getText().toString();
            String email = EmailEditText1.getText().toString();

            if(password.length() <= 0 && email.length() <= 0){
                PasswordEditText1.startAnimation(shakeError());
                EmailEditText1.startAnimation(shakeError());
            } else if (password.length() <= 0) {
                PasswordEditText1.startAnimation(shakeError());
            } else if (email.length() <= 0) {
                EmailEditText1.startAnimation(shakeError());
            } else {
                // If UsernameEditText and PasswordEditText are both entered, check if details are in correct
                PasswordAuthentication();
            } // End onClick
        }); // End Sign in button

        // Start Account Creation Activity
        create_account_AccountButton.setOnClickListener(v -> {
            // Launch to MainScreen when successful
            Intent i = new Intent(v.getContext(), AccountCreation.class);
            startActivity(i);
        }); // End create account button

        // Add onCheckedListener on checkbox
        // End Checkbox Change
        ShowPasswordCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            EditText PasswordEditText12 = findViewById(R.id.PasswordEditText);

            // Alternative EditTextfield between password view and text view as checkbox is clicked
            if (PasswordEditText12.getInputType() == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                PasswordEditText12.setInputType( InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            } else {
                PasswordEditText12.setInputType( InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD );
            } // End if
        }); // End Checkbox Listener

        // Hide keyboard on focus off
        PasswordEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                hideKeyboard(v);
            } // End if
        }); // End Keyboard hider

        EmailEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                hideKeyboard(v);
            } // End if
        }); // End Keyboard hider
    } // End OnCreate

    // Google Sign up authentication
    private void PasswordAuthentication(){
        EditText PasswordEditText = findViewById(R.id.PasswordEditText);
        EditText EmailEditText = findViewById(R.id.EmailEditText);

        String password = PasswordEditText.getText().toString();
        String email = EmailEditText.getText().toString();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if(task.isSuccessful()) {
                        // Sign in success
                        Log.d("TAG", "signInWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();

                        // Launch to MainScreen when successful
                        Intent i = new Intent(AccountLogin.this, MainScreen.class);
                        startActivity(i);
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("TAG", "signInWithEmail:failure", task.getException());
                        Toast.makeText(AccountLogin.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    } // End if(task.isSuccessful())
                });
    }

    // Firebase sign up and Google authentication
    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    } // End Sign In

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
            } else {
                // Google Sign In failed
                Toast.makeText(AccountLogin.this, "Auth went wrong",Toast.LENGTH_SHORT).show();
            } // End if (result.isSuccess())
        } // End if (requestCode == RC_SIGN_IN)
    } // End onActivityResult

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        // End onComplete
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success
                        Log.d("TAG", "signInWithCredential:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("TAG", "signInWithCredential:failure", task.getException());
                        Toast.makeText(AccountLogin.this, "Authentication Failed",Toast.LENGTH_SHORT).show();
                    } // End If
                });
    } // End firebaseAuthWithGoogle

    // Hide keyboard when screen is pressed
    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    } // End Hide keyboard method

    // Creating Shake Animation for incorrect Username or Password
    public TranslateAnimation shakeError() {
        TranslateAnimation shake = new TranslateAnimation(0, 25, 0, 0);
        shake.setDuration(400);
        shake.setInterpolator(new CycleInterpolator(6));
        return shake;
    } // End Animation

    // Prevent user from pressing on back button
    @Override
    public void onBackPressed() {

    } // End onBackPressed

} // End Main Class (Account Login)

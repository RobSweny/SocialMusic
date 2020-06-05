package com.example.rob.socialmusic.accounts;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
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
import com.example.rob.socialmusic.R;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;

public class AccountCreation extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private CheckBox ShowPasswordCheckbox;
    private EditText EmailEditText;
    private EditText PasswordEditText;
    private Button CreateAccountButton;
    private Button BackCreateAccountButton;

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
    } // End onStart


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_creation);

        mAuth = FirebaseAuth.getInstance();

        // Initializing
        ShowPasswordCheckbox = findViewById(R.id.ShowPasswordCheckbox);
        EmailEditText = findViewById(R.id.EmailEditText);
        PasswordEditText = findViewById(R.id.PasswordEditText);
        CreateAccountButton = findViewById(R.id.CreateAccountButton);
        BackCreateAccountButton = findViewById(R.id.BackCreateAccountButton);


        // Start Account Login  Activity
        BackCreateAccountButton.setOnClickListener(v -> {
            // Launch to Login  Activity when successful
            Intent i = new Intent(v.getContext(), AccountLogin.class);
            startActivity(i);
        }); // End create account button

        /*
            On Button click sign in, check if both password and username editTextFields are empty
            then both separately, an animation is used to shake the editTextField when the field is empty.
            If both fields are filled in, check if the user already has an account, if not, prompt to create one.
        */
        CreateAccountButton.setOnClickListener(v -> {
            String password = PasswordEditText.getText().toString();
            String email = EmailEditText.getText().toString();

            if(password.length() <= 0 && email.length() <= 0){
                PasswordEditText.startAnimation(shakeError());
                EmailEditText.startAnimation(shakeError());
            } else if (password.length() <= 0) {
                PasswordEditText.startAnimation(shakeError());
            } else if (email.length() <= 0) {
                EmailEditText.startAnimation(shakeError());
            } else {
                accountCreation();
            } // If
        }); // End Sign in button


        // Add onCheckedListener on checkbox
        // End Checkbox Change
        ShowPasswordCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Alternative EditTextfield between password view and text view as checkbox is clicked
            if (PasswordEditText.getInputType() == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                PasswordEditText.setInputType( InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            } else {
                PasswordEditText.setInputType( InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD );
            } // End if
        }); // End Checkbox Listener

        // Hide keyboard on focus
        // #off
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

    } // End onCreate

    private void accountCreation(){
        String password = PasswordEditText.getText().toString();
        String email = EmailEditText.getText().toString();

        // End onComplete
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    Log.d("TAG", "createUserWithEmail:success");
                    // if task is successful  then AuthStateListener  will get notified you can get user details there.
                    // if task is not successful show error
                    if (!task.isSuccessful()) {
                         try {
                            throw task.getException();
                        } catch (FirebaseAuthUserCollisionException e) {
                             Toast.makeText(AccountCreation.this, "Another user exists with this account", Toast.LENGTH_SHORT).show();
                        } catch (FirebaseNetworkException e) {
                             Toast.makeText(AccountCreation.this, "Network issues! Try again later", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                             Toast.makeText(AccountCreation.this, "Authentication failed! Please ensure a valid email address is entered", Toast.LENGTH_SHORT).show();
                        } // End try catch
                    } else {
                        // Sign in success, update UI with the signed-in user's information
                        Toast.makeText(AccountCreation.this, "Authentication failed. Please ensure a valid email was entered",
                                Toast.LENGTH_LONG).show();
                        FirebaseUser user = mAuth.getCurrentUser();
                    } // End if
                });
    } // End accountCreation method

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

} // End AccountCreation


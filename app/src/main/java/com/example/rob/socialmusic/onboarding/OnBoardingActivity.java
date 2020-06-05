package com.example.rob.socialmusic.onboarding;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.example.rob.socialmusic.R;
import com.example.rob.socialmusic.accounts.AccountLogin;
import java.util.ArrayList;

public class OnBoardingActivity extends AppCompatActivity {

    private static final String TAG = "OnBoardingActivity";
    public static final String MyPREFERENCES = "myprefs";
    private SharedPreferences mPreferences;
    private LinearLayout pager_indicator;
    private int dotsCount;
    private ImageView[] dots;
    private ViewPager onboard_pager;
    private OnBoard_Adapter mAdapter;
    private Button btn_get_started;
    int previous_pos = 0;

    ArrayList<OnBoardItem> onBoardItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_on_boarding);
        btn_get_started = findViewById(R.id.btn_get_started);
        onboard_pager = findViewById(R.id.pager_introduction);
        pager_indicator = findViewById(R.id.viewPagerCountDots);

        // Retrieve user preferences
        mPreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);

        /* Remove this comment to adjust onboarding */
        //SharedPreferences.Editor editor = mPreferences.edit();
        //Change boolean to True
        //editor.putBoolean("onboarding_complete", false);
        //editor.apply();

        // User has completed the onBoarding experience, send them to the Account Login Screen
        if(mPreferences.getBoolean("onboarding_complete", true)){
            Log.v(TAG, "onboarding has been completed, loading Account Login");

            // Progress to user login screen
            Intent i = new Intent(this  , AccountLogin.class);
            startActivity(i);
        } else {
            // The user hasn't seen the Onboarding yet.
            Log.v(TAG, "onboarding has not been completed");
            // Grab the Titles, Images and descriptions for each page
            loadData();
            // Set Titles, Images and descriptions for each page
            mAdapter = new OnBoard_Adapter(this,onBoardItems);
            // Set the viewpager
            onboard_pager.setAdapter(mAdapter);
            // Set the current page to the first
            onboard_pager.setCurrentItem(0);
            // Check for Page changes
            onboard_pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                } // End onPageScrolled

                @Override
                public void onPageSelected(int position) {
                    // Change the current position and dot images
                    for (int i = 0; i < dotsCount; i++) {
                        dots[i].setImageDrawable(ContextCompat.getDrawable(OnBoardingActivity.this, R.drawable.non_selected_item_dot));
                    } // End For
                    dots[position].setImageDrawable(ContextCompat.getDrawable(OnBoardingActivity.this, R.drawable.selected_item_dot));
                    // Current page + 1;
                    int pos = position + 1;
                    // If on last page, prompt the user to proceed and exit onboarding
                    if(pos == dotsCount && previous_pos == (dotsCount - 1))
                        show_animation();
                    else if(pos == (dotsCount - 1) && previous_pos == dotsCount)
                        hide_animation();

                    previous_pos = pos;
                } // End onPageSelected
                @Override
                public void onPageScrollStateChanged(int state) { }
            });

            btn_get_started.setOnClickListener(v -> {
                // User has completed the onBoarding experience, let them pass
                SharedPreferences.Editor editor1 = mPreferences.edit();
                // Change user preferences boolean to True
                editor1.putBoolean("onboarding_complete", true);
                editor1.apply();

                // Progress to user login screen
                Intent i = new Intent(v.getContext(), AccountLogin.class);
                startActivity(i);
            });
            setUiPageViewController();
        } // End Else
    } // End onCreate

    // Load data into the viewpager
    public void loadData() {
        int[] header = {R.string.onBoardingHeader1, R.string.onBoardingHeader2, R.string.onBoardingHeader3, R.string.onBoardingHeader4};
        int[] desc = {R.string.onBoardingDescription1, R.string.onBoardingDescription2, R.string.onBoardingDescription3, R.string.onBoardingDescription4};
        int[] imageId = {R.drawable.onboarding_spotify, R.drawable.onboarding_googlemaps, R.drawable.onboarding_googlemapslocation, R.drawable.onboarding_privacy};

        for(int i = 0; i < imageId.length; i++) {
            OnBoardItem item=new OnBoardItem();
            item.setImageID(imageId[i]);
            item.setTitle(getResources().getString(header[i]));
            item.setDescription(getResources().getString(desc[i]));
            onBoardItems.add(item);
        } // End for
    } // End loadData

     // Button btn_get_started raised animation
     public void show_animation() {
        // On the last page of onBoarding show the animation to finish (btn_get_started.setVisibility(View.VISIBLE))
        Animation show = AnimationUtils.loadAnimation(this, R.anim.slide_up_anim);
        btn_get_started.startAnimation(show);
        show.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) { btn_get_started.setVisibility(View.VISIBLE); }
            @Override
            public void onAnimationRepeat(Animation animation) { }
            @Override
            public void onAnimationEnd(Animation animation) { btn_get_started.clearAnimation();  }
        });
    } // End show_animation

    // If the user moves from final page, hide the proceed button
    public void hide_animation() {
        Animation hide = AnimationUtils.loadAnimation(this, R.anim.slide_down_anim);
        btn_get_started.startAnimation(hide);
        hide.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) { }
            @Override
            public void onAnimationRepeat(Animation animation) { }
            @Override
            public void onAnimationEnd(Animation animation) {
                btn_get_started.clearAnimation();
                btn_get_started.setVisibility(View.GONE);
            } // End onAnimationEnd
        });
    } // End hide_animation

    private void setUiPageViewController() {
        // Retrieve page count
        dotsCount = mAdapter.getCount();
        dots = new ImageView[dotsCount];

        // For each page, have a new ImageView
        for (int i = 0; i < dotsCount; i++) {
            dots[i] = new ImageView(this);
            dots[i].setImageDrawable(ContextCompat.getDrawable(OnBoardingActivity.this, R.drawable.non_selected_item_dot));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(6, 0, 6, 0);
            pager_indicator.addView(dots[i], params);
        } // End for loop
        dots[0].setImageDrawable(ContextCompat.getDrawable(OnBoardingActivity.this, R.drawable.selected_item_dot));
    } // End setUiPageViewController
}

package com.example.appcontroller;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private Switch fanSwitch, outletSwitch, lightSwitch, allSwitch;
    private NumberPicker hourPicker, minutePicker, secondPicker;
    private Button startTimerButton, logoutButton, resetTimerButton;
    private TextView countdownDisplay;
    private DatabaseReference fanStatusRef, outletStatusRef, lightStatusRef, allStatusRef, timerRef;
    private CountDownTimer countDownTimer;
    private MediaPlayer mediaPlayer;

    // Flag to check if alert has been shown
    private boolean isTimeAlertShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Database references
        fanStatusRef = FirebaseDatabase.getInstance().getReference("Controllers/ESP_1/componentButtonList/fanStatus/indefiniteOn");
        outletStatusRef = FirebaseDatabase.getInstance().getReference("Controllers/ESP_1/componentButtonList/outletStatus/indefiniteOn");
        lightStatusRef = FirebaseDatabase.getInstance().getReference("Controllers/ESP_1/componentButtonList/chargerStatus/indefiniteOn");
        allStatusRef = FirebaseDatabase.getInstance().getReference("Controllers/ESP_1/componentButtonList/allStatus");
        timerRef = FirebaseDatabase.getInstance().getReference("Controllers/ESP_1/lightStatus/reference/time");

        // Initialize UI components
        fanSwitch = findViewById(R.id.fanSwitch);
        outletSwitch = findViewById(R.id.outletSwitch);
        lightSwitch = findViewById(R.id.lightSwitch);
        allSwitch = findViewById(R.id.allSwitch);
        hourPicker = findViewById(R.id.hourPicker);
        minutePicker = findViewById(R.id.minutePicker);
        secondPicker = findViewById(R.id.secondPicker);
        startTimerButton = findViewById(R.id.startTimerButton);
        countdownDisplay = findViewById(R.id.countdownDisplay);
        logoutButton = findViewById(R.id.logoutButton);
        resetTimerButton = findViewById(R.id.resetTimerButton); // Initialize reset button

        // Configure NumberPicker ranges
        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(24);
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        secondPicker.setMinValue(0);
        secondPicker.setMaxValue(59);

        // Setup individual listeners for each switch
        setupSwitchListeners();

        // Start timer button listener
        startTimerButton.setOnClickListener(view -> startTimerFromInput());

        // Logout button listener
        logoutButton.setOnClickListener(view -> logout());

        // Reset timer button listener
        resetTimerButton.setOnClickListener(view -> resetTimer());
    }

    // Method to setup listeners for all switches
    private void setupSwitchListeners() {
        fanSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            fanStatusRef.setValue(isChecked);
            handleSwitchState("Fan", isChecked);
        });

        outletSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            outletStatusRef.setValue(isChecked);
            handleSwitchState("Outlet (Charger)", isChecked);
        });

        lightSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            lightStatusRef.setValue(isChecked);
            handleSwitchState("Light", isChecked);
        });

        allSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            allStatusRef.setValue(isChecked);

            // Set all switches to the same state when 'allSwitch' is toggled
            fanSwitch.setChecked(isChecked);
            outletSwitch.setChecked(isChecked);
            lightSwitch.setChecked(isChecked);

            // Disable all switches if 'allSwitch' is ON
            setSwitchesEnabled(!isChecked);

            // Update Firebase values accordingly
            fanStatusRef.setValue(isChecked);
            outletStatusRef.setValue(isChecked);
            lightStatusRef.setValue(isChecked);

            handleSwitchState("All Devices", isChecked);
        });
    }

    // Method to enable/disable all switches
    private void setSwitchesEnabled(boolean enabled) {
        fanSwitch.setEnabled(enabled);
        outletSwitch.setEnabled(enabled);
        lightSwitch.setEnabled(enabled);
    }

    // Method to display toast message based on switch state
    private void handleSwitchState(String deviceName, boolean isChecked) {
        String status = isChecked ? "ON" : "OFF";
        Toast.makeText(this, deviceName + " is " + status, Toast.LENGTH_SHORT).show();
    }

    // Method to start timer based on NumberPicker values
    private void startTimerFromInput() {
        int hours = hourPicker.getValue();
        int minutes = minutePicker.getValue();
        int seconds = secondPicker.getValue();

        // Convert hours, minutes, and seconds to milliseconds
        int duration = (hours * 3600 + minutes * 60 + seconds) * 1000;

        if (duration > 0) {
            // Save the timer duration to Firebase
            saveTimerDataToFirebase(hours, minutes, seconds);

            // Start the timer
            startTimer(duration);
        } else {
            Toast.makeText(this, "Please select a valid time", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to save the timer data to Firebase
    private void saveTimerDataToFirebase(int hours, int minutes, int seconds) {
        // Create a map to store the timer data
        Map<String, Integer> timerData = new HashMap<>();
        timerData.put("hours", hours);
        timerData.put("minutes", minutes);
        timerData.put("seconds", seconds);
        timerRef.setValue(timerData);
    }

    // Method to start a timer with countdown display
    private void startTimer(int duration) {
        // Turn on the devices if they're off
        if (!fanSwitch.isChecked()) {
            fanSwitch.setChecked(true);
        }
        if (!outletSwitch.isChecked()) {
            outletSwitch.setChecked(true);
        }
        if (!lightSwitch.isChecked()) {
            lightSwitch.setChecked(true);
        }

        // Initialize and start the countdown timer
        countDownTimer = new CountDownTimer(duration, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Calculate hours, minutes, and seconds remaining
                int hours = (int) (millisUntilFinished / (1000 * 60 * 60));
                int minutes = (int) ((millisUntilFinished % (1000 * 60 * 60)) / (1000 * 60));
                int seconds = (int) ((millisUntilFinished % (1000 * 60)) / 1000);

                // Display the remaining time
                String timeRemaining = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                countdownDisplay.setText(timeRemaining);

                // Check if the remaining time is below 5 minutes and the alert hasn't been shown yet
                if (minutes < 5 && !isTimeAlertShown) {
                    // Show a pop-up window and play sound
                    showTimeAlert();
                    playAlertSound();
                    // Set the flag to true so the alert won't show again
                    isTimeAlertShown = true;
                }
            }

            @Override
            public void onFinish() {
                // Turn off the devices and update Firebase
                fanSwitch.setChecked(false);
                outletSwitch.setChecked(false);
                lightSwitch.setChecked(false);
                fanStatusRef.setValue(false);
                outletStatusRef.setValue(false);
                lightStatusRef.setValue(false);

                // Reset the countdown display
                countdownDisplay.setText("00:00:00");

                // Stop the alert sound if it's still playing
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer = null;
                }

                // Reset the alert flag when the timer finishes
                isTimeAlertShown = false;

                // Show toast message
                Toast.makeText(LoginActivity.this, "Timer ended, all devices turned OFF", Toast.LENGTH_SHORT).show();
            }
        }.start();
    }

    // Method to reset the timer and stop the countdown
    private void resetTimer() {
        // Stop the countdown timer if it's running
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }

        // Reset the countdown display to 00:00:00
        countdownDisplay.setText("00:00:00");

        // Turn off the devices and update Firebase
        fanSwitch.setChecked(false);
        outletSwitch.setChecked(false);
        lightSwitch.setChecked(false);
        fanStatusRef.setValue(false);
        outletStatusRef.setValue(false);
        lightStatusRef.setValue(false);

        // Stop the alert sound if it's still playing
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        // Reset the alert flag
        isTimeAlertShown = false;

        // Show a toast message
        Toast.makeText(this, "Timer reset, all devices turned OFF", Toast.LENGTH_SHORT).show();
    }

    // Method to show a time alert pop-up
    private void showTimeAlert() {
        new AlertDialog.Builder(this)
                .setTitle("Time Alert")
                .setMessage("Time is running out! Please take action.")
                .setPositiveButton("OK", null)
                .show();
    }

    // Method to play an alert sound when time is running out
    private void playAlertSound() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.alertnotif); // Place your alert sound in res/raw
        }
        mediaPlayer.start();
    }

    // Method to log out the user
    private void logout() {
        // Here you can implement your logout logic, such as clearing session data
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }
}

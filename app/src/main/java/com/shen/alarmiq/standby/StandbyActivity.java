package com.shen.alarmiq.standby;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

import com.shen.alarmiq.AlarmiqApp;
import com.shen.alarmiq.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class StandbyActivity extends AppCompatActivity {

    private FrameLayout container;
    private TextView txtTime;
    private TextView txtDate;
    private ImageView imgBackground;
    private View overlay;

    private int currentStyle = 0; // 0: Digital, 1: Analog (Placeholder), 2: Minimal
    private GestureDetectorCompat gestureDetector;
    private android.view.OrientationEventListener orientationEventListener;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    private final Runnable updateTimeRunnable = new Runnable() {
        @Override
        public void run() {
            updateTime();
            handler.postDelayed(this, 1000);
        }
    };

    private final Runnable shiftUIRunnable = new Runnable() {
        @Override
        public void run() {
            applyBurnInProtection();
            handler.postDelayed(this, 60000); // Shift every minute
        }
    };

    private final BroadcastReceiver powerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
                finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        boolean isPreview = getIntent().getBooleanExtra("is_preview", false);
        boolean charging = isCharging();

        android.util.Log.d("StandbyActivity", "onCreate: isPreview=" + isPreview + ", isCharging=" + charging);

        // Initial check: if not charging, and launched from background, maybe finish?
        // Skip for preview mode.
        if (!isPreview && !charging) {
            android.util.Log.d("StandbyActivity", "Finishing: not charging and not preview");
            finish();
            return;
        }

        // Ensure landscape and always on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_standby);

        container = findViewById(R.id.standbyContainer);
        txtTime = findViewById(R.id.txtStandbyTime);
        txtDate = findViewById(R.id.txtStandbyDate);
        imgBackground = findViewById(R.id.imgStandbyBackground);
        overlay = findViewById(R.id.standbyOverlay);

        gestureDetector = new GestureDetectorCompat(this, new StandbyGestureListener());

        loadPreferences();
        hideSystemUI();
        
        handler.post(updateTimeRunnable);
        handler.post(shiftUIRunnable);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        registerReceiver(powerReceiver, filter);

        if (!isPreview) {
            setupOrientationListener();
        }
    }

    private void setupOrientationListener() {
        orientationEventListener = new android.view.OrientationEventListener(this, 
                android.hardware.SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation == ORIENTATION_UNKNOWN) return;

                // Check if physical orientation is portrait (approx 0 or 180 degrees)
                boolean isPortrait = (orientation >= 0 && orientation <= 30) || 
                                    (orientation >= 150 && orientation <= 210) ||
                                    (orientation >= 330 && orientation <= 360);

                if (isPortrait) {
                    android.util.Log.d("StandbyActivity", "Physical portrait detected, finishing Standby");
                    finish();
                }
            }
        };
        orientationEventListener.enable();
    }

    private boolean isCharging() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);
        if (batteryStatus == null) return false;
        
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL;
                            
        int plugged = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        boolean isPlugged = plugged == BatteryManager.BATTERY_PLUGGED_AC ||
                            plugged == BatteryManager.BATTERY_PLUGGED_USB ||
                            plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS;
                            
        return isCharging || isPlugged;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    private class StandbyGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (Math.abs(velocityX) > Math.abs(velocityY)) {
                if (velocityX > 0) {
                    // Swipe Right
                    switchStyle(-1);
                } else {
                    // Swipe Left
                    switchStyle(1);
                }
                return true;
            }
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            // Open customization menu in MainActivity
            Intent intent = new Intent(StandbyActivity.this, com.shen.alarmiq.MainActivity.class);
            intent.putExtra("show_standby", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }

    private void switchStyle(int direction) {
        currentStyle = (currentStyle + direction + 3) % 3;
        applyStyle();
    }

    private void applyStyle() {
        switch (currentStyle) {
            case 0: // Digital
                txtTime.setTextSize(140);
                txtDate.setVisibility(View.VISIBLE);
                break;
            case 1: // Minimal
                txtTime.setTextSize(100);
                txtDate.setVisibility(View.GONE);
                break;
            case 2: // Bold
                txtTime.setTextSize(180);
                txtDate.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void loadPreferences() {
        SharedPreferences prefs = getSharedPreferences(AlarmiqApp.PREFS_SETTINGS, MODE_PRIVATE);
        boolean burnInProtection = prefs.getBoolean("standby_burn_in_enabled", true);
        if (!burnInProtection) {
            handler.removeCallbacks(shiftUIRunnable);
        }
        
        String backgroundUriString = prefs.getString("standby_background_uri", null);
        if (backgroundUriString != null) {
            try {
                Uri uri = Uri.parse(backgroundUriString);
                // Important: Try to verify if we still have access
                imgBackground.setImageURI(uri);
                imgBackground.setVisibility(View.VISIBLE);
                
                // AMOLED protection: desaturate and dim
                ColorMatrix matrix = new ColorMatrix();
                matrix.setSaturation(0.2f); // Desaturate
                imgBackground.setColorFilter(new ColorMatrixColorFilter(matrix));
                overlay.setAlpha(0.8f); // Heavy dim
            } catch (SecurityException | NullPointerException e) {
                // Denied access to the URI, fallback to black
                imgBackground.setVisibility(View.GONE);
                overlay.setAlpha(0.0f);
            }
        } else {
            imgBackground.setVisibility(View.GONE);
            overlay.setAlpha(0.0f);
        }
        
        container.setBackgroundColor(Color.BLACK);
    }

    private void updateTime() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d", Locale.getDefault());
        txtTime.setText(timeFormat.format(new Date()));
        txtDate.setText(dateFormat.format(new Date()));
    }

    private void applyBurnInProtection() {
        // Pixel shifting: move the container slightly
        int dx = random.nextInt(11) - 5; // -5 to +5 pixels
        int dy = random.nextInt(11) - 5;
        
        txtTime.setTranslationX(dx);
        txtTime.setTranslationY(dy);
        txtDate.setTranslationX(dx);
        txtDate.setTranslationY(dy);
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateTimeRunnable);
        handler.removeCallbacks(shiftUIRunnable);
        try {
            unregisterReceiver(powerReceiver);
        } catch (Exception ignored) {}
        if (orientationEventListener != null) {
            orientationEventListener.disable();
        }
    }
}

package com.shen.alarmiq.standby;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.shen.alarmiq.AlarmiqApp;
import com.shen.alarmiq.MainActivity;
import com.shen.alarmiq.R;

import java.util.Random;

public class StandbyActivity extends AppCompatActivity {

    private ViewPager2 pager;
    private View dimOverlay;
    private ImageView imgBackground;
    private View hintLeft, hintRight;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    private boolean isDimmed = false;
    private long lastInteractionTime = System.currentTimeMillis();
    private static final long DIM_DELAY_MS = 10000;

    // DVD Bouncing State
    private float dvdX, dvdY;
    private float dvdDX = 2.5f, dvdDY = 2.5f;
    private View dvdView;

    private final Runnable updateTickRunnable = new Runnable() {
        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void run() {
            if (pager.getAdapter() != null) {
                pager.getAdapter().notifyDataSetChanged();
            }
            updateDVDBounce();
            checkDimming();
            handler.postDelayed(this, 30); // ~33fps for smooth bounce
        }
    };

    private final Runnable hideHintsRunnable = () -> {
        if (hintLeft != null && hintRight != null) {
            hintLeft.animate().alpha(0).setDuration(500).start();
            hintRight.animate().alpha(0).setDuration(500).start();
        }
    };

    private final BroadcastReceiver powerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_POWER_DISCONNECTED.equals(intent.getAction())) {
                finish();
            }
        }
    };

    private android.view.OrientationEventListener orientationEventListener;

    private final GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public void onLongPress(MotionEvent e) {
            if (isDimmed) return;
            Intent intent = new Intent(StandbyActivity.this, MainActivity.class);
            intent.putExtra("show_standby", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            resetInteraction();
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            resetInteraction();
            return false;
        }
    };

    private androidx.core.view.GestureDetectorCompat detector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        boolean isPreview = getIntent().getBooleanExtra("is_preview", false);
        if (!isPreview && !isCharging()) {
            finish();
            return;
        }

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

        pager = findViewById(R.id.standbyPager);
        dimOverlay = findViewById(R.id.dimOverlay);
        imgBackground = findViewById(R.id.imgStandbyBackground);
        hintLeft = findViewById(R.id.hintLeft);
        hintRight = findViewById(R.id.hintRight);

        detector = new androidx.core.view.GestureDetectorCompat(this, gestureListener);

        pager.setAdapter(new StandbyPagerAdapter());
        pager.setOffscreenPageLimit(4);

        // Random starting face
        pager.setCurrentItem(random.nextInt(5), false);

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                resetInteraction();
                dvdView = null; // Clear view to force re-find
            }
        });

        loadPreferences();
        hideSystemUI();
        resetInteraction();

        // Arrow Navigation
        hintLeft.setOnClickListener(v -> {
            resetInteraction();
            int current = pager.getCurrentItem();
            pager.setCurrentItem(current > 0 ? current - 1 : 4, true);
        });

        hintRight.setOnClickListener(v -> {
            resetInteraction();
            int current = pager.getCurrentItem();
            pager.setCurrentItem(current < 4 ? current + 1 : 0, true);
        });

        handler.post(updateTickRunnable);

        IntentFilter filter = new IntentFilter(Intent.ACTION_POWER_DISCONNECTED);
        registerReceiver(powerReceiver, filter);

        if (!isPreview) {
            setupOrientationListener();
        }
    }

    private void setupOrientationListener() {
        orientationEventListener = new android.view.OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation == ORIENTATION_UNKNOWN) return;
                boolean isPortrait = (orientation >= 0 && orientation <= 30) || 
                                    (orientation >= 150 && orientation <= 210) ||
                                    (orientation >= 330 && orientation <= 360);
                if (isPortrait) finish();
            }
        };
        orientationEventListener.enable();
    }

    private void checkDimming() {
        long now = System.currentTimeMillis();
        if (!isDimmed && (now - lastInteractionTime > DIM_DELAY_MS)) {
            enterDimMode();
        }
    }

    private void enterDimMode() {
        isDimmed = true;
        dimOverlay.setVisibility(View.VISIBLE);
        dimOverlay.animate().alpha(0.85f).setDuration(2000).start();
        hideSystemUI();
    }

    private void resetInteraction() {
        lastInteractionTime = System.currentTimeMillis();
        if (isDimmed) {
            isDimmed = false;
            dimOverlay.animate().alpha(0).setDuration(300).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    dimOverlay.setVisibility(View.GONE);
                }
            }).start();
        }
        
        // Show hints temporarily
        if (hintLeft != null && hintRight != null) {
            hintLeft.setAlpha(0.3f);
            hintRight.setAlpha(0.3f);
            handler.removeCallbacks(hideHintsRunnable);
            handler.postDelayed(hideHintsRunnable, 3000);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (detector != null) detector.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    private void updateDVDBounce() {
        if (pager.getCurrentItem() != StandbyPagerAdapter.TYPE_DVD) return;

        if (dvdView == null) {
            View rv = pager.getChildAt(0);
            if (rv instanceof RecyclerView) {
                RecyclerView recyclerView = (RecyclerView) rv;
                for (int i = 0; i < recyclerView.getChildCount(); i++) {
                    View child = recyclerView.getChildAt(i);
                    View found = child.findViewById(R.id.dvdContainer);
                    if (found != null) {
                        dvdView = found;
                        // Initial random position
                        dvdX = random.nextInt(Math.max(1, pager.getWidth() - 300));
                        dvdY = random.nextInt(Math.max(1, pager.getHeight() - 200));
                        break;
                    }
                }
            }
        }

        if (dvdView != null && dvdView.getWidth() > 0) {
            int pW = pager.getWidth();
            int pH = pager.getHeight();
            int vW = dvdView.getWidth();
            int vH = dvdView.getHeight();

            if (pW == 0 || pH == 0) return;

            dvdX += dvdDX;
            dvdY += dvdDY;

            if (dvdX <= 0) {
                dvdDX = Math.abs(dvdDX);
                dvdX = 0;
                changeDVDColor();
            } else if (dvdX + vW >= pW) {
                dvdDX = -Math.abs(dvdDX);
                dvdX = pW - vW;
                changeDVDColor();
            }

            if (dvdY <= 0) {
                dvdDY = Math.abs(dvdDY);
                dvdY = 0;
                changeDVDColor();
            } else if (dvdY + vH >= pH) {
                dvdDY = -Math.abs(dvdDY);
                dvdY = pH - vH;
                changeDVDColor();
            }

            dvdView.setTranslationX(dvdX);
            dvdView.setTranslationY(dvdY);
        }
    }

    private void changeDVDColor() {
        if (dvdView instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) dvdView;
            TextView tv = group.findViewById(R.id.txtTime);
            if (tv != null) {
                int color = Color.rgb(random.nextInt(156)+100, random.nextInt(156)+100, random.nextInt(156)+100);
                tv.setTextColor(color);
                if (group.getChildCount() > 1) {
                    View label = group.getChildAt(1);
                    if (label instanceof TextView) ((TextView) label).setTextColor(color);
                }
            }
        }
    }

    private boolean isCharging() {
        Intent batteryStatus = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatus == null) return false;
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        int plugged = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL || plugged > 0;
    }

    private void loadPreferences() {
        SharedPreferences prefs = getSharedPreferences(AlarmiqApp.PREFS_SETTINGS, MODE_PRIVATE);
        String bgUri = prefs.getString("standby_background_uri", null);
        if (bgUri != null) {
            try {
                imgBackground.setImageURI(Uri.parse(bgUri));
                imgBackground.setVisibility(View.VISIBLE);
                ColorMatrix cm = new ColorMatrix();
                cm.setSaturation(0.2f);
                imgBackground.setColorFilter(new ColorMatrixColorFilter(cm));
            } catch (Exception ignored) {}
        }
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        try { unregisterReceiver(powerReceiver); } catch (Exception ignored) {}
        if (orientationEventListener != null) orientationEventListener.disable();
    }
}

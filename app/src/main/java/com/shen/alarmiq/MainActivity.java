package com.shen.alarmiq;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.shen.alarmiq.alarm.AlarmFragment;
import com.shen.alarmiq.standby.StandbyActivity;
import com.shen.alarmiq.standby.StandbySettingsFragment;
import com.shen.alarmiq.stopwatch.StopwatchFragment;
import com.shen.alarmiq.timer.TimerFragment;
import com.shen.alarmiq.world.WorldClockFragment;

public class MainActivity extends AppCompatActivity {

    private final ActivityResultLauncher<String> notificationsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {});

    private boolean standbyHandled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AlarmiqApp.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            standbyHandled = savedInstanceState.getBoolean("standby_handled", false);
        }

        View root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, 0, bars.right, 0);
            return insets;
        });

        setupThemeToggle();
        setupAboutDialog();
        setupStandbySettingsButton();

        ViewPager2 pager = findViewById(R.id.pager);
        if (pager != null) {
            setupPager(pager);
        } else {
            setupMultiPane(savedInstanceState);
        }

        // Surgical intent handling: Only process show_standby on fresh launch or new intent
        handleStandbyIntent(getIntent(), savedInstanceState == null);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationsLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void handleStandbyIntent(android.content.Intent intent, boolean isFresh) {
        if (intent != null && intent.getBooleanExtra("show_standby", false)) {
            if (isFresh || !standbyHandled) {
                standbyHandled = true;
                intent.removeExtra("show_standby");

                ViewPager2 pager = findViewById(R.id.pager);
                if (pager != null) {
                    pager.setCurrentItem(4, false);
                } else {
                    startActivity(new android.content.Intent(this, com.shen.alarmiq.standby.StandbySettingsActivity.class));
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("standby_handled", standbyHandled);
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        standbyHandled = false; // Reset to allow processing the new intent
        handleStandbyIntent(intent, true);
    }

    private void setupAboutDialog() {
        View btnAbout = findViewById(R.id.btnAbout);
        if (btnAbout == null) return;

        btnAbout.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.about_title)
                    .setMessage(R.string.about_message)
                    .setPositiveButton(R.string.donate, (dialog, which) -> {
                        openUrl("https://buymeacoffee.com/lBUDKgM");
                    })
                    .setNeutralButton(R.string.source_code, (dialog, which) -> {
                        openUrl("https://github.com/ProjectSakura/Alarmiq");
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });
    }

    private void openUrl(String url) {
        try {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url));
            startActivity(intent);
        } catch (Exception ignored) {}
    }

    private void setupStandbySettingsButton() {
        View btnStandby = findViewById(R.id.btnStandbySettings);
        if (btnStandby == null) return;

        btnStandby.setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, com.shen.alarmiq.standby.StandbySettingsActivity.class));
        });
    }

    private void setupThemeToggle() {
        ImageButton btnToggle = findViewById(R.id.btnThemeToggle);
        if (btnToggle == null) return;

        // Determine if we are currently in dark mode (either explicitly or via system)
        int currentMode = AppCompatDelegate.getDefaultNightMode();
        boolean isDark;
        if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            isDark = true;
        } else if (currentMode == AppCompatDelegate.MODE_NIGHT_NO) {
            isDark = false;
        } else {
            // MODE_NIGHT_FOLLOW_SYSTEM or MODE_NIGHT_UNSPECIFIED
            isDark = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                    == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        }

        btnToggle.setImageResource(isDark ? R.drawable.ic_sun : R.drawable.ic_moon);

        btnToggle.setOnClickListener(v -> {
            // Toggle explicitly between YES and NO
            int newMode = isDark ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES;
            
            // Save preference FIRST
            getSharedPreferences(AlarmiqApp.PREFS_SETTINGS, MODE_PRIVATE)
                    .edit()
                    .putInt(AlarmiqApp.KEY_THEME, newMode)
                    .apply();
            
            // Then apply it. Activity will be recreated automatically.
            AppCompatDelegate.setDefaultNightMode(newMode);
        });
    }

    private void setupPager(ViewPager2 pager) {
        TabLayout tabs = findViewById(R.id.tabs);
        pager.setAdapter(new MainPagerAdapter(this));
        pager.setOffscreenPageLimit(4);
        new TabLayoutMediator(tabs, pager, (tab, position) -> {
            switch (position) {
                case 0: tab.setText(R.string.tab_alarms); break;
                case 1: tab.setText(R.string.tab_stopwatch); break;
                case 2: tab.setText(R.string.tab_timer); break;
                case 3: tab.setText(R.string.tab_world); break;
                case 4: tab.setText(R.string.standby_mode); break;
            }
        }).attach();
    }

    private void setupMultiPane(Bundle savedInstanceState) {
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        
        tx.replace(R.id.paneAlarm, findOrCreate("pane-alarm", AlarmFragment.class), "pane-alarm");
        tx.replace(R.id.paneStopwatch, findOrCreate("pane-stopwatch", StopwatchFragment.class), "pane-stopwatch");
        tx.replace(R.id.paneTimer, findOrCreate("pane-timer", TimerFragment.class), "pane-timer");
        tx.replace(R.id.paneWorld, findOrCreate("pane-world", WorldClockFragment.class), "pane-world");
        
        tx.commit();
    }

    private androidx.fragment.app.Fragment findOrCreate(String tag, Class<? extends androidx.fragment.app.Fragment> clazz) {
        androidx.fragment.app.Fragment f = getSupportFragmentManager().findFragmentByTag(tag);
        if (f == null) {
            try {
                f = clazz.newInstance();
            } catch (Exception ignored) {}
        }
        return f;
    }
}

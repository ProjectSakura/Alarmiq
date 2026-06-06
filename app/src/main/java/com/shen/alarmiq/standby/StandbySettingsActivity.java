package com.shen.alarmiq.standby;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.shen.alarmiq.AlarmiqApp;
import com.shen.alarmiq.R;

public class StandbySettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AlarmiqApp.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_standby_settings);

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
        // Fix for toolbar back button
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, new StandbySettingsFragment())
                    .commit();
        }
    }
}

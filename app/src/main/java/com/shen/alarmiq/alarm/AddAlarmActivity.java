package com.shen.alarmiq.alarm;

import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.shen.alarmiq.R;
import com.shen.alarmiq.math.Difficulty;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class AddAlarmActivity extends AppCompatActivity {

    public static final String RESULT_TRIGGER_MILLIS = "trigger_millis";
    public static final String RESULT_ALARM_LABEL = "alarm_label";

    private static final int FULL_WEEK_MASK = 0x7F;

    private AlarmStorage storage;
    private AlarmScheduler scheduler;
    private Alarm alarm;

    private AnalogClockPicker clock;
    private TextView txtHour;
    private TextView txtMinute;
    private TextView txtCountdown;
    private TextView txtModeHint;
    private TextView txtSoundName;
    private MaterialButtonToggleGroup toggleAmPm;
    private TextInputEditText inputLabel;
    private LinearLayout difficultyContainer;
    private MaterialButton btnEveryDay;

    /** Button id at index i toggles bit `dayBitForButtonIndex[i]` in the repeat mask. */
    private final int[] dayButtonIds = {
            R.id.dayMon, R.id.dayTue, R.id.dayWed, R.id.dayThu,
            R.id.dayFri, R.id.daySat, R.id.daySun
    };
    /** Bit indices matching Calendar.DAY_OF_WEEK - 1 (Sun=0). */
    private final int[] dayBitForButtonIndex = {1, 2, 3, 4, 5, 6, 0};

    /** Suppress day-toggle re-entrancy when programmatically applying state. */
    private boolean applyingBulkDays = false;

    private final ActivityResultLauncher<Intent> ringtoneLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() != RESULT_OK || r.getData() == null) return;
                Uri uri = r.getData().getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                if (uri == null) {
                    alarm.soundUri = null;
                    alarm.soundTitle = null;
                } else {
                    alarm.soundUri = uri.toString();
                    try {
                        alarm.soundTitle = RingtoneManager.getRingtone(this, uri).getTitle(this);
                    } catch (Exception e) {
                        alarm.soundTitle = null;
                    }
                }
                updateSoundName();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_alarm);

        storage = new AlarmStorage(this);
        scheduler = new AlarmScheduler(this);

        long id = getIntent().getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L);
        if (id > 0) {
            alarm = storage.getById(id);
        }
        if (alarm == null) {
            alarm = new Alarm();
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        clock = findViewById(R.id.clock);
        txtHour = findViewById(R.id.txtHour);
        txtMinute = findViewById(R.id.txtMinute);
        txtCountdown = findViewById(R.id.txtCountdown);
        txtModeHint = findViewById(R.id.txtModeHint);
        txtSoundName = findViewById(R.id.txtSoundName);
        toggleAmPm = findViewById(R.id.toggleAmPm);
        inputLabel = findViewById(R.id.inputLabel);
        difficultyContainer = findViewById(R.id.difficultyContainer);
        btnEveryDay = findViewById(R.id.btnEveryDay);

        applyInsets();

        clock.setTime(alarm.hour, alarm.minute);
        updateDigital(alarm.hour, alarm.minute);
        updateAmPmState(alarm.hour);
        updateModeHint(clock.isHourMode());
        updateDigitalColors(clock.isHourMode());

        clock.setOnTimeChangedListener((h, m) -> {
            alarm.hour = h;
            alarm.minute = m;
            updateDigital(h, m);
            updateAmPmState(h);
            updateCountdown();
        });
        clock.setOnModeChangedListener(hourMode -> {
            updateModeHint(hourMode);
            updateDigitalColors(hourMode);
        });

        wireDigitalTap(txtHour, true);
        wireDigitalTap(txtMinute, false);

        toggleAmPm.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            int currentHour = alarm.hour;
            int base = currentHour % 12;
            if (checkedId == R.id.btnPm) {
                alarm.hour = base + 12;
            } else {
                alarm.hour = base;
            }
            clock.setTime(alarm.hour, alarm.minute);
            updateDigital(alarm.hour, alarm.minute);
            updateCountdown();
        });

        inputLabel.setText(alarm.label);
        inputLabel.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                alarm.label = s.toString();
            }
        });

        findViewById(R.id.cardSound).setOnClickListener(v -> openRingtonePicker());
        updateSoundName();

        setupDayToggles();
        setupEveryDayToggle();
        setupDifficultyCards();
        updateCountdown();

        findViewById(R.id.btnSave).setOnClickListener(v -> save());
    }

    private void applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, 0, bars.right, 0);
            return insets;
        });
        View save = findViewById(R.id.btnSave);
        ViewCompat.setOnApplyWindowInsetsListener(save, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            lp.bottomMargin = bars.bottom + dp(20);
            v.setLayoutParams(lp);
            return insets;
        });
    }

    @SuppressWarnings("ClickableViewAccessibility")
    private void wireDigitalTap(TextView field, boolean hourField) {
        GestureDetector gd = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                clock.setMode(hourField);
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (hourField) showHourEditDialog();
                else showMinuteEditDialog();
                return true;
            }
        });
        field.setClickable(true);
        field.setOnTouchListener((v, ev) -> gd.onTouchEvent(ev));
    }

    private void showHourEditDialog() {
        boolean is24 = DateFormat.is24HourFormat(this);
        int displayHour;
        if (is24) {
            displayHour = alarm.hour;
        } else {
            displayHour = alarm.hour % 12;
            if (displayHour == 0) displayHour = 12;
        }
        int maxVal = is24 ? 23 : 12;
        int minVal = is24 ? 0 : 1;
        showNumberDialog(getString(R.string.set_hour), String.valueOf(displayHour), minVal, maxVal, value -> {
            if (is24) {
                alarm.hour = value;
            } else {
                int normalized = value % 12; // 12 → 0
                boolean wasPm = alarm.hour >= 12;
                alarm.hour = normalized + (wasPm ? 12 : 0);
            }
            clock.setTime(alarm.hour, alarm.minute);
            updateDigital(alarm.hour, alarm.minute);
            updateAmPmState(alarm.hour);
            updateCountdown();
        });
    }

    private void showMinuteEditDialog() {
        showNumberDialog(getString(R.string.set_minute), String.valueOf(alarm.minute), 0, 59, value -> {
            alarm.minute = value;
            clock.setTime(alarm.hour, alarm.minute);
            updateDigital(alarm.hour, alarm.minute);
            updateCountdown();
        });
    }

    private interface IntConsumer { void accept(int value); }

    private void showNumberDialog(String title, String initial, int minVal, int maxVal, IntConsumer onAccept) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(initial);
        input.setSelectAllOnFocus(true);
        int pad = dp(20);
        FrameLayout container = new FrameLayout(this);
        container.setPadding(pad, dp(8), pad, 0);
        container.addView(input, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));

        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setView(container)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String raw = input.getText().toString().trim();
                    if (raw.isEmpty()) return;
                    int v;
                    try { v = Integer.parseInt(raw); }
                    catch (NumberFormatException e) { return; }
                    if (v < minVal) v = minVal;
                    if (v > maxVal) v = maxVal;
                    onAccept.accept(v);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
        input.requestFocus();
    }

    private void updateDigital(int hour24, int minute) {
        boolean is24 = DateFormat.is24HourFormat(this);
        int displayHour;
        if (is24) {
            displayHour = hour24;
            txtHour.setText(String.format(Locale.getDefault(), "%02d", displayHour));
        } else {
            displayHour = hour24 % 12;
            if (displayHour == 0) displayHour = 12;
            txtHour.setText(String.format(Locale.getDefault(), "%d", displayHour));
        }
        txtMinute.setText(String.format(Locale.getDefault(), "%02d", minute));
    }

    private void updateAmPmState(int hour24) {
        if (hour24 < 12) {
            toggleAmPm.check(R.id.btnAm);
        } else {
            toggleAmPm.check(R.id.btnPm);
        }
    }

    private void updateModeHint(boolean hourMode) {
        txtModeHint.setText(hourMode ? R.string.hint_hour_mode : R.string.hint_minute_mode);
    }

    private void updateDigitalColors(boolean hourMode) {
        int active = resolveThemeColor("colorPrimary", 0xFF7DD3FC);
        int inactive = resolveThemeColor("colorOnSurface", 0xFFE0E0E0);
        txtHour.setTextColor(hourMode ? active : applyAlpha(inactive, 0x99));
        txtMinute.setTextColor(hourMode ? applyAlpha(inactive, 0x99) : active);
    }

    private int applyAlpha(int rgb, int alphaByte) {
        return (alphaByte << 24) | (rgb & 0x00FFFFFF);
    }

    private void updateSoundName() {
        if (alarm.soundTitle != null && !alarm.soundTitle.isEmpty()) {
            txtSoundName.setText(alarm.soundTitle);
        } else {
            txtSoundName.setText(R.string.sound_default);
        }
    }

    private void openRingtonePicker() {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,
                RingtoneManager.TYPE_ALARM | RingtoneManager.TYPE_RINGTONE);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.pick_sound));
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
        Uri current = alarm.soundUri == null
                ? RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                : Uri.parse(alarm.soundUri);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Parcelable) current);
        ringtoneLauncher.launch(intent);
    }

    private void setupDayToggles() {
        for (int i = 0; i < dayButtonIds.length; i++) {
            int btnIdx = i;
            MaterialButton btn = findViewById(dayButtonIds[i]);
            int bit = 1 << dayBitForButtonIndex[btnIdx];
            boolean checked = (alarm.repeatMask & bit) != 0;
            btn.setChecked(checked);
            btn.addOnCheckedChangeListener((b, isChecked) -> {
                if (isChecked) {
                    alarm.repeatMask |= bit;
                } else {
                    alarm.repeatMask &= ~bit;
                }
                if (!applyingBulkDays) {
                    syncEveryDayChip();
                }
                updateCountdown();
            });
        }
    }

    private void setupEveryDayToggle() {
        btnEveryDay.setChecked((alarm.repeatMask & FULL_WEEK_MASK) == FULL_WEEK_MASK);
        btnEveryDay.addOnCheckedChangeListener((b, isChecked) -> {
            applyingBulkDays = true;
            if (isChecked) {
                alarm.repeatMask = FULL_WEEK_MASK;
                for (int btnId : dayButtonIds) {
                    ((MaterialButton) findViewById(btnId)).setChecked(true);
                }
            } else if ((alarm.repeatMask & FULL_WEEK_MASK) == FULL_WEEK_MASK) {
                // Only clear when going from all → none. If user is unchecking
                // a chip that the per-day buttons already disagreed with, ignore.
                alarm.repeatMask = 0;
                for (int btnId : dayButtonIds) {
                    ((MaterialButton) findViewById(btnId)).setChecked(false);
                }
            }
            applyingBulkDays = false;
            updateCountdown();
        });
    }

    private void syncEveryDayChip() {
        boolean all = (alarm.repeatMask & FULL_WEEK_MASK) == FULL_WEEK_MASK;
        if (btnEveryDay.isChecked() != all) {
            applyingBulkDays = true;
            btnEveryDay.setChecked(all);
            applyingBulkDays = false;
        }
    }

    private void setupDifficultyCards() {
        difficultyContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Difficulty d : Difficulty.values()) {
            MaterialCardView card = (MaterialCardView) inflater.inflate(
                    R.layout.item_difficulty, difficultyContainer, false);
            TextView title = card.findViewById(R.id.txtTitle);
            TextView desc = card.findViewById(R.id.txtDesc);
            View check = card.findViewById(R.id.imgCheck);
            title.setText(d.labelRes);
            desc.setText(d.descRes);
            boolean selected = d == alarm.difficulty;
            card.setChecked(selected);
            check.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);

            card.setOnClickListener(v -> {
                alarm.difficulty = d;
                for (int i = 0; i < difficultyContainer.getChildCount(); i++) {
                    MaterialCardView c = (MaterialCardView) difficultyContainer.getChildAt(i);
                    boolean isSel = i == d.ordinal();
                    c.setChecked(isSel);
                    c.findViewById(R.id.imgCheck).setVisibility(isSel ? View.VISIBLE : View.INVISIBLE);
                }
            });

            difficultyContainer.addView(card);
        }
    }

    private void updateCountdown() {
        long delta = alarm.nextTriggerMillis() - System.currentTimeMillis();
        if (delta < 0) delta = 0;
        long hours = TimeUnit.MILLISECONDS.toHours(delta);
        long mins = TimeUnit.MILLISECONDS.toMinutes(delta) - hours * 60;
        String span = hours > 0 ? (hours + "h " + mins + "m") : (mins + "m");
        txtCountdown.setText(getString(R.string.alarm_set_for, span));
    }

    private void save() {
        alarm.enabled = true;
        storage.upsert(alarm);
        long trigger = scheduler.schedule(alarm);
        Intent data = new Intent();
        data.putExtra(RESULT_TRIGGER_MILLIS, trigger);
        if (alarm.label != null && !alarm.label.trim().isEmpty()) {
            data.putExtra(RESULT_ALARM_LABEL, alarm.label.trim());
        }
        setResult(RESULT_OK, data);
        finish();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private int resolveThemeColor(String attrName, int fallback) {
        int attrId = getResources().getIdentifier(attrName, "attr", getPackageName());
        if (attrId == 0) {
            attrId = getResources().getIdentifier(attrName, "attr", "android");
        }
        if (attrId == 0) return fallback;
        android.util.TypedValue tv = new android.util.TypedValue();
        if (!getTheme().resolveAttribute(attrId, tv, true)) return fallback;
        if (tv.resourceId != 0) {
            return androidx.core.content.ContextCompat.getColor(this, tv.resourceId);
        }
        return tv.data;
    }
}

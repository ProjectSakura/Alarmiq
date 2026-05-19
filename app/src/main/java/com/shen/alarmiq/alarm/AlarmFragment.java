package com.shen.alarmiq.alarm;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.shen.alarmiq.R;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class AlarmFragment extends Fragment implements AlarmAdapter.Listener {

    private AlarmStorage storage;
    private AlarmScheduler scheduler;
    private AlarmAdapter adapter;
    private RecyclerView recycler;
    private LinearLayout emptyState;
    private ExtendedFloatingActionButton fab;

    private final ActivityResultLauncher<Intent> editLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    this::onEditorResult);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_alarms, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        storage = new AlarmStorage(requireContext());
        scheduler = new AlarmScheduler(requireContext());

        recycler = view.findViewById(R.id.recycler);
        emptyState = view.findViewById(R.id.emptyState);
        fab = view.findViewById(R.id.fabAdd);

        boolean is24 = DateFormat.is24HourFormat(requireContext());
        adapter = new AlarmAdapter(this, is24);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        ViewCompat.setOnApplyWindowInsetsListener(recycler, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(),
                    v.getPaddingRight(), bars.bottom + dp(120));
            return insets;
        });
        ViewCompat.setOnApplyWindowInsetsListener(fab, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            lp.bottomMargin = bars.bottom + dp(24);
            lp.rightMargin = bars.right + dp(24);
            lp.leftMargin = bars.left + dp(24);
            v.setLayoutParams(lp);
            return insets;
        });

        fab.setOnClickListener(v -> openEditor(-1L));
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        List<Alarm> alarms = storage.getAll();
        adapter.submit(alarms);
        boolean empty = alarms.isEmpty();
        recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void openEditor(long alarmId) {
        if (!scheduler.canScheduleExact()) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setMessage(R.string.needs_exact_alarm_permission)
                    .setPositiveButton(android.R.string.ok, (d, w) -> {
                        Intent i = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:" + requireContext().getPackageName()));
                        startActivity(i);
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return;
        }
        Intent i = new Intent(requireContext(), AddAlarmActivity.class);
        if (alarmId > 0) i.putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId);
        editLauncher.launch(i);
    }

    private void onEditorResult(ActivityResult result) {
        refresh();
        if (result.getResultCode() != android.app.Activity.RESULT_OK || result.getData() == null) return;
        long trigger = result.getData().getLongExtra(AddAlarmActivity.RESULT_TRIGGER_MILLIS, -1L);
        if (trigger <= 0) return;
        String label = result.getData().getStringExtra(AddAlarmActivity.RESULT_ALARM_LABEL);
        showCountdownSnackbar(trigger, label);
    }

    @Override
    public void onAlarmClick(Alarm alarm) {
        openEditor(alarm.id);
    }

    @Override
    public void onAlarmToggled(Alarm alarm, boolean enabled) {
        alarm.enabled = enabled;
        storage.upsert(alarm);
        if (enabled) {
            long trigger = scheduler.schedule(alarm);
            showCountdownSnackbar(trigger, null);
        } else {
            scheduler.cancel(alarm.id);
        }
    }

    @Override
    public void onAlarmMenuClick(Alarm alarm, View anchor) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.inflate(R.menu.menu_alarm_row);
        menu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_modify) {
                openEditor(alarm.id);
                return true;
            }
            if (id == R.id.action_delete) {
                confirmDelete(alarm);
                return true;
            }
            return false;
        });
        menu.show();
    }

    private void confirmDelete(Alarm alarm) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(alarm.formatTime(DateFormat.is24HourFormat(requireContext())))
                .setMessage(R.string.delete_alarm_confirm)
                .setPositiveButton(R.string.delete, (d, w) -> {
                    scheduler.cancel(alarm.id);
                    storage.delete(alarm.id);
                    refresh();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showCountdownSnackbar(long triggerMillis, String label) {
        if (getView() == null) return;
        long delta = triggerMillis - System.currentTimeMillis();
        if (delta < 0) delta = 0;
        long hours = TimeUnit.MILLISECONDS.toHours(delta);
        long mins = TimeUnit.MILLISECONDS.toMinutes(delta) - hours * 60;
        String span = hours > 0 ? (hours + "h " + mins + "m") : (mins + "m");
        String message = (label != null && !label.isEmpty())
                ? getString(R.string.alarm_in_with_label, label, span)
                : getString(R.string.alarm_set_for, span);
        Snackbar.make(getView(), message, Snackbar.LENGTH_LONG)
                .setAnchorView(fab)
                .show();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}

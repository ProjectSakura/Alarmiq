package com.shen.alarmiq.standby;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.shen.alarmiq.AlarmiqApp;
import com.shen.alarmiq.R;

public class StandbySettingsFragment extends Fragment {

    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    saveBackgroundUri(uri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_standby_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupSwitches(view);
        setupOverlayPermission(view);
        setupBackgroundPicker(view);
        setupPreviewButton(view);
        setupGuide(view);
    }

    private void setupPreviewButton(View view) {
        MaterialButton btnPreview = view.findViewById(R.id.btnPreviewStandby);
        btnPreview.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), StandbyActivity.class);
            intent.putExtra("is_preview", true);
            startActivity(intent);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) {
            updateOverlayPermissionUI(getView());
        }
    }

    private void setupSwitches(View view) {
        MaterialSwitch switchEnabled = view.findViewById(R.id.switchStandbyEnabled);
        MaterialSwitch switchBurnIn = view.findViewById(R.id.switchBurnIn);

        switchEnabled.setChecked(requireContext().getSharedPreferences(AlarmiqApp.PREFS_SETTINGS, Context.MODE_PRIVATE)
                .getBoolean(StandbyReceiver.KEY_STANDBY_ENABLED, true));
        switchBurnIn.setChecked(requireContext().getSharedPreferences(AlarmiqApp.PREFS_SETTINGS, Context.MODE_PRIVATE)
                .getBoolean("standby_burn_in_enabled", true));

        switchEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            requireContext().getSharedPreferences(AlarmiqApp.PREFS_SETTINGS, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(StandbyReceiver.KEY_STANDBY_ENABLED, isChecked)
                    .apply();
            
            Intent intent = new Intent(requireContext(), StandbyService.class);
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    requireContext().startForegroundService(intent);
                } else {
                    requireContext().startService(intent);
                }
            } else {
                requireContext().stopService(intent);
            }
        });

        switchBurnIn.setOnCheckedChangeListener((buttonView, isChecked) -> {
            requireContext().getSharedPreferences(AlarmiqApp.PREFS_SETTINGS, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("standby_burn_in_enabled", isChecked)
                    .apply();
        });
    }

    private void setupOverlayPermission(View view) {
        MaterialButton btnGrant = view.findViewById(R.id.btnGrantOverlay);
        btnGrant.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + requireContext().getPackageName()));
            startActivity(intent);
        });
        updateOverlayPermissionUI(view);
    }

    private void updateOverlayPermissionUI(View view) {
        boolean hasPermission = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hasPermission = Settings.canDrawOverlays(requireContext());
        }

        View layout = view.findViewById(R.id.layoutOverlayPermission);
        View divider = view.findViewById(R.id.dividerPermission);
        if (hasPermission) {
            layout.setVisibility(View.GONE);
            divider.setVisibility(View.GONE);
        } else {
            layout.setVisibility(View.VISIBLE);
            divider.setVisibility(View.VISIBLE);
        }
    }

    private void setupBackgroundPicker(View view) {
        MaterialButton btnPick = view.findViewById(R.id.btnPickBackground);
        btnPick.setOnClickListener(v -> {
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        MaterialButton btnReset = view.findViewById(R.id.btnResetBackground);
        btnReset.setOnClickListener(v -> {
            requireContext().getSharedPreferences(AlarmiqApp.PREFS_SETTINGS, Context.MODE_PRIVATE)
                    .edit()
                    .remove("standby_background_uri")
                    .apply();
        });
    }

    private void saveBackgroundUri(Uri uri) {
        try {
            requireContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {}

        requireContext().getSharedPreferences(AlarmiqApp.PREFS_SETTINGS, Context.MODE_PRIVATE)
                .edit()
                .putString("standby_background_uri", uri.toString())
                .apply();
    }

    private void setupGuide(View view) {
        TextView txtGuide = view.findViewById(R.id.txtStandbyGuide);
        String guide = getString(R.string.standby_guide_step1) + "\n" +
                getString(R.string.standby_guide_step2) + "\n" +
                getString(R.string.standby_guide_step3) + "\n" +
                getString(R.string.standby_guide_step4) + "\n" +
                getString(R.string.standby_guide_step5);
        txtGuide.setText(guide);
    }
}

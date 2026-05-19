package com.shen.alarmiq.alarm;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.shen.alarmiq.R;

import java.util.ArrayList;
import java.util.List;

public class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.VH> {

    public interface Listener {
        void onAlarmClick(Alarm alarm);
        void onAlarmToggled(Alarm alarm, boolean enabled);
        void onAlarmMenuClick(Alarm alarm, View anchor);
    }

    private final List<Alarm> items = new ArrayList<>();
    private final Listener listener;
    private final boolean is24h;

    public AlarmAdapter(Listener listener, boolean is24h) {
        this.listener = listener;
        this.is24h = is24h;
        setHasStableIds(true);
    }

    public void submit(List<Alarm> alarms) {
        items.clear();
        items.addAll(alarms);
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).id;
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alarm, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(VH h, int position) {
        Alarm a = items.get(position);
        h.bind(a);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class VH extends RecyclerView.ViewHolder {
        final TextView txtTime;
        final TextView txtLabel;
        final TextView txtMeta;
        final MaterialSwitch switchEnabled;
        final ImageButton btnMenu;

        VH(View v) {
            super(v);
            txtTime = v.findViewById(R.id.txtTime);
            txtLabel = v.findViewById(R.id.txtLabel);
            txtMeta = v.findViewById(R.id.txtMeta);
            switchEnabled = v.findViewById(R.id.switchEnabled);
            btnMenu = v.findViewById(R.id.btnMenu);
        }

        void bind(Alarm a) {
            txtTime.setText(a.formatTime(is24h));
            if (a.label != null && !a.label.trim().isEmpty()) {
                txtLabel.setText(a.label);
                txtLabel.setVisibility(View.VISIBLE);
            } else {
                txtLabel.setVisibility(View.GONE);
            }
            String difficulty = itemView.getContext().getString(a.difficulty.labelRes);
            String repeat = formatRepeat(a.repeatMask);
            txtMeta.setText(repeat.isEmpty() ? difficulty : difficulty + " · " + repeat);

            switchEnabled.setOnCheckedChangeListener(null);
            switchEnabled.setChecked(a.enabled);
            switchEnabled.setOnCheckedChangeListener((b, c) -> listener.onAlarmToggled(a, c));

            itemView.setOnClickListener(v -> listener.onAlarmClick(a));
            btnMenu.setOnClickListener(v -> listener.onAlarmMenuClick(a, v));
        }
    }

    private static String formatRepeat(int mask) {
        if (mask == 0) return "";
        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        StringBuilder sb = new StringBuilder();
        int weekdays = (1 << 1) | (1 << 2) | (1 << 3) | (1 << 4) | (1 << 5);
        int weekend = (1 << 0) | (1 << 6);
        if (mask == weekdays) return "Weekdays";
        if (mask == weekend) return "Weekends";
        if (mask == (weekdays | weekend)) return "Every day";
        for (int i = 0; i < 7; i++) {
            if ((mask & (1 << i)) != 0) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(days[i]);
            }
        }
        return sb.toString();
    }
}

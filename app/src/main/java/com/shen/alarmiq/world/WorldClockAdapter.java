package com.shen.alarmiq.world;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.shen.alarmiq.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class WorldClockAdapter extends RecyclerView.Adapter<WorldClockAdapter.VH> {

    public interface Listener {
        void onCityClick(String zoneId, View anchor);
    }

    private final List<String> ids = new ArrayList<>();
    private final boolean is24h;
    private final Listener listener;

    public WorldClockAdapter(boolean is24h, Listener listener) {
        this.is24h = is24h;
        this.listener = listener;
        setHasStableIds(true);
    }

    public void submit(List<String> newIds) {
        ids.clear();
        ids.addAll(newIds);
        notifyDataSetChanged();
    }

    /** Re-render times in place without a full notifyDataSetChanged. */
    public void refreshTimes() {
        notifyItemRangeChanged(0, ids.size());
    }

    @Override
    public long getItemId(int position) {
        return ids.get(position).hashCode();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_world_clock, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        h.bind(ids.get(position));
    }

    @Override
    public int getItemCount() {
        return ids.size();
    }

    class VH extends RecyclerView.ViewHolder {
        final TextView txtCity;
        final TextView txtOffset;
        final TextView txtTime;

        VH(View v) {
            super(v);
            txtCity = v.findViewById(R.id.txtCity);
            txtOffset = v.findViewById(R.id.txtOffset);
            txtTime = v.findViewById(R.id.txtTime);
        }

        void bind(String zoneId) {
            TimeZone zone = Cities.timeZoneFor(zoneId);
            txtCity.setText(Cities.displayNameForZone(zoneId));
            txtTime.setText(formatTime(zone, is24h));
            txtOffset.setText(formatOffset(itemView, zone));
            itemView.setOnClickListener(v -> listener.onCityClick(zoneId, v));
        }
    }

    private static String formatTime(TimeZone zone, boolean is24h) {
        SimpleDateFormat fmt = new SimpleDateFormat(is24h ? "HH:mm" : "h:mm a", Locale.getDefault());
        fmt.setTimeZone(zone);
        return fmt.format(new Date());
    }

    private static String formatOffset(View itemView, TimeZone zone) {
        TimeZone local = TimeZone.getDefault();
        long now = System.currentTimeMillis();
        long deltaMs = zone.getOffset(now) - local.getOffset(now);
        if (deltaMs == 0) return itemView.getContext().getString(R.string.offset_same);

        long absMs = Math.abs(deltaMs);
        long hours = TimeUnit.MILLISECONDS.toHours(absMs);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(absMs) - hours * 60;
        String span = minutes == 0
                ? hours + "h"
                : hours + "h " + minutes + "m";
        int resId = deltaMs > 0 ? R.string.offset_ahead : R.string.offset_behind;
        return itemView.getContext().getString(resId, span);
    }
}

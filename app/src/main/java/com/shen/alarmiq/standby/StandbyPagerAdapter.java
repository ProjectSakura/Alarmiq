package com.shen.alarmiq.standby;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.shen.alarmiq.R;
import com.shen.alarmiq.alarm.AnalogClockPicker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class StandbyPagerAdapter extends RecyclerView.Adapter<StandbyPagerAdapter.ViewHolder> {

    public static final int TYPE_DIGITAL = 0;
    public static final int TYPE_ANALOG = 1;
    public static final int TYPE_MINIMAL = 2;
    public static final int TYPE_DVD = 3;
    public static final int TYPE_STACKED = 4;

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId;
        switch (viewType) {
            case TYPE_ANALOG: layoutId = R.layout.item_standby_analog; break;
            case TYPE_MINIMAL: layoutId = R.layout.item_standby_minimal; break;
            case TYPE_DVD: layoutId = R.layout.item_standby_dvd; break;
            case TYPE_STACKED: layoutId = R.layout.item_standby_stacked; break;
            case TYPE_DIGITAL:
            default: layoutId = R.layout.item_standby_digital; break;
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new ViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.update();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return 5;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final int type;
        public ViewHolder(@NonNull View itemView, int type) {
            super(itemView);
            this.type = type;
        }

        void update() {
            Calendar cal = Calendar.getInstance();
            Date now = cal.getTime();
            
            if (type == TYPE_STACKED) {
                TextView tvH = itemView.findViewById(R.id.txtHours);
                TextView tvM = itemView.findViewById(R.id.txtMinutes);
                if (tvH != null) tvH.setText(new SimpleDateFormat("HH", Locale.getDefault()).format(now));
                if (tvM != null) tvM.setText(new SimpleDateFormat("mm", Locale.getDefault()).format(now));
            } else {
                TextView tvTime = itemView.findViewById(R.id.txtTime);
                if (tvTime != null) {
                    String format = (type == TYPE_MINIMAL) ? "HH:mm" : "HH:mm";
                    tvTime.setText(new SimpleDateFormat(format, Locale.getDefault()).format(now));
                }
                
                TextView tvDate = itemView.findViewById(R.id.txtDate);
                if (tvDate != null) {
                    String format = (type == TYPE_ANALOG) ? "MMM d" : "EEEE, MMMM d";
                    tvDate.setText(new SimpleDateFormat(format, Locale.getDefault()).format(now).toUpperCase());
                }
            }

            if (type == TYPE_ANALOG) {
                AnalogClockPicker clock = itemView.findViewById(R.id.analogClock);
                if (clock != null) {
                    clock.setTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
                }
            }
        }
    }
}

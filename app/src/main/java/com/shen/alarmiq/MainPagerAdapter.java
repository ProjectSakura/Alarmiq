package com.shen.alarmiq;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.shen.alarmiq.alarm.AlarmFragment;
import com.shen.alarmiq.stopwatch.StopwatchFragment;
import com.shen.alarmiq.timer.TimerFragment;
import com.shen.alarmiq.world.WorldClockFragment;

public class MainPagerAdapter extends FragmentStateAdapter {

    public MainPagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 1: return new StopwatchFragment();
            case 2: return new TimerFragment();
            case 3: return new WorldClockFragment();
            case 0:
            default: return new AlarmFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}

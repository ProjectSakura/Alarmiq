package com.shen.alarmiq.world;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.shen.alarmiq.R;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class WorldClockFragment extends Fragment {

    private WorldClockStorage storage;
    private WorldClockAdapter adapter;
    private RecyclerView recycler;
    private LinearLayout emptyState;
    private ExtendedFloatingActionButton fab;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            adapter.refreshTimes();
            handler.postDelayed(this, nextMinuteDelayMs());
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_world_clock, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        storage = new WorldClockStorage(requireContext());

        recycler = view.findViewById(R.id.recyclerCities);
        emptyState = view.findViewById(R.id.emptyState);
        fab = view.findViewById(R.id.fabAddCity);

        boolean is24 = DateFormat.is24HourFormat(requireContext());
        adapter = new WorldClockAdapter(is24, this::onCityClick);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        ViewCompat.setOnApplyWindowInsetsListener(recycler, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            boolean isMultiPane = getActivity() != null && getActivity().findViewById(R.id.pager) == null;
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(),
                    v.getPaddingRight(), (isMultiPane ? 0 : bars.bottom) + dp(100));
            return insets;
        });
        ViewCompat.setOnApplyWindowInsetsListener(fab, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            boolean isMultiPane = getActivity() != null && getActivity().findViewById(R.id.pager) == null;
            lp.bottomMargin = (isMultiPane ? 0 : bars.bottom) + dp(24);
            lp.rightMargin = (isMultiPane ? 0 : bars.right) + dp(24);
            lp.leftMargin = (isMultiPane ? 0 : bars.left) + dp(24);
            v.setLayoutParams(lp);
            return insets;
        });

        fab.setOnClickListener(v -> openCityPicker());

        refresh();
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter.refreshTimes();
        handler.removeCallbacks(tick);
        handler.postDelayed(tick, nextMinuteDelayMs());
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    private void refresh() {
        List<String> ids = storage.getAll();
        adapter.submit(ids);
        boolean empty = ids.isEmpty();
        recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void onCityClick(String zoneId, View anchor) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.getMenu().add(0, 1, 0, R.string.remove_city);
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                storage.remove(zoneId);
                refresh();
                return true;
            }
            return false;
        });
        menu.show();
    }

    private void openCityPicker() {
        View root = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_city_picker, null, false);
        TextInputEditText input = root.findViewById(R.id.inputSearch);
        RecyclerView list = root.findViewById(R.id.recyclerCities);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));

        final AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_city)
                .setView(root)
                .setNegativeButton(R.string.cancel, null)
                .create();

        CityPickAdapter pickAdapter = new CityPickAdapter(city -> {
            storage.add(city.zoneId);
            refresh();
            dialog.dismiss();
        });
        list.setAdapter(pickAdapter);
        pickAdapter.submit(Cities.filter(""));

        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                pickAdapter.submit(Cities.filter(s.toString()));
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        dialog.show();
    }

    private long nextMinuteDelayMs() {
        Calendar c = Calendar.getInstance();
        int seconds = c.get(Calendar.SECOND);
        int millis = c.get(Calendar.MILLISECOND);
        long delay = (60 - seconds) * 1000L - millis;
        if (delay < 250) delay += 60000L;
        return delay;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    /** Lightweight inner adapter for the search results. */
    private static class CityPickAdapter extends RecyclerView.Adapter<CityPickAdapter.VH> {

        interface OnPick { void onPick(Cities.City city); }

        private final java.util.List<Cities.City> data = new java.util.ArrayList<>();
        private final OnPick onPick;

        CityPickAdapter(OnPick onPick) { this.onPick = onPick; }

        void submit(java.util.List<Cities.City> next) {
            data.clear();
            data.addAll(next);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_city_pick, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Cities.City c = data.get(position);
            holder.txtName.setText(c.displayName);
            holder.txtOffset.setText(formatUtc(TimeZone.getTimeZone(c.zoneId)));
            holder.itemView.setOnClickListener(v -> onPick.onPick(c));
        }

        @Override
        public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            final TextView txtName;
            final TextView txtOffset;
            VH(View v) {
                super(v);
                txtName = v.findViewById(R.id.txtCityName);
                txtOffset = v.findViewById(R.id.txtCityOffset);
            }
        }

        private static String formatUtc(TimeZone zone) {
            int offsetMin = zone.getOffset(System.currentTimeMillis()) / 60_000;
            int absMin = Math.abs(offsetMin);
            int h = absMin / 60;
            int m = absMin % 60;
            char sign = offsetMin >= 0 ? '+' : '-';
            if (m == 0) {
                return String.format(Locale.getDefault(), "UTC%c%d", sign, h);
            }
            return String.format(Locale.getDefault(), "UTC%c%d:%02d", sign, h, m);
        }
    }
}

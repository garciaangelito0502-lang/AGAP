package ph.agap.app.ui.home;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.Calendar;
import java.util.List;

import ph.agap.app.R;
import ph.agap.app.captions.GoogleOnDeviceSupport;
import ph.agap.app.captions.VoskModelManager;
import ph.agap.app.captions.VoskModelSpec;
import ph.agap.app.demo.DemoMode;
import ph.agap.app.ui.MainActivity;
import ph.agap.app.ui.captions.CaptionsViewModel;
import ph.agap.app.ui.info.DemoModeActivity;
import ph.agap.app.ui.info.SoundCategoriesActivity;
import ph.agap.app.ui.info.VibrationPatternsActivity;
import ph.agap.app.ui.settings.SettingsActivity;
import ph.agap.app.watch.WatchSender;

/**
 * Home: the two modes, a status card (what works offline right now, and the microphone), and the
 * other screens. AGAP never listens on Home: the microphone is used only on the Captions and
 * Alerts tabs (contract 1.10), so the status says so.
 */
public class HomeFragment extends Fragment {

    private static final String ENGLISH = "en-US";
    private ViewGroup statusRows;

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ((TextView) view.findViewById(R.id.greeting)).setText(greetingForNow());

        MainActivity main = (MainActivity) requireActivity();
        view.findViewById(R.id.card_captions).setOnClickListener(v -> main.selectTab(R.id.nav_captions));
        view.findViewById(R.id.card_alerts).setOnClickListener(v -> main.selectTab(R.id.nav_alerts));
        view.findViewById(R.id.row_categories).setOnClickListener(v -> open(SoundCategoriesActivity.class));
        view.findViewById(R.id.row_vibration).setOnClickListener(v -> open(VibrationPatternsActivity.class));
        view.findViewById(R.id.row_demo).setOnClickListener(v -> open(DemoModeActivity.class));
        view.findViewById(R.id.row_settings).setOnClickListener(v -> open(SettingsActivity.class));
        statusRows = view.findViewById(R.id.home_status_rows);
    }

    private final Runnable watchChanged = () -> {
        if (getView() != null) showStatus();
    };

    @Override
    public void onStart() {
        super.onStart();
        WatchSender watch = WatchSender.get(requireContext());
        watch.addListener(watchChanged);
        watch.refresh();
        showStatus();
    }

    @Override
    public void onStop() {
        WatchSender.get(requireContext()).removeListener(watchChanged);
        super.onStop();
    }

    /** One line per item, each with a check (ready) or info (action needed) icon and words. */
    private void showStatus() {
        Context context = requireContext();
        boolean mic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
        statusRows.removeAllViews();
        if (DemoMode.isOn()) addStatus(false, R.string.home_status_demo);
        addStatus(true, R.string.home_status_offline);
        addStatus(mic, mic ? R.string.home_status_mic_off : R.string.home_status_mic_not_allowed);
        addStatus(true, R.string.home_status_alerts);
        addEnglishStatus(context, mic);
        boolean filipino = VoskModelManager.isInstalled(context, VoskModelSpec.FILIPINO);
        addStatus(filipino, filipino ? R.string.home_status_filipino_ready : R.string.home_status_filipino_setup);
        addWatchStatus(context);
    }

    /** The watch link (contract 1.11): which AGAP watch is connected, if any. */
    private void addWatchStatus(Context context) {
        WatchSender watch = WatchSender.get(context);
        if (!watch.isAvailable()) {
            addStatus(false, getString(R.string.home_status_watch_unavailable));
            return;
        }
        List<String> names = watch.connectedWatchNames();
        if (names.isEmpty()) {
            addStatus(false, getString(R.string.home_status_watch_none));
        } else {
            addStatus(true, getString(R.string.home_status_watch, TextUtils.join(", ", names)));
        }
    }

    private void addEnglishStatus(Context context, boolean mic) {
        CaptionsViewModel captions = new ViewModelProvider(requireActivity()).get(CaptionsViewModel.class);
        GoogleOnDeviceSupport.Result english = captions.englishSupport;
        if (english == null) {
            if (!mic) {
                addStatus(false, R.string.home_status_english_later);
                return;
            }
            addStatus(false, R.string.home_status_english_checking);
            GoogleOnDeviceSupport.check(context, ENGLISH, result -> {
                captions.englishSupport = result;
                if (getView() != null && isAdded()) showStatus();
            });
            return;
        }
        switch (english.status) {
            case NEEDS_DOWNLOAD:
            case DOWNLOADING:
                addStatus(false, R.string.home_status_english_pack);
                break;
            case OLD_ANDROID:
            case NO_RECOGNIZER:
            case LANGUAGE_NOT_SUPPORTED:
                boolean own = VoskModelManager.isInstalled(context, VoskModelSpec.ENGLISH);
                addStatus(own, own ? R.string.home_status_english_own_ready : R.string.home_status_english_own_setup);
                break;
            default:
                addStatus(true, R.string.home_status_english_ready);
        }
    }

    private void addStatus(boolean ready, @StringRes int text) {
        addStatus(ready, getString(text));
    }

    private void addStatus(boolean ready, String text) {
        TextView row = new TextView(requireContext());
        row.setText(text);
        row.setTextAppearance(R.style.TextAppearance_AGAP_Body);
        row.setMinHeight(Math.round(44 * getResources().getDisplayMetrics().density));
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setCompoundDrawablePadding(Math.round(12 * getResources().getDisplayMetrics().density));
        row.setCompoundDrawablesRelativeWithIntrinsicBounds(ready ? R.drawable.ic_check : R.drawable.ic_info, 0, 0, 0);
        TextViewCompat.setCompoundDrawableTintList(row, ColorStateList.valueOf(accent()));
        row.setPadding(0, Math.round(4 * getResources().getDisplayMetrics().density), 0,
                Math.round(4 * getResources().getDisplayMetrics().density));
        statusRows.addView(row);
    }

    private int accent() {
        TypedValue value = new TypedValue();
        requireContext().getTheme().resolveAttribute(R.attr.agapAccent, value, true);
        return value.data;
    }

    private void open(Class<?> screen) {
        startActivity(new Intent(requireContext(), screen));
    }

    private static int greetingForNow() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 12) return R.string.home_greeting_morning;
        if (hour >= 12 && hour < 18) return R.string.home_greeting_afternoon;
        return R.string.home_greeting_evening;
    }
}

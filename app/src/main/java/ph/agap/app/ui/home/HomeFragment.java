package ph.agap.app.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Calendar;

import ph.agap.app.R;
import ph.agap.app.ui.MainActivity;
import ph.agap.app.ui.info.DemoModeActivity;
import ph.agap.app.ui.info.SoundCategoriesActivity;
import ph.agap.app.ui.info.VibrationPatternsActivity;
import ph.agap.app.ui.settings.SettingsActivity;

public class HomeFragment extends Fragment {

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

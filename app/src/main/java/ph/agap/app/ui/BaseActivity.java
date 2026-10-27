package ph.agap.app.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.appbar.MaterialToolbar;

import ph.agap.app.R;
import ph.agap.app.settings.SettingsStore;

/**
 * Applies the user's text size and display mode to every screen, and handles the system bars.
 * A screen that was built with an older look rebuilds itself when it comes back into view.
 */
public abstract class BaseActivity extends AppCompatActivity {

    protected SettingsStore settings;
    private String appliedLook;

    @Override
    protected void attachBaseContext(Context base) {
        // The app's text size multiplies the phone's own font size, so both settings count.
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.fontScale = config.fontScale * SettingsStore.from(base).getTextScale();
        super.attachBaseContext(base.createConfigurationContext(config));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        settings = SettingsStore.from(this);
        setTheme(settings.isHighContrast() ? R.style.Theme_AGAP_HighContrast : R.style.Theme_AGAP);
        appliedLook = settings.lookKey();
        super.onCreate(savedInstanceState);

        // Draw behind the system bars on every Android version, as Android 15 does anyway,
        // and pad the screen ourselves (see setUpScreen).
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat bars =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        bars.setAppearanceLightStatusBars(false);
        bars.setAppearanceLightNavigationBars(!settings.isHighContrast());
    }

    @Override
    protected void onResume() {
        super.onResume();
        rebuildIfLookChanged();
    }

    /** Rebuilds this screen if the text size or display mode changed since it was built. */
    protected void rebuildIfLookChanged() {
        if (!appliedLook.equals(settings.lookKey())) {
            recreate();
        }
    }

    /**
     * Sets up the top bar and the padding for the status and navigation bars.
     * Call after setContentView. The layout needs a view with id "root" and the top_bar include.
     *
     * @param showUp      show a Back arrow in the top bar
     * @param padBottom   pad the bottom for the navigation bar (false when a bottom navigation
     *                    view does it itself)
     */
    protected MaterialToolbar setUpScreen(boolean showUp, boolean padBottom) {
        MaterialToolbar bar = findViewById(R.id.top_bar);
        View holder = findViewById(R.id.top_bar_holder);
        View root = findViewById(R.id.root);

        bar.setTitle(getTitle());
        if (showUp) {
            bar.setNavigationIcon(R.drawable.ic_arrow_back);
            bar.setNavigationContentDescription(R.string.navigate_up);
            bar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        }

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sys = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            holder.setPadding(0, sys.top, 0, 0);
            v.setPadding(sys.left, 0, sys.right, padBottom ? sys.bottom : 0);
            return insets;
        });
        return bar;
    }
}

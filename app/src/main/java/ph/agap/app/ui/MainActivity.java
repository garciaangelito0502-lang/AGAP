package ph.agap.app.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import ph.agap.app.R;
import ph.agap.app.captions.VoskModelManager;
import ph.agap.app.ui.alerts.AlertsFragment;
import ph.agap.app.ui.captions.CaptionsFragment;
import ph.agap.app.ui.home.HomeFragment;
import ph.agap.app.ui.settings.SettingsActivity;

/**
 * Home and the three main tabs (Captions, Alerts, History). Only one tab's screen exists at a
 * time, which later keeps Live Captions and Sound Alerts from holding the microphone together.
 */
public class MainActivity extends BaseActivity {

    private static final String STATE_TAB = "tab";

    private BottomNavigationView nav;
    private MaterialToolbar bar;
    @IdRes private int currentTab;

    // Back from a tab other than Home goes to Home first.
    private final OnBackPressedCallback backToHome = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            nav.setSelectedItemId(R.id.nav_home);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bar = setUpScreen(false, false);
        bar.inflateMenu(R.menu.top_bar_main);
        bar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            return false;
        });

        nav = findViewById(R.id.bottom_nav);
        nav.setOnItemSelectedListener(item -> {
            showTab(item.getItemId());
            return true;
        });
        nav.setOnItemReselectedListener(item -> { });
        getOnBackPressedDispatcher().addCallback(this, backToHome);

        if (savedInstanceState == null) {
            showTab(R.id.nav_home);
        } else {
            // The fragment manager has already restored the tab's screen.
            currentTab = savedInstanceState.getInt(STATE_TAB, R.id.nav_home);
            updateChrome();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_TAB, currentTab);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        // Once AGAP is no longer on screen, give back the Filipino model's memory (about 1 GB).
        if (level >= TRIM_MEMORY_UI_HIDDEN) {
            VoskModelManager.releaseMemory();
        }
    }

    /** Switches to a tab as if its button had been pressed (used by the Home cards). */
    public void selectTab(@IdRes int tabId) {
        nav.setSelectedItemId(tabId);
    }

    private void showTab(@IdRes int tabId) {
        if (tabId == currentTab) {
            return;
        }
        currentTab = tabId;
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, createScreen(tabId))
                .commit();
        updateChrome();
    }

    private void updateChrome() {
        bar.setTitle(titleFor(currentTab));
        backToHome.setEnabled(currentTab != R.id.nav_home);
    }

    private Fragment createScreen(@IdRes int tabId) {
        if (tabId == R.id.nav_captions) {
            return new CaptionsFragment();
        } else if (tabId == R.id.nav_alerts) {
            return new AlertsFragment();
        } else if (tabId == R.id.nav_history) {
            return PlaceholderFragment.newInstance(
                    R.string.history_title, R.string.placeholder_history, R.drawable.ic_history);
        }
        return new HomeFragment();
    }

    private int titleFor(@IdRes int tabId) {
        if (tabId == R.id.nav_captions) return R.string.captions_title;
        if (tabId == R.id.nav_alerts) return R.string.alerts_title;
        if (tabId == R.id.nav_history) return R.string.history_title;
        return R.string.app_name;
    }
}

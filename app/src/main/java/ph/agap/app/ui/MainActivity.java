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
import ph.agap.app.ui.history.HistoryFragment;
import ph.agap.app.ui.home.HomeFragment;
import ph.agap.app.ui.settings.SettingsActivity;

/**
 * Home and the three main tabs (Captions, Alerts, History). Only one tab's screen exists at a
 * time, which later keeps Live Captions and Sound Alerts from holding the microphone together.
 */
public class MainActivity extends BaseActivity {

    private static final String STATE_TAB = "tab";

    /** Intent extra: which tab to open (TAB_CAPTIONS), used by Demonstration Mode's sample captions. */
    public static final String EXTRA_TAB = "tab";
    public static final String TAB_CAPTIONS = "captions";

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
            if (item.getItemId() == R.id.action_clear_history) {
                Fragment screen = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (screen instanceof HistoryFragment) ((HistoryFragment) screen).confirmClear();
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
            openRequestedTab(getIntent());
        } else {
            // The fragment manager has already restored the tab's screen.
            currentTab = savedInstanceState.getInt(STATE_TAB, R.id.nav_home);
            updateChrome();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        openRequestedTab(intent);
    }

    private void openRequestedTab(@Nullable Intent intent) {
        if (intent != null && TAB_CAPTIONS.equals(intent.getStringExtra(EXTRA_TAB))) {
            nav.setSelectedItemId(R.id.nav_captions);
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
        bar.getMenu().findItem(R.id.action_clear_history).setVisible(currentTab == R.id.nav_history);
        backToHome.setEnabled(currentTab != R.id.nav_home);
    }

    private Fragment createScreen(@IdRes int tabId) {
        if (tabId == R.id.nav_captions) {
            return new CaptionsFragment();
        } else if (tabId == R.id.nav_alerts) {
            return new AlertsFragment();
        } else if (tabId == R.id.nav_history) {
            return new HistoryFragment();
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

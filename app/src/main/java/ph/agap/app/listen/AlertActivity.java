package ph.agap.app.listen;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import ph.agap.app.history.ShownVia;
import ph.agap.app.settings.SettingsStore;
import ph.agap.app.ui.MainActivity;
import ph.agap.app.ui.alerts.AlertOverlayFragment;

/**
 * The full-screen alert when AGAP is not in view (Phase 11): over the lock screen when the screen is
 * off or locked (opened by the alert's full-screen notification, which also turns the screen on),
 * or when the user taps an alert notification. It shows the same alert screen as the app
 * (AlertOverlayFragment) and closes with it. The keyguard stays: the phone is not unlocked.
 */
public class AlertActivity extends AppCompatActivity implements AppVisibility.AlertHost {

    private static final String EXTRA_ID = "ph.agap.app.alert_id";

    static Intent intent(Context context, long ledgerId) {
        return new Intent(context, AlertActivity.class)
                .putExtra(EXTRA_ID, ledgerId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    @Override
    protected void attachBaseContext(Context base) {
        // The user's text size, as on every AGAP screen (BaseActivity).
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.fontScale = config.fontScale * SettingsStore.from(base).getTextScale();
        super.attachBaseContext(base.createConfigurationContext(config));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setShowWhenLocked(true);
        setTurnScreenOn(true);
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            // Rebuilt by Android: the alert it showed was saved already.
            finish();
            return;
        }
        show(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        show(intent);
    }

    private void show(Intent intent) {
        long id = intent.getLongExtra(EXTRA_ID, 0);
        AlertLedger.Entry entry = AlertLedger.get(id);
        if (entry == null) {
            // AGAP was restarted since this alert: it is in the Event History.
            startActivity(new Intent(this, MainActivity.class)
                    .putExtra(MainActivity.EXTRA_TAB, MainActivity.TAB_HISTORY)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            finish();
            return;
        }
        ShownVia via = AlertPresenter.isLocked(this) ? ShownVia.LOCK_SCREEN : ShownVia.APP_SCREEN;
        AlertOverlayFragment.show(this, entry.event, entry.thresholdPercent, false, via, id);
    }
}

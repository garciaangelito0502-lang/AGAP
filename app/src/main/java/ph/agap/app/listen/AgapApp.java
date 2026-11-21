package ph.agap.app.listen;

import android.app.Application;

import ph.agap.app.demo.DemoMode;
import ph.agap.app.history.HistoryRepository;

/**
 * Sets up what background listening needs once per app start (Phase 11): which AGAP screens are in
 * view, the notification channels, and the links between the listening sessions and the floating
 * window, popup and widget.
 */
public final class AgapApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppVisibility.install(this);
        AgapNotifications.createChannels(this);

        AppVisibility.addListener(() -> {
            // With AGAP back in view its own screens show the captions and alerts.
            FloatingCaptions.update(this);
            if (AppVisibility.isAppVisible()) PopupAlert.hide(this);
        });

        // Demonstration Mode never runs next to real listening (contract 1.9): turning it on stops both.
        DemoMode.addListener(() -> {
            if (!DemoMode.isOn()) return;
            AlertsSession.get(this).stop();
            if (CaptionsSession.get(this).isListeningForReal()) CaptionsSession.get(this).stop();
        });

        HistoryRepository.get(this).addListener(() -> AgapWidget.refresh(this));
    }
}

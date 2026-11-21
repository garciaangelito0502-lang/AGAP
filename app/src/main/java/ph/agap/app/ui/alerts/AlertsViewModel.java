package ph.agap.app.ui.alerts;

import androidx.lifecycle.ViewModel;

/**
 * What the Sound Alerts screen remembers while the app is open. The listening session itself, its
 * last alert and its notes are AlertsSession's (Phase 11). Memory only.
 */
public class AlertsViewModel extends ViewModel {

    /** Set once the microphone permission has been asked for, to tell "never asked" from "blocked". */
    boolean askedForMic;
}

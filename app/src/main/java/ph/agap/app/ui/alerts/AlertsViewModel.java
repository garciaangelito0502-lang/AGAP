package ph.agap.app.ui.alerts;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;

import java.io.IOException;
import java.io.InputStream;

import ph.agap.app.alerts.AlertEvent;
import ph.agap.app.alerts.AnnexA;

/** Keeps the Annex A list and the last alert while the app is open. Memory only. */
public class AlertsViewModel extends AndroidViewModel {

    @Nullable private AnnexA annex;
    @Nullable AlertEvent lastEvent;
    int sessionCount;
    boolean askedForMic;
    /** Says that the other listening feature was stopped, or stopped this one (MicOwner). */
    @Nullable String micNote;
    /** Why the last session stopped by itself, shown until the next start. */
    @Nullable String failure;
    /** Android is giving the microphone to a call or another app: AGAP hears silence (Phase 8). */
    boolean micSilenced;

    public AlertsViewModel(@NonNull Application application) {
        super(application);
    }

    /** The Annex A alert list from the app's assets, read once. */
    AnnexA annex() {
        if (annex == null) {
            try (InputStream in = getApplication().getAssets().open(AnnexA.ASSET)) {
                annex = AnnexA.parse(in);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read " + AnnexA.ASSET, e);
            }
        }
        return annex;
    }
}

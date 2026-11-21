package ph.agap.app.ui.captions;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;

import ph.agap.app.captions.GoogleOnDeviceSupport;
import ph.agap.app.captions.VoskModelManager;

/**
 * What the Live Captions screen remembers while the app is open: the chosen language and what the
 * phone can do for it. The session and its transcript are CaptionsSession's (Phase 11).
 * Memory only: it is gone when the app closes (contract 1.2).
 */
public class CaptionsViewModel extends ViewModel {

    /** The two caption languages. One is used at a time (contract 1.3). */
    enum Language { ENGLISH, FILIPINO }

    /** The language chosen for the next session. Cannot change while listening. */
    Language language = Language.ENGLISH;

    /** The last answer about English on-device support; null until checked. */
    /** Shared with Home, which shows whether English captions are ready. */
    @Nullable public GoogleOnDeviceSupport.Result englishSupport;

    /** The Filipino model on this phone, and the English fallback model; created when first needed. */
    @Nullable VoskModelManager filipino;
    @Nullable VoskModelManager englishFallback;

    /** Set once the microphone permission has been asked for, to tell "never asked" from "blocked". */
    boolean askedForMic;

    @Override
    protected void onCleared() {
        if (filipino != null) filipino.shutdown();
        if (englishFallback != null) englishFallback.shutdown();
    }
}

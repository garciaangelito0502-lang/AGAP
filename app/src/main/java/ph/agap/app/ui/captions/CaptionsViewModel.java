package ph.agap.app.ui.captions;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;

import ph.agap.app.captions.CaptionListener;
import ph.agap.app.captions.GoogleOnDeviceSupport;
import ph.agap.app.captions.Transcript;
import ph.agap.app.captions.VoskModelManager;

/**
 * Keeps the caption text while the app is open, so it is still there after a tab switch.
 * Memory only: it is gone when the app closes (contract 1.2).
 */
public class CaptionsViewModel extends ViewModel {

    /** The two caption languages. One is used at a time (contract 1.3). */
    enum Language { ENGLISH, FILIPINO }

    final Transcript transcript = new Transcript();

    /** The language chosen for the next session. Cannot change while listening. */
    Language language = Language.ENGLISH;

    /** The last answer about English on-device support; null until checked. */
    /** Shared with Home, which shows whether English captions are ready. */
    @Nullable public GoogleOnDeviceSupport.Result englishSupport;

    /** The Filipino model on this phone, and the English fallback model; created when first needed. */
    @Nullable VoskModelManager filipino;
    @Nullable VoskModelManager englishFallback;

    /** Why the last session stopped by itself, shown until the next start. */
    @Nullable CaptionListener.Failure lastFailure;

    /** Set once the microphone permission has been asked for, to tell "never asked" from "blocked". */
    boolean askedForMic;

    /** Says that the other listening feature was stopped, or stopped this one (MicOwner). */
    @Nullable String micNote;

    /** Android is giving the microphone to a call or another app: AGAP hears silence (Phase 8). */
    boolean micSilenced;

    @Override
    protected void onCleared() {
        if (filipino != null) filipino.shutdown();
        if (englishFallback != null) englishFallback.shutdown();
    }
}

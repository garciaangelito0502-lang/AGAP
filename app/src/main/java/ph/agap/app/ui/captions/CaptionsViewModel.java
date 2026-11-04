package ph.agap.app.ui.captions;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;

import ph.agap.app.captions.CaptionListener;
import ph.agap.app.captions.GoogleOnDeviceSupport;
import ph.agap.app.captions.Transcript;

/**
 * Keeps the caption text while the app is open, so it is still there after a tab switch.
 * Memory only: it is gone when the app closes (contract 1.2).
 */
public class CaptionsViewModel extends ViewModel {

    final Transcript transcript = new Transcript();

    /** The last answer about English on-device support; null until checked. */
    @Nullable GoogleOnDeviceSupport.Result englishSupport;

    /** Why the last session stopped by itself, shown until the next start. */
    @Nullable CaptionListener.Failure lastFailure;

    /** Set once the microphone permission has been asked for, to tell "never asked" from "blocked". */
    boolean askedForMic;
}

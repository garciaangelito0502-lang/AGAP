package ph.agap.app.ui.captions;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.UnderlineSpan;
import android.util.TypedValue;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import ph.agap.app.R;
import ph.agap.app.ui.SlowPulse;
import ph.agap.app.captions.CaptionEngine;
import ph.agap.app.captions.CaptionListener.Failure;
import ph.agap.app.captions.DemoCaptionEngine;
import ph.agap.app.captions.GoogleOnDeviceEngine;
import ph.agap.app.captions.GoogleOnDeviceSupport;
import ph.agap.app.captions.Transcript;
import ph.agap.app.captions.VoskCaptionEngine;
import ph.agap.app.captions.VoskModelManager;
import ph.agap.app.captions.VoskModelSpec;
import ph.agap.app.demo.DemoMode;
import ph.agap.app.listen.CaptionsSession;
import ph.agap.app.settings.SettingsStore;
import ph.agap.app.ui.captions.CaptionsViewModel.Language;

/**
 * Live Captions. The user picks a language and taps Start; the words appear as they are heard.
 * The session itself (engine and transcript) is CaptionsSession's (Phase 11). With background
 * listening off (the default) it stops when this screen leaves the display, as in 1.0 (contract
 * 1.10); with it on, it goes on until Stop and the words also float over other apps. The screen
 * stays on while listening here. The text stays after Stop until Clear or the next Start, so the
 * research team can compare it with their script.
 */
public class CaptionsFragment extends Fragment {

    private static final String ENGLISH = "en-US";

    private CaptionsViewModel model;
    private CaptionsSession session;
    /** The last session failure this screen has acted on. */
    @Nullable private Failure seenFailure;

    private MaterialButtonToggleGroup languageGroup;
    private MaterialButton englishButton;
    private MaterialButton filipinoButton;
    private TextView languageNote;
    private View statusDot;
    private TextView statusText;
    private View messageCard;
    private TextView messageText;
    private LinearProgressIndicator messageProgress;
    private MaterialButton messageAction;
    private ScrollView captionScroll;
    private TextView captionText;
    private View pausedNote;
    private View backgroundNote;
    private MaterialButton clearButton;
    private MaterialButton startStop;
    private SlowPulse pulse;

    private final ActivityResultLauncher<String> askMic = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                model.askedForMic = true;
                refresh();
                checkLanguage();
            });

    private final Runnable sessionChanged = () -> {
        noticeFailure();
        if (getView() != null) {
            renderTranscript();
            refresh();
        }
    };

    public CaptionsFragment() {
        super(R.layout.fragment_captions);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        model = new ViewModelProvider(requireActivity()).get(CaptionsViewModel.class);
        session = CaptionsSession.get(requireContext());
        // A session that went on in the background keeps its language (AGAP may have been rebuilt).
        if (session.isRunning()) model.language = session.isFilipino() ? Language.FILIPINO : Language.ENGLISH;

        languageGroup = view.findViewById(R.id.language_group);
        englishButton = view.findViewById(R.id.lang_english);
        filipinoButton = view.findViewById(R.id.lang_filipino);
        languageNote = view.findViewById(R.id.language_note);
        statusDot = view.findViewById(R.id.status_dot);
        statusText = view.findViewById(R.id.status_text);
        messageCard = view.findViewById(R.id.message_card);
        messageText = view.findViewById(R.id.message_text);
        messageProgress = view.findViewById(R.id.message_progress);
        messageAction = view.findViewById(R.id.message_action);
        captionScroll = view.findViewById(R.id.caption_scroll);
        captionText = view.findViewById(R.id.caption_text);
        pausedNote = view.findViewById(R.id.alerts_paused_note);
        backgroundNote = view.findViewById(R.id.background_note);
        clearButton = view.findViewById(R.id.clear_button);
        startStop = view.findViewById(R.id.start_stop);

        // The check mark is the non-colour cue for the chosen language.
        languageGroup.check(model.language == Language.FILIPINO ? R.id.lang_filipino : R.id.lang_english);
        showChosenLanguage();
        languageGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            Language chosen = checkedId == R.id.lang_filipino ? Language.FILIPINO : Language.ENGLISH;
            if (chosen == model.language) return;
            // The buttons are disabled while listening, so this is only reached between sessions.
            model.language = chosen;
            session.clearFailure();
            session.clearMicNote();
            // The Filipino model needs about 1 GB of memory: free it when it is no longer wanted.
            if (chosen == Language.ENGLISH) VoskModelManager.releaseMemory();
            showChosenLanguage();
            refresh();
            checkLanguage();
        });

        pulse = SlowPulse.alpha(statusDot, 0.3f, 1000);

        startStop.setOnClickListener(v -> {
            if (session.isRunning()) session.stop(); else startListening();
        });
        clearButton.setOnClickListener(v -> {
            session.clear();
            renderTranscript();
            refresh();
        });

        renderTranscript();
    }

    @Override
    public void onStart() {
        super.onStart();
        VoskModelManager.Listener redraw = () -> {
            if (getView() != null) refresh();
        };
        manager(VoskModelSpec.FILIPINO).setListener(redraw);
        manager(VoskModelSpec.ENGLISH).setListener(redraw);
        session.addListener(sessionChanged);
        noticeFailure();
        checkLanguage();
        renderTranscript();
        refresh();
    }

    @Override
    public void onStop() {
        session.removeListener(sessionChanged);
        // Listening only while this screen is on display (contract 1.10), unless background
        // listening is on (Phase 11): then ListeningService keeps it going. Demonstration Mode's
        // sample captions never go on in the background.
        if (!keepsListeningWhenHidden()) session.stop();
        manager(VoskModelSpec.FILIPINO).setListener(null);
        manager(VoskModelSpec.ENGLISH).setListener(null);
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        pulse.stop();
        super.onDestroyView();
    }

    private boolean keepsListeningWhenHidden() {
        return SettingsStore.from(requireContext()).isBackgroundListeningOn()
                && !DemoMode.isOn() && !session.isDemo();
    }

    /** The English pack went missing during a session: look at English again (once per failure). */
    private void noticeFailure() {
        Failure failure = session.lastFailure();
        if (failure == seenFailure) return;
        seenFailure = failure;
        if (failure == Failure.LANGUAGE_PACK) {
            model.englishSupport = null;
            if (getView() != null) checkEnglish();
        }
    }

    // ---- Starting ----

    private boolean hasMic() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** The manager for one bundled Vosk model, created when first needed. */
    private VoskModelManager manager(VoskModelSpec spec) {
        if (spec == VoskModelSpec.FILIPINO) {
            if (model.filipino == null) model.filipino = new VoskModelManager(requireContext(), spec);
            return model.filipino;
        }
        if (model.englishFallback == null) model.englishFallback = new VoskModelManager(requireContext(), spec);
        return model.englishFallback;
    }

    /** The manager for the language in use (only meaningful when {@link #usesVosk()}). */
    private VoskModelManager voskManager() {
        return manager(isFilipino() ? VoskModelSpec.FILIPINO : VoskModelSpec.ENGLISH);
    }

    /**
     * English runs on AGAP's own Vosk model when Google's offline English cannot be used on this phone:
     * Android below 13, no Google speech service, or English not supported (contract 1.3, D-014).
     * A pack that merely needs downloading is not a reason: that goes through Google's one-time download.
     */
    private boolean englishFallback() {
        if (isFilipino()) return false;
        if (debugForcesVoskEnglish()) return true;
        GoogleOnDeviceSupport.Result r = model.englishSupport;
        return r != null && (r.status == GoogleOnDeviceSupport.Status.OLD_ANDROID
                || r.status == GoogleOnDeviceSupport.Status.NO_RECOGNIZER
                || r.status == GoogleOnDeviceSupport.Status.LANGUAGE_NOT_SUPPORTED);
    }

    /** Filipino always, English only on the fallback: the two cases that use a Vosk model. */
    private boolean usesVosk() {
        return isFilipino() || englishFallback();
    }

    /** Debug builds only: the boolean "force_vosk_english" in preferences "agap_debug" tests the fallback. */
    private boolean debugForcesVoskEnglish() {
        Context context = requireContext();
        boolean debuggable = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        return debuggable && context.getSharedPreferences("agap_debug", Context.MODE_PRIVATE)
                .getBoolean("force_vosk_english", false);
    }

    private boolean isFilipino() {
        return model.language == Language.FILIPINO;
    }

    private String languageName() {
        return getString(isFilipino() ? R.string.captions_lang_filipino : R.string.captions_lang_english);
    }

    /** Puts the check mark on the chosen language's button only. */
    private void showChosenLanguage() {
        englishButton.setIcon(isFilipino() ? null
                : ContextCompat.getDrawable(requireContext(), R.drawable.ic_check));
        filipinoButton.setIcon(isFilipino()
                ? ContextCompat.getDrawable(requireContext(), R.drawable.ic_check) : null);
    }

    /** Looks at what the chosen language needs (English pack, or a Vosk model on disk). */
    private void checkLanguage() {
        if (isFilipino()) {
            manager(VoskModelSpec.FILIPINO).refresh();
        } else {
            checkEnglish();
            if (englishFallback()) manager(VoskModelSpec.ENGLISH).refresh();
        }
    }

    /** Asks the recognizer about English, unless the answer is already known to be final. */
    private void checkEnglish() {
        if (!hasMic() || session.isRunning()) return;
        GoogleOnDeviceSupport.Result known = model.englishSupport;
        if (known != null && known.status != GoogleOnDeviceSupport.Status.NEEDS_DOWNLOAD
                && known.status != GoogleOnDeviceSupport.Status.DOWNLOADING
                && known.status != GoogleOnDeviceSupport.Status.UNKNOWN) {
            return;
        }
        GoogleOnDeviceSupport.check(requireContext(), ENGLISH, result -> {
            model.englishSupport = result;
            if (englishFallback()) manager(VoskModelSpec.ENGLISH).refresh();
            if (getView() != null) refresh();
        });
    }

    private boolean canStart() {
        // Demonstration Mode plays a sample script: no microphone, permission or model needed.
        if (DemoMode.isOn()) return true;
        if (usesVosk()) {
            return hasMic() && voskManager().state() == VoskModelManager.State.INSTALLED;
        }
        GoogleOnDeviceSupport.Result support = model.englishSupport;
        return hasMic() && support != null
                && (support.status == GoogleOnDeviceSupport.Status.READY
                || support.status == GoogleOnDeviceSupport.Status.UNKNOWN);
    }

    private void startListening() {
        if (session.isRunning() || !canStart()) return;
        boolean demo = DemoMode.isOn();
        // Google's English engine is only possible on Android 13+, but lint cannot see canStart()'s check.
        if (!demo && !usesVosk() && Build.VERSION.SDK_INT < 33) return;
        // Two separate engines, never mixed in one session (contract 1.3).
        CaptionEngine engine;
        if (demo) {
            // Sample captions only; the microphone is never opened (contract 1.9).
            engine = new DemoCaptionEngine(isFilipino());
        } else if (usesVosk()) {
            VoskModelManager vosk = voskManager();
            engine = new VoskCaptionEngine(vosk.modelDir(), vosk.spec().languageTag);
        } else {
            engine = new GoogleOnDeviceEngine(requireContext(), model.englishSupport.languageTag,
                    model.englishSupport.service);
        }
        // A new session starts with an empty page.
        session.start(engine, demo, isFilipino(), languageName());
    }

    // ---- Drawing ----

    private void renderTranscript() {
        Transcript t = session.transcript();
        SpannableStringBuilder text = new SpannableStringBuilder();
        for (String line : t.lines()) {
            newParagraph(text);
            text.append(line);
        }
        if (!t.partial().isEmpty()) {
            newParagraph(text);
            int start = text.length();
            text.append(t.partial());
            // Words that may still change: lighter and underlined, so colour is not the only cue.
            text.setSpan(new ForegroundColorSpan(themeColor(R.attr.agapTextSecondary)),
                    start, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            text.setSpan(new UnderlineSpan(), start, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        boolean atBottom = !captionScroll.canScrollVertically(1);
        captionText.setText(text);
        if (atBottom) {
            captionScroll.post(() -> captionScroll.scrollTo(0, captionText.getBottom()));
        }
        clearButton.setVisibility(!session.isRunning() && !t.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /** Starts a new line after a half-line gap, so each sentence reads as its own paragraph. */
    private static void newParagraph(SpannableStringBuilder text) {
        if (text.length() == 0) return;
        text.append('\n');
        int gap = text.length();
        text.append('\n');
        text.setSpan(new RelativeSizeSpan(0.4f), gap, gap + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    /** Sets the status line, the message card and the buttons from the current state. */
    private void refresh() {
        boolean listening = session.isRunning();
        boolean unpacking = usesVosk() && voskManager().state() == VoskModelManager.State.UNPACKING;
        // Say so on screen when English runs on AGAP's own model instead of Google's.
        languageNote.setText(englishFallback() ? R.string.captions_en_fallback : R.string.captions_language_note);
        View root = getView();
        // The screen stays on while listening, and while the Filipino model unpacks (a few minutes).
        if (root != null) root.setKeepScreenOn(listening || unpacking);

        // The language is chosen before Start and cannot change during a session.
        englishButton.setEnabled(!listening && !unpacking);
        filipinoButton.setEnabled(!listening && !unpacking);

        pausedNote.setVisibility(listening ? View.VISIBLE : View.GONE);
        backgroundNote.setVisibility(listening && keepsListeningWhenHidden() ? View.VISIBLE : View.GONE);
        clearButton.setVisibility(!listening && !session.transcript().isEmpty() ? View.VISIBLE : View.GONE);

        statusDot.setVisibility(listening ? View.VISIBLE : View.GONE);
        if (listening) {
            int status = DemoMode.isOn() ? R.string.captions_status_sample
                    : session.heardReady() ? R.string.captions_status_listening : R.string.captions_status_starting;
            statusText.setText(getString(status, languageName()));
            statusText.setTextColor(themeColor(R.attr.agapAccent));
            if (!pulse.isStarted()) pulse.start();
        } else {
            statusText.setText(getString(R.string.captions_status_idle, languageName()));
            statusText.setTextColor(themeColor(R.attr.agapTextSecondary));
            pulse.stop();
        }

        startStop.setText(listening ? R.string.captions_stop : R.string.captions_start);
        startStop.setIconResource(listening ? R.drawable.ic_stop : R.drawable.ic_mic);
        startStop.setEnabled(listening || canStart());

        showMessage();
    }

    private void showMessage() {
        if (DemoMode.isOn()) {
            if (!session.isRunning()) message(getString(R.string.captions_demo_note)); else hideMessage();
        } else if (session.micNote() != null) {
            message(session.micNote());
        } else if (session.isRunning()) {
            if (session.micSilenced()) {
                message(getString(R.string.mic_silenced), 0, null);
            } else if (usesVosk() && !session.heardReady()) {
                message(getString(R.string.captions_vosk_loading, languageName()), 0, null);
            } else {
                hideMessage();
            }
        } else if (!hasMic()) {
            boolean blocked = model.askedForMic
                    && !shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO);
            if (blocked) {
                message(R.string.captions_mic_blocked, R.string.captions_open_settings, v -> openAppSettings());
            } else {
                message(R.string.captions_mic_explain, R.string.captions_allow_mic,
                        v -> askMic.launch(Manifest.permission.RECORD_AUDIO));
            }
        } else if (isFilipino()) {
            showVoskMessage();
        } else if (model.englishSupport == null && !debugForcesVoskEnglish()) {
            message(R.string.captions_checking, 0, null);
        } else if (englishFallback()) {
            showVoskMessage();
        } else if (session.lastFailure() == Failure.RECOGNIZER) {
            message(R.string.captions_failed, 0, null);
        } else {
            switch (model.englishSupport.status) {
                case NEEDS_DOWNLOAD:
                    message(R.string.captions_pack_missing, R.string.captions_download_pack, v -> {
                        GoogleOnDeviceSupport.requestDownload(requireContext(), model.englishSupport);
                        model.englishSupport = null;
                        message(R.string.captions_pack_downloading, R.string.captions_check_again,
                                w -> checkEnglish());
                    });
                    break;
                case DOWNLOADING:
                    message(R.string.captions_pack_downloading, R.string.captions_check_again,
                            v -> checkEnglish());
                    break;
                case OLD_ANDROID:
                    message(R.string.captions_old_android, 0, null);
                    break;
                case NO_RECOGNIZER:
                case LANGUAGE_NOT_SUPPORTED:
                    message(R.string.captions_no_recognizer, 0, null);
                    break;
                default:
                    hideMessage();
            }
        }
    }

    /** A Vosk language (Filipino, or the English fallback): model not set up, unpacking, low storage, failed. */
    private void showVoskMessage() {
        VoskModelManager vosk = voskManager();
        String language = languageName();
        switch (vosk.state()) {
            case NOT_INSTALLED:
                message(getString(R.string.captions_vosk_setup, language,
                                storageText(vosk.spec().unpackedBytes)),
                        R.string.captions_setup_action, v -> vosk.startUnpack());
                break;
            case UNPACKING:
                messageCard.setVisibility(View.VISIBLE);
                messageText.setText(getString(R.string.captions_vosk_unpacking, language, vosk.percent()));
                messageProgress.setVisibility(View.VISIBLE);
                messageProgress.setProgressCompat(vosk.percent(), true);
                messageAction.setVisibility(View.GONE);
                break;
            case LOW_STORAGE:
                message(getString(R.string.captions_vosk_low_storage, language,
                                storageText(vosk.spec().requiredFreeBytes),
                                storageText(vosk.freeBytes())),
                        R.string.captions_check_again, v -> {
                            vosk.refresh();
                            refresh();
                        });
                break;
            case FAILED:
                message(getString(R.string.captions_vosk_failed, language), R.string.captions_setup_action,
                        v -> {
                            vosk.refresh();
                            vosk.startUnpack();
                        });
                break;
            case INSTALLED:
                if (session.lastFailure() == Failure.MODEL) {
                    message(getString(R.string.captions_vosk_model_failed, language),
                            R.string.captions_setup_again_action, v -> {
                                session.clearFailure();
                                vosk.discard();
                                refresh();
                            });
                } else if (session.lastFailure() == Failure.RECOGNIZER) {
                    message(R.string.captions_failed, 0, null);
                } else {
                    hideMessage();
                }
                break;
            default:
                message(R.string.captions_checking, 0, null);
        }
    }

    private String storageText(long bytes) {
        if (bytes >= 1_000_000_000L) {
            return getString(R.string.captions_storage_gb, bytes / 1e9);
        }
        return getString(R.string.captions_storage_mb, bytes / 1_000_000L);
    }

    private void message(@StringRes int text, @StringRes int action, @Nullable View.OnClickListener onAction) {
        message(getString(text), action, onAction);
    }

    private void message(CharSequence text, @StringRes int action, @Nullable View.OnClickListener onAction) {
        messageCard.setVisibility(View.VISIBLE);
        messageText.setText(text);
        messageProgress.setVisibility(View.GONE);
        if (action != 0) {
            messageAction.setVisibility(View.VISIBLE);
            messageAction.setText(action);
            messageAction.setOnClickListener(onAction);
        } else {
            messageAction.setVisibility(View.GONE);
        }
    }

    private void message(CharSequence text) {
        message(text, 0, null);
    }

    private void hideMessage() {
        messageCard.setVisibility(View.GONE);
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", requireContext().getPackageName(), null));
        startActivity(intent);
    }

    private int themeColor(int attr) {
        TypedValue value = new TypedValue();
        requireContext().getTheme().resolveAttribute(attr, value, true);
        return value.data;
    }
}

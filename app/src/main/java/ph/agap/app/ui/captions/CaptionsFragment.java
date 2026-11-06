package ph.agap.app.ui.captions;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
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

import ph.agap.app.R;
import ph.agap.app.captions.CaptionEngine;
import ph.agap.app.captions.CaptionListener;
import ph.agap.app.captions.CaptionRelay;
import ph.agap.app.captions.GoogleOnDeviceEngine;
import ph.agap.app.captions.GoogleOnDeviceSupport;
import ph.agap.app.captions.Transcript;
import ph.agap.app.mic.MicOwner;

/**
 * Live Captions. The user picks a language and taps Start; the words appear as they are heard.
 * Listening stops when this screen leaves the display, and the screen stays on while listening
 * (contract 1.10). The text stays after Stop until Clear or the next Start, so the research
 * team can compare it with their script.
 */
public class CaptionsFragment extends Fragment implements CaptionListener, MicOwner.User {

    private static final String ENGLISH = "en-US";

    private CaptionsViewModel model;
    @Nullable private CaptionEngine engine;
    private boolean heardReady;

    private MaterialButton englishButton;
    private View statusDot;
    private TextView statusText;
    private View messageCard;
    private TextView messageText;
    private MaterialButton messageAction;
    private ScrollView captionScroll;
    private TextView captionText;
    private View pausedNote;
    private MaterialButton clearButton;
    private MaterialButton startStop;
    private ValueAnimator pulse;

    private final ActivityResultLauncher<String> askMic = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                model.askedForMic = true;
                refresh();
                checkEnglish();
            });

    public CaptionsFragment() {
        super(R.layout.fragment_captions);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        model = new ViewModelProvider(requireActivity()).get(CaptionsViewModel.class);

        englishButton = view.findViewById(R.id.lang_english);
        statusDot = view.findViewById(R.id.status_dot);
        statusText = view.findViewById(R.id.status_text);
        messageCard = view.findViewById(R.id.message_card);
        messageText = view.findViewById(R.id.message_text);
        messageAction = view.findViewById(R.id.message_action);
        captionScroll = view.findViewById(R.id.caption_scroll);
        captionText = view.findViewById(R.id.caption_text);
        pausedNote = view.findViewById(R.id.alerts_paused_note);
        clearButton = view.findViewById(R.id.clear_button);
        startStop = view.findViewById(R.id.start_stop);

        // Only English exists in this build; the check mark is the non-colour cue for the choice.
        englishButton.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_check));
        // Set here, not in the layout: the toggle group re-enables its buttons when it is inflated.
        view.findViewById(R.id.lang_filipino).setEnabled(false);

        pulse = ObjectAnimator.ofFloat(statusDot, View.ALPHA, 1f, 0.3f);
        pulse.setDuration(1000);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.setRepeatMode(ValueAnimator.REVERSE);

        startStop.setOnClickListener(v -> {
            if (engine == null) startListening(); else stopListening();
        });
        clearButton.setOnClickListener(v -> {
            model.transcript.clear();
            renderTranscript();
            refresh();
        });

        renderTranscript();
    }

    @Override
    public void onStart() {
        super.onStart();
        refresh();
        checkEnglish();
    }

    @Override
    public void onStop() {
        // Listening only while this screen is on display (contract 1.10).
        stopListening();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        pulse.cancel();
        super.onDestroyView();
    }

    // ---- Starting and stopping ----

    private boolean hasMic() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** Asks the recognizer about English, unless the answer is already known to be final. */
    private void checkEnglish() {
        if (!hasMic() || engine != null) return;
        GoogleOnDeviceSupport.Result known = model.englishSupport;
        if (known != null && known.status != GoogleOnDeviceSupport.Status.NEEDS_DOWNLOAD
                && known.status != GoogleOnDeviceSupport.Status.DOWNLOADING
                && known.status != GoogleOnDeviceSupport.Status.UNKNOWN) {
            return;
        }
        GoogleOnDeviceSupport.check(requireContext(), ENGLISH, result -> {
            model.englishSupport = result;
            if (getView() != null) refresh();
        });
    }

    private boolean canStart() {
        GoogleOnDeviceSupport.Result support = model.englishSupport;
        return hasMic() && support != null
                && (support.status == GoogleOnDeviceSupport.Status.READY
                || support.status == GoogleOnDeviceSupport.Status.UNKNOWN);
    }

    private void startListening() {
        // canStart() is only true on Android 13+, but lint cannot see that.
        if (engine != null || !canStart() || Build.VERSION.SDK_INT < 33) return;
        // A new session starts with an empty page.
        model.transcript.clear();
        model.lastFailure = null;
        heardReady = false;
        engine = new GoogleOnDeviceEngine(requireContext(), model.englishSupport.languageTag,
                model.englishSupport.service);
        String stopped = MicOwner.take(this);
        model.micNote = stopped == null ? null : getString(R.string.mic_other_stopped, stopped);
        engine.start(this);
        renderTranscript();
        refresh();
    }

    private void stopListening() {
        if (engine == null) return;
        engine.stop();
        engine = null;
        endSession();
    }

    private void endSession() {
        MicOwner.release(this);
        model.transcript.endSegment();
        CaptionRelay.publishStopped();
        if (getView() != null) {
            renderTranscript();
            refresh();
        }
    }

    // ---- MicOwner.User ----

    @Override
    public String micUserName() {
        return getString(R.string.captions_title);
    }

    @Override
    public void onMicTaken(String byWhom) {
        stopListening();
        model.micNote = getString(R.string.mic_taken, byWhom);
        if (getView() != null) refresh();
    }

    // ---- CaptionListener ----

    @Override
    public void onListening() {
        if (!heardReady) {
            heardReady = true;
            refresh();
        }
    }

    @Override
    public void onPartial(String text) {
        model.transcript.setPartial(text);
        CaptionRelay.publish(model.transcript.latest(), false);
        renderTranscript();
    }

    @Override
    public void onFinal(String text) {
        model.transcript.addFinal(text);
        CaptionRelay.publish(model.transcript.latest(), true);
        renderTranscript();
    }

    @Override
    public void onSegmentEnd() {
        boolean hadPartial = !model.transcript.partial().isEmpty();
        model.transcript.endSegment();
        if (hadPartial) {
            CaptionRelay.publish(model.transcript.latest(), true);
            renderTranscript();
        }
    }

    @Override
    public void onFailed(Failure failure) {
        engine = null;
        model.lastFailure = failure;
        if (failure == Failure.LANGUAGE_PACK) {
            model.englishSupport = null;
            checkEnglish();
        }
        endSession();
    }

    // ---- Drawing ----

    private void renderTranscript() {
        Transcript t = model.transcript;
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
        clearButton.setVisibility(engine == null && !t.isEmpty() ? View.VISIBLE : View.GONE);
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
        boolean listening = engine != null;
        View root = getView();
        if (root != null) root.setKeepScreenOn(listening);

        pausedNote.setVisibility(listening ? View.VISIBLE : View.GONE);
        clearButton.setVisibility(!listening && !model.transcript.isEmpty() ? View.VISIBLE : View.GONE);

        statusDot.setVisibility(listening ? View.VISIBLE : View.GONE);
        if (listening) {
            statusText.setText(heardReady ? R.string.captions_status_listening
                                          : R.string.captions_status_starting);
            statusText.setTextColor(themeColor(R.attr.agapAccent));
            if (!pulse.isStarted()) pulse.start();
        } else {
            statusText.setText(R.string.captions_status_idle);
            statusText.setTextColor(themeColor(R.attr.agapTextSecondary));
            pulse.cancel();
            statusDot.setAlpha(1f);
        }

        startStop.setText(listening ? R.string.captions_stop : R.string.captions_start);
        startStop.setIconResource(listening ? R.drawable.ic_stop : R.drawable.ic_mic);
        startStop.setEnabled(listening || canStart());

        showMessage();
    }

    private void showMessage() {
        if (model.micNote != null) {
            message(model.micNote);
        } else if (engine != null) {
            hideMessage();
        } else if (!hasMic()) {
            boolean blocked = model.askedForMic
                    && !shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO);
            if (blocked) {
                message(R.string.captions_mic_blocked, R.string.captions_open_settings, v -> openAppSettings());
            } else {
                message(R.string.captions_mic_explain, R.string.captions_allow_mic,
                        v -> askMic.launch(Manifest.permission.RECORD_AUDIO));
            }
        } else if (model.englishSupport == null) {
            message(R.string.captions_checking, 0, null);
        } else if (model.lastFailure == Failure.RECOGNIZER) {
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

    private void message(@StringRes int text, @StringRes int action, @Nullable View.OnClickListener onAction) {
        messageCard.setVisibility(View.VISIBLE);
        messageText.setText(text);
        if (action != 0) {
            messageAction.setVisibility(View.VISIBLE);
            messageAction.setText(action);
            messageAction.setOnClickListener(onAction);
        } else {
            messageAction.setVisibility(View.GONE);
        }
    }

    private void message(CharSequence text) {
        messageCard.setVisibility(View.VISIBLE);
        messageText.setText(text);
        messageAction.setVisibility(View.GONE);
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

package ph.agap.app.ui.alerts;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ph.agap.app.R;
import ph.agap.app.alerts.AlertCategory;
import ph.agap.app.alerts.AlertDetector;
import ph.agap.app.alerts.AlertEvent;
import ph.agap.app.alerts.SoundAlertEngine;
import ph.agap.app.mic.MicOwner;
import ph.agap.app.settings.SettingsStore;

/**
 * Sound Alerts, minimal version: Start/Stop, a listening indicator and the last detected alert as
 * text. The full alert screen, vibration and Event History come in Phase 5. Listening stops when
 * this screen leaves the display, and the screen stays on while listening (contract 1.10).
 */
public class AlertsFragment extends Fragment implements SoundAlertEngine.Listener, MicOwner.User {

    private AlertsViewModel model;
    @Nullable private SoundAlertEngine engine;
    private boolean heardReady;
    private float threshold;

    private View statusDot;
    private TextView statusText;
    private View messageCard;
    private TextView messageText;
    private MaterialButton messageAction;
    private MaterialCardView lastCard;
    private TextView lastLabel;
    private TextView lastDetails;
    private TextView sessionCount;
    private TextView closestNow;
    private View captionsPausedNote;
    private MaterialButton startStop;
    private ValueAnimator pulse;

    private final ActivityResultLauncher<String> askMic = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                model.askedForMic = true;
                refresh();
            });

    public AlertsFragment() {
        super(R.layout.fragment_alerts);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        model = new ViewModelProvider(requireActivity()).get(AlertsViewModel.class);
        statusDot = view.findViewById(R.id.status_dot);
        statusText = view.findViewById(R.id.status_text);
        messageCard = view.findViewById(R.id.message_card);
        messageText = view.findViewById(R.id.message_text);
        messageAction = view.findViewById(R.id.message_action);
        lastCard = view.findViewById(R.id.last_alert_card);
        lastLabel = view.findViewById(R.id.last_alert_label);
        lastDetails = view.findViewById(R.id.last_alert_details);
        sessionCount = view.findViewById(R.id.session_count);
        closestNow = view.findViewById(R.id.closest_now);
        captionsPausedNote = view.findViewById(R.id.captions_paused_note);
        startStop = view.findViewById(R.id.start_stop);

        pulse = ObjectAnimator.ofFloat(statusDot, View.ALPHA, 1f, 0.3f);
        pulse.setDuration(1000);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.setRepeatMode(ValueAnimator.REVERSE);

        startStop.setOnClickListener(v -> {
            if (engine == null) startListening(); else stopListening();
        });
        showLastEvent();
        closestNow.setText("");
    }

    @Override
    public void onStart() {
        super.onStart();
        refresh();
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

    private void startListening() {
        if (engine != null || !hasMic()) return;
        String stopped = MicOwner.take(this);
        model.micNote = stopped == null ? null : getString(R.string.mic_other_stopped, stopped);
        model.failure = null;
        model.sessionCount = 0;
        heardReady = false;
        threshold = SettingsStore.from(requireContext()).getAlertThreshold();
        engine = new SoundAlertEngine(requireContext(), model.annex());
        engine.start(threshold, this);
        showLastEvent();
        refresh();
    }

    private void stopListening() {
        if (engine == null) return;
        engine.stop();
        engine = null;
        MicOwner.release(this);
        if (getView() != null) {
            closestNow.setText("");
            refresh();
        }
    }

    // ---- MicOwner.User ----

    @Override
    public String micUserName() {
        return getString(R.string.alerts_title);
    }

    @Override
    public void onMicTaken(String byWhom) {
        stopListening();
        model.micNote = getString(R.string.mic_taken, byWhom);
        if (getView() != null) refresh();
    }

    // ---- SoundAlertEngine.Listener ----

    @Override
    public void onListening() {
        heardReady = true;
        refresh();
    }

    @Override
    public void onWindow(AlertDetector.Result result, String topClass, float topScore) {
        if (result.event != null) {
            model.lastEvent = result.event;
            model.sessionCount++;
            showLastEvent();
        }
        if (result.best == null) {
            closestNow.setText(R.string.alerts_closest_none);
        } else {
            closestNow.setText(getString(R.string.alerts_closest, result.best.type.label,
                    percent(result.best.score), result.best.yamnetClass, topClass, percent(topScore),
                    percent(threshold)));
        }
    }

    @Override
    public void onFailed(String reason) {
        engine = null;
        MicOwner.release(this);
        model.failure = reason;
        refresh();
    }

    // ---- Drawing ----

    private void showLastEvent() {
        AlertEvent event = model.lastEvent;
        if (event == null) {
            lastLabel.setText(R.string.alerts_none_yet);
            lastLabel.setTextAppearance(R.style.TextAppearance_AGAP_Body);
            lastDetails.setVisibility(View.GONE);
            lastCard.setCardBackgroundColor(themeColor(R.attr.agapSurface));
            lastCard.setStrokeColor(themeColor(R.attr.agapOutline));
        } else {
            int row = rowAttr(event.category());
            int ink = themeColor(inkAttr(event.category()));
            lastLabel.setText(event.label());
            lastLabel.setTextAppearance(R.style.TextAppearance_AGAP_Headline);
            lastLabel.setTextColor(ink);
            lastDetails.setVisibility(View.VISIBLE);
            lastDetails.setTextColor(ink);
            lastDetails.setText(getString(R.string.alerts_details, event.category().displayName,
                    percent(event.confidence), formatTime(event.detectedAtMillis)));
            lastCard.setCardBackgroundColor(themeColor(row));
            lastCard.setStrokeColor(ColorStateList.valueOf(ink));
        }
        sessionCount.setText(getString(R.string.alerts_session_count, model.sessionCount));
    }

    private void refresh() {
        boolean listening = engine != null;
        View root = getView();
        if (root == null) return;
        root.setKeepScreenOn(listening);
        captionsPausedNote.setVisibility(listening ? View.VISIBLE : View.GONE);

        statusDot.setVisibility(listening ? View.VISIBLE : View.GONE);
        if (listening) {
            statusText.setText(heardReady ? R.string.alerts_status_listening : R.string.alerts_status_starting);
            statusText.setTextColor(themeColor(R.attr.agapAccent));
            if (!pulse.isStarted()) pulse.start();
        } else {
            statusText.setText(R.string.alerts_status_idle);
            statusText.setTextColor(themeColor(R.attr.agapTextSecondary));
            pulse.cancel();
            statusDot.setAlpha(1f);
        }
        startStop.setText(listening ? R.string.captions_stop : R.string.captions_start);
        startStop.setIconResource(listening ? R.drawable.ic_stop : R.drawable.ic_mic);
        startStop.setEnabled(listening || hasMic());
        showMessage();
    }

    private void showMessage() {
        if (model.micNote != null) {
            message(model.micNote, 0, null);
        } else if (model.failure != null && engine == null) {
            message(getString(R.string.alerts_failed, model.failure), 0, null);
        } else if (engine == null && !hasMic()) {
            boolean blocked = model.askedForMic
                    && !shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO);
            if (blocked) {
                message(getString(R.string.alerts_mic_blocked), R.string.captions_open_settings, v -> {
                    startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", requireContext().getPackageName(), null)));
                });
            } else {
                message(getString(R.string.alerts_mic_explain), R.string.captions_allow_mic,
                        v -> askMic.launch(Manifest.permission.RECORD_AUDIO));
            }
        } else {
            messageCard.setVisibility(View.GONE);
        }
    }

    private void message(CharSequence text, @StringRes int action, @Nullable View.OnClickListener onAction) {
        messageCard.setVisibility(View.VISIBLE);
        messageText.setText(text);
        messageAction.setVisibility(action == 0 ? View.GONE : View.VISIBLE);
        if (action != 0) {
            messageAction.setText(action);
            messageAction.setOnClickListener(onAction);
        }
    }

    private static int percent(float score) {
        return Math.round(score * 100f);
    }

    private static String formatTime(long millis) {
        return new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(new Date(millis));
    }

    @AttrRes
    private static int rowAttr(AlertCategory category) {
        switch (category) {
            case EMERGENCY: return R.attr.agapEmergencyRow;
            case TRAFFIC: return R.attr.agapTrafficRow;
            default: return R.attr.agapHouseholdRow;
        }
    }

    @AttrRes
    private static int inkAttr(AlertCategory category) {
        switch (category) {
            case EMERGENCY: return R.attr.agapEmergencyInk;
            case TRAFFIC: return R.attr.agapTrafficInk;
            default: return R.attr.agapHouseholdInk;
        }
    }

    private int themeColor(@AttrRes int attr) {
        TypedValue value = new TypedValue();
        requireContext().getTheme().resolveAttribute(attr, value, true);
        return value.data;
    }
}

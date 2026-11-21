package ph.agap.app.ui.alerts;

import android.Manifest;
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
import ph.agap.app.alerts.AlertDetector;
import ph.agap.app.alerts.AlertEvent;
import ph.agap.app.demo.DemoMode;
import ph.agap.app.listen.AlertsSession;
import ph.agap.app.settings.SettingsStore;
import ph.agap.app.ui.SlowPulse;
import ph.agap.app.ui.info.DemoModeActivity;

/**
 * Sound Alerts: Start/Stop, a listening indicator and the last detected alert. Each alert opens
 * the full-screen alert with its vibration (AlertOverlayFragment), which also saves it to the
 * Event History; listening goes on behind it. The listening itself is AlertsSession's (Phase 11).
 * With background listening off (the default) it stops when this screen leaves the display, as in
 * 1.0 (contract 1.10); with it on, it goes on until Stop. The screen stays on while listening here.
 * In Demonstration Mode the microphone is never opened (contract 1.9).
 */
public class AlertsFragment extends Fragment {

    private AlertsViewModel model;
    private AlertsSession session;

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
    private View backgroundNote;
    private MaterialButton startStop;
    private SlowPulse pulse;

    private final Runnable sessionChanged = () -> {
        if (getView() != null) {
            showLastEvent();
            showClosest();
            refresh();
        }
    };

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
        session = AlertsSession.get(requireContext());
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
        backgroundNote = view.findViewById(R.id.background_note);
        startStop = view.findViewById(R.id.start_stop);

        pulse = SlowPulse.alpha(statusDot, 0.3f, 1000);

        startStop.setOnClickListener(v -> {
            if (session.isRunning()) session.stop(); else startListening();
        });
        showLastEvent();
        closestNow.setText("");
    }

    @Override
    public void onStart() {
        super.onStart();
        session.addListener(sessionChanged);
        // The threshold may have been changed in Settings meanwhile: show the value that will be used.
        showLastEvent();
        showClosest();
        refresh();
    }

    @Override
    public void onStop() {
        session.removeListener(sessionChanged);
        // Listening only while this screen is on display (contract 1.10), unless background
        // listening is on (Phase 11): then ListeningService keeps it going.
        if (!keepsListeningWhenHidden()) session.stop();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        pulse.stop();
        super.onDestroyView();
    }

    private boolean keepsListeningWhenHidden() {
        return SettingsStore.from(requireContext()).isBackgroundListeningOn() && !DemoMode.isOn();
    }

    // ---- Starting ----

    private boolean hasMic() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void startListening() {
        if (session.isRunning() || !hasMic() || DemoMode.isOn()) return;
        session.start();
    }

    // ---- Drawing ----

    private void showLastEvent() {
        AlertEvent event = session.lastEvent();
        if (event == null) {
            lastLabel.setText(R.string.alerts_none_yet);
            lastLabel.setTextAppearance(R.style.TextAppearance_AGAP_Body);
            lastDetails.setVisibility(View.GONE);
            lastCard.setCardBackgroundColor(themeColor(R.attr.agapSurface));
            lastCard.setStrokeColor(themeColor(R.attr.agapOutline));
        } else {
            int row = AlertLooks.rowAttr(event.category());
            int ink = themeColor(AlertLooks.inkAttr(event.category()));
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
        int percent = session.isRunning() ? session.thresholdPercent()
                : SettingsStore.from(requireContext()).getAlertThresholdPercent();
        sessionCount.setText(getString(R.string.alerts_session_count, session.sessionCount(), percent));
    }

    private void showClosest() {
        AlertDetector.Result result = session.lastWindow();
        if (!session.isRunning() || result == null) {
            closestNow.setText("");
        } else if (result.best == null) {
            closestNow.setText(R.string.alerts_closest_none);
        } else {
            closestNow.setText(getString(R.string.alerts_closest, result.best.type.label,
                    percent(result.best.score), result.best.yamnetClass, session.topClass(),
                    percent(session.topScore()), percent(session.threshold())));
        }
    }

    private void refresh() {
        boolean listening = session.isRunning();
        View root = getView();
        if (root == null) return;
        root.setKeepScreenOn(listening);
        captionsPausedNote.setVisibility(listening ? View.VISIBLE : View.GONE);
        backgroundNote.setVisibility(listening && keepsListeningWhenHidden() ? View.VISIBLE : View.GONE);

        statusDot.setVisibility(listening ? View.VISIBLE : View.GONE);
        if (listening) {
            statusText.setText(session.heardReady() ? R.string.alerts_status_listening : R.string.alerts_status_starting);
            statusText.setTextColor(themeColor(R.attr.agapAccent));
            if (!pulse.isStarted()) pulse.start();
        } else {
            statusText.setText(R.string.alerts_status_idle);
            statusText.setTextColor(themeColor(R.attr.agapTextSecondary));
            pulse.stop();
        }
        startStop.setText(listening ? R.string.captions_stop : R.string.captions_start);
        startStop.setIconResource(listening ? R.drawable.ic_stop : R.drawable.ic_mic);
        startStop.setEnabled(listening || (hasMic() && !DemoMode.isOn()));
        showMessage();
    }

    private void showMessage() {
        boolean listening = session.isRunning();
        if (DemoMode.isOn() && !listening) {
            message(getString(R.string.alerts_demo_note), R.string.alerts_demo_open,
                    v -> startActivity(new Intent(requireContext(), DemoModeActivity.class)));
        } else if (listening && session.micSilenced()) {
            message(getString(R.string.mic_silenced), 0, null);
        } else if (session.micNote() != null) {
            message(session.micNote(), 0, null);
        } else if (session.failure() != null && !listening) {
            message(getString(R.string.alerts_failed, session.failure()), 0, null);
        } else if (!listening && !hasMic()) {
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

    private int themeColor(@AttrRes int attr) {
        TypedValue value = new TypedValue();
        requireContext().getTheme().resolveAttribute(attr, value, true);
        return value.data;
    }
}

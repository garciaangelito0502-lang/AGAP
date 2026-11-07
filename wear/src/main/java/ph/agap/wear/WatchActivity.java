package ph.agap.wear;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.Nullable;

import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.Date;
import java.util.List;

import ph.agap.app.alerts.AlertCategory;
import ph.agap.app.alerts.AlertIcons;
import ph.agap.app.watch.WatchMessages;

/**
 * The only watch screen. Underneath: what the phone is doing (or that it is not connected), or the
 * phone's live caption text, large and scrollable. On top: the newest alert, full screen in the
 * category's colour and badge shape, until it is tapped. The watch never uses its microphone.
 */
public class WatchActivity extends Activity implements WatchHub.Listener {

    private static final String TAG = "AGAP/Watch";
    /** How often the screen checks that the phone is still connected. */
    private static final long CHECK_EVERY_MILLIS = 10_000;
    /** A message from the phone this recently also counts as connected. */
    private static final long HEARD_RECENTLY_MILLIS = 15_000;
    private static final int REQUEST_NOTIFICATIONS = 1;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable checkPhone = this::checkPhone;
    private WatchHub hub;
    /** Null until the first check has finished. */
    @Nullable private Boolean phoneConnected;
    private boolean askedPhone;

    private View statusPanel;
    private TextView statusTitle;
    private TextView statusDetail;
    private TextView statusDemo;
    private View captionsPanel;
    private TextView captionsHeader;
    private ScrollView captionsScroll;
    private TextView captionsText;
    private View alertPanel;
    private ScrollView alertScroll;
    private View alertContent;
    /** The alert drawn last, to start a new one scrolled to the top. */
    @Nullable private WatchMessages.Alert shownAlert;
    private TextView alertSample;
    private ImageView alertBadge;
    private ImageView alertIcon;
    private TextView alertLabel;
    private TextView alertCategory;
    private TextView alertMeta;
    private TextView alertClose;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // An alert that arrives with the screen off opens this screen over the watch face.
        setShowWhenLocked(true);
        setTurnScreenOn(true);
        setContentView(R.layout.activity_watch);

        statusPanel = findViewById(R.id.status_panel);
        statusTitle = findViewById(R.id.status_title);
        statusDetail = findViewById(R.id.status_detail);
        statusDemo = findViewById(R.id.status_demo);
        captionsPanel = findViewById(R.id.captions_panel);
        captionsHeader = findViewById(R.id.captions_header);
        captionsScroll = findViewById(R.id.captions_scroll);
        captionsText = findViewById(R.id.captions_text);
        alertPanel = findViewById(R.id.alert_panel);
        alertScroll = findViewById(R.id.alert_scroll);
        alertContent = findViewById(R.id.alert_content);
        alertSample = findViewById(R.id.alert_sample);
        alertBadge = findViewById(R.id.alert_badge);
        alertIcon = findViewById(R.id.alert_icon);
        alertLabel = findViewById(R.id.alert_label);
        alertCategory = findViewById(R.id.alert_category);
        alertMeta = findViewById(R.id.alert_meta);
        alertClose = findViewById(R.id.alert_close);
        if (getResources().getConfiguration().fontScale > 1.1f) {
            View frame = findViewById(R.id.alert_badge_frame);
            int size = getResources().getDimensionPixelSize(R.dimen.badge_large_text);
            frame.getLayoutParams().width = size;
            frame.getLayoutParams().height = size;
            int icon = getResources().getDimensionPixelSize(R.dimen.badge_icon_large_text);
            alertIcon.getLayoutParams().width = icon;
            alertIcon.getLayoutParams().height = icon;
        }

        hub = WatchHub.get(this);
        alertContent.setOnClickListener(v -> hub.dismissAlert());

        // Needed so an alert can open this screen while it is closed (Wear OS 4 and newer ask for it).
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hub.setListener(this);
        // The screen shows the alert itself now.
        AlertNotifier.cancel(this);
        askedPhone = false;
        render();
        checkPhone();
    }

    @Override
    protected void onPause() {
        hub.setListener(null);
        main.removeCallbacks(checkPhone);
        super.onPause();
    }

    @Override
    public void onWatchChanged() {
        render();
    }

    /**
     * With the watch dimmed, this screen stays in front but the display is off. Starting the screen
     * again (it is singleTop and turns the screen on) wakes the display to show the alert.
     */
    @Override
    public void onAlert() {
        if (getSystemService(PowerManager.class).isInteractive()) return;
        Log.i(TAG, "waking the screen for the alert");
        startActivity(new Intent(this, WatchActivity.class));
    }

    /** Is the phone connected? The first time, also asks the AGAP phone app what it is doing. */
    private void checkPhone() {
        main.removeCallbacks(checkPhone);
        Wearable.getNodeClient(this).getConnectedNodes()
                .addOnSuccessListener(this::onNodes)
                .addOnFailureListener(e -> {
                    Log.w(TAG, "could not look for the phone: " + e);
                    onNodes(List.of());
                });
        main.postDelayed(checkPhone, CHECK_EVERY_MILLIS);
    }

    private void onNodes(List<Node> nodes) {
        if (isFinishing() || isDestroyed()) return;
        phoneConnected = !nodes.isEmpty();
        if (!isConnected()) {
            hub.forgetState();
        } else if (!askedPhone || hub.state() == null) {
            askedPhone = true;
            for (Node node : nodes) {
                Wearable.getMessageClient(this).sendMessage(node.getId(), WatchMessages.PATH_HELLO, new byte[0])
                        .addOnFailureListener(e -> Log.w(TAG, "could not reach the phone: " + e));
            }
        }
        render();
    }

    private boolean isConnected() {
        return Boolean.TRUE.equals(phoneConnected) || hub.heardFromPhoneWithin(HEARD_RECENTLY_MILLIS);
    }

    // ---- Drawing ----

    private void render() {
        WatchMessages.Alert alert = hub.pendingAlert();
        alertPanel.setVisibility(alert != null ? View.VISIBLE : View.GONE);
        if (alert != null && alert != shownAlert) {
            renderAlert(alert);
            alertScroll.scrollTo(0, 0);
            alertScroll.requestFocus();
        }
        shownAlert = alert;

        WatchMessages.State state = hub.state();
        boolean connected = isConnected();
        boolean captions = connected && state != null && state.mode == WatchMessages.Mode.CAPTIONS;
        captionsPanel.setVisibility(captions ? View.VISIBLE : View.GONE);
        statusPanel.setVisibility(captions ? View.GONE : View.VISIBLE);
        // Captions are read, not glanced at: keep the screen on while they run.
        if (captions) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            renderCaptions(state);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            renderStatus(connected, state);
        }
        // Only the alert is read out while it covers the screen.
        int below = alert != null ? View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                : View.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
        statusPanel.setImportantForAccessibility(below);
        captionsPanel.setImportantForAccessibility(below);
    }

    private void renderStatus(boolean connected, @Nullable WatchMessages.State state) {
        if (phoneConnected == null && !connected) {
            statusTitle.setText(R.string.app_name);
            statusDetail.setText(R.string.watch_checking);
        } else if (!connected) {
            statusTitle.setText(R.string.watch_no_phone_title);
            statusDetail.setText(R.string.watch_no_phone_detail);
        } else if (state == null) {
            statusTitle.setText(R.string.watch_phone_title);
            statusDetail.setText(R.string.watch_waiting);
        } else if (state.mode == WatchMessages.Mode.ALERTS) {
            statusTitle.setText(R.string.watch_alerts_title);
            statusDetail.setText(R.string.watch_alerts_detail);
        } else {
            statusTitle.setText(R.string.watch_phone_title);
            statusDetail.setText(R.string.watch_idle_detail);
        }
        statusDemo.setVisibility(connected && state != null && state.demo ? View.VISIBLE : View.GONE);
    }

    private void renderCaptions(WatchMessages.State state) {
        // Demonstration Mode: "Sample captions", in the demo colour, as on the phone.
        captionsHeader.setText(getString(state.demo ? R.string.watch_captions_header_sample
                : R.string.watch_captions_header, languageName(state.languageTag)));
        captionsHeader.setTextColor(getColor(state.demo ? R.color.hc_demo : R.color.hc_text_secondary));

        CaptionBuffer captions = hub.captions();
        boolean empty = captions.isEmpty();
        captionsText.setTextColor(getColor(empty ? R.color.hc_text_secondary : R.color.hc_text));
        // Follow the newest words, unless the user has scrolled back to read.
        boolean atEnd = !captionsScroll.canScrollVertically(1);
        captionsText.setText(empty ? getString(R.string.watch_captions_waiting) : captions.text());
        if (atEnd) captionsScroll.post(() -> captionsScroll.fullScroll(View.FOCUS_DOWN));
        // Lets the crown or rotating bezel scroll the text.
        if (hub.pendingAlert() == null && !captionsScroll.hasFocus()) captionsScroll.requestFocus();
    }

    private String languageName(String tag) {
        if (tag.startsWith("fil") || tag.startsWith("tl")) return getString(R.string.watch_lang_filipino);
        if (tag.startsWith("en")) return getString(R.string.watch_lang_english);
        return tag;
    }

    private void renderAlert(WatchMessages.Alert alert) {
        AlertCategory category = alert.category;
        int strong = getColor(strong(category));
        int onStrong = getColor(onStrong(category));
        String time = DateFormat.getTimeFormat(this).format(new Date(alert.detectedAtMillis));

        alertPanel.setBackgroundColor(strong);
        alertBadge.setImageResource(AlertIcons.badgeShape(category));
        alertBadge.setImageTintList(ColorStateList.valueOf(onStrong));
        alertIcon.setImageResource(AlertIcons.icon(alert.label, category));
        alertIcon.setImageTintList(ColorStateList.valueOf(strong));
        alertLabel.setText(alert.label);
        alertCategory.setText(category.displayName);
        alertMeta.setText(getString(R.string.watch_alert_meta, alert.confidencePercent, time));
        alertSample.setVisibility(alert.demo ? View.VISIBLE : View.GONE);
        for (TextView view : new TextView[] {alertSample, alertLabel, alertCategory, alertMeta, alertClose}) {
            view.setTextColor(onStrong);
        }
        String label = alert.demo ? getString(R.string.watch_sample_title, alert.label) : alert.label;
        alertContent.setContentDescription(getString(R.string.watch_alert_description,
                label, category.displayName, time));
    }

    @ColorRes
    private static int strong(AlertCategory category) {
        switch (category) {
            case EMERGENCY: return R.color.emergency_strong;
            case TRAFFIC: return R.color.traffic_strong;
            default: return R.color.household_strong;
        }
    }

    @ColorRes
    private static int onStrong(AlertCategory category) {
        switch (category) {
            case EMERGENCY: return R.color.emergency_on_strong;
            case TRAFFIC: return R.color.traffic_on_strong;
            default: return R.color.household_on_strong;
        }
    }
}

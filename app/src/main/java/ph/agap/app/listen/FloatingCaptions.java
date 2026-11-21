package ph.agap.app.listen;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.List;

import ph.agap.app.R;
import ph.agap.app.captions.Transcript;
import ph.agap.app.settings.SettingsStore;
import ph.agap.app.ui.MainActivity;

/**
 * The floating caption window (Phase 11; needs "Display over other apps"): while Live Captions
 * listens in the background, the newest lines float over whatever app is open, on plain white or
 * black (the user's display mode), at the user's text size. Drag it by its top bar; Open goes back
 * to AGAP, Stop ends the session. It shows only while AGAP itself is not in view; Android keeps
 * such windows off the lock screen. Main thread only.
 */
final class FloatingCaptions {

    private static final String TAG = "AGAP/Captions";
    /** The newest finished lines shown above the line being heard. */
    private static final int LINES = 3;
    private static final int VISIBLE_TEXT_LINES = 4;

    @Nullable private static View window;
    @Nullable private static WindowManager.LayoutParams params;
    /** Where the user dragged it, kept while AGAP runs. */
    private static int savedY = -1;
    private static String lookKey = "";

    private FloatingCaptions() { }

    /** Shows or hides the window to match: real captions, background on, AGAP not in view, allowed. */
    static void update(Context app) {
        SettingsStore settings = SettingsStore.from(app);
        boolean wanted = CaptionsSession.get(app).isListeningForReal()
                && settings.isBackgroundListeningOn()
                && !AppVisibility.isAppVisible()
                && BackgroundPermissions.overlayAllowed(app);
        if (window != null && (!wanted || !lookKey.equals(settings.lookKey()))) hide(app);
        try {
            if (wanted && window == null) show(app);
            if (window != null) render(app);
        } catch (RuntimeException e) {
            // The floating window must never stop the captions; they still reach the notification and watch.
            Log.e(TAG, "floating captions failed", e);
            hide(app);
        }
    }

    static void wordsChanged(Context app) {
        if (window == null) return;
        try {
            render(app);
        } catch (RuntimeException e) {
            Log.e(TAG, "floating captions failed", e);
            hide(app);
        }
    }

    @SuppressLint("ClickableViewAccessibility") // dragging is extra; Open and Stop are normal buttons
    private static void show(Context app) {
        WindowManager wm = app.getSystemService(WindowManager.class);
        if (wm == null) return;
        SettingsStore settings = SettingsStore.from(app);
        lookKey = settings.lookKey();
        Context ui = uiContext(app, settings);
        View view = LayoutInflater.from(ui).inflate(R.layout.floating_captions, null);

        TextView caption = view.findViewById(R.id.floating_text);
        ScrollView scroll = view.findViewById(R.id.floating_scroll);
        ViewGroup.LayoutParams lp = scroll.getLayoutParams();
        lp.height = caption.getLineHeight() * VISIBLE_TEXT_LINES + caption.getPaddingTop() + caption.getPaddingBottom();
        scroll.setLayoutParams(lp);

        view.findViewById(R.id.floating_open).setOnClickListener(v ->
                app.startActivity(new Intent(app, MainActivity.class)
                        .putExtra(MainActivity.EXTRA_TAB, MainActivity.TAB_CAPTIONS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP)));
        view.findViewById(R.id.floating_stop).setOnClickListener(v -> CaptionsSession.get(app).stop());

        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP;
        p.y = savedY >= 0 ? savedY : app.getResources().getDisplayMetrics().heightPixels / 2;
        p.setTitle("AGAP captions");

        View handle = view.findViewById(R.id.floating_handle);
        handle.setOnTouchListener(new View.OnTouchListener() {
            private float downRawY;
            private int downY;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                if (params == null || window == null) return false;
                if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    downRawY = e.getRawY();
                    downY = params.y;
                    return true;
                }
                if (e.getActionMasked() == MotionEvent.ACTION_MOVE) {
                    int max = app.getResources().getDisplayMetrics().heightPixels - window.getHeight();
                    params.y = Math.max(0, Math.min(Math.max(0, max), downY + Math.round(e.getRawY() - downRawY)));
                    savedY = params.y;
                    try {
                        wm.updateViewLayout(window, params);
                    } catch (RuntimeException ignored) {
                        // removed meanwhile
                    }
                    return true;
                }
                return e.getActionMasked() == MotionEvent.ACTION_UP;
            }
        });

        try {
            wm.addView(view, p);
            window = view;
            params = p;
        } catch (RuntimeException e) {
            Log.w(TAG, "floating captions not shown: " + e.getMessage());
        }
    }

    private static void hide(Context app) {
        View view = window;
        window = null;
        params = null;
        if (view == null) return;
        WindowManager wm = app.getSystemService(WindowManager.class);
        try {
            if (wm != null) wm.removeView(view);
        } catch (RuntimeException ignored) {
            // already gone
        }
    }

    private static void render(Context app) {
        View view = window;
        if (view == null) return;
        CaptionsSession session = CaptionsSession.get(app);
        TextView title = view.findViewById(R.id.floating_title);
        title.setText(app.getString(R.string.floating_title, session.languageName()));

        Transcript t = session.transcript();
        SpannableStringBuilder text = new SpannableStringBuilder();
        List<String> lines = t.lines();
        for (int i = Math.max(0, lines.size() - LINES); i < lines.size(); i++) {
            if (text.length() > 0) text.append('\n');
            text.append(lines.get(i));
        }
        if (!t.partial().isEmpty()) {
            if (text.length() > 0) text.append('\n');
            int start = text.length();
            text.append(t.partial());
            // Words that may still change: lighter and underlined, as on the Live Captions screen.
            text.setSpan(new ForegroundColorSpan(themeColor(view.getContext(), R.attr.agapTextSecondary)),
                    start, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            text.setSpan(new UnderlineSpan(), start, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (text.length() == 0) text.append(app.getString(R.string.floating_waiting));
        TextView caption = view.findViewById(R.id.floating_text);
        caption.setText(text);
        ScrollView scroll = view.findViewById(R.id.floating_scroll);
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    private static Context uiContext(Context app, SettingsStore settings) {
        Configuration config = new Configuration(app.getResources().getConfiguration());
        config.fontScale = config.fontScale * settings.getTextScale();
        int theme = settings.isHighContrast() ? R.style.Theme_AGAP_HighContrast : R.style.Theme_AGAP;
        return new ContextThemeWrapper(app.createConfigurationContext(config), theme);
    }

    private static int themeColor(Context context, int attr) {
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(attr, value, true);
        return value.data;
    }
}

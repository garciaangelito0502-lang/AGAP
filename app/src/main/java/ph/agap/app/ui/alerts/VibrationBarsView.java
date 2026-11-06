package ph.agap.app.ui.alerts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import ph.agap.app.R;
import ph.agap.app.alerts.AlertCategory;
import ph.agap.app.vibration.VibrationPatterns;

/**
 * Draws one play of a category's vibration pattern as rounded bars (long or short buzzes), so the
 * rhythm can be seen as well as felt. Bar and gap widths follow the pattern's timings.
 */
public class VibrationBarsView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bar = new RectF();
    private long[] timings = new long[0];

    public VibrationBarsView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setPattern(AlertCategory category, int color) {
        timings = VibrationPatterns.timings(category);
        paint.setColor(color);
        setContentDescription(getContext().getString(description(category)));
        invalidate();
    }

    /** The pattern in words, for the guide and for screen readers. */
    @StringRes
    public static int description(AlertCategory category) {
        switch (category) {
            case EMERGENCY: return R.string.vibration_emergency;
            case TRAFFIC: return R.string.vibration_traffic;
            default: return R.string.vibration_household;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float d = getResources().getDisplayMetrics().density;
        int w = resolveSize((int) (180 * d), widthMeasureSpec);
        int h = resolveSize((int) (14 * d), heightMeasureSpec);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (timings.length < 2) return;
        // Gaps are drawn at most 300 ms wide, so long pauses do not squeeze the buzzes.
        long total = 0;
        for (int i = 1; i < timings.length; i++) total += i % 2 == 1 ? timings[i] : Math.min(timings[i], 300);
        float scale = (getWidth() - getPaddingLeft() - getPaddingRight()) / (float) total;
        float radius = getHeight() / 2f;
        float x = getPaddingLeft();
        for (int i = 1; i < timings.length; i++) {
            boolean on = i % 2 == 1;
            float w = (on ? timings[i] : Math.min(timings[i], 300)) * scale;
            if (on) {
                bar.set(x, 0, x + w, getHeight());
                canvas.drawRoundRect(bar, radius, radius, paint);
            }
            x += w;
        }
    }
}

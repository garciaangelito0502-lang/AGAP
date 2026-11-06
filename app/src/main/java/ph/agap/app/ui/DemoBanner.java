package ph.agap.app.ui;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import ph.agap.app.R;
import ph.agap.app.demo.DemoMode;

/**
 * The Demonstration Mode banner at the top of every screen and of the alert screen. Diagonal
 * purple stripes set it apart from the plain purple top bar; the text says what the mode means.
 */
public final class DemoBanner {

    private DemoBanner() { }

    /** Shows or hides a banner view (layout demo_banner) to match Demonstration Mode. */
    public static void update(@Nullable View banner) {
        if (banner == null) return;
        if (banner.getBackground() == null || !(banner.getBackground() instanceof Stripes)) {
            banner.setBackground(new Stripes(
                    ContextCompat.getColor(banner.getContext(), R.color.brand),
                    ContextCompat.getColor(banner.getContext(), R.color.demo_stripe),
                    banner.getResources().getDisplayMetrics().density));
        }
        banner.setVisibility(DemoMode.isOn() ? View.VISIBLE : View.GONE);
    }

    /** Diagonal stripes, 10dp wide, at 45 degrees. */
    static final class Stripes extends Drawable {
        private final int base;
        private final Paint stripe = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float width;
        private final Path path = new Path();

        Stripes(int base, int stripeColor, float density) {
            this.base = base;
            this.stripe.setColor(stripeColor);
            this.width = 10 * density;
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            Rect b = getBounds();
            canvas.drawColor(base);
            float h = b.height();
            for (float x = b.left - h; x < b.right; x += 2 * width) {
                path.reset();
                path.moveTo(x, b.bottom);
                path.lineTo(x + width, b.bottom);
                path.lineTo(x + width + h, b.top);
                path.lineTo(x + h, b.top);
                path.close();
                canvas.drawPath(path, stripe);
            }
        }

        @Override
        public void setAlpha(int alpha) {
            stripe.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            stripe.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.OPAQUE;
        }
    }
}

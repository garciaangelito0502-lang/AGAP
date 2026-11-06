package ph.agap.app.captions;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

/**
 * Sample captions for Demonstration Mode (contract 1.9). It never opens the microphone: it plays a
 * short fixed script, word by word, through the same listener calls as a real engine, so the
 * Captions screen looks and behaves as it does with live speech. Main thread only.
 */
public final class DemoCaptionEngine implements CaptionEngine {

    private static final String[] ENGLISH = {
            "Good morning everyone.",
            "This is a sample of live captions.",
            "The words appear on the screen as they are spoken.",
            "In Demonstration Mode the microphone stays off.",
    };

    private static final String[] FILIPINO = {
            "Magandang umaga po sa inyong lahat.",
            "Ito ay halimbawa ng mga caption.",
            "Lumalabas ang mga salita sa screen habang nagsasalita ang tao.",
            "Sa Demonstration Mode, hindi ginagamit ang mikropono.",
    };

    /** Time between words, and the pause after each sentence, in milliseconds. */
    static final long WORD_MS = 380;
    static final long SENTENCE_PAUSE_MS = 1200;

    private final String languageTag;
    private final String[] script;
    private final Handler main = new Handler(Looper.getMainLooper());
    @Nullable private CaptionListener listener;
    private int sentence;
    private int word;
    private boolean running;

    /** @param filipino true for the Filipino script, false for English */
    public DemoCaptionEngine(boolean filipino) {
        this.script = filipino ? FILIPINO : ENGLISH;
        this.languageTag = filipino ? "fil-PH" : "en-US";
    }

    /** The script's sentences (for tests). */
    static String[] script(boolean filipino) {
        return (filipino ? FILIPINO : ENGLISH).clone();
    }

    @Override
    public void start(CaptionListener listener) {
        if (running) return;
        this.listener = listener;
        running = true;
        sentence = 0;
        word = 0;
        main.post(() -> {
            if (running) listener.onListening();
        });
        main.postDelayed(this::step, SENTENCE_PAUSE_MS);
    }

    @Override
    public void stop() {
        running = false;
        main.removeCallbacksAndMessages(null);
    }

    @Override
    public String languageTag() {
        return languageTag;
    }

    @Override
    public String engineName() {
        return "Sample captions (Demonstration Mode)";
    }

    private void step() {
        if (!running || listener == null) return;
        if (sentence >= script.length) return; // script done; stays "listening" until Stop
        String[] words = script[sentence].split(" ");
        word++;
        if (word < words.length) {
            listener.onPartial(join(words, word));
            main.postDelayed(this::step, WORD_MS);
        } else {
            listener.onFinal(script[sentence]);
            sentence++;
            word = 0;
            main.postDelayed(this::step, SENTENCE_PAUSE_MS);
        }
    }

    private static String join(String[] words, int count) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) out.append(' ');
            out.append(words[i]);
        }
        return out.toString();
    }
}

package ph.agap.app.captions;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionService;
import android.speech.RecognitionSupport;
import android.speech.RecognitionSupportCallback;
import android.speech.SpeechRecognizer;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Finds out whether this phone can caption a language offline with Google's recognizer, and
 * whether the offline language pack is installed. Needs Android 13 (API 33), the first version
 * that can ask about installed language packs.
 *
 * Android's own on-device recognizer is used when the phone has one (Pixel phones do). Many
 * other phones do not register it but have Google's speech service ("Speech Services by
 * Google"); then that service is used with EXTRA_PREFER_OFFLINE, which asks it to use only its
 * offline engine (D-013).
 */
public final class GoogleOnDeviceSupport {

    private static final String TAG = "AGAP/Captions";

    public enum Status {
        /** The pack is installed: captions work offline. */
        READY,
        /** The phone supports the language but the pack is not installed yet. */
        NEEDS_DOWNLOAD,
        /** The pack is being downloaded. */
        DOWNLOADING,
        /** Android older than 13. */
        OLD_ANDROID,
        /** No on-device recognizer on this phone. */
        NO_RECOGNIZER,
        /** The on-device recognizer does not know this language. */
        LANGUAGE_NOT_SUPPORTED,
        /** The recognizer could not say; starting may still work. */
        UNKNOWN
    }

    /** The answer, with the exact language tag to use (it may be another variant, such as en-GB). */
    public static final class Result {
        public final Status status;
        public final String languageTag;
        /** Google's speech service to use, or null for Android's own on-device recognizer. */
        @Nullable public final ComponentName service;

        Result(Status status, String languageTag) {
            this(status, languageTag, null);
        }

        Result(Status status, String languageTag, @Nullable ComponentName service) {
            this.status = status;
            this.languageTag = languageTag;
            this.service = service;
        }

        Result withService(@Nullable ComponentName newService) {
            return new Result(status, languageTag, newService);
        }
    }

    /** Packages of Google's speech recognition service, best first. */
    private static final String[] GOOGLE_SPEECH_PACKAGES = {
            "com.google.android.tts",                    // Speech Services by Google
            "com.google.android.googlequicksearchbox"    // the Google app
    };

    private GoogleOnDeviceSupport() { }

    /** Checks support for languageTag and calls back once on the main thread. */
    public static void check(Context context, String languageTag, Consumer<Result> callback) {
        if (Build.VERSION.SDK_INT < 33) {
            callback.accept(new Result(Status.OLD_ANDROID, languageTag));
            return;
        }
        ComponentName service = null;
        if (!SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            service = findGoogleService(context);
            if (service == null) {
                Log.i(TAG, "no on-device recognizer and no Google speech service");
                callback.accept(new Result(Status.NO_RECOGNIZER, languageTag));
                return;
            }
            Log.i(TAG, "no system on-device recognizer; using " + service.flattenToShortString());
        }
        ComponentName chosen = service;
        SpeechRecognizer checker = create(context, chosen);
        Handler main = new Handler(Looper.getMainLooper());
        // Google's speech service was seen to answer one check with an error and then two results
        // (Xiaomi 13, 2026-11-04). So answer once, and let a real result win over an early error.
        boolean[] answered = {false};
        checker.checkRecognitionSupport(GoogleOnDeviceEngine.recognizeIntent(languageTag),
                ContextCompat.getMainExecutor(context), new RecognitionSupportCallback() {
                    @Override
                    public void onSupportResult(@NonNull RecognitionSupport support) {
                        if (answered[0]) return;
                        answered[0] = true;
                        checker.destroy();
                        Log.i(TAG, "on-device support: installed " + support.getInstalledOnDeviceLanguages()
                                + ", pending " + support.getPendingOnDeviceLanguages()
                                + ", supported " + support.getSupportedOnDeviceLanguages());
                        callback.accept(evaluate(languageTag,
                                support.getInstalledOnDeviceLanguages(),
                                support.getPendingOnDeviceLanguages(),
                                support.getSupportedOnDeviceLanguages()).withService(chosen));
                    }

                    @Override
                    public void onError(int error) {
                        Log.w(TAG, "support check error: " + RecognizerErrors.name(error));
                        main.postDelayed(() -> {
                            if (answered[0]) return;
                            answered[0] = true;
                            checker.destroy();
                            callback.accept(new Result(Status.UNKNOWN, languageTag, chosen));
                        }, 1500);
                    }
                });
    }

    /** Starts Google's one-time download of the offline pack. Needs internet while it runs. */
    public static void requestDownload(Context context, Result support) {
        if (Build.VERSION.SDK_INT < 33) return;
        SpeechRecognizer downloader = create(context, support.service);
        downloader.triggerModelDownload(GoogleOnDeviceEngine.recognizeIntent(support.languageTag));
        // The request reaches the service asynchronously: destroying at once could drop it.
        new Handler(Looper.getMainLooper()).postDelayed(downloader::destroy, 10_000);
        Log.i(TAG, "pack download requested for " + support.languageTag);
    }

    /** Android's on-device recognizer when service is null, otherwise that Google service. */
    @RequiresApi(33)
    static SpeechRecognizer create(Context context, @Nullable ComponentName service) {
        if (service == null) {
            return SpeechRecognizer.createOnDeviceSpeechRecognizer(context);
        }
        return SpeechRecognizer.createSpeechRecognizer(context, service);
    }

    @Nullable
    private static ComponentName findGoogleService(Context context) {
        List<ResolveInfo> services = context.getPackageManager()
                .queryIntentServices(new Intent(RecognitionService.SERVICE_INTERFACE), 0);
        for (String pkg : GOOGLE_SPEECH_PACKAGES) {
            for (ResolveInfo info : services) {
                if (pkg.equals(info.serviceInfo.packageName)) {
                    return new ComponentName(info.serviceInfo.packageName, info.serviceInfo.name);
                }
            }
        }
        return null;
    }

    /** Picks the status from the recognizer's language lists. Kept apart so it can be unit tested. */
    static Result evaluate(String wanted, @Nullable List<String> installed,
                           @Nullable List<String> pending, @Nullable List<String> supported) {
        String exact = find(installed, wanted, true);
        if (exact != null) return new Result(Status.READY, exact);
        // Another installed variant (such as en-GB for en-US) works offline now: better than a download.
        String sameLanguage = find(installed, wanted, false);
        if (sameLanguage != null) return new Result(Status.READY, sameLanguage);
        if (find(pending, wanted, true) != null) return new Result(Status.DOWNLOADING, wanted);
        if (find(supported, wanted, true) != null) return new Result(Status.NEEDS_DOWNLOAD, wanted);
        sameLanguage = find(pending, wanted, false);
        if (sameLanguage != null) return new Result(Status.DOWNLOADING, sameLanguage);
        sameLanguage = find(supported, wanted, false);
        if (sameLanguage != null) return new Result(Status.NEEDS_DOWNLOAD, sameLanguage);
        return new Result(Status.LANGUAGE_NOT_SUPPORTED, wanted);
    }

    @Nullable
    private static String find(@Nullable List<String> tags, String wanted, boolean exact) {
        if (tags == null) return null;
        String wantedLanguage = languageOf(wanted);
        for (String tag : tags) {
            if (exact ? tag.equalsIgnoreCase(wanted) : languageOf(tag).equals(wantedLanguage)) {
                return tag;
            }
        }
        return null;
    }

    private static String languageOf(String tag) {
        int dash = tag.indexOf('-');
        return (dash < 0 ? tag : tag.substring(0, dash)).toLowerCase(Locale.ROOT);
    }
}

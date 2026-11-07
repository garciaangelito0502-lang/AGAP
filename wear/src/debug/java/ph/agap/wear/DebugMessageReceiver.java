package ph.agap.wear;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import ph.agap.app.alerts.AlertCategory;
import ph.agap.app.watch.WatchMessages;

/**
 * Debug builds only (not in the release APK). Builds the exact bytes the phone would send and hands
 * them to WatchHub, so the watch screens can be tested on an emulator with no paired phone:
 *
 * <pre>
 * adb shell am broadcast -n ph.agap.app/ph.agap.wear.DebugMessageReceiver --es type alert \
 *     --es label "Fire Alarm" --es category Emergency --ei percent 91 [--ez demo true] [--ez vibrate false]
 * adb shell am broadcast -n ph.agap.app/ph.agap.wear.DebugMessageReceiver --es type caption \
 *     --es text "Magandang umaga" [--ez final true] [--es lang fil-PH]
 * adb shell am broadcast -n ph.agap.app/ph.agap.wear.DebugMessageReceiver --es type state \
 *     --es mode CAPTIONS [--es lang en-US] [--ez demo true]
 * </pre>
 */
public class DebugMessageReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String type = String.valueOf(intent.getStringExtra("type"));
        String lang = intent.getStringExtra("lang") != null ? intent.getStringExtra("lang") : "";
        WatchHub hub = WatchHub.get(context);
        switch (type) {
            case "alert":
                hub.onMessage(WatchMessages.PATH_ALERT, WatchMessages.alert(
                        String.valueOf(intent.getStringExtra("label")),
                        AlertCategory.fromDisplayName(String.valueOf(intent.getStringExtra("category"))),
                        intent.getIntExtra("percent", 90),
                        System.currentTimeMillis(),
                        intent.getBooleanExtra("demo", false),
                        intent.getBooleanExtra("vibrate", true)));
                break;
            case "caption":
                hub.onMessage(WatchMessages.PATH_CAPTION, WatchMessages.caption(
                        String.valueOf(intent.getStringExtra("text")), intent.getBooleanExtra("final", false), lang));
                break;
            case "state":
                hub.onMessage(WatchMessages.PATH_STATE, WatchMessages.state(
                        WatchMessages.Mode.valueOf(String.valueOf(intent.getStringExtra("mode"))), lang,
                        intent.getBooleanExtra("demo", false)));
                break;
            default:
                Log.w("AGAP/Watch", "debug message: unknown type " + type);
        }
    }
}

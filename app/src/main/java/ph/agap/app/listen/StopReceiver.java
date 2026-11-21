package ph.agap.app.listen;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * The Stop button of the "AGAP is listening" notification and of the home-screen widget (Phase 11):
 * stops Sound Alerts or Live Captions, which ends background listening. Works from anywhere, and
 * does nothing if nothing is listening (for example after Android closed AGAP).
 */
public final class StopReceiver extends BroadcastReceiver {

    static final String ACTION_STOP = "ph.agap.app.action.STOP_LISTENING";

    static PendingIntent pendingIntent(Context context) {
        Intent intent = new Intent(context, StopReceiver.class).setAction(ACTION_STOP);
        return PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION_STOP.equals(intent.getAction())) return;
        Log.i("AGAP/Background", "Stop pressed outside AGAP");
        AlertsSession.get(context).stop();
        CaptionsSession.get(context).stop();
        ListeningService.update(context);
        AgapWidget.refresh(context);
    }
}

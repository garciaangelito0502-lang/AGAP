package ph.agap.wear;

import androidx.annotation.NonNull;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Receives the phone's messages over the Wearable Data Layer, also when the AGAP watch screen is
 * closed (Google Play services starts this service). The watch never listens to sound itself.
 */
public class WatchListenerService extends WearableListenerService {

    @Override
    public void onMessageReceived(@NonNull MessageEvent event) {
        WatchHub.get(this).onMessage(event.getPath(), event.getData());
    }
}

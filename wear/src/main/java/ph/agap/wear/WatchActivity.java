package ph.agap.wear;

import android.app.Activity;
import android.os.Bundle;

/** Watch screen. Phase 7 adds alerts and live caption text received from the phone. */
public class WatchActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch);
    }
}

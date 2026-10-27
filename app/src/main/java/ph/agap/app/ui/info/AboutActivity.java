package ph.agap.app.ui.info;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;

import ph.agap.app.R;
import ph.agap.app.ui.BaseActivity;

/** What AGAP is and is not (contract 6.7), and the licences of the tools inside it (contract 8.2). */
public class AboutActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        setUpScreen(true, true);

        String version;
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            version = "?";
        }
        ((TextView) findViewById(R.id.about_version)).setText(getString(R.string.about_version, version));
    }
}

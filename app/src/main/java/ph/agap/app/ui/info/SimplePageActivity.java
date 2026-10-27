package ph.agap.app.ui.info;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import ph.agap.app.R;
import ph.agap.app.ui.BaseActivity;

/** A screen with a title and one block of text. Used until each screen is built (Phases 5 and 6). */
public abstract class SimplePageActivity extends BaseActivity {

    @StringRes
    protected abstract int bodyText();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_page);
        setUpScreen(true, true);
        ((TextView) findViewById(R.id.page_body)).setText(bodyText());
    }
}

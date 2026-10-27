package ph.agap.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import ph.agap.app.R;

/** Stands in for a tab whose screen is built in a later phase. */
public class PlaceholderFragment extends Fragment {

    private static final String ARG_TITLE = "title";
    private static final String ARG_BODY = "body";
    private static final String ARG_ICON = "icon";

    public PlaceholderFragment() {
        super(R.layout.fragment_placeholder);
    }

    public static PlaceholderFragment newInstance(@StringRes int title, @StringRes int body,
                                                  @DrawableRes int icon) {
        Bundle args = new Bundle();
        args.putInt(ARG_TITLE, title);
        args.putInt(ARG_BODY, body);
        args.putInt(ARG_ICON, icon);
        PlaceholderFragment fragment = new PlaceholderFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Bundle args = requireArguments();
        ((TextView) view.findViewById(R.id.placeholder_title)).setText(args.getInt(ARG_TITLE));
        ((TextView) view.findViewById(R.id.placeholder_body)).setText(args.getInt(ARG_BODY));
        ((ImageView) view.findViewById(R.id.placeholder_icon)).setImageResource(args.getInt(ARG_ICON));
    }
}

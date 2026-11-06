package ph.agap.app.ui.history;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import ph.agap.app.R;
import ph.agap.app.history.HistoryRepository;

/**
 * Event History (contract 1.6): every real alert, including false ones, newest first. Tapping one
 * opens its details with both times to the millisecond and the response time. Demonstration Mode
 * alerts are never here. Clearing asks first, because the group's results come from this list.
 */
public class HistoryFragment extends Fragment {

    private HistoryRepository repository;
    private HistoryAdapter adapter;
    private RecyclerView list;
    private View empty;
    private int count;
    private final Runnable reload = this::reload;

    public HistoryFragment() {
        super(R.layout.fragment_history);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repository = HistoryRepository.get(requireContext());
        list = view.findViewById(R.id.history_list);
        empty = view.findViewById(R.id.history_empty);
        adapter = new HistoryAdapter(id -> startActivity(
                new Intent(requireContext(), AlertDetailActivity.class).putExtra(AlertDetailActivity.EXTRA_ID, id)));
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        repository.addListener(reload);
        reload();
    }

    @Override
    public void onStop() {
        repository.removeListener(reload);
        super.onStop();
    }

    /** How many alerts are listed (for the clear-history question). */
    public int count() {
        return count;
    }

    /** Asks, then deletes every alert in the history. */
    public void confirmClear() {
        if (count == 0) {
            Snackbar.make(requireView(), R.string.history_already_empty, Snackbar.LENGTH_SHORT).show();
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.history_clear_title)
                .setMessage(getResources().getQuantityString(R.plurals.history_clear_message, count, count))
                .setNegativeButton(R.string.history_clear_cancel, null)
                .setPositiveButton(R.string.history_clear_confirm, (d, w) -> repository.clear(null))
                .show();
    }

    private void reload() {
        repository.loadAll(records -> {
            if (getView() == null) return;
            count = records.size();
            adapter.submit(requireContext(), records);
            empty.setVisibility(records.isEmpty() ? View.VISIBLE : View.GONE);
            list.setVisibility(records.isEmpty() ? View.GONE : View.VISIBLE);
        });
    }
}

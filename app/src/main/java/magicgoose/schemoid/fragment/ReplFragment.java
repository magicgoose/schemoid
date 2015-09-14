package magicgoose.schemoid.fragment;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import magicgoose.schemoid.R;
import magicgoose.schemoid.TheApp;
import magicgoose.schemoid.scheme.ISchemeRunner;
import magicgoose.schemoid.scheme.SchemeLogItemKind;
import magicgoose.schemoid.util.ClipboardUtil;
import magicgoose.schemoid.util.ReactiveList;
import rx.Subscription;

public class ReplFragment extends Fragment implements BackKeyHandler {

    private EditText codeEditText;
    private ISchemeRunner<SelectableSchemeLogItem> schemeRunner;
    private Subscription schemeOutputSubscription;
    private LogAdapter logAdapter;
    private RecyclerView logView;
    private ReactiveList<SelectableSchemeLogItem> log;
    private int selectedCount;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(selectedCount > 0 ? R.menu.menu_repl_selection : R.menu.menu_repl, menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.abort:
                abortEval();
                return true;
            case R.id.reset:
                resetEval();
                return true;
            case R.id.clear_log:
                log.clear();
                return true;
            case R.id.copy:
                copySelectedLogItems();
                return true;
            case R.id.delete:
                deleteSelectedLogItems();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void copySelectedLogItems() {
        final StringBuilder sb = new StringBuilder();
        for (final SelectableSchemeLogItem selectableSchemeLogItem : log) {
            if (selectableSchemeLogItem.isSelected) {
                sb.append(selectableSchemeLogItem.content);
                sb.append('\n');
            }
        }
        ClipboardUtil.copyToClipboard(getActivity(), "Code", sb.toString());
    }

    private void deleteSelectedLogItems() {
        for (int i = 0; i < log.size(); i++) {
            if (log.get(i).isSelected) {
                log.remove(i);
                --i;
            }
        }
        selectedCount = 0;
        invalidateSelectedCount();
    }

    private void resetEval() {
        if (schemeRunner != null) {
            schemeRunner.reset();
        }
    }

    private void abortEval() {
        if (schemeRunner != null) {
            schemeRunner.abort();
        }
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.repl, container, false);
    }

    @Override
    public void onViewCreated(final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final FragmentActivity ctx = getActivity();

        codeEditText = (EditText) view.findViewById(R.id.code_input);
        codeEditText.addTextChangedListener(new CodeEditTextWatcher());

        view.findViewById(R.id.eb_eval).setOnClickListener(this::evalClick);

        view.findViewById(R.id.eb_left_arrow).setOnClickListener(this::arrowClick);
        view.findViewById(R.id.eb_right_arrow).setOnClickListener(this::arrowClick);

        view.findViewById(R.id.eb_left_paren).setOnClickListener(this::parenClick);
        view.findViewById(R.id.eb_right_paren).setOnClickListener(this::parenClick);

        logView = ((RecyclerView) view.findViewById(R.id.code_output));
        logAdapter = new LogAdapter(logView);
        logView.setLayoutManager(new LinearLayoutManager(ctx, LinearLayoutManager.VERTICAL, false));
        logView.setAdapter(logAdapter);

        schemeRunner = TheApp.getInstance().getSchemeRunner();
        log = schemeRunner.getLog();
        schemeOutputSubscription = bindAdapterToList(logAdapter, log);
        selectedCount = getSelectedCount(log);
    }

    private int getSelectedCount(final ReactiveList<SelectableSchemeLogItem> log) {
        int count = 0;
        for (final SelectableSchemeLogItem selectableSchemeLogItem : log) {
            if (selectableSchemeLogItem.isSelected) {
                ++count;
            }
        }
        return count;
    }

    private Subscription bindAdapterToList(final LogAdapter logAdapter, final ReactiveList<SelectableSchemeLogItem> log) {
        logAdapter.reset(log);
        return log.getChanges().subscribe(listChange -> {
            switch (listChange.listChangeKind) {
                case Delete:
                    logAdapter.notifyItemRangeRemoved(listChange.rangeStart, listChange.count);
                    break;
                case Insert:
                    logAdapter.notifyItemRangeInserted(listChange.rangeStart, listChange.count);
                    logView.scrollToPosition(listChange.rangeStart + listChange.count - 1);
                    break;
                case Update:
                    logAdapter.notifyItemRangeChanged(listChange.rangeStart, listChange.count);
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        });
    }

    private void parenClick(final View view) {
        final String paren = view.getId() == R.id.eb_left_paren ? "(" : ")";
        codeEditText.getText().insert(codeEditText.getSelectionStart(), paren);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.codeEditText = null;
        this.schemeRunner = null;
        schemeOutputSubscription.unsubscribe();
        schemeOutputSubscription = null;
        logAdapter = null;
        logView = null;
        log = null;
    }

    private void arrowClick(final View view) {
        final int offset = view.getId() == R.id.eb_left_arrow ? -1 : 1;
        moveCursor(offset);
    }

    private void moveCursor(final int shift) {
        final int current = codeEditText.getSelectionStart();
        final int next = current + shift;
        final int nextClamped = Math.max(0, Math.min(codeEditText.length(), next));
        codeEditText.setSelection(nextClamped);
    }

    private void evalClick(final View view) {
        final String text = this.codeEditText.getText().toString().trim();
        if (text.length() == 0)
            return;
        this.codeEditText.getText().clear();
        this.schemeRunner.pushInput(text);
    }

    @Override
    public boolean handleBackKey() {
        if (selectedCount > 0) {
            deselectAllLogItems();
            return true;
        }
        return false;
    }

    private void deselectAllLogItems() {
        for (int i = 0; i < log.size(); i++) {
            final SelectableSchemeLogItem item = log.get(i);
            if (item.isSelected) {
                item.isSelected = false;
                log.touch(i, 1);
            }
        }
        selectedCount = 0;
        invalidateSelectedCount();
    }

    @SuppressLint("NewApi")
    private void invalidateSelectedCount() {
        final boolean isInSelectionMode = selectedCount > 0;
        final FragmentActivity activity = getActivity();
        activity.invalidateOptionsMenu();
    }

    private class LogAdapter extends RecyclerView.Adapter<LogItemVH> implements View.OnClickListener {

        private final RecyclerView logView;
        private List<SelectableSchemeLogItem> items = new ArrayList<>();

        public LogAdapter(final RecyclerView logView) {
            this.logView = logView;
        }

        @Override
        public LogItemVH onCreateViewHolder(final ViewGroup parent, final int viewType) {
            final View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.log_item, parent, false);
            itemView.setOnClickListener(this);
            final TextView textView = (TextView) itemView.findViewById(R.id.text);
            final View overlayView = itemView.findViewById(R.id.overlay);
            return new LogItemVH(itemView, textView, overlayView);
        }

        @Override
        public void onBindViewHolder(final LogItemVH holder, final int position) {
            holder.updateFor(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public void reset(final List<SelectableSchemeLogItem> items) {
            this.items = items;
            notifyDataSetChanged();
        }

        @Override
        public void onClick(final View v) {
            final RecyclerView.LayoutManager layoutManager = logView.getLayoutManager();
            final int position = layoutManager.getPosition(v);
            final SelectableSchemeLogItem item = items.get(position);
            item.isSelected ^= true;
            selectedCount += (item.isSelected ? 1 : -1);
            if (selectedCount == 0 || (selectedCount == 1 && item.isSelected)) {
                invalidateSelectedCount();
            }
            notifyItemChanged(position);
        }
    }

    private static class LogItemVH extends RecyclerView.ViewHolder {

        private final TextView textView;
        private final View overlayView;

        public LogItemVH(final View itemView, final TextView textView, final View overlayView) {
            super(itemView);
            this.textView = textView;
            this.overlayView = overlayView;
        }

        public void updateFor(final SelectableSchemeLogItem logItem) {
            itemView.setBackgroundResource(getLogItemColorResId(logItem.kind));
            textView.setText(logItem.getDisplayContent());
            textView.setTypeface(Typeface.create(textView.getTypeface(), getLogItemTextStyle(logItem.kind)));
            if (logItem.isSelected) {
                overlayView.setBackgroundResource(R.drawable.log_item_overlay_selected);
            } else {
                overlayView.setBackgroundColor(Color.TRANSPARENT);
            }
        }

        private int getLogItemTextStyle(final SchemeLogItemKind kind) {
            switch (kind) {
                case SystemInfo:
                    return Typeface.ITALIC;
                default:
                    return Typeface.NORMAL;
            }
        }

        private int getLogItemColorResId(final SchemeLogItemKind kind) {
            switch (kind) {
                case Input:
                    return R.color.log_background_input;
                case Output:
                    return R.color.log_background_output;
                case ErrorOutput:
                    return R.color.log_background_error_output;
                case SystemInfo:
                    return R.color.log_background_info_output;
            }
            throw new IllegalArgumentException();
        }
    }

    private class CodeEditTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
        }

        @Override
        public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
        }

        @Override
        public void afterTextChanged(final Editable s) {
        }
    }
}

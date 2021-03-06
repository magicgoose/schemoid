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
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import magicgoose.schemoid.R;
import magicgoose.schemoid.TheApp;
import magicgoose.schemoid.scheme.ISchemeRunner;
import magicgoose.schemoid.scheme.SchemeLogItemKind;
import magicgoose.schemoid.scheme.exception.MalformedInputException;
import magicgoose.schemoid.scheme.parser.HighlightRegion;
import magicgoose.schemoid.scheme.parser.SchemeParser;
import magicgoose.schemoid.scheme.parser.SchemeToken;
import magicgoose.schemoid.util.ClipboardUtil;
import magicgoose.schemoid.util.ReactiveList;
import magicgoose.schemoid.view.SelectableEditText;
import rx.Subscription;

public class ReplFragment extends Fragment implements BackKeyHandler {

    public static final String TAG = ReplFragment.class.getSimpleName();
    private SelectableEditText codeEditText;
    private ISchemeRunner<SelectableSchemeLogItem> schemeRunner;
    private Subscription schemeOutputSubscription;
    private LogAdapter logAdapter;
    private RecyclerView logView;
    private ReactiveList<SelectableSchemeLogItem> log;
    private int selectedCount;
    private SchemeParser schemeParser;
    private List<SchemeToken> tokens;
    private final List<Subscription> resumeSubscriptions = new ArrayList<>();

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

        codeEditText = (SelectableEditText) view.findViewById(R.id.code_input);

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
        schemeParser = TheApp.getInstance().getSchemeParser();
        log = schemeRunner.getLog();
        schemeOutputSubscription = bindAdapterToList(logAdapter, log);
        selectedCount = getSelectedCount(log);
    }

    @Override
    public void onResume() {
        super.onResume();

        resumeSubscriptions.addAll(Arrays.asList(
                codeEditText.getTextObservable()
                        .subscribe(this::onInputTextChanged),
                codeEditText.getSelectionStartObservable()
                        .subscribe(this::onInputSelectionChanged),
                codeEditText.getBackspaceObservable()
                        .subscribe(this::onBackspace),
                codeEditText.getNewlineObservable()
                        .subscribe(this::onNewline)
        ));
    }

    @Override
    public void onPause() {
        super.onPause();
        for (final Subscription subscription : resumeSubscriptions) {
            subscription.unsubscribe();
        }
        resumeSubscriptions.clear();
        try {
            TheApp.getInstance().saveLog(log);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void onNewline(Void dummy) {
        Log.w(TAG, "onNewline()");
        final int selectionStart = codeEditText.getSelectionStart();
        final Editable text = codeEditText.getText();
        if (tokens == null) {
            tokens = schemeParser.tokenize(text.toString());
        }
        final int indentLength = getIndentLength(tokens, selectionStart);
        final String textToInsert = getNewlineString(indentLength);
        text.insert(selectionStart, textToInsert);
    }

    private String getNewlineString(final int indentLength) {
        final int indentSize = 2;
        final int spaceCount = indentSize * indentLength;
        final StringBuilder sb = new StringBuilder(1 + spaceCount);
        sb.append('\n');
        for (int i = 0; i < spaceCount; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private int getIndentLength(final List<SchemeToken> tokens, final int cursor) {
        int result = 0;
        for (final SchemeToken token : tokens) {
            if (token.end > cursor)
                break;
            switch (token.kind) {
                case OpeningParen:
                    ++result;
                    break;
                case ClosingParen:
                    --result;
                    break;
            }
        }
        return Math.max(0, result);
    }

    private void onBackspace(Void dummy) {
        final Editable text = codeEditText.getText();
        final int selectionStart = codeEditText.getSelectionStart();
        if (selectionStart > 0) {
            if (text.length() > selectionStart &&
                    text.charAt(selectionStart - 1) == '(' &&
                    text.charAt(selectionStart) == ')') {
                text.delete(selectionStart - 1, selectionStart + 1);
            } else if (Character.isWhitespace(text.charAt(selectionStart - 1))) {
                int i = selectionStart - 1;
                while (i - 1 >= 0) {
                    final char ch = text.charAt(i - 1);
                    if (Character.isWhitespace(ch)) {
                        --i;
                    } else {
                        break;
                    }
                    if (ch == '\n') // there has to be a way to delete only some of the lines
                        break;
                }
                text.delete(i, selectionStart);
            } else {
                text.delete(selectionStart - 1, selectionStart);
            }
        }
    }

    private void onInputTextChanged(String newText) {
        Log.w(TAG, "onInputTextChanged(…)");
        tokens = schemeParser.tokenize(newText);
        postUpdateSpans();
    }

    private final Runnable updateSpansRunnable = this::updateSpans;

    private void postUpdateSpans() {
        if (codeEditText == null) return;
        codeEditText.removeCallbacks(updateSpansRunnable);
        codeEditText.post(updateSpansRunnable);
    }

    private void onInputSelectionChanged(Void dummy) {
        postUpdateSpans();
    }

    private void updateSpans() {
        Log.w(TAG, "updateSpans()");
        if (tokens == null)
            return;

        codeEditText.eventsDisabled = true;

        final String text = codeEditText.getText().toString();
        final int selectionStart = codeEditText.getSelectionStart();
        final int selectionEnd = codeEditText.getSelectionEnd();

        final SpannableString spannableString = new SpannableString(text);

        final HighlightRegion highlightRegion = schemeParser.getHighlightRegionForParens(tokens, selectionStart);

        if (highlightRegion.start != null || highlightRegion.end != null) {
            final int midStart = highlightRegion.start == null ? 0 : highlightRegion.start.start;
            final int midEnd = highlightRegion.end == null ? text.length() : Math.min(text.length(), highlightRegion.end.end);
            spannableString.setSpan(
                    backgroundColorSpan(R.color.parens_region_highlight),
                    midStart, midEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        if (highlightRegion.start != null) {
            spannableString.setSpan(
                    backgroundColorSpan(R.color.parens_pair_highlight),
                    highlightRegion.start.start, highlightRegion.start.start + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        if (highlightRegion.end != null) {
            spannableString.setSpan(
                    backgroundColorSpan(R.color.parens_pair_highlight),
                    highlightRegion.end.end - 1, highlightRegion.end.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        codeEditText.setText(spannableString);
        codeEditText.setSelection(selectionStart, selectionEnd);

        codeEditText.post(() -> codeEditText.eventsDisabled = false);
    }

    private BackgroundColorSpan backgroundColorSpan(int colorId) {
        return new BackgroundColorSpan(getResources().getColor(colorId));
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
        final boolean isOpeningParen = view.getId() == R.id.eb_left_paren;
        final String paren = isOpeningParen ? "(" : ")";
        final Editable text = codeEditText.getText();
        final int length = text.length();

        final int selectionStart = codeEditText.getSelectionStart();

        // sometimes spannable string builder fails for no obvious reason when trying to insert text
        // hence this stupid code
        if (isOpeningParen) {
            text.insert(selectionStart, "()");
            moveCursor(-1);
        } else {
            if (selectionStart < length && text.charAt(selectionStart) == ')') {
                moveCursor(1);
            } else {
                text.insert(selectionStart, paren);
            }
        }

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.codeEditText = null;
        this.schemeRunner = null;
        this.schemeParser = null;
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
        try {
            this.schemeRunner.pushInput(text);
            this.codeEditText.getText().clear();
        } catch (MalformedInputException e) {
            TheApp.getInstance().showToast(R.string.malformed_input);
        }
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
        final FragmentActivity activity = getActivity();
        activity.supportInvalidateOptionsMenu();
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
}

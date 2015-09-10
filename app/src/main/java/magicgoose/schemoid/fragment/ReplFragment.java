package magicgoose.schemoid.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;

import magicgoose.schemoid.R;
import magicgoose.schemoid.TheApp;
import magicgoose.schemoid.scheme.ISchemeRunner;
import magicgoose.schemoid.scheme.SchemeLogItem;
import magicgoose.schemoid.scheme.SchemeLogItemKind;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

public class ReplFragment extends Fragment {

    private EditText codeEditText;
    private ISchemeRunner schemeRunner;
    private Subscription schemeOutputSubscription;
    private LogAdapter logAdapter;
    private View editorButtons;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
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

        editorButtons = view.findViewById(R.id.editor_buttons);
        codeEditText = (EditText) view.findViewById(R.id.code_input);
        codeEditText.setOnFocusChangeListener(this::handleCodeEditTextFocusChange);
        handleCodeEditTextFocusChange(codeEditText, codeEditText.hasFocus());

        view.findViewById(R.id.eb_eval).setOnClickListener(this::evalClick);

        view.findViewById(R.id.eb_left_arrow).setOnClickListener(this::arrowClick);
        view.findViewById(R.id.eb_right_arrow).setOnClickListener(this::arrowClick);

        view.findViewById(R.id.eb_left_paren).setOnClickListener(this::parenClick);
        view.findViewById(R.id.eb_right_paren).setOnClickListener(this::parenClick);

        final RecyclerView logView = ((RecyclerView) view.findViewById(R.id.code_output));
        logAdapter = new LogAdapter(logView);
        logView.setLayoutManager(new LinearLayoutManager(ctx, LinearLayoutManager.VERTICAL, false));
        logView.setAdapter(logAdapter);

        schemeRunner = TheApp.getInstance().getSchemeRunner();
        schemeOutputSubscription = schemeRunner.getOutputs().
                observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onSchemeOutput);
    }

    private void handleCodeEditTextFocusChange(final View view, final boolean hasFocus) {
        editorButtons.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
    }

    private void onSchemeOutput(SchemeLogItem output) {
        logAdapter.appendItem(output);
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
        editorButtons = null;
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

    private static class LogAdapter extends RecyclerView.Adapter<LogItemVH> {

        private final ArrayList<SchemeLogItem> items = new ArrayList<>();
        private final RecyclerView logView;

        public LogAdapter(final RecyclerView logView) {
            this.logView = logView;
        }

        public void appendItem(SchemeLogItem item) {
            final int prevSize = items.size();
            items.add(item);
            notifyItemInserted(prevSize);
            logView.scrollToPosition(prevSize);
        }

        @Override
        public LogItemVH onCreateViewHolder(final ViewGroup parent, final int viewType) {
            final TextView view = (TextView) LayoutInflater.from(parent.getContext()).inflate(R.layout.log_item, parent, false);
            return new LogItemVH(view);
        }

        @Override
        public void onBindViewHolder(final LogItemVH holder, final int position) {
            holder.updateFor(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private static class LogItemVH extends RecyclerView.ViewHolder {

        public LogItemVH(final View itemView) {
            super(itemView);
        }

        public void updateFor(final SchemeLogItem logItem) {
            final TextView view = (TextView) this.itemView;
            view.setText(logItem.content);
            view.setBackgroundResource(getLogItemColorResId(logItem.kind));
        }

        private int getLogItemColorResId(final SchemeLogItemKind kind) {
            switch (kind) {
                case Input:
                    return R.color.log_background_input;
                case Output:
                    return R.color.log_background_output;
                case ErrorOutput:
                    return R.color.log_background_error_output;
            }
            throw new IllegalArgumentException();
        }
    }
}

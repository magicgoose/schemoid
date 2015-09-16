package magicgoose.schemoid.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.EditText;

import rx.Observable;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

public class SelectableEditText extends EditText {

    private String oldString;

    public Observable<Pair<Integer, Integer>> getSelectionStartObservable() {
        return selectionStartSubject.distinctUntilChanged();
    }

    public Observable<String> getTextObservable() {
        return textSubject;
    }

    public Observable<Void> getBackspaceObservable() {
        return backspaceSubject;
    }

    private final BehaviorSubject<Pair<Integer, Integer>> selectionStartSubject = BehaviorSubject.create();
    private final BehaviorSubject<String> textSubject = BehaviorSubject.create();
    private final PublishSubject<Void> backspaceSubject = PublishSubject.create();


    @Override
    protected void onSelectionChanged(final int selStart, final int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        if (selectionStartSubject == null) return;
        selectionStartSubject.onNext(Pair.create(selStart, selEnd));
    }

    @Override
    protected void onTextChanged(final CharSequence text, final int start, final int lengthBefore, final int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        if (selectionStartSubject == null || textSubject == null) return;
        final String newString = text.toString();
        if (newString.equals(oldString))
            return;
        oldString = newString;
        textSubject.onNext(newString);
        selectionStartSubject.onNext(Pair.create(getSelectionStart(), getSelectionEnd()));
    }

    @Override
    public InputConnection onCreateInputConnection(@NonNull final EditorInfo outAttrs) {
        return new YobaInputConnection(super.onCreateInputConnection(outAttrs),
                true);
    }

    public SelectableEditText(final Context context) {
        super(context);
    }

    public SelectableEditText(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public SelectableEditText(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SelectableEditText(final Context context, final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private class YobaInputConnection extends InputConnectionWrapper {
        public YobaInputConnection(InputConnection target, boolean mutable) {
            super(target, mutable);
        }

        @Override
        public boolean sendKeyEvent(KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getKeyCode() == KeyEvent.KEYCODE_DEL) {

                notifyBackspace();
                return false;
            }
            return super.sendKeyEvent(event);
        }

        @Override
        public boolean deleteSurroundingText(final int beforeLength, final int afterLength) {
            return super.deleteSurroundingText(beforeLength, afterLength);
        }
    }

    private void notifyBackspace() {
        if (backspaceSubject != null)
            backspaceSubject.onNext(null);
    }
}

package magicgoose.schemoid.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
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

    public Observable<Void> getSelectionStartObservable() {
        return selectionStartSubject;
    }

    public Observable<String> getTextObservable() {
        return textSubject;
    }

    public Observable<Void> getBackspaceObservable() {
        return backspaceSubject;
    }

    public Observable<Void> getNewlineObservable() {
        return newlineSubject;
    }

    private final BehaviorSubject<Void> selectionStartSubject = BehaviorSubject.create();
    private final BehaviorSubject<String> textSubject = BehaviorSubject.create();
    private final PublishSubject<Void> backspaceSubject = PublishSubject.create();
    private final PublishSubject<Void> newlineSubject = PublishSubject.create();


    public boolean eventsDisabled = false;


    @Override
    protected void onSelectionChanged(final int selStart, final int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        if (eventsDisabled)
            return;
        if (selectionStartSubject == null) return;
        selectionStartSubject.onNext(null);
    }

    @Override
    protected void onTextChanged(final CharSequence text, final int start, final int lengthBefore, final int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        if (eventsDisabled) return;
        if (selectionStartSubject == null || textSubject == null) return;
        final String newString = text.toString();
        if (newString.equals(oldString))
            return;
        oldString = newString;
        textSubject.onNext(newString);
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
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                final int keyCode = event.getKeyCode();
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DEL:
                        notifyBackspace();
                        return false;
                    case KeyEvent.KEYCODE_ENTER:
                    case KeyEvent.KEYCODE_NUMPAD_ENTER:
                        notifyNewline();
                        return false;
                }

            }
            return super.sendKeyEvent(event);
        }
    }

    private void notifyNewline() {
        if (newlineSubject != null)
            newlineSubject.onNext(null);
    }

    private void notifyBackspace() {
        if (backspaceSubject != null)
            backspaceSubject.onNext(null);
    }
}

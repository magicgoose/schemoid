package magicgoose.schemoid.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.text.Layout;
import android.text.Spannable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.EditText;
import android.widget.TextView;

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

    private final BehaviorSubject<Void> selectionStartSubject = BehaviorSubject.create();
    private final BehaviorSubject<String> textSubject = BehaviorSubject.create();
    private final PublishSubject<Void> backspaceSubject = PublishSubject.create();

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
//
//    /* Code adapted from TextView. It's a hidden api */
//    private static int getVerticalOffset(TextView view) {
//        int offset = 0;
//        final int gravity = view.getGravity() & Gravity.VERTICAL_GRAVITY_MASK;
//
//        Layout l = view.getLayout();
//
//        if (gravity != Gravity.TOP) {
//            // TODO(pa): handle padding with optical insets drawables
//            int padding = view.getExtendedPaddingTop() + view.getExtendedPaddingBottom();
//            int boxHeight = view.getMeasuredHeight() - padding;
//            int textHeight = l.getHeight();
//
//            if (textHeight < boxHeight) {
//                if (gravity == Gravity.BOTTOM) {
//                    offset = boxHeight - textHeight;
//                } else { // (gravity == Gravity.CENTER_VERTICAL)
//                    offset = (boxHeight - textHeight) >> 1;
//                }
//            }
//        }
//        return offset;
//    }
//
//    @Override
//    protected void onDraw(@NonNull Canvas canvas) {
//        Spannable text = getText();
//        FixedBackgroundColorSpan[] spans = text.getSpans(0, text.length(), FixedBackgroundColorSpan.class);
//        if (spans.length > 1) {
//            int cursorOffsetVertical = getVerticalOffset(this);
//            Layout layout = getLayout();
//            for (FixedBackgroundColorSpan span : spans) {
//                int start = text.getSpanStart(span);
//                int end = text.getSpanEnd(span);
//                layout.getSelectionPath(start, end, mFocusHighlight);
//                mFocusHighlightPaint.setColor(span.getBackgroundColor());
//                canvas.translate(0, cursorOffsetVertical);
//                canvas.drawPath(mFocusHighlight, mFocusHighlightPaint);
//                canvas.translate(0, -cursorOffsetVertical);
//            }
//        }
//        super.onDraw(canvas);
//    }
}

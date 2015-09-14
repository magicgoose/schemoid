package magicgoose.schemoid.scheme;

import android.text.TextUtils;

public class SchemeLogItem {
    public final SchemeLogItemKind kind;
    public final String content;

    private transient String displayContent;

    public SchemeLogItem(final SchemeLogItemKind kind, final String content) {
        this.kind = kind;
        this.content = content;
    }

    public String getDisplayContent() {
        switch (kind) {
            case Input:
                return formatInput(content);
            default:
                return content;
        }
    }

    private String formatInput(final String input) {
        if (displayContent == null) {
            final String[] lines = input.split("\\n");
            lines[0] = "> " + lines[0];
            for (int i = 1; i < lines.length; i++) {
                lines[i] = "  " + lines[i];
            }
            displayContent = TextUtils.join("\n", lines);
        }
        return displayContent;
    }
}

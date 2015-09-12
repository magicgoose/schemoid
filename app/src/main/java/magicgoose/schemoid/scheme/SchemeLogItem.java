package magicgoose.schemoid.scheme;

public class SchemeLogItem {
    public final SchemeLogItemKind kind;
    public final String displayContent;
    public final String formattedContent;

    public SchemeLogItem(final SchemeLogItemKind kind, final String displayContent, final String formattedContent) {
        this.kind = kind;
        this.displayContent = displayContent;
        this.formattedContent = formattedContent;
    }
}

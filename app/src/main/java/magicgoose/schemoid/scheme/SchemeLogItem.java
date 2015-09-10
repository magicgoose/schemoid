package magicgoose.schemoid.scheme;

public class SchemeLogItem {
    public final SchemeLogItemKind kind;
    public final String content;

    public SchemeLogItem(final SchemeLogItemKind kind, final String content) {
        this.kind = kind;
        this.content = content;
    }
}

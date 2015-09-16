package magicgoose.schemoid.scheme.parser;

public class SchemeToken {
    // where it's located in the source
    public final int start;
    public final int end;

    public final SchemeTokenKind kind;

    public SchemeToken(final int start, final int end, final SchemeTokenKind kind) {
        this.start = start;
        this.end = end;
        this.kind = kind;
    }

    public String clip(final String input) {
        return input.substring(this.start, this.end);
    }
}

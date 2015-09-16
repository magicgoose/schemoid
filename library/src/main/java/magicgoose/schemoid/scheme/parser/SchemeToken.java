package magicgoose.schemoid.scheme.parser;

@SuppressWarnings("ComparableImplementedButEqualsNotOverridden")
public class SchemeToken implements Comparable<SchemeToken> {
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

    public static SchemeToken indexSearchKey(final int position) {
        return new SchemeToken(position, position, SchemeTokenKind.SomethingElse);
    }

    @Override
    public int compareTo(final SchemeToken other) {
        if (this.start >= other.end)
            return 1;
        if (other.start >= this.end)
            return -1;

        return 0;
    }
}

package magicgoose.schemoid.scheme.parser;

public class HighlightRegion {
    public final SchemeToken start;
    public final SchemeToken end;

    public HighlightRegion(final SchemeToken start, final SchemeToken end) {
        this.start = start;
        this.end = end;
    }
}

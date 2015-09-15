package magicgoose.schemoid.scheme.parser;

import java.util.Collections;
import java.util.List;

public class SchemeExpr {
    // where it's located in the source
    public final int start;
    public final int end;

    public final List<SchemeExpr> children;

    public final SchemeExprKind kind;

    public SchemeExpr(final SchemeExprKind kind, final List<SchemeExpr> children, final int start, final int end) {
        this.start = start;
        this.end = end;
        this.children = children;
        this.kind = kind;
    }

    public SchemeExpr(final SchemeExprKind kind, final int start, final int end) {
        this.start = start;
        this.end = end;
        this.kind = kind;
        children = Collections.emptyList();
    }

    public SchemeExpr(final SchemeExprKind kind, final SchemeExpr child, final int start, final int end) {
        this.start = start;
        this.end = end;
        this.kind = kind;
        children = Collections.singletonList(child);
    }

    @Override
    public String toString() {
        return "SchemeExpr{\n" +
            "kind=" + kind +
            ", start=" + start +
            ", end=" + end +
            ", children=" + children +
            '}';
    }

    public String clip(final String input) {
        return input.substring(this.start, this.end);
    }

    public String reprint(final String input) {
        switch (kind) {
            case List:
                return reprintList(input);
            case QuotedExpression:
                return "'" + children.get(0).reprint(input);
            default:
                return clip(input);
        }
    }

    private String reprintList(final String input) {
        final StringBuilder sb = new StringBuilder();
        sb.append('(');
        boolean first = true;
        for (final SchemeExpr child : children) {
            if (!first) {
                sb.append(' ');
            }
            first = false;
            sb.append(child.reprint(input));
        }
        sb.append(')');
        return sb.toString();
    }
}

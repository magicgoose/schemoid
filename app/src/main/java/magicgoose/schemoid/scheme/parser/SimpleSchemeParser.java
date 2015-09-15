package magicgoose.schemoid.scheme.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// it doesn't care about a lot of things, its main purpose is
// to provide information for syntax(braces) highlighting and
// splitting text with several expressions
// TODO: somehow allow parsing incomplete expressions
public class SimpleSchemeParser implements SchemeParser {
    private static final Pattern charPattern =
        Pattern.compile("^#\\\\(\\d+|u\\d+|\\D[^\\)\\(\\d\\s]*)");
    private static final Pattern stringPattern =
        Pattern.compile("^\"(\\\\\\\\|\\\\\"|[^\"])*\"");
    private static final Pattern somethingElsePattern =
        Pattern.compile("^[^'\\(\\)\\s]+");

    @Override
    public List<SchemeExpr> parseAll(final String source) {
        final ArrayList<SchemeExpr> exprs = new ArrayList<>();
        int start = 0;
        while (true) {
            final SchemeExpr parseResult = parseOneExpr(start, source);
            if (parseResult == null) {
                return exprs; // TODO: report remaining unmatched input
            }
            exprs.add(parseResult);
            start = parseResult.end;
        }
    }

    private SchemeExpr parseOneExpr(final int start, final String input) {
        final int realStart = skipWhitespace(start, input);
        return parseOneExprNoLeadingWhitespace(realStart, input);
    }

    private SchemeExpr parseOneExprNoLeadingWhitespace(final int start, final String input) {
        if (start >= input.length())
            return null;

        return parseFirstMatch(start, input,
            this::parseQuotedExpr,
            this::parseChar,
            this::parseString,
            this::parseList,
            this::parseSomething
        );
    }

    private SchemeExpr parseQuotedExpr(final int start, final String input) {
        if (input.startsWith("'", start)) {
            final SchemeExpr quotedExpr = parseOneExprNoLeadingWhitespace(start + 1, input);
            if (quotedExpr != null) {
                return new SchemeExpr(SchemeExprKind.QuotedExpression,
                    quotedExpr,
                    start, quotedExpr.end);
            }
        }
        return null;
    }

    private SchemeExpr parseChar(final int start, final String input) {
        return parseRegex(start, input,
            charPattern, SchemeExprKind.LiteralChar);
    }
    private SchemeExpr parseString(final int start, final String input) {
        return parseRegex(start, input,
            stringPattern, SchemeExprKind.LiteralString);
    }
    private SchemeExpr parseSomething(final int start, final String input) {
        return parseRegex(start, input,
            somethingElsePattern, SchemeExprKind.SomethingElse);
    }

    private SchemeExpr parseList(final int start, final String input) {
        if (input.charAt(start) != '(') {
            return null;
        }
        int i = start + 1;
        final ArrayList<SchemeExpr> items = new ArrayList<>();
        while (true) {
            if (i >= input.length())
                return null;
            i = skipWhitespace(i, input);
            if (i >= input.length())
                return null;

            if (input.charAt(i) == ')') {
                return new SchemeExpr(SchemeExprKind.List, items,
                    start, i + 1);
            }

            final SchemeExpr nextExpr = parseOneExprNoLeadingWhitespace(i, input);
            if (nextExpr == null) {
                return null;
            }
            items.add(nextExpr);
            i = nextExpr.end;
        }
    }



    private SchemeExpr parseRegex(final int start, final String input, final Pattern pattern, final SchemeExprKind kind) {
        final Matcher matcher = pattern.matcher(input.substring(start));
        if (matcher.find()) {
            final int end = matcher.end();
            return new SchemeExpr(kind, start, end + start);
        }
        return null;
    }

    private SchemeExpr parseFirstMatch(final int start, final String input, ParserFun... options) {
        for (final ParserFun option : options) {
            final SchemeExpr expr = option.tryParse(start, input);
            if (expr != null) {
                return expr;
            }
        }
        return null;
    }

    private int skipWhitespace(final int start, final String input) {
        int realStart = start;
        while (realStart < input.length() && Character.isWhitespace(input.charAt(realStart))) {
            realStart++;
        }
        return realStart;
    }

    interface ParserFun {
        SchemeExpr tryParse(final int start, final String input);
    }
}

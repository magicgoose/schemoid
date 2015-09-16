package magicgoose.schemoid;

import java.util.List;

import magicgoose.schemoid.scheme.parser.SchemeToken;
import magicgoose.schemoid.scheme.parser.SimpleSchemeParser;

public class Main {
    public static void main(String[] args) {
        final SimpleSchemeParser parser = new SimpleSchemeParser();


        final String source = "(define (stupid-factorial x)" +
                "(display '(#\\u123 \"asdas\\\"fafdg\"))" +
                "(if (> x 1) (* x (stupid-factorial (- x 1))) 1))";
        final List<SchemeToken> tokens = parser.tokenize(
                source
        );

        for (final SchemeToken token : tokens) {
            System.out.println(token.clip(source) + " — " + token.kind);
        }
    }
}

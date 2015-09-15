package magicgoose.schemoid.scheme.parser;

import java.util.List;

public interface SchemeParser {
    List<SchemeExpr> parseAll(String source);
}

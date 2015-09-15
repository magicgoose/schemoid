package magicgoose.schemoid.scheme;


import java.io.Closeable;

import magicgoose.schemoid.scheme.exception.MalformedInputException;
import magicgoose.schemoid.util.ReactiveList;
import rx.Observable;

public interface ISchemeRunner<TLogItem> extends Closeable {
    ReactiveList<TLogItem> getLog();
    void pushInput(String input) throws MalformedInputException;
    void abort();
    void reset();
}

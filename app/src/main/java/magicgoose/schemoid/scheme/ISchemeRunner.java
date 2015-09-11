package magicgoose.schemoid.scheme;


import java.io.Closeable;

import magicgoose.schemoid.util.ReactiveList;
import rx.Observable;

public interface ISchemeRunner extends Closeable {
    ReactiveList<SchemeLogItem> getLog();
    void pushInput(String input);
    void abort();
    void reset();
}

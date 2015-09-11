package magicgoose.schemoid.scheme;


import java.io.Closeable;

import rx.Observable;

public interface ISchemeRunner extends Closeable {
    Observable<SchemeLogItem> getOutputs();
    void pushInput(String input);
    void abort();
    void reset();
}

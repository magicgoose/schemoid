package magicgoose.schemoid.scheme;


import java.io.Closeable;

import rx.Observable;

public interface ISchemeRunner extends Closeable {
    Observable<String> getOutputs();
    void pushInput(String input);
}

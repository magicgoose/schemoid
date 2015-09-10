package magicgoose.schemoid.scheme;

import java.io.IOException;

import jscheme.JScheme;
import rx.Observable;
import rx.subjects.ReplaySubject;

//public class DummySchemeRunner implements ISchemeRunner {
//
//    private final ReplaySubject<SchemeLogItem> outputsSubject = ReplaySubject.create();
//
//    @Override
//    public Observable<SchemeLogItem> getOutputs() {
//        return outputsSubject;
//    }
//
//    @Override
//    public void pushInput(final String input) {
//        final String reply = "You said: \"" + input + "\"; I don't know what to reply, because I'm a DummySchemeRunner";
//        outputsSubject.onNext(reply);
//    }
//
//    @Override
//    public void close() throws IOException {
//    }
//}

package magicgoose.schemoid.scheme;

import android.annotation.SuppressLint;
import android.content.res.Resources;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jscheme.JScheme;
import jsint.U;
import rx.Observable;
import rx.subjects.ReplaySubject;

public class JSchemeRunner implements ISchemeRunner {

    private final ReplaySubject<SchemeLogItem> outputsSubject = ReplaySubject.create();

    private final JScheme jsc = new JScheme();

    private final ExecutorService es = Executors.newSingleThreadExecutor();

    public JSchemeRunner(Resources rs) {
        es.submit(() -> doInit(rs));
    }

    @SuppressLint("NewApi")
    private void doInit(final Resources rs) {
        try (final InputStreamReader istr = new InputStreamReader(new BufferedInputStream(rs.getAssets().open("jscheme.init")))) {
            final Object loadResult = jsc.load(istr);
            final boolean loadResultAsBool = U.to_bool(loadResult);
            if (!loadResultAsBool) {
                throw new RuntimeException("Could not load init script");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Observable<SchemeLogItem> getOutputs() {
        return outputsSubject;
    }

    @Override
    public void pushInput(final String input) {
        outputsSubject.onNext(new SchemeLogItem(SchemeLogItemKind.Input, input));
        es.submit(() -> doProcessString(input));
    }

    private void doProcessString(final String input) {
        final SchemeLogItem result = doEvalString(input);
        outputsSubject.onNext(result);
    }

    private SchemeLogItem doEvalString(final String input) {
        try {
            final Object ret = jsc.eval(input);
            if (ret == jsint.Primitive.DO_NOT_DISPLAY) {
                return outputLogItem("<ok>");
            } else {
                return outputLogItem(jsint.U.stringify(ret));
            }
        } catch (final jscheme.SchemeException e) {
            return errorLogItem(e.getMessage());
        } catch (final jsint.BacktraceException e) {
            return errorLogItem(e.getMessage());
        } catch (final Exception e) {
            return errorLogItem(e.toString());
        } catch (final Error e) {
            return errorLogItem(e.toString());
        } catch (final Throwable e) {
            return errorLogItem(e.toString());
        }
    }

    private SchemeLogItem errorLogItem(final String text) {
        return new SchemeLogItem(SchemeLogItemKind.ErrorOutput, text);
    }

    private SchemeLogItem outputLogItem(final String text) {
        return new SchemeLogItem(SchemeLogItemKind.Output, text);
    }

    @Override
    public void close() throws IOException {
        es.shutdownNow();
    }
}
